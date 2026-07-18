package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

/** CD15定时滚动排程异步执行边界。 */
public interface Cd15TimedRollingAsyncExecutor {

    /** 异步执行已创建的滚动任务。 */
    void execute(String taskId, Cd15RollingTarget target, String inputVersion);
}
