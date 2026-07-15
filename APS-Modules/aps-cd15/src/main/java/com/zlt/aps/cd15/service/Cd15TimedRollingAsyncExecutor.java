package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

/** CD15定时滚动排程异步执行入口。 */
public interface Cd15TimedRollingAsyncExecutor {

    /**
     * 执行指定滚动任务。
     *
     * @param taskId 任务ID
     * @param target 滚动目标
     * @param inputVersion 输入版本
     */
    void execute(String taskId, Cd15RollingTarget target, String inputVersion);
}