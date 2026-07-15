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

    /**
     * 按成型逐班自然需求执行 CD15 试排，并回调班次进度。
     *
     * @param input 自动排程输入
     * @param listener 进度监听器
     * @return 多班试排结果
     */
    Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, Cd15ScheduleProgressListener listener);

    /**
     * 从指定 CLASS 序号开始执行后缀试排，调用前会扣减输入中的前缀已排资源。
     *
     * @param input 自动排程输入
     * @param startClassIndex 起排 CLASS 序号
     * @return 多班试排结果
     */
    Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, int startClassIndex);

    /**
     * 从指定 CLASS 序号开始执行后缀试排，并回调班次进度。
     *
     * @param input 自动排程输入
     * @param startClassIndex 起排 CLASS 序号
     * @param listener 进度监听器
     * @return 多班试排结果
     */
    Cd15MultiShiftScheduleResult execute(Cd15AutoScheduleInput input, int startClassIndex,
                                         Cd15ScheduleProgressListener listener);
}