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

    /**
     * 成型总余量额度封顶扣减量。
     */
    private final BigDecimal quotaCappedQty;

    /**
     * 来源量减已排量、未排量和额度封顶扣减量后的差额。
     */
    private final BigDecimal difference;

    /**
     * 创建数量守恒结果，不含额度封顶扣减量。
     *
     * @param sourceFinalPlanQty 来源任务最终计划量
     * @param assignedQty 已排任务计划量
     * @param unplannedQty 未排任务计划量
     */
    public ScheduleResultConservationResult(BigDecimal sourceFinalPlanQty, BigDecimal assignedQty,
                                            BigDecimal unplannedQty) {
        this(sourceFinalPlanQty, assignedQty, unplannedQty, BigDecimal.ZERO);
    }

    /**
     * 创建数量守恒结果。
     *
     * <p>守恒式扩展为四项：来源最终计划量 = 已排量 + 未排量 + 额度封顶扣减量。
     * 额度封顶扣减量是成型总余量封顶造成的独立去向，既不进已排也不进未排。</p>
     *
     * @param sourceFinalPlanQty 来源任务最终计划量
     * @param assignedQty        已排任务计划量
     * @param unplannedQty       未排任务计划量
     * @param quotaCappedQty     成型总余量额度封顶扣减量
     */
    public ScheduleResultConservationResult(BigDecimal sourceFinalPlanQty, BigDecimal assignedQty,
                                            BigDecimal unplannedQty, BigDecimal quotaCappedQty) {
        this.sourceFinalPlanQty = sourceFinalPlanQty;
        this.assignedQty = assignedQty;
        this.unplannedQty = unplannedQty;
        this.quotaCappedQty = quotaCappedQty;
        this.difference = sourceFinalPlanQty
                .subtract(assignedQty.add(unplannedQty).add(quotaCappedQty));
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
     * 获取成型总余量额度封顶扣减量。
     *
     * @return 额度封顶扣减量
     */
    public BigDecimal getQuotaCappedQty() {
        return this.quotaCappedQty;
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
     * 判断来源量是否等于已排量、未排量与额度封顶扣减量之和。
     *
     * @return 差额为零时返回 true
     */
    public boolean isBalanced() {
        return this.difference.compareTo(BigDecimal.ZERO) == 0;
    }
}
