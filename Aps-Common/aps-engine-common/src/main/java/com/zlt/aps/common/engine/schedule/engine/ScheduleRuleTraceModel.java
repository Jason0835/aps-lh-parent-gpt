package com.zlt.aps.common.engine.schedule.engine;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * TM/TC 规则轨迹公共运行态模型。
 *
 * <p>只统一命中条目的保存和复制，具体解释 JSON 的外部结构仍由 TM/TC 领域轨迹类决定。</p>
 */
@Getter
public abstract class ScheduleRuleTraceModel implements ScheduleRuleTrace {

    /** 规则命中明细。 */
    protected final List<ScheduleRuleTraceItemModel> ruleHits = new ArrayList<>();

    /**
     * 追加一条字符串形式的规则命中记录。
     *
     * @param ruleCode 规则编码
     * @param result 规则结果
     * @param evidence 规则证据
     */
    @Override
    public void addRuleHit(String ruleCode, String result, Object evidence) {
        this.ruleHits.add(new ScheduleRuleTraceItemModel(ruleCode, result, evidence));
    }

    /**
     * 复制另一条公共规则轨迹的命中记录。
     *
     * @param sourceTrace 来源规则轨迹
     */
    public void appendFrom(ScheduleRuleTrace sourceTrace) {
        if (sourceTrace == null || sourceTrace.getRuleHits() == null) {
            return;
        }
        this.ruleHits.addAll(sourceTrace.getRuleHits());
    }
}
