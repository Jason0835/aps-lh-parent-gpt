package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 自动排程质量摘要模型访问端口。
 *
 * @param <C> 上下文类型
 * @param <T> 任务类型
 * @param <G> 计划组类型
 * @param <M> 候选机台类型
 */
public interface ScheduleQualityModelAccess<C, T extends ScheduleQualityTask,
        G extends ScheduleQualityPlanGroup<T>, M extends ScheduleQualityMachineCandidate> {

    Map<String, G> getPlanTaskGroupMap(C context);

    int getTaskDraftCount(C context);

    List<T> getScheduledTasks(C context);

    List<M> getMachineCandidates(C context);

    Map<String, BigDecimal> getProductShiftShortageMap(C context);
}

