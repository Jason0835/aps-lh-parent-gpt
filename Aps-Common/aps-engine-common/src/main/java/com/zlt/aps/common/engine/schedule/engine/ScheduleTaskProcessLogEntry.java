package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.ScheduleProcessTraceEvent;
import lombok.Data;

/** 自动排程任务关联过程日志公共运行态模型。 */
@Data
public class ScheduleTaskProcessLogEntry {

    /**
     * 日志类别：选机分配阶段。
     */
    public static final String CATEGORY_MACHINE_ASSIGN = "MACHINE_ASSIGN";
    /** 日志类别：工装预检查阶段。 */
    public static final String CATEGORY_TOOL_PRECHECK = "TOOL_PRECHECK";
    /** 日志类别：产能扣减阶段。 */
    public static final String CATEGORY_CAPACITY_DEDUCTION = "CAPACITY_DEDUCTION";
    /** 日志类别：工装台账结算阶段。 */
    public static final String CATEGORY_TOOL_LEDGER_SETTLEMENT = "TOOL_LEDGER_SETTLEMENT";

    /** 所属排程班次序号。 */
    protected Integer shiftOrder;
    /** 日志所属阶段类别。 */
    protected String logCategory;
    /** 关联任务的业务唯一键。 */
    protected String taskBusinessKey;
    /** 日志条目发生顺序号。 */
    protected Long occurrenceOrder;
    /** 摘要日志格式化模板。 */
    protected String format;
    /** 摘要日志格式化参数数组。 */
    protected Object[] args;
    /** 完整过程追踪事件对象（FULL 日志模式使用）。 */
    protected ScheduleProcessTraceEvent fullEvent;
    /** 是否已渲染写入过程日志缓冲器。 */
    protected boolean rendered;
}
