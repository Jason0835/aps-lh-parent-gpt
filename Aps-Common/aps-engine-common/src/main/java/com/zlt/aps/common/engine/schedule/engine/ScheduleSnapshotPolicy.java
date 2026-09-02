package com.zlt.aps.common.engine.schedule.engine;

/** 排程快照遍历领域端口。 */
public interface ScheduleSnapshotPolicy<C, T, R> {

    R createResult();

    void populateTaskExplain(R result, T task, C context);

    void populateSystemAnalysis(R result, T task);
}
