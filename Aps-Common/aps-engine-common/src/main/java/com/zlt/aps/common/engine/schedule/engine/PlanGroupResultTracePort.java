package com.zlt.aps.common.engine.schedule.engine;

import java.util.Map;

/**
 * 计划量汇总组结果轨迹端口。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 * @param <G> 计划组类型
 */
public interface PlanGroupResultTracePort<C, T extends ScheduleTaskDraftModel,
        G extends SchedulePlanTaskGroup<T>> {

    /**
     * 记录来源任务分摊轨迹。
     *
     * @param context       排程上下文
     * @param sourceTask    来源任务
     * @param aggregateTask 汇总任务
     * @param taskGroup     计划组
     * @param evidence      公共证据
     */
    void traceSourceAllocation(C context, T sourceTask, T aggregateTask, G taskGroup,
                               Map<String, Object> evidence);

    /**
     * 记录汇总任务轨迹和过程日志。
     *
     * @param context       排程上下文
     * @param aggregateTask 汇总任务
     * @param taskGroup     计划组
     * @param evidence      公共证据
     */
    void traceAggregateResult(C context, T aggregateTask, G taskGroup, Map<String, Object> evidence);
}
