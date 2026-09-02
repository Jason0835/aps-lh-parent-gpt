package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * 结果、未排数量守恒计算结果。
 */
public class ScheduleResultConservationResult {

    /** 来源任务最终计划量。 */
    private final BigDecimal sourceFinalPlanQty;

    /** 已排任务计划量。 */
    private final BigDecimal assignedQty;

    /** 未排任务计划量。 */
    private final BigDecimal unplannedQty;

    /** 来源量减已排量和未排量后的差额。 */
    private final BigDecimal difference;

    /**
     * 创建数量守恒结果。
     *
     * @param sourceFinalPlanQty 来源任务最终计划量
     * @param assignedQty 已排任务计划量
     * @param unplannedQty 未排任务计划量
     */
    public ScheduleResultConservationResult(BigDecimal sourceFinalPlanQty, BigDecimal assignedQty,
                                            BigDecimal unplannedQty) {
        this.sourceFinalPlanQty = sourceFinalPlanQty;
        this.assignedQty = assignedQty;
        this.unplannedQty = unplannedQty;
        this.difference = sourceFinalPlanQty.subtract(assignedQty.add(unplannedQty));
    }

    /**
     * 获取来源任务最终计划量。
     *
     * @return 来源任务最终计划量
     */
    public BigDecimal getSourceFinalPlanQty() {
        return this.sourceFinalPlanQty;
    }

    /**
     * 获取已排任务计划量。
     *
     * @return 已排任务计划量
     */
    public BigDecimal getAssignedQty() {
        return this.assignedQty;
    }

    /**
     * 获取未排任务计划量。
     *
     * @return 未排任务计划量
     */
    public BigDecimal getUnplannedQty() {
        return this.unplannedQty;
    }

    /**
     * 获取守恒差额。
     *
     * @return 守恒差额
     */
    public BigDecimal getDifference() {
        return this.difference;
    }

    /**
     * 判断来源量是否等于已排量与未排量之和。
     *
     * @return 差额为零时返回 true
     */
    public boolean isBalanced() {
        return this.difference.compareTo(BigDecimal.ZERO) == 0;
    }
}
