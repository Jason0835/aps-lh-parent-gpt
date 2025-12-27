package com.zlt.aps.factory.handler;

import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * 设置续作sku的计划量(先暂定高优先级)
     * 根据分组计划信息及对应分组的续作Sku信息
     * 统计已经有计划量的Sku信息对应的胎胚信息
     * 和使用模具数信息
     * 并设置到groupContinueInfo对象中
     * 取高优先级量还是总净需求量？
     *
     * @param groupPlanInfo     分组计划-TBR为结构
     * @param groupContinueInfo 分组对应的续作信息-TBR为结构
     */
    public static void setContinueSkuPlanDemandQty(ProductionPlanGroupInfo groupPlanInfo, CxContinueInfoHelper groupContinueInfo) {
        List<MonthPlanProductionRequirePlanVo> groupPlanList = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanList)) {
            //todo 记录日志
            return;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = groupContinueInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuMouldNumberMap)) {
            //todo 记录日志
            return;
        }
        //提取续作Sku计划
        Set<String> skuMaterialDescSet = continueSkuMouldNumberMap.keySet();
        List<MonthPlanProductionRequirePlanVo> continueSkuPlanList = groupPlanList.stream().filter(groupPlan -> groupPlan.hasProduction() && skuMaterialDescSet.contains(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(continueSkuPlanList)) {
            //todo 记录日志
            return;
        }
        //分组合计续作Sku的计划量-高优先级
        Map<String, List<MonthPlanProductionRequirePlanVo>> continueSkuGroupMap = continueSkuPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Map<String, Long> continueSkuProductionQtyMap = new HashMap<>();
        continueSkuGroupMap.forEach((materialDesc, planList) -> continueSkuProductionQtyMap.put(materialDesc, planList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getHeightProductionQty).sum()));
        //设置续作Sku的高优级量
        continueSkuMouldNumberMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            Long planDemandQty = continueSkuProductionQtyMap.get(materialDesc);
            if (null == planDemandQty) {
                planDemandQty = BigDecimal.ZERO.longValue();
            }
            cxContinueSkuInfo.setPlanDemandQty(planDemandQty);
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

    private ContinueSkuCalculator() {

    }
}
