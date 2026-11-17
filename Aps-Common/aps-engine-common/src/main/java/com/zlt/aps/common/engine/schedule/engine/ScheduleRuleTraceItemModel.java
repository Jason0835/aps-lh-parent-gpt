package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/** TM/TC 规则轨迹条目公共运行态模型。 */
@Data
public class ScheduleRuleTraceItemModel {
    /**
     * 规则编码。
     */
    protected String ruleCode;
    /** 规则执行结果（如 PASS、FAIL、SKIP 等）。 */
    protected String result;
    /** 规则判定依据的证据对象。 */
    protected Object evidence;

    /**
     * 创建规则轨迹条目。
     *
     * @param ruleCode 规则编码
     * @param result   规则执行结果
     * @param evidence 判定证据对象
     */
    public ScheduleRuleTraceItemModel(String ruleCode, String result, Object evidence) {
        this.ruleCode = ruleCode;
        this.result = result;
        this.evidence = evidence;
    }
}
