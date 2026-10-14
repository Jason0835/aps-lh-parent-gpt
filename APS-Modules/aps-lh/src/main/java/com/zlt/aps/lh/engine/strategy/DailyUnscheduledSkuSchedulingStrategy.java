package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;

import java.util.List;

/**
 * S4.5 当日未上机 SKU 独立置换阶段策略。
 *
 * <p>SKU 先参加正常日期池竞争；当日所有班次均无正量且仍有合法待排需求时，
 * 本策略只允许置换当前业务日普通新增形成的机台占用，不得影响前日指定交替、结构切换及前序续作。</p>
 *
 * @author APS
 * @date 2026/10/10
 */
public interface DailyUnscheduledSkuSchedulingStrategy {

    /**
     * 处理正常阶段后当日未上机的 SKU，并同步日驱动绑定生命周期。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 三天窗口共用日驱动状态
     * @param pendingCandidates 通过当日正常准入的剩余候选
     */
    void handleUnscheduledSkus(LhScheduleContext context,
                               DayScheduleContext dayContext,
                               DayDrivenScheduleState state,
                               List<DailyNewSpecCandidate> pendingCandidates);
}
