package com.zlt.aps.common.engine.schedule;

import java.util.List;
import java.util.Map;

/**
 * 公共默认机台过滤规则链模板。
 *
 * <p>模板统一处理工作日历门禁、规则顺序、静态过滤跳过产能、规则启停、通过证据和拒绝状态写回，
 * TM/TC 通过适配方法保留领域规则编码、参数和独有约束。</p>
 *
 * @param <C> 机台规则上下文类型
 * @param <T> 待排任务类型
 * @param <M> 候选机台类型
 */
public abstract class AbstractMachineFilterRule<C, T, M> {

    /**
     * 执行完整机台过滤规则链。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 过滤结果
     * @throws RuntimeException 候选机台或上下文校验失败时由领域适配器抛出业务异常
     */
    public final ScheduleRuleResult evaluate(M candidate, C context) {
        return this.evaluateInternal(candidate, context, false);
    }

    /**
     * 执行不含当前班次剩余产能判断的静态过滤规则链。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 静态过滤结果
     * @throws RuntimeException 候选机台或上下文校验失败时由领域适配器抛出业务异常
     */
    public final ScheduleRuleResult evaluateStatic(M candidate, C context) {
        return this.evaluateInternal(candidate, context, true);
    }

    /**
     * 执行指定过滤范围。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @param staticOnly 是否跳过剩余产能规则
     * @return 过滤结果
     */
    private ScheduleRuleResult evaluateInternal(M candidate, C context, boolean staticOnly) {
        this.validateCandidateAndContext(candidate, context);
        ScheduleRuleResult calendarResult = this.checkWorkCalendarConflict(candidate, context);
        if (calendarResult != null) {
            return this.reject(candidate, calendarResult);
        }

        T task = this.getTask(context);
        List<String> ruleOrder = this.resolveRuleOrder(context);
        Map<String, Object> evidence = this.getCandidateEvidence(candidate);
        evidence.put(staticOnly ? "staticFilterRuleOrder" : "filterRuleOrder", ruleOrder);
        for (String ruleCode : ruleOrder) {
            if (staticOnly && this.getRemainCapacityRuleCode().equals(ruleCode)) {
                evidence.put("staticFilterSkipped:" + ruleCode, Boolean.TRUE);
                continue;
            }
            if (!this.isRuleEnabled(context, ruleCode)) {
                evidence.put("filterRuleDisabled:" + ruleCode, Boolean.TRUE);
                continue;
            }
            ScheduleRuleResult ruleResult = this.evaluateRule(ruleCode, candidate, context);
            if (ruleResult != null) {
                return this.reject(candidate, ruleResult);
            }
        }

        this.setFiltered(candidate, false);
        Map<String, Object> passEvidence = this.getCandidateEvidence(candidate);
        passEvidence.put("task", task == null ? null : this.getTaskBusinessKey(task));
        passEvidence.put("machineCode", this.getMachineCode(candidate));
        return ScheduleRuleResult.pass(this.getDefaultPassCode(), this.getDefaultPassDescription(), passEvidence);
    }

    /**
     * 校验候选机台和规则上下文。
     *
     * @param candidate 候选机台
     * @param context   规则上下文
     */
    protected abstract void validateCandidateAndContext(M candidate, C context);

    /**
     * 获取上下文中的任务。
     *
     * @param context 规则上下文
     * @return 待排任务
     */
    protected abstract T getTask(C context);

    /**
     * 获取任务业务键。
     *
     * @param task 待排任务
     * @return 业务键
     */
    protected abstract String getTaskBusinessKey(T task);

    /**
     * 获取候选机台证据映射。
     *
     * @param candidate 候选机台
     * @return 证据映射
     */
    protected abstract Map<String, Object> getCandidateEvidence(M candidate);

    /**
     * 获取机台编码。
     *
     * @param candidate 候选机台
     * @return 机台编码
     */
    protected abstract String getMachineCode(M candidate);

    /**
     * 写入候选机台过滤状态。
     *
     * @param candidate 候选机台
     * @param filtered 是否被过滤
     */
    protected abstract void setFiltered(M candidate, boolean filtered);

    /**
     * 写入候选机台过滤原因。
     *
     * @param candidate 候选机台
     * @param reasonCode 过滤原因编码
     * @param reasonDesc 过滤原因说明
     */
    protected abstract void setFilterReason(M candidate, String reasonCode, String reasonDesc);

    /**
     * 检查工作日历停产门禁，并由领域实现写入日历证据。
     *
     * @param candidate 候选机台
     * @param context   规则上下文
     * @return 发生门禁冲突时返回拒绝结果，否则返回 null
     */
    protected abstract ScheduleRuleResult checkWorkCalendarConflict(M candidate, C context);

    /**
     * 解析本次排程实际使用的规则顺序。
     *
     * @param context 规则上下文
     * @return 规则编码顺序
     */
    protected abstract List<String> resolveRuleOrder(C context);

    /**
     * 判断规则是否启用。
     *
     * @param context  规则上下文
     * @param ruleCode 规则编码
     * @return 是否启用
     */
    protected abstract boolean isRuleEnabled(C context, String ruleCode);

    /**
     * 执行单项领域过滤规则。
     *
     * @param ruleCode 规则编码
     * @param candidate 候选机台
     * @param context 规则上下文
     * @return 规则拒绝结果；通过或未知规则返回 null
     */
    protected abstract ScheduleRuleResult evaluateRule(String ruleCode, M candidate, C context);

    /**
     * 获取剩余产能规则编码。
     *
     * @return 剩余产能规则编码
     */
    protected String getRemainCapacityRuleCode() {
        return "REMAIN_CAPACITY";
    }

    /**
     * 获取默认通过原因编码。
     *
     * @return 通过原因编码
     */
    protected abstract String getDefaultPassCode();

    /**
     * 获取默认通过原因说明。
     *
     * @return 通过原因说明
     */
    protected abstract String getDefaultPassDescription();

    /**
     * 写入候选过滤原因并构建拒绝结果。
     *
     * @param candidate 候选机台
     * @param result    领域规则拒绝结果
     * @return 公共拒绝结果
     */
    private ScheduleRuleResult reject(M candidate, ScheduleRuleResult result) {
        this.setFiltered(candidate, true);
        this.setFilterReason(candidate, result.getReasonCode(), result.getReasonDesc());
        Map<String, Object> evidence = this.getCandidateEvidence(candidate);
        evidence.put("ruleCode", result.getReasonCode());
        evidence.put("reasonDesc", result.getReasonDesc());
        return ScheduleRuleResult.reject(result.getReasonCode(), result.getReasonDesc(), evidence);
    }
}
