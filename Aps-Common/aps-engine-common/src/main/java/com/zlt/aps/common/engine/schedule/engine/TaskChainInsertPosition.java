package com.zlt.aps.common.engine.schedule.engine;

/** 任务链插入位置公共视图。 */
public interface TaskChainInsertPosition {

    String getMachineCode();

    Integer getShiftOrder();

    String getAnchorTaskId();
}
