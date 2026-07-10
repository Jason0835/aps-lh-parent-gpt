package com.zlt.aps.cd15.engine.constant;

/**
 * 斜裁排程异步任务类型。
 */
public final class Cd15ScheduleTaskType {

    /** 自动排程。 */
    public static final String AUTO_SCHEDULE = "AUTO_SCHEDULE";
    /** 插单滚动重排。 */
    public static final String INSERT_ORDER = "INSERT_ORDER";
    /** 转机台滚动重排。 */
    public static final String TRANSFER_MACHINE = "TRANSFER_MACHINE";
    /** 调量滚动重排。 */
    public static final String CHANGE_QTY = "CHANGE_QTY";
    /** 自动滚动排程。 */
    public static final String TIMED_ROLLING = "TIMED_ROLLING";

    private Cd15ScheduleTaskType() {
    }
}
