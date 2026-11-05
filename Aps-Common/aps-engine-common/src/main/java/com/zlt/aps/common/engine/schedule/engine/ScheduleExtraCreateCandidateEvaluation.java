package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个物料在一台机上的隔离试算结果，不代表实际生产或资源占用。
 */
@Data
public class ScheduleExtraCreateCandidateEvaluation<T extends ScheduleTaskDraftModel> {
    /**
     * 已保存来源身份及机台专属结算的独立副本。
     */
    private T task;
    /**
     * 机台编码，用于稳定并列和同物料有限次选。
     */
    private String machineCode;
    /**
     * 最终合法整行承接量，已经过损耗、卷长、起排及全部资源取小。
     */
    private BigDecimal allowedQty;
    /**
     * 原机台评分，总分0仍可成为合法候选。
     */
    private BigDecimal score;
    /**
     * 来源、日历、时效、口型、切换和绑定证明全部有效；未知不是true。
     */
    private boolean proofsReady;
    /**
     * 公共窗口门禁证据，不能只用一个布尔值绕过时效和白名单校验。
     */
    private ScheduleExtraCreateWindowProof windowProof;
    /**
     * 未通过试算时的稳定拒绝码；为空表示该机台已通过全部门禁。
     */
    private String rejectReasonCode;
    /**
     * 未通过试算时已经算出的数量或时间明细，可为空。
     */
    private String rejectDetail;
}
