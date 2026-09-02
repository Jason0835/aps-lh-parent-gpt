package com.zlt.aps.common.engine.schedule.engine;

/** 自动排程质量计算需要的落库数量摘要契约。 */
public interface ScheduleQualityPersistSummary {

    int getResultCount();

    int getUnplannedCount();
}

