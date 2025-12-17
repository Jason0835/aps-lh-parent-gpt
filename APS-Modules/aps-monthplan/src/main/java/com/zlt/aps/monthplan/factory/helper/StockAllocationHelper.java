package com.zlt.aps.monthplan.factory.helper;


import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.aps.monthplan.api.domain.entity.MpOrderOffsetAllocation;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存分配服务 - 根据库存对冲顺序配置进行智能分配
 * @author Yelq
 */
@Slf4j
public class StockAllocationHelper {
  /**
   * 根据库存对冲顺序配置，进行库存分配
   */
  public static List<MpOrderOffsetAllocation> calculateStockAllocation(
      String monthPlanVersion,
      Map<String, List<SalesOrderPool>> saleOrderGroupMap,
      Map<String,List<MpFinishedProductStock>> finishedProductStockMap,
      Map<String,Long> mdmMonthSurplusMap) {
    List<MpOrderOffsetAllocation> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(saleOrderGroupMap)) {
      return result;
    }
    for (Map.Entry<String, List<SalesOrderPool>> entry : saleOrderGroupMap.entrySet()) {
      String groupKey = entry.getKey();
      List<SalesOrderPool> saleOrders = entry.getValue();

      List<MpOrderOffsetAllocation> groupAllocations = processOrderGroup(
          monthPlanVersion,
          finishedProductStockMap,
          mdmMonthSurplusMap,
          groupKey,
          saleOrders
      );
      result.addAll(groupAllocations);
    }

    return result;
  }

  /**
   * 处理单个订单组的库存分配
   */
  private static List<MpOrderOffsetAllocation> processOrderGroup(
      String monthPlanVersion,
      Map<String,List<MpFinishedProductStock>> finishedProductStockMap,
      Map<String,Long> mdmMonthSurplusMap,
      String groupKey,
      List<SalesOrderPool> saleOrders) {
    List<MpFinishedProductStock> stockInfos = finishedProductStockMap.get(groupKey);
    // 无库存情况处理
    if (CollectionUtils.isEmpty(stockInfos)) {
      log.warn("No stock found for group: {}", groupKey);
      return createZeroAllocations(monthPlanVersion,mdmMonthSurplusMap,groupKey, saleOrders);
    }
    // 获取排序配置并排序订单
    List<SalesOrderPool> sortedOrders = getSortedOrders(saleOrders);
    // 执行库存分配
    StockAllocationResult result = allocateStockForOrders(monthPlanVersion,groupKey,mdmMonthSurplusMap,sortedOrders, stockInfos);
    return result.getAllocations();
  }

  /**
   * 为无库存订单创建零分配记录
   */
  private static List<MpOrderOffsetAllocation> createZeroAllocations(String monthPlanVersion,Map<String,Long> mdmMonthSurplusMap,String groupKey, List<SalesOrderPool> saleOrders) {
    List<MpOrderOffsetAllocation> allocations = new ArrayList<>();
    long plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey,0L);
    if(plannedSurplus == 0L) {
      for (SalesOrderPool order : saleOrders) {
        long orderQty = null == order.getOrdQty()?BigDecimal.ZERO.longValue():order.getOrdQty().longValue();
        allocations.add(buildAllocation(order, monthPlanVersion,0L,plannedSurplus, 0L,orderQty));
      }
      return allocations;
    }
    // 获取排序配置并排序订单
    List<SalesOrderPool> sortedOrders = getSortedOrders(saleOrders);
    // 5、库存冲减后，继续扣减月底计划余量部分
    StockAllocationResult result = allocateMonthSurplusForOrders(monthPlanVersion,plannedSurplus,sortedOrders);
    return result.getAllocations();
  }

  /**
   * 获取排序后的订单列表
   */
  private static List<SalesOrderPool> getSortedOrders(
      List<SalesOrderPool> saleOrders) {
    return saleOrders.stream()
        .sorted(getHighPerformanceComparator())
        .collect(Collectors.toList());
  }

  /**
   * 高性能自定义比较器（适用于大数据量）
   */
  private static Comparator<SalesOrderPool> getHighPerformanceComparator() {
    return new SalesOrderComparator();
  }

  /**
   * 自定义高性能比较器实现
   * 避免重复解析和lambda开销
   */
  private static class SalesOrderComparator implements Comparator<SalesOrderPool> {

    @Override
    public int compare(SalesOrderPool o1, SalesOrderPool o2) {
      // 1. 比较供应链优先级
      int scmPriorityCompare = compareScmPriority(o1, o2);
      if (scmPriorityCompare != 0) {
        return scmPriorityCompare;
      }

      // 2. 比较提报日期
      int dateCompare = compareBillDate(o1, o2);
      if (dateCompare != 0) {
        return dateCompare;
      }

      // 3. 比较提报量
      return compareOrdQty(o1, o2);
    }

    private int compareScmPriority(SalesOrderPool o1, SalesOrderPool o2) {
      Integer p1 = parseScmPriority(o1.getScmPriority());
      Integer p2 = parseScmPriority(o2.getScmPriority());

      if (p1 == null && p2 == null) {
        return 0;
      }
      if (p1 == null) {
        return 1; // null排最后
      }
      if (p2 == null) {
        return -1;
      }

      return Integer.compare(p1, p2);
    }

    private int compareBillDate(SalesOrderPool o1, SalesOrderPool o2) {
      Date d1 = o1.getBillDate();
      Date d2 = o2.getBillDate();

      if (d1 == null && d2 == null) {
        return 0;
      }
      // null排最后
      if (d1 == null) {
        return 1;
      }
      if (d2 == null) {
        return -1;
      }

      return d1.compareTo(d2);
    }

    private int compareOrdQty(SalesOrderPool o1, SalesOrderPool o2) {
      BigDecimal q1 = o1.getOrdQty();
      BigDecimal q2 = o2.getOrdQty();

      if (q1 == null && q2 == null) {
        return 0;
      }
      // null排最后
      if (q1 == null) {
        return 1;
      }
      if (q2 == null) {
        return -1;
      }
      return q1.compareTo(q2);
    }

    private Integer parseScmPriority(String scmPriority) {
      if (scmPriority == null || scmPriority.trim().isEmpty()) {
        return null;
      }
      try {
        return Integer.parseInt(scmPriority.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  /**
   * 为订单列表执行库存分配
   */
  private static StockAllocationResult allocateStockForOrders(String monthPlanVersion,String groupKey,Map<String,Long> mdmMonthSurplusMap,List<SalesOrderPool> sortedOrders,List<MpFinishedProductStock> stockInfos) {
    Long plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey,0L);
    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus,
        stockInfos
    );
    List<MpOrderOffsetAllocation> allocations = new ArrayList<>();
    for (SalesOrderPool order : sortedOrders) {
      MpOrderOffsetAllocation allocation = allocateStockForSingleOrder(monthPlanVersion,order,context);
      allocations.add(allocation);
    }
    return new StockAllocationResult(allocations);
  }

  private static StockAllocationResult allocateMonthSurplusForOrders(String monthPlanVersion, Long plannedSurplus, List<SalesOrderPool> sortedOrders) {
    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus,
        null
    );
    List<MpOrderOffsetAllocation> allocations = new ArrayList<>();
    for (SalesOrderPool order : sortedOrders) {
      MpOrderOffsetAllocation allocation = allocateMonthSurplusForSingleOrder(monthPlanVersion,order,context);
      allocations.add(allocation);
    }
    return new StockAllocationResult(allocations);
  }



  /**
   * 为单个订单分配库存
   */
  private static MpOrderOffsetAllocation allocateStockForSingleOrder(String monthPlanVersion,SalesOrderPool order, StockAllocationContext context) {
    long stockQty = context.getStockInfos().stream().mapToLong(MpFinishedProductStock::getStockQty).sum();
    // 5、库存冲减后，继续扣减月底计划余量部分
    long orderQty = null == order.getOrdQty()?BigDecimal.ZERO.longValue():order.getOrdQty().longValue();
    // 库存分配量
    long allocationQty = calculateAllocationQuantity(order, context);
    Long plannedSurplus =  context.plannedSurplus;
    long produceQtyDue = orderQty - allocationQty;
    if (plannedSurplus.compareTo(produceQtyDue) >= 0) {
      plannedSurplus = plannedSurplus - produceQtyDue;
      produceQtyDue = BigDecimal.ZERO.longValue();
    } else {
      plannedSurplus =  BigDecimal.ZERO.longValue();
      produceQtyDue = produceQtyDue - plannedSurplus;
    }
    context.setPlannedSurplus(plannedSurplus);
    return buildAllocation(order, monthPlanVersion,stockQty,plannedSurplus, allocationQty,produceQtyDue);
  }

  private static MpOrderOffsetAllocation allocateMonthSurplusForSingleOrder(String monthPlanVersion, SalesOrderPool order, StockAllocationContext context) {
    // 5、库存冲减后，继续扣减月底计划余量部分
    long produceQtyDue = null == order.getOrdQty()?BigDecimal.ZERO.longValue():order.getOrdQty().longValue();
    Long plannedSurplus =  context.plannedSurplus;
    if (plannedSurplus.compareTo(produceQtyDue) >= 0) {
      plannedSurplus = plannedSurplus - produceQtyDue;
      produceQtyDue = BigDecimal.ZERO.longValue();
    } else {
      plannedSurplus =  BigDecimal.ZERO.longValue();
      produceQtyDue = produceQtyDue - plannedSurplus;
    }
    context.setPlannedSurplus(plannedSurplus);
    return buildAllocation(order, monthPlanVersion,0,context.plannedSurplus, 0,produceQtyDue);
  }

  /**
   * 计算单个订单的分配数量
   */
  private static long calculateAllocationQuantity(SalesOrderPool order, StockAllocationContext context) {
    List<MpFinishedProductStock> stockInfos = context.getStockInfos();
    // 订单有年周号要求
    if(StringUtils.isNotBlank(order.getWeekYear())) {
        return reduceInventoryByWeekYear(
            order,
            stockInfos);
    }
    //   (3)订单中有动平衡要求的，则可冲减的库存必须是动平衡库存，
    //   (3.3）注：有动平衡、均匀性的，可以冲减库存不带动平衡和均匀性标志的，但需要做提醒；
    if(StringUtils.isNotBlank(order.getDynamicBalance())) {
      return reduceInventory(
          order,
          stockInfos,true,false);
    }
    if(StringUtils.isNotBlank(order.getUniformity())) {
      return reduceInventory(
          order,
          stockInfos,false,true);
    }
    return reduceInventory(
        order,
        stockInfos,false,false);
  }

  /**
   *  订单中有年周号要求的，则库存冲减需取得满足年周号要求的库存，在此基础上年周号越早的优先对冲
   * @param order 订单
   * @param stockInfos 库存
   * @return 冲减结果
   */
  private static long reduceInventoryByWeekYear(SalesOrderPool order, List<MpFinishedProductStock> stockInfos) {
    BigDecimal orderQty = order.getOrdQty();
    if (orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("数量无效，无法冲减库存");
      return BigDecimal.ZERO.longValue();
    }
    int orderWeekYear = Integer.parseInt(order.getWeekYear());
    // 3. 过滤和排序库存
    List<MpFinishedProductStock> eligibleStocks = filterAndSortStocksByWeekYear(stockInfos, orderWeekYear);
    if (CollectionUtils.isEmpty(eligibleStocks)) {
      log.warn("没有符合条件的库存可以冲减");
      return BigDecimal.ZERO.longValue();
    }
    // 4. 执行冲减
    ReductionDetail detail = performReduction(eligibleStocks, orderQty);
    return orderQty.subtract(detail.getRemainingOrderQty()).longValue();
  }

  private static long reduceInventory(SalesOrderPool order, List<MpFinishedProductStock> stockInfos,boolean requiresDynamicBalance,boolean requiresUniformity) {
    BigDecimal orderQty = order.getOrdQty();
    if (orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("数量无效，无法冲减库存");
      return BigDecimal.ZERO.longValue();
    }
    // 3. 过滤和排序库存
    List<MpFinishedProductStock> eligibleStocks = intelligentStockSelection(stockInfos, requiresDynamicBalance,requiresUniformity);
    if (CollectionUtils.isEmpty(eligibleStocks)) {
      log.warn("没有符合条件的库存可以冲减");
      return BigDecimal.ZERO.longValue();
    }
    // 4. 执行冲减
    ReductionDetail detail = performReduction(eligibleStocks, orderQty);
    return orderQty.subtract(detail.getRemainingOrderQty()).longValue();
  }


  /**
   * 高级算法：智能动平衡库存选择（考虑多个维度）
   */
  private static List<MpFinishedProductStock> intelligentStockSelection(
      List<MpFinishedProductStock> stockInfos,boolean requiresDynamicBalance,boolean requiresUniformity) {
    return stockInfos.stream()
        .filter(StockAllocationHelper::isValidStock)
        .sorted(buildIntelligentComparator(requiresDynamicBalance,requiresUniformity))
        .collect(Collectors.toList());
  }

  /**
   * 判断库存是否有效
   */
  private static boolean isValidStock(MpFinishedProductStock stock) {
    return stock.getStockQty() != null
        && stock.getStockQty() > 0;
  }

  /**
   * 构建智能比较器
   */
  private static Comparator<MpFinishedProductStock> buildIntelligentComparator(
      boolean requiresDynamicBalance,boolean requiresUniformity) {
    List<Comparator<MpFinishedProductStock>> comparators = new ArrayList<>();
    if (requiresDynamicBalance) {
      // 优先级1：动平衡库存优先
      comparators.add(Comparator.comparing(
          stock -> isDynamicBalanceStock(stock) ? 0 : 1
      ));
    }
    if (requiresUniformity) {
      // 优先级2：动平衡库存优先
      comparators.add(Comparator.comparing(
          stock -> isUniformityStock(stock) ? 0 : 1
      ));
    }
    // 优先级3：库存数量大的优先（提高冲减效率）
    comparators.add(Comparator.comparing(
        MpFinishedProductStock::getStockQty,
        Comparator.reverseOrder()
    ));
    // 优先级4：库存ID小的优先（先进先出，假设ID递增）
    comparators.add(Comparator.comparing(MpFinishedProductStock::getId));
    // 组合所有比较器
    Comparator<MpFinishedProductStock> result = null;
    for (Comparator<MpFinishedProductStock> comparator : comparators) {
      if (result == null) {
        result = comparator;
      } else {
        result = result.thenComparing(comparator);
      }
    }
    return result != null ? result : Comparator.comparing(MpFinishedProductStock::getId);
  }

  /**
   * 判断库存是否为动平衡库存
   */
  private static boolean isDynamicBalanceStock(MpFinishedProductStock stock) {
    return YesOrNoEnum.YES.getCode().equals(stock.getDynamicBalance());
  }

  /**
   * 判断库存是否为均匀性库存
   */
  private static boolean isUniformityStock(MpFinishedProductStock stock) {
    return YesOrNoEnum.YES.getCode().equals(stock.getUniformity());
  }



  /**
   * 执行冲减逻辑
   */
  private static ReductionDetail performReduction(
      List<MpFinishedProductStock> eligibleStocks,
      BigDecimal orderQty) {
    ReductionDetail detail = new ReductionDetail();
    BigDecimal remainingQty = orderQty;
    List<ReductionItem> reductionItems = new ArrayList<>();
    for (MpFinishedProductStock stock : eligibleStocks) {
      if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      // 获取当前库存数量
      BigDecimal stockQty = BigDecimal.valueOf(stock.getStockQty());
      ReductionItem item = new ReductionItem();
      item.setStockId(stock.getId());
      item.setStockWeekYear(stock.getWeekYear());
      item.setOriginalStockQty(stockQty);
      if (stockQty.compareTo(remainingQty) >= 0) {
        // 当前库存足够冲减
        stock.setStockQty(stockQty.subtract(remainingQty).longValue());
        item.setReducedQty(remainingQty);
        item.setRemainingStockQty(BigDecimal.valueOf(stock.getStockQty()));
        remainingQty = BigDecimal.ZERO;
      } else {
        // 当前库存不足，全部冲减
        stock.setStockQty(0L);
        item.setReducedQty(stockQty);
        item.setRemainingStockQty(BigDecimal.ZERO);
        remainingQty = remainingQty.subtract(stockQty);
      }
      reductionItems.add(item);
    }
    detail.setReductionItems(reductionItems);
    detail.setRemainingOrderQty(remainingQty);
    detail.setFullyReduced(remainingQty.compareTo(BigDecimal.ZERO) == 0);
    return detail;
  }

  private static List<MpFinishedProductStock> filterAndSortStocksByWeekYear(List<MpFinishedProductStock> stockInfos, int orderWeekYear) {
    List<MpFinishedProductStock> result =  stockInfos.stream()
        // 过滤：库存年周号晚于订单年周号
        .filter(stock -> {
          if (StringUtils.isBlank(stock.getWeekYear()) || stock.getStockQty() == null || stock.getStockQty() <= 0) {
            return false;
          }
          int stockWeekYear = Integer.parseInt(stock.getWeekYear());
          stock.setStockWeekYear(stockWeekYear);
          return stockWeekYear < orderWeekYear;
        })
        // 收集为列表
        .collect(Collectors.toList());
      if(CollectionUtils.isEmpty(result)) {
        return Collections.emptyList();
      }
     // 排序：年周号从早到晚（升序）
      result.sort(Comparator.comparing(MpFinishedProductStock::getStockWeekYear));
      return result;
  }

  /**
   * 构建分配记录
   */
  private static MpOrderOffsetAllocation buildAllocation(SalesOrderPool order, String version,long stockQty,long plannedSurplus,long allocationQty,long produceQtyDue) {
    MpOrderOffsetAllocation allocation = new MpOrderOffsetAllocation();
    allocation.setBaseVale(null);
    allocation.setFactoryCode(order.getFactoryCode());
    allocation.setYear(order.getYear());
    allocation.setMonth(order.getMonth());
    allocation.setMonthPlanVersion(version);
    allocation.setProductTypeCode(order.getProductType());
    allocation.setBrand(order.getBrand());
    allocation.setAreaCode(order.getArea());
    allocation.setCustomCode(order.getSalCode());
    // allocation.setCustomName();
    allocation.setCustomNationCode(order.getSalNCode());
    allocation.setDeliverGoodsType(order.getDeliverGoodsType());
    allocation.setDestinationNationCode(order.getNatCode());
    allocation.setDynamicBalance(order.getDynamicBalance());
    allocation.setUniformity(order.getUniformity());
    allocation.setMaterialCode(order.getOriMaterialCode());
    allocation.setMaterialDesc(order.getMaterialDesc());
    allocation.setPoNumber(order.getSalCodePo());
    allocation.setWeekYear(order.getWeekYear());
    allocation.setScmId(order.getScmDetailId());
    allocation.setScmPriority(order.getScmPriority());
    allocation.setPlannedSurplus(plannedSurplus);
    // allocation.setMesMaterialCode();
    // allocation.setLocationType(order);
    allocation.setId(null);
    allocation.setOrderQty(order.getOrdQty().longValue());
    allocation.setStockQty(stockQty);
    allocation.setAllocationQty(allocationQty);
    allocation.setProduceQtyDue(produceQtyDue);
    return allocation;
  }

  /**
   * 库存分配上下文 - 用于跟踪分配过程中的库存状态
   */
  @Setter
  @Getter
  private static class StockAllocationContext {
    private Long plannedSurplus;
    private List<MpFinishedProductStock> stockInfos;

    public StockAllocationContext(Long plannedSurplus,List<MpFinishedProductStock> stockInfos) {
      this.plannedSurplus = plannedSurplus;
      this.stockInfos = stockInfos;
    }
  }

  /**
   * 库存分配结果类（替代Record）
   */
  @Getter
  private static class StockAllocationResult {
    private final List<MpOrderOffsetAllocation> allocations;

    public StockAllocationResult(List<MpOrderOffsetAllocation> allocations) {
      this.allocations = allocations;
    }

  }

  /**
   * 冲减项
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  private static class ReductionItem {
    private Long stockId;
    private String stockWeekYear;
    private BigDecimal originalStockQty;
    private BigDecimal reducedQty;
    private BigDecimal remainingStockQty;
    private LocalDate reductionTime;

  }
  /**
   * 冲减详情
   */
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  private static class ReductionDetail {
    private List<ReductionItem> reductionItems;
    private BigDecimal remainingOrderQty;
    private boolean fullyReduced;
    private BigDecimal totalReducedQty;
    private BigDecimal earliestStockWeekYear;
    private BigDecimal latestStockWeekYear;

  }

}
