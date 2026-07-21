package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 自动排程结果解释日志草稿。 */
@Data
@Builder
public class Cd15ScheduleExplainLogDraft {

    private String resultKey;
    private String logType;
    private List<Cd15ScheduleShiftSlotDraft> shiftDetails;
}
