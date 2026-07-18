package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;

/** 单班自动排程执行边界。 */
public interface Cd15SingleShiftScheduleService {

    /**
     * 执行一个斜裁班次的候选、试算和资源提交。
     */
    Cd15ShiftExecutionResult execute(Cd15AutoScheduleContext context,
                                     Cd15AutoScheduleInput input,
                                     Cd15ShiftDescriptor shift,
                                     Cd15ShiftResourceState initialState,
                                     Cd15RollingScheduleContext rolling);
}
