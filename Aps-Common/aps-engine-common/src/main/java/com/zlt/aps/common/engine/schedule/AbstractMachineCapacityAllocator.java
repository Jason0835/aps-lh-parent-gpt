package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;

/**
 * 公共机台容量拆分算法模板。
 *
 * <p>模板固定“请求量归一化 → 机台剩余产能归一化 → 本班承接量 → 溢出量”的顺序，
 * 领域实现只提供任务中的请求计划量。产能回退、同班其他机台承接、顺延、工装结算和任务链操作
 * 仍由 TM、TC 各自服务负责，避免把领域状态误合并到公共层。</p>
 *
 * @param <T> 领域任务类型
 */
public abstract class AbstractMachineCapacityAllocator<T> {

    /**
     * 执行一次公共机台容量分配。
     *
     * @param task 当前领域任务
     * @param remainCapacity 当前选中机台的分配前剩余产能
     * @return 本班承接量、溢出量和完整承接标识
     * @throws IllegalArgumentException 任务为空时抛出
     */
    public final ScheduleCapacityAllocationResult allocate(T task, BigDecimal remainCapacity) {
        this.validateTask(task);
        BigDecimal requestedPlanQty = this.nonNegative(this.resolveRequestedPlanQty(task));
        BigDecimal normalizedRemainCapacity = this.nonNegative(remainCapacity);
        BigDecimal assignedPlanQty = requestedPlanQty.min(normalizedRemainCapacity);
        BigDecimal overflowPlanQty = requestedPlanQty.subtract(assignedPlanQty).max(BigDecimal.ZERO);
        return new ScheduleCapacityAllocationResult(requestedPlanQty, assignedPlanQty,
                overflowPlanQty, normalizedRemainCapacity);
    }

    /**
     * 校验领域任务。
     *
     * @param task 领域任务
     */
    protected void validateTask(T task) {
        if (task == null) {
            throw new IllegalArgumentException("机台容量分配任务不能为空");
        }
    }

    /**
     * 读取领域任务的请求计划量。
     *
     * @param task 领域任务
     * @return 请求计划量
     */
    protected abstract BigDecimal resolveRequestedPlanQty(T task);

    /**
     * 将空值或负值归一为零。
     *
     * @param value 原始数值
     * @return 非负数值
     */
    protected BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
