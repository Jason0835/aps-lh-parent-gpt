package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleRequest;
import com.zlt.aps.cd15.engine.model.Cd15SingleShiftScheduleResult;

/**
 * CD15 单规格单班排程执行器。
 */
public interface Cd15SingleShiftScheduleExecutor {

    Cd15SingleShiftScheduleResult execute(Cd15SingleShiftScheduleRequest request);
}