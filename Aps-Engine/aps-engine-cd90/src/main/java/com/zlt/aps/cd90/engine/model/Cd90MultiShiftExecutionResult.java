package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 多班滚动排程的内存执行结果。 */
@Data
@Builder
public class Cd90MultiShiftExecutionResult {

    /** 按输出窗口顺序保存的班次结果。 */
    private List<Cd90ShiftExecutionResult> shiftResults;
    /** 执行完成后的滚动上下文。 */
    private Cd90RollingScheduleContext rollingContext;
    /** 按多班实际处理顺序汇总的候选执行轨迹。 */
    private List<Cd90ScheduleAttemptTrace> attemptTraces;
    /** 窗口结束后的未排结果内存模型。 */
    private List<Cd90UnscheduledResultModel> unscheduledResults;
}
