package com.zlt.aps.tc.service;

/**
 * 胎侧自动排程异步执行边界。
 */
public interface TcAutoScheduleAsyncExecutor {

    /**
     * 执行胎侧自动排程任务。
     *
     * @param taskId 对外任务 ID
     */
    void execute(String taskId);
}