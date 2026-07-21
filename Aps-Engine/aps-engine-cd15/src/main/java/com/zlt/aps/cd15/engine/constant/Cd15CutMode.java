package com.zlt.aps.cd15.engine.constant;

/**
 * 斜裁生产模式常量。
 */
public final class Cd15CutMode {

    /** 单裁。 */
    public static final String SINGLE = "SINGLE";
    /** 分裁。 */
    public static final String SPLIT = "SPLIT";
    /** 双模式机台，按任务裁断类型使用单裁或分裁能力。 */
    public static final String DAILY_OUTPUT = "DAILY_OUTPUT";

    private Cd15CutMode() {
    }
}
