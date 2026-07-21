package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 多班滚动排程的内存执行结果。 */
@Data
@Builder
public class Cd15MultiShiftExecutionResult {

    /** 按输出窗口顺序保存的班次结果。 */
    private List<Cd15ShiftExecutionResult> shiftResults;
    /** 执行完成后的滚动上下文。 */
    private Cd15RollingScheduleContext rollingContext;
    /** 按多班实际处理顺序汇总的候选执行轨迹。 */
    private List<Cd15ScheduleAttemptTrace> attemptTraces;
    /** 窗口结束后的未排结果内存模型。 */
    private List<Cd15UnscheduledResultModel> unscheduledResults;
    /** 首班输入锁定的钢带成型来源追溯信息。 */
    private Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip;
}
