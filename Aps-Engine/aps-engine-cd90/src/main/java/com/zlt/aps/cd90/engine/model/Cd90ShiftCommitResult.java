package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

/** 单规格班次资源提交结果。 */
@Data
@Builder
public class Cd90ShiftCommitResult {
    /** 是否提交成功。 */
    private boolean success;
    /** 失败原因编码。 */
    private String failureReason;
    /** 成功后的新资源状态；失败时为原状态。 */
    private Cd90ShiftResourceState state;
    /** 本次追加的任务。 */
    private Cd90ShiftScheduleTask task;
}
