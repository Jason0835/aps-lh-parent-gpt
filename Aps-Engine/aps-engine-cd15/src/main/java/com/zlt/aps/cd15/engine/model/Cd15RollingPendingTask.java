package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 跨班滚动时保留任务身份的待排节点。
 */
@Data
@Builder
public class Cd15RollingPendingTask {

    /** 任务稳定唯一键。 */
    private String taskKey;
    /** 来源排程结果ID。 */
    private Long sourceResultId;
    /** 来源批次号。 */
    private String sourceBatchNo;
    /** 来源工单号。 */
    private String sourceOrderNo;
    /** 原班次字段。 */
    private String originalClassField;
    /** 原生产顺序。 */
    private Integer originalProduceOrder;
    /** 当前目标班次字段。 */
    private String targetClassField;
    /** 施工材料稳定键。 */
    private String materialKey;
    /** 钢带代号。 */
    private String steelStripCode;
    /** 大卷代码。 */
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
    /** 分裁组合稳定键。 */
    private String splitGroupKey;
    /** 来源机台。 */
    private String sourceMachineCode;
    /** 插单指定的硬约束机台。 */
    private String requiredMachineCode;
    /** 原计划量。 */
    private BigDecimal originalQuantity;
    /** 已排数量。 */
    private BigDecimal scheduledQuantity;
    /** 尚待排数量。 */
    private BigDecimal remainingQuantity;
    /** 是否人工插单硬约束。 */
    private boolean hardInsert;
    /** 是否锁定不可移动。 */
    private boolean locked;
    /** 是否从上一班连续生产。 */
    private boolean continueFromPreviousShift;
    /** 最近一次真实限制原因。 */
    private String lastLimitReason;
    /** 是否为当前班紧急缺口。 */
    private boolean urgentCurrentShiftShortage;
    /** 原任务稳定顺序，新任务使用最大值。 */
    private int stableOrder;
    /** 滚动调整或未排原因。 */
    private String reasonCode;
}
