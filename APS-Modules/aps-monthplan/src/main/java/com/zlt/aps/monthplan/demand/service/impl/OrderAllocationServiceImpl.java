package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.demand.mapper.DpOrderOffsetDetailEntityMapper;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 订单冲减分配ServiceImpl
 * @author 16799 - nick
 */
@Service
@Slf4j
public class OrderAllocationServiceImpl{

    @Resource
    private FactoryMonthPlanProductionFinalResultEntityMapper finalResultMapper;

    @Resource
    private DpOrderOffsetDetailEntityMapper orderOffsetDetailMapper;

    /**
     * 按月分配生产量
     * @param year 年份
     * @param month 月份
     * @param factoryCode 工厂代码
     * @param monthPlanVersion 需求版本     ----     来源不同，版本也不同，  1. 最终结果版本  2. 预测订单版本  3. 最终结果调整版本
     *
     * @param materialTotalQtyMap 物料总量 MAP { 物料编码 : 总量 }  可以直接参考方法 ：getFormalOrderMaterialTotal(year, month, factoryCode)
     *              materialTotalQtyMap   为 null 时，会抛出异常 ， 各自业务采集组装数据调用
     * <p>
     *              materialTotalQtyMap  来源【预测订单】需要自行传入，  注意 (订单冲减表应该有对应版本数据)  ,  否则异常
     *              materialTotalQtyMap  来源【最终结果】需要自行传入 ， 注意 (订单冲减表应该有对应版本数据)  ,  否则异常
     *              materialTotalQtyMap  来源【最终结果调整】需要自行传入 ， 注意 (订单冲减表应该有对应版本数据)   ,  否则异常
     *
     * @throws Exception 抛出异常各自处理
     *
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocateProductionByMonth(Integer year, Integer month, String factoryCode, String monthPlanVersion, Map<String, Integer> materialTotalQtyMap) throws Exception {
        // 1. 获取月计划版本
        if (StringUtils.isBlank(monthPlanVersion)) {
            throw new RuntimeException("未找到对应的月计划版本");
        }

        // 2. 获取正式订单的物料总量 MAP_A
        if (materialTotalQtyMap == null || materialTotalQtyMap.isEmpty()) {
            // materialTotalQtyMap = getFormalOrderMaterialTotal(year, month, factoryCode);
            throw new RuntimeException("未找到对应的物料总量");
        }

        // 3. 获取需要分配的订单详情
        List<DpOrderOffsetDetail> orderDetails = getOrderDetailsForAllocation(
                year, month, factoryCode, monthPlanVersion);

        if (orderDetails == null || orderDetails.isEmpty()) {
            throw new RuntimeException("未找到对应版本的订单冲减分配详情");
        }

        // 4. 按物料分组并排序
        Map<String, List<DpOrderOffsetDetail>> groupedOrders = groupAndSortOrders(orderDetails);

        // 5. 分配生产量并批量更新
        allocateAndUpdateOrders(materialTotalQtyMap, groupedOrders);
    }

    /**
     * 获取正式订单的物料总量
     */
    private Map<String, Integer> getFormalOrderMaterialTotal(Integer year, Integer month, String factoryCode) {
        // 先查询所有符合条件的记录
        List<FactoryMonthPlanProductionFinalResult> results = finalResultMapper.selectList(
                new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                        .eq(FactoryMonthPlanProductionFinalResult::getYear, year)
                        .eq(FactoryMonthPlanProductionFinalResult::getMonth, month)
                        .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, factoryCode)
                        // 正式订单
                        .eq(FactoryMonthPlanProductionFinalResult::getConstructionStage, "3")
        );

        // 在内存中按物料编码分组并汇总
        Map<String, Integer> materialTotalQtyMap = new HashMap<>();
        for (FactoryMonthPlanProductionFinalResult result : results) {
            String materialCode = result.getMaterialCode();
            Integer totalQty = result.getTotalQty() != null ? result.getTotalQty() : 0;

            materialTotalQtyMap.put(
                    materialCode,
                    materialTotalQtyMap.getOrDefault(materialCode, 0) + totalQty
            );
        }

        return materialTotalQtyMap;
    }

    /**
     * 获取需要分配的订单详情
     */
    private List<DpOrderOffsetDetail> getOrderDetailsForAllocation(
            Integer year, Integer month, String factoryCode, String monthPlanVersion) {
        return orderOffsetDetailMapper.selectList(
                new LambdaQueryWrapper<DpOrderOffsetDetail>()
                        .eq(DpOrderOffsetDetail::getYear, year)
                        .eq(DpOrderOffsetDetail::getMonth, month)
                        .eq(DpOrderOffsetDetail::getFactoryCode, factoryCode)
                        .eq(DpOrderOffsetDetail::getMonthPlanVersion, monthPlanVersion)
                        // 排除暂缓订单，优先级为5表示暂缓
                        .notIn(DpOrderOffsetDetail::getOrderPriority, "5")
        );
    }

    /**
     * 按物料分组并排序
     */
    private Map<String, List<DpOrderOffsetDetail>> groupAndSortOrders(List<DpOrderOffsetDetail> orders) {
        // 先按物料分组
        Map<String, List<DpOrderOffsetDetail>> groupedMap = new HashMap<>();
        for (DpOrderOffsetDetail order : orders) {
            String materialCode = order.getMaterialCode();
            if (!groupedMap.containsKey(materialCode)) {
                groupedMap.put(materialCode, new ArrayList<>());
            }
            groupedMap.get(materialCode).add(order);
        }

        // 对每组进行排序
        for (Map.Entry<String, List<DpOrderOffsetDetail>> entry : groupedMap.entrySet()) {
            List<DpOrderOffsetDetail> orderList = entry.getValue();
            orderList.sort(new Comparator<DpOrderOffsetDetail>() {
                @Override
                public int compare(DpOrderOffsetDetail o1, DpOrderOffsetDetail o2) {
                    // 1. 按订单优先级排序（高优先级1 > 中优先级3）
                    int priorityCompare = comparePriority(o1.getOrderPriority(), o2.getOrderPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }

                    // 2. 按提报日期排序（早的在前）
                    if (o1.getBillDate() != null && o2.getBillDate() != null) {
                        int dateCompare = o1.getBillDate().compareTo(o2.getBillDate());
                        if (dateCompare != 0) {
                            return dateCompare;
                        }
                    } else if (o1.getBillDate() != null) {
                        return -1; // o1有日期，o2无日期，o1排前面
                    } else if (o2.getBillDate() != null) {
                        return 1; // o2有日期，o1无日期，o2排前面
                    }

                    // 3. 按produceQtyDue降序排序（大的在前）
                    long qty1 = o1.getProduceQtyDue() != null ? o1.getProduceQtyDue() : 0L;
                    long qty2 = o2.getProduceQtyDue() != null ? o2.getProduceQtyDue() : 0L;
                    return Long.compare(qty2, qty1); // 降序
                }
            });
        }

        return groupedMap;
    }

    /**
     * 比较订单优先级
     */
    private int comparePriority(String priority1, String priority2) {
        Map<String, Integer> priorityOrder = new HashMap<>();
        // 高优先级
        priorityOrder.put("1", 1);
        // 中优先级
        priorityOrder.put("3", 2);

        int order1 = priorityOrder.getOrDefault(priority1, 3);
        int order2 = priorityOrder.getOrDefault(priority2, 3);
        return Integer.compare(order1, order2);
    }

    /**
     * 分配生产量并批量更新
     */
    private void allocateAndUpdateOrders(
            Map<String, Integer> materialTotalQtyMap,
            Map<String, List<DpOrderOffsetDetail>> groupedOrders) {

        List<DpOrderOffsetDetail> ordersToUpdate = new ArrayList<>();

        for (Map.Entry<String, List<DpOrderOffsetDetail>> entry : groupedOrders.entrySet()) {
            String materialCode = entry.getKey();
            List<DpOrderOffsetDetail> orderList = entry.getValue();
            Integer totalAllocationQty = materialTotalQtyMap.get(materialCode);

            if (totalAllocationQty == null || totalAllocationQty <= 0) {
                // 没有该物料的分配量，将所有订单的生产量设为0
                for (DpOrderOffsetDetail order : orderList) {
                    order.setProductionQty(0);
                    ordersToUpdate.add(order);
                }
                continue;
            }

            // 分配逻辑
            int remainingQty = totalAllocationQty;

            for (DpOrderOffsetDetail order : orderList) {
                if (remainingQty <= 0) {
                    order.setProductionQty(0);
                    ordersToUpdate.add(order);
                    continue;
                }

                int maxAllocatable = order.getProduceQtyDue() != null ? order.getProduceQtyDue() : 0;
                int allocatedQty = Math.min(maxAllocatable, remainingQty);

                order.setProductionQty(allocatedQty);
                ordersToUpdate.add(order);
                remainingQty -= allocatedQty;
            }

            // 记录未完全分配的情况（可选）
            if (remainingQty > 0) {
                log.warn("物料 {} 有 {} 数量未分配", materialCode, remainingQty);
            }
        }

        // 批量更新
        if (!ordersToUpdate.isEmpty()) {
            batchUpdateProductionQty(ordersToUpdate);
        }
    }

    /**
     * 批量更新生产数量
     */
    private void batchUpdateProductionQty(List<DpOrderOffsetDetail> ordersToUpdate) {
        // 分批次更新，避免SQL过长
        int batchSize = 500;
        for (int i = 0; i < ordersToUpdate.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ordersToUpdate.size());
            List<DpOrderOffsetDetail> batchList = ordersToUpdate.subList(i, end);
            orderOffsetDetailMapper.updateBatchProductionQty(batchList);
        }
    }
}