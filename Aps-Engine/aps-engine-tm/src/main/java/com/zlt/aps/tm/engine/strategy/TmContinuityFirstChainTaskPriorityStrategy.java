package com.zlt.aps.tm.engine.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.api.enums.TmScheduleStrategyEnum;
import com.zlt.aps.tm.engine.domain.TmChainSortScore;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 供应时长优先、连续性决胜策略，保留原参数编码并由 TmStrategyRegistry 收集注册。 */
@Component
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
        List<TmTaskDraft> presetTaskList = remainingTaskList.stream()
                .filter(task -> !task.isUnassigned()).collect(Collectors.toList());
        List<TmTaskDraft> candidateTaskList = CollUtil.isEmpty(presetTaskList)
                ? new ArrayList<>(remainingTaskList) : presetTaskList;
        candidateTaskList.sort(Comparator
                .comparing((TmTaskDraft task) -> task.getSupplyHours() == null)
                .thenComparing(task -> task.getSupplyHours() == null ? BigDecimal.ZERO : task.getSupplyHours())
                .thenComparing(TmTaskDraft::getLatestStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((TmTaskDraft task) -> chainScoreMap.getOrDefault(
                                task.getBusinessKey(), TmChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(TmTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return candidateTaskList.get(0);
    }
}
