package com.zlt.aps.common.engine.schedule.engine;

import java.util.Comparator;
import java.util.List;

/**
 * 自动排程任务排序公共引擎。
 *
 * <p>该具体类完整固定排序来源判定、稳定排序、序号写回、轨迹和日志时机。</p>
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public final class TaskSortEngine<C, T extends ScheduleSortableTask> {

    /**
     * 执行任务排序。
     *
     * @param context 排程上下文
     * @param policy 领域排序策略
     * @param tracePort 领域轨迹端口
     */
    public void sort(C context, TaskSortPolicy<C, T> policy, TaskSortTracePort<C, T> tracePort) {
        policy.validateContext(context);
        List<T> taskList = policy.getTaskList(context);
        if (taskList == null || taskList.isEmpty()) {
            return;
        }

        String strategyCode = policy.resolveStrategyCode(context);
        String beforeOrder = tracePort.summarizeTaskOrder(context);
        boolean planCalcOrderReady = taskList.stream()
                .allMatch(task -> task != null && task.getPlanCalcOrderIndex() != null);
        String sortSource = planCalcOrderReady ? "PLAN_CALC_ORDER" : "LEGACY_TASK_SORT";
        if (planCalcOrderReady) {
            taskList.sort(Comparator
                    .comparing((T task) -> task.getPlanCalcOrderIndex(),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(task -> this.defaultString(task.getBusinessKey())));
        }
        else {
            Comparator<T> originalComparator = policy.buildStrategyComparator(context, strategyCode);
            taskList.sort(this.buildStartupAwareComparator(originalComparator,
                    policy.prependSupplyHoursPriority(context, strategyCode)));
        }

        String afterOrder = tracePort.summarizeTaskOrder(context);
        tracePort.logTaskSortSummary(context, strategyCode, sortSource, beforeOrder, afterOrder, taskList.size());
        for (int index = 0; index < taskList.size(); index++) {
            T task = taskList.get(index);
            int sortIndex = planCalcOrderReady && task.getPlanCalcOrderIndex() != null
                    ? task.getPlanCalcOrderIndex() : index + 1;
            task.setBaseSortIndex(sortIndex);
            tracePort.recordTaskSort(context, task, strategyCode, sortSource, sortIndex,
                    policy.isStartupShift(context, task));
            tracePort.logTaskSortDetail(context, task, strategyCode, sortSource, sortIndex);
        }
    }

    /**
     * 构建班次、供应时长和领域策略组成的兼容比较器。
     *
     * @param originalComparator 领域比较器
     * @param prependSupplyHoursPriority 是否追加供应时长排序
     * @return 公共兼容比较器
     */
    private Comparator<T> buildStartupAwareComparator(Comparator<T> originalComparator,
                                                       boolean prependSupplyHoursPriority) {
        Comparator<T> comparator = Comparator
                .comparing((T task) -> task.getShiftOrder(),
                        Comparator.nullsLast(Comparator.naturalOrder()));
        if (prependSupplyHoursPriority) {
            comparator = comparator.thenComparing((T task) -> task.getSupplyHours(),
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        return comparator.thenComparing(originalComparator);
    }

    /**
     * 将空业务键转换为空串，保持稳定排序口径。
     *
     * @param value 原始业务键
     * @return 非空业务键
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
