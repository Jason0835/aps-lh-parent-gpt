package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * 单次机台产能分配结果。
 *
 * <p>结果只描述本班选中机台能够承接的数量和需要顺延的数量，
 * 不包含任务实体、机台实体或顺延落库动作，便于 TM、TC 复用相同的容量拆分口径。</p>
 */
public class ScheduleCapacityAllocationResult {

    /** 本次请求计划量。 */
    private final BigDecimal requestedPlanQty;

    /** 本班机台实际承接量。 */
    private final BigDecimal assignedPlanQty;

    /** 本班机台无法承接、需要后续处理的溢出量。 */
    private final BigDecimal overflowPlanQty;

    /** 分配前机台剩余产能。 */
    private final BigDecimal remainCapacity;

    /** 是否完整承接本次请求计划量。 */
    private final boolean fullyAssigned;

    /**
     * 创建容量分配结果。
     *
     * @param requestedPlanQty 本次请求计划量
     * @param assignedPlanQty 本班机台实际承接量
     * @param overflowPlanQty 需要后续处理的溢出量
     * @param remainCapacity 分配前机台剩余产能
     */
    public ScheduleCapacityAllocationResult(BigDecimal requestedPlanQty,
                                            BigDecimal assignedPlanQty,
                                            BigDecimal overflowPlanQty,
                                            BigDecimal remainCapacity) {
        this.requestedPlanQty = requestedPlanQty;
        this.assignedPlanQty = assignedPlanQty;
        this.overflowPlanQty = overflowPlanQty;
        this.remainCapacity = remainCapacity;
        this.fullyAssigned = overflowPlanQty.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 获取本次请求计划量。
     *
     * @return 请求计划量
     */
    public BigDecimal getRequestedPlanQty() {
        return requestedPlanQty;
    }

    /**
     * 获取本班实际承接量。
     *
     * @return 实际承接量
     */
    public BigDecimal getAssignedPlanQty() {
        return assignedPlanQty;
    }

    /**
     * 获取需要后续处理的溢出量。
     *
     * @return 溢出量
     */
    public BigDecimal getOverflowPlanQty() {
        return overflowPlanQty;
    }

    /**
     * 获取分配前剩余产能。
     *
     * @return 分配前剩余产能
     */
    public BigDecimal getRemainCapacity() {
        return remainCapacity;
    }

    /**
     * 判断本次请求是否被完整承接。
     *
     * @return 完整承接返回 true
     */
    public boolean isFullyAssigned() {
        return fullyAssigned;
    }
}
