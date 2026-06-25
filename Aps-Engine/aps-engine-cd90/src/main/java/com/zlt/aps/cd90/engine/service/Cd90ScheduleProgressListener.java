package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;

/** 多班自动排程进度监听器，由编排层转换为任务心跳。 */
public interface Cd90ScheduleProgressListener {

    Cd90ScheduleProgressListener NO_OP = (progress, stage, stageName, shift) -> { };

    /**
     * 接收排程进度事件。
     *
     * @param progress 进度百分比
     * @param stage 阶段编码
     * @param stageName 阶段名称
     * @param shift 当前班次；非班次事件允许为空
     */
    void onProgress(int progress, String stage, String stageName, Cd90ShiftDescriptor shift);
}
