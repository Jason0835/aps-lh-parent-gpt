package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

/** TM/TC 规则轨迹条目公共运行态模型。 */
@Data
public class ScheduleRuleTraceItemModel {
    protected String ruleCode;
    protected String result;
    protected Object evidence;

    public ScheduleRuleTraceItemModel(String ruleCode, String result, Object evidence) {
        this.ruleCode = ruleCode;
        this.result = result;
        this.evidence = evidence;
    }
}
