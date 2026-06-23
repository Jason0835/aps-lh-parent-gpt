package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单规格班次资源提交请求。 */
@Data
@Builder
public class Cd90ShiftCommitRequest {
    /** 帘布代码。 */
    private String clothCode;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 帘线规格。 */
    private String cordSpec;
    /** 当前直裁班次字段。 */
    private String classField;
    /** 当前班次开始时间。 */
    private LocalDateTime shiftStart;
    /** 当前班次结束时间。 */
    private LocalDateTime shiftEnd;
    /** 单车卷曲米数。 */
    private BigDecimal coilMeter;
    /** 是否收尾规格。 */
    private boolean closeOut;
    /** 非收尾部分排最小车数。 */
    private int partialMinVehicleCount;
    /** 候选机台试算方案。 */
    private Cd90MachineTrialPlan trialPlan;
}
