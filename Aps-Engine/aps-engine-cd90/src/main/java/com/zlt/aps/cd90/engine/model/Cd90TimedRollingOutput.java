package com.zlt.aps.cd90.engine.model;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleLaneAllocation;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 定时滚动排程的内存差异输出。 */
@Data
@Builder
public class Cd90TimedRollingOutput {

    private String batchNo;
    private String inputVersion;
    private List<Cd90ScheduleResult> insertedResults;
    private List<Cd90ScheduleResult> updatedResults;
    private List<Cd90ScheduleResult> logicallyDeletedResults;
    private List<Cd90ScheduleLaneAllocation> replacementLaneAllocations;
    private List<Cd90UnscheduleResult> unscheduledResults;
    private List<Cd90RollingAdjustmentDraft> adjustments;
}
