package com.zlt.aps.tm.engine.domain;

import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面候选机台对象。
 *
 * <p>用于承载候选机台的剩余产能、链尾胶料、链尾口型、过滤状态和评分结果。规则链会
 * 原地标记过滤状态，评分策略会写入评分结果。</p>
 */
@Data
public class TmMachineCandidate {

    /** 机台编码 */
    private String machineCode;

    /** 剩余产能 */
    private BigDecimal remainCapacity;

    /** 链尾胶料 */
    private String tailGlueCode;

    /** 链尾口型板 */
    private String tailMouthPlateCode;

    /** 是否已被过滤 */
    private boolean filtered;

    /** 过滤原因编码 */
    private String filterReasonCode;

    /** 过滤原因描述 */
    private String filterReasonDesc;

    /** 过滤证据 */
    private Object filterEvidence;

    /** 评分结果 */
    private ScheduleScoreResult scoreResult;

    /**
     * 标记候选机台被过滤。
     *
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     * @param evidence   过滤证据
     */
    public void markFiltered(String reasonCode, String reasonDesc, Object evidence) {
        this.filtered = true;
        this.filterReasonCode = reasonCode;
        this.filterReasonDesc = reasonDesc;
        this.filterEvidence = evidence;
    }

    /**
     * 写入候选机台评分结果。
     *
     * @param scoreResult 评分结果
     */
    public void applyScore(ScheduleScoreResult scoreResult) {
        this.scoreResult = scoreResult;
    }
}
