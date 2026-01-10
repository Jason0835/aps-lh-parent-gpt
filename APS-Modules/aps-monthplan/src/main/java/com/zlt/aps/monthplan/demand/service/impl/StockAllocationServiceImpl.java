package com.zlt.aps.monthplan.demand.service.impl;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件名称：StockAllocationService.java
 * 描    述：StockAllocationService 库存冲减 分配服务
 *  @author 16799 - nick
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAllocationServiceImpl {

    private static final String QUALITY_YES = "1";
    private static final String QUALITY_NO = "0";

    /**
     * 计算未分配库存
     */
    public Map<String, Integer> calculateStockWithoutOrder( List<MdmProductStock> finishedProductStocks, List<SalesOrderPool> salesOrderPools) {
        if (CollectionUtils.isEmpty(finishedProductStocks)) {
            return Collections.emptyMap();
        }

        // 1. 按物料分组库存
        Map<String, List<Map<String, Object>>> stockByMaterial =
                groupStocksByMaterial(finishedProductStocks);

        // 2. 按物料分组订单
        Map<String, List<Map<String, Object>>> ordersByMaterial =
                groupOrdersByMaterial(salesOrderPools);

        // 3. 计算每个物料的剩余库存
        Map<String, Integer> result = new HashMap<>();

        for (String materialCode : stockByMaterial.keySet()) {
            List<Map<String, Object>> stockList = stockByMaterial.get(materialCode);
            List<Map<String, Object>> orderList = ordersByMaterial.getOrDefault(materialCode,
                    Collections.emptyList());

            // 计算该物料的剩余库存
            int remainingStock = calculateMaterialRemainingStock(stockList, orderList);
            result.put(materialCode, remainingStock);
        }

        return result;
    }

    /**
     * 按物料分组库存
     */
    private Map<String, List<Map<String, Object>>> groupStocksByMaterial(
            List<MdmProductStock> finishedProductStocks) {

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (MdmProductStock stock : finishedProductStocks) {
            String materialCode = stock.getMaterialCode();

            // 创建库存记录Map
            Map<String, Object> stockRecord = createStockRecord(stock);

            // 按物料分组
            result.computeIfAbsent(materialCode, k -> new ArrayList<>())
                    .add(stockRecord);
        }

        // 对每个物料的库存按周号排序
        for (List<Map<String, Object>> stockList : result.values()) {
            stockList.sort(Comparator.comparingInt(
                    record -> (Integer) record.get("weekYearValue")));
        }

        return result;
    }

    /**
     * 按物料分组订单
     */
    private Map<String, List<Map<String, Object>>> groupOrdersByMaterial(
            List<SalesOrderPool> salesOrderPools) {

        if (CollectionUtils.isEmpty(salesOrderPools)) {
            return Collections.emptyMap();
        }

        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        for (SalesOrderPool order : salesOrderPools) {
            String materialCode = order.getOriMaterialCode();

            // 创建订单记录Map
            Map<String, Object> orderRecord = createOrderRecord(order);

            // 按物料分组
            result.computeIfAbsent(materialCode, k -> new ArrayList<>())
                    .add(orderRecord);
        }

        // 对每个物料的订单排序（质量要求多的优先）
        for (List<Map<String, Object>> orderList : result.values()) {
            orderList.sort(this::compareOrderPriority);
        }

        return result;
    }

    /**
     * 创建库存记录Map
     */
    private Map<String, Object> createStockRecord(MdmProductStock stock) {
        Map<String, Object> record = new HashMap<>();
        record.put("materialCode", stock.getMaterialCode());
        record.put("weekYear", stock.getWeekYear());
        record.put("weekYearValue", parseWeekYear(stock.getWeekYear()));
        record.put("isDynamicBalance", stock.getIsDynamicBalance());
        record.put("isUniformity", stock.getIsUniformity());
        record.put("stockQty", stock.getStockQty());
        // 初始剩余数量等于库存数量
        record.put("remainingQty", stock.getStockQty());
        return record;
    }

    /**
     * 创建订单记录Map
     */
    private Map<String, Object> createOrderRecord(SalesOrderPool order) {
        Map<String, Object> record = new HashMap<>();
        record.put("materialCode", order.getOriMaterialCode());
        record.put("weekYear", order.getWeekYear());
        record.put("weekYearValue", parseWeekYear(order.getWeekYear()));
        record.put("isDynamicBalance", order.getIsDynamicBalance());
        record.put("isUniformity", order.getIsUniformity());
        record.put("ordQty", order.getOrdQty().intValue());

        // 计算质量要求数量
        int qualityCount = (QUALITY_YES.equals(order.getIsDynamicBalance()) ? 1 : 0) +
                (QUALITY_YES.equals(order.getIsUniformity()) ? 1 : 0);
        record.put("qualityCount", qualityCount);

        return record;
    }

    /**
     * 计算单个物料的剩余库存
     */
    private int calculateMaterialRemainingStock(
            List<Map<String, Object>> stockList,
            List<Map<String, Object>> orderList) {

        // 复制库存列表，避免修改原始数据
        List<Map<String, Object>> availableStocks = copyStockList(stockList);

        // 处理订单分配
        processOrderAllocation(orderList, availableStocks);

        // 计算总剩余库存
        return availableStocks.stream()
                .mapToInt(record -> (Integer) record.get("remainingQty"))
                .sum();
    }

    /**
     * 处理订单分配
     */
    private void processOrderAllocation(
            List<Map<String, Object>> orderList,
            List<Map<String, Object>> stockList) {

        if (CollectionUtils.isEmpty(orderList)) {
            return;
        }

        for (Map<String, Object> order : orderList) {
            allocateSingleOrder(order, stockList);
        }
    }

    /**
     * 分配单个订单
     */
    private void allocateSingleOrder(Map<String, Object> order,
                                     List<Map<String, Object>> stockList) {

        int orderWeekYear = (Integer) order.get("weekYearValue");
        int remainingOrderQty = (Integer) order.get("ordQty");

        // 筛选满足周号要求的库存
        List<Map<String, Object>> eligibleStocks = stockList.stream()
                .filter(stock ->
                        (Integer) stock.get("weekYearValue") >= orderWeekYear &&
                                (Integer) stock.get("remainingQty") > 0)
                .collect(Collectors.toList());

        if (eligibleStocks.isEmpty()) {
            return;
        }

        // 获取订单质量要求
        boolean requireDynamicBalance = QUALITY_YES.equals(order.get("isDynamicBalance"));
        boolean requireUniformity = QUALITY_YES.equals(order.get("isUniformity"));

        // 分配逻辑
        if (requireDynamicBalance || requireUniformity) {
            remainingOrderQty = allocateOrderWithQualityRequirement(
                    order, eligibleStocks, remainingOrderQty);
        } else {
            remainingOrderQty = allocateFromStocks(eligibleStocks, remainingOrderQty);
        }
    }

    /**
     * 分配有质量要求的订单
     */
    private int allocateOrderWithQualityRequirement(
            Map<String, Object> order,
            List<Map<String, Object>> eligibleStocks,
            int orderQty) {

        int remainingQty = orderQty;
        boolean requireDynamicBalance = QUALITY_YES.equals(order.get("isDynamicBalance"));
        boolean requireUniformity = QUALITY_YES.equals(order.get("isUniformity"));

        // 阶段1：同时满足动平衡和均匀性
        if (requireDynamicBalance && requireUniformity) {
            List<Map<String, Object>> fullMatchStocks = eligibleStocks.stream()
                    .filter(stock ->
                            QUALITY_YES.equals(stock.get("isDynamicBalance")) &&
                                    QUALITY_YES.equals(stock.get("isUniformity")))
                    .collect(Collectors.toList());

            remainingQty = allocateFromStocks(fullMatchStocks, remainingQty);
        }

        // 阶段2：满足部分质量要求
        if (remainingQty > 0) {
            List<Map<String, Object>> partialMatchStocks = eligibleStocks.stream()
                    .filter(stock -> matchesPartialQualityRequirement(
                            requireDynamicBalance, requireUniformity, stock))
                    .collect(Collectors.toList());

            remainingQty = allocateFromStocks(partialMatchStocks, remainingQty);
        }

        // 阶段3：两个质量都不满足
        if (remainingQty > 0) {
            List<Map<String, Object>> noQualityStocks = eligibleStocks.stream()
                    .filter(stock ->
                            QUALITY_NO.equals(stock.get("isDynamicBalance")) &&
                                    QUALITY_NO.equals(stock.get("isUniformity")))
                    .collect(Collectors.toList());

            remainingQty = allocateFromStocks(noQualityStocks, remainingQty);
        }

        return remainingQty;
    }

    /**
     * 检查是否满足部分质量要求
     */
    private boolean matchesPartialQualityRequirement(
            boolean requireDynamicBalance,
            boolean requireUniformity,
            Map<String, Object> stock) {

        String stockDynamicBalance = (String) stock.get("isDynamicBalance");
        String stockUniformity = (String) stock.get("isUniformity");

        if (requireDynamicBalance && requireUniformity) {
            // 要求两个质量，但库存只满足一个
            return (QUALITY_YES.equals(stockDynamicBalance) ||
                    QUALITY_YES.equals(stockUniformity));
        } else if (requireDynamicBalance) {
            return QUALITY_YES.equals(stockDynamicBalance);
        } else if (requireUniformity) {
            return QUALITY_YES.equals(stockUniformity);
        }

        return false;
    }

    /**
     * 从库存列表中分配数量
     */
    private int allocateFromStocks(List<Map<String, Object>> stocks, int orderQty) {
        // 按周号排序（从早到晚）
        stocks.sort(Comparator.comparingInt(
                stock -> (Integer) stock.get("weekYearValue")));

        int remainingOrderQty = orderQty;

        for (Map<String, Object> stock : stocks) {
            if (remainingOrderQty <= 0) {
                break;
            }

            int remainingStockQty = (Integer) stock.get("remainingQty");
            if (remainingStockQty > 0) {
                int allocateQty = Math.min(remainingStockQty, remainingOrderQty);
                stock.put("remainingQty", remainingStockQty - allocateQty);
                remainingOrderQty -= allocateQty;
            }
        }

        return remainingOrderQty;
    }

    /**
     * 比较订单优先级
     */
    private int compareOrderPriority(Map<String, Object> order1,
                                     Map<String, Object> order2) {
        // 质量要求数量多的优先
        int qualityCount1 = (Integer) order1.get("qualityCount");
        int qualityCount2 = (Integer) order2.get("qualityCount");

        if (qualityCount1 != qualityCount2) {
            return Integer.compare(qualityCount2, qualityCount1); // 降序
        }

        // 质量要求相同，按周号从小到大
        int weekYear1 = (Integer) order1.get("weekYearValue");
        int weekYear2 = (Integer) order2.get("weekYearValue");
        return Integer.compare(weekYear1, weekYear2);
    }

    /**
     * 复制库存列表
     */
    private List<Map<String, Object>> copyStockList(List<Map<String, Object>> original) {
        List<Map<String, Object>> copy = new ArrayList<>(original.size());

        for (Map<String, Object> record : original) {
            Map<String, Object> recordCopy = new HashMap<>(record);
            copy.add(recordCopy);
        }

        return copy;
    }

    /**
     * 解析周号为整数值
     */
    private int parseWeekYear(String weekYear) {
        if (weekYear == null || weekYear.length() != 4) {
            return 0;
        }

        try {
            int week = Integer.parseInt(weekYear.substring(0, 2));
            int year = Integer.parseInt(weekYear.substring(2, 4));
            return year * 100 + week; // 如2501表示25年01周
        } catch (NumberFormatException e) {
            // 解析失败返回0
            return 0;
        }
    }
}