package com.zlt.aps.tc.engine.domain;

import com.zlt.aps.common.engine.schedule.ScheduleProcessTraceEvent;
import lombok.Data;

/**
 * 胎侧任务关联过程日志。
 *
 * <p>仅用于暂存需要在机台分配结束后按最终任务链输出的过程日志，不参与任何排程计算。</p>
 */
@Data
public class TcTaskProcessLogEntry {

    /** 机台过滤与选择过程日志类别。 */
    public static final String CATEGORY_MACHINE_ASSIGN = "MACHINE_ASSIGN";

    /** 选机后的工装预校验过程日志类别。 */
    public static final String CATEGORY_TOOL_PRECHECK = "TOOL_PRECHECK";

    /** 选机后的产能扣减过程日志类别。 */
    public static final String CATEGORY_CAPACITY_DEDUCTION = "CAPACITY_DEDUCTION";

    /** 实际任务承接后的工装账本结算过程日志类别。 */
    public static final String CATEGORY_TOOL_LEDGER_SETTLEMENT = "TOOL_LEDGER_SETTLEMENT";

    /** 日志归属班次。 */
    private Integer shiftOrder;

    /** 日志类别，用于按代码执行阶段分区渲染。 */
    private String logCategory;

    /** 日志归属任务业务键。 */
    private String taskBusinessKey;

    /** 日志原始发生顺序。 */
    private Long occurrenceOrder;

    /** 摘要日志格式。 */
    private String format;

    /** 摘要日志参数。 */
    private Object[] args;

    /** FULL 级过程事件。 */
    private ScheduleProcessTraceEvent fullEvent;

    /** 是否已经写入通用过程日志缓冲。 */
    private boolean rendered;
}
