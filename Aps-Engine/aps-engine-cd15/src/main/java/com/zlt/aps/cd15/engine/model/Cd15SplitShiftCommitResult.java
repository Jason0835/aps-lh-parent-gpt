package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/**
 * 分裁组合原子提交结果。
 */
@Data
@Builder
public class Cd15SplitShiftCommitResult {

    /** 是否两条任务同时提交成功。 */
    private boolean success;
    /** 提交后的资源状态；失败时返回原状态。 */
    private Cd15ShiftResourceState state;
    /** 第一条钢带任务。 */
    private Cd15ShiftScheduleTask firstTask;
    /** 第二条钢带任务。 */
    private Cd15ShiftScheduleTask secondTask;
    /** 第一条钢带部分排原因。 */
    private String firstPartialReason;
    /** 第二条钢带部分排原因。 */
    private String secondPartialReason;
    /** 组合提交失败原因。 */
    private String failureReason;
}
