package com.zlt.aps.common.engine.schedule;

/**
 * 通用排程评分策略接口。
 *
 * <p>用于候选机台、候选任务等排序评分。策略只计算评分结果，不直接修改任务链。</p>
 *
 * @param <T> 被评分目标类型
 * @param <C> 排程上下文类型
 */
public interface IScheduleScoreStrategy<T, C> {

    /**
     * 获取评分策略编码。
     *
     * @return 评分策略编码，用于日志和解释信息
     */
    String getStrategyCode();

    /**
     * 对目标对象评分。
     *
     * @param target  被评分目标
     * @param context 排程上下文
     * @return 评分结果，包含评分项、总分和说明
     */
    ScheduleScoreResult score(T target, C context);
}
