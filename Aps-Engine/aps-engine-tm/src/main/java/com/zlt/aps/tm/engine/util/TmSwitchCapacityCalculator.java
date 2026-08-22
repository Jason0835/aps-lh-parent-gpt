package com.zlt.aps.tm.engine.util;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 胎面任务切换与机台产能的纯计算工具。
 *
 * <p>仅处理数值归一化和算术，不读取上下文、不写入任务或候选机台证据，供任务链时间重算和
 * 机台候选产能计算复用，避免两处出现不同的空值、舍入或下限口径。</p>
 */
public final class TmSwitchCapacityCalculator {

    private TmSwitchCapacityCalculator() {
    }

    /**
     * 将固定产能扣减量按生产速度换算为切换小时数。
     *
     * @param capacityDeduct 固定产能扣减量
     * @param machineSpeed 生产速度
     * @return 非负切换小时数；扣减量或速度无效时返回零
     */
    public static BigDecimal convertCapacityDeductToHours(BigDecimal capacityDeduct, BigDecimal machineSpeed) {
        BigDecimal normalizedCapacityDeduct = BigDecimalUtils.valueOf(capacityDeduct);
        BigDecimal normalizedMachineSpeed = BigDecimalUtils.valueOf(machineSpeed);
        if (normalizedCapacityDeduct.compareTo(BigDecimal.ZERO) <= 0
                || normalizedMachineSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return normalizedCapacityDeduct.divide(normalizedMachineSpeed, TmScheduleConstants.DECIMAL_CALCULATION_SCALE,
                RoundingMode.HALF_UP);
    }

    /**
     * 计算扣除检修、已排计划和切换损失后的剩余产能。
     *
     * @param maxCapacity 班次最大产能
     * @param maintenanceDeduct 检修产能扣减量
     * @param assignedPlanQty 已排计划量
     * @param existingSwitchDeduct 已发生切换的产能扣减量
     * @param currentSwitchDeduct 当前候选任务的切换产能扣减量
     * @return 不小于零的剩余产能
     */
    public static BigDecimal calculateRemainCapacity(BigDecimal maxCapacity, BigDecimal maintenanceDeduct,
                                                     BigDecimal assignedPlanQty, BigDecimal existingSwitchDeduct,
                                                     BigDecimal currentSwitchDeduct) {
        return BigDecimalUtils.valueOf(maxCapacity).subtract(BigDecimalUtils.valueOf(maintenanceDeduct))
                .subtract(BigDecimalUtils.valueOf(assignedPlanQty))
                .subtract(BigDecimalUtils.valueOf(existingSwitchDeduct))
                .subtract(BigDecimalUtils.valueOf(currentSwitchDeduct)).max(BigDecimal.ZERO);
    }
}
