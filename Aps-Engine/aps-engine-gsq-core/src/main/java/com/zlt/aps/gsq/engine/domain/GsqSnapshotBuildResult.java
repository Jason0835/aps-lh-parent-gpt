package com.zlt.aps.gsq.engine.domain;

import lombok.Data;

/**
 * 钢丝圈解释快照构建结果。
 *
 * <p>Phase 4 重构新增：对齐胎圈 {@code TqSnapshotBuildResult}，用于承载单个钢丝圈规格的规则命中、
 * 候选机台、未排证据和系统分析文本。该对象由 {@code GsqSnapshotBuildService} 在 S6 阶段构建，
 * 序列化为 JSON 后写入 {@code T_GSQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。</p>
 *
 * <p>与胎圈的差异：</p>
 * <ul>
 *   <li>胎圈复用 Phase 2 的 EXPLAIN_JSON 字段落库；钢丝圈同样复用 EXPLAIN_JSON 字段</li>
 *   <li>钢丝圈业务键为 steelRingCode，而非 beadCode</li>
 *   <li>钢丝圈有换盘判断、钢丝直径过滤、产线过滤等独有机台策略</li>
 * </ul>
 *
 * @author APS
 */
@Data
public class GsqSnapshotBuildResult {

    /** 规则命中 JSON（GsqRuleTrace.toExplainJson() 序列化结果） */
    private String ruleHitJson;

    /** 候选机台 JSON（S3 阶段策略链过滤后的机台列表，含过滤状态和评分） */
    private String candidateMachineJson;

    /** 选中机台编码（最终分配的机台，多个逗号分隔） */
    private String selectedMachineCode;

    /** 机台分配说明（含定点机台/寸口/钢丝直径/产线过滤等策略命中说明） */
    private String machineSelectReason;

    /** 分配状态：SUCCESS（已分配）/ UNPLANNED（未排）/ PARTIAL（部分班次未排） */
    private String assignStatus;

    /** 未排证据 JSON（6个班次均无计划量的原因说明） */
    private String unplannedEvidenceJson;

    /** 系统分析说明（综合规则命中和分配结果的人类可读文本） */
    private String sysAnalysis;

    /** 任务最高异常级别：INFO/WARN/ERROR */
    private String issueLevel;

    /** 任务结构化异常 JSON（含具体异常编码和详情） */
    private String issueJson;
}
