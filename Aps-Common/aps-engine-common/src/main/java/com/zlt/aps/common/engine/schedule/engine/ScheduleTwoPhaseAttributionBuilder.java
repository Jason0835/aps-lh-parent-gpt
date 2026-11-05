package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 来源归属视图构造器；只创建解释副本，不创建第二份物理生产或资源占用。
 */
public final class ScheduleTwoPhaseAttributionBuilder {

    /**
     * 将一条物理行按原额度归属拆成内部解释视图，物理行始终保持最终完整数量。
     *
     * @param context  已完成逐班分配的运行上下文
     * @param copyTask 领域独立副本函数
     * @param <T>      领域任务类型
     * @return 来源归属片段，不得提交至任务链或结果表
     */
    public <T extends ScheduleTaskDraftModel> List<T> build(ScheduleContextModel<T, ?, ?, ?, ?> context,
                                                            Function<T, T> copyTask) {
        List<T> fragments = new ArrayList<>();
        for (T task : context.getTaskDraftList()) {
            if (task == null) {
                continue;
            }
            Map<String, BigDecimal> owners = context.getTwoPhaseCarryLedger().ownersOf(task.getBusinessKey());
            if (task.getQuantityStage() != ScheduleQuantityStage.ASSIGNED || owners.isEmpty()) {
                fragments.add(task);
                continue;
            }
            owners = new LinkedHashMap<>(owners);
            if (BigDecimalUtils.valueOf(task.getExtraAssignQty()).signum() > 0) {
                owners.putIfAbsent(task.getPlanGroupKey(), BigDecimal.ZERO);
            }
            for (Map.Entry<String, BigDecimal> owner : owners.entrySet()) {
                BigDecimal extra = Objects.equals(owner.getKey(), task.getPlanGroupKey())
                        ? BigDecimalUtils.valueOf(task.getExtraAssignQty()) : BigDecimal.ZERO;
                BigDecimal quantity = owner.getValue().add(extra);
                if (quantity.signum() <= 0) {
                    continue;
                }
                T fragment = copyTask.apply(task);
                task.copyTwoPhaseStateTo(fragment);
                fragment.setPhysicalPlanQty(task.getPlanQty());
                fragment.setPhysicalBaseAssignQty(task.getBaseAssignQty());
                fragment.setPhysicalExtraAssignQty(task.getExtraAssignQty());
                fragment.setPlanGroupKey(owner.getKey());
                fragment.setPlanQty(quantity);
                fragment.setBaseAssignQty(owner.getValue());
                fragment.setExtraAssignQty(extra);
                if (!Objects.equals(owner.getKey(), task.getPlanGroupKey())) {
                    SchedulePlanTaskGroup<T> originGroup = context.getPlanTaskGroupMap().get(owner.getKey());
                    if (originGroup == null) {
                        throw new IllegalStateException("Base ownership refers to an unknown source plan group");
                    }
                    T origin = originGroup.getAggregateTask();
                    fragment.setDemandEventKey(origin.getDemandEventKey());
                    fragment.setDemandShiftKey(origin.getDemandShiftKey());
                    fragment.setOriginDemandShiftKey(origin.getOriginDemandShiftKey());
                    fragment.setOriginBaseQuotaKey(origin.getOriginBaseQuotaKey());
                    fragment.setBaseProductionShiftKey(origin.getBaseProductionShiftKey());
                    fragment.setCopyPurpose(ScheduleCopyPurpose.BASE_CARRY);
                    fragment.setSettlementMode(ScheduleSettlementMode.POST_LOSS_CARRY);
                    fragment.setExtraPolicy(ScheduleExtraPolicy.FORBID_BASE_CARRY);
                    fragment.setBaseCommittedQty(owner.getValue());
                    fragment.setTargetCommittedQty(owner.getValue());
                    fragment.setBaseRequired(owner.getValue());
                    fragment.setBaseRemaining(BigDecimal.ZERO);
                    fragment.setExtraPotentialQty(BigDecimal.ZERO);
                    fragment.setExtraTargetQty(BigDecimal.ZERO);
                    fragment.setExtraRemaining(BigDecimal.ZERO);
                    fragment.setExtraAllowedQty(BigDecimal.ZERO);
                    fragment.setExtraActualQty(BigDecimal.ZERO);
                    fragment.setToolLedgerOrder(null);
                    fragment.setToolUsedQty(BigDecimal.ZERO);
                    fragment.setAvailableToolQty(null);
                    fragment.setRemainingToolQty(null);
                    new ScheduleTwoPhaseRequestPreparer().relocateBaseCarry(fragment,
                            context.getScheduleDate(), task.getShiftOrder(), origin.getProductionAllocKey(),
                            task.getBusinessKey() + ":" + owner.getKey());
                    fragment.setQuantityStage(ScheduleQuantityStage.ASSIGNED);
                }
                fragments.add(fragment);
            }
        }
        return fragments;
    }
}
