package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;

/**
 * 新增排产单个Machine×SKU组合的无副作用真实可开产计划解析入口。
 *
 * <p>实现必须复用正式新增排产时间轴，不得修改机台、模具、换模、首检、胶囊、结果或数量账本。</p>
 *
 * @author APS
 */
@FunctionalInterface
public interface NewSpecMachineAvailabilityResolver {

    /**
     * 解析指定机台与候选SKU的真实可开产计划。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param candidate 当前日期池候选
     * @param machine 指定运行态机台
     * @return 无副作用时间计划；不可用时返回带原因的计划
     */
    NewSpecMachineAvailabilityPlan resolve(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DailyNewSpecCandidate candidate,
            MachineScheduleDTO machine);
}
