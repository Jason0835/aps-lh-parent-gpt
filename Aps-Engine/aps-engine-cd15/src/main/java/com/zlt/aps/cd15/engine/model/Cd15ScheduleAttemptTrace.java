package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 单次候选规格处理的有序执行轨迹。 */
@Data
@Builder
public class Cd15ScheduleAttemptTrace {

    /** 排程结果班次字段。 */
    private String classField;
    /** 业务班次编码。 */
    private String shiftCode;
    /** 班次展示名称，用于分析文案显示，如夜班07/20。 */
    private String shiftDisplayName;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 当前班重新计算的净需求量。 */
    private BigDecimal netDemandQuantity;
    /** 当前尝试实际提交的排产量。 */
    private BigDecimal scheduledQuantity;
    /** 内部失败原因；成功时为空。 */
    private String failureReason;
    /** 成功但仅部分排产时的实际限制原因编码。 */
    private String partialReason;
    /** 多班循环内的实际发生顺序。 */
    private int sequence;
}
