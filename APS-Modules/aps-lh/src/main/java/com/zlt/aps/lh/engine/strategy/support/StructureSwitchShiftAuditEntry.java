package com.zlt.aps.lh.engine.strategy.support;

import lombok.Data;

import java.io.Serializable;

/**
 * 结构切换逐班竞争的标量证据快照，不持有可变候选、机台或业务账本。
 * 同一来源、班次、阶段、SKU及物理机台只保留最近状态，曾形成合格提案的事实单独保留。
 */
@Data
public class StructureSwitchShiftAuditEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本轮供胚来源主键。 */
    private Long sourceId;
    /** 后结构名称。 */
    private String structureName;
    /** 实际竞争班次开始时间戳，跨日不能只使用班别。 */
    private long shiftStartMillis;
    /** 竞争阶段。 */
    private String phase;
    /** SKU物料编码。 */
    private String materialCode;
    /** SKU产品状态。 */
    private String productStatus;
    /** 物理机台编码；候选准入快照为空。 */
    private String machineCode;
    /** 最近一次扫描时的目标台数；未扫描时为空，不能解释为零。 */
    private Integer targetMachineCount;
    /** 原始日期池。 */
    private String poolDate;
    /** 最近决策阶段。 */
    private String decisionStage;
    /** 最近决策原始原因。 */
    private String reason;
    /** 最近一次完整时间轴的换模开始时间。 */
    private String changeoverStart;
    /** 最近一次完整时间轴的首检或生产占用开始时间。 */
    private String occupationStart;
    /** 最近一次完整时间轴的正式生产开始时间。 */
    private String formalStart;
    /** 最近一次完整时间轴的占用班次开始时间戳。 */
    private Long occupationShiftStartMillis;
    /** 冻结时间轴模式；不将无供胚等待探针当成实际时间轴。 */
    private String timingMode;
    /** 此组合是否曾在当前竞争班次形成同班可执行提案；不表示最终提交成功。 */
    private boolean sameShiftProposalGenerated;
    /** 此组合最近一次真实提交返回结果；为空表示未观测到提交，不能声称提交失败。 */
    private String submissionOutcome;
}
