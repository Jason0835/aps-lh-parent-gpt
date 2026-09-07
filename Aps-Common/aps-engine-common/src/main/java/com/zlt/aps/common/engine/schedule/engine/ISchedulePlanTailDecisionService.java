package com.zlt.aps.common.engine.schedule.engine;

import java.util.List;

/**
 * TM/TC 自动排程计划组收尾判定公共服务接口。
 *
 * @param <T> 排程任务类型
 */
public interface ISchedulePlanTailDecisionService<T extends ScheduleTaskDraftModel> {

    /**
     * 将收尾判定结果写入汇总生产任务。
     *
     * @param aggregateTask  汇总生产任务
     * @param sourceTaskList 原始来源任务
     */
    void applyTailDecision(T aggregateTask, List<T> sourceTaskList);
}
