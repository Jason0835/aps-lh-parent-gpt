package com.zlt.aps.monthplan.factory.helper;


import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpSimulatedOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 库存分配服务 - 根据库存对冲顺序配置进行智能分配
 * @author Yelq
 */
@Slf4j
public class PredictionAllocationHelper {

  /**
   * 根据库存对冲顺序配置，进行库存分配
   */
  public static List<DpSimulatedOffsetDetail> calculateSaleOrder(
      List<DpSimulatedOffsetDetail> tMonthDemands,
      List<FactoryMonthPlanMouldDayResult> productionFinalResults,
      List<MpMonthPlanMonitor>  mpMonthPlanMonitors) {
    List<DpSimulatedOffsetDetail> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(tMonthDemands)) {
      return result;
    }
    Map<String,List<DpSimulatedOffsetDetail>>  netDemandGroupMap = tMonthDemands.stream().collect(Collectors.groupingBy(DpSimulatedOffsetDetail::getMaterialCode));
    Map<String,Integer> productionQtyMap = calculateProductionQty(productionFinalResults);
    Map<String,Integer> completionQtyMap = calculateCompletionQty(mpMonthPlanMonitors);
    Map<String,List<FactoryMonthPlanMouldDayResult>> productionGroupMap = getProductionGroupMap(productionFinalResults);
    List<DpSimulatedOffsetDetail> allocations;
    BigDecimal stockQty;
    List<FactoryMonthPlanMouldDayResult> productionResults;
    int productionQty;
    int completionQty;
    for (Map.Entry<String, List<DpSimulatedOffsetDetail>> entry : netDemandGroupMap.entrySet()) {
      productionQty = productionQtyMap.getOrDefault(entry.getKey(),0);
      completionQty = completionQtyMap.getOrDefault(entry.getKey(), 0);
      stockQty = BigDecimal.valueOf(productionQty - completionQty);
      productionResults = productionGroupMap.get(entry.getKey());
      allocations = processOrderGroup(entry.getValue(),completionQty,stockQty,productionResults);
      if(CollectionUtils.isEmpty(allocations)) {
        continue;
      }
      result.addAll(allocations);
    }
    return result;
  }

  private static Map<String, List<FactoryMonthPlanMouldDayResult>> getProductionGroupMap(List<FactoryMonthPlanMouldDayResult> productionFinalResults) {
    if(CollectionUtils.isEmpty(productionFinalResults)) {
      return Collections.emptyMap();
    }
    return productionFinalResults.stream()
        .filter(Objects::nonNull)
        .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()))
        .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode));
  }

  private static Map<String, Integer> calculateCompletionQty(List<MpMonthPlanMonitor> mpMonthPlanMonitors) {
    if(CollectionUtils.isEmpty(mpMonthPlanMonitors)) {
      return Collections.emptyMap();
    }
    return mpMonthPlanMonitors.stream()
        .filter(Objects::nonNull)
        .filter(monthPlanMonitor -> StringUtils.isNotBlank(monthPlanMonitor.getMaterialCode()) &&   null != monthPlanMonitor.getProductionQty() )
        .collect(Collectors.groupingBy(MpMonthPlanMonitor::getMaterialCode, Collectors.summingInt(MpMonthPlanMonitor::getProductionQty)));
  }


  /**
   * 处理单个订单组的库存分配
   */
  private static List<DpSimulatedOffsetDetail> processOrderGroup(
      List<DpSimulatedOffsetDetail> saleOrders,
      int completionQty,
      BigDecimal stockQty,
      List<FactoryMonthPlanMouldDayResult> productionResults
      ) {
          List<DpSimulatedOffsetDetail> result = new ArrayList<>();
          // 定义优先级处理配置 (高 > 周期 > 中 > 常规 >  暂缓)
          List<PriorityProcessor> processors = Arrays.asList(
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_HIGHT,
                  FactoryMonthPlanMouldDayResult::getHeightProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP,
                  FactoryMonthPlanMouldDayResult::getCycleProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_MID,
                  FactoryMonthPlanMouldDayResult::getMidProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP,
                  FactoryMonthPlanMouldDayResult::getConventionProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_POSTPONE,
                  FactoryMonthPlanMouldDayResult::getPostponeProductionQty)
          );

          // 处理每个优先级
          for (PriorityProcessor processor : processors) {
            processPriority(saleOrders,completionQty, stockQty,productionResults, result, processor);
          }
          return result;
  }

  /**
   * 处理单一优先级
   */
  private static void processPriority(
      List<DpSimulatedOffsetDetail> saleOrders,
      int completionQty,
      BigDecimal stockQty,
      List<FactoryMonthPlanMouldDayResult> productionResults,
      List<DpSimulatedOffsetDetail> result,
      PriorityProcessor processor) {
    // 查找该优先级的销售订单
    Optional<DpSimulatedOffsetDetail> saleOrderOpt = findSaleOrderByPriority(saleOrders, processor.getPriority());
    if (!saleOrderOpt.isPresent()) {
      return;
    }
    // 计算该优先级的总订单数量
    int totalOrderQty = calculateTotalOrderQty(saleOrders, processor.getPriority());
    BigDecimal remainingQty = BigDecimal.valueOf(totalOrderQty);
    DpSimulatedOffsetDetail saleOrder = saleOrderOpt.get();
    if (stockQty.compareTo(remainingQty) >= 0) {
      stockQty = stockQty.subtract(remainingQty);
      remainingQty = BigDecimal.ZERO;
    } else {
      // 当前库存不足，全部冲减
      stockQty = BigDecimal.ZERO;
      remainingQty = remainingQty.subtract(stockQty);
    }
    saleOrder.setStockQty(stockQty.intValue());
    // 计算该优先级的生产数量
    int productionQty = calculateProductionQty(productionResults, processor.getProductionQtyExtractor());
    saleOrder.setProductionQty(productionQty);
    saleOrder.setCompletionQty(completionQty);
    saleOrder.setBaseVale(null);
    saleOrder.setId(null);
    // 计算净需求量
    saleOrder.setNetQty(remainingQty.intValue());
    result.add(saleOrder);
  }

  /**
   * 计算总订单数量
   */
  private static int calculateTotalOrderQty(
      List<DpSimulatedOffsetDetail> saleOrders,
      String priority) {

    return saleOrders.stream()
        .filter(order -> priority.equals(order.getScmPriority()))
        .filter(order -> order.getNetQty() != null && order.getNetQty() > 0)
        .mapToInt(DpSimulatedOffsetDetail::getNetQty)
        .sum();
  }

  /**
   * 计算生产数量
   */
  private static int calculateProductionQty(
      List<FactoryMonthPlanMouldDayResult> productionResults,
      ToIntFunction<FactoryMonthPlanMouldDayResult> qtyExtractor) {

    if (CollectionUtils.isEmpty(productionResults)) {
      return 0;
    }
    return productionResults.stream()
        .mapToInt(qtyExtractor)
        .filter(qty -> qty > 0)
        .sum();
  }

  /**
   * 按优先级查找销售订单
   */
  private static Optional<DpSimulatedOffsetDetail> findSaleOrderByPriority(
      List<DpSimulatedOffsetDetail> saleOrders,
      String priority) {

    return saleOrders.stream()
        .filter(order -> priority.equals(order.getScmPriority()))
        .findFirst();
  }

  private static Map<String,Integer> calculateProductionQty(List<FactoryMonthPlanMouldDayResult> productionFinalResults) {
    if(CollectionUtils.isEmpty(productionFinalResults)) {
      return Collections.emptyMap();
    }
    return productionFinalResults.stream()
        .filter(Objects::nonNull)
        .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) &&   null != productionFinalResult.getTotalQty())
        .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode, Collectors.summingInt(FactoryMonthPlanMouldDayResult::getTotalQty)));
  }


  /**
   * 优先级处理器 - 封装优先级处理逻辑
   */
  @Getter
  private static class PriorityProcessor {
    private final String priority;
    private final ToIntFunction<FactoryMonthPlanMouldDayResult> productionQtyExtractor;

    public PriorityProcessor(String priority,
                             ToIntFunction<FactoryMonthPlanMouldDayResult> productionQtyExtractor) {
      this.priority = priority;
      this.productionQtyExtractor = productionQtyExtractor;
    }

  }

}
