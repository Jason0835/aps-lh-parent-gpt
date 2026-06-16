package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面库存预测结果。
 *
 * <p>用于存储单个胎面规格的库存预测信息，包括6点库存、早班需求量、
 * 早班计划量和14点预计库存。</p>
 */
@Data
public class TmStockForecast {

    /** 胎面编码 */
    private String treadCode;

    /** 6点库存快照，单位米 */
    private BigDecimal sixClockStockQty;

    /** 早班胎面需求量，单位米（从成型计划表获取） */
    private BigDecimal firstShiftDemandQty;

    /** 早班胎面计划量，单位米（从T_TM_SCHEDULE_RESULT获取） */
    private BigDecimal firstShiftPlanQty;

    /** 14点预计库存，单位米（计算公式：6点库存 - 早班需求量 + 早班计划量） */
    private BigDecimal rollingStockQty;

    /**
     * 计算14点预计库存。
     *
     * <p>计算公式：14点预计库存 = 6点库存 - 早班需求量 + 早班计划量</p>
     */
    public void calculateRollingStockQty() {
        BigDecimal sixClock = sixClockStockQty != null ? sixClockStockQty : BigDecimal.ZERO;
        BigDecimal demand = firstShiftDemandQty != null ? firstShiftDemandQty : BigDecimal.ZERO;
        BigDecimal plan = firstShiftPlanQty != null ? firstShiftPlanQty : BigDecimal.ZERO;
        this.rollingStockQty = sixClock.subtract(demand).add(plan);
    }
}
