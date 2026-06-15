package com.zlt.aps.tm.engine.strategy;

import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import com.zlt.aps.tm.engine.domain.TmMachineRuleContext;

/**
 * 胎面机台评分策略接口。
 *
 * <p>用于对已通过硬约束的候选机台评分，评分结果用于排序和解释输出。</p>
 */
public interface ITmMachineScoreStrategy {

    /**
     * 获取评分策略编码。
     *
     * @return 策略编码
     */
    String getStrategyCode();

    /**
     * 执行候选机台评分。
     *
     * @param candidate 候选机台
     * @param context   机台规则上下文
     * @return 评分结果
     */
    ScheduleScoreResult score(TmMachineCandidate candidate, TmMachineRuleContext context);
}
