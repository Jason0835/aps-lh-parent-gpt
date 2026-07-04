package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;

/** CD90定时滚动排程异步执行边界。 */
public interface Cd90TimedRollingAsyncExecutor {

    /** 异步执行已创建的滚动任务。 */
    void execute(String taskId, Cd90RollingTarget target, String inputVersion);
}
