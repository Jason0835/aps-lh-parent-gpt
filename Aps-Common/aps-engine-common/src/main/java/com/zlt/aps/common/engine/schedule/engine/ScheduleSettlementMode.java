package com.zlt.aps.common.engine.schedule.engine;

/**
 * 数量结算路由；已结算输入禁止再次计算损耗或起排量。
 */
public enum ScheduleSettlementMode {
    NORMAL, TAIL_DUAL_INPUT, POST_LOSS_CARRY, POST_LOSS_PRESET, RETAINED_FACT
}
