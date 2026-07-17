package com.zlt.aps.tc.engine.strategy;

import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.tc.engine.domain.TcMachineCandidate;
import com.zlt.aps.tc.engine.domain.TcMachineRuleContext;

/**
 * 胎侧候选机台过滤规则接口。
 *
 * <p>用于开班、机台状态、检修、口型、胶料、定点禁排和产能等硬约束过滤。</p>
 */
public interface ITcMachineFilterRule {

    /**
     * 获取规则编码。
     *
     * @return 规则编码
     */
    String getRuleCode();

    /**
     * 执行机台过滤规则。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 规则执行结果
     */
    ScheduleRuleResult evaluate(TcMachineCandidate candidate, TcMachineRuleContext context);
}
