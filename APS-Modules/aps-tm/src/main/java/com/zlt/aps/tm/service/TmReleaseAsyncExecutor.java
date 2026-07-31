package com.zlt.aps.tm.service;

/**
 * 胎面排程异步下发MES执行器。
 */
public interface TmReleaseAsyncExecutor {

    /**
     * 异步装配并下发发布数据。
     *
     * @param taskId 发布任务ID
     */
    void execute(String taskId);
}
