package com.zlt.aps.tm.engine.service;

/**
 * 胎面自动排程进度监听器。
 */
@FunctionalInterface
public interface TmAutoScheduleProgressListener {

    /**
     * 更新自动排程进度。
     *
     * @param progress  进度百分比
     * @param stage     阶段编码
     * @param stageName 阶段名称
     */
    void update(int progress, String stage, String stageName);
}
