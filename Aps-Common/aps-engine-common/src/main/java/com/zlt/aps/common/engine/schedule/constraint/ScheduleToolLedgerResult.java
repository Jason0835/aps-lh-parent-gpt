package com.zlt.aps.common.engine.schedule.constraint;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面、胎侧共用的工装账本结算结果。
 */
@Data
public class ScheduleToolLedgerResult {

    /** 结算前可用工装数量 */
    private BigDecimal availableToolQty;

    /** 本次允许安排的计划增量 */
    private BigDecimal allowedPlanQty;

    /** 因工装不足无法安排的计划增量 */
    private BigDecimal overflowPlanQty;

    /** 本次工装净占用，正数表示占用、负数表示释放 */
    private BigDecimal toolUsedQty;

    /** 结算后剩余工装数量 */
    private BigDecimal remainingToolQty;
}
