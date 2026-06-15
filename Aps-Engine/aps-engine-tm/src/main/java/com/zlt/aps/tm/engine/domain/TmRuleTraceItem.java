package com.zlt.aps.tm.engine.domain;

import lombok.Data;

/**
 * 胎面规则命中明细。
 *
 * <p>用于保存单条规则编码、执行结果和证据对象，供 `TmRuleTrace` 汇总为解释 JSON。</p>
 */
@Data
public class TmRuleTraceItem {

    /** 规则编码 */
    private String ruleCode;

    /** 执行结果 */
    private String result;

    /** 证据对象 */
    private Object evidence;

    /**
     * 创建规则命中明细。
     *
     * @param ruleCode 规则编码
     * @param result   执行结果
     * @param evidence 证据对象
     */
    public TmRuleTraceItem(String ruleCode, String result, Object evidence) {
        this.ruleCode = ruleCode;
        this.result = result;
        this.evidence = evidence;
    }
}
