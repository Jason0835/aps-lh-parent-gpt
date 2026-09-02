package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.*;

/**
 * 自动排程库存预测公共引擎。
 *
 * @param <C> 排程上下文类型
 * @param <F> 库存预测结果类型
 */
public final class InventoryPredictionEngine<C extends InventoryPredictionContext<F>,
        F extends ScheduleInventoryForecast> {

    /**
     * 执行库存预测主流程。
     *
     * @param context 排程上下文
     * @param policy 领域策略
     * @param tracePort 日志端口
     */
    public void predict(C context, InventoryPredictionPolicy<C, F> policy,
                        InventoryPredictionTracePort<F> tracePort) {
        policy.validateContext(context);
        Date scheduleDate = context.getScheduleDate();
        String factoryCode = context.getFactoryCode();
        List<String> taskProductCodes = policy.getTaskProductCodes(context);
        Map<String, BigDecimal> sixClockStockMap = policy.querySixClockStock(factoryCode, scheduleDate);
        List<String> productCodes = this.mergeInventoryProductCodes(taskProductCodes, sixClockStockMap);
        context.setInventoryProductCodeSet(new LinkedHashSet<>(productCodes));
        if (productCodes.isEmpty()) {
            context.setStockForecastMap(new HashMap<>());
            tracePort.logNoTasks();
            return;
        }

        policy.handleMissingStock(context, taskProductCodes, sixClockStockMap, scheduleDate);
        boolean useRecipe = policy.isRecipeMode(context);
        Map<String, BigDecimal> firstShiftDemandMap = policy.queryFirstShiftDemand(
                factoryCode, scheduleDate, productCodes, useRecipe);
        Map<String, BigDecimal> firstShiftPlanMap = policy.queryFirstShiftPlan(
                factoryCode, scheduleDate, productCodes);

        Map<String, F> stockForecastMap = new HashMap<>();
        for (String productCode : productCodes) {
            F forecast = policy.createForecast(productCode);
            forecast.setSixClockStockQty(sixClockStockMap.getOrDefault(productCode, BigDecimal.ZERO));
            forecast.setFirstShiftDemandQty(firstShiftDemandMap.getOrDefault(productCode, BigDecimal.ZERO));
            forecast.setFirstShiftPlanQty(firstShiftPlanMap.getOrDefault(productCode, BigDecimal.ZERO));
            forecast.calculateRollingStockQty();
            stockForecastMap.put(productCode, forecast);
            tracePort.logForecast(productCode, forecast);
        }
        context.setStockForecastMap(stockForecastMap);
        tracePort.logCompleted(stockForecastMap.size());
    }

    /**
     * 合并可排任务产品和前一天正净库存产品，形成库存预测产品集合。
     *
     * @param taskProductCodes 可排任务产品编码
     * @param sixClockStockMap 前一天六点净库存
     * @return 有序去重后的库存预测产品编码
     */
    private List<String> mergeInventoryProductCodes(List<String> taskProductCodes,
                                                    Map<String, BigDecimal> sixClockStockMap) {
        LinkedHashSet<String> productCodeSet = new LinkedHashSet<>();
        if (taskProductCodes != null) {
            productCodeSet.addAll(taskProductCodes);
        }
        if (sixClockStockMap != null) {
            sixClockStockMap.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .filter(entry -> entry.getValue() != null
                            && entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .map(Map.Entry::getKey)
                    .forEach(productCodeSet::add);
        }
        return new ArrayList<>(productCodeSet);
    }
}
