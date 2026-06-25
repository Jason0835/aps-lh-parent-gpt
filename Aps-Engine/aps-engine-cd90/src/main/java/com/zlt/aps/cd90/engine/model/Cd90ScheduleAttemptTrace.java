package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 单次候选规格处理的有序执行轨迹。 */
@Data
@Builder
public class Cd90ScheduleAttemptTrace {

    /** 排程结果班次字段。 */
    private String classField;
    /** 业务班次编码。 */
    private String shiftCode;
    /** 帘布代码。 */
    private String clothCode;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 当前班重新计算的净需求量。 */
    private BigDecimal netDemandQuantity;
    /** 当前尝试实际提交的排产量。 */
    private BigDecimal scheduledQuantity;
    /** 内部失败原因；成功时为空。 */
    private String failureReason;
    /** 多班循环内的实际发生顺序。 */
    private int sequence;
}
