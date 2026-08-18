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

    /** 日志归属班次。 */
    private Integer shiftOrder;

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
