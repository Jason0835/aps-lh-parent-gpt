package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * TM/TC 当前班任务分配决策纯计算器。
 *
 * <p>只统一零计划、工装溢出和产能溢出的分支口径，不负责机台、任务链、工装状态或顺延落库。</p>
 */
public final class ScheduleTaskAllocationDecisionCalculator {

    /**
     * 计算当前班任务的分配分支和合计顺延量。
     *
     * @param currentShiftPlanQty 当前班最终计划量
     * @param toolOverflowQty 工装限制溢出量
     * @param capacityOverflowQty 产能限制溢出量
     * @return 任务分配决策
     */
    public ScheduleTaskAllocationDecision calculate(BigDecimal currentShiftPlanQty, BigDecimal toolOverflowQty,
                                                    BigDecimal capacityOverflowQty) {
        return new ScheduleTaskAllocationDecision(this.nonNegative(currentShiftPlanQty),
                this.nonNegative(toolOverflowQty), this.nonNegative(capacityOverflowQty));
    }

    /**
     * 将空值或负值归零。
     *
     * @param value 原始数值
     * @return 非负数值
     */
    private BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
