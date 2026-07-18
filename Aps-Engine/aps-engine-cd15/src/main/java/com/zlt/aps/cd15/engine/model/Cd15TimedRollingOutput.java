package com.zlt.aps.cd15.engine.model;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15UnscheduleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 定时滚动排程的内存差异输出。 */
@Data
@Builder
public class Cd15TimedRollingOutput {

    private String batchNo;
    private String inputVersion;
    private List<Cd15ScheduleResult> insertedResults;
    private List<Cd15ScheduleResult> updatedResults;
    private List<Cd15ScheduleResult> logicallyDeletedResults;
    private List<Cd15ScheduleLaneAllocation> replacementLaneAllocations;
    private List<Cd15UnscheduleResult> unscheduledResults;
    private List<Cd15RollingAdjustmentDraft> adjustments;
}
