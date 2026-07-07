package com.zlt.aps.tm.service;

/**
 * 胎面自动排程异步执行边界。
 */
public interface TmAutoScheduleAsyncExecutor {

    /**
     * 执行胎面自动排程任务。
     *
     * @param taskId 对外任务 ID
     */
    void execute(String taskId);
}