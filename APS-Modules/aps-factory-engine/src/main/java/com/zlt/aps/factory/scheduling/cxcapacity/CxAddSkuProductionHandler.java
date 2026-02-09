package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.MouldProductionLimitTypeEnum;
import com.zlt.aps.factory.daylimit.MouldShellBaseInfoVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.factory.enums.ProductionQtyModelEnum;
import com.zlt.aps.factory.handler.CxLhMouldProductionCalculator;
import com.zlt.aps.factory.handler.SkuPrioritySelector;
import com.zlt.aps.factory.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
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
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = groupPlanInfo.getGroupName();
        String onLineMachineInfo = String.join(StringConstant.COMMA, groupPlanInfo.getAllocationCxMachineCodeSet());
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
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
            productionContext.addSkuProductionLimitInfo(materialDesc, MouldProductionLimitTypeEnum.FIND_MOULD_LIMIT);
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays);
            return;
        }
        //重新确认排产时间范围-再次修正排产范围
        groupPlanInfo.correctProductionDateRange(context, needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue()), lhGroup, doubleMouldList, onLineMachineInfo);
        startDay = lhGroup.getClosingDay();
        endDay = lhGroup.getEndDay();
        log.info(TbrMouldProductionLogRecorder.addContinueGroupContinueMachineCorrectLhGroupRangeLog(context, groupName, onLineMachineInfo, startDay, endDay));
        if (null == startDay || null == endDay) {
            retrieveNextSku(context, groupPlanInfo, needProductionInfo, excludeDays);
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
        CxLhMouldProductionCalculator.lhProductionByGroupHandler(context, lhProductionQtyHelper, startDay, endDay, doubleMouldList, needProductionInfo.getNeedProductionList(), false);
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
    public void productionAddSku(Context context, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
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
        // 根据结构特殊材料情况重算结束日期
        endDay = caculateEndDayBySpecialMaterial(startDay, endDay, productionPlanList, productionContext, productionPlanInfo);

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
     * 根据结构特殊材料情况重算结束日期
     *
     * @param startDay
     * @param endDay
     * @param productionPlanList
     * @param productionContext
     * @param productionPlanInfo
     * @return
     */
    private Integer caculateEndDayBySpecialMaterial(Integer startDay, Integer endDay,
                                                    List<MonthPlanProductionRequirePlanVo> productionPlanList,
                                                    TbrProductionContext productionContext,
                                                    ProductionPlanGroupInfo productionPlanInfo) {
        if (startDay > endDay) {
            return endDay;
        }
        if (endDay == productionContext.getProductionEndDay()) { // 如果是本月排产最后一天，直接跳过，不需要拉量或者舍弃
            return endDay;
        }
        // 判断如果是特殊结构，需要判断是否最后一个结构
        Map<String, BigDecimal> materialMap = productionPlanInfo.getEmbryoSpecialMaterialInfoMap(); // 本结构涉及的特殊材料清单
        if (CollectionUtils.isEmpty(materialMap)) { // 非特殊结构直接跳过
            return endDay;
        }
        // 取出与本结构使用相同特殊材料的结构排产
        List<ProductionPlanGroupInfo> specialPlanList = productionContext.getGroupProductionInfo().values().stream()
                .filter(plan -> { // 过滤使用相同特殊材料的结构
                    if (plan == productionPlanInfo) { // 检查本结构之外的特殊结构
                        return false;
                    }
                    Map<String, BigDecimal> otherMaterialMap = plan.getEmbryoSpecialMaterialInfoMap(); // 涉及的特殊材料清单
                    if (CollectionUtils.isEmpty(otherMaterialMap)) {
                        return false;
                    }
                    return materialMap.keySet().stream().anyMatch(material -> otherMaterialMap.containsKey(material)); // 特殊材料与新增结构的特殊材料清单有交集
                }).collect(Collectors.toList());
        if (specialPlanList.stream().anyMatch(plan -> plan.getLeftOverNeedAllocationDays() > 0)) { // 其他特殊规格有任意一个没有排完，说明还不是最后一个结构，跳过
            return endDay;
        }
        productionPlanInfo.setIsLatestSpecialMaterial(true); // 打上最后一个规格的标记
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext
                .getSpecialMaterialInfoMap(); // 特殊材料库存列表
        Integer limitProductionQty = caculateLimitProductionQtyByStock(materialMap, specialMaterialInfoMap); // 特殊材料库存的可生产上限
        if (limitProductionQty <= 0) { // 可生产上限不足，则不能排产
            return startDay - 1;
        }
//        Integer realProductionQty = caculateRealProductionQtyBySpecialMaterial(productionPlanInfo,
//                specialMaterialInfoMap, materialMap); // 根据特殊材料计算实际可排产量
        // 判断同特殊材料排排产量落在哪个区间：
        // 1、计划量*单号模除标准长度，如果余数小于日硫化量 * 配比*单耗：舍弃余数部分
        // 2、计划量*单号模除标准长度，如果余数大于等于日硫化量 * 配比*单耗：计划量补标准长度 - 日硫化量 * 配比*单耗
        Integer allocationQty = specialPlanList.stream()
                .mapToInt(plan -> plan.getMinLhDayCapacityQty() * plan.getMinLhMachineCount() * plan.getTheoryDays())
                .sum(); // 统计已排量 = 日硫化量 * 配比 * 已排天数
        Integer sumPlanQty = allocationQty + productionPlanInfo.getSumPlanQty(); // 同特殊材料结构总预计排产量
        // 取出各结构的特殊材料清单交集
        Map<String, BigDecimal> specialIntersectionMap = new HashMap<>();
        specialIntersectionMap.putAll(materialMap);
        for (ProductionPlanGroupInfo plan : specialPlanList) {
            plan.getEmbryoSpecialMaterialInfoMap().keySet().stream().forEach(materialCode -> {
                if (!specialIntersectionMap.containsKey(materialCode)) {
                    specialIntersectionMap.remove(materialCode);
                }
            });
        }
        if (CollectionUtils.isEmpty(specialIntersectionMap)) { // 都没有交集，直接重置未本结构的物料清单
            specialIntersectionMap.putAll(materialMap);
        }
        Entry<String, BigDecimal> entry = specialIntersectionMap.entrySet().stream().findFirst().get();
        BigDecimal unitConsumeQty = entry.getValue(); // 单耗
        Long standardLength = specialMaterialInfoMap.get(entry.getKey()).keySet().stream().findFirst().get(); // 标准长度
        BigDecimal remainderQty = BigDecimalUtils.multiply(sumPlanQty, unitConsumeQty)
                .remainder(BigDecimalUtils.valueOf(standardLength)); // 计算余数
        // 区间阈值 = 硫化量 * 配比*单耗
        BigDecimal threshold = BigDecimalUtils.multiply(productionPlanInfo.getMinLhDayCapacityQty(),
                productionPlanInfo.getMinLhMachineCount(), unitConsumeQty);
        boolean isAddQty = false;
        Integer productionQty = productionPlanInfo.getSumPlanQty();
        Integer realProductionQty = 0; // 重算实际的量
        if (remainderQty.compareTo(threshold) >= 0) { // 超过阈值，尝试补量
            if (limitProductionQty >= productionQty + standardLength - threshold.intValue()) {
                // 检查补量后不超过可生产上限才进行补量
                isAddQty = true;
                realProductionQty = (int) (productionQty + standardLength - threshold.intValue());
            }
        }
        if (!isAddQty) { // 不补量，则需要将计划量扣减掉余数部分
            realProductionQty = productionQty - threshold.intValue();
        }
        if (realProductionQty <= 0) { // 可生产上限不足，则不能排产
            return startDay - 1;
        }
        // 计算排产天数 = ceil(计划量 / 日硫化量 / 配比)
        BigDecimal theoryDays = BigDecimalUtils.div(realProductionQty, BigDecimalUtils
                        .multiply(productionPlanInfo.getMinLhDayCapacityQty(), productionPlanInfo.getMinLhMachineCount(), true),
                2, false);
        theoryDays = theoryDays.setScale(0, RoundingMode.UP);
        return startDay + theoryDays.intValue();
    }

    /**
     * 计算特殊材料库存的最大可排产量
     *
     * @param materialMap            特殊材料用量清单
     * @param specialMaterialInfoMap 特殊材料库存
     * @return
     */
    private Integer caculateLimitProductionQtyByStock(Map<String, BigDecimal> materialMap,
                                                      Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
        // 根据各材料的库存使用情况限制排产量
        Integer limitProductionQty = null;
        for (Entry<String, BigDecimal> entry : materialMap.entrySet()) {
            // 预估本结构的用量
            String materialCode = entry.getKey(); // 特殊材料物料
            BigDecimal unitConsumeQty = entry.getValue(); // 单胎消耗量
            Map<Long, SpecialMaterialInfoVo> specialMaterialinfo = specialMaterialInfoMap.get(materialCode); // 取出各标准用量的特殊材料库存
            if (specialMaterialinfo == null) {
                limitProductionQty = 0;
                break;
            }
            // 累计可用库存
            Long totalStock = specialMaterialinfo.values().stream()
                    .mapToLong(s -> s.getStock() - s.getSumProductionQty()).sum();
            if (totalStock <= 0) {
                limitProductionQty = 0;
                break;
            }
            // 换算成成品数
            Integer stockCanProductionQty = BigDecimalUtils.div(totalStock, unitConsumeQty, 2)
                    .setScale(0, RoundingMode.DOWN).intValue();
            if (limitProductionQty == null) { // 如果未分配量还有剩余，需要更新可排产量
                limitProductionQty = stockCanProductionQty;
            } else {
                limitProductionQty = Math.min(limitProductionQty, stockCanProductionQty);
            }
        }
        return limitProductionQty;
    }

    /**
     * 计算特殊材料库存的最大可排产量
     *
     * @param productionPlanInfo
     * @param specialMaterialInfoMap
     * @param materialMap
     * @return
     */
    private Integer caculateRealProductionQtyBySpecialMaterial(ProductionPlanGroupInfo productionPlanInfo,
                                                               Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap,
                                                               Map<String, BigDecimal> materialMap) {
        // 复制可用库存，用于计算预估值
        Map<String, Map<Long, SpecialMaterialInfoVo>> avaliableStockMap = specialMaterialInfoMap.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> {
                    Map<Long, SpecialMaterialInfoVo> stockMap = entry.getValue();
                    return stockMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, stockEntry -> {
                        SpecialMaterialInfoVo oldStock = stockEntry.getValue();
                        SpecialMaterialInfoVo copyStock = new SpecialMaterialInfoVo();
                        copyStock.setStock(oldStock.getStock());
                        copyStock.setSumProductionQty(oldStock.getSumProductionQty());
                        return copyStock;
                    }));
                }));

        // 根据本结构最大需求量预计排产量
        Integer productionQty = productionPlanInfo.getSumPlanQty();

        // 根据各材料的库存使用情况限制排产量
        // 例如结构需要两个特殊材料，特殊材料1库存可生产100，特殊材料2库存只能生产80，则按80算预计生产天数
        Integer realProductionQty = productionQty;
        for (Entry<String, BigDecimal> entry : materialMap.entrySet()) {
            // 预估本结构的用量
            String materialCode = entry.getKey(); // 特殊材料物料
            BigDecimal unitConsumeQty = entry.getValue(); // 单胎消耗量
            Long materialConsumeQty = BigDecimalUtils.multiply(unitConsumeQty, realProductionQty, true).longValue(); // 总消耗量
            Long unAllocationQty = materialConsumeQty; // 待分配量
            Map<Long, SpecialMaterialInfoVo> specialMaterialinfo = avaliableStockMap.get(materialCode); // 取出各标准用量的特殊材料库存
            List<SpecialMaterialInfoVo> stockList = specialMaterialinfo.values().stream()
                    .filter(s -> s.getSumProductionQty() < s.getStock()) // 取出库存还有剩余的库存信息
                    .sorted((s1, s2) -> {
                        // 第一顺位：从已排量最大的开始分配
                        Long sumProductionQty1 = s1.getSumProductionQty();
                        Long sumProductionQty2 = s2.getSumProductionQty();
                        int result = sumProductionQty2.compareTo(sumProductionQty1);
                        if (result != 0) {
                            return result;
                        }
                        // 第二顺位：从剩余库存大于本结构用量且最接近的开始分配：abs(库存-已分配-需求量)
                        Boolean isEnoughStock1 = s1.getStock() - sumProductionQty1 > materialConsumeQty;
                        Boolean isEnoughStock2 = s2.getStock() - sumProductionQty2 > materialConsumeQty;
                        if (!isEnoughStock1 || !isEnoughStock2) { // 任意一个可用库存小于结构用量，都结束，且优先使用大于结构用量的
                            return isEnoughStock2.compareTo(isEnoughStock1);
                        }
                        Long remainQty1 = s1.getStock() - sumProductionQty1 - materialConsumeQty;
                        Long remainQty2 = s2.getStock() - sumProductionQty2 - materialConsumeQty;
                        result = remainQty1.compareTo(remainQty2);
                        return result;
                    }).collect(Collectors.toList());
            for (SpecialMaterialInfoVo stockInfo : stockList) {
                Long sumProductionQty = stockInfo.getSumProductionQty(); // 已排量
                Long avaliableStockQty = stockInfo.getStock() - sumProductionQty; // 可用库存库存
                Long consumeQty = Math.min(unAllocationQty, avaliableStockQty); // 消耗量，取
                stockInfo.setSumProductionQty(sumProductionQty + consumeQty); // 更新已排量
                unAllocationQty = unAllocationQty - consumeQty; // 分配库存给消耗量
            }
            if (unAllocationQty > 0) { // 如果未分配量还有剩余，需要更新可排产量
                // 待分配量换算成排产量后从实际排产量中扣减，向上取整
                realProductionQty = BigDecimalUtils.div(materialConsumeQty - unAllocationQty, unitConsumeQty, 2)
                        .setScale(0, RoundingMode.DOWN).intValue();
            }
        }
        return realProductionQty;
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
     * 当前选择的Sku不能正常上机，则重新获取下一组Sku
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组计划
     * @param currentSelected 当前选择的Sku信息
     * @param excludeDays     排除的收尾时间点
     */
    private void retrieveNextSku(Context context, ProductionPlanGroupInfo groupPlanInfo, SkuNeedProductionInfo currentSelected, Set<Integer> excludeDays) {
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
    private void retrieveNextSku(Context context, SkuNeedProductionInfo currentSelected, String cxMachineCode, List<MonthPlanProductionRequirePlanVo> productionPlanList, CxMachineAllocationPlanHelper productionPlan, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        //没有模具则标记本轮不再参与
        List<MonthPlanProductionRequirePlanVo> needProductionList = currentSelected.getNeedProductionList();
        needProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.NO.getValue()));
        //递归：重新获取下一组
        productionAddSku(context, cxMachineCode, productionPlanList, productionPlan, mouldShellMap);
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
}
