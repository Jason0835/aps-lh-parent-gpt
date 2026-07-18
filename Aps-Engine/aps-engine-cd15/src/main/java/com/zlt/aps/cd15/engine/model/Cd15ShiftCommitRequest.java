package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 单规格班次资源提交请求。 */
@Data
@Builder
public class Cd15ShiftCommitRequest {
    /** 施工材料稳定键。 */
    private String materialKey;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 帘线规格。 */
    private String cordSpec;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 当前层位斜裁宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 是否加强层。 */
    private boolean reinforcement;
    /** 裁断模式。 */
    private String cutMode;
    /** 分裁任务组稳定键。 */
    private String splitGroupKey;
    /** 当前斜裁班次字段。 */
    private String classField;
    /** 当前班次开始时间。 */
    private LocalDateTime shiftStart;
    /** 当前班次结束时间。 */
    private LocalDateTime shiftEnd;
    /** 是否收尾规格。 */
    private boolean closeOut;
    /** 非收尾部分排最小车数。 */
    private int partialMinVehicleCount;
    /** 候选机台试算方案。 */
    private Cd15MachineTrialPlan trialPlan;
}
