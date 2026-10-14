package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.DailyUnscheduledSkuSchedulingStrategy;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.service.impl.NewSpecResultSubstitutionCoordinator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 当日未上机 SKU 新增结果置换策略。
 *
 * <p>本策略保留正常候选顺序，具体的当日零量筛选、新增结果撤销、指定机台重排、账本恢复和
 * 跨日在机绑定替换统一委托给协调器。日驱动主链的阶段顺序保持不变。</p>
 *
 * @author APS
 * @date 2026/10/10
 */
@Component
public class DefaultDailyUnscheduledSkuSchedulingStrategy implements DailyUnscheduledSkuSchedulingStrategy {

    /**
     * 新增结果置换的中心协调入口。
     */
    @Resource
    private NewSpecResultSubstitutionCoordinator substitutionCoordinator;

    /**
     * 将当日剩余正常候选交给置换协调器，隔离重排作用域内禁止递归置换。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param state 跨业务日绑定及候选状态
     * @param pendingCandidates 通过正常准入的剩余候选
     */
    @Override
    public void handleUnscheduledSkus(LhScheduleContext context,
                                      DayScheduleContext dayContext,
                                      DayDrivenScheduleState state,
                                      List<DailyNewSpecCandidate> pendingCandidates) {
        if (CollectionUtils.isEmpty(pendingCandidates)) {
            return;
        }
        /*
         * 指定机台隔离排产会复用一次新增主链，并再次进入当日置换阶段。临时置换指令存在时
         * 当前调用属于内层验证，必须直接返回，避免递归触发同一置换算法。
         */
        if (context != null && context.getScheduleSubstitutionDirective() != null) {
            return;
        }
        this.substitutionCoordinator.substitute(
                context, dayContext, state, pendingCandidates);
    }
}
