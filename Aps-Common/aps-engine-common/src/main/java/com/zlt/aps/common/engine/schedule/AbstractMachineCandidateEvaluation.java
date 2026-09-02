package com.zlt.aps.common.engine.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 公共候选机台过滤、评分和轨迹回写模板。
 *
 * <p>模板固定“复制候选 → 补齐任务相关运行态 → 逐候选过滤 → 对通过候选评分 → 领域排序 → 回写候选轨迹”的顺序。
 * TM/TC 只适配候选复制、动态属性补齐、规则上下文、解释轨迹和领域排序；未排原因、顺延和任务链操作仍由各自机台分配服务负责。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 待排任务类型
 * @param <M> 候选机台类型
 * @param <RC> 规则和评分上下文类型
 */
public abstract class AbstractMachineCandidateEvaluation<C, T, M, RC>
        extends AbstractMachineCarryoverEvaluator<C, T, M, RC> {

    /**
     * 执行一次候选机台过滤、评分和排序。
     *
     * @param context 排程上下文
     * @param task 待排任务
     * @param sourceCandidates 原始候选机台
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @return 全部候选和通过过滤候选
     * @throws IllegalArgumentException 规则或评分策略为空时抛出
     */
    public final ScheduleCandidateEvaluationResult<M> evaluate(C context, T task,
                                                               List<M> sourceCandidates,
                                                               IScheduleRule<M, RC> filterRule,
                                                               IScheduleScoreStrategy<M, RC> scoreStrategy) {
        if (filterRule == null || scoreStrategy == null) {
            throw new IllegalArgumentException("机台过滤规则和评分策略不能为空");
        }
        if (sourceCandidates == null || sourceCandidates.isEmpty()) {
            return new ScheduleCandidateEvaluationResult<>(Collections.emptyList(), Collections.emptyList());
        }

        List<M> candidates = this.copyCandidates(sourceCandidates);
        this.prepareCandidatesForTask(task, context, candidates);
        RC ruleContext = this.buildRuleContext(task, context);
        List<M> passedCandidates = new ArrayList<>();
        for (M candidate : candidates) {
            ScheduleRuleResult ruleResult = filterRule.evaluate(candidate, ruleContext);
            this.recordFilterResult(context, task, candidate, ruleResult);
            if (ruleResult.isPassed()) {
                passedCandidates.add(candidate);
            }
        }
        for (M candidate : passedCandidates) {
            ScheduleScoreResult scoreResult = scoreStrategy.score(candidate, ruleContext);
            this.recordScoreResult(context, task, candidate, scoreResult);
        }
        List<M> sortedPassedCandidates = this.sortPassedCandidates(task, passedCandidates);
        this.recordCandidateTrace(context, task, candidates);
        return new ScheduleCandidateEvaluationResult<>(candidates, sortedPassedCandidates);
    }

    /**
     * 复制候选机台列表。
     *
     * @param sourceCandidates 原始候选机台
     * @return 领域候选机台副本
     */
    protected abstract List<M> copyCandidates(List<M> sourceCandidates);

    /**
     * 按当前任务补齐候选机台动态属性。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @param candidates 候选机台副本
     */
    protected abstract void prepareCandidatesForTask(T task, C context, List<M> candidates);

    /**
     * 创建规则和评分上下文。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @return 领域规则上下文
     */
    protected abstract RC buildRuleContext(T task, C context);

    /**
     * 回写单候选过滤轨迹。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param candidate 候选机台
     * @param result 过滤结果
     */
    protected abstract void recordFilterResult(C context, T task, M candidate, ScheduleRuleResult result);

    /**
     * 回写单候选评分轨迹。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param candidate 候选机台
     * @param result 评分结果
     */
    protected abstract void recordScoreResult(C context, T task, M candidate, ScheduleScoreResult result);

    /**
     * 回写全部候选轨迹。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param candidates 全部候选机台
     */
    protected abstract void recordCandidateTrace(C context, T task, List<M> candidates);

    /**
     * 按领域规则对通过过滤的候选排序。
     *
     * @param task 当前任务
     * @param passedCandidates 通过过滤并完成评分的候选机台
     * @return 排序后的候选机台
     */
    protected abstract List<M> sortPassedCandidates(T task, List<M> passedCandidates);
}
