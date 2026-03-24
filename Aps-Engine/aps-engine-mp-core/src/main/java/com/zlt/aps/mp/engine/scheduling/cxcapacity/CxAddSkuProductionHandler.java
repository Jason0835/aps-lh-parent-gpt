package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.*;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionQtyModelEnum;
import com.zlt.aps.mp.engine.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.mp.engine.handler.SkuMouldSelector;
import com.zlt.aps.mp.engine.handler.SkuPrioritySelector;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
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
@Component
@RequiredArgsConstructor
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
    public void productionAddSkuByContinueCxMachine(Context context, ProductionPlanGroupInfo groupPlanInfo, Set<Integer> excludeDays) {
        //基础校验 有可排产计划且能找到最早收尾的硫化组
        EarliestConclusionLhGroupHelper lhGroup = checkBaseProductionCondition(context, groupPlanInfo, excludeDays);
        if (null == lhGroup) {
            return;
        }
        String groupName = groupPlanInfo.getGroupName();
        String onLineMachineInfo = String.join(StringConstant.COMMA, groupPlanInfo.getAllocationCxMachineCodeSet());
        //成型分配的排产范围起始日~分组收尾日
        Integer startDay = lhGroup.getClosingDay();
        Integer endDay = lhGroup.getEndDay();
        TbrMouldProductionLogRecorder.addGroupFindLhMachineRangeLog(context, groupName, onLineMachineInfo, startDay, endDay);
        //提取结构内可排产的Sku信息
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProductionThisRound()).collect(Collectors.toList());
        BeforeSkuProductionInfo beforeSkuInfo = lhGroup.getBeforeSkuInfo();
        //获取优先级最高的Sku信息
        String materialDesc = getSelectedAddSku(productionContext, startDay, endDay, leftOverHasProductionList);
        log.info(TbrMouldProductionLogRecorder.addContinueGroupLhGroupFindSkuLog(context, groupName, onLineMachineInfo, materialDesc));
        if (StringUtils.isBlank(materialDesc)) {
            //20260113 剔除需要排除的收尾时间点
            excludeDays.add(startDay);
            groupPlanInfo.afterProductionResetThisRound();
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
        //是否最后一个Sku
        boolean isLastSkuPlan = isLastSkuPlan(leftOverHasProductionList, materialDesc);
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = SkuMouldSelector.selectedDoubleMouldByRange(productionContext, materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addContinueLhGroupSkuNoFindMouldLog(context, groupName, onLineMachineInfo, materialDesc));
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.FIND_MOULD_LIMIT);
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays, isLastSkuPlan, startDay);
            return;
        }
        //重新确认排产时间范围-再次修正排产范围
        MonthPlanProductionRequirePlanVo addSkuInfo = needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue());
        groupPlanInfo.correctProductionDateRange(context, addSkuInfo, lhGroup, doubleMouldList, onLineMachineInfo);
        Integer newStartDay = lhGroup.getClosingDay();
        endDay = lhGroup.getEndDay();
        log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueMachineCorrectLhGroupRangeLog(context, groupName, onLineMachineInfo, startDay, endDay));
        if (null == newStartDay || null == endDay || !startDay.equals(newStartDay)) {
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays, isLastSkuPlan, startDay);
            return;
        }
        //20260129 修正前排产Sku信息，可能因为模具排产日
        correctBeforeSku(context, lhGroup, doubleMouldList, groupName, startDay);
        Integer sumProductionQty = needProductionInfo.getSumNeedProductionQty();
        Integer dayMaxProductionQty = needProductionInfo.getDayMaxProductionQty();
        //实际排产量
        Integer realSumProductionQty = BigDecimal.ZERO.intValue();
        LhProductionQtyHelper lhProductionQtyHelper = new LhProductionQtyHelper(groupPlanInfo, groupPlanInfo.getAllocationCxMachineCodeSet(), lhGroup.transformCxLhGroup(), sumProductionQty, realSumProductionQty, dayMaxProductionQty);
        //开始排产
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, doubleMouldList, needProductionInfo.getNeedProductionList(), ContinueTypeEnum.NO_CONTINUE);
        //递归：重新获取下一组
        Integer productionQty = lhProductionQtyHelper.getRealSumProductionQty();
        if (productionQty > BigDecimal.ZERO.intValue()) {
            addChangeMouldInfo(productionContext, addSkuInfo, startDay, beforeSkuInfo, doubleMouldList);
            groupPlanInfo.afterProductionResetThisRound();
        }
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
    public void productionAddSku(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap, Set<Integer> excludeDays) {
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
        CxLhProductionHelper cxLhGroup = cxMachineInfo.getEarliestConclusionLhGroup(excludeDays);
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
            productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap, excludeDays);
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
        List<ProductionMouldInfoVo> doubleMouldList = SkuMouldSelector.selectedDoubleMouldByRange(productionContext, materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuNoFindMouldLog(context, groupName, cxMachineCode, materialDesc));
            retrieveNextSku(context, needProductionInfo, cxMachineCode, productionPlanList, productionPlan, mouldShellMap, excludeDays);
            return;
        }
        //判断选择的Sku，能否进行上机排产-此时判断胎胚种类数、模壳、模具配比、胶囊卡盘
        MonthPlanProductionRequirePlanVo addSkuInfo = needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue());
        CxLhProductionHelper newLh = cxMachineInfo.getCorrectProductionDateRange(context, addSkuInfo, cxLhGroup, endDay, doubleMouldList);
        if (null == newLh) {
            retrieveNextSku(context, needProductionInfo, cxMachineCode, productionPlanList, productionPlan, mouldShellMap, excludeDays);
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
        Integer productionQty = lhProductionQtyHelper.getRealSumProductionQty();
        if (productionQty > BigDecimal.ZERO.intValue()) {
            addChangeMouldInfo(productionContext, addSkuInfo, startDay, cxLhGroup.getBeforeSku(), doubleMouldList);
            productionPlanInfo.afterProductionResetThisRound();
        }
        productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap, excludeDays);
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
    private EarliestConclusionLhGroupHelper checkBaseProductionCondition(Context context, ProductionPlanGroupInfo groupPlanInfo, Set<Integer> excludeDays) {
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
    private String getSelectedAddSku(TbrProductionContext productionContext, Integer startDay, Integer endDay, List<MonthPlanProductionRequirePlanVo> productionPlanList) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return StringUtils.EMPTY;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = productionPlanList.stream().filter(singlePlan -> singlePlan.hasProductionThisRound()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return StringUtils.EMPTY;
        }
        //提取所有sku的物料描述
        Set<String> allMaterialDescSet = hasProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> enableMaterialDescSet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, allMaterialDescSet, startDay, endDay);
        if (CollectionUtils.isEmpty(enableMaterialDescSet)) {
            return StringUtils.EMPTY;
        }
        List<MonthPlanProductionRequirePlanVo> enablePlanList = hasProductionList.stream().filter(plan -> enableMaterialDescSet.contains(plan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enablePlanList)) {
            return StringUtils.EMPTY;
        }
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuPlanMap = enablePlanList.stream().filter(item -> StringUtils.isNotBlank(item.getMaterialDesc())).collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        /**
         * 供应链标注"优先字样"最先排产
         * 先排产高优先级，在排产其它净需求
         * 模具产能约束 --> 库销比低优先 -> 小于50条 -> 净需求大
         * 模具产能约束，则取排产量小的
         *
         */
        // 3. 选择最高优先级SKU
        Optional<String> highestPrioritySku = SkuPrioritySelector.selectHighestPrioritySku(skuPlanMap, productionContext, startDay, endDay);
        if (highestPrioritySku.isPresent()) {
            String highestPriorityMaterialDesc = highestPrioritySku.get();
            log.info("最高优先级SKU:{} ", highestPriorityMaterialDesc);
            return highestPriorityMaterialDesc;
        } else {
            log.info("没有找到符合条件的SKU");
            return StringUtils.EMPTY;
        }
    }

    /**
     * 从分组计划中获取选中Sku(selectedMaterialDesc)还需排产量
     * 如果需整个排产，则为所有未排量，否则先排产高优级量
     *
     * @param productionPlanList   分组排产计划(TBR-结构名)
     * @param selectedMaterialDesc 选中的Sku
     * @return
     */
    private SkuNeedProductionInfo getNeedProductionQty(List<MonthPlanProductionRequirePlanVo> productionPlanList, String selectedMaterialDesc) {
        if (CollectionUtils.isEmpty(productionPlanList) || StringUtils.isBlank(selectedMaterialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> selectedPlanList = productionPlanList.stream().filter(plan -> plan.hasThisRoundSelectedProduction(selectedMaterialDesc)).collect(Collectors.toList());
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
     * 当前选择的Sku是否最后一个
     *
     * @param leftOverHasProductionList 还有需求量的Sku集合
     * @param selectedSkuMaterialDesc   当前选中的Sku
     * @return
     */
    private boolean isLastSkuPlan(List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList, String selectedSkuMaterialDesc) {
        if (StringUtils.isBlank(selectedSkuMaterialDesc)) {
            return false;
        }
        if (CollectionUtils.isEmpty(leftOverHasProductionList)) {
            return true;
        }
        List<MonthPlanProductionRequirePlanVo> otherList = leftOverHasProductionList.stream().filter(singlePlan -> !selectedSkuMaterialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(otherList)) {
            return true;
        }
        return false;
    }

    /**
     * 当前选择的Sku不能正常上机，则重新获取下一组Sku
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组计划
     * @param currentSelected 当前选择的Sku信息
     * @param excludeDays     排除的收尾时间点
     * @param isLastSkuPlan   是否最后一个Sku
     */
    private void retrieveNextSku(Context context, ProductionPlanGroupInfo groupPlanInfo, SkuNeedProductionInfo currentSelected, Set<Integer> excludeDays, boolean isLastSkuPlan, Integer startDay) {
        //没有模具则标记本轮不再参与
        List<MonthPlanProductionRequirePlanVo> needProductionList = currentSelected.getNeedProductionList();
        if (!isLastSkuPlan) {
            needProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
            //递归：重新获取下一组
            productionAddSkuByContinueCxMachine(context, groupPlanInfo, excludeDays);
            return;
        }
        //最后一组Sku的处理 加入剔除的收尾的时间点
        excludeDays.add(startDay);
        groupPlanInfo.afterProductionResetThisRound();
        //递归：重新下一轮
        productionAddSkuByContinueCxMachine(context, groupPlanInfo, excludeDays);
        return;
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
    private void retrieveNextSku(Context context, SkuNeedProductionInfo currentSelected, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap, Set<Integer> excludeDays) {
        //没有模具则标记本轮不再参与
        List<MonthPlanProductionRequirePlanVo> needProductionList = currentSelected.getNeedProductionList();
        needProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
        //递归：重新获取下一组
        productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap, excludeDays);
    }

    /**
     * 修正当前排产的Sku信息
     *
     * @param context         排产上下文
     * @param lhGroup         收尾硫化组
     * @param doubleMouldList 排产模具
     * @param groupName       分组名-TBR 结构
     * @param startDay        起始排产日
     */
    private void correctBeforeSku(Context context, EarliestConclusionLhGroupHelper lhGroup, List<ProductionMouldInfoVo> doubleMouldList, String groupName, Integer startDay) {
        if (null == lhGroup || CollectionUtils.isEmpty(doubleMouldList) || StringUtils.isBlank(groupName) || null == startDay) {
            return;
        }
        List<Integer> productionQtyList = new ArrayList<>();
        Set<String> materialDescSet = new HashSet<>();
        doubleMouldList.forEach(singleMould -> {
            Map<Integer, List<CxMouldDayProductionHelper>> allDayProductionInfo = singleMould.getDayProductionInfo();
            if (CollectionUtils.isEmpty(allDayProductionInfo)) {
                return;
            }
            boolean isAddProductionQty = true;
            List<CxMouldDayProductionHelper> dayProductionList = allDayProductionInfo.get(startDay);
            if (CollectionUtils.isEmpty(dayProductionList)) {
                isAddProductionQty = false;
                Integer previousDay = context.getPreviousDay(startDay);
                if (null == previousDay) {
                    return;
                }
                dayProductionList = allDayProductionInfo.get(previousDay);
            }
            if (CollectionUtils.isEmpty(dayProductionList)) {
                return;
            }
            CxMouldDayProductionHelper lastProduction = dayProductionList.get(dayProductionList.size() - BigDecimal.ONE.intValue());
            if (!lastProduction.getStructureName().equals(groupName)) {
                return;
            }
            materialDescSet.add(lastProduction.getMaterialDesc());
            List<CxMouldDayProductionHelper> materialAllProductionInfo = dayProductionList.stream().filter(single -> single.getMaterialDesc().equals(lastProduction.getMaterialDesc())).collect(Collectors.toList());
            if (isAddProductionQty) {
                Integer sumProductionQty = materialAllProductionInfo.stream().mapToInt(CxMouldDayProductionHelper::getProductionQty).sum();
                productionQtyList.add(sumProductionQty);
            }
        });
        if (CollectionUtils.isEmpty(materialDescSet) || materialDescSet.size() > BigDecimal.ONE.intValue()) {
            return;
        }
        String materialDesc = materialDescSet.stream().findFirst().orElse(null);
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanProductionRequirePlanVo> planList = productionContext.getAllSkuProductionPlan().get(materialDesc);
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }
        MonthPlanProductionRequirePlanVo planInfo = planList.get(BigDecimal.ZERO.intValue());
        Integer dayLhQty = planInfo.getMaxDaySingleLhMachineQty();
        Integer productionQty = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(productionQtyList)) {
            productionQty = productionQtyList.stream().mapToInt(Integer::intValue).sum();
        }
        lhGroup.updateBeforeSkuInfo(materialDesc, planInfo.getMaterialCode(), productionQty, dayLhQty);
    }

    /**
     * 换模次数+1处理
     *
     * @param productionContext 排产上下文
     * @param addSkuInfo        排产Sku
     * @param startDay          换模日
     * @param beforeSku         前Sku信息
     * @param doubleMouldList   排产模具
     */
    private void addChangeMouldInfo(TbrProductionContext productionContext, MonthPlanProductionRequirePlanVo addSkuInfo, Integer startDay, BeforeSkuProductionInfo beforeSku, List<ProductionMouldInfoVo> doubleMouldList) {
        ChangeMouldInfo changeMouldInfo = ChangeMouldInfo.buildChangeMouldInfo(productionContext, addSkuInfo, beforeSku, beforeSku);
        boolean isChangeMould = changeMouldInfo.isChangeMould();
        if (!isChangeMould) {
            return;
        }
        //需要换模-换模次数处理
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Integer changeMouldDay = startDay;
        Set<String> mouldCodeSet = doubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, changeMouldDay, addSkuInfo.getMaterialDesc(), mouldCodeSet);
    }

    /**
     * 设置剩余的每日硫化机台数
     *
     * @param context
     * @param allGroupPlanInfo
     * @param currentStructName
     */
    public void setRemainLhMachineCount(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, String currentStructName) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer endDay = productionContext.getMonthDays();
        GroupPlanCxLhCapacityLimitHelper capacityLimitHelper;
        ProductionPlanGroupInfo groupPlan;
        int accUsedLhMachines;
        // 1. 获取总的硫化机台数
        Integer totalLhMachines = productionContext.getBaseDataContainer().getLhMachineInfoList().size();
        // 2. 按日更新 结构下每日剩余可用的硫化机台数
        for (int i = ProductionConstant.MONTH_START_DAY; i <= endDay; i++) {
            // 更新当前结构的 剩余可使用的硫化机台
            groupPlan = allGroupPlanInfo.get(currentStructName);
            if (groupPlan == null) {
                continue;
            }

            // 获取其他结构已使用的硫化机台数
            accUsedLhMachines = getOtherStructUsedLhMachines(allGroupPlanInfo, currentStructName, i);
            // 是更新每日剩余可用的硫化机台数
            if (PubUtil.isEmpty(groupPlan.getDayProductionLimitInfo())) {
                continue;
            }
            capacityLimitHelper = groupPlan.getDayProductionLimitInfo().get(i);
            if (capacityLimitHelper == null) {
                continue;
            }
            capacityLimitHelper.updateRemainMaxLhMachines(totalLhMachines - accUsedLhMachines);
        }
    }

    /**
     * 获取其他结构已使用的硫化机台数
     *
     * @param allGroupPlanInfo  所有结构计划
     * @param currentStructName 当前结构名称
     * @param iDay              当前日
     * @return 其他结构已使用的硫化机台数
     */
    private Integer getOtherStructUsedLhMachines(Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, String currentStructName, int iDay) {
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        ProductionPlanGroupInfo groupPlan;
        int accUsedLhMachines = 0;
        for (Map.Entry<String, ProductionPlanGroupInfo> entry : allGroupPlanInfo.entrySet()) {
            if (entry.getKey().equals(currentStructName)) {
                //排除当前结构
                continue;
            }
            groupPlan = entry.getValue();
            Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = groupPlan.getDailyCapacityLimitVoMap();
            if (PubUtil.isEmpty(dailyCapacityLimitVoMap)) {
                continue;
            }
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(iDay);
            if (dailyCapacityLimitVo == null) {
                continue;
            }
            accUsedLhMachines += dailyCapacityLimitVo.getUsedLhMachines();
        }
        return accUsedLhMachines;
    }
}
