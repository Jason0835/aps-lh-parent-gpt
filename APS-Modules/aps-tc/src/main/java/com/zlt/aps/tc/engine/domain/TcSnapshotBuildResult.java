package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎侧解释快照构建结果。
 *
 * <p>用于承载单个任务的规则命中、候选机台、未排证据和系统分析文本，供落库服务转换为解释实体。</p>
 */
@Data
public class TcSnapshotBuildResult {

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

    /** 任务最高异常级别 */
    private String issueLevel;

    /** 任务结构化异常 JSON */
    private String issueJson;
}
