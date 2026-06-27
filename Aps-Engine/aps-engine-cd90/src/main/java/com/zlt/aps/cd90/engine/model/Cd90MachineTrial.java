package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 候选机台纯试算结果。
 */
@Data
@Builder
public class Cd90MachineTrial {

    /** 候选机台编码。 */
    private String machineCode;
    /** 最终损耗率百分数。 */
    private BigDecimal lossRatePercent;
    /** 损耗率命中层级。 */
    private String lossRateLevel;
    /** 实际排产量。 */
    private BigDecimal actualQuantity;
    /** 工装可排量。 */
    private BigDecimal toolingQuantity;
    /** 机台产能可排量。 */
    private BigDecimal capacityQuantity;
    /** 最终可排量。 */
    private BigDecimal finalSchedulableQuantity;
    /** 是否能完整容纳实际排产量。 */
    private boolean fullyAccommodated;
    /** 是否为JOB_TYPE=0定点优先机台。 */
    private boolean preferredMachine;
    /** MACHINE_PRIORITY中的顺序，数值越小优先级越高。 */
    private int priorityOrder;
    /** 机台链尾是否与当前帘线规格相同。 */
    private boolean sameTailSpec;
    /** 本次规格切换耗时秒数。 */
    private int changeSeconds;
    /** 本次任务按试算量生产耗时秒数。 */
    private int productionSeconds;
    /** 排入后的班次剩余秒数。 */
    private int remainingSeconds;
    /** 考虑大卷成熟后的任务开裁时间。 */
    private LocalDateTime taskStartTime;
    /** 大卷成熟导致的整体等待秒数。 */
    private int agingDelaySeconds;
    /** 本次任务的大卷成熟流水试算结果。 */
    private Cd90BigRollAgingAllocation agingAllocation;
    /** 试算受限原因：TOOLING_LIMIT表示工装不足，CAPACITY_LIMIT表示机台产能不足；未受限时为空。 */
    private String limitReason;
}
