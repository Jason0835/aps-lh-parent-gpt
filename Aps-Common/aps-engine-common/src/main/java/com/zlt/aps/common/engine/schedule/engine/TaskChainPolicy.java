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

    /**
     * 维护任务链的换型资源字段，不复算起止时间。
     *
     * <p>延迟时间终算模式下由公共引擎在链结构或数量变更后调用：按链邻接关系刷新
     * 各节点的换型资源消耗字段（规格切换小时、胶料切换小时及产能扣减等），供分配
     * 阶段的换型产能扣减即时读取；起止时间不在本方法维护，由分配结束后的统一终算
     * 写入。实现必须与领域时间计算器共用同一套换型公式，并复刻其异常输入下的字段
     * 置零与链内失效传播语义。</p>
     *
     * @param context     排程上下文
     * @param chain       当前机台班次任务链
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     */
    void maintainChainSwitchResources(C context, ScheduleTaskLinkedList<T> chain,
                                      String machineCode, Integer shiftOrder);

    void recalculateChainTimes(C context, ScheduleTaskLinkedList<T> chain,
                               String machineCode, Integer shiftOrder);

    void traceChainState(C context, ScheduleTaskLinkedList<T> chain, String operation,
                         String machineCode, Integer shiftOrder, String changedTaskId);
}
