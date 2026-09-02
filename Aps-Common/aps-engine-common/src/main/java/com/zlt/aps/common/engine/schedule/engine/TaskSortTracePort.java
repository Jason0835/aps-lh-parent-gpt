package com.zlt.aps.common.engine.schedule.engine;

/**
 * 自动排程任务排序轨迹端口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface TaskSortTracePort<C, T extends ScheduleSortableTask> {

    /**
     * 汇总当前任务顺序。
     *
     * @param context 排程上下文
     * @return 顺序摘要
     */
    String summarizeTaskOrder(C context);

    /**
     * 记录任务排序规则轨迹。
     *
     * @param context 排程上下文
     * @param task 排程任务
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param sortIndex 排序序号
     * @param startupShift 是否为开产班次
     */
    void recordTaskSort(C context, T task, String strategyCode, String sortSource,
                        int sortIndex, boolean startupShift);

    /**
     * 记录排序汇总日志。
     *
     * @param context 排程上下文
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param beforeOrder 排序前摘要
     * @param afterOrder 排序后摘要
     * @param taskCount 任务数量
     */
    void logTaskSortSummary(C context, String strategyCode, String sortSource,
                            String beforeOrder, String afterOrder, int taskCount);

    /**
     * 记录任务排序明细日志。
     *
     * @param context 排程上下文
     * @param task 排程任务
     * @param strategyCode 策略编码
     * @param sortSource 排序来源
     * @param sortIndex 排序序号
     */
    void logTaskSortDetail(C context, T task, String strategyCode, String sortSource, int sortIndex);
}

