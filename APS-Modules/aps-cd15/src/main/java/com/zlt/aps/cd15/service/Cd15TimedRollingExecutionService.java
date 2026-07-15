package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

/** CD15定时滚动排程执行编排服务。 */
public interface Cd15TimedRollingExecutionService {

    /**
     * 执行目标班次后缀滚动排程。
     *
     * @param taskId 任务ID
     * @param target 滚动目标
     * @param inputVersion 输入版本
     */
    void execute(String taskId, Cd15RollingTarget target, String inputVersion);
}