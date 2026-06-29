package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 大卷成熟流水分配结果。
 */
@Data
@Builder
public class Cd90BigRollAgingAllocation {

    /** 是否满足本次任务所需大卷米数。 */
    private boolean success;
    /** 不满足时的内部失败原因。 */
    private String failureReason;
    /** 本次任务需要占用的米数。 */
    private BigDecimal requestedQuantity;
    /** 本次任务实际锁定的米数。 */
    private BigDecimal allocatedQuantity;
    /** 机台原预计可上机时间。 */
    private LocalDateTime originalStartTime;
    /** 考虑大卷成熟后的任务开裁时间。 */
    private LocalDateTime taskStartTime;
    /** 本次选中大卷中的最晚成熟时间。 */
    private LocalDateTime latestReleaseTime;
    /** 因静置期造成的整体延后秒数。 */
    private int delaySeconds;
    /** 被选中的库存流水及本次占用量。 */
    private List<Cd90BigRollAgingAllocationItem> items;
}
