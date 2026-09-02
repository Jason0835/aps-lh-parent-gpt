package com.zlt.aps.common.engine.schedule.engine;

/**
 * 库存预测日志端口。
 *
 * @param <F> 库存预测结果类型
 */
public interface InventoryPredictionTracePort<F extends ScheduleInventoryForecast> {

    /** 记录无任务日志。 */
    void logNoTasks();

    /** @param productCode 产品编码 @param forecast 预测结果 */
    void logForecast(String productCode, F forecast);

    /** @param forecastCount 预测数量 */
    void logCompleted(int forecastCount);
}

