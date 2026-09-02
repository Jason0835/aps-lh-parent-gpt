package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
/** TM/TC 单任务快照公共运行态结果。 */
@Data
public class ScheduleSnapshotBuildResultModel {
    protected String ruleHitJson;
    protected String candidateMachineJson;
    protected BigDecimal selectedMachineScore;
    protected String machineSelectReason;
    protected String assignStatus;
    protected String unplannedEvidenceJson;
    protected String sysAnalysis;
}
