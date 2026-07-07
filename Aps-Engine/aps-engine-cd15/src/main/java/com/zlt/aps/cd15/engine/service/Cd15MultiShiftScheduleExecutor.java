package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftScheduleResult;

/**
 * CD15 多班滚动试排执行器。
 */
public interface Cd15MultiShiftScheduleExecutor {

    /**
     * 按成型逐班自然需求执行 CD15 试排。
     *
     * @param input 自动排程输入
     * @return 多班试排结果
     */
    Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input);
}