package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.SpecialSkuSchedulingStrategy;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.service.impl.NewSpecResultSubstitutionCoordinator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 特殊 SKU 新增结果置换策略。
 *
 * <p>本策略只负责编排特殊候选顺序，具体的新增结果撤销、指定机台重排、账本恢复和
 * 跨日在机绑定替换统一委托给协调器。日驱动主链的阶段顺序保持不变。</p>
 *
 * @author APS
 */
@Component
public class DefaultSpecialSkuSchedulingStrategy implements SpecialSkuSchedulingStrategy {

    /** 新增结果置换的中心协调入口。 */
    @Resource
    private NewSpecResultSubstitutionCoordinator substitutionCoordinator;

    @Override
    public void handleRemainingSpecialSkus(LhScheduleContext context,
                                           DayScheduleContext dayContext,
                                           DayDrivenScheduleState state,
                                           List<DailyNewSpecCandidate> specialCandidates) {
        if (CollectionUtils.isEmpty(specialCandidates)) {
            return;
        }
        /*
         * 指定机台隔离排产会复用一次新增主链，并再次进入特殊阶段。临时置换指令存在时
         * 当前调用属于内层验证，必须直接返回，避免递归触发同一置换算法。
         */
        if (context != null && context.getScheduleSubstitutionDirective() != null) {
            return;
        }
        this.substitutionCoordinator.substitute(
                context, dayContext, state, specialCandidates);
    }
}
