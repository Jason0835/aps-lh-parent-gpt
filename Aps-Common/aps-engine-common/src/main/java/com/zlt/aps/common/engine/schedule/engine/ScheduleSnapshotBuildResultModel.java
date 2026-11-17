package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
/** TM/TC 单任务快照公共运行态结果。 */
@Data
public class ScheduleSnapshotBuildResultModel {
    /**
     * 规则命中与排序决策证据 JSON。
     */
    protected String ruleHitJson;
    /** 候选机台过滤与评分明细 JSON。 */
    protected String candidateMachineJson;
    /** 最终选中机台的综合评分。 */
    protected BigDecimal selectedMachineScore;
    /** 最终选机原因说明。 */
    protected String machineSelectReason;
    /** 任务分配状态（如已分配、未排等）。 */
    protected String assignStatus;
    /** 未排原因及诊断证据 JSON。 */
    protected String unplannedEvidenceJson;
    /** 系统自动生成的排程分析摘要。 */
    protected String sysAnalysis;
}
