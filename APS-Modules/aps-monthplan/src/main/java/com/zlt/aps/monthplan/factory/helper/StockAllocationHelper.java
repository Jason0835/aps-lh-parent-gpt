package com.zlt.aps.monthplan.factory.helper;


import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.utils.ApsCommonUtil;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;

import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
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

  private final static String ZERO_YEAR_WEEK = "0000";
  /**
   * 根据库存对冲顺序配置，进行库存分配
   */
  public static List<DpOrderOffsetDetail> calculateStockAllocation(
      String monthPlanVersion,
      YearMonth yearMonth,
      Map<String, List<SalesOrderPool>> saleOrderGroupMap,
      Map<String,List<MdmProductStock>> finishedProductStockMap,
      Map<String,Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap) {
    List<DpOrderOffsetDetail> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(saleOrderGroupMap)) {
      return result;
    }
    for (Map.Entry<String, List<SalesOrderPool>> entry : saleOrderGroupMap.entrySet()) {
      String groupKey = entry.getKey();
      List<SalesOrderPool> saleOrders = entry.getValue();

      List<DpOrderOffsetDetail> groupAllocations = processOrderGroup(
          monthPlanVersion,
          yearMonth,
          finishedProductStockMap,
          mdmMonthSurplusMap,
          materialInfoMap,
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
  private static List<DpOrderOffsetDetail> processOrderGroup(
      String monthPlanVersion,
      YearMonth yearMonth,
      Map<String,List<MdmProductStock>> finishedProductStockMap,
      Map<String,Integer> mdmMonthSurplusMap,
      Map<String, MdmMaterialInfo> materialInfoMap,
      String groupKey,
      List<SalesOrderPool> saleOrders) {
    List<MdmProductStock> stockInfos = finishedProductStockMap.get(groupKey);
    // 无库存情况处理
    if (CollectionUtils.isEmpty(stockInfos)) {
      log.warn("No stock found for group: {}", groupKey);
      return createZeroAllocations(monthPlanVersion,yearMonth,mdmMonthSurplusMap,materialInfoMap,groupKey, saleOrders);
    }
    // 获取排序配置并排序订单
    List<SalesOrderPool> sortedOrders = getSortedOrders(saleOrders);
    // 执行库存分配
    StockAllocationResult result = allocateStockForOrders(monthPlanVersion,yearMonth,groupKey,mdmMonthSurplusMap,materialInfoMap,sortedOrders, stockInfos);
    return result.getAllocations();
  }

  /**
   * 为无库存订单创建零分配记录
   */
  private static List<DpOrderOffsetDetail> createZeroAllocations(String monthPlanVersion,YearMonth yearMonth,Map<String,Integer> mdmMonthSurplusMap,Map<String, MdmMaterialInfo> materialInfoMap,String groupKey, List<SalesOrderPool> saleOrders) {
    List<DpOrderOffsetDetail> allocations = new ArrayList<>();
    int plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey,0);
    if(plannedSurplus == 0L) {
      for (SalesOrderPool order : saleOrders) {
        int orderQty = null == order.getOrdQty()?BigDecimal.ZERO.intValue():order.getOrdQty().intValue();
        allocations.add(buildAllocation(order, materialInfoMap,monthPlanVersion,yearMonth,BigDecimal.ZERO.intValue(),BigDecimal.ZERO.intValue(), BigDecimal.ZERO.intValue(),orderQty));
      }
      return allocations;
    }
    // 获取排序配置并排序订单
    List<SalesOrderPool> sortedOrders = getSortedOrders(saleOrders);
    // 5、库存冲减后，继续扣减月底计划余量部分
    StockAllocationResult result = allocateMonthSurplusForOrders(monthPlanVersion,yearMonth,plannedSurplus,materialInfoMap,sortedOrders);
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
   * 高性能自定义比较器（适用于大数据量）
   */
  private static Comparator<MdmProductStock> getHighPerformanceComparator(boolean requireBalance,boolean requireUniformity) {
    return new ProductStockComparator(requireBalance,requireUniformity);
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
   * 自定义高性能比较器实现
   * 避免重复解析和lambda开销
   */
  private static class ProductStockComparator implements Comparator<MdmProductStock> {
    private final boolean requiresDynamicBalance;
    private final boolean requiresUniformity;

    public ProductStockComparator(boolean requiresDynamicBalance, boolean requiresUniformity) {
      this.requiresDynamicBalance = requiresDynamicBalance;
      this.requiresUniformity = requiresUniformity;
    }

    @Override
    public int compare(MdmProductStock o1, MdmProductStock o2) {
        // 优先级1: 动平衡优先
        if(requiresDynamicBalance) {
            // 1. 比较供应链优先级
            Integer dynamicBalanceCompare1 = isDynamicBalanceStock(o1) ? 0 : 1;
            Integer dynamicBalanceCompare2 = isDynamicBalanceStock(o2) ? 0 : 1;
            if(!dynamicBalanceCompare1.equals(dynamicBalanceCompare2)) {
                return dynamicBalanceCompare1.compareTo(dynamicBalanceCompare2);
            }
            Integer uniformityCompare1 = !isUniformityStock(o1) ? 0 : 1;
            Integer uniformityCompare2 = !isUniformityStock(o2) ? 0 : 1;
            if(!uniformityCompare1.equals(uniformityCompare2)) {
              return uniformityCompare1.compareTo(uniformityCompare2);
            }
            Integer yearWeekCompare = compareYearWeek(o1, o2);
            if (yearWeekCompare != 0) {
              return yearWeekCompare;
            }
            Integer leftStockQtyCompare = compareLeftStockQty(o1,o2);
            if (leftStockQtyCompare != 0) {
              return leftStockQtyCompare;
            }
            return o1.getId().compareTo(o2.getId());
        }

        if(requiresUniformity) {
          Integer uniformityCompare1 = isUniformityStock(o1) ? 0 : 1;
          Integer uniformityCompare2 = isUniformityStock(o2) ? 0 : 1;
          if(!uniformityCompare1.equals(uniformityCompare2)) {
            return uniformityCompare1.compareTo(uniformityCompare2);
          }
          Integer dynamicBalanceCompare1 = !isDynamicBalanceStock(o1) ? 0 : 1;
          Integer dynamicBalanceCompare2 = !isDynamicBalanceStock(o2) ? 0 : 1;
          if(!dynamicBalanceCompare1.equals(dynamicBalanceCompare2)) {
            return dynamicBalanceCompare1.compareTo(dynamicBalanceCompare2);
          }
          Integer yearWeekCompare = compareYearWeek(o1, o2);
          if (yearWeekCompare != 0) {
            return yearWeekCompare;
          }
          Integer leftStockQtyCompare = compareLeftStockQty(o1,o2);
          if (leftStockQtyCompare != 0) {
            return leftStockQtyCompare;
          }
          return o1.getId().compareTo(o2.getId());
        }

      Integer uniformityCompare1 = !isUniformityStock(o1) ? 0 : 1;
      Integer uniformityCompare2 = !isUniformityStock(o2) ? 0 : 1;
      if(!uniformityCompare1.equals(uniformityCompare2)) {
        return uniformityCompare1.compareTo(uniformityCompare2);
      }
      Integer dynamicBalanceCompare1 = !isDynamicBalanceStock(o1) ? 0 : 1;
      Integer dynamicBalanceCompare2 = !isDynamicBalanceStock(o2) ? 0 : 1;
      if(!dynamicBalanceCompare1.equals(dynamicBalanceCompare2)) {
        return dynamicBalanceCompare1.compareTo(dynamicBalanceCompare2);
      }
      Integer yearWeekCompare = compareYearWeek(o1, o2);
      if (yearWeekCompare != 0) {
        return yearWeekCompare;
      }
      Integer leftStockQtyCompare = compareLeftStockQty(o1,o2);
      if (leftStockQtyCompare != 0) {
        return leftStockQtyCompare;
      }
      return o1.getId().compareTo(o2.getId());
    }

    private Integer compareLeftStockQty(MdmProductStock o1, MdmProductStock o2) {
        Integer leftStockQty = o1.getLeftOverQty();
        Integer rightStockQty = o2.getLeftOverQty();
        if (leftStockQty == null && rightStockQty == null) {
          return 0;
        }
        if (leftStockQty == null) {
          return 1;
        }
        if (rightStockQty == null) {
          return -1;
        }
        return rightStockQty.compareTo(leftStockQty);
    }

    private Integer compareYearWeek(MdmProductStock o1, MdmProductStock o2) {
      Integer p1 = parseParam(o1.getWeekYear());
      Integer p2 = parseParam(o2.getWeekYear());
      if (p1 == null && p2 == null) {
        return 0;
      }
      if (p1 == null) {
        return -1; // null排最后
      }
      if (p2 == null) {
        return 1;
      }
      String transformed1 = o1.getWeekYear().substring(2) + o1.getWeekYear().substring(0,2);
      String transformed2 = o2.getWeekYear().substring(2) + o2.getWeekYear().substring(0,2);
      p1 = parseParam(transformed1);
      p2 = parseParam(transformed2);
      return Integer.compare(p1, p2);
    }

    private Integer parseParam(String param) {
      if (param == null || param.trim().isEmpty()) {
        return null;
      }
      try {
        return Integer.parseInt(param.trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
  }

  /**
   * 为订单列表执行库存分配
   */
  private static StockAllocationResult allocateStockForOrders(String monthPlanVersion,YearMonth yearMonth,String groupKey,Map<String,Integer> mdmMonthSurplusMap,Map<String, MdmMaterialInfo> materialInfoMap,List<SalesOrderPool> sortedOrders,List<MdmProductStock> stockInfos) {
    int plannedSurplus = mdmMonthSurplusMap.getOrDefault(groupKey,0);
    int matchStockQty = stockInfos.stream().filter(item -> null != item.getStockQty()).mapToInt(MdmProductStock::getStockQty).sum();
    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus,
        matchStockQty,
        stockInfos,
        materialInfoMap
    );

    List<DpOrderOffsetDetail> allocations = new ArrayList<>();
    for (SalesOrderPool order : sortedOrders) {
      DpOrderOffsetDetail allocation = allocateStockForSingleOrder(monthPlanVersion,yearMonth,order,context);
      allocations.add(allocation);
    }
    return new StockAllocationResult(allocations);
  }

  private static StockAllocationResult allocateMonthSurplusForOrders(String monthPlanVersion,YearMonth yearMonth, Integer plannedSurplus,Map<String, MdmMaterialInfo> materialInfoMap,List<SalesOrderPool> sortedOrders) {
    StockAllocationContext context = new StockAllocationContext(
        plannedSurplus,
        0,
        null,
        materialInfoMap
    );
    List<DpOrderOffsetDetail> allocations = new ArrayList<>();
    for (SalesOrderPool order : sortedOrders) {
      DpOrderOffsetDetail allocation = allocateMonthSurplusForSingleOrder(monthPlanVersion,yearMonth,order,context);
      allocations.add(allocation);
    }
    return new StockAllocationResult(allocations);
  }



  /**
   * 为单个订单分配库存
   */
  private static DpOrderOffsetDetail allocateStockForSingleOrder(String monthPlanVersion,YearMonth yearMonth,SalesOrderPool order, StockAllocationContext context) {
    // 5、库存冲减后，继续扣减月底计划余量部分
    int orderQty = null == order.getOrdQty()?BigDecimal.ZERO.intValue():order.getOrdQty().intValue();
    // 库存分配量
    List<MdmProductStock> matchProductStocks = filterAndSortStocks(order, context);
    int stockQty  = calculateMatchStockQty(matchProductStocks);
    int allocationQty = calculateAllocationQty(orderQty,matchProductStocks);
    int produceQtyDue = orderQty - allocationQty;
    int plannedSurplus = BigDecimal.ZERO.intValue();
    if(produceQtyDue > 0 && context.getPlannedSurplus() > 0) {
      if(context.getPlannedSurplus() >= produceQtyDue) {
        plannedSurplus = produceQtyDue;
        context.setPlannedSurplus(context.getPlannedSurplus() - produceQtyDue);
        produceQtyDue = BigDecimal.ZERO.intValue();
      }else{
        plannedSurplus = context.getPlannedSurplus();
        produceQtyDue = produceQtyDue  - context.getPlannedSurplus();
        context.setPlannedSurplus(BigDecimal.ZERO.intValue());
      }
    }
    return buildAllocation(order,context.getMaterialInfoMap(), monthPlanVersion,yearMonth,stockQty,plannedSurplus, allocationQty,produceQtyDue);
  }

  private static int calculateAllocationQty(int orderQty, List<MdmProductStock> matchProductStocks) {
     if(orderQty == BigDecimal.ZERO.intValue() || CollectionUtils.isEmpty(matchProductStocks)) {
        return BigDecimal.ZERO.intValue();
     }
      BigDecimal oriOrderQty = BigDecimal.valueOf(orderQty);
      BigDecimal remainingQty = BigDecimal.valueOf(orderQty);
      for (MdmProductStock stock : matchProductStocks) {
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
          break;
        }
        // 获取当前库存数量
        BigDecimal stockQty = BigDecimal.valueOf(stock.getLeftOverQty());
        if (stockQty.compareTo(remainingQty) >= 0) {
          // 当前库存足够冲减
          stock.setLeftOverQty(stockQty.subtract(remainingQty).intValue());
          remainingQty = BigDecimal.ZERO;
        } else {
          // 当前库存不足，全部冲减
          stock.setLeftOverQty(0);
          remainingQty = remainingQty.subtract(stockQty);
        }
      }
      return oriOrderQty.subtract(remainingQty).intValue();
  }

  private static int calculateMatchStockQty(List<MdmProductStock> matchProductStocks) {
      if(CollectionUtils.isEmpty(matchProductStocks)) {
          return BigDecimal.ZERO.intValue();
      }
      return  matchProductStocks.stream().filter(item -> null != item.getLeftOverQty()).mapToInt(MdmProductStock::getLeftOverQty).sum();
  }

  private static DpOrderOffsetDetail allocateMonthSurplusForSingleOrder(String monthPlanVersion,YearMonth yearMonth,SalesOrderPool order, StockAllocationContext context) {
    int produceQtyDue = null == order.getOrdQty()?BigDecimal.ZERO.intValue():order.getOrdQty().intValue();
    int plannedSurplus = BigDecimal.ZERO.intValue();
    if(produceQtyDue > 0 && context.getPlannedSurplus() > 0) {
      if(context.getPlannedSurplus() >= produceQtyDue) {
        plannedSurplus = produceQtyDue;
        context.setPlannedSurplus(context.getPlannedSurplus() - produceQtyDue);
        produceQtyDue = BigDecimal.ZERO.intValue();
      }else{
        plannedSurplus = context.getPlannedSurplus();
        produceQtyDue = produceQtyDue  - context.getPlannedSurplus();
        context.setPlannedSurplus(BigDecimal.ZERO.intValue());
      }
    }
    return buildAllocation(order,context.getMaterialInfoMap(), monthPlanVersion,yearMonth,BigDecimal.ZERO.intValue(),plannedSurplus, BigDecimal.ZERO.intValue(),produceQtyDue);
  }

  /**
   * 计算单个订单的分配数量
   */
  private static List<MdmProductStock> filterAndSortStocks(SalesOrderPool order, StockAllocationContext context) {
    List<MdmProductStock> stockInfos = context.getStockInfos();
    // 订单有年周号要求
    if(StringUtils.isNotBlank(order.getWeekYear()) && !ZERO_YEAR_WEEK.equals(order.getWeekYear()) && ApsCommonUtil.isNumber(order.getWeekYear())) {
        return reduceInventoryByWeekYear(
            order,
            stockInfos);
    }
    //   (3)订单中有动平衡要求的，则可冲减的库存必须是动平衡库存，
    //   (3.3）注：有动平衡、均匀性的，可以冲减库存不带动平衡和均匀性标志的，但需要做提醒；
    if(StringUtils.isNotBlank(order.getIsDynamicBalance())) {
      return reduceInventory(
          order,
          stockInfos,true,false);
    }
    if(StringUtils.isNotBlank(order.getIsUniformity())) {
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
  private static List<MdmProductStock> reduceInventoryByWeekYear(SalesOrderPool order, List<MdmProductStock> stockInfos) {
    BigDecimal orderQty = order.getOrdQty();
    if (orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("数量无效，无法冲减库存");
      return Collections.emptyList();
    }
    String transformed = order.getWeekYear().substring(2) + order.getWeekYear().substring(0,2);
    int orderWeekYear = Integer.parseInt(transformed);
    // 3. 过滤和排序库存
    List<MdmProductStock> eligibleStocks = filterAndSortStocksByWeekYear(stockInfos, orderWeekYear);
    if (CollectionUtils.isEmpty(eligibleStocks)) {
      log.warn("没有符合条件的库存可以冲减");
      return Collections.emptyList();
    }
    return eligibleStocks;
  }

  private static List<MdmProductStock> reduceInventory(SalesOrderPool order, List<MdmProductStock> stockInfos,boolean requiresDynamicBalance,boolean requiresUniformity) {
    BigDecimal orderQty = order.getOrdQty();
    if (orderQty == null || orderQty.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("数量无效，无法冲减库存");
      return Collections.emptyList();
    }
    // 3. 过滤和排序库存
    List<MdmProductStock> eligibleStocks = intelligentStockSelection(stockInfos, requiresDynamicBalance,requiresUniformity);
    if (CollectionUtils.isEmpty(eligibleStocks)) {
      log.warn("没有符合条件的库存可以冲减");
      return Collections.emptyList();
    }
    return eligibleStocks;
  }


  /**
   * 高级算法：智能动平衡库存选择（考虑多个维度）
   */
  private static List<MdmProductStock> intelligentStockSelection(
      List<MdmProductStock> stockInfos,boolean requiresDynamicBalance,boolean requiresUniformity) {
    return stockInfos.stream()
        .filter(StockAllocationHelper::isValidStock)
        .sorted(getHighPerformanceComparator(requiresDynamicBalance,requiresUniformity))
        .collect(Collectors.toList());
  }

  /**
   * 判断库存是否有效
   */
  private static boolean isValidStock(MdmProductStock stock) {
    return stock.getLeftOverQty() != null
        && stock.getLeftOverQty() > 0;
  }

  /**
   * 判断库存是否为动平衡库存
   */
  private static boolean isDynamicBalanceStock(MdmProductStock stock) {
    return YesOrNoEnum.YES.getCode().equals(stock.getIsDynamicBalance());
  }

  /**
   * 判断库存是否为均匀性库存
   */
  private static boolean isUniformityStock(MdmProductStock stock) {
    return YesOrNoEnum.YES.getCode().equals(stock.getIsUniformity());
  }


  private static List<MdmProductStock> filterAndSortStocksByWeekYear(List<MdmProductStock> stockInfos, int orderWeekYear) {
    List<MdmProductStock> result =  stockInfos.stream()
        // 过滤：库存年周号晚于订单年周号
        .filter(stock -> {
          if (StringUtils.isBlank(stock.getWeekYear()) || !ApsCommonUtil.isNumber(stock.getWeekYear()) || stock.getLeftOverQty() == null || stock.getLeftOverQty() <= 0) {
            return false;
          }
          String transformed = stock.getWeekYear().substring(2) + stock.getWeekYear().substring(0,2);
          int stockWeekYear = Integer.parseInt(transformed);
          stock.setStockWeekYear(stockWeekYear);
          return stockWeekYear >= orderWeekYear;
        })
        // 收集为列表
        .collect(Collectors.toList());
      if(CollectionUtils.isEmpty(result)) {
        return Collections.emptyList();
      }
     // 排序：年周号从早到晚（升序）
      result.sort(Comparator.comparing(MdmProductStock::getStockWeekYear));
      return result;
  }

  /**
   * 构建分配记录
   */
  private static DpOrderOffsetDetail buildAllocation(SalesOrderPool order,Map<String, MdmMaterialInfo> materialInfoMap, String version,YearMonth yearMonth,int stockQty,int plannedSurplus,int allocationQty,int produceQtyDue) {
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
    // allocation.setCustomName();
    allocation.setCustomNationCode(order.getSalNCode());
    allocation.setDestinationNationCode(order.getNatCode());
    allocation.setMaterialCode(order.getOriMaterialCode());
    allocation.setPoNumber(order.getSalCodePo());
    allocation.setScmId(order.getScmDetailId());
    allocation.setPlannedSurplus(plannedSurplus);
    // allocation.setMesMaterialCode();
    // allocation.setLocationType(order);
    allocation.setOrderQty(order.getOrdQty() == null?BigDecimal.ZERO.intValue():order.getOrdQty().intValue());
    allocation.setStockQty(stockQty);
    allocation.setAllocationQty(allocationQty);
    allocation.setProduceQtyDue(produceQtyDue);
    MdmMaterialInfo materialInfo = materialInfoMap.get(order.getOriMaterialCode());
    if(null != materialInfo) {
      allocation.setSpecifications(materialInfo.getSpecifications());
      allocation.setPattern(materialInfo.getPattern());
    }
    return allocation;
  }

  /**
   * 库存分配上下文 - 用于跟踪分配过程中的库存状态
   */
  @Setter
  @Getter
  private static class StockAllocationContext {
    private Integer plannedSurplus;
    private Integer  matchStockQty;
    private List<MdmProductStock> stockInfos;
    private Map<String, MdmMaterialInfo> materialInfoMap;
    public StockAllocationContext(Integer plannedSurplus,Integer  matchStockQty,List<MdmProductStock> stockInfos,Map<String, MdmMaterialInfo> materialInfoMap) {
      this.plannedSurplus = plannedSurplus;
      this.matchStockQty = matchStockQty;
      this.stockInfos = stockInfos;
      this.materialInfoMap = materialInfoMap;
    }
  }

  /**
   * 库存分配结果类（替代Record）
   */
  @Getter
  private static class StockAllocationResult {
    private final List<DpOrderOffsetDetail> allocations;

    public StockAllocationResult(List<DpOrderOffsetDetail> allocations) {
      this.allocations = allocations;
    }

  }

}
