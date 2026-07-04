package com.zlt.aps.tm.engine.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面损耗率匹配结果。
 */
@Data
public class TmLossRuleMatchResult {

    /** 匹配层级 */
    private String matchLevel;

    /** 最终损耗率，百分比 */
    private BigDecimal lossRate;

    /** 命中规则 */
    private TmLossRule matchedRule;
}