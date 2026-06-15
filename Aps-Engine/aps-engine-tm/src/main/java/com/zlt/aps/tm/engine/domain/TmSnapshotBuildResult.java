package com.zlt.aps.tm.engine.domain;

import lombok.Data;

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

    /** 未排证据 JSON */
    private String unplannedEvidenceJson;

    /** 系统分析说明 */
    private String sysAnalysis;
}
