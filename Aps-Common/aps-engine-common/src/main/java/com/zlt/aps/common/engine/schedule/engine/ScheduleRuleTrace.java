package com.zlt.aps.common.engine.schedule.engine;

import java.util.List;

/** TM/TC 规则轨迹公共写入契约。 */
public interface ScheduleRuleTrace {

    List<? extends ScheduleRuleTraceItemModel> getRuleHits();

    void addRuleHit(String ruleCode, String result, Object evidence);

    String toExplainJson();
}
