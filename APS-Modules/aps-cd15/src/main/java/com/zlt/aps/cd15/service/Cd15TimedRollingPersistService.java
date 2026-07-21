package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import org.redisson.api.RLock;

/** CD15定时滚动排程最终短事务。 */
public interface Cd15TimedRollingPersistService {

    /** 校验锁和输入版本后，原子提交原批次差异及双快照日志。 */
    void persist(String taskId, Cd15RollingTarget target,
                 Cd15TimedRollingOutput output, RLock lock);
}
