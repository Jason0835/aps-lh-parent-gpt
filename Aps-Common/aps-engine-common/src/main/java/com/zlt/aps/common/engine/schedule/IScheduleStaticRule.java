package com.zlt.aps.common.engine.schedule;

/**
 * 支持静态候选校验的通用排程规则接口。
 *
 * <p>静态校验用于顺延前瞻，只判断不包含当前班次动态产能的硬约束；完整候选评估仍使用
 * {@link #evaluate(Object, Object)}。领域过滤接口可继续保留自己的默认实现和解释字段。</p>
 *
 * @param <T> 被校验目标类型
 * @param <C> 排程上下文类型
 */
public interface IScheduleStaticRule<T, C> extends IScheduleRule<T, C> {

    /**
     * 执行不包含当前班次动态产能的静态规则校验。
     *
     * @param target  被校验目标
     * @param context 排程上下文
     * @return 静态规则结果
     */
    ScheduleRuleResult evaluateStatic(T target, C context);
}
