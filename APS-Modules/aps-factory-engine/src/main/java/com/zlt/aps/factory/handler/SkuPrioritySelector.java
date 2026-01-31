package com.zlt.aps.factory.handler;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SKU优先级选择器
 * 根据复杂业务规则从SKU集合中选出最高优先级的SKU
 * @author Yelq
 */
public class SkuPrioritySelector {
  /**
   * 主方法：选择最高优先级的SKU
   * @param skuPlanMap SKU到需求计划列表的映射
   * @return 最高优先级的SKU，如果没有则返回Optional.empty()
   */
  public static Optional<String> selectHighestPrioritySku(
      Map<String, List<MonthPlanProductionRequirePlanVo>> skuPlanMap,TbrProductionContext productionContext, Integer startDay, Integer endDay) {
    if (CollectionUtils.isEmpty(skuPlanMap)) {
      return Optional.empty();
    }
    // 1. 转换数据为SKU信息对象
    List<SkuPriorityInfo> allSkuInfos = convertToSkuPriorityInfo(skuPlanMap,productionContext,startDay,endDay);
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
   * 应用嵌套优先级过滤器
   * 每一级过滤后，如果还有多个SKU，进入下一级
   */
  private static List<SkuPriorityInfo> applyNestedPriorityFilters(List<SkuPriorityInfo> skuInfos) {
    List<SkuPriorityInfo> currentList = new ArrayList<>(skuInfos);

    // 第1级：供应链优先标记
    List<SkuPriorityInfo> heightPrioritySkus = filterBySupplyChainPriority(currentList);
    if (!CollectionUtils.isEmpty(heightPrioritySkus)) {
      if(heightPrioritySkus.size() == 1) {
        return heightPrioritySkus;
      }
      currentList = heightPrioritySkus;
    }
    // 第2级：模具产能受限约束
    List<SkuPriorityInfo> moldCapacityLimitSkus = filterByMoldCapacityLimit(currentList);
    if (!CollectionUtils.isEmpty(moldCapacityLimitSkus)) {
      if(moldCapacityLimitSkus.size() == 1) {
        return moldCapacityLimitSkus;
      }
      currentList = moldCapacityLimitSkus;
    }

    // 第3级：库销比约束
    List<SkuPriorityInfo> inventorySaleRatioSkus = filterByInventorySaleRatio(currentList);
    if (!CollectionUtils.isEmpty(inventorySaleRatioSkus)) {
      if(inventorySaleRatioSkus.size() == 1) {
        return inventorySaleRatioSkus;
      }
      currentList = inventorySaleRatioSkus;
    }

    // 第4级：小于50条约束
    List<SkuPriorityInfo> lessMinQtySkus = filterByLessMinQty(currentList);
    if (!CollectionUtils.isEmpty(lessMinQtySkus)) {
      if(lessMinQtySkus.size() == 1) {
        return lessMinQtySkus;
      }
      currentList = lessMinQtySkus;
    }
    // 第5级：净需求大约束
    return filterByNetRequirement(currentList);
  }

  /**
   * 第5级过滤器：净需求大约束
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
   * 第3级过滤器：库销比约束
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
   * 第2级过滤器：模具产能受限约束
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
   * 转换为SKU优先级信息对象
   */
  private static List<SkuPriorityInfo> convertToSkuPriorityInfo(
      Map<String, List<MonthPlanProductionRequirePlanVo>> skuPlanMap,TbrProductionContext productionContext, Integer startDay, Integer endDay) {

    return skuPlanMap.entrySet().stream()
        .map(entry -> {
          String sku = entry.getKey();
          List<MonthPlanProductionRequirePlanVo> plans = entry.getValue();

          return createSkuPriorityInfo(sku, plans,productionContext,startDay,endDay);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * 创建SKU优先级信息
   */
  private static SkuPriorityInfo createSkuPriorityInfo(
      String sku, List<MonthPlanProductionRequirePlanVo> plans,TbrProductionContext productionContext, Integer startDay, Integer endDay) {
    if (CollectionUtils.isEmpty(plans)) {
      return null;
    }
    SkuPriorityInfo info = new SkuPriorityInfo();
    info.setSku(sku);
    // 计算聚合指标
    calculateAggregateMetrics(info, plans,productionContext,startDay,endDay);

    return info;
  }

  /**
   * 计算聚合指标
   */
  private static void calculateAggregateMetrics(
      SkuPriorityInfo info, List<MonthPlanProductionRequirePlanVo> plans,TbrProductionContext productionContext, Integer startDay, Integer endDay) {

    // 1. 供应链优先标记（只要有一个计划标记为"优先"）
    boolean hasSupplyChainPriority = plans.stream()
        .anyMatch(SkuPrioritySelector::hasSupplyChainPriority);
    info.setHasSupplyChainPriority(hasSupplyChainPriority);

    // 2. 模具产能受限情况

    //是否共用模具受限？--最后两副
    Set<String> limitShareMouldSet = productionContext.getLimitShareMouldOtherSku(info.getSku(), startDay, endDay);
    info.setHasMoldCapacityLimit(!CollectionUtils.isEmpty(limitShareMouldSet));

    if(info.hasMoldCapacityLimit) {
      // 3. 模具受限的净需求量总和
      int moldLimitedNetRequirement = plans.stream().filter(plan -> null != plan.getVirtualProductionQty())
          .mapToInt(MonthPlanProductionRequirePlanVo::getVirtualProductionQty)
          .sum();
      info.setMoldLimitedNetRequirement(moldLimitedNetRequirement);
    }else{
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
   * 检查是否有供应链优先标记
   */
  private static boolean hasSupplyChainPriority(MonthPlanProductionRequirePlanVo plan) {
    return YesOrNoEnum.YES.getCode().equals(plan.getIsPrioritize());
  }

  /**
   * SKU优先级信息类
   */
  @Data
  public static class SkuPriorityInfo {
    private String sku;
    private boolean hasSupplyChainPriority;
    private boolean hasMoldCapacityLimit;
    private int moldLimitedNetRequirement;
    private double inventorySaleRatio;
    private boolean isLessMinQty;
    private int totalNetRequirement;
    private List<MonthPlanProductionRequirePlanVo> plans;

    @Override
    public String toString() {
      return String.format("SKU: %s, 供应链优先: %s, 模具受限: %s, 受限净需求: %d, " +
              "库销比: %.2f, 小于最小排产量: %s, 总净需求: %d",
          sku, hasSupplyChainPriority, hasMoldCapacityLimit,
          moldLimitedNetRequirement, inventorySaleRatio,
          isLessMinQty, totalNetRequirement);
    }
  }

}
