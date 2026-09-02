package com.zlt.aps.common.engine.schedule.engine;

/** 排程解释快照公共编排引擎。 */
public final class ScheduleSnapshotEngine<C, T, R> {

    /**
     * 构建单任务解释快照。
     *
     * @param task 任务
     * @param context 上下文
     * @param policy 领域快照端口
     * @return 快照结果
     */
    public R buildTaskExplain(T task, C context, ScheduleSnapshotPolicy<C, T, R> policy) {
        R result = policy.createResult();
        if (context != null && task != null) {
            policy.populateTaskExplain(result, task, context);
        }
        policy.populateSystemAnalysis(result, task);
        return result;
    }
}
