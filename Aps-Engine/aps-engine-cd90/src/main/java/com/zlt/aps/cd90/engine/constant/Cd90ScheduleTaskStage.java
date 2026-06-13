package com.zlt.aps.cd90.engine.constant;

/**
 * 直裁自动排程任务阶段编码。
 */
public final class Cd90ScheduleTaskStage {

    public static final String VALIDATE_DATA = "VALIDATE_DATA";
    public static final String LOAD_INPUT = "LOAD_INPUT";
    public static final String INIT_RESOURCE = "INIT_RESOURCE";
    public static final String SCHEDULE_SHIFT = "SCHEDULE_SHIFT";
    public static final String BUILD_UNSCHEDULED = "BUILD_UNSCHEDULED";
    public static final String SAVE_RESULT = "SAVE_RESULT";
    public static final String COMPLETE = "COMPLETE";

    private Cd90ScheduleTaskStage() {
    }
}
