package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;

/** CD90定时滚动排程单任务执行服务。 */
public interface Cd90TimedRollingExecutionService {

    /** 在后台线程中完成锁、Engine计算和原子持久化。 */
    void execute(String taskId, Cd90RollingTarget target, String inputVersion);
}
