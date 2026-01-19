package com.zlt.aps.monthplan.factory.helper;


import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
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
  public static List<DpOrderOffsetDetail> calculateSaleOrder(
      List<DpOrderOffsetDetail> netDemands,
      List<FactoryMonthPlanMouldDayResult> productionFinalResults) {
    List<DpOrderOffsetDetail> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(netDemands)) {
      return result;
    }
    Map<String,List<DpOrderOffsetDetail>>  netDemandGroupMap = netDemands.stream().collect(Collectors.groupingBy(DpOrderOffsetDetail::getMaterialCode));
    Map<String,List<FactoryMonthPlanMouldDayResult>> productionGroupMap = calculateProductionQty(productionFinalResults);
    List<DpOrderOffsetDetail> allocations;
    List<FactoryMonthPlanMouldDayResult> productionResults;
    for (Map.Entry<String, List<DpOrderOffsetDetail>> entry : netDemandGroupMap.entrySet()) {
      productionResults = productionGroupMap.get(entry.getKey());
      allocations = processOrderGroup(entry.getValue(), productionResults);
      if(CollectionUtils.isEmpty(allocations)) {
        continue;
      }
      result.addAll(allocations);
    }
    return result;
  }


  /**
   * 处理单个订单组的库存分配
   */
  private static List<DpOrderOffsetDetail> processOrderGroup(
      List<DpOrderOffsetDetail> saleOrders,
      List<FactoryMonthPlanMouldDayResult> productionResults
      ) {
          List<DpOrderOffsetDetail> result = new ArrayList<>();
          // 定义优先级处理配置
          List<PriorityProcessor> processors = Arrays.asList(
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_HIGHT,
                  FactoryMonthPlanMouldDayResult::getHeightProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_MID,
                  FactoryMonthPlanMouldDayResult::getMidProductionQty),
              new PriorityProcessor(ApsConstant.SAL_PRIORITY_POSTPONE,
                  FactoryMonthPlanMouldDayResult::getPostponeProductionQty)
          );

          // 处理每个优先级
          for (PriorityProcessor processor : processors) {
            processPriority(saleOrders, productionResults, result, processor);
          }
          return result;
  }

  /**
   * 处理单一优先级
   */
  private static void processPriority(
      List<DpOrderOffsetDetail> saleOrders,
      List<FactoryMonthPlanMouldDayResult> productionResults,
      List<DpOrderOffsetDetail> result,
      PriorityProcessor processor) {

    // 查找该优先级的销售订单
    Optional<DpOrderOffsetDetail> saleOrderOpt = findSaleOrderByPriority(saleOrders, processor.getPriority());

    if (!saleOrderOpt.isPresent()) {
      return;
    }

    DpOrderOffsetDetail saleOrder = saleOrderOpt.get();

    // 计算该优先级的生产数量
    int productionQty = calculateProductionQty(productionResults, processor.getProductionQtyExtractor());

    // 计算该优先级的总订单数量
    int totalOrderQty = calculateTotalOrderQty(saleOrders, processor.getPriority());

    // 计算净需求量
    int netDemand = Math.max(totalOrderQty - productionQty, 0);

    // 更新并添加到结果
    saleOrder.setProduceQtyDue(netDemand);
    result.add(saleOrder);
  }

  /**
   * 计算总订单数量
   */
  private static int calculateTotalOrderQty(
      List<DpOrderOffsetDetail> saleOrders,
      String priority) {

    return saleOrders.stream()
        .filter(order -> priority.equals(order.getScmPriority()))
        .filter(order -> order.getProduceQtyDue() != null && order.getProduceQtyDue() > 0)
        .mapToInt(DpOrderOffsetDetail::getProduceQtyDue)
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
  private static Optional<DpOrderOffsetDetail> findSaleOrderByPriority(
      List<DpOrderOffsetDetail> saleOrders,
      String priority) {

    return saleOrders.stream()
        .filter(order -> priority.equals(order.getScmPriority()))
        .filter(order -> order.getProduceQtyDue() != null && order.getProduceQtyDue() > 0)
        .findFirst();
  }

  private static Map<String,List<FactoryMonthPlanMouldDayResult>> calculateProductionQty(List<FactoryMonthPlanMouldDayResult> productionFinalResults) {
    if(CollectionUtils.isEmpty(productionFinalResults)) {
      return Collections.emptyMap();
    }
    return productionFinalResults.stream()
        .filter(Objects::nonNull)
        .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()))
        .collect(Collectors.groupingBy(FactoryMonthPlanMouldDayResult::getMaterialCode));
  }


  public static Map<String,List<MpMonthPlanMonitor>> calculateCompleteQty(List<MpMonthPlanMonitor> mpMonthPlanMonitors) {
    if(CollectionUtils.isEmpty(mpMonthPlanMonitors)) {
      return Collections.emptyMap();
    }
    return mpMonthPlanMonitors.stream()
        .filter(Objects::nonNull)
        .filter(monthPlanMonitor -> StringUtils.isNotBlank(monthPlanMonitor.getMaterialCode()))
        .collect(Collectors.groupingBy(MpMonthPlanMonitor::getMaterialCode));
  }

  public static List<SupplyOrderPool> calculateSupplyOrder(List<SupplyOrderPool> cycleStockOrders, List<FactoryMonthPlanMouldDayResult> productionFinalResults, List<MpMonthPlanMonitor> mpMonthPlanMonitors) {
    List<SupplyOrderPool> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(cycleStockOrders)) {
      return result;
    }
    Map<String,List<SupplyOrderPool>>  netDemandGroupMap = cycleStockOrders.stream().collect(Collectors.groupingBy(SupplyOrderPool::getMaterialCode));
    Map<String,List<FactoryMonthPlanMouldDayResult>> productionGroupMap = calculateProductionQty(productionFinalResults);
    Map<String,List<MpMonthPlanMonitor>> completionGroupMap = calculateCompleteQty(mpMonthPlanMonitors);
    List<SupplyOrderPool> allocations;
    List<FactoryMonthPlanMouldDayResult> productionResults;
    List<MpMonthPlanMonitor> completionResults;
    for (Map.Entry<String, List<SupplyOrderPool>> entry : netDemandGroupMap.entrySet()) {
      productionResults = productionGroupMap.get(entry.getKey());
      completionResults = completionGroupMap.get(entry.getKey());
      allocations = processSupplyOrderGroup(entry.getValue(), productionResults,completionResults);
      result.addAll(allocations);
    }
    return result;
  }

  private static void resetCompletionResults(List<MpMonthPlanMonitor> completionResults) {
    if(CollectionUtils.isEmpty(completionResults)) {
      return;
    }
    completionResults.forEach(mpMonthPlanMonitor -> mpMonthPlanMonitor.setProductionQty(BigDecimal.ZERO.intValue()));
  }

  private static List<SupplyOrderPool> processSupplyOrderGroup(List<SupplyOrderPool> supplyOrderPools, List<FactoryMonthPlanMouldDayResult> productionResults, List<MpMonthPlanMonitor> completionResults) {
    List<SupplyOrderPool> result = new ArrayList<>();
    // 定义优先级处理配置
    List<PriorityProcessor> processors = Arrays.asList(
        new PriorityProcessor(ApsConstant.SAL_PRIORITY_CYCLE_STOCK_UP,
            FactoryMonthPlanMouldDayResult::getCycleProductionQty),
        new PriorityProcessor(ApsConstant.SAL_PRIORITY_PRECEDENT_STOCK_UP,
            FactoryMonthPlanMouldDayResult::getConventionProductionQty)
    );
    // 处理每个优先级
    for (PriorityProcessor processor : processors) {
      processSupplyPriority(supplyOrderPools, productionResults, completionResults,result, processor);
    }
    return result;
  }

  private static void processSupplyPriority(List<SupplyOrderPool> supplyOrderPools, List<FactoryMonthPlanMouldDayResult> productionResults, List<MpMonthPlanMonitor> completionResults, List<SupplyOrderPool> result, PriorityProcessor processor) {
    // 查找该优先级的销售订单
    Optional<SupplyOrderPool> supplyOrderOpt = findSupplyOrderByPriority(supplyOrderPools, processor.getPriority());
    if (!supplyOrderOpt.isPresent()) {
      return;
    }
    SupplyOrderPool supplyOrder = supplyOrderOpt.get();
    // 计算该优先级的生产数量
    int productionQty = calculateProductionQty(productionResults, processor.getProductionQtyExtractor());
    // 计算该优先级的总订单数量
    int totalSupplyOrderQty = calculateTotalSupplyOrderQty(supplyOrderPools, processor.getPriority());
    int totalCompleteQty = calculateTotalCompleteQty(completionResults);
    // 计算净需求量
    int netDemand = Math.max(totalSupplyOrderQty - productionQty + totalCompleteQty, 0);
    // 更新并添加到结果
    supplyOrder.setQty(netDemand);
    result.add(supplyOrder);
    resetCompletionResults(completionResults);
  }

  private static int calculateTotalCompleteQty(List<MpMonthPlanMonitor> completionResults) {
    if(CollectionUtils.isEmpty(completionResults)) {
      return 0;
    }
    return completionResults.stream().filter(item -> null != item.getProductionQty()).mapToInt(MpMonthPlanMonitor::getProductionQty).sum();
  }

  private static int calculateTotalSupplyOrderQty(List<SupplyOrderPool> supplyOrderPools, String priority) {
    return supplyOrderPools.stream()
        .filter(order -> priority.equals(order.getOrderType()))
        .filter(order -> order.getQty() != null && order.getQty() > 0)
        .mapToInt(SupplyOrderPool::getQty)
        .sum();
  }

  private static Optional<SupplyOrderPool> findSupplyOrderByPriority(List<SupplyOrderPool> supplyOrderPools, String priority) {
    return supplyOrderPools.stream()
        .filter(order -> priority.equals(order.getOrderType()))
        .filter(order -> order.getQty() != null && order.getQty() > 0)
        .findFirst();
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
