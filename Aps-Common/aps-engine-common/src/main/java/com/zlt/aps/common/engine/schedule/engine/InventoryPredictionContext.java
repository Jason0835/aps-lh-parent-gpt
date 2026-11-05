package com.zlt.aps.common.engine.schedule.engine;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 库存预测公共上下文契约。
 *
 * @param <F> 库存预测结果类型
 */
public interface InventoryPredictionContext<F extends ScheduleInventoryForecast> {

    /** @return 排程日期 */
    LocalDate getScheduleDate();

    /** @return 工厂编号 */
    String getFactoryCode();

    /** @return 加载时的前日六点净库存快照 */
    Map<String, BigDecimal> getSixClockStockSnapshot();

    /** @return 加载时的首班需求快照 */
    Map<String, BigDecimal> getFirstShiftDemandSnapshot();

    /** @return 加载时确定的库存预测产品集合 */
    Set<String> getInventoryProductCodeSet();

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
