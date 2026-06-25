package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import org.redisson.api.RLock;

/** 自动排程最终短事务服务。 */
public interface Cd90AutoSchedulePersistService {

    /**
     * 复核版本并保存一个完整自动排程批次。
     *
     * @return 新排程批次号
     */
    String persist(String taskId, Cd90AutoScheduleContext context,
                   Cd90AutoScheduleOutputDraft output, RLock lock);
}
