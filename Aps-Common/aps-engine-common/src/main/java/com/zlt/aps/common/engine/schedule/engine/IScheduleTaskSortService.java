package com.zlt.aps.common.engine.schedule.engine;

/**
 * TM/TC 自动排程任务排序步骤公共服务接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface IScheduleTaskSortService<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> {

    /**
     * 执行待排任务排序。
     *
     * @param context 排程上下文
     */
    void sort(C context);
}
