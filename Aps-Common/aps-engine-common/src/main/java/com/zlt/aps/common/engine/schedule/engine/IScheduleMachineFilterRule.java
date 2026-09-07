package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.IScheduleStaticRule;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;

/**
 * TM/TC 自动排程候选机台过滤公共规则接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 * @param <M> 候选机台类型
 */
public interface IScheduleMachineFilterRule<
        C extends ScheduleContextModel<T, ?, ?, ?, M>,
        T extends ScheduleTaskDraftModel,
        M extends ScheduleMachineCandidateModel>
        extends IScheduleStaticRule<M, ScheduleMachineRuleContext<T, C>> {

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    String getRuleCode();

    /**
     * 执行机台过滤规则。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果
     */
    ScheduleRuleResult evaluate(M candidate, ScheduleMachineRuleContext<T, C> context);

    /**
     * 执行不含当前班次剩余产能的静态机台过滤。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果
     */
    @Override
    default ScheduleRuleResult evaluateStatic(M candidate, ScheduleMachineRuleContext<T, C> context) {
        return this.evaluate(candidate, context);
    }
}
