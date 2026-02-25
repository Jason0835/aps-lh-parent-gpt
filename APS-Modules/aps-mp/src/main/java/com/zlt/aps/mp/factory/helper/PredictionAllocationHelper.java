package com.zlt.aps.mp.factory.helper;


import com.google.common.collect.Lists;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanMonitor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      DpDemandPlan createCondition,
      List<DpOrderOffsetDetail> tMonthDemands,
      List<FactoryMonthPlanMouldDayResult> productionFinalResults,
      List<MpMonthPlanMonitor>  mpMonthPlanMonitors) {
    List<DpOrderOffsetDetail> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(tMonthDemands)) {
      return result;
    }
    Map<String,List<DpOrderOffsetDetail>>  netDemandGroupMap = tMonthDemands.stream().collect(Collectors.groupingBy(DpOrderOffsetDetail::getMaterialCode));
    Map<String,Integer> productionQtyMap = calculateProductionQty(productionFinalResults);
    Map<String,Integer> completionQtyMap = calculateCompletionQty(mpMonthPlanMonitors);
    Map<String,List<FactoryMonthPlanMouldDayResult>> productionGroupMap = getProductionGroupMap(productionFinalResults);
    List<DpOrderOffsetDetail> allocations;
    BigDecimal stockQty;
    List<FactoryMonthPlanMouldDayResult> productionResults;
    int productionQty;
    int completionQty;
    int totalProductionQty;
    // 计算该优先级的总订单数量
    int totalOrderQty;
    for (Map.Entry<String, List<DpOrderOffsetDetail>> entry : netDemandGroupMap.entrySet()) {
      totalOrderQty = calculateTotalOrderQty(entry.getValue());
      if(totalOrderQty <= 0) {
        continue;
      }
      productionQty = productionQtyMap.getOrDefault(entry.getKey(),0);
      if(productionQtyMap.containsKey(entry.getKey()) && productionQty == 0) {
        continue;
      }
      completionQty = completionQtyMap.getOrDefault(entry.getKey(), 0);
      totalProductionQty = productionQty - completionQty;
      if(totalOrderQty <= totalProductionQty) {
         continue;
      }
      stockQty = BigDecimal.valueOf(productionQty - completionQty);
      productionResults = productionGroupMap.get(entry.getKey());
      PredictionAllocationContext context = new PredictionAllocationContext(
          createCondition,
          entry.getValue(),
          stockQty,
          productionResults
      );
      allocations = processOrderGroup(context);
      if(CollectionUtils.isEmpty(allocations)) {
        continue;
      }
      List<String> scmPriorities = Lists.newArrayList(ApsConstant.SAL_PRIORITY_HIGHT,ApsConstant.SAL_PRIORITY_MID,ApsConstant.SAL_PRIORITY_POSTPONE);
      int totalProduceQtyDue =   allocations.stream().filter(item -> scmPriorities.contains(item.getScmPriority()) && null != item.getProduceQtyDue()).mapToInt(DpOrderOffsetDetail::getProduceQtyDue).sum();
      if(totalProduceQtyDue <= 0) {
        continue;
      }
      allocations.forEach(allocation -> {
        log.info("calculateSaleOrder----> monthPlanVersion:{}, materialCode: {},priority:{},totalOrderQty:{},productionQty:{},stockQty:{}",
            allocation.getMonthPlanVersion(),
            allocation.getMaterialCode(),
            allocation.getScmPriority(),
            allocation.getOrderQty(),allocation.getProductionQty(),
            allocation.getStockQty());
      });
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
  private static List<DpOrderOffsetDetail> processOrderGroup(
      PredictionAllocationContext context
      ) {
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
            if(context.getStockQty().compareTo(BigDecimal.ZERO) <= 0) {
               break;
            }
            processPriority(context, processor);
          }
          return context.getSaleOrders();
  }

  /**
   * 处理单一优先级
   */
  private static void processPriority(
      PredictionAllocationContext context,
      PriorityProcessor processor) {

    if(CollectionUtils.isEmpty(context.getSaleOrders())) {
      return;
    }
    List<DpOrderOffsetDetail> saleOrders = context.getSaleOrders();
    // 查找该优先级的销售订单
    List<DpOrderOffsetDetail> saleOrdersByPriority = findSaleOrderByPriority(saleOrders, processor.getPriority());
    if (CollectionUtils.isEmpty(saleOrdersByPriority)) {
      return;
    }
    List<DpOrderOffsetDetail> otherSaleOrders = saleOrders.stream().filter(item -> !processor.getPriority().equals(item.getScmPriority())).collect(Collectors.toList());
    if(CollectionUtils.isEmpty(otherSaleOrders)) {
      otherSaleOrders = Lists.newArrayList();
    }
    // 计算该优先级的总订单数量
    int totalOrderQty = calculateTotalOrderQty(saleOrdersByPriority);
    if(totalOrderQty <= 0) {
      context.setSaleOrders(otherSaleOrders);
      return;
    }
    // 计算该优先级的生产数量
    int productionQty = calculateProductionQty(context.getProductionResults(), processor.getProductionQtyExtractor());
    DpOrderOffsetDetail saleOrder = saleOrdersByPriority.get(0);
    BigDecimal sumOrderQty = BigDecimal.valueOf(totalOrderQty);
    BigDecimal stockQty = context.getStockQty();
    log.info("processPriority ---》 before ---》 monthPlanVersion:{}, materialCode: {},priority:{},totalOrderQty:{},productionQty:{},stockQty:{}",
        context.getCreateCondition().getMonthPlanVersion(),
        saleOrder.getMaterialCode(),
        processor.getPriority(),
        sumOrderQty.intValue(),productionQty,
        stockQty.intValue());
    if(stockQty.compareTo(BigDecimal.ZERO) >= 0) {
      if(stockQty.compareTo(BigDecimal.ZERO) > 0 && stockQty.compareTo(sumOrderQty) >= 0) {
        stockQty = stockQty.subtract(sumOrderQty);
        sumOrderQty = BigDecimal.ZERO;
      }else{
        sumOrderQty = sumOrderQty.subtract(stockQty);
        stockQty = BigDecimal.ZERO;
      }
    }
    context.setStockQty(stockQty);
    if(sumOrderQty.compareTo(BigDecimal.ZERO) <= 0) {
      context.setSaleOrders(otherSaleOrders);
      return;
    }
    log.info("processPriority ---》 after ---》 monthPlanVersion:{}, materialCode: {},priority:{},totalOrderQty:{},productionQty:{},stockQty:{}",
        context.getCreateCondition().getMonthPlanVersion(),
        saleOrder.getMaterialCode(),
        processor.getPriority(),
        sumOrderQty.intValue(),productionQty,
        stockQty.intValue());
    DpDemandPlan createCondition =  context.getCreateCondition();
    DpOrderOffsetDetail entity = new DpOrderOffsetDetail();
    BeanUtils.copyProperties(saleOrder, entity);
    entity.setFactoryCode(createCondition.getFactoryCode());
    entity.setYear(createCondition.getYear());
    entity.setMonth(createCondition.getMonth());
    entity.setMonthPlanVersion(createCondition.getMonthPlanVersion());
    entity.setOrderQty(sumOrderQty.intValue());
    entity.setStockQty(BigDecimal.ZERO.intValue());
    entity.setAllocationQty(BigDecimal.ZERO.intValue());
    entity.setPlannedSurplus(BigDecimal.ZERO.intValue());
    entity.setProduceQtyDue(sumOrderQty.intValue());
    entity.setProductionQty(productionQty);
    entity.setBaseVale(null);
    entity.setId(null);
    otherSaleOrders.add(entity);
    context.setSaleOrders(otherSaleOrders);
  }

  /**
   * 计算总订单数量
   */
  private static int calculateTotalOrderQty(
      List<DpOrderOffsetDetail> saleOrders) {

    return saleOrders.stream()
        .filter(order -> order.getProduceQtyDue()!= null)
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
  private static List<DpOrderOffsetDetail> findSaleOrderByPriority(
      List<DpOrderOffsetDetail> saleOrders,
      String priority) {

    return saleOrders.stream()
        .filter(order -> priority.equals(order.getScmPriority())).collect(Collectors.toList());
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

  /**
   * 库存分配上下文 - 用于跟踪分配过程中的库存状态
   */
  @Setter
  @Getter
  private static class PredictionAllocationContext {
    private DpDemandPlan createCondition;
    private List<DpOrderOffsetDetail> saleOrders;
    private BigDecimal stockQty;
    private List<FactoryMonthPlanMouldDayResult> productionResults;
    public PredictionAllocationContext(DpDemandPlan createCondition,List<DpOrderOffsetDetail> saleOrders,BigDecimal stockQty,List<FactoryMonthPlanMouldDayResult> productionResults) {
      this.createCondition = createCondition;
      this.saleOrders = saleOrders;
      this.stockQty = stockQty;
      this.productionResults = productionResults;
    }
  }

}
