package com.zlt.aps.tq.engine.domain;

import lombok.Data;

/**
 * 胎圈规则命中明细。
 *
 * <p>保存单条规则编码、执行结果和证据对象，供 {@link TqRuleTrace} 汇总为解释 JSON。</p>
 *
 * @author APS
 */
@Data
public class TqRuleTraceItem {

    /** 规则编码（对应 {@link com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum#getCode()}） */
    private String ruleCode;

    /** 执行结果（对应 {@link com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum#getCode()}） */
    private String result;

    /** 证据对象（任意可序列化为 JSON 的对象，如 Map、POJO 等） */
    private Object evidence;

    /**
     * 创建规则命中明细。
     *
     * @param ruleCode 规则编码
     * @param result   执行结果
     * @param evidence 证据对象
     */
    public TqRuleTraceItem(String ruleCode, String result, Object evidence) {
        this.ruleCode = ruleCode;
        this.result = result;
        this.evidence = evidence;
    }
}
