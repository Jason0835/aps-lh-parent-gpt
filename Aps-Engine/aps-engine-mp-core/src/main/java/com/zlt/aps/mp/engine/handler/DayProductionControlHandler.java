package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionSkuPriorityVo;
import com.zlt.aps.mp.engine.enums.ProductionShareTypeEnum;
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
     * 根据参数，获取Top列表中共用模具，共用胎胚，共用成型编号的Sku
     * Top列表中的排产量相差在范围内
     * 先找共用模具，共用胎胚，后找共用成型编号
     *
     * @param productionContext      排产上下文
     * @param selectedTopList        排产Sku列表
     * @param dayProductionLimitInfo 日排产信息
     * @param startDay               开始排产日
     * @param endDay                 排产结束日
     * @return
     */
    public static List<ProductionSkuPriorityVo> getPriorityByShareInfo(TbrProductionContext productionContext,
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
        //是否需要共用模具、共用胎胚、共用成型编号优先：需满足差值范围
        boolean isFindShare = isFindSameDifferenceFlag(productionContext, selectedTopList);
        if (!isFindShare) {
            return selectedTopList;
        }
        //获取共用模具或是共用胎胚的预排Sku列表
        return getPriorityByShareInfo(productionContext, selectedTopList, dayProductionLimitInfo, startDay);
    }

    /**
     * 判断是否需要共用模具、共用胎胚、同成型编号优先
     * true 表示需要 false 表示不需要
     *
     * @param productionContext 排产上下文
     * @param selectedTopList   高优先级预排产Sku列表
     * @return
     */
    private static boolean isFindSameDifferenceFlag(TbrProductionContext productionContext, List<ProductionSkuPriorityVo> selectedTopList) {
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
     * 是否有共用的预排Sku
     * 1、共用模具、共用胎胚优先
     * 2、其次共用成型编号优先
     *
     * @param productionContext      排产上下文
     * @param selectedTopList        预排Sku列表
     * @param dayProductionLimitInfo 所有日排产信息
     * @param startDay               开始排产日
     * @return
     */
    private static List<ProductionSkuPriorityVo> getPriorityByShareInfo(TbrProductionContext productionContext,
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
        Integer previousDay = getPreviousDay(dayProductionLimitInfo, startDay);
        Map<String, ProductionSkuPriorityVo> resultMap = Maps.newHashMap();
        addSharePreSkuInfo(resultMap, productionContext, ProductionShareTypeEnum.SHARE_MOLD_OR_EMBRYO, dayProductionLimitInfo, selectedTopList, startDay);
        addSharePreSkuInfo(resultMap, productionContext, ProductionShareTypeEnum.SHARE_MOLD_OR_EMBRYO, dayProductionLimitInfo, selectedTopList, previousDay);
        if (!CollectionUtils.isEmpty(resultMap)) {
            //共用模具、共用胎胚优先
            return Lists.newArrayList(resultMap.values());
        }
        //20260731+ 没有共用模具，共用胎胚 则增加共用成型编号
        addSharePreSkuInfo(resultMap, productionContext, ProductionShareTypeEnum.SHARE_FORMING_NO, dayProductionLimitInfo, selectedTopList, startDay);
        addSharePreSkuInfo(resultMap, productionContext, ProductionShareTypeEnum.SHARE_FORMING_NO, dayProductionLimitInfo, selectedTopList, previousDay);
        //没有任何共用
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
     * 增加在productionDay与selectedTopList中的Sku有共用信息的预排Sku
     *
     * @param storageMap           需要放入共用信息集合
     * @param productionContext    排产上下文
     * @param shareTypeEnum        共用类型
     * @param allDayProductionInfo 分组内所有日排产信息
     * @param selectedTopList      预排的Top列表Sku信息
     * @param productionDay        排产日
     */
    private static void addSharePreSkuInfo(Map<String, ProductionSkuPriorityVo> storageMap,
                                           TbrProductionContext productionContext,
                                           ProductionShareTypeEnum shareTypeEnum,
                                           Map<Integer, GroupPlanCxLhCapacityLimitHelper> allDayProductionInfo,
                                           List<ProductionSkuPriorityVo> selectedTopList,
                                           Integer productionDay) {
        if (null == storageMap) {
            return;
        }
        Map<String, ProductionSkuPriorityVo> dayShareResult = getSharePreSkuInfo(productionContext, shareTypeEnum, allDayProductionInfo, selectedTopList, productionDay);
        if (CollectionUtils.isEmpty(dayShareResult)) {
            return;
        }
        storageMap.putAll(dayShareResult);
    }

    /**
     * 获取在productionDay中，已排产Sku中是否有与selectedTopList列表有共用信息
     * 的预排Sku
     * SHARE_MOLD_OR_EMBRYO : 共模具或是共生胎
     * SHARE_FORMING_NO: 同成型编号
     *
     * @param productionContext    排产上下文
     * @param shareTypeEnum        共用类型
     * @param allDayProductionInfo 完整的日排产信息
     * @param selectedTopList      预排Sku列表
     * @param productionDay        排产日
     * @return
     */
    private static Map<String, ProductionSkuPriorityVo> getSharePreSkuInfo(TbrProductionContext productionContext,
                                                                           ProductionShareTypeEnum shareTypeEnum,
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
            if (isHasShareByShareType(productionContext, shareTypeEnum, singleSku, dayProductionInfo)) {
                resultMap.put(singleSku.getMaterialDesc(), singleSku);
            }
        });
        if (CollectionUtils.isEmpty(resultMap)) {
            return Collections.emptyMap();
        }
        return resultMap;
    }

    /**
     * 预计排产Sku是否在dayPlanedInfo中具有共用信息排产
     * SHARE_MOLD_OR_EMBRYO : 共模具或是共生胎
     * SHARE_FORMING_NO: 同成型编号
     *
     * @param productionContext 排产上下文
     * @param shareType         共用类型业务
     * @param preProductionSku  预计排产Sku
     * @param dayPlanedInfo     日排产信息
     * @return
     */
    private static boolean isHasShareByShareType(TbrProductionContext productionContext,
                                                 ProductionShareTypeEnum shareType,
                                                 ProductionSkuPriorityVo preProductionSku,
                                                 GroupPlanCxLhCapacityLimitHelper dayPlanedInfo) {
        if (null == dayPlanedInfo || null == preProductionSku || null == shareType) {
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
        if (ProductionShareTypeEnum.SHARE_MOLD_OR_EMBRYO == shareType) {
            //共用模具或是同胎胚
            String embryoCode = planList.get(BigDecimal.ZERO.intValue()).getEmbryoCode();
            Set<String> embryoCodeSet = dayPlanedInfo.getProductionEmbryoCodeSet();
            boolean isShareEmbryo = isShareInfo(embryoCodeSet, embryoCode);
            Set<String> productionSkuSet = productionSkuQtyInfo.keySet();
            boolean isShareMold = isShareMold(productionContext, productionSkuSet, materialDesc);
            return isShareEmbryo || isShareMold;
        }
        if (ProductionShareTypeEnum.SHARE_FORMING_NO == shareType) {
            //同成型编号
            String formingNo = planList.get(BigDecimal.ZERO.intValue()).getFormingNo();
            if (StringUtils.isBlank(formingNo)) {
                return false;
            }
            Set<String> formingNoSet = dayPlanedInfo.getProductionFormingNoSet(productionContext);
            return isShareInfo(formingNoSet, formingNo);
        }
        return false;
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
     * 是否有共用信息
     * 场景：共用胎胚，共用成型编号
     *
     * @param allShareInfoSet 已排产共用信息(胎胚号，成型编号)
     * @param preAddShareInfo 预计加入的
     * @return
     */
    private static boolean isShareInfo(Set<String> allShareInfoSet, String preAddShareInfo) {
        if (CollectionUtils.isEmpty(allShareInfoSet)) {
            return false;
        }
        return allShareInfoSet.contains(preAddShareInfo);
    }

    private DayProductionControlHandler() {

    }
}
