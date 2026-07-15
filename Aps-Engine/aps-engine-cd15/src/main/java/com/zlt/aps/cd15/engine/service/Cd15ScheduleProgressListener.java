package com.zlt.aps.cd15.engine.service;

/** CD15 自动排程进度回调。 */
@FunctionalInterface
public interface Cd15ScheduleProgressListener {

    Cd15ScheduleProgressListener NO_OP = (progress, stage, stageName) -> { };

    /**
     * 更新排程任务进度。
     *
     * @param progress 进度百分比
     * @param stage 阶段编码
     * @param stageName 阶段名称
     */
    void onProgress(int progress, String stage, String stageName);
}