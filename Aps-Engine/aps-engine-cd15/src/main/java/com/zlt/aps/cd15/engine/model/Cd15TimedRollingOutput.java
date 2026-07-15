package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** CD15目标班次后缀滚动试排输出。 */
@Data
@Builder
public class Cd15TimedRollingOutput {

    /** 试排输入版本，持久化前需要二次校验。 */
    private String inputVersion;
    /** 需要替换落库的目标班次及后续班次结果草稿。 */
    private List<Cd15ScheduleResultDraft> replacementResults;
    /** 需要替换落库的库排分配草稿。 */
    private List<Cd15LaneAllocationDraft> replacementLaneAllocations;
    /** 目标班次及后续班次仍未排结果。 */
    private List<Cd15SingleShiftScheduleResult> unscheduledResults;
}