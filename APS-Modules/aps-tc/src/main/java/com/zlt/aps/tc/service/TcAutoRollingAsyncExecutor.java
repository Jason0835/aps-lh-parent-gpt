package com.zlt.aps.tc.service;

/**
 * 胎侧自动滚动异步执行边界。
 */
public interface TcAutoRollingAsyncExecutor {

    /**
     * 异步执行自动滚动任务。
     *
     * @param taskId 任务ID
     */
    void execute(String taskId);
}
