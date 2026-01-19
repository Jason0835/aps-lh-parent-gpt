package com.zlt.aps.factory.handler;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.cxcapacity.TbrProductionGroupLogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 续作Sku计算器
 *
 * @author ZLT
 * @date 20251227
 */
@Slf4j
public class ContinueSkuCalculator {

    /**
     * 设置续作sku的计划量及单模日硫化产能(先暂定高优先级)
     * 根据分组计划信息及对应分组的续作Sku信息
     * 统计已经有计划量的Sku信息对应的胎胚信息
     * 和使用模具数信息
     * 并设置到groupContinueInfo对象中
     * 取高优先级量还是总净需求量？
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划-TBR为结构
     * @param groupContinueInfo 分组对应的续作信息-TBR为结构
     */
    public static void setContinueSkuPlanDemandQty(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        String groupName = groupContinueInfo.getGroupName();
        List<MonthPlanProductionRequirePlanVo> groupPlanList = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addContinueGroupNoGroupPlanLog(context, groupName));
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuMouldNumberMap)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addContinueGroupNoContinueSkuLog(context, groupName));
            return;
        }
        //20260119 初始设置：续作在产机台信息，可能续作本身没有计划(高优先级没有量或是计划没有量)
        continueSkuMouldNumberMap.forEach((materialDesc, cxContinueSkuInfo) -> cxContinueSkuInfo.setOnLineCxMachineSet(groupContinueInfo.getCxMachineCodeSet()));
        //提取续作Sku计划
        Set<String> skuMaterialDescSet = continueSkuMouldNumberMap.keySet();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = groupPlanList.stream().filter(groupPlan -> groupPlan.hasProduction() && skuMaterialDescSet.contains(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addContinueGroupContinueSkuEmptyPlanLog(context, groupName));
            return;
        }
        //分组合计续作Sku的计划量-高优先级
        Map<String, List<MonthPlanProductionRequirePlanVo>> continueSkuGroupMap = continueSkuPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        //设置续作Sku的高优级量及单模日硫化量
        continueSkuMouldNumberMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            List<MonthPlanProductionRequirePlanVo> planList = continueSkuGroupMap.get(materialDesc);
            Integer planDemandQty = getContinueSkuSummaryQty(planList);
            cxContinueSkuInfo.setPlanDemandQty(planDemandQty);
            cxContinueSkuInfo.setContinueSkuPlanList(planList);
            if (CollectionUtils.isEmpty(planList)) {
                log.info(TbrProductionGroupLogRecorder.addContinueGroupContinueSkuNoPlanLog(context, groupName, materialDesc));
                return;
            }
            MonthPlanProductionRequirePlanVo plan = planList.get(BigDecimal.ZERO.intValue());
            cxContinueSkuInfo.setDayVulcanizationQty(plan.getDayVulcanizationQty());
        });
        //提取对应续作Sku有高优先级量的胎胚信息
        List<CxContinueSkuInfoHelper> continueSkuInfoList = continueSkuMouldNumberMap.values().stream().collect(Collectors.toList());
        List<CxContinueSkuInfoHelper> hasPlanDemandQtyList = continueSkuInfoList.stream().filter(continueSkuInfo -> continueSkuInfo.getPlanDemandQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasPlanDemandQtyList)) {
            return;
        }
        Set<String> effectiveEmbryoSet = hasPlanDemandQtyList.stream().map(CxContinueSkuInfoHelper::getEmbryoCode).collect(Collectors.toSet());
        groupContinueInfo.setContinueEffectiveEmbryoSet(effectiveEmbryoSet);
        Integer effectiveMouldNumber = hasPlanDemandQtyList.stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        groupContinueInfo.setContinueEffectiveMouldNumber(effectiveMouldNumber);
    }

    /**
     * 根据在产成型机台，设置分组计划的成型硫化产能限制信息
     * 及成型硫化组--集合
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划对象
     * @param groupContinueInfo 分组计划对应的续作信息
     */
    public static void initContinueCxMachineLimit(Context context, ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        if (null == groupContinueInfo || null == groupPlanInfo) {
            //todo 记录日志
            return;
        }
        List<ProductGroupCxCapacityInfo> cxCapacityInfoList = groupContinueInfo.getCxCapacityInfoList();
        if (CollectionUtils.isEmpty(cxCapacityInfoList)) {
            //todo 记录日志
            return;
        }
        Set<String> cxMachineInfo = groupContinueInfo.getCxMachineCodeSet();
        String groupName = groupPlanInfo.getGroupName();
        Integer maxEmbryoCodeCount = cxCapacityInfoList.stream().mapToInt(ProductGroupCxCapacityInfo::getMaxEmbryoCodeCount).sum();
        Integer maxLhMachineCount = cxCapacityInfoList.stream().mapToInt(ProductGroupCxCapacityInfo::getMaxLhMachineCount).sum();
        Map<String, Integer> minLhMachineInfo = cxCapacityInfoList.stream().collect(Collectors.toMap(ProductGroupCxCapacityInfo::getCxMachineCode, ProductGroupCxCapacityInfo::getMinLhMachineCount));
        Integer maxDays = context.getMonthDays();
        Set<Integer> stopDays = context.getStopDays();
        //初始限制设置--随着续作Sku排产及后续排产会进行更新变化
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> limitMap = new HashMap<>();
        for (int day = ProductionConstant.MONTH_START_DAY; day <= maxDays; day++) {
            if (stopDays.contains(day)) {
                continue;
            }
            GroupPlanCxLhCapacityLimitHelper limitHelper = GroupPlanCxLhCapacityLimitHelper.buildEmptyData(day, maxEmbryoCodeCount, maxLhMachineCount);
            limitHelper.getMinLhMachineInfo().putAll(minLhMachineInfo);
            limitMap.put(day, limitHelper);
        }
        groupPlanInfo.setDayProductionLimitInfo(limitMap);
        //硫化分组设置?
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = new HashMap<>(maxLhMachineCount);
        for (int lhGroupNo = BigDecimal.ONE.intValue(); lhGroupNo <= maxLhMachineCount; lhGroupNo++) {
            CxLhProductionHelper cxLhGroup = CxLhProductionHelper.createEmptyLhGroup(groupName, lhGroupNo, cxMachineInfo);
            cxLhRatioMap.put(lhGroupNo, cxLhGroup);
        }
        groupPlanInfo.setCxLhRatioMap(cxLhRatioMap);
    }

    /**
     * 汇总续作Sku初始排产量
     * 如果是按总量排，则Sum(净需求排产量)
     * 否则Sum(高优先级排产量)
     *
     * @param planList 续作Sku计划集合
     * @return
     */
    public static Integer getContinueSkuSummaryQty(List<MonthPlanProductionRequirePlanVo> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return BigDecimal.ZERO.intValue();
        }
        //是否按总需求排产
        Integer isProductionBySum = planList.get(BigDecimal.ZERO.intValue()).getIsProductionBySum();
        if (YesOrNoEnum.YES.getValue().equals(isProductionBySum)) {
            //总净需求量
            return planList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
        }
        //高优先级排产量
        return planList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum();
    }

    /**
     * 结构计划下的续作Sku分配硫化组信息
     *
     * @param groupPlanInfo     结构计划信息
     * @param assignedLhGroupNo 结构下已分配的硫化组集合信息
     * @param continueSkuInfo   待分配硫化组的续作Sku信息
     * @return
     */
    @Deprecated
    public static List<CxLhProductionHelper> continueSkuAllocationLhGroup(Context context, ProductionPlanGroupInfo groupPlanInfo, Set<Integer> assignedLhGroupNo, CxContinueSkuInfoHelper continueSkuInfo) {
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = groupPlanInfo.getCxLhRatioMap();
        Integer maxLhGroupNo = cxLhRatioMap.size();
        Integer mouldNumber = continueSkuInfo.getMouldNumber();
        Integer continueSkuMaxLhGroup = continueSkuInfo.getUsedLhMachineCountByMouldNumber();
        List<ProductionMouldInfoVo> mouldList = SkuMouldSelector.getContinueSkuMouldNumberInit(context, continueSkuInfo.getMaterialDesc(), mouldNumber);
        List<CxLhProductionHelper> allocationGroupList = new ArrayList<>();
        int assignedCount = BigDecimal.ONE.intValue();
        for (int lhGroupNo = BigDecimal.ONE.intValue(); lhGroupNo <= maxLhGroupNo; lhGroupNo++) {
            if (assignedLhGroupNo.contains(lhGroupNo)) {
                continue;
            }
            if (assignedCount > continueSkuMaxLhGroup) {
                break;
            }
            assignedLhGroupNo.add(lhGroupNo);
            CxLhProductionHelper allocationGroup = cxLhRatioMap.get(lhGroupNo);
            Integer startMouldNumber = continueSkuInfo.getUsedMouldIndex(assignedCount - BigDecimal.ONE.intValue());
            Integer endMouldNumber = continueSkuInfo.getUsedMouldIndex(assignedCount);
            List<ProductionMouldInfoVo> selectedDoubleMouldList = mouldList.subList(startMouldNumber, endMouldNumber);
            allocationGroupList.add(allocationGroup);
            fullContinueMaterialAndMould(allocationGroup, continueSkuInfo, selectedDoubleMouldList);
            assignedCount = assignedCount + BigDecimal.ONE.intValue();
        }
        return allocationGroupList;
    }

    /**
     * 填充续作物料及模具
     *
     * @param cxLhGroup               成型硫化分组
     * @param continueSkuInfo         续作Sku信息
     * @param selectedDoubleMouldList 选中的模具
     */
    private static void fullContinueMaterialAndMould(CxLhProductionHelper cxLhGroup, CxContinueSkuInfoHelper continueSkuInfo, List<ProductionMouldInfoVo> selectedDoubleMouldList) {
        cxLhGroup.setMaterialCode(continueSkuInfo.getMaterialCode());
        cxLhGroup.setMaterialDesc(continueSkuInfo.getMaterialDesc());
        if (CollectionUtils.isEmpty(selectedDoubleMouldList)) {
            return;
        }
        Set<String> mouldCodeSet = selectedDoubleMouldList.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        cxLhGroup.setProductionMouldSet(mouldCodeSet);
    }

    private ContinueSkuCalculator() {

    }
}
