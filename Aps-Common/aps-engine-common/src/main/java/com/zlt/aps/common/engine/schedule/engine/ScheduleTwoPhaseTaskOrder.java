package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 两阶段基础排序适配器，不改变全局排序策略和自动滚动入口。
 */
public final class ScheduleTwoPhaseTaskOrder {

    /**
     * 冻结原规则次序，基础轮以供应时长为最高优先级并同步两个消费索引。
     * 空供应时长排在已知任务之后，不伪造为零；同紧迫度沿原序稳定排序。
     *
     * @param tasks 当前班次在原策略下排好的任务
     */
    public void sortBaseTasks(List<? extends ScheduleTaskDraftModel> tasks) {
        tasks.stream().filter(Objects::nonNull).forEach(task -> {
            if (task.getExtraPrioritySnapshot() == null) {
                task.setExtraPrioritySnapshot(task.getBaseSortIndex() == null
                        ? task.getPlanCalcOrderIndex() : task.getBaseSortIndex());
            }
        });
        tasks.sort(Comparator.comparing(ScheduleTaskDraftModel::getSupplyHours,
                        Comparator.nullsLast(BigDecimal::compareTo))
                .thenComparing(ScheduleTaskDraftModel::getExtraPrioritySnapshot,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScheduleTaskDraftModel::getBusinessKey,
                        Comparator.nullsLast(String::compareTo)));
        int startIndex = tasks.stream().map(ScheduleTaskDraftModel::getBaseSortIndex)
                .filter(Objects::nonNull).min(Integer::compareTo).orElse(1);
        for (int index = 0; index < tasks.size(); index++) {
            tasks.get(index).setPlanCalcOrderIndex(startIndex + index);
            tasks.get(index).setBaseSortIndex(startIndex + index);
        }
    }

    /**
     * 恢复扩增轮原规则次序，不反写基础轮消费索引。
     *
     * @param tasks 备库原地扩增候选
     */
    public void sortExtraTasks(List<? extends ScheduleTaskDraftModel> tasks) {
        tasks.sort(Comparator.comparing(ScheduleTaskDraftModel::getExtraPrioritySnapshot,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ScheduleTaskDraftModel::getBusinessKey,
                        Comparator.nullsLast(String::compareTo)));
    }
}
