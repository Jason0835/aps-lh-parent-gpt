package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * TM/TC 共用的结果数量守恒纯计算器。
 *
 * <p>只比较来源最终计划量、已排量和未排量，不读取领域上下文，也不决定结果、未排和解释表的落库方式。</p>
 */
public final class ScheduleResultConservationCalculator {

    /**
     * 计算来源任务与结果分流数量的守恒差额。
     *
     * @param sourceFinalPlanQty 来源任务最终计划量
     * @param assignedQty 已排任务计划量
     * @param unplannedQty 未排任务计划量
     * @return 数量守恒结果
     */
    public ScheduleResultConservationResult calculate(BigDecimal sourceFinalPlanQty, BigDecimal assignedQty,
                                                      BigDecimal unplannedQty) {
        return new ScheduleResultConservationResult(this.nvl(sourceFinalPlanQty), this.nvl(assignedQty),
                this.nvl(unplannedQty));
    }

    /**
     * 空数值按零处理。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
