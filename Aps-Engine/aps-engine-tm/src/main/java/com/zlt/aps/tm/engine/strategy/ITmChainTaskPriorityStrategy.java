package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.tm.engine.domain.TmChainSortScore;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.List;
import java.util.Map;

/** 胎面班次内动态任务优先策略。 */
public interface ITmChainTaskPriorityStrategy {
    /** @return 唯一策略编码 */
    String getStrategyCode();

    /**
     * 选择下一条应排任务。
     * @param remainingTaskList 当前班次剩余任务
     * @param context 自动排程上下文
     * @param chainScoreMap 各任务最佳连续性分
     * @return 下一条任务
     * @throws IllegalArgumentException 剩余任务为空时抛出
     */
    TmTaskDraft select(List<TmTaskDraft> remainingTaskList, TmScheduleContext context,
                       Map<String, TmChainSortScore> chainScoreMap);
}
