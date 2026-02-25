package com.zlt.aps.mp.factory.helper;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 库存分配服务
 * @author Yelq
 */
@Slf4j
public class StockAllocationHelper {

  private static final String YES_CODE = YesOrNoEnum.YES.getCode();

  public static List<DpOrderOffsetDetail> calculateStockAllocation(
      String monthPlanVersion,
      YearMonth yearMonth,
      Map<String, List<SalesOrderPool>> saleOrderGroupMap,
      Map<String, List<MdmProductStock>> finishedProductStockMap,
      Map<String, Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap,
      String weekYearForEudr) {

    if (CollectionUtils.isEmpty(saleOrderGroupMap)) {
      return Collections.emptyList();
    }

    return saleOrderGroupMap.entrySet().parallelStream()
        .flatMap(entry -> {
          String groupKey = entry.getKey();
          List<SalesOrderPool> saleOrders = entry.getValue();
          List<DpOrderOffsetDetail> groupAllocations = processOrderGroup(
              monthPlanVersion, yearMonth,
              finishedProductStockMap, mdmMonthSurplusMap,
              materialInfoMap, groupKey, saleOrders, weekYearForEudr);
          return groupAllocations.stream();
        })
        .collect(Collectors.toList());
  }

  // ========== 订单组处理 ==========
  private static List<DpOrderOffsetDetail> processOrderGroup(
      String monthPlanVersion, YearMonth yearMonth,
      Map<String, List<MdmProductStock>> finishedProductStockMap,
      Map<String, Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap,
      String groupKey,
      List<SalesOrderPool> saleOrders,
      String weekYearForEudr) {

    // 订单只排序一次
    List<SalesOrderPool> sortedOrders = getSortedOrders(saleOrders);

    List<MdmProductStock> stockInfos = finishedProductStockMap.get(groupKey);
    if (CollectionUtils.isEmpty(stockInfos)) {
      log.warn("No stock found for group: {}", groupKey);
      return createZeroAllocations(monthPlanVersion, yearMonth,
          mdmMonthSurplusMap, materialInfoMap, groupKey, sortedOrders);
    }

    // 预计算库存索引
    StockIndex stockIndex = buildStockIndex(stockInfos, weekYearForEudr);

    // 执行分配（传入已排序订单）
    return allocateStockForOrders(monthPlanVersion, yearMonth, groupKey,
        mdmMonthSurplusMap, materialInfoMap, sortedOrders, stockIndex, weekYearForEudr)
        .getAllocations();
  }

  // ========== 订单排序 ==========
  private static List<SalesOrderPool> getSortedOrders(List<SalesOrderPool> orders) {
    if (orders.size() <= 1) {
      return orders;
    }
    List<SalesOrderPool> copy = new ArrayList<>(orders);
    copy.sort(new SalesOrderComparator());
    return copy;
  }

  private static class SalesOrderComparator implements Comparator<SalesOrderPool> {
    @Override
    public int compare(SalesOrderPool o1, SalesOrderPool o2) {
      // 1. 供应链优先级（缓存）
      Integer p1 = o1.getCachedScmPriority();
      Integer p2 = o2.getCachedScmPriority();
      if (p1 != null || p2 != null) {
        if (p1 == null) {
          return 1;
        }
        if (p2 == null) {
          return -1;
        }
        int cmp = Integer.compare(p1, p2);
        if (cmp != 0) {
          return cmp;
        }
      }
      // 2. 提报日期
      Date d1 = o1.getBillDate();
      Date d2 = o2.getBillDate();
      if (d1 != null || d2 != null) {
        if (d1 == null) {
          return 1;
        }
        if (d2 == null) {
          return -1;
        }
        int cmp = d1.compareTo(d2);
        if (cmp != 0) {
          return cmp;
        }
      }
      // 3. 订单量
      BigDecimal q1 = o1.getOrdQty();
      BigDecimal q2 = o2.getOrdQty();
      if (q1 != null || q2 != null) {
        if (q1 == null) {
          return 1;
        }
        if (q2 == null) {
          return -1;
        }
        return q1.compareTo(q2);
      }
      return 0;
    }
  }

  // ========== 库存索引构建 ==========
  private static StockIndex buildStockIndex(List<MdmProductStock> stockInfos, String weekYearForEudr) {
    // 1. 按年周号升序排序（并缓存转换值）
    List<MdmProductStock> sortedByWeekYear = stockInfos.stream()
        .sorted(Comparator.comparingInt(s -> {
          Integer wy = s.getCachedWeekYearInt();
          return wy != null ? wy : Integer.MAX_VALUE;
        }))
        .collect(Collectors.toList());

    // 2. 按动平衡/均匀性分类
    List<MdmProductStock> dynamicBalanceOnly = new ArrayList<>();
    List<MdmProductStock> uniformityOnly = new ArrayList<>();
    List<MdmProductStock> both = new ArrayList<>();
    List<MdmProductStock> none = new ArrayList<>();

    for (MdmProductStock s : stockInfos) {
      boolean db = s.isDynamicBalanceCached();
      boolean uf = s.isUniformityCached();
      if (db && uf) {
        both.add(s);
      } else if (db) {
        dynamicBalanceOnly.add(s);
      } else if (uf) {
        uniformityOnly.add(s);
      } else {
        none.add(s);
      }
    }

    // 3. EUDR 库存预过滤（按年周号）
    int eudrThreshold = -1;
    if (StringUtils.isNotBlank(weekYearForEudr)) {
      String transformed = weekYearForEudr.substring(2) + weekYearForEudr.substring(0, 2);
      eudrThreshold = Integer.parseInt(transformed);
    }
    final int threshold = eudrThreshold;
    List<MdmProductStock> eudrStocks = (threshold == -1) ? Collections.emptyList() :
        sortedByWeekYear.stream()
            .filter(s -> {
              Integer wy = s.getCachedWeekYearInt();
              return wy != null && wy >= threshold;
            })
            .collect(Collectors.toList());

    return new StockIndex(sortedByWeekYear, dynamicBalanceOnly, uniformityOnly,
        both, none, eudrStocks);
  }

  // ========== 无库存组处理 ==========
  private static List<DpOrderOffsetDetail> createZeroAllocations(
      String monthPlanVersion, YearMonth yearMonth,
      Map<String, Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap,
      String groupKey,
      List<SalesOrderPool> sortedOrders) {

    int plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey, 0);
    if (plannedSurplus == 0) {
      List<DpOrderOffsetDetail> allocations = new ArrayList<>(sortedOrders.size());
      for (SalesOrderPool order : sortedOrders) {
        int orderQty = order.getOrdQty() == null ? 0 : order.getOrdQty().intValue();
        allocations.add(buildAllocation(order, materialInfoMap, monthPlanVersion,
            yearMonth, 0, 0, 0, orderQty));
      }
      return allocations;
    }
    // 有余量，分配余量（已排序订单）
    return allocateMonthSurplusForOrders(monthPlanVersion, yearMonth,
        plannedSurplus, materialInfoMap, sortedOrders).getAllocations();
  }

  // ========== 主分配逻辑 ==========
  private static StockAllocationResult allocateStockForOrders(
      String monthPlanVersion, YearMonth yearMonth, String groupKey,
      Map<String, Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap,
      List<SalesOrderPool> sortedOrders,
      StockIndex stockIndex,
      String weekYearForEudr) {

    int plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey, 0);
    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus, stockIndex, materialInfoMap, weekYearForEudr);

    List<DpOrderOffsetDetail> allocations = new ArrayList<>(sortedOrders.size());
    for (SalesOrderPool order : sortedOrders) {
      allocations.add(allocateStockForSingleOrder(
          monthPlanVersion, yearMonth, order, context));
    }
    return new StockAllocationResult(allocations);
  }

  private static DpOrderOffsetDetail allocateStockForSingleOrder(
      String monthPlanVersion, YearMonth yearMonth,
      SalesOrderPool order, StockAllocationContext context) {

    int orderQty = order.getOrdQty() == null ? 0 : order.getOrdQty().intValue();
    List<MdmProductStock> matchStocks = filterStocksFast(order, context);

    // 计算库存总量
    int stockQty = 0;
    for (MdmProductStock s : matchStocks) {
      Integer qty = s.getLeftOverQty();
      if (qty != null) {
        stockQty += qty;
      }
    }

    int allocationQty = allocateFromStocks(orderQty, matchStocks);
    int produceQtyDue = orderQty - allocationQty;
    int plannedSurplusUsed = 0;

    if (produceQtyDue > 0 && context.getPlannedSurplus() > 0) {
      int surplus = context.getPlannedSurplus();
      if (surplus >= produceQtyDue) {
        plannedSurplusUsed = produceQtyDue;
        context.setPlannedSurplus(surplus - produceQtyDue);
        produceQtyDue = 0;
      } else {
        plannedSurplusUsed = surplus;
        produceQtyDue -= surplus;
        context.setPlannedSurplus(0);
      }
    }

    return buildAllocation(order, context.getMaterialInfoMap(), monthPlanVersion,
        yearMonth, stockQty, plannedSurplusUsed, allocationQty, produceQtyDue);
  }

  /**
   *  库存分配
   * @param orderQty
   * @param stocks
   * @return
   */
  private static int allocateFromStocks(int orderQty, List<MdmProductStock> stocks) {
    if (orderQty == 0 || CollectionUtils.isEmpty(stocks)) {
      return 0;
    }
    int remaining = orderQty;
    for (MdmProductStock stock : stocks) {
      if (remaining <= 0) {
        break;
      }
      Integer stockQty = stock.getLeftOverQty();
      if (stockQty == null || stockQty <= 0) {
        continue;
      }
      if (stockQty >= remaining) {
        stock.setLeftOverQty(stockQty - remaining);
        remaining = 0;
      } else {
        stock.setLeftOverQty(0);
        remaining -= stockQty;
      }
    }
    return orderQty - remaining;
  }

  // ========== 快速库存筛选 ==========
  private static List<MdmProductStock> filterStocksFast(
      SalesOrderPool order, StockAllocationContext context) {

    StockIndex idx = context.getStockIndex();

    // 1. EUDR
    if (YES_CODE.equals(order.getIsEudr())) {
      return filterValidStocks(idx.eudrStocks);
    }

    // 2. 年周号要求（使用二分查找，零警告）
    Integer orderWeekYear = order.getCachedWeekYearInt();
    if (orderWeekYear != null && orderWeekYear > 0) {
      MdmProductStock dummy = new MdmProductStock();
      // 临时设置，仅用于查找
      dummy.setCachedWeekYearInt(orderWeekYear);
      List<MdmProductStock> sorted = idx.sortedByWeekYear;
      int pos = Collections.binarySearch(sorted, dummy,
          Comparator.comparingInt(MdmProductStock::getCachedWeekYearInt));
      if (pos < 0) {
        pos = -pos - 1;
      }
      if (pos >= sorted.size()) {
        return Collections.emptyList();
      }
      return filterValidStocks(sorted.subList(pos, sorted.size()));
    }

    // 3. 动平衡/均匀性
    boolean requireDb = StringUtils.isNotBlank(order.getIsDynamicBalance());
    boolean requireUf = StringUtils.isNotBlank(order.getIsUniformity());

    List<MdmProductStock> candidates;
    if (requireDb && requireUf) {
      candidates = idx.both;
    } else if (requireDb) {
      candidates = idx.dynamicBalanceOnly;
    } else if (requireUf) {
      candidates = idx.uniformityOnly;
    } else {
      candidates = idx.none;
    }

    List<MdmProductStock> valid = filterValidStocks(candidates);
    if (valid.size() > 1) {
      valid.sort(new ProductStockComparator(requireDb, requireUf));
    }
    return valid;
  }

  private static List<MdmProductStock> filterValidStocks(List<MdmProductStock> stocks) {
    if (CollectionUtils.isEmpty(stocks)) {
      return Collections.emptyList();
    }
    List<MdmProductStock> result = new ArrayList<>(stocks.size());
    for (MdmProductStock s : stocks) {
      if (s.getLeftOverQty() != null && s.getLeftOverQty() > 0) {
        result.add(s);
      }
    }
    return result;
  }

  // ========== 库存比较器 ==========
  private static class ProductStockComparator implements Comparator<MdmProductStock> {
    private final boolean requiresDynamicBalance;
    private final boolean requiresUniformity;

    ProductStockComparator(boolean requiresDynamicBalance, boolean requiresUniformity) {
      this.requiresDynamicBalance = requiresDynamicBalance;
      this.requiresUniformity = requiresUniformity;
    }

    @Override
    public int compare(MdmProductStock o1, MdmProductStock o2) {
      int cmp;

      if (requiresDynamicBalance) {
        cmp = Boolean.compare(o2.isDynamicBalanceCached(), o1.isDynamicBalanceCached());
        if (cmp != 0) {
          return cmp;
        }
        cmp = Boolean.compare(o2.isUniformityCached(), o1.isUniformityCached());
        if (cmp != 0) {
          return -cmp;
        }
      }

      if (requiresUniformity && !requiresDynamicBalance) {
        cmp = Boolean.compare(o2.isUniformityCached(), o1.isUniformityCached());
        if (cmp != 0) {
          return cmp;
        }
        cmp = Boolean.compare(o2.isDynamicBalanceCached(), o1.isDynamicBalanceCached());
        if (cmp != 0) {
          return -cmp;
        }
      }

      // 年周号（早的优先）
      cmp = Integer.compare(
          o1.getCachedWeekYearInt() != null ? o1.getCachedWeekYearInt() : Integer.MAX_VALUE,
          o2.getCachedWeekYearInt() != null ? o2.getCachedWeekYearInt() : Integer.MAX_VALUE);
      if (cmp != 0) {
        return cmp;
      }

      // 剩余量（大的优先）
      cmp = Integer.compare(
          o2.getLeftOverQty() != null ? o2.getLeftOverQty() : 0,
          o1.getLeftOverQty() != null ? o1.getLeftOverQty() : 0);
      if (cmp != 0) {
        return cmp;
      }

      return Long.compare(o1.getId(), o2.getId());
    }
  }

  // ========== 月底余量分配 ==========
  private static StockAllocationResult allocateMonthSurplusForOrders(
      String monthPlanVersion, YearMonth yearMonth,
      Integer plannedSurplus,
      Map<String, MdmMaterialInfo> materialInfoMap,
      List<SalesOrderPool> sortedOrders) {

    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus, null, materialInfoMap, null);

    List<DpOrderOffsetDetail> allocations = new ArrayList<>(sortedOrders.size());
    for (SalesOrderPool order : sortedOrders) {
      int produceQtyDue = order.getOrdQty() == null ? 0 : order.getOrdQty().intValue();
      int plannedSurplusUsed = 0;

      if (produceQtyDue > 0 && context.getPlannedSurplus() > 0) {
        int surplus = context.getPlannedSurplus();
        if (surplus >= produceQtyDue) {
          plannedSurplusUsed = produceQtyDue;
          context.setPlannedSurplus(surplus - produceQtyDue);
          produceQtyDue = 0;
        } else {
          plannedSurplusUsed = surplus;
          produceQtyDue -= surplus;
          context.setPlannedSurplus(0);
        }
      }

      allocations.add(buildAllocation(order, materialInfoMap, monthPlanVersion,
          yearMonth, 0, plannedSurplusUsed, 0, produceQtyDue));
    }
    return new StockAllocationResult(allocations);
  }

  // ========== 构建分配记录 ==========
  private static DpOrderOffsetDetail buildAllocation(
      SalesOrderPool order,
      Map<String, MdmMaterialInfo> materialInfoMap,
      String version,
      YearMonth yearMonth,
      int stockQty,
      int plannedSurplus,
      int allocationQty,
      int produceQtyDue) {

    DpOrderOffsetDetail allocation = new DpOrderOffsetDetail();
    BeanUtils.copyProperties(order, allocation);
    allocation.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
    allocation.setYear(yearMonth.getYear());
    allocation.setMonth(yearMonth.getMonthValue());
    allocation.setId(null);
    allocation.setBaseVale(null);
    allocation.setMonthPlanVersion(version);
    allocation.setProductTypeCode(order.getProductType());
    allocation.setAreaCode(order.getArea());
    allocation.setCustomCode(order.getSalCode());
    allocation.setCustomNationCode(order.getSalNCode());
    allocation.setDestinationNationCode(order.getNatCode());
    allocation.setMaterialCode(order.getOriMaterialCode());
    allocation.setPoNumber(order.getSalCodePo());
    allocation.setScmId(order.getScmDetailId());
    allocation.setPlannedSurplus(plannedSurplus);
    allocation.setOrderQty(order.getOrdQty() == null ? 0 : order.getOrdQty().intValue());
    allocation.setStockQty(stockQty);
    allocation.setAllocationQty(allocationQty);
    allocation.setProduceQtyDue(produceQtyDue);

    MdmMaterialInfo materialInfo = materialInfoMap.get(order.getOriMaterialCode());
    if (materialInfo != null) {
      allocation.setSpecifications(materialInfo.getSpecifications());
      allocation.setPattern(materialInfo.getPattern());
    }
    return allocation;
  }

  // ========== 辅助内部类 ==========
  @lombok.Getter
  private static class StockIndex {
    private final List<MdmProductStock> sortedByWeekYear;
    private final List<MdmProductStock> dynamicBalanceOnly;
    private final List<MdmProductStock> uniformityOnly;
    private final List<MdmProductStock> both;
    private final List<MdmProductStock> none;
    private final List<MdmProductStock> eudrStocks;

    StockIndex(List<MdmProductStock> sortedByWeekYear,
               List<MdmProductStock> dynamicBalanceOnly,
               List<MdmProductStock> uniformityOnly,
               List<MdmProductStock> both,
               List<MdmProductStock> none,
               List<MdmProductStock> eudrStocks) {
      this.sortedByWeekYear = sortedByWeekYear;
      this.dynamicBalanceOnly = dynamicBalanceOnly;
      this.uniformityOnly = uniformityOnly;
      this.both = both;
      this.none = none;
      this.eudrStocks = eudrStocks;
    }
  }

  @lombok.Setter
  @lombok.Getter
  private static class StockAllocationContext {
    private int plannedSurplus;
    private final StockIndex stockIndex;
    private final Map<String, MdmMaterialInfo> materialInfoMap;
    private final String weekYearForEudr;

    StockAllocationContext(int plannedSurplus, StockIndex stockIndex,
                           Map<String, MdmMaterialInfo> materialInfoMap,
                           String weekYearForEudr) {
      this.plannedSurplus = plannedSurplus;
      this.stockIndex = stockIndex;
      this.materialInfoMap = materialInfoMap;
      this.weekYearForEudr = weekYearForEudr;
    }
  }

  @lombok.Getter
  private static class StockAllocationResult {
    private final List<DpOrderOffsetDetail> allocations;
    StockAllocationResult(List<DpOrderOffsetDetail> allocations) {
      this.allocations = allocations;
    }
  }
}