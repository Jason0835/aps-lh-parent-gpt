package com.zlt.aps.tc.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 胎侧人工滚动纯计算结果。
 */
@Data
public class TcManualRollingResult {
    /** 最终已排任务。 */
    private List<TcManualTaskDraft> scheduledTaskList = new ArrayList<>();
    /** 第六班后未排任务。 */
    private List<TcManualTaskDraft> unplannedTaskList = new ArrayList<>();
    /** 最终机台班次任务链。 */
    private MachineShiftTaskChain<TcManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();
    /** 受影响结果分组。 */
    private Set<String> affectedResultGroupKeySet = new LinkedHashSet<>();
    /** 受影响既有结果 ID。 */
    private Set<Long> affectedResultIdSet = new LinkedHashSet<>();
    /** 链表变化摘要。 */
    private List<String> chainChangeSummaryList = new ArrayList<>();
    /** 计算前总量。 */
    private BigDecimal beforeTotalQty = BigDecimal.ZERO;
    /** 命令净变化量。 */
    private BigDecimal commandDeltaQty = BigDecimal.ZERO;
    /** 计算后已排总量。 */
    private BigDecimal scheduledTotalQty = BigDecimal.ZERO;
    /** 计算后未排总量。 */
    private BigDecimal unplannedTotalQty = BigDecimal.ZERO;
}
