package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 单规格班次资源提交结果。 */
@Data
@Builder
public class Cd15ShiftCommitResult {
    /** 是否提交成功。 */
    private boolean success;
    /** 失败原因编码。 */
    private String failureReason;
    /** 成功但仅部分排产时的实际限制原因编码。 */
    private String partialReason;
    /** 单规格分裁均分后必须转入下一班的精确余量。 */
    private BigDecimal equalShareRemainderQuantity;
    /** 成功后的新资源状态；失败时为原状态。 */
    private Cd15ShiftResourceState state;
    /** 本次追加的任务。 */
    private Cd15ShiftScheduleTask task;
}
