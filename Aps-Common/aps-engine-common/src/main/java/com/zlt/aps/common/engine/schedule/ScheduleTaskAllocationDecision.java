package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * 当前班任务分配后的纯决策结果。
 */
public class ScheduleTaskAllocationDecision {

    /** 当前班最终计划量。 */
    private final BigDecimal currentShiftPlanQty;

    /** 工装限制溢出量。 */
    private final BigDecimal toolOverflowQty;

    /** 产能限制溢出量。 */
    private final BigDecimal capacityOverflowQty;

    /** 合计顺延量。 */
    private final BigDecimal carryoverQty;

    /**
     * 创建任务分配决策。
     *
     * @param currentShiftPlanQty 当前班最终计划量
     * @param toolOverflowQty 工装限制溢出量
     * @param capacityOverflowQty 产能限制溢出量
     */
    public ScheduleTaskAllocationDecision(BigDecimal currentShiftPlanQty, BigDecimal toolOverflowQty,
                                          BigDecimal capacityOverflowQty) {
        this.currentShiftPlanQty = currentShiftPlanQty;
        this.toolOverflowQty = toolOverflowQty;
        this.capacityOverflowQty = capacityOverflowQty;
        this.carryoverQty = toolOverflowQty.add(capacityOverflowQty);
    }

    /**
     * 判断当前班没有计划量且没有工装溢出。
     *
     * @return 无需追加当前班节点时返回 true
     */
    public boolean isZeroPlan() {
        return this.currentShiftPlanQty.compareTo(BigDecimal.ZERO) <= 0
                && this.toolOverflowQty.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * 判断当前班没有计划量但存在工装溢出。
     *
     * @return 需要先结算工装并全部顺延时返回 true
     */
    public boolean isToolOnlyOverflow() {
        return this.currentShiftPlanQty.compareTo(BigDecimal.ZERO) <= 0
                && this.toolOverflowQty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 获取当前班最终计划量。
     *
     * @return 当前班最终计划量
     */
    public BigDecimal getCurrentShiftPlanQty() {
        return this.currentShiftPlanQty;
    }

    /**
     * 获取工装溢出量。
     *
     * @return 工装溢出量
     */
    public BigDecimal getToolOverflowQty() {
        return this.toolOverflowQty;
    }

    /**
     * 获取产能溢出量。
     *
     * @return 产能溢出量
     */
    public BigDecimal getCapacityOverflowQty() {
        return this.capacityOverflowQty;
    }

    /**
     * 获取工装和产能合计顺延量。
     *
     * @return 合计顺延量
     */
    public BigDecimal getCarryoverQty() {
        return this.carryoverQty;
    }
}
