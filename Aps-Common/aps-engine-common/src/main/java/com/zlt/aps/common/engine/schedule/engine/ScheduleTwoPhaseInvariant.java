package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 两阶段输入证明及物理结果守恒检查，不读库、不改变事实或资源。
 */
public final class ScheduleTwoPhaseInvariant {

    /**
     * 检查已结算输入证明，保证顺延与显式预置不会重新损耗或换机套用旧候选。
     *
     * @param task        待选中任务
     * @param machineCode 即将承接的机台
     * @return 证明是否完整且与候选机台相符
     */
    public boolean validPostLossInput(ScheduleTaskDraftModel task, String machineCode) {
        if (task.getSettlementMode() == ScheduleSettlementMode.POST_LOSS_PRESET) {
            return task.getInputQuantityStage() == ScheduleQuantityStage.POST_LOSS_PRESET
                    && task.getInputSnapshotKey() != null && task.getPresetInputPlanQty() != null
                    && task.getPresetInputPlanQty().signum() >= 0
                    && task.getPresetInputPlanQty().stripTrailingZeros().scale() <= 0;
        }
        if (task.getSettlementMode() == ScheduleSettlementMode.POST_LOSS_CARRY) {
            return task.getCopyPurpose() == ScheduleCopyPurpose.BASE_CARRY
                    && task.getInputQuantityStage() == ScheduleQuantityStage.POST_LOSS_CARRY
                    && task.getParentInputSnapshotKey() != null && task.getOriginBaseQuotaKey() != null
                    && task.getSettlementMachineCode() != null
                    && Objects.equals(task.getSettlementMachineCode(), machineCode)
                    && task.getCarryRequestQty() != null && task.getCarryRequestQty().signum() >= 0;
        }
        return true;
    }

    /**
     * 核对物理结果单机、整数米及分量守恒；诊断副本不作为生产事实。
     *
     * @param tasks 领域真实任务集合
     * @return 是否满足同日单机和数量守恒
     */
    public boolean validAssignments(List<? extends ScheduleTaskDraftModel> tasks) {
        Map<String, String> machines = new HashMap<>();
        for (ScheduleTaskDraftModel task : tasks) {
            if (task == null || task.getQuantityStage() != ScheduleQuantityStage.ASSIGNED) {
                continue;
            }
            // 保留已执行事实沿用历史数量精度，不参与本次两阶段整数米与分量重算，但仍参与同日单机校验。
            if (task.getSettlementMode() == ScheduleSettlementMode.RETAINED_FACT
                    || task.getRequestSourceMode() == ScheduleRequestSourceMode.RETAINED_FACT) {
                if (BigDecimalUtils.valueOf(task.getPlanQty()).signum() > 0) {
                    if (task.getMachineCode() == null || task.getProcessCode() == null) {
                        return false;
                    }
                    String retainedMachine = machines.putIfAbsent(task.getProcessCode(), task.getMachineCode());
                    if (retainedMachine != null && !Objects.equals(retainedMachine, task.getMachineCode())) {
                        return false;
                    }
                }
                continue;
            }
            BigDecimal quantity = BigDecimalUtils.valueOf(task.getPlanQty());
            BigDecimal base = BigDecimalUtils.valueOf(task.getBaseAssignQty());
            BigDecimal extra = BigDecimalUtils.valueOf(task.getExtraAssignQty());
            // 独立备库行不能混入基础额度，来源归属不依赖物理结果表新增列。
            if (Boolean.TRUE.equals(task.getExtraCreateTask())
                    && (base.signum() != 0 || extra.compareTo(quantity) != 0
                    || !Boolean.TRUE.equals(task.getExtraEligibleTask())
                    || extra.compareTo(BigDecimalUtils.valueOf(task.getExtraTargetQty())) > 0)) {
                return false;
            }
            if (quantity.signum() < 0 || base.signum() < 0 || extra.signum() < 0
                    || base.add(extra).compareTo(quantity) != 0
                    || quantity.stripTrailingZeros().scale() > 0) {
                return false;
            }
            if (quantity.signum() > 0) {
                if (task.getMachineCode() == null || task.getProcessCode() == null) {
                    return false;
                }
                String previous = machines.putIfAbsent(task.getProcessCode(), task.getMachineCode());
                if (previous != null && !Objects.equals(previous, task.getMachineCode())) {
                    return false;
                }
            }
        }
        return true;
    }
}
