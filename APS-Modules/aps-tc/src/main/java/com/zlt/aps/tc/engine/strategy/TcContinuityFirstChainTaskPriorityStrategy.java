package com.zlt.aps.tc.engine.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.TcChainSortScore;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 供应时长优先、连续性决胜策略，保留原参数编码并由 TcStrategyRegistry 收集注册。 */
@Component
public class TcContinuityFirstChainTaskPriorityStrategy implements ITcChainTaskPriorityStrategy {

    @Override
    public String getStrategyCode() {
        return TcScheduleStrategyEnum.CONTINUITY_FIRST.getCode();
    }

    @Override
    public TcTaskDraft select(List<TcTaskDraft> remainingTaskList, TcScheduleContext context,
                              Map<String, TcChainSortScore> chainScoreMap) {
        if (CollUtil.isEmpty(remainingTaskList)) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.remainingTasksEmpty"));
        }
        List<TcTaskDraft> presetTaskList = remainingTaskList.stream()
                .filter(task -> !task.isUnassigned()).collect(Collectors.toList());
        List<TcTaskDraft> candidateTaskList = CollUtil.isEmpty(presetTaskList)
                ? new ArrayList<>(remainingTaskList) : presetTaskList;
        candidateTaskList.sort(Comparator
                .comparing((TcTaskDraft task) -> task.getSupplyHours() == null)
                .thenComparing(task -> task.getSupplyHours() == null ? BigDecimal.ZERO : task.getSupplyHours())
                .thenComparing(TcTaskDraft::getLatestStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((TcTaskDraft task) -> chainScoreMap.getOrDefault(
                                task.getBusinessKey(), TcChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(TcTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return candidateTaskList.get(0);
    }
}
