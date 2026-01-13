package com.zlt.aps.monthplan.factory.helper;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmAreaCapaAllocation;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售需求计划辅助类
 *
 * @author ZLT
 * @date 20250513
 */
@Slf4j
public class SaleRequirePlanHelper {

    /**
     * 销售订单按SKU分组
     *
     * @param salesOrders
     * @return
     */
    public static Map<String, List<SalesOrderPool>> getGroupSalesOrder(List<SalesOrderPool> salesOrders) {
        if (CollectionUtils.isEmpty(salesOrders)) {
            return Collections.emptyMap();
        }
        return salesOrders
                .parallelStream()
                .filter(Objects::nonNull)
                .filter(order -> order.getGroupKey() != null)
                .collect(Collectors.groupingByConcurrent(
                        SalesOrderPool::getGroupKey,
                        Collectors.toCollection(ArrayList::new)
                ));
    }

    /**
     * 处理净需求
     */
    public static List<DpDemandPlan> processNetDemands(
        DpDemandPlan createCondition,List<DpOrderOffsetDetail> netDemands, List<MdmAreaCapaAllocation> areaCapaAllocations) {
        if (CollectionUtils.isEmpty(areaCapaAllocations)) {
            return transformAllocationsToDemandPlans(createCondition,netDemands);
        }
        return processNetDemandsWithCapacity(createCondition,netDemands, areaCapaAllocations);
    }

    /**
     * 处理有产能配置的净需求
     */
    private static List<DpDemandPlan> processNetDemandsWithCapacity(
        DpDemandPlan createCondition,List<DpOrderOffsetDetail> netDemands,
            List<MdmAreaCapaAllocation> areaCapaAllocations) {
        List<DpDemandPlan> result = new ArrayList<>();
        // 按区域分组净需求
        Map<String, List<DpOrderOffsetDetail>> demandsByArea = netDemands.stream()
                .collect(Collectors.groupingBy(DpOrderOffsetDetail::getAreaCode));
        // 按区域分组产能配置
        Map<String, List<MdmAreaCapaAllocation>> capacityByArea = areaCapaAllocations.stream()
                .collect(Collectors.groupingBy(MdmAreaCapaAllocation::getAreaCode));
        // 处理每个区域
        demandsByArea.forEach((areaCode, orders) -> {
            List<DpOrderOffsetDetail> sortedOrders = sortOrdersByPriority(orders);
            List<MdmAreaCapaAllocation> areaCapacities = capacityByArea.get(areaCode);

            if (org.apache.commons.collections.CollectionUtils.isEmpty(areaCapacities)) {
                result.addAll(transformAllocationsToDemandPlans(createCondition,sortedOrders));
                return;
            }

            // 计算总产能和总需求
            long totalCapacity = areaCapacities.stream().filter(item -> null != item.getCapacityAllocation())
                    .mapToLong(item -> item.getCapacityAllocation().longValue())
                    .sum();


            List<DpOrderOffsetDetail> highSaleOrders = sortedOrders.stream().filter(item -> ApsConstant.SAL_PRIORITY_HIGHT.equals(item.getScmPriority())).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(highSaleOrders)) {
                long totalDemand = highSaleOrders.stream()
                    .mapToLong(DpOrderOffsetDetail::getProduceQtyDue)
                    .sum();
                // 调整优先级
                if (totalDemand > totalCapacity) {
                    processDemandHighPriorityExcludingLast(highSaleOrders, totalDemand - totalCapacity);
                } else {
                    sortedOrders.stream().filter(item -> ApsConstant.SAL_PRIORITY_MID.equals(item.getScmPriority())).forEach(order ->
                        order.setScmPriority(ApsConstant.SAL_PRIORITY_HIGHT));
                }
            }else{
                List<DpOrderOffsetDetail> midSaleOrders = sortedOrders.stream().filter(item -> ApsConstant.SAL_PRIORITY_MID.equals(item.getScmPriority())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(midSaleOrders)) {
                    long totalDemand = midSaleOrders.stream()
                        .mapToLong(DpOrderOffsetDetail::getProduceQtyDue)
                        .sum();
                    // 调整优先级
                    if (totalDemand > totalCapacity) {
                        processDemandMidPriorityExcludingLast(midSaleOrders, totalDemand - totalCapacity);
                    }
                }

            }

            result.addAll(transformAllocationsToDemandPlans(createCondition,sortedOrders));
        });

        return result;
    }

    private static void processDemandMidPriorityExcludingLast(List<DpOrderOffsetDetail> sortedOrders, long overAreaCapacityValue) {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(sortedOrders) || overAreaCapacityValue <= 0) {
            return;
        }
        long accumulatedQty = 0;
        // 从列表尾端开始遍历
        for (int i = sortedOrders.size() - 1; i >= 0; i--) {
            DpOrderOffsetDetail order = sortedOrders.get(i);
            // 跳过已处理或无效的订单
            if (order == null || order.getProduceQtyDue() == null || order.getProduceQtyDue() <= 0) {
                continue;
            }
            // 检查当前累加值是否已经达到或超过阈值
            // 注意：先检查，再累加
            long currentOrderQty = order.getProduceQtyDue();
            if(accumulatedQty + currentOrderQty > overAreaCapacityValue &&  accumulatedQty < overAreaCapacityValue) {
                break;
            }
            // 相当于调整某个订单时候，加上这个订单量大于总产能，但是减去这个订单量小于总产能情况，这个订单不需要调整为中优先级
            if (accumulatedQty + currentOrderQty >= overAreaCapacityValue) {
                break;
            } else {
                // 累加净需求量并设置优先级
                accumulatedQty += currentOrderQty;
                order.setScmPriority(ApsConstant.SAL_PRIORITY_HIGHT);
            }
        }
    }

    /**
     * 从列表尾端开始累加净需求量，直到达到或超过指定值
     * 注意：跳出循环的那个订单不修改优先级
     *
     * @param sortedOrders          排序后的净需求列表
     * @param overAreaCapacityValue 超出区域产能值
     */
    private static void processDemandHighPriorityExcludingLast(
            List<DpOrderOffsetDetail> sortedOrders,
            long overAreaCapacityValue) {

        if (org.apache.commons.collections.CollectionUtils.isEmpty(sortedOrders) || overAreaCapacityValue <= 0) {
            return;
        }
        long accumulatedQty = 0;
        // 从列表尾端开始遍历
        for (int i = sortedOrders.size() - 1; i >= 0; i--) {
            DpOrderOffsetDetail order = sortedOrders.get(i);
            // 跳过已处理或无效的订单
            if (order == null || order.getProduceQtyDue() == null || order.getProduceQtyDue() <= 0) {
                continue;
            }

            // 检查当前累加值是否已经达到或超过阈值
            // 注意：先检查，再累加
            long currentOrderQty = order.getProduceQtyDue();
            log.info("index:{},currentOrderQty:{}:accumulatedQty:{}", i, currentOrderQty, accumulatedQty);
            // 相当于调整某个订单时候，加上这个订单量大于总产能，但是减去这个订单量小于总产能情况，这个订单不需要调整为中优先级
            if (accumulatedQty + currentOrderQty >= overAreaCapacityValue) {
                break;
            } else {
                // 累加净需求量并设置优先级
                accumulatedQty += currentOrderQty;
                order.setScmPriority(ApsConstant.SAL_PRIORITY_MID);
            }
        }
    }

    private static List<DpOrderOffsetDetail> sortOrdersByPriority(List<DpOrderOffsetDetail> saleOrders) {
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

        private int compareOrdQty(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
            Integer q1 = o1.getProduceQtyDue();
            Integer q2 = o2.getProduceQtyDue();

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
     * 转换订单分配为需求计划
     */
    private static List<DpDemandPlan> transformAllocationsToDemandPlans(
        DpDemandPlan createCondition,List<DpOrderOffsetDetail> orders) {

        return orders.stream()
                .map(item -> buildDemandPlanFromAllocation(createCondition,item))
                .collect(Collectors.toList());
    }

    private static DpDemandPlan buildDemandPlanFromAllocation(DpDemandPlan createCondition,DpOrderOffsetDetail netDemand) {
        DpDemandPlan demandPlan = new DpDemandPlan();
        BeanUtils.copyProperties(netDemand, demandPlan);
        demandPlan.setFactoryCode(createCondition.getFactoryCode());
        demandPlan.setYear(createCondition.getYear());
        demandPlan.setMonth(createCondition.getMonth());
        demandPlan.setMonthPlanVersion(createCondition.getMonthPlanVersion());
        demandPlan.setPlanType(createCondition.getPlanType());
        demandPlan.setOrderQty(netDemand.getOrderQty());
        demandPlan.setIsDynamicBalance(YesOrNoEnum.YES.getCode().equals(netDemand.getIsDynamicBalance())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setIsUniformity(YesOrNoEnum.YES.getCode().equals(netDemand.getIsUniformity())?YesOrNoEnum.YES.getCode():YesOrNoEnum.NO.getCode());
        demandPlan.setNetQty(netDemand.getProduceQtyDue());
        demandPlan.setYearWeek(netDemand.getWeekYear());
        return demandPlan;
    }

    private SaleRequirePlanHelper() {

    }
}
