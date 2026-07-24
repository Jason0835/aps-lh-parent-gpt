package com.zlt.aps.tc.service;

/**
 * 胎侧人工操作异步执行器。
 */
public interface TcOperationAsyncExecutor {

    /**
     * 异步执行指定人工任务。
     *
     * @param taskId 任务编号
     */
    void execute(String taskId);
}
