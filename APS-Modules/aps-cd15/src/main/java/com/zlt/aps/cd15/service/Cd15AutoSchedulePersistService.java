package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;
import org.redisson.api.RLock;

/** 自动排程最终短事务服务。 */
public interface Cd15AutoSchedulePersistService {

    /**
     * 复核版本并保存一个完整自动排程批次。
     *
     * @return 新排程批次号
     */
    String persist(String taskId, Cd15AutoScheduleContext context,
                   Cd15AutoScheduleOutputDraft output, RLock lock);
}
