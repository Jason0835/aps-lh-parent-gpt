package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15RollingTarget;
import com.zlt.aps.cd15.engine.model.Cd15TimedRollingOutput;
import org.redisson.api.RLock;

/** CD15定时滚动排程最终持久化服务。 */
public interface Cd15TimedRollingPersistService {

    /**
     * 在短事务内替换目标班次及后续班次排程结果。
     *
     * @param taskId 任务ID
     * @param target 滚动目标
     * @param output 试排输出
     * @param lock 排程日执行锁
     */
    void persist(String taskId, Cd15RollingTarget target, Cd15TimedRollingOutput output, RLock lock);
}