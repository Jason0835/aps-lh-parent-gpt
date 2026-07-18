package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 当前班次内存任务链节点。 */
@Data
@Builder
public class Cd15ShiftScheduleTask {
    /** 斜裁班次字段。 */
    private String classField;
    /** 滚动来源任务稳定键。 */
    private String sourceTaskKey;
    /** 滚动来源排程结果ID。 */
    private Long sourceResultId;
    /** 施工材料稳定键。 */
    private String materialKey;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 钢压大卷代码。 */
    private String bigRollCode;
    /** 裁断角度。 */
    private String cuttingAngle;
    /** 当前层位斜裁宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 是否加强层。 */
    private boolean reinforcement;
    /** 裁断模式：SINGLE或SPLIT。 */
    private String cutMode;
    /** 分裁任务组稳定键。 */
    private String splitGroupKey;
    /** 钢带规格。 */
    private String cordSpec;
    /** 机台编码。 */
    private String machineCode;
    /** 本班计划量。 */
    private BigDecimal planQuantity;
    /** 计划入库车数。 */
    private int vehicleCount;
    /** 当前机台生产顺序。 */
    private int produceOrder;
    /** 预计开始时间。 */
    private LocalDateTime expectedStartTime;
    /** 预计结束时间。 */
    private LocalDateTime expectedEndTime;
    /** 库排分配明细。 */
    private List<Cd15StorageLaneAllocation> laneAllocations;
}
