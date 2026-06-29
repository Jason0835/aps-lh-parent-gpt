package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionSkuPriorityVo;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日排产控制处理
 *
 * @author ZLT
 * @date 20260626
 */
@Slf4j
public class DayProductionControlHandler {

    /**
     * 20260422+
     * 获取最终可挑选的级别
     * 单独将供应链优先最先
     *
     * @param selectedTopList 符合条件的Top3列表
     * @return
     */
    public static List<ProductionSkuPriorityVo> getFinalSelectedList(List<ProductionSkuPriorityVo> selectedTopList) {
        if (CollectionUtils.isEmpty(selectedTopList)) {
            return Collections.emptyList();
        }
        //20260422+ 供应链优先-即物料优先
        List<ProductionSkuPriorityVo> hasSupplyChainPriorityList = selectedTopList.stream().filter(singleSelected -> singleSelected.isHasSupplyChainPriority()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasSupplyChainPriorityList)) {
            return selectedTopList;
        }
        return hasSupplyChainPriorityList;
    }
    /**
     * 根据参数，获取Top列表中共用模具，共用胎胚的Sku
     * Top列表中的排产量相差在范围内
     *
     * @param productionContext      排产上下文
     * @param selectedTopList        排产Sku列表
     * @param dayProductionLimitInfo 日排产信息
     * @param startDay               开始排产日
     * @param endDay                 排产结束日
     * @return
     */
    public static List<ProductionSkuPriorityVo> getShareMoldOrShareEmbryo(TbrProductionContext productionContext,
                                                                          List<ProductionSkuPriorityVo> selectedTopList,
                                                                          Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo,
                                                                          Integer startDay,
                                                                          Integer endDay) {
        if (CollectionUtils.isEmpty(selectedTopList)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return selectedTopList;
        }
        //是否需要共用模具、共用胎胚优先：需满足差值范围
        boolean isFindShare = isFindShareMoldOrEmbryo(productionContext, selectedTopList);
        if (!isFindShare) {
            return selectedTopList;
        }
        //获取共用模具或是共用胎胚的预排Sku列表
        return getShareMoldOrEmbryo(productionContext, selectedTopList, dayProductionLimitInfo, startDay);
    }

    /**
     * 判断是否需要共用模具、共用胎胚优先
     * true 表示需要 false 表示不需要
     *
     * @param productionContext 排产上下文
     * @param selectedTopList   高优先级预排产Sku列表
     * @return
     */
    private static boolean isFindShareMoldOrEmbryo(TbrProductionContext productionContext, List<ProductionSkuPriorityVo> selectedTopList) {
        if (CollectionUtils.isEmpty(selectedTopList)) {
            return false;
        }
        //预排产Sku的排产量差值范围
        Integer diffRange = productionContext.getBaseDataContainer().getParamConfiguration().getShareMoldOrEmbryoPriorityRange();
        if (null == diffRange || diffRange <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //是否在差值范围内
        boolean isRange = false;
        int size = selectedTopList.size();
        for (int before = 0; before < size; before++) {
            for (int after = before + BigDecimal.ONE.intValue(); after < size; after++) {
                Integer diffRangeValue = Math.abs(selectedTopList.get(before).getNeedPlanedQty() - selectedTopList.get(after).getNeedPlanedQty());
                if (diffRangeValue <= diffRange) {
                    isRange = true;
                    break;
                }
            }
            if (isRange) {
                break;
            }
        }
        return isRange;
    }

    /**
     * 获取在startDay或是startDay-1天，预排的Sku
     * 是否有共模具或是共胎胚的预排Sku
     *
     * @param productionContext      排产上下文
     * @param selectedTopList        预排Sku列表
     * @param dayProductionLimitInfo 所有日排产信息
     * @param startDay               开始排产日
     * @return
     */
    private static List<ProductionSkuPriorityVo> getShareMoldOrEmbryo(TbrProductionContext productionContext,
                                                                      List<ProductionSkuPriorityVo> selectedTopList,
                                                                      Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo,
                                                                      Integer startDay) {
        if (CollectionUtils.isEmpty(selectedTopList) || null == startDay) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return selectedTopList;
        }
        //找在startDay或是startDay前一天有共用模具或是共用胎胚的
        Map<String, ProductionSkuPriorityVo> resultMap = Maps.newHashMap();
        Map<String, ProductionSkuPriorityVo> currentDayResult = getSharePreSkuInfo(productionContext, dayProductionLimitInfo, selectedTopList, startDay);
        if (!CollectionUtils.isEmpty(currentDayResult)) {
            resultMap.putAll(currentDayResult);
        }
        Integer previousDay = getPreviousDay(dayProductionLimitInfo, startDay);
        Map<String, ProductionSkuPriorityVo> previousDayResult = getSharePreSkuInfo(productionContext, dayProductionLimitInfo, selectedTopList, previousDay);
        if (!CollectionUtils.isEmpty(previousDayResult)) {
            resultMap.putAll(previousDayResult);
        }
        //没有共用的
        if (CollectionUtils.isEmpty(resultMap)) {
            return selectedTopList;
        }
        return Lists.newArrayList(resultMap.values());
    }

    /**
     * 获取在startDay前一天的排产日
     *
     * @param allDayProductionInfo 所有日产信息
     * @param startDay             开始排产日
     * @return
     */
    private static Integer getPreviousDay(Map<Integer, GroupPlanCxLhCapacityLimitHelper> allDayProductionInfo, Integer startDay) {
        List<Integer> allProductionDayList = Lists.newArrayList(allDayProductionInfo.keySet());
        allProductionDayList.sort(Comparator.comparing(Integer::intValue));
        int index = -BigDecimal.ONE.intValue();
        for (Integer productionDay : allProductionDayList) {
            index = index + BigDecimal.ONE.intValue();
            if (startDay.equals(productionDay)) {
                break;
            }
        }
        int previousDayIndex = index - BigDecimal.ONE.intValue();
        if (previousDayIndex < BigDecimal.ZERO.intValue()) {
            return null;
        }
        return allProductionDayList.get(previousDayIndex);
    }

    /**
     * 获取在productionDay中，从selectedTopList列表中找出是否有共用模具或是共用胎胚
     * 的预排Sku
     *
     * @param productionContext    排产上下文
     * @param allDayProductionInfo 完整的日排产信息
     * @param selectedTopList      预排Sku列表
     * @param productionDay        排产日
     * @return
     */
    private static Map<String, ProductionSkuPriorityVo> getSharePreSkuInfo(TbrProductionContext productionContext,
                                                                           Map<Integer, GroupPlanCxLhCapacityLimitHelper> allDayProductionInfo,
                                                                           List<ProductionSkuPriorityVo> selectedTopList,
                                                                           Integer productionDay) {
        if (CollectionUtils.isEmpty(allDayProductionInfo) || null == productionDay || CollectionUtils.isEmpty(selectedTopList)) {
            return Collections.emptyMap();
        }
        GroupPlanCxLhCapacityLimitHelper dayProductionInfo = allDayProductionInfo.get(productionDay);
        if (null == dayProductionInfo) {
            return Collections.emptyMap();
        }
        Map<String, ProductionSkuPriorityVo> resultMap = Maps.newHashMap();
        selectedTopList.forEach(singleSku -> {
            if (isHasShareMoldOrEmbryo(productionContext, singleSku, dayProductionInfo)) {
                resultMap.put(singleSku.getMaterialDesc(), singleSku);
            }
        });
        if (CollectionUtils.isEmpty(resultMap)) {
            return Collections.emptyMap();
        }
        return resultMap;
    }

    /**
     * 预计排产Sku是否在dayPlanedInfo中共模具或是共生胎
     *
     * @param productionContext 排产上下文
     * @param preProductionSku  预计排产Sku
     * @param dayPlanedInfo     日排产信息
     * @return
     */
    private static boolean isHasShareMoldOrEmbryo(TbrProductionContext productionContext,
                                                  ProductionSkuPriorityVo preProductionSku,
                                                  GroupPlanCxLhCapacityLimitHelper dayPlanedInfo) {
        if (null == dayPlanedInfo || null == preProductionSku) {
            return false;
        }
        Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = dayPlanedInfo.getProductionSkuQtyInfo();
        if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
            return false;
        }
        String materialDesc = preProductionSku.getMaterialDesc();
        List<MonthPlanProductionRequirePlanVo> planList = productionContext.getAllSkuProductionPlan().get(materialDesc);
        if (CollectionUtils.isEmpty(planList)) {
            return false;
        }
        String embryoCode = planList.get(BigDecimal.ZERO.intValue()).getEmbryoCode();
        Set<String> embryoCodeSet = dayPlanedInfo.getProductionEmbryoCodeSet();
        boolean isShareEmbryo = isShareEmbryoCode(embryoCodeSet, embryoCode);
        Set<String> productionSkuSet = productionSkuQtyInfo.keySet();
        boolean isShareMold = isShareMold(productionContext, productionSkuSet, materialDesc);
        return isShareEmbryo || isShareMold;
    }

    /**
     * 是否有共用模具
     *
     * @param productionContext 排产上下文
     * @param productionSkuSet  已排产Sku信息
     * @param materialDesc      预计排产Sku
     * @return
     */
    private static boolean isShareMold(TbrProductionContext productionContext,
                                       Set<String> productionSkuSet,
                                       String materialDesc) {
        if (CollectionUtils.isEmpty(productionSkuSet) || StringUtils.isBlank(materialDesc)) {
            return false;
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        for (String productionMaterialDesc : productionSkuSet) {
            if (baseDataContainer.isShareMouldSameGroup(productionMaterialDesc, materialDesc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否共用胎胚
     *
     * @param embryoCodeSet 已排产胎胚
     * @param embryoCode    胎胚号
     * @return
     */
    private static boolean isShareEmbryoCode(Set<String> embryoCodeSet, String embryoCode) {
        if (CollectionUtils.isEmpty(embryoCodeSet)) {
            return false;
        }
        return embryoCodeSet.contains(embryoCode);
    }

    private DayProductionControlHandler() {

    }
}
