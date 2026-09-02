package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;

/**
 * 任务链公共引擎领域策略端口。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 */
public interface TaskChainPolicy<C, T extends ScheduleTaskDraftModel> {

    RuntimeException error(TaskChainErrorType errorType, String detail);

    void recalculateChainTimes(C context, ScheduleTaskLinkedList<T> chain,
                               String machineCode, Integer shiftOrder);

    void traceChainState(C context, ScheduleTaskLinkedList<T> chain, String operation,
                         String machineCode, Integer shiftOrder, String changedTaskId);
}
