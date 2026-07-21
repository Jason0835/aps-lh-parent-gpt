package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 最终事务可消费的自动排程输出草稿包。 */
@Data
@Builder
public class Cd15AutoScheduleOutputDraft {

    private List<Cd15ScheduleResultDraft> scheduleResults;
    private List<Cd15LaneAllocationDraft> laneAllocations;
    private List<Cd15ScheduleExplainLogDraft> explainLogs;
    private List<Cd15UnscheduledResultModel> unscheduledResults;
    /** 多班执行时实际使用的逐班净需求轨迹，用于需求快照。 */
    private List<Cd15ScheduleAttemptTrace> demandTraces;
}
