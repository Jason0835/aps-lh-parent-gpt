package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 单个斜裁班次的内存执行结果。 */
@Data
@Builder
public class Cd15ShiftExecutionResult {

    /** 当前班次描述。 */
    private Cd15ShiftDescriptor shift;
    /** 当前班最终资源状态。 */
    private Cd15ShiftResourceState state;
    /** 当前班成功提交的任务。 */
    private List<Cd15ShiftScheduleTask> tasks;
    /** 按钢带代码保存的规格级失败原因。 */
    private Map<String, String> failures;
    /** 当前班按实际处理顺序保存的候选执行轨迹。 */
    private List<Cd15ScheduleAttemptTrace> attemptTraces;
}
