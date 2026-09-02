package com.zlt.aps.common.engine.schedule.engine;

import java.util.Comparator;
import java.util.List;

/**
 * 自动排程任务排序领域策略端口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface TaskSortPolicy<C, T extends ScheduleSortableTask> {

    /**
     * 校验领域上下文。
     *
     * @param context 排程上下文
     */
    void validateContext(C context);

    /**
     * 获取待排序任务。
     *
     * @param context 排程上下文
     * @return 待排序任务
     */
    List<T> getTaskList(C context);

    /**
     * 获取排序策略编码。
     *
     * @param context 排程上下文
     * @return 排序策略编码
     */
    String resolveStrategyCode(C context);

    /**
     * 构建领域排序比较器。
     *
     * @param context 排程上下文
     * @param strategyCode 策略编码
     * @return 领域比较器
     */
    Comparator<T> buildStrategyComparator(C context, String strategyCode);

    /**
     * 判断公共引擎是否需要在领域比较器前追加 supplyHours 升序。
     *
     * @param context 排程上下文
     * @param strategyCode 排序策略编码
     * @return 是否追加 supplyHours 升序；默认追加以保持旧策略行为
     */
    default boolean prependSupplyHoursPriority(C context, String strategyCode) {
        return true;
    }

    /**
     * 判断任务是否属于开产班次。
     *
     * @param context 排程上下文
     * @param task 排程任务
     * @return 是否属于开产班次
     */
    boolean isStartupShift(C context, T task);
}
