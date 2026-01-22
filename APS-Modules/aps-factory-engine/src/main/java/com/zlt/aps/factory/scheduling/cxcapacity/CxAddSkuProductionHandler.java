package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.DayCapacityLimitVo;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.enums.ProductionQtyModelEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在机结构，新增规格模具排产处理
 *
 * @author ZLT
 * @date 20251220
 */
@Slf4j
public class CxAddSkuProductionHandler {

    /**
     * 在机结构 - 在产机台的新增规格排产
     * 按优先级估算,此时经过在机结构对续作部分排产，
     * 已经初步进行在产机台的分配
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组排产计划信息，包含分组名(TBR=结构名)、起始及理论收尾日期
     * @param excludeDays   排除的收尾时间点
     */
    public static void productionAddSkuByContinueCxMachine(Context context, ProductionPlanGroupInfo groupPlanInfo, Set<Integer> excludeDays) {
        //基础校验 有可排产计划且能找到最早收尾的硫化组
        EarliestConclusionLhGroupHelper lhGroup = checkBaseProductionCondition(context, groupPlanInfo, excludeDays);
        if (null == lhGroup) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = groupPlanInfo.getGroupName();
        String onLineMachineInfo = String.join(StringConstant.COMMA, groupPlanInfo.getAllocationCxMachineCodeSet());
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
//        if (CollectionUtils.isEmpty(groupPlanData)) {
//            //记录日志
//            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoPlanLog(context, groupName, onLineMachineInfo));
//            return;
//        }
//        List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProductionThisRound()).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(leftOverHasProductionList)) {
//            //记录日志
//            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoProductionPlanLog(context, groupName, onLineMachineInfo));
//            return;
//        }
//        //获取最先收尾的硫化组
//        EarliestConclusionLhGroupHelper lhGroup = groupPlanInfo.getEarliestConclusionLhInfo(productionContext, excludeDays);
//        if (null == lhGroup) {
//            //记录日志
//            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoLhGroupLog(context, groupName, onLineMachineInfo));
//            return;
//        }
//        log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineLhGroupRangeLog(context, groupName, onLineMachineInfo, startDay, endDay));
//        if (startDay > endDay) {
//            return;
//        }
        List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProductionThisRound()).collect(Collectors.toList());
        Integer startDay = lhGroup.getClosingDay();
        //成型分配的排产范围起始日~分组收尾日
        Integer endDay = lhGroup.getEndDay();
        //获取优先级最高的Sku信息
        String materialDesc = getSelectedAddSku(productionContext, startDay, endDay, leftOverHasProductionList);
        log.info(TbrMouldProductionLogRecorder.addContinueGroupLhGroupFindSkuLog(context, groupName, onLineMachineInfo, materialDesc));
        if (StringUtils.isBlank(materialDesc)) {
            //20260113 剔除需要排除的收尾时间点
            excludeDays.add(startDay);
            //递归：重新获取下一组
            productionAddSkuByContinueCxMachine(context, groupPlanInfo, excludeDays);
            return;
        }
        //计算需要排产的量
        SkuNeedProductionInfo needProductionInfo = getNeedProductionQty(leftOverHasProductionList, materialDesc);
        if (null == needProductionInfo) {
            //todo 记录日志
            return;
        }
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = productionContext.selectedDoubleMouldByRange(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueLhGroupSkuNoFindMouldLog(context, groupName, onLineMachineInfo, materialDesc));
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays);
            return;
        }
        //重新确认排产时间范围
        groupPlanInfo.correctProductionDateRange(context, needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue()), lhGroup, doubleMouldList, onLineMachineInfo);
        startDay = lhGroup.getClosingDay();
        endDay = lhGroup.getEndDay();
        log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueMachineCorrectLhGroupRangeLog(context, groupName, onLineMachineInfo, startDay, endDay));
        if (null == startDay || null == endDay) {
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays);
            return;
        }
        //根据模具的排产范围再次修正排产范围
        Integer sumProductionQty = needProductionInfo.getSumNeedProductionQty();
        Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupPlanInfo, groupPlanInfo.getAllocationCxMachineCodeSet(), lhGroup.transformCxLhGroup(), sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //开始排产 CxLhMouldProductionCalculator.lhProductionHandler(context, lhProductionQtyHelper, startDay, endDay, doubleMouldList, needProductionInfo.getNeedProductionList());
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, doubleMouldList, needProductionInfo.getNeedProductionList());
        //递归：重新获取下一组
        productionAddSkuByContinueCxMachine(context, groupPlanInfo, excludeDays);
    }

    /**
     * 新增结构新增规格排产
     * 或是在机结构新增机台排产
     * 按优先级估算，此时是按机台+结构方式排产
     *
     * @param context            排产上下文
     * @param cxMachineCode      成型机台
     * @param productionPlanList 还需排产的计划
     * @param productionPlan     分组排产信息，包含分组名(TBR=结构名)、起始及理论收尾日期
     * @param mouldShellMap      模壳信息
     */
    public static void productionAddSku(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
        String groupName = productionPlanInfo.getGroupName();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addGroupCxMachineMouldNoFindMachineInfoLog(context, groupName, cxMachineCode));
            return;
        }
        //获取最先收尾的硫化组
        CxLhProductionHelper cxLhGroup = cxMachineInfo.getEarliestConclusionLhGroup();
        if (null == cxLhGroup) {
            return;
        }
        Integer startDay = cxLhGroup.getProductionDay();
        //成型分配的排产范围起始日~分组收尾日
        Integer endDay = productionPlan.getEndDay();
        if (startDay > endDay) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupStartLimitEndLog(context, groupName, cxMachineCode, startDay, endDay));
            return;
        }
        //获取优先级最高的Sku信息
        String materialDesc = getSelectedAddSku(productionContext, startDay, endDay, productionPlanList);
        if (StringUtils.isBlank(materialDesc)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupNoFindSkuLog(context, groupName, cxMachineCode));
            //20260113 标记硫化组不再参与，重新获取下一组
            cxLhGroup.setIsProduction(YesOrNoEnum.NO.getValue());
            //递归：重新获取下一组
            productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
            return;
        }
        //计算需要排产的量
        SkuNeedProductionInfo needProductionInfo = getNeedProductionQty(productionPlanList, materialDesc);
        if (null == needProductionInfo) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuNoProductionQtyLog(context, groupName, cxMachineCode, materialDesc));
            return;
        }
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = productionContext.selectedDoubleMouldByRange(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuNoFindMouldLog(context, groupName, cxMachineCode, materialDesc));
            retrieveNextSku(context, needProductionInfo, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
            return;
        }
        //判断选择的Sku，能否进行上机排产-此时判断胎胚种类数、模壳、模具配比、胶囊卡盘
        MonthPlanProductionRequirePlanVo addSkuInfo = needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue());
        CxLhProductionHelper newLh = cxMachineInfo.getCorrectProductionDateRange(context, addSkuInfo, cxLhGroup, endDay, doubleMouldList);
        if (null == newLh) {
            retrieveNextSku(context, needProductionInfo, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
            return;
        }
        startDay = newLh.getProductionDay();
        endDay = newLh.getEndDay();
        String mouldInfo = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.joining(StringConstant.COMMA));
        log.info(TbrMouldProductionLogRecorder.addLhGroupSkuUsedFindMouldProductionLog(context, groupName, cxMachineCode, materialDesc, mouldInfo, startDay, endDay));
        Integer sumProductionQty = needProductionInfo.getSumNeedProductionQty();
        Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        Set<String> cxMachineInfoSet = new HashSet<>();
        cxMachineInfoSet.add(cxMachineCode);
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(productionPlanInfo, cxMachineInfoSet, cxLhGroup, sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //开始排产
        CxLhMouldProductionCalculator.lhProductionByCxMachineHandler(context, lhProductionQtyHelper, startDay, endDay, doubleMouldList, needProductionInfo.getNeedProductionList());
        //递归：重新获取下一组
        productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
    }

    /**
     * 校验基本排产条件
     * 1、groupPlanInfo存在排产计划
     * 2、groupPlanInfo本轮有能排产的计划
     * 3、groupPlanInfo中能找到收尾且可排产的硫化组
     * 4、收尾的硫化组startDay不能超出endDay
     * true 表示匹配 false表示不匹配
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组计划对象实例
     * @param excludeDays   需要排产的排产日
     * @return
     */
    private static EarliestConclusionLhGroupHelper checkBaseProductionCondition(Context context, ProductionPlanGroupInfo groupPlanInfo, Set<Integer> excludeDays) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = groupPlanInfo.getGroupName();
        String onLineMachineInfo = String.join(StringConstant.COMMA, groupPlanInfo.getAllocationCxMachineCodeSet());
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoPlanLog(context, groupName, onLineMachineInfo));
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProductionThisRound()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverHasProductionList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoProductionPlanLog(context, groupName, onLineMachineInfo));
            return null;
        }
        //获取最先收尾的硫化组
        EarliestConclusionLhGroupHelper lhGroup = groupPlanInfo.getEarliestConclusionLhInfo(productionContext, excludeDays);
        if (null == lhGroup) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineNoLhGroupLog(context, groupName, onLineMachineInfo));
            return null;
        }
        Integer startDay = lhGroup.getClosingDay();
        //成型分配的排产范围起始日~分组收尾日
        Integer endDay = lhGroup.getEndDay();
        log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueCxMachineLhGroupRangeLog(context, groupName, onLineMachineInfo, startDay, endDay));
        if (startDay > endDay) {
            return null;
        }
        return lhGroup;
    }

    /**
     * 从排产计划中挑选出在startDay~endDay能进行排产的sku计划
     * 1、挑选在startDay~endDay还可进行双模排产的sku
     * 2、有供应链优先字样的计划最优先
     * 3、其次考虑先高优先级再排产其它净需求
     * 4、库销比低的优先
     * 5、小于50条的优先
     * 6、净需求量大的优先
     * 7、如果挑选的sku与其它sku是共用模具，且是存在其它sku最后两副模具(即模具受限)
     * 则，需排产量小的优先
     *
     * @param productionContext  排产上下文
     * @param startDay           排产开始日
     * @param endDay             排产结束日
     * @param productionPlanList 排产计划
     * @return
     */
    private static String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay, List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return "";
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = productionPlanList.stream().filter(singlePlan -> singlePlan.hasProductionThisRound()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return "";
        }
        //提取所有sku的物料描述
        Set<String> allMaterialDescSet = hasProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> enableMaterialDescSet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
            return "";
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = hasProductionList.stream().filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enablePlanList)) {
            return "";
        }
        /**
         * 供应链标注"优先字样"最先排产
         * 先排产高优先级，在排产其它净需求
         * 模具产能约束 --> 库销比低优先 -> 小于50条 -> 净需求大
         * 模具产能约束，则取排产量小的
         *
         */
        List<MonthPlanProductionRequirePlanVo> hasPrioritizeList = enablePlanList.stream().filter(plan -> YesOrNoEnum.YES.getCode().equals(plan.getIsPrioritize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasPrioritizeList)) {
            hasPrioritizeList = enablePlanList;
        }
        //高优先级优先
        List<MonthPlanProductionRequirePlanVo> heightRequireList = hasPrioritizeList.stream().filter(plan -> plan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(heightRequireList)) {
            heightRequireList = hasPrioritizeList;
        }
        //库销比低的优先
        Double minInventorySalesRatio = heightRequireList.stream().mapToDouble(MonthPlanProductionRequirePlanVo::getInventorySalesRatio).min().getAsDouble();
        List<MonthPlanProductionRequirePlanVo> minInventorySalesRatioList = heightRequireList.stream().filter(plan -> minInventorySalesRatio.equals(plan.getInventorySalesRatio())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minInventorySalesRatioList)) {
            minInventorySalesRatioList = heightRequireList;
        }
        //小于50条的优先 productionContext.getBaseDataContainer().getParamConfiguration().getMinQty())
        List<MonthPlanProductionRequirePlanVo> lessMinQtyList = minInventorySalesRatioList.stream().filter(plan -> plan.isLess(plan.getMinProductionQty())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lessMinQtyList)) {
            lessMinQtyList = minInventorySalesRatioList;
        }
        //量大优先
        lessMinQtyList.sort(Comparator.comparing(MonthPlanProductionRequirePlanVo::getVirtualProductionQty, Comparator.reverseOrder()));
        String materialDesc = lessMinQtyList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        //是否共用模具受限？--最后两副
        Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(limitShareMouldSet)) {
            return materialDesc;
        }
        List<MonthPlanProductionRequirePlanVo> limitShareList = enablePlanList.stream().filter(plan -> limitShareMouldSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(limitShareList)) {
            return materialDesc;
        }
        //加入自己
        enablePlanList.forEach(plan -> {
            if (materialDesc.equals(plan.getMaterialDesc())) {
                limitShareList.add(plan);
            }
        });
        Map<String, Long> limitGroup = new HashMap<>();
        Map<String, List<MonthPlanProductionRequirePlanVo>> limitShareMap = limitShareList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        limitShareMap.forEach((limitMaterial, planList) -> limitGroup.put(limitMaterial, planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getVirtualProductionQty).sum()));
        Optional<Map.Entry<String, Long>> minEntry = limitGroup.entrySet().stream().min(Map.Entry.comparingByValue());
        return minEntry.get().getKey();
    }

    /**
     * 从分组计划中获取选中Sku(selectedMaterialDesc)还需排产量
     * 如果需整个排产，则为所有未排量，否则先排产高优级量
     *
     * @param productionPlanList   分组排产计划(TBR-结构名)
     * @param selectedMaterialDesc 选中的Sku
     * @return
     */
    private static SkuNeedProductionInfo getNeedProductionQty(List<MonthPlanProductionRequirePlanVo> productionPlanList, String selectedMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList) || StringUtils.isBlank(selectedMaterialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream().filter(plan -> plan.hasSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selectedPlanList)) {
            return null;
        }
        //需按净需求一起排产
        if (YesOrNoEnum.YES.getValue().equals(selectedPlanList.get(BigDecimal.ZERO.intValue()).getIsProductionBySum())) {
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, selectedPlanList);
        }
        //是否有供应链优先标记
        List<MonthPlanProductionRequirePlanVo> hasPrioritizeList = selectedPlanList.stream().filter(plan -> plan.hasPrioritizeQty()).collect(Collectors.toList());
        //供应链优先
        if (!CollectionUtils.isEmpty(hasPrioritizeList)) {
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, hasPrioritizeList);
        }
        //是否有高优级排产量
        List<MonthPlanProductionRequirePlanVo> heightList = selectedPlanList.stream().filter(plan -> plan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        //高优先级优先
        if (!CollectionUtils.isEmpty(heightList)) {
            return new SkuNeedProductionInfo(ProductionQtyModelEnum.HEIGHT_QTY, heightList);
        }
        return new SkuNeedProductionInfo(ProductionQtyModelEnum.NET_QTY, selectedPlanList);
    }

    /**
     * 当前选择的Sku不能正常上机，则重新获取下一组Sku
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组计划
     * @param currentSelected 当前选择的Sku信息
     * @param excludeDays     排除的收尾时间点
     */
    private static void retrieveNextSku(Context context, ProductionPlanGroupInfo groupPlanInfo, SkuNeedProductionInfo currentSelected, Set<Integer> excludeDays) {
        //没有模具则标记本轮不再参与
        List<MonthPlanProductionRequirePlanVo> needProductionList = currentSelected.getNeedProductionList();
        needProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
        //递归：重新获取下一组
        productionAddSkuByContinueCxMachine(context, groupPlanInfo, excludeDays);
    }

    /**
     * 当前选择的Sku不能正常上机，则重新获取下一组Sku
     *
     * @param context            排产上下文
     * @param currentSelected    当前选择的Sku信息
     * @param cxMachineCode      成型机台
     * @param productionPlanList 还需排产的计划
     * @param productionPlan     分组排产信息，包含分组名(TBR=结构名)、起始及理论收尾日期
     * @param mouldShellMap      模壳信息
     */
    private static void retrieveNextSku(Context context, SkuNeedProductionInfo currentSelected, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        //没有模具则标记本轮不再参与
        List<MonthPlanProductionRequirePlanVo> needProductionList = currentSelected.getNeedProductionList();
        needProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
        //递归：重新获取下一组
        productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
    }
}
