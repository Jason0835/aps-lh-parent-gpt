package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.engine.domain.Cd15ScheduleTask;

/** PENDING任务创建结果，用于区分本次新建和幂等返回的已有任务。 */
public final class Cd15ScheduleTaskCreationResult {

    private final Cd15ScheduleTask task;
    private final boolean created;

    private Cd15ScheduleTaskCreationResult(Cd15ScheduleTask task, boolean created) {
        this.task = task;
        this.created = created;
    }

    /** 构造本次新建结果。 */
    public static Cd15ScheduleTaskCreationResult created(Cd15ScheduleTask task) {
        return new Cd15ScheduleTaskCreationResult(task, true);
    }

    /** 构造已有活动任务结果。 */
    public static Cd15ScheduleTaskCreationResult existing(Cd15ScheduleTask task) {
        return new Cd15ScheduleTaskCreationResult(task, false);
    }

    public Cd15ScheduleTask getTask() {
        return task;
    }

    public boolean isCreated() {
        return created;
    }
}
