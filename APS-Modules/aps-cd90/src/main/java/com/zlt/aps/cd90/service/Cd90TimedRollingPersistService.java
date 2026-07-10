package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.model.Cd90RollingTarget;
import com.zlt.aps.cd90.engine.model.Cd90TimedRollingOutput;
import org.redisson.api.RLock;

/** CD90定时滚动排程最终短事务。 */
public interface Cd90TimedRollingPersistService {

    /** 校验锁和输入版本后，原子提交原批次差异及双快照日志。 */
    void persist(String taskId, Cd90RollingTarget target,
                 Cd90TimedRollingOutput output, RLock lock);
}
