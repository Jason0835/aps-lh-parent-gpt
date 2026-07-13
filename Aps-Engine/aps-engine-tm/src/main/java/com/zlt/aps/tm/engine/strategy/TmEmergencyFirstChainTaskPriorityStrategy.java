package com.zlt.aps.tm.engine.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.zlt.aps.tm.engine.domain.TmChainSortScore;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 库存紧急优先策略，预置机台仍作为硬约束优先处理。 */
public class TmEmergencyFirstChainTaskPriorityStrategy implements ITmChainTaskPriorityStrategy {
    /** 紧急优先策略编码 */
    public static final String STRATEGY_CODE = "EMERGENCY_FIRST";

    @Override
    public String getStrategyCode() {
        return STRATEGY_CODE;
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
                .thenComparing((TmTaskDraft task) -> chainScoreMap.getOrDefault(task.getBusinessKey(), TmChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return candidateTaskList.get(0);
    }
}
