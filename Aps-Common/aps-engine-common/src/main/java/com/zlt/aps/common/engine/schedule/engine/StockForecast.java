package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TM/TC 共用的库存预测结果。
 *
 * <p>胎面和胎侧的库存预测计算口径一致，仅产品编码字段在业务层使用不同名称。
 * 公共模型统一使用 {@code processCode} 表示参与排程的工序产品编码。</p>
 */
@Data
public class StockForecast implements ScheduleInventoryForecast {

    /** 参与排程的工序产品编码。 */
    private String processCode;

    /** 6点库存快照，单位米。 */
    private BigDecimal sixClockStockQty;

    /** 早班需求量，单位米（从成型计划表获取）。 */
    private BigDecimal firstShiftDemandQty;

    /** 早班计划量，单位米（从对应工序排程结果获取）。 */
    private BigDecimal firstShiftPlanQty;

    /** 14点预计库存，单位米（计算公式：6点库存 - 早班需求量 + 早班计划量）。 */
    private BigDecimal rollingStockQty;

    /** 工装折算使用的有效卷曲长度，单位米/套。 */
    private BigDecimal effectiveCurlLength;

    /** 有效卷曲长度来源：产品配置或默认参数。 */
    private String curlLengthSource;

    /**
     * 计算14点预计库存。
     *
     * <p>计算公式：14点预计库存 = 6点库存 - 早班需求量 + 早班计划量，结果不小于零。</p>
     */
    @Override
    public void calculateRollingStockQty() {
        BigDecimal sixClock = this.sixClockStockQty != null ? this.sixClockStockQty : BigDecimal.ZERO;
        BigDecimal demand = this.firstShiftDemandQty != null ? this.firstShiftDemandQty : BigDecimal.ZERO;
        BigDecimal plan = this.firstShiftPlanQty != null ? this.firstShiftPlanQty : BigDecimal.ZERO;
        BigDecimal calculatedRollingStockQty = sixClock.subtract(demand).add(plan);
        if (calculatedRollingStockQty.compareTo(BigDecimal.ZERO) < 0) {
            calculatedRollingStockQty = BigDecimal.ZERO;
        }
        this.rollingStockQty = calculatedRollingStockQty;
    }
}
