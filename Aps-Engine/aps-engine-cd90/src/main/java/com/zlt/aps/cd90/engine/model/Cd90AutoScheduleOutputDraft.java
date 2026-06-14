package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 最终事务可消费的自动排程输出草稿包。 */
@Data
@Builder
public class Cd90AutoScheduleOutputDraft {

    private List<Cd90ScheduleResultDraft> scheduleResults;
    private List<Cd90LaneAllocationDraft> laneAllocations;
    private List<Cd90ScheduleExplainLogDraft> explainLogs;
    private List<Cd90UnscheduledResultModel> unscheduledResults;
}
