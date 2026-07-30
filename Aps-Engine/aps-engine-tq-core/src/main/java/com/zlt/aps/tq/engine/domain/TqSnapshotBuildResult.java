package com.zlt.aps.tq.engine.domain;

import lombok.Data;

/**
 * 胎圈解释快照构建结果。
 *
 * <p>Phase 4 重构新增：对齐胎侧 {@code TcSnapshotBuildResult}，用于承载单个胎圈规格的规则命中、
 * 候选机台、未排证据和系统分析文本。该对象由 {@code TqSnapshotBuildService} 在 S6 阶段构建，
 * 序列化为 JSON 后写入 {@code T_TQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。</p>
 *
 * <p>与胎侧的差异：胎侧解释快照独立写入解释表 {@code T_TC_SCHEDULE_RESULT_EXPLAIN}；
 * 胎圈为避免引入额外表结构变更，复用 Phase 2 已添加的 {@code EXPLAIN_JSON} 字段，
 * 将快照整体序列化为 JSON 落库。</p>
 *
 * @author APS
 */
@Data
public class TqSnapshotBuildResult {

    /** 规则命中 JSON（TqRuleTrace.toExplainJson() 序列化结果） */
    private String ruleHitJson;

    /** 候选机台 JSON（S3 阶段 searchOptionalMachineList 过滤后的机台列表） */
    private String candidateMachineJson;

    /** 选中机台编码（最终分配的机台，多个逗号分隔） */
    private String selectedMachineCode;

    /** 机台分配说明（含定点机台/口型板机台命中策略） */
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
