package com.zlt.aps.tm.service;

/**
 * 胎面人工操作异步执行器。
 */
public interface TmOperationAsyncExecutor {

    /**
     * 异步执行指定人工任务。
     *
     * @param taskId 任务编号
     */
    void execute(String taskId);
}
