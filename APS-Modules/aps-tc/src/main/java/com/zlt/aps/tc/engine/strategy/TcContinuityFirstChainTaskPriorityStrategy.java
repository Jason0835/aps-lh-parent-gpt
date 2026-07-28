package com.zlt.aps.tc.engine.strategy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.enums.TcScheduleStrategyEnum;
import com.zlt.aps.tc.engine.domain.TcChainSortScore;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.TcTaskDraft;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 连续性优先策略，保持二期实施前的生产排序口径，由 TcStrategyRegistry 收集注册。 */
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
        for (TcTaskDraft task : remainingTaskList) {
            if (!task.isUnassigned()) {
                return task;
            }
        }
        List<TcTaskDraft> orderedTaskList = new ArrayList<>(remainingTaskList);
        orderedTaskList.sort(Comparator
                .comparing((TcTaskDraft task) -> chainScoreMap.getOrDefault(task.getBusinessKey(), TcChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(TcTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return orderedTaskList.get(0);
    }
}
