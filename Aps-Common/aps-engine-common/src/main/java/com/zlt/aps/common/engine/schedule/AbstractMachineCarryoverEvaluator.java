package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公共机台顺延静态前瞻模板。
 *
 * <p>模板固定当前班静态可行性、当前班容量耗尽判断、后续班次逐班静态校验和原任务班次恢复顺序。
 * TM/TC 只适配候选复制、任务运行态补齐、班次窗口、任务班次读写和候选剩余产能，
 * 不把顺延任务、工装账本或未排枚举下沉到公共模块。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 待排任务类型
 * @param <M> 候选机台类型
 * @param <RC> 静态规则上下文类型
 */
public abstract class AbstractMachineCarryoverEvaluator<C, T, M, RC> {

    /**
     * 判断当前任务是否仅受当前班动态产能或开班状态影响，可以继续探测后续班次。
     *
     * @param context          排程上下文
     * @param task             当前任务
     * @param sourceCandidates 原始候选机台
     * @param currentShiftOrder 当前班次序号
     * @param maxShiftOrder    最大班次序号
     * @param filterRule       静态机台过滤规则
     * @return 当前静态候选全部无产能或后续班次存在静态候选时返回 true
     * @throws IllegalArgumentException 过滤规则为空时抛出
     */
    public final boolean isCapacityOrShiftBlockedCarryover(C context, T task,
                                                            List<M> sourceCandidates,
                                                            int currentShiftOrder,
                                                            int maxShiftOrder,
                                                            IScheduleStaticRule<M, RC> filterRule) {
        if (filterRule == null) {
            throw new IllegalArgumentException("静态机台过滤规则不能为空");
        }
        if (sourceCandidates == null || sourceCandidates.isEmpty()) {
            return false;
        }

        List<M> staticCandidates = this.copyCandidates(sourceCandidates);
        this.prepareCandidatesForTask(task, context, staticCandidates);
        RC ruleContext = this.buildRuleContext(task, context);
        List<M> staticPassedCandidates = staticCandidates.stream()
                .filter(candidate -> filterRule.evaluateStatic(candidate, ruleContext).isPassed())
                .collect(Collectors.toList());
        if (!staticPassedCandidates.isEmpty()
                && staticPassedCandidates.stream()
                .allMatch(candidate -> this.normalizeCapacity(this.getRemainCapacity(candidate))
                        .compareTo(BigDecimal.ZERO) <= 0)) {
            return true;
        }
        return this.hasFutureStaticCandidate(context, task, sourceCandidates, currentShiftOrder,
                maxShiftOrder, filterRule);
    }

    /**
     * 按后续班次逐班执行静态规则，判断是否存在可承接机台。
     *
     * @param context          排程上下文
     * @param task             当前任务
     * @param sourceCandidates 原始候选机台
     * @param currentShiftOrder 当前班次序号
     * @param maxShiftOrder    最大班次序号
     * @param filterRule       静态机台过滤规则
     * @return 存在后续静态候选返回 true
     */
    private boolean hasFutureStaticCandidate(C context, T task, List<M> sourceCandidates,
                                              int currentShiftOrder, int maxShiftOrder,
                                              IScheduleStaticRule<M, RC> filterRule) {
        Integer originalShiftOrder = this.getTaskShiftOrder(task);
        try {
            for (int futureShiftOrder = currentShiftOrder + 1;
                 futureShiftOrder <= maxShiftOrder; futureShiftOrder++) {
                if (!this.isShiftAvailable(context, futureShiftOrder)) {
                    continue;
                }
                this.setTaskShiftOrder(task, futureShiftOrder);
                List<M> futureCandidates = this.copyCandidates(sourceCandidates);
                this.prepareCandidatesForTask(task, context, futureCandidates);
                RC futureRuleContext = this.buildRuleContext(task, context);
                if (futureCandidates.stream()
                        .anyMatch(candidate -> filterRule.evaluateStatic(candidate, futureRuleContext).isPassed())) {
                    return true;
                }
            }
            return false;
        } finally {
            this.setTaskShiftOrder(task, originalShiftOrder);
        }
    }

    /**
     * 复制候选机台列表。
     *
     * @param sourceCandidates 原始候选机台
     * @return 候选机台副本
     */
    protected abstract List<M> copyCandidates(List<M> sourceCandidates);

    /**
     * 按当前任务补齐候选机台运行态。
     *
     * @param task       当前任务
     * @param context    排程上下文
     * @param candidates 候选机台副本
     */
    protected abstract void prepareCandidatesForTask(T task, C context, List<M> candidates);

    /**
     * 创建静态规则上下文。
     *
     * @param task    当前任务
     * @param context 排程上下文
     * @return 规则上下文
     */
    protected abstract RC buildRuleContext(T task, C context);

    /**
     * 判断指定后续班次是否有有效班次窗口。
     *
     * @param context    排程上下文
     * @param shiftOrder 待检查班次
     * @return 班次可尝试返回 true
     */
    protected abstract boolean isShiftAvailable(C context, int shiftOrder);

    /**
     * 读取任务当前班次。
     *
     * @param task 当前任务
     * @return 原始班次，可为空
     */
    protected abstract Integer getTaskShiftOrder(T task);

    /**
     * 临时设置任务班次用于后续班次静态前瞻。
     *
     * @param task       当前任务
     * @param shiftOrder 待设置班次
     */
    protected abstract void setTaskShiftOrder(T task, Integer shiftOrder);

    /**
     * 读取候选机台剩余产能。
     *
     * @param candidate 候选机台
     * @return 剩余产能，可为空
     */
    protected abstract BigDecimal getRemainCapacity(M candidate);

    /**
     * 将空值统一为零，供静态容量耗尽判断使用。
     *
     * @param capacity 原始剩余产能
     * @return 非空剩余产能
     */
    private BigDecimal normalizeCapacity(BigDecimal capacity) {
        return capacity == null ? BigDecimal.ZERO : capacity;
    }
}
