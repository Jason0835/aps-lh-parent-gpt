package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;

import java.util.List;

/**
 * S4.5 特殊 SKU 独立置换阶段策略。
 *
 * <p>特殊 SKU 先参加正常日期池竞争；正常阶段后仍有余量时，本策略只允许置换当前业务日
 * 新增排产形成的机台占用，不得截断或删除机台前序续作结果。</p>
 *
 * @author APS
 */
public interface SpecialSkuSchedulingStrategy {

    /**
     * 处理正常阶段后仍剩余的特殊 SKU，并同步日驱动绑定生命周期。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 三天窗口共用日驱动状态
     * @param specialCandidates 剩余特殊候选
     */
    void handleRemainingSpecialSkus(LhScheduleContext context,
                                    DayScheduleContext dayContext,
                                    DayDrivenScheduleState state,
                                    List<DailyNewSpecCandidate> specialCandidates);
}
