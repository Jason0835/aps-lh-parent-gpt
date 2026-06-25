package com.zlt.aps.common.engine.schedule;

import lombok.Data;

/**
 * 通用排程规则执行结果。
 *
 * <p>用于表达规则是否通过、原因编码、原因描述和证据对象。该对象只承载规则证据，
 * 不修改任务链。</p>
 */
@Data
public class ScheduleRuleResult {

    /** 是否通过 */
    private boolean passed;

    /** 原因编码 */
    private String reasonCode;

    /** 原因描述 */
    private String reasonDesc;

    /** 规则证据 */
    private Object evidence;

    /**
     * 创建规则结果。
     *
     * @param passed     是否通过
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     * @param evidence   规则证据
     */
    public ScheduleRuleResult(boolean passed, String reasonCode, String reasonDesc, Object evidence) {
        this.passed = passed;
        this.reasonCode = reasonCode;
        this.reasonDesc = reasonDesc;
        this.evidence = evidence;
    }

    public static ScheduleRuleResult pass(String reasonCode, String reasonDesc, Object evidence) {
        return new ScheduleRuleResult(true, reasonCode, reasonDesc, evidence);
    }

    public static ScheduleRuleResult reject(String reasonCode, String reasonDesc, Object evidence) {
        return new ScheduleRuleResult(false, reasonCode, reasonDesc, evidence);
    }
}
