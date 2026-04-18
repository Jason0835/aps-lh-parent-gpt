package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.FormalRoundEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SkuNeedProductionInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SKU优先级选择器
 * 根据复杂业务规则从SKU集合中选出最高优先级的SKU
 *
 * @author Yelq
 */
public class SkuPrioritySelector {

    /**
     * 20260403+
     * 挑选最高优先级的Sku
     * 需要对比进入高优先级Top列表的Sku，
     * 再对比产能覆盖情况
     * 场景：在机结构在产机台
     * 1、同规格同花纹->同模具
     * 2、新增Sku
     *
     * @param context         排产上下文
     * @param productionStage 排产阶段
     * @param formalRound     轮次
     * @param groupPlanInfo   分组计划
     * @param lhGroup         收尾硫化组
     * @param continueType    类型(续作同规格同花纹、同模具)
     * @param allSkuList      可选择Sku计划
     * @param excludeSkuSet   需要剔除的Sku信息
     * @param startDay        开始排产日
     * @param endDay          结束排产日
     * @return
     */
    public static String getHighestPrioritySku(Context context, ProductionStageEnum productionStage, FormalRoundEnum formalRound, ProductionPlanGroupInfo groupPlanInfo, EarliestConclusionLhGroupHelper lhGroup, ContinueTypeEnum continueType, List<MonthPlanProductionRequirePlanVo> allSkuList, Set<String> excludeSkuSet, Integer startDay, Integer endDay) {
        List<MonthPlanProductionRequirePlanVo> effectiveSkuList = getEffectiveSkuList(allSkuList, excludeSkuSet);
        if (CollectionUtils.isEmpty(effectiveSkuList)) {
            return StringUtils.EMPTY;
        }
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = effectiveSkuList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<SkuPriorityInfo> skuPriorityList = new ArrayList<>();
        skuGroupMap.forEach((skuMaterialDesc, planList) -> {
            SkuPriorityInfo skuPriorityInfo = buildSkuPriorityInfo(productionContext, skuMaterialDesc, planList, startDay, endDay);
            if (null == skuPriorityInfo) {
                return;
            }
            skuPriorityList.add(skuPriorityInfo);
        });
        if (CollectionUtils.isEmpty(skuPriorityList)) {
            return StringUtils.EMPTY;
        }
        Integer maxCount = productionContext.getBaseDataContainer().getParamConfiguration().getHeightPrioritySkuPreCount();
        List<SkuPriorityInfo> selectedList = getHighestPriorityCount(skuPriorityList, maxCount);
        if (CollectionUtils.isEmpty(selectedList)) {
            return StringUtils.EMPTY;
        }
        Integer maxLhDays = getLhMaxDays(context, startDay, endDay);
        List<ProductionSkuPriorityVo> selectedSkuPriorityList = new ArrayList<>();
        selectedList.forEach(singlePriority -> {
            ProductionSkuPriorityVo priority = buildProductionSkuPriorityInfo(productionContext, singlePriority, groupPlanInfo, continueType, allSkuList, lhGroup, maxLhDays, startDay, endDay);
            if (null == priority) {
                return;
            }
            selectedSkuPriorityList.add(priority);
        });
        if (CollectionUtils.isEmpty(selectedSkuPriorityList)) {
            return StringUtils.EMPTY;
        }
        //最低实单硫化机台
        if (FormalRoundEnum.ACTUAL_MIN_LH_MACHINE == formalRound) {
            return getCoveredMostSku(selectedSkuPriorityList);
        }
        String coveredResult = getCoveredMostSku(selectedSkuPriorityList);
        if(!StringUtils.EMPTY.equals(coveredResult)){
            return coveredResult;
        }
        //不可覆盖，挑选剩余量最少的
        selectedSkuPriorityList.sort(Comparator.comparing(ProductionSkuPriorityVo::getDiffValueByNoCovered));
        return selectedSkuPriorityList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
    }

    /**
     * 20260408+
     * 只使用在模拟排产阶段-新增Sku
     * 挑选最高优先级的Sku
     * 需要对比进入高优先级Top列表的Sku，
     * 再对比产能覆盖情况
     *
     * @param context         排产上下文
     * @param productionStage 排产阶段
     * @param cxMachineInfo   排产成型机
     * @param cxLhGroup       收尾硫化组
     * @param allSkuList      可选择Sku计划
     * @param excludeSkuSet   需要剔除的Sku信息
     * @param startDay        开始排产日
     * @param endDay          结束排产日
     * @return
     */
    public static String getHighestPrioritySku(Context context, ProductionStageEnum productionStage, CxMachineBaseInfoVo cxMachineInfo, CxLhProductionHelper cxLhGroup, List<MonthPlanProductionRequirePlanVo> allSkuList, Set<String> excludeSkuSet, Integer startDay, Integer endDay) {
        List<MonthPlanProductionRequirePlanVo> effectiveSkuList = getEffectiveSkuList(allSkuList, excludeSkuSet);
        if (CollectionUtils.isEmpty(effectiveSkuList)) {
            return StringUtils.EMPTY;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, List<MonthPlanProductionRequirePlanVo>> skuGroupMap = effectiveSkuList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc));
        List<SkuPriorityInfo> skuPriorityList = new ArrayList<>();
        skuGroupMap.forEach((skuMaterialDesc, planList) -> {
            SkuPriorityInfo skuPriorityInfo = buildSkuPriorityInfo(productionContext, skuMaterialDesc, planList, startDay, endDay);
            if (null == skuPriorityInfo) {
                return;
            }
            skuPriorityList.add(skuPriorityInfo);
        });
        if (CollectionUtils.isEmpty(skuPriorityList)) {
            return StringUtils.EMPTY;
        }
        Integer maxCount = productionContext.getBaseDataContainer().getParamConfiguration().getHeightPrioritySkuPreCount();
        List<SkuPriorityInfo> selectedList = getHighestPriorityCount(skuPriorityList, maxCount);
        if (CollectionUtils.isEmpty(selectedList)) {
            return StringUtils.EMPTY;
        }
        Integer maxLhDays = getLhMaxDays(context, startDay, endDay);
        List<ProductionSkuPriorityVo> selectedSkuPriorityList = new ArrayList<>();
        selectedList.forEach(singlePriority -> {
            ProductionSkuPriorityVo priority = buildProductionSkuPriorityInfo(productionContext, singlePriority, cxMachineInfo, allSkuList, cxLhGroup, maxLhDays, startDay, endDay);
            if (null == priority) {
                return;
            }
            selectedSkuPriorityList.add(priority);
        });
        if (CollectionUtils.isEmpty(selectedSkuPriorityList)) {
            return StringUtils.EMPTY;
        }
        //可覆盖--挑选可排产量多的
        List<ProductionSkuPriorityVo> coveredList = selectedSkuPriorityList.stream().filter(single -> single.isCovered()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(coveredList)) {
            coveredList.sort(Comparator.comparing(ProductionSkuPriorityVo::getNeedDays, Comparator.reverseOrder()));
            return coveredList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
        }
        //不可覆盖，挑选剩余量最少的
        selectedSkuPriorityList.sort(Comparator.comparing(ProductionSkuPriorityVo::getDiffValueByNoCovered));
        return selectedSkuPriorityList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
    }

    /**
     * 主方法：选择最高优先级的SKU
     *
     * @param skuPlanMap SKU到需求计划列表的映射
     * @return 最高优先级的SKU，如果没有则返回Optional.empty()
     */
    public static Optional<String> selectHighestPrioritySku(
            Map<String, List<MonthPlanProductionRequirePlanVo>> skuPlanMap, TbrProductionContext productionContext, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(skuPlanMap)) {
            return Optional.empty();
        }
        // 1. 转换数据为SKU信息对象
        List<SkuPriorityInfo> allSkuInfos = convertToSkuPriorityInfo(skuPlanMap, productionContext, startDay, endDay);
        if (allSkuInfos.isEmpty()) {
            return Optional.empty();
        }
        // 2. 执行嵌套优先级筛选
        List<SkuPriorityInfo> filteredSkuInfos = applyNestedPriorityFilters(allSkuInfos);
        // 3. 如果还有多个SKU，按照净需求降序排序取第一个
        if (!CollectionUtils.isEmpty(filteredSkuInfos) && filteredSkuInfos.size() > 1) {
            filteredSkuInfos.sort((a, b) ->
                    Integer.compare(b.getTotalNetRequirement(), a.getTotalNetRequirement()));
        }
        // 4. 返回结果
        return CollectionUtils.isEmpty(filteredSkuInfos) ?
                Optional.empty() :
                Optional.of(filteredSkuInfos.get(0).getSku());
    }

    /**
     * 获取有效的可排产Sku列表
     * 需要剔除已经排产的Sku
     *
     * @param allSkuList    所有计划列表
     * @param excludeSkuSet 需要剔除的Sku列表
     * @return
     */
    private static List<MonthPlanProductionRequirePlanVo> getEffectiveSkuList(List<MonthPlanProductionRequirePlanVo> allSkuList, Set<String> excludeSkuSet) {
        //挑选可排产计划
        if (CollectionUtils.isEmpty(allSkuList)) {
            return Collections.emptyList();
        }
        Set<String> rejectSkuSet = Optional.ofNullable(excludeSkuSet).orElse(Collections.emptySet());
        List<MonthPlanProductionRequirePlanVo> effectiveSkuList = allSkuList.stream().filter(single -> !rejectSkuSet.contains(single.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveSkuList)) {
            return Collections.emptyList();
        }
        return effectiveSkuList;
    }

    /**
     * 挑选可覆盖且能排产最多的Sku
     *
     * @param selectedSkuPriorityList 高优先级Sku列表
     * @return
     */
    private static String getCoveredMostSku(List<ProductionSkuPriorityVo> selectedSkuPriorityList) {
        if (CollectionUtils.isEmpty(selectedSkuPriorityList)) {
            return StringUtils.EMPTY;
        }
        //可覆盖--挑选可排产量多的
        List<ProductionSkuPriorityVo> coveredList = selectedSkuPriorityList.stream().filter(single -> single.isCovered()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(coveredList)) {
            return StringUtils.EMPTY;
        }
        coveredList.sort(Comparator.comparing(ProductionSkuPriorityVo::getNeedDays, Comparator.reverseOrder()));
        return coveredList.get(BigDecimal.ZERO.intValue()).getMaterialDesc();
    }

    /**
     * 构建Sku排产优先级对象
     *
     * @param productionContext 排产上下文
     * @param skuMaterialDesc   Sku信息
     * @param planList          sku对应的计划
     * @param startDay          开始排产日
     * @param endDay            结束排产日
     * @return
     */
    private static SkuPriorityInfo buildSkuPriorityInfo(TbrProductionContext productionContext, String skuMaterialDesc, List<MonthPlanProductionRequirePlanVo> planList, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(planList)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> blankList = planList.stream().filter(singlePlan -> StringUtils.isBlank(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(blankList)) {
            return null;
        }
        Set<String> materialDescSet = planList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        if (materialDescSet.size() != BigDecimal.ONE.intValue()) {
            return null;
        }
        return createSkuPriorityInfo(skuMaterialDesc, planList, productionContext, startDay, endDay);
    }

    /**
     * 获取最高优先级的列表
     *
     * @param skuPriorityList 所有列表
     * @param maxCount        最大个数
     * @return
     */
    private static List<SkuPriorityInfo> getHighestPriorityCount(List<SkuPriorityInfo> skuPriorityList, Integer maxCount) {
        if (CollectionUtils.isEmpty(skuPriorityList) || null == maxCount || maxCount < BigDecimal.ZERO.intValue()) {
            return Collections.emptyList();
        }
        int size = skuPriorityList.size();
        if (size <= maxCount) {
            return skuPriorityList;
        }
        //排序：供应链优先 -> 先高优先级(模具受限优先 -> 库销比优先 -> 小批量优先 -> 量大优先) -> 再其他(模具受限优先 -> 库销比优先 -> 小批量优先 -> 量大优先)
        List<SkuPriorityInfo> highestList = new ArrayList<>();
        for (int count = BigDecimal.ONE.intValue(); count <= maxCount; ) {
            Set<String> foundSet = CollectionUtils.isEmpty(highestList) ? Collections.emptySet() : highestList.stream().map(SkuPriorityInfo::getSku).collect(Collectors.toSet());
            List<SkuPriorityInfo> matchList = skuPriorityList.stream().filter(singlePriority -> !foundSet.contains(singlePriority.getSku())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(matchList)) {
                break;
            }
            SkuPriorityInfo highestPriority = getHighestPrioritySku(matchList);
            if (null != highestPriority) {
                highestList.add(highestPriority);
                count = count + BigDecimal.ONE.intValue();
            }
        }
        return highestList;
//            //排序：供应链优先 -> 先高优先级(模具受限优先 -> 库销比优先 -> 小批量优先 -> 量大优先) -> 再其他(模具受限优先 -> 库销比优先 -> 小批量优先 -> 量大优先)
//            skuPriorityList.sort(Comparator.comparing(SkuPriorityInfo::isHasSupplyChainPriority, Comparator.reverseOrder()).thenComparing(SkuPriorityInfo::isHasHeightPriority, Comparator.reverseOrder()).thenComparing(SkuPriorityInfo::isHasMoldCapacityLimit, Comparator.reverseOrder()).thenComparing(SkuPriorityInfo::getInventorySaleRatio).thenComparing(SkuPriorityInfo::isLessMinQty, Comparator.reverseOrder()).thenComparing(SkuPriorityInfo::getTotalNetRequirement, Comparator.reverseOrder()));
//        return skuPriorityList.subList(BigDecimal.ZERO.intValue(), maxCount);
    }

    /**
     * 可排产天数信息
     *
     * @param context  排产上下文
     * @param startDay 开始排产日
     * @param endDay   排产结束日
     * @return
     */
    private static Integer getLhMaxDays(Context context, Integer startDay, Integer endDay) {
        Integer canProductionDays = BigDecimal.ZERO.intValue();
        Set<Integer> stopSet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        for (Integer productionDay = startDay; productionDay <= endDay; productionDay++) {
            if (stopSet.contains(productionDay)) {
                continue;
            }
            canProductionDays = canProductionDays + BigDecimal.ONE.intValue();
        }
        return canProductionDays;
    }

    /**
     * 取得优先级最高的一个
     *
     * @param findList 需要查找的Sku集合
     * @return
     */
    private static SkuPriorityInfo getHighestPrioritySku(List<SkuPriorityInfo> findList) {
        //执行嵌套优先级筛选
        List<SkuPriorityInfo> resultList = applyNestedPriorityFilters(findList);
        if (!CollectionUtils.isEmpty(resultList) && resultList.size() > BigDecimal.ONE.intValue()) {
            //如果还有多个SKU，按照净需求降序排序取第一个
            resultList.sort(Comparator.comparing(SkuPriorityInfo::getTotalNetRequirement, Comparator.reverseOrder()));
        }
        return resultList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 构建排产Sku列表(Top)单Sku对象
     *
     * @param context        排产上下文
     * @param singlePriority 单Sku优先级信息
     * @param groupPlanInfo  分组计划
     * @param continueType   类型(同规格同花纹、同模具)
     * @param allSkuList     分组下排产Sku信息
     * @param lhGroup        收尾组信息
     * @param maxLhDays      最大硫化天数
     * @param startDay       开始排产日
     * @param endDay         排产结束日
     * @return
     */
    private static ProductionSkuPriorityVo buildProductionSkuPriorityInfo(Context context, SkuPriorityInfo singlePriority, ProductionPlanGroupInfo groupPlanInfo, ContinueTypeEnum continueType, List<MonthPlanProductionRequirePlanVo> allSkuList, EarliestConclusionLhGroupHelper lhGroup, Integer maxLhDays, Integer startDay, Integer endDay) {
        String materialDesc = singlePriority.getSku();
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = SkuMouldSelector.selectedDoubleMouldByRange(context, materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            return null;
        }
        //计算需要排产的量
        SkuNeedProductionInfo needProductionInfo = SkuProductionQtySelector.getNeedProductionQty(continueType, allSkuList, materialDesc, true);
        if (null == needProductionInfo) {
            //todo 记录日志
            return null;
        }
        //确认排产时间范围-可排产天数
        MonthPlanProductionRequirePlanVo addSkuInfo = needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue());
        Set<Integer> productionDaySet = groupPlanInfo.getMouldProductionLimitInfo(context, addSkuInfo, lhGroup, doubleMouldList);
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return null;
        }
        return new ProductionSkuPriorityVo(materialDesc, maxLhDays, productionDaySet.size(), needProductionInfo.getMaxNeedDays());
    }

    /**
     * 构建排产Sku列表(Top)单Sku对象
     * 场景：
     * 1、机台反选结构
     * 2、结构正向选机台
     *
     * @param context        排产上下文
     * @param singlePriority 单Sku优先级信息
     * @param cxMachineInfo  分配的成型机台
     * @param allSkuList     分组下排产Sku信息
     * @param cxLhGroup      收尾组信息
     * @param maxLhDays      最大硫化天数
     * @param startDay       开始排产日
     * @param endDay         排产结束日
     * @return
     */
    private static ProductionSkuPriorityVo buildProductionSkuPriorityInfo(Context context, SkuPriorityInfo singlePriority, CxMachineBaseInfoVo cxMachineInfo, List<MonthPlanProductionRequirePlanVo> allSkuList, CxLhProductionHelper cxLhGroup, Integer maxLhDays, Integer startDay, Integer endDay) {
        String materialDesc = singlePriority.getSku();
        //选择模具
        List<ProductionMouldInfoVo> doubleMouldList = SkuMouldSelector.selectedDoubleMouldByRange(context, materialDesc, startDay, endDay);
        if (CollectionUtils.isEmpty(doubleMouldList)) {
            return null;
        }
        //计算需要排产的量
        SkuNeedProductionInfo needProductionInfo = SkuProductionQtySelector.getNeedProductionQty(ContinueTypeEnum.NO_CONTINUE, allSkuList, materialDesc, true);
        if (null == needProductionInfo) {
            //todo 记录日志
            return null;
        }
        MonthPlanProductionRequirePlanVo addSkuInfo = needProductionInfo.getNeedProductionList().get(BigDecimal.ZERO.intValue());
        //确认排产时间范围-可排产天数
        Set<Integer> productionDaySet = cxMachineInfo.getMouldProductionLimitInfo(context, addSkuInfo, cxLhGroup, endDay, doubleMouldList);
        if (CollectionUtils.isEmpty(productionDaySet)) {
            return null;
        }
        return new ProductionSkuPriorityVo(materialDesc, maxLhDays, productionDaySet.size(), needProductionInfo.getMaxNeedDays());
    }

    /**
     * 应用嵌套优先级过滤器
     * 每一级过滤后，如果还有多个SKU，进入下一级
     */
    private static List<SkuPriorityInfo> applyNestedPriorityFilters(List<SkuPriorityInfo> skuInfos) {
        List<SkuPriorityInfo> currentList = new ArrayList<>(skuInfos);

        // 第1级：供应链优先标记
        List<SkuPriorityInfo> supplyChainPrioritySkus = filterBySupplyChainPriority(currentList);
        if (!CollectionUtils.isEmpty(supplyChainPrioritySkus)) {
            if (supplyChainPrioritySkus.size() == 1) {
                return supplyChainPrioritySkus;
            }
            currentList = supplyChainPrioritySkus;
        }
        // 第2级：高优先级优先标记
        List<SkuPriorityInfo> heightPrioritySkus = filterByHeightPriority(currentList);
        if (!CollectionUtils.isEmpty(heightPrioritySkus)) {
            if (heightPrioritySkus.size() == 1) {
                return heightPrioritySkus;
            }
            currentList = heightPrioritySkus;
        }

        // 第3级：模具产能受限约束
        List<SkuPriorityInfo> moldCapacityLimitSkus = filterByMoldCapacityLimit(currentList);
        if (!CollectionUtils.isEmpty(moldCapacityLimitSkus)) {
            if (moldCapacityLimitSkus.size() == 1) {
                return moldCapacityLimitSkus;
            }
            currentList = moldCapacityLimitSkus;
        }

        // 第4级：库销比约束
        List<SkuPriorityInfo> inventorySaleRatioSkus = filterByInventorySaleRatio(currentList);
        if (!CollectionUtils.isEmpty(inventorySaleRatioSkus)) {
            if (inventorySaleRatioSkus.size() == 1) {
                return inventorySaleRatioSkus;
            }
            currentList = inventorySaleRatioSkus;
        }

        // 第5级：小于50条约束
        List<SkuPriorityInfo> lessMinQtySkus = filterByLessMinQty(currentList);
        if (!CollectionUtils.isEmpty(lessMinQtySkus)) {
            if (lessMinQtySkus.size() == 1) {
                return lessMinQtySkus;
            }
            currentList = lessMinQtySkus;
        }
        // 第6级：净需求大约束
        return filterByNetRequirement(currentList);
    }

    /**
     * 第1级过滤器：供应链优先标记
     */
    private static List<SkuPriorityInfo> filterBySupplyChainPriority(List<SkuPriorityInfo> skuInfos) {
        // 找出所有有供应链优先标记的SKU
        List<SkuPriorityInfo> prioritizedSkus = skuInfos.stream()
                .filter(SkuPriorityInfo::isHasSupplyChainPriority)
                .collect(Collectors.toList());
        // 如果有，返回这些SKU；否则返回所有SKU
        return prioritizedSkus.isEmpty() ? new ArrayList<>(skuInfos) : prioritizedSkus;
    }

    /**
     * 第2级过滤器：高优先级优先标记
     */
    private static List<SkuPriorityInfo> filterByHeightPriority(List<SkuPriorityInfo> skuInfos) {
        // 找出所有有高优先级标记的SKU
        List<SkuPriorityInfo> prioritizedSkus = skuInfos.stream()
                .filter(SkuPriorityInfo::isHasHeightPriority)
                .collect(Collectors.toList());
        // 如果有，返回这些SKU；否则返回所有SKU
        return prioritizedSkus.isEmpty() ? new ArrayList<>(skuInfos) : prioritizedSkus;
    }

    /**
     * 第3级过滤器：模具产能受限约束
     */
    private static List<SkuPriorityInfo> filterByMoldCapacityLimit(List<SkuPriorityInfo> skuInfos) {
        // 找出所有有模具产能受限的SKU
        List<SkuPriorityInfo> moldLimitedSkus = skuInfos.stream()
                .filter(SkuPriorityInfo::isHasMoldCapacityLimit)
                .collect(Collectors.toList());
        // 如果没有模具受限的SKU，返回所有SKU
        if (CollectionUtils.isEmpty(moldLimitedSkus)) {
            return new ArrayList<>(skuInfos);
        }
        // 如果有模具受限的SKU，找出受限净需求量最小的SKU
        int minMoldLimitedNetRequirement = moldLimitedSkus.stream()
                .mapToInt(SkuPriorityInfo::getMoldLimitedNetRequirement)
                .min()
                .orElse(Integer.MAX_VALUE);
        // 过滤出受限净需求量等于最小值的SKU
        return moldLimitedSkus.stream()
                .filter(info -> minMoldLimitedNetRequirement == info.getMoldLimitedNetRequirement())
                .collect(Collectors.toList());
    }

    /**
     * 第4级过滤器：小于50条约束
     */
    private static List<SkuPriorityInfo> filterByLessMinQty(List<SkuPriorityInfo> skuInfos) {
        // 找出计划数小于50的SKU
        List<SkuPriorityInfo> lessMinQtySkus = skuInfos.stream()
                .filter(SkuPriorityInfo::isLessMinQty)
                .collect(Collectors.toList());
        // 如果有小于50的SKU，返回这些；否则返回所有
        return CollectionUtils.isEmpty(lessMinQtySkus) ? new ArrayList<>(skuInfos) : lessMinQtySkus;
    }

    /**
     * 第5级过滤器：库销比约束
     */
    private static List<SkuPriorityInfo> filterByInventorySaleRatio(List<SkuPriorityInfo> skuInfos) {
        // 找出库销比最小的SKU
        double minInventorySaleRatio = skuInfos.stream()
                .mapToDouble(SkuPriorityInfo::getInventorySaleRatio)
                .min()
                .orElse(Double.MAX_VALUE);
        // 过滤出库销比等于最小值的SKU
        return skuInfos.stream()
                .filter(info -> minInventorySaleRatio == info.getInventorySaleRatio())
                .collect(Collectors.toList());
    }

    /**
     * 第6级过滤器：净需求大约束
     */
    private static List<SkuPriorityInfo> filterByNetRequirement(List<SkuPriorityInfo> skuInfos) {
        // 找出净需求最大的SKU
        int maxNetRequirement = skuInfos.stream()
                .mapToInt(SkuPriorityInfo::getTotalNetRequirement)
                .max()
                .orElse(Integer.MIN_VALUE);

        // 过滤出净需求等于最大值的SKU
        return skuInfos.stream()
                .filter(info -> maxNetRequirement == info.getTotalNetRequirement())
                .collect(Collectors.toList());
    }

    /**
     * 转换为SKU优先级信息对象
     */
    private static List<SkuPriorityInfo> convertToSkuPriorityInfo(
            Map<String, List<MonthPlanProductionRequirePlanVo>> skuPlanMap, TbrProductionContext productionContext, Integer startDay, Integer endDay) {

        return skuPlanMap.entrySet().stream()
                .map(entry -> {
                    String sku = entry.getKey();
                    List<MonthPlanProductionRequirePlanVo> plans = entry.getValue();

                    return createSkuPriorityInfo(sku, plans, productionContext, startDay, endDay);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 创建SKU优先级信息
     */
    private static SkuPriorityInfo createSkuPriorityInfo(
            String sku, List<MonthPlanProductionRequirePlanVo> plans, TbrProductionContext productionContext, Integer startDay, Integer endDay) {
        if (CollectionUtils.isEmpty(plans)) {
            return null;
        }
        SkuPriorityInfo info = new SkuPriorityInfo();
        info.setSku(sku);

        // 计算聚合指标
        calculateAggregateMetrics(info, plans, productionContext, startDay, endDay);

        return info;
    }

    /**
     * 计算聚合指标
     *
     * @param info
     * @param plans
     * @param productionContext
     * @param startDay
     * @param endDay
     */
    private static void calculateAggregateMetrics(SkuPriorityInfo info,
                                                  List<MonthPlanProductionRequirePlanVo> plans,
                                                  TbrProductionContext productionContext,
                                                  Integer startDay,
                                                  Integer endDay) {
        //20260413+ 重新计算库销比
        resetCalculateInventorySalesRatio(info.getSku(), productionContext);
        // 1. 供应链优先标记（只要有一个计划标记为"优先"）
        boolean hasSupplyChainPriority = plans.stream()
                .anyMatch(SkuPrioritySelector::hasSupplyChainPriority);
        info.setHasSupplyChainPriority(hasSupplyChainPriority);

        //1.1 高优先级量标记
        boolean hasHeightPriority = plans.stream().anyMatch(SkuPrioritySelector::hasHeightQtyPriority);
        info.setHasHeightPriority(hasHeightPriority);

        // 2. 模具产能受限情况 是否共用模具受限？--最后两副
        Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(info.getSku(), startDay, endDay);
        info.setHasMoldCapacityLimit(!CollectionUtils.isEmpty(limitShareMouldSet));

        if (info.isHasMoldCapacityLimit()) {
            // 3. 模具受限的净需求量总和
            int moldLimitedNetRequirement = plans.stream().filter(plan -> null != plan.getVirtualProductionQty())
                    .mapToInt(MonthPlanProductionRequirePlanVo::getVirtualProductionQty)
                    .sum();
            info.setMoldLimitedNetRequirement(moldLimitedNetRequirement);
        } else {
            info.setMoldLimitedNetRequirement(0);
        }
        // 4. 库销比（取平均值）
        double avgInventorySaleRatio = plans.stream()
                .filter(plan -> plan.getInventorySalesRatio() != null)
                .mapToDouble(MonthPlanProductionRequirePlanVo::getInventorySalesRatio)
                .min()
                .orElse(0.0);
        info.setInventorySaleRatio(avgInventorySaleRatio);

        boolean hasLessMinQty = plans.stream()
                .anyMatch(plan -> plan.isLess(plan.getMinProductionQty()));
        // 5. 小于最小批量
        info.setLessMinQty(hasLessMinQty);
        // 6. 净需求总量
        int totalNetRequirement = plans.stream().filter(plan -> plan.getVirtualProductionQty() != null)
                .mapToInt(MonthPlanProductionRequirePlanVo::getVirtualProductionQty)
                .sum();
        info.setTotalNetRequirement(totalNetRequirement);
        // 7. 其他可能需要的信息
        info.setPlans(new ArrayList<>(plans));
    }

    /**
     * 重新计算库销比
     *
     * @param materialDesc      Sku
     * @param productionContext 排产上下文
     */
    private static void resetCalculateInventorySalesRatio(String materialDesc, TbrProductionContext productionContext) {
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> allSkuPlanList = productionContext.getAllSkuProductionPlan().get(materialDesc);
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = allSkuPlanList.stream().filter(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return;
        }
        Integer allProductionQty = productionContext.getBaseDataContainer().getSumProductionQty(materialDesc);
        allSkuPlanList.forEach(singlePlan -> singlePlan.calculateInventorySalesRatio(allProductionQty));
    }

    /**
     * 检查是否有供应链优先标记
     *
     * @param plan
     */
    private static boolean hasSupplyChainPriority(MonthPlanProductionRequirePlanVo plan) {
        return YesOrNoEnum.YES.getCode().equals(plan.getIsPrioritize());
    }

    /**
     * 计划是否还有高优先级待排产量
     *
     * @param plan
     * @return
     */
    private static boolean hasHeightQtyPriority(MonthPlanProductionRequirePlanVo plan) {
        return plan.getHeightProductionQty() > BigDecimal.ZERO.intValue();
    }
}
