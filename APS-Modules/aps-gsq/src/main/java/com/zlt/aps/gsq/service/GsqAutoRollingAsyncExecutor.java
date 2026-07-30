package com.zlt.aps.gsq.service;

/**
 * 钢丝圈自动滚动异步执行边界。
 *
 * @author APS
 */
public interface GsqAutoRollingAsyncExecutor {

    /**
     * 异步执行自动滚动任务。
     *
     * @param taskId 任务ID
     */
    void execute(String taskId);
}
