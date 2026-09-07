package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程机台分配步骤公共服务接口。
 *
 * @param <C> 排程上下文类型
 */
public interface IScheduleMachineAssignService<C extends ScheduleContextModel<?, ?, ?, ?, ?>> {

    /**
     * 执行机台分配。
     *
     * @param context 排程上下文
     */
    void assign(C context);
}
