package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面解释快照构建结果。
 *
 * <p>用于承载单个任务的规则命中、候选机台、未排证据和系统分析文本，供落库服务转换为解释实体。</p>
 */
@Data
public class TmSnapshotBuildResult {

    /** 规则命中 JSON */
    private String ruleHitJson;

    /** 候选机台 JSON */
    private String candidateMachineJson;

    /** 选中机台评分 */
    private BigDecimal selectedMachineScore;

    /** 最终选机说明 */
    private String machineSelectReason;

    /** 分配状态 */
    private String assignStatus;

    /** 未排证据 JSON */
    private String unplannedEvidenceJson;

    /** 系统分析说明 */
    private String sysAnalysis;
}
