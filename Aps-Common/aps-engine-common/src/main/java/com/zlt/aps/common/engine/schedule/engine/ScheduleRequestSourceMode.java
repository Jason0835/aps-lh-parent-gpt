package com.zlt.aps.common.engine.schedule.engine;

/**
 * 两阶段请求来源；不决定损耗结算路径。
 */
public enum ScheduleRequestSourceMode {
    NORMAL, LEAD, PRESET_PRELOSS, PRESET_POSTLOSS, NEW_SPEC, EXPERIMENT,
    FUTURE_SUPPLEMENT, BLOCKED, RETAINED_FACT
}
