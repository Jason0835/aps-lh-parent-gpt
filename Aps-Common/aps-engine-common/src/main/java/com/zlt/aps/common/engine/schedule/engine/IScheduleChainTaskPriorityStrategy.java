package com.zlt.aps.common.engine.schedule.engine;

import java.util.List;
import java.util.Map;

/**
 * TM/TC 自动排程班次内任务优先公共策略接口。
 *
 * @param <C> 排程上下文类型
 * @param <T> 排程任务类型
 */
public interface IScheduleChainTaskPriorityStrategy<C extends ScheduleContextModel<T, ?, ?, ?, ?>,
        T extends ScheduleTaskDraftModel> {

    /**
     * 获取策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 选择下一条应排任务。
     *
     * @param remainingTaskList 当前班次剩余任务
     * @param context          排程上下文
     * @param chainScoreMap    各任务最佳连续性分
     * @return 下一条任务
     * @throws IllegalArgumentException 剩余任务为空时抛出
     */
    T select(List<T> remainingTaskList, C context,
             Map<String, ScheduleChainSortScoreModel> chainScoreMap);
}
