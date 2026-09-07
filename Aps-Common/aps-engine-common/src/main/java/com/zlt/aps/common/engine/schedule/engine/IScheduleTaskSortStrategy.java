package com.zlt.aps.common.engine.schedule.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * TM/TC 自动排程任务排序公共策略接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface IScheduleTaskSortStrategy<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 构建任务排序比较器。
     *
     * @param context 排程上下文
     * @return 任务排序比较器
     */
    Comparator<T> buildComparator(C context);

    /**
     * 判断公共排序入口是否需要追加 supplyHours 升序。
     *
     * @return 是否追加 supplyHours 升序
     */
    default boolean prependSupplyHoursPriority() {
        return true;
    }

    /**
     * 构建排序规则证据。
     *
     * @param context 排程上下文
     * @param task    当前任务
     * @return 排序规则证据
     */
    default Map<String, Object> buildSortEvidence(C context, T task) {
        return Collections.emptyMap();
    }
}
