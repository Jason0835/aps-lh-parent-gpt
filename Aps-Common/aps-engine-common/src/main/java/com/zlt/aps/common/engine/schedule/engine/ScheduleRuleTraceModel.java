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
     * 只写入解释 JSON、不进入现有过程日志渲染链路的证据。
     */
    protected final List<ScheduleRuleTraceItemModel> explainOnlyRuleHits = new ArrayList<>();

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
     * 追加只供解释 JSON 使用的规则证据。
     *
     * @param ruleCode 规则编码
     * @param result   规则结果
     * @param evidence 结构化证据
     */
    public void addExplainOnlyRuleHit(String ruleCode, String result, Object evidence) {
        this.explainOnlyRuleHits.add(new ScheduleRuleTraceItemModel(ruleCode, result, evidence));
    }

    /**
     * 获取规则解释 JSON 的完整命中列表。
     *
     * @return 普通过程证据和解释专用证据的合并列表
     */
    public List<ScheduleRuleTraceItemModel> getAllRuleHits() {
        List<ScheduleRuleTraceItemModel> allRuleHits = new ArrayList<>(this.ruleHits);
        allRuleHits.addAll(this.explainOnlyRuleHits);
        return allRuleHits;
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
        if (sourceTrace instanceof ScheduleRuleTraceModel) {
            this.explainOnlyRuleHits.addAll(((ScheduleRuleTraceModel) sourceTrace).getExplainOnlyRuleHits());
        }
    }
}
