package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleProcessTraceEvent;
import lombok.Data;

/** 自动排程任务关联过程日志公共运行态模型。 */
@Data
public class ScheduleTaskProcessLogEntry {

    public static final String CATEGORY_MACHINE_ASSIGN = "MACHINE_ASSIGN";
    public static final String CATEGORY_TOOL_PRECHECK = "TOOL_PRECHECK";
    public static final String CATEGORY_CAPACITY_DEDUCTION = "CAPACITY_DEDUCTION";
    public static final String CATEGORY_TOOL_LEDGER_SETTLEMENT = "TOOL_LEDGER_SETTLEMENT";

    protected Integer shiftOrder;
    protected String logCategory;
    protected String taskBusinessKey;
    protected Long occurrenceOrder;
    protected String format;
    protected Object[] args;
    protected ScheduleProcessTraceEvent fullEvent;
    protected boolean rendered;
}
