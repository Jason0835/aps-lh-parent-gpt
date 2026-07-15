package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;
import org.redisson.api.RLock;

import java.time.LocalDate;

/** CD15自动排程最终落库服务。 */
public interface Cd15AutoSchedulePersistService {

    String persist(String taskId, String factoryCode, LocalDate scheduleDate,
                   Cd15MultiShiftScheduleResult output, RLock lock);
}