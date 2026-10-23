package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftFieldUtil;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同步历史强制交接后的逐机审计事实，不重新计算余量分配或消费数量。
 *
 * @author APS
 * @date 2026/10/22
 */
public final class PreviousAlternateForcedReleaseSupport {

    /**
     * 工具类不允许实例化。
     */
    private PreviousAlternateForcedReleaseSupport() {
    }

    /**
     * 按新正量及强制释放更新快照，旧收尾公式不再约束主动让出的后段。
     *
     * @param context 已完成本机截量的上下文
     * @param event 源历史交替事件
     * @param result 本机原续作结果
     * @param release 本次真实交接时刻
     */
    public static void updateSnapshot(LhScheduleContext context, PreviousAlternatePlanReleaseEvent event,
            LhScheduleResult result, Date release) {
        ContinuationEndingAllocationSnapshot previous = context.getContinuationSurplusEndingSnapshotMap().get(result);
        if (Objects.isNull(previous)) {
            return;
        }
        ContinuationEndingAllocationSnapshot updated = BeanUtil.copyProperties(
                previous, ContinuationEndingAllocationSnapshot.class);
        updated.setShiftQuantities(context.getScheduleWindowShifts().stream().mapToInt(shift -> {
            Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
            return Objects.isNull(quantity) ? 0 : quantity;
        }).toArray());
        int firstShift = ShiftFieldUtil.resolveFirstPlannedShiftIndex(result);
        updated.setProductionStartTime(firstShift > 0 ? ShiftFieldUtil.getShiftStartTime(result, firstShift) : null);
        updated.setProductionEndTime(result.getSpecEndTime());
        updated.setReleaseTime(release);
        updated.setFinishState(ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE);
        updated.setFinishDeadline(release);
        updated.setHistoricalFinishPlanId(event.getPlan().getId());
        updated.setFinishRoleMachineCode(event.getMachineCode());
        updated.setFinishRule("历史指定后料强制交接");
        updated.setTDayFinishConstraint(null);
        updated.setNightFinishConstraint(null);
        context.getContinuationSurplusEndingSnapshotMap().put(result, updated);
        updateGroup(context, result);
    }

    /**
     * 写时复制对应收尾组的完成输出，避免旧的整组完成状态掩盖新增回流余量。
     *
     * @param context 当前收尾组与逐机快照
     * @param result 已截量的原续作结果
     */
    private static void updateGroup(LhScheduleContext context, LhScheduleResult result) {
        for (ContinuationRemainderFinishGroup group : context.getContinuationRemainderFinishGroups()) {
            if (!group.getOriginalResults().contains(result) || Objects.isNull(group.getOriginalMachineFinishPlan())) {
                continue;
            }
            ContinuationFinishScheduleResult updated = BeanUtil.copyProperties(
                    group.getOriginalMachineFinishPlan(), ContinuationFinishScheduleResult.class);
            List<ContinuationMachineFinishPlan> plans = group.getOriginalMachineFinishPlan().getMachinePlans().stream()
                    .map(plan -> BeanUtil.copyProperties(plan, ContinuationMachineFinishPlan.class))
                    .collect(Collectors.toList());
            for (int index = 0; index < group.getMachineProfiles().size(); index++) {
                ContinuationEndingMachineProfile profile = group.getMachineProfiles().get(index);
                if (!profile.getOriginals().contains(result)) {
                    continue;
                }
                ContinuationEndingAllocationSnapshot fact = context.getContinuationSurplusEndingSnapshotMap().get(result);
                ContinuationMachineFinishPlan plan = plans.get(index);
                plan.setPlanQty(profile.getOriginals().stream().mapToInt(ShiftFieldUtil::resolveScheduledQty).sum());
                plan.setFinishState(ContinuationMachineFinishPlan.FinishState.FORCED_RELEASE);
                plan.setFinishTime(fact.getProductionEndTime());
                plan.setOfflineTime(fact.getReleaseTime());
                plan.setFinishDeadline(fact.getReleaseTime());
                plan.setHistoricalFinishPlanId(fact.getHistoricalFinishPlanId());
                plan.setFinishRule(fact.getFinishRule());
                plan.setTDayFinishConstraint(null);
                plan.setNightFinishConstraint(null);
                int lastShift = ShiftFieldUtil.resolveLastPlannedShiftIndex(result);
                plan.setFinishShiftIndex(lastShift > 0 ? lastShift : null);
            }
            updated.setMachinePlans(plans);
            int previousQty = group.getOriginalMachineFinishPlan().getMachinePlans().stream()
                    .mapToInt(ContinuationMachineFinishPlan::getPlanQty).sum();
            updated.setRemainingQty(group.getOriginalMachineFinishPlan().getRemainingQty() + previousQty
                    - plans.stream().mapToInt(ContinuationMachineFinishPlan::getPlanQty).sum());
            group.setOriginalMachineFinishPlan(updated);
        }
    }
}
