package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.IScheduleScoreStrategy;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;

/**
 * TM/TC 自动排程候选机台评分公共策略接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 * @param <M> 候选机台类型
 */
public interface IScheduleMachineScoreStrategy<
        C extends ScheduleContextModel<T, ?, ?, ?, M>,
        T extends ScheduleTaskDraftModel,
        M extends ScheduleMachineCandidateModel>
        extends IScheduleScoreStrategy<M, ScheduleMachineRuleContext<T, C>> {

    /**
     * 获取评分策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 执行候选机台评分。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 评分结果
     */
    ScheduleScoreResult score(M candidate, ScheduleMachineRuleContext<T, C> context);
}
