package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个候选机台纯试算输入。
 */
@Data
@Builder
public class Cd15CandidateMachineTrialInput {

    /** GDYY库存使用的帘线大卷编码。 */
    private String bigRollCode;
    /** 钢带代码。 */
    private String steelStripCode;
    /** 候选机台代码。 */
    private String machineCode;
    /** 净需求量。 */
    private BigDecimal netDemandQuantity;
    /** 是否收尾规格。 */
    private boolean closeOut;
    /** 是否按单规格一出二模式试算。 */
    private boolean singleSpecSplit;
    /** 最小起排量。 */
    private BigDecimal minimumStartQuantity;
    /** 各班计划量均分阈值，按均分前完整计划量判断是否触发均分。 */
    private BigDecimal equalShareThreshold;
    /** 是否为均分后转入下一班的剩余计划量。 */
    private boolean equalShareAlreadyApplied;
    /** 单车按胎体长度方向卷取后，对应的斜裁排程米数。 */
    private BigDecimal vehiclePlanQuantity;
    /** 斜裁宽度，单位毫米。 */
    private BigDecimal craftWidth;
    /** 单耗，单位毫米/条。 */
    private BigDecimal unitConsumeMillimeter;
    /** 大卷幅宽，单位毫米。 */
    private BigDecimal cordWidth;
    /** 工装总数。 */
    private int totalToolingCount;
    /** 当前库排占用车数。 */
    private int occupiedVehicleCount;
    /** 当前裁断模式的满班产能，单位米/班。 */
    private BigDecimal shiftCapacity;
    /** 班次时长小时数。 */
    private int shiftHours;
    /** 已扣除检修和前序任务后的剩余秒数。 */
    private int remainingSeconds;
    /** 机台原预计可上机时间。 */
    private LocalDateTime originalStartTime;
    /** 当前班次可参与试算的大卷成熟流水。 */
    private List<Cd15BigRollAgingStock> bigRollAgingStocks;
    /** 机台任务链上一规格。 */
    private String previousSpec;
    /** 当前帘线规格。 */
    private String currentSpec;
    /** 规格切换耗时分钟数。 */
    private int specChangeMinutes;
    /** 机台链尾大卷与斜裁规格。 */
    private Cd15MachineTailState previousTail;
    /** 当前任务大卷与斜裁规格。 */
    private Cd15MachineTailState currentTail;
    /** 同大卷不同规格切换耗时。 */
    private int sameRollDiffSpecChangeMinutes;
    /** 不同大卷同规格切换耗时。 */
    private int diffRollSameSpecChangeMinutes;
    /** 不同大卷不同规格切换耗时。 */
    private int diffRollDiffSpecChangeMinutes;
    /** 损耗率规则。 */
    private List<Cd15LossRateRule> lossRateRules;
    /** 通用损耗率兜底（百分比），四层优先级均未命中时使用。 */
    private BigDecimal fallbackLossRatePercent;
    /** 是否为定点优先机台。 */
    private boolean preferredMachine;
    /** 是否为该规格上一次生产/本次续作的原机台。 */
    private boolean historyMachine;
    /** 配置的机台优先顺序。 */
    private int priorityOrder;
}
