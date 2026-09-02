package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程进度监听器公共接口。
 */
@FunctionalInterface
public interface ScheduleProgressListener {

    /**
     * 更新自动排程进度。
     *
     * @param progress 进度百分比
     * @param stage 阶段编码
     * @param stageName 阶段名称
     */
    void update(int progress, String stage, String stageName);
}
