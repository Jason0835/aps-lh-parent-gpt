package com.zlt.aps.tm.engine.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.enums.TmScheduleStrategyEnum;
import com.zlt.aps.tm.engine.domain.TmChainSortScore;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 连续性优先策略，保持二期实施前的生产排序口径。 */
public class TmContinuityFirstChainTaskPriorityStrategy implements ITmChainTaskPriorityStrategy {

    @Override
    public String getStrategyCode() {
        return TmScheduleStrategyEnum.CONTINUITY_FIRST.getCode();
    }

    @Override
    public TmTaskDraft select(List<TmTaskDraft> remainingTaskList, TmScheduleContext context,
                              Map<String, TmChainSortScore> chainScoreMap) {
        if (CollUtil.isEmpty(remainingTaskList)) {
            throw new IllegalArgumentException("班次内剩余任务不能为空");
        }
        for (TmTaskDraft task : remainingTaskList) {
            if (!task.isUnassigned()) {
                return task;
            }
        }
        List<TmTaskDraft> orderedTaskList = new ArrayList<>(remainingTaskList);
        orderedTaskList.sort(Comparator
                .comparing((TmTaskDraft task) -> chainScoreMap.getOrDefault(task.getBusinessKey(), TmChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return orderedTaskList.get(0);
    }
}
