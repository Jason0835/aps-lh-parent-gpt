package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * TM/TC 损耗率公共匹配结果。
 */
@Data
public class ScheduleLossRuleMatchResult {

    /** 匹配层级。 */
    private String matchLevel;

    /** 最终损耗率，百分比。 */
    private BigDecimal lossRate;

    /** 命中的公共损耗率规则。 */
    private ScheduleLossRuleModel matchedRule;
}
