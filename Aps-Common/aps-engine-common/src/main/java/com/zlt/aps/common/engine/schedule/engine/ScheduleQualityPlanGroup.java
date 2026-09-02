package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * 自动排程质量计算需要的计划组契约。
 *
 * @param <T> 任务类型
 */
public interface ScheduleQualityPlanGroup<T extends ScheduleQualityTask> {

    String getPlanGroupKey();

    T getAggregateTask();

    BigDecimal getGroupFinalPlanQty();
}

