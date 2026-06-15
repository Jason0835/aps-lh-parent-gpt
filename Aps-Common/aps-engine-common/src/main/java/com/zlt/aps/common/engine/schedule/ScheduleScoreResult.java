package com.zlt.aps.common.engine.schedule;

import lombok.Data;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用排程评分结果。
 *
 * <p>用于记录评分策略对候选目标的评分项、总分和说明。该对象只承载评分结果，
 * 不修改任务链。</p>
 */
@Data
public class ScheduleScoreResult {

    /** 策略编码 */
    private String strategyCode;

    /** 评分项明细 */
    private Map<String, BigDecimal> scoreItems = new LinkedHashMap<>();

    /** 总分 */
    private BigDecimal totalScore = BigDecimal.ZERO;

    /** 评分说明 */
    private String description;

    public void setScoreItems(Map<String, BigDecimal> scoreItems) {
        this.scoreItems = scoreItems == null ? new LinkedHashMap<>() : scoreItems;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore == null ? BigDecimal.ZERO : totalScore;
    }
}
