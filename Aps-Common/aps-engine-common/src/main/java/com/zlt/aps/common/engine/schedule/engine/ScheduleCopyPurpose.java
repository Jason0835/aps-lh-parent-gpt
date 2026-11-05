package com.zlt.aps.common.engine.schedule.engine;

/**
 * 派生用途由调用者指定，试算和诊断不能提交生产事实。
 */
public enum ScheduleCopyPurpose {
    BASE_ASSIGN, BASE_CARRY, EARLY_FILL, PROBE, UNPLANNED_DIAGNOSTIC
}
