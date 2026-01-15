package com.zlt.aps.monthplan.factory.helper;


import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      List<SupplyOrderPool> cycleStockOrders,
      List<FactoryMonthPlanProductionFinalResult> productionFinalResults,
      List<MpMonthPlanMonitor>  mpMonthPlanMonitors) {
    List<DpOrderOffsetDetail> result = new ArrayList<>();
    if(CollectionUtils.isEmpty(netDemands)) {
      return result;
    }
    Map<String,List<DpOrderOffsetDetail>>  netDemandGroupMap = netDemands.stream().collect(Collectors.groupingBy(DpOrderOffsetDetail::getMaterialCode));
    Map<String,Integer> productionGroupMap = calculateProductionQty(productionFinalResults);
    Map<String,Integer> completionGroupMap = calculateCompleteQty(mpMonthPlanMonitors);
    Map<String,Integer> cycleStockQtyMap = calculatecycleStockQty(cycleStockOrders);
    for (Map.Entry<String, List<DpOrderOffsetDetail>> entry : netDemandGroupMap.entrySet()) {
      String groupKey = entry.getKey();
      List<DpOrderOffsetDetail> saleOrders = entry.getValue();
      DpOrderOffsetDetail saleOrder = processOrderGroup(
          groupKey,
          saleOrders,
          cycleStockQtyMap,
          productionGroupMap,
          completionGroupMap
      );
      if(null == saleOrder) {
        continue;
      }
      result.add(saleOrder);
    }
    return result;
  }

  private static Map<String, Integer> calculatecycleStockQty(List<SupplyOrderPool> cycleStockOrders) {
    if(CollectionUtils.isEmpty(cycleStockOrders)) {
      return Collections.emptyMap();
    }
    return cycleStockOrders.stream()
        .filter(Objects::nonNull)
        .filter(supplyOrder -> StringUtils.isNotBlank(supplyOrder.getMaterialCode()) && null != supplyOrder.getQty())
        .collect(Collectors.groupingBy(
            SupplyOrderPool::getMaterialCode,
            Collectors.summingInt(SupplyOrderPool::getQty)
        ));
  }


  /**
   * 处理单个订单组的库存分配
   */
  private static DpOrderOffsetDetail processOrderGroup(
      String groupKey,
      List<DpOrderOffsetDetail> saleOrders,
      Map<String,Integer> cycleStockQtyMap,
      Map<String,Integer> productionQtyMap,
      Map<String,Integer> completedQtyMap
      ) {
         // 9、从7步骤中的订单数据，按SKU扣减T月月度计划对应实单已排产量(销售订单)+ T月已生产量，得到销售订单剩余还未排产量
         int netDemand = saleOrders.stream().mapToInt(DpOrderOffsetDetail::getProduceQtyDue).sum();
         if(BigDecimal.ZERO.intValue() == netDemand) {
           return null;
         }
          // T月实单未排产量：	300	(高优先级净需求+中优先级+暂缓订单-T月实单排产量+T月实单已完成量)
         int realUnproductionQty = netDemand +  cycleStockQtyMap.getOrDefault(groupKey,0)  - productionQtyMap.getOrDefault(groupKey, 0) + completedQtyMap.getOrDefault(groupKey, 0);
          if(realUnproductionQty <= BigDecimal.ZERO.intValue()) {
            return null;
          }
         List<DpOrderOffsetDetail> sortList = getSortedOrders(saleOrders);
         DpOrderOffsetDetail saleOrder = sortList.get(0);
         saleOrder.setOrderQty(realUnproductionQty);
         saleOrder.setProduceQtyDue(realUnproductionQty);
         return saleOrder;
  }

  private static Map<String,Integer> calculateProductionQty(List<FactoryMonthPlanProductionFinalResult> productionFinalResults) {
    if(CollectionUtils.isEmpty(productionFinalResults)) {
      return Collections.emptyMap();
    }
    return productionFinalResults.stream()
        .filter(Objects::nonNull)
        .filter(productionFinalResult -> StringUtils.isNotBlank(productionFinalResult.getMaterialCode()) && null != productionFinalResult.getTotalQty())
        .collect(Collectors.groupingBy(
            FactoryMonthPlanProductionFinalResult::getMaterialCode,
            Collectors.summingInt(FactoryMonthPlanProductionFinalResult::getTotalQty)
        ));
  }

  public static Map<String, Integer> calculateCompleteQty(List<MpMonthPlanMonitor> mpMonthPlanMonitors) {
    if(org.springframework.util.CollectionUtils.isEmpty(mpMonthPlanMonitors)) {
      return Collections.emptyMap();
    }
    return mpMonthPlanMonitors.stream()
        .filter(Objects::nonNull)
        .filter(monthPlanMonitor -> StringUtils.isNotBlank(monthPlanMonitor.getMaterialCode()) && null != monthPlanMonitor.getProductionQty())
        .collect(Collectors.groupingBy(
            MpMonthPlanMonitor::getMaterialCode,
            Collectors.summingInt(MpMonthPlanMonitor::getProductionQty)
        ));
  }

  /**
   * 获取排序后的订单列表
   */
  private static List<DpOrderOffsetDetail> getSortedOrders(
      List<DpOrderOffsetDetail> saleOrders) {
    return saleOrders.stream()
        .sorted(getHighPerformanceComparator())
        .collect(Collectors.toList());
  }

  /**
   * 高性能自定义比较器（适用于大数据量）
   */
  private static Comparator<DpOrderOffsetDetail> getHighPerformanceComparator() {
    return new SalesOrderComparator();
  }

  /**
   * 自定义高性能比较器实现
   * 避免重复解析和lambda开销
   */
  private static class SalesOrderComparator implements Comparator<DpOrderOffsetDetail> {

    @Override
    public int compare(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
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

    private int compareScmPriority(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
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

    private int compareBillDate(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
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

    /**
     * 按生产应完成数量降序排列（从大到小）
     * 空值处理：null 值排在最后
     */
    private int compareOrdQty(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
      Integer q1 = o1.getProduceQtyDue();
      Integer q2 = o2.getProduceQtyDue();

      // 两个都为 null，视为相等
      if (q1 == null && q2 == null) {
        return 0;
      }

      // 只有 q1 为 null，q2 不为 null，降序时 null 排最后，所以返回 1
      if (q1 == null) {
        return 1;
      }

      // 只有 q2 为 null，q1 不为 null，降序时非 null 值在前，所以返回 -1
      if (q2 == null) {
        return -1;
      }

      // 降序排列：q2 在前，q1 在后
      return q2.compareTo(q1);
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
}
