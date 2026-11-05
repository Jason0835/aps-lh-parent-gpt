package com.zlt.aps.common.engine.schedule;

import java.util.List;

/**
 * 接收一次机台评估产生的轨迹，调用方决定轨迹的隔离和发布方式。
 *
 * @param <C> 排程上下文类型
 * @param <T> 任务类型
 * @param <M> 机台候选类型
 */
public interface ScheduleCandidateTraceCollector<C, T, M> {

    /**
     * 接收机台过滤结果。
     *
     * @param context   当前排程上下文
     * @param task      当前任务
     * @param candidate 机台候选
     * @param result    过滤结果
     */
    void recordFilter(C context, T task, M candidate, ScheduleRuleResult result);

    /**
     * 接收机台评分结果。
     *
     * @param context   当前排程上下文
     * @param task      当前任务
     * @param candidate 机台候选
     * @param result    评分结果
     */
    void recordScore(C context, T task, M candidate, ScheduleScoreResult result);

    /**
     * 接收所有机台候选轨迹。
     *
     * @param context    当前排程上下文
     * @param task       当前任务
     * @param candidates 全部机台候选
     */
    void recordCandidates(C context, T task, List<M> candidates);
}
