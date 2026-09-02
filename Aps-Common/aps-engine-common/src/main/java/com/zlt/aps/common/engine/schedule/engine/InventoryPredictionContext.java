package com.zlt.aps.common.engine.schedule.engine;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 库存预测公共上下文契约。
 *
 * @param <F> 库存预测结果类型
 */
public interface InventoryPredictionContext<F extends ScheduleInventoryForecast> {

    /** @return 排程日期 */
    Date getScheduleDate();

    /** @return 工厂编号 */
    String getFactoryCode();

    /**
     * 写入库存预测结果。
     *
     * @param stockForecastMap 产品编码与预测结果映射
     */
    void setStockForecastMap(Map<String, F> stockForecastMap);

    /**
     * 写入库存预测和工装占用使用的产品集合。
     *
     * @param productCodeSet 库存产品编码集合
     */
    void setInventoryProductCodeSet(Set<String> productCodeSet);
}
