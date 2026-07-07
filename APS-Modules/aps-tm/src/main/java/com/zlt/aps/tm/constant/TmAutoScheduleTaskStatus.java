package com.zlt.aps.tm.constant;

/**
 * 胎面自动排程任务状态常量。
 */
public final class TmAutoScheduleTaskStatus {

    /** 等待执行 */
    public static final String PENDING = "PENDING";

    /** 执行中 */
    public static final String RUNNING = "RUNNING";

    /** 执行成功 */
    public static final String SUCCESS = "SUCCESS";

    /** 执行失败 */
    public static final String FAILED = "FAILED";

    private TmAutoScheduleTaskStatus() {
    }
}