package com.zlt.aps.common.engine.schedule;

/**
 * 通用排程规则过滤接口。
 *
 * <p>用于机台过滤、任务可排校验等可扩展规则。规则实现只负责判断和返回证据，
 * 不直接修改任务链，不吞异常。</p>
 *
 * @param <T> 被校验目标类型
 * @param <C> 排程上下文类型
 */
public interface IScheduleRule<T, C> {

    /**
     * 获取规则编码。
     *
     * @return 规则编码，用于日志和解释信息
     */
    String getRuleCode();

    /**
     * 执行规则判断。
     *
     * @param target  被校验目标
     * @param context 排程上下文
     * @return 规则判断结果，包含是否通过、原因编码、原因描述和证据
     */
    ScheduleRuleResult evaluate(T target, C context);
}
