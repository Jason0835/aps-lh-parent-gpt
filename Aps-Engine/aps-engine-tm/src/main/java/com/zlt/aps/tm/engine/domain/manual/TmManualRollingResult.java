package com.zlt.aps.tm.engine.domain.manual;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 胎面人工滚动计算结果。
 */
@Data
public class TmManualRollingResult {

    /** 最终已排任务 */
    private List<TmManualTaskDraft> scheduledTaskList = new ArrayList<>();

    /** 第6班后仍未承接的任务 */
    private List<TmManualTaskDraft> unplannedTaskList = new ArrayList<>();

    /** 最终机台班次任务链 */
    private MachineShiftTaskChain<TmManualTaskDraft> taskChainGroup = new MachineShiftTaskChain<>();

    /** 受影响结果分组 */
    private Set<String> affectedResultGroupKeySet = new LinkedHashSet<>();

    /** 受影响既有结果ID */
    private Set<Long> affectedResultIdSet = new LinkedHashSet<>();

    /** 机台链表变化摘要 */
    private List<String> chainChangeSummaryList = new ArrayList<>();

    /** 计算前任务总量 */
    private BigDecimal beforeTotalQty = BigDecimal.ZERO;

    /** 命令引起的净变化量 */
    private BigDecimal commandDeltaQty = BigDecimal.ZERO;

    /** 计算后已排总量 */
    private BigDecimal scheduledTotalQty = BigDecimal.ZERO;

    /** 计算后新增未排总量 */
    private BigDecimal unplannedTotalQty = BigDecimal.ZERO;
}
