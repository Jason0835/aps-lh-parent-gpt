package com.zlt.aps.tc.service;

/**
 * 胎侧排程发布异步执行边界。
 */
public interface TcReleaseAsyncExecutor {

    /**
     * 异步执行发布任务。
     *
     * @param taskId 发布任务ID
     */
    void execute(String taskId);
}
