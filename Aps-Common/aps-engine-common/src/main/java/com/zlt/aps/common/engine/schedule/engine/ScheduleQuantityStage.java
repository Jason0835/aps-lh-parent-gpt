package com.zlt.aps.common.engine.schedule.engine;

/**
 * 运行态数量生命周期；禁排诊断没有生产数量阶段。
 */
public enum ScheduleQuantityStage {
    PRE_LOSS_REQUEST, POST_LOSS_CANDIDATE, ASSIGNED, POST_LOSS_CARRY, POST_LOSS_PRESET
}
