package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/** 库存预测结果的公共数值契约。 */
public interface ScheduleInventoryForecast {

    /** @return 六点库存 */
    BigDecimal getSixClockStockQty();

    /** @param quantity 六点库存 */
    void setSixClockStockQty(BigDecimal quantity);

    /** @return 早班需求量 */
    BigDecimal getFirstShiftDemandQty();

    /** @param quantity 早班需求量 */
    void setFirstShiftDemandQty(BigDecimal quantity);

    /** @return 早班计划量 */
    BigDecimal getFirstShiftPlanQty();

    /** @param quantity 早班计划量 */
    void setFirstShiftPlanQty(BigDecimal quantity);

    /** @return 滚动库存 */
    BigDecimal getRollingStockQty();

    /** 计算滚动库存。 */
    void calculateRollingStockQty();
}

