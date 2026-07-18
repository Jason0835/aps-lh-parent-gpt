package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;

/** CD15定时滚动排程单任务执行服务。 */
public interface Cd15TimedRollingExecutionService {

    /** 在后台线程中完成锁、Engine计算和原子持久化。 */
    void execute(String taskId, Cd15RollingTarget target, String inputVersion);
}
