package com.zlt.aps.cd90.engine.model;

import com.zlt.aps.cd90.engine.domain.Cd90ScheduleTask;

/** PENDING任务创建结果，用于区分本次新建和幂等返回的已有任务。 */
public final class Cd90ScheduleTaskCreationResult {

    private final Cd90ScheduleTask task;
    private final boolean created;

    private Cd90ScheduleTaskCreationResult(Cd90ScheduleTask task, boolean created) {
        this.task = task;
        this.created = created;
    }

    /** 构造本次新建结果。 */
    public static Cd90ScheduleTaskCreationResult created(Cd90ScheduleTask task) {
        return new Cd90ScheduleTaskCreationResult(task, true);
    }

    /** 构造已有活动任务结果。 */
    public static Cd90ScheduleTaskCreationResult existing(Cd90ScheduleTask task) {
        return new Cd90ScheduleTaskCreationResult(task, false);
    }

    public Cd90ScheduleTask getTask() {
        return task;
    }

    public boolean isCreated() {
        return created;
    }
}
