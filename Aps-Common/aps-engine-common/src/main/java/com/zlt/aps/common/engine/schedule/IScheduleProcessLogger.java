package com.zlt.aps.common.engine.schedule;

/**
 * 通用排程过程日志接口。
 *
 * <p>用于串联自动排程、人工调整、局部重算和落库过程。接口不限定日志落库方式，
 * 由具体业务模块实现控制日志级别、敏感信息过滤和持久化策略。</p>
 *
 * @param <C> 排程上下文类型
 */
public interface IScheduleProcessLogger<C> {

    /**
     * 记录步骤开始。
     *
     * @param context      排程上下文
     * @param stepCode     步骤编码
     * @param inputSummary 输入摘要
     */
    void logStepStart(C context, String stepCode, String inputSummary);

    /**
     * 记录步骤结束。
     *
     * @param context       排程上下文
     * @param stepCode      步骤编码
     * @param outputSummary 输出摘要
     */
    void logStepEnd(C context, String stepCode, String outputSummary);

    /**
     * 记录规则执行结果。
     *
     * @param context  排程上下文
     * @param ruleCode 规则编码
     * @param result   规则结果
     */
    void logRuleResult(C context, String ruleCode, ScheduleRuleResult result);

    /**
     * 记录任务链变更。
     *
     * @param context 排程上下文
     * @param result  链表变更结果
     */
    void logChainChange(C context, ScheduleChainChangeResult<?> result);
}
