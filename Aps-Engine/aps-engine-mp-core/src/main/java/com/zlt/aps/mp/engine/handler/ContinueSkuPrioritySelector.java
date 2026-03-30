package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 续作Sku优先级选择器
 *
 * @author ZLT
 * @date 20260327
 */
@Slf4j
public class ContinueSkuPrioritySelector {

    /**
     * @param context            排产上下文
     * @param productionPlanInfo 分组计划
     * @param continueType       续作类型：同规格同花纹 共用模具
     * @param continueSkuMap     续作Sku信息
     */
    public static List<MonthPlanProductionRequirePlanVo> getContinueSkuPlanByType(Context context, ProductionStageEnum productionStage, ProductionPlanGroupInfo productionPlanInfo, ContinueTypeEnum continueType, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (CollectionUtils.isEmpty(continueSkuMap)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionRequirePlanVo> productionPlanList = productionPlanInfo.getGroupPlanData().stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        //没有待排计划
        if (CollectionUtils.isEmpty(productionPlanList)) {
            //todo 记录日志
            return Collections.emptyList();
        }
        Map<Long, MonthPlanProductionRequirePlanVo> matchMap = new HashMap<>();
        continueSkuMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            //获取同规格同花纹或是同生胎同模具的其它sku排产计划9
            Set<String> shareMouldMaterialDescSet = getShareMouldSkuByContinueSku(productionPlanList, cxContinueSkuInfo);
            List<MonthPlanProductionRequirePlanVo> singleMatchList = productionPlanInfo.getContinueListByType(productionStage, continueType, materialDesc, shareMouldMaterialDescSet, cxContinueSkuInfo);
            if (CollectionUtils.isEmpty(singleMatchList)) {
                return;
            }
            singleMatchList.forEach(singlePlan -> matchMap.put(singlePlan.getMonthPlanId(), singlePlan));
        });
        if (CollectionUtils.isEmpty(matchMap)) {
            return Collections.emptyList();
        }
        return matchMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 挑选最高优先级的Sku
     *
     * @param productionStage 排产阶段
     * @param allSkuList      可选择Sku计划
     * @param excludeSkuSet   需要剔除的Sku信息
     * @return
     */
    public static String getHeightPrioritySku(ProductionStageEnum productionStage, List<MonthPlanProductionRequirePlanVo> allSkuList, Set<String> excludeSkuSet) {
        //挑选可排产计划
        if (CollectionUtils.isEmpty(allSkuList)) {
            //todo 记录日志
            return "";
        }
        Set<String> rejectSkuSet = Optional.ofNullable(excludeSkuSet).orElse(Collections.emptySet());
        List<MonthPlanProductionRequirePlanVo> sameMultipleSkuList = allSkuList.stream().filter(single -> !rejectSkuSet.contains(single.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameMultipleSkuList)) {
            return "";
        }
        //先取得高优先级量最大的
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = sameMultipleSkuList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        Map<String, Integer> productionSkuMap = new HashMap<>();
        skuGroupMap.forEach((skuMaterialDesc, groupPlanList) -> {
            Integer sumProductionQty = ContinueSkuCalculator.getContinueSkuSummaryQty(productionStage, groupPlanList);
            if (sumProductionQty > BigDecimal.ZERO.intValue()) {
                productionSkuMap.put(skuMaterialDesc, sumProductionQty);
            }
        });
        if (CollectionUtils.isEmpty(productionSkuMap)) {
            //todo 记录日志
            return "";
        }
        Optional<Map.Entry<String, Integer>> maxEntry = productionSkuMap.entrySet().stream().max(Map.Entry.comparingByValue());
        return maxEntry.get().getKey();
    }

    /**
     * 从模具关系中和硫化组排产模具，挑选共用模具的物料集合
     *
     * @param productionPlanList 结构下所有排产Sku
     * @param continueSku        收尾信息
     */
    private static Set<String> getShareMouldSkuByContinueSku(List<MonthPlanProductionRequirePlanVo> productionPlanList, CxContinueSkuInfoHelper continueSku) {
        Set<String> shareMouldMaterialDescSet = new HashSet<>();
        productionPlanList.forEach(single -> {
            if (continueSku.getMainPattern().equals(single.getMainPattern())) {
                shareMouldMaterialDescSet.add(single.getMaterialDesc());
            }
        });
        return shareMouldMaterialDescSet;
    }

}
