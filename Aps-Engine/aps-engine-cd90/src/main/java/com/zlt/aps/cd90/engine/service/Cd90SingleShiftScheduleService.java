package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;

/** 单班自动排程执行边界。 */
public interface Cd90SingleShiftScheduleService {

    /**
     * 执行一个直裁班次的候选、试算和资源提交。
     */
    Cd90ShiftExecutionResult execute(Cd90AutoScheduleContext context,
                                     Cd90AutoScheduleInput input,
                                     Cd90ShiftDescriptor shift,
                                     Cd90ShiftResourceState initialState,
                                     Cd90RollingScheduleContext rolling);
}
