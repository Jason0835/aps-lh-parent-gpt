package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 单个直裁班次的内存执行结果。 */
@Data
@Builder
public class Cd90ShiftExecutionResult {

    /** 当前班次描述。 */
    private Cd90ShiftDescriptor shift;
    /** 当前班最终资源状态。 */
    private Cd90ShiftResourceState state;
    /** 当前班成功提交的任务。 */
    private List<Cd90ShiftScheduleTask> tasks;
    /** 按帘布代码保存的规格级失败原因。 */
    private Map<String, String> failures;
    /** 当前班按实际处理顺序保存的候选执行轨迹。 */
    private List<Cd90ScheduleAttemptTrace> attemptTraces;
}
