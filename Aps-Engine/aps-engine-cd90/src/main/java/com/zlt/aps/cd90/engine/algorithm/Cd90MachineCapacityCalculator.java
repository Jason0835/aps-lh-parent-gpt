package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90MachineCapacityTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTailState;
import com.zlt.aps.cd90.engine.constant.Cd90ChangeoverType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 直裁机台班产能增量试算器。
 *
 * <p>试算只返回产能结果，不更新任务链和班次剩余秒数。</p>
 */
@Component
public class Cd90MachineCapacityCalculator {

    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");

    /**
     * 计算候选机台当前班次可承载的数量。
     *
     * @param quota 满班理论定额，单位米/班
     * @param shiftHours 班次时长，单位小时
     * @param maintenanceSeconds 检修重叠秒数
     * @param previousSpec 上一任务规格
     * @param currentSpec 当前任务规格
     * @param specChangeMinutes 规格切换耗时分钟数
     * @param requestedQuantity 请求试排数量
     * @return 机台产能试算结果
     */
    public Cd90MachineCapacityTrial calculateInitial(BigDecimal quota,
                                                     int shiftHours,
                                                     int maintenanceSeconds,
                                                     String previousSpec,
                                                     String currentSpec,
                                                     int specChangeMinutes,
                                                     BigDecimal requestedQuantity) {
        if (maintenanceSeconds < 0) {
            throw new IllegalArgumentException("检修重叠秒数不能小于0");
        }
        int fullShiftSeconds = Math.multiplyExact(shiftHours, SECONDS_PER_HOUR.intValue());
        return calculateWithRemainingSeconds(quota, shiftHours,
                Math.max(0, fullShiftSeconds - maintenanceSeconds),
                previousSpec, currentSpec, specChangeMinutes, requestedQuantity);
    }

    /** 按大卷和直裁规格组合计算初始班产能。 */
    public Cd90MachineCapacityTrial calculateInitial(BigDecimal quota,
                                                     int shiftHours,
                                                     int maintenanceSeconds,
                                                     Cd90MachineTailState previousTail,
                                                     Cd90MachineTailState currentTail,
                                                     int sameRollDiffSpecMinutes,
                                                     int diffRollSameSpecMinutes,
                                                     int diffRollDiffSpecMinutes,
                                                     BigDecimal requestedQuantity) {
        if (maintenanceSeconds < 0) {
            throw new IllegalArgumentException("检修重叠秒数不能小于0");
        }
        int fullShiftSeconds = Math.multiplyExact(shiftHours, SECONDS_PER_HOUR.intValue());
        return calculateWithRemainingSeconds(quota, shiftHours,
                Math.max(0, fullShiftSeconds - maintenanceSeconds), previousTail, currentTail,
                sameRollDiffSpecMinutes, diffRollSameSpecMinutes, diffRollDiffSpecMinutes,
                requestedQuantity);
    }

    /**
     * 使用已扣除检修和前序任务占用后的剩余秒数执行增量试算。
     *
     * @param quota 满班理论定额，单位米/班
     * @param shiftHours 班次时长，单位小时
     * @param remainingSeconds 当前任务链可用剩余秒数
     * @param previousSpec 上一任务规格
     * @param currentSpec 当前任务规格
     * @param specChangeMinutes 规格切换耗时分钟数
     * @param requestedQuantity 请求试排数量
     * @return 机台产能试算结果
     */
    public Cd90MachineCapacityTrial calculateWithRemainingSeconds(BigDecimal quota,
                                                                  int shiftHours,
                                                                  int remainingSeconds,
                                                                  String previousSpec,
                                                                  String currentSpec,
                                                                  int specChangeMinutes,
                                                                  BigDecimal requestedQuantity) {
        requirePositive(quota, "满班理论定额");
        requirePositive(requestedQuantity, "请求试排数量");
        if (shiftHours <= 0 || remainingSeconds < 0 || specChangeMinutes < 0) {
            throw new IllegalArgumentException("班次、剩余时间和切换时间参数不合法");
        }

        int fullShiftSeconds = Math.multiplyExact(shiftHours, SECONDS_PER_HOUR.intValue());
        BigDecimal speed = quota.divide(BigDecimal.valueOf(fullShiftSeconds), 12, RoundingMode.HALF_UP);
        int changeSeconds = Objects.equals(previousSpec, currentSpec) ? 0 : specChangeMinutes * 60;
        int productionAvailableSeconds = Math.max(0, remainingSeconds - changeSeconds);
        BigDecimal capacityQuantity = quota
                .multiply(BigDecimal.valueOf(productionAvailableSeconds))
                .divide(BigDecimal.valueOf(fullShiftSeconds), 10, RoundingMode.DOWN);
        BigDecimal schedulableQuantity = requestedQuantity.min(capacityQuantity);
        int productionSeconds = schedulableQuantity.signum() == 0 ? 0
                : schedulableQuantity
                        .multiply(BigDecimal.valueOf(fullShiftSeconds))
                        .divide(quota, 0, RoundingMode.CEILING)
                        .intValueExact();
        int afterSeconds = Math.max(0, remainingSeconds - changeSeconds - productionSeconds);

        return Cd90MachineCapacityTrial.builder()
                .machineSpeed(speed)
                .changeSeconds(changeSeconds)
                .productionSeconds(productionSeconds)
                .capacityQuantity(normalize(schedulableQuantity))
                .remainingSeconds(afterSeconds)
                .fullyAccommodated(schedulableQuantity.compareTo(requestedQuantity) >= 0)
                .build();
    }

    /** 按大卷和直裁规格组合计算剩余班产能。 */
    public Cd90MachineCapacityTrial calculateWithRemainingSeconds(BigDecimal quota,
                                                                  int shiftHours,
                                                                  int remainingSeconds,
                                                                  Cd90MachineTailState previousTail,
                                                                  Cd90MachineTailState currentTail,
                                                                  int sameRollDiffSpecMinutes,
                                                                  int diffRollSameSpecMinutes,
                                                                  int diffRollDiffSpecMinutes,
                                                                  BigDecimal requestedQuantity) {
        requirePositive(quota, "满班理论定额");
        requirePositive(requestedQuantity, "请求试排数量");
        if (shiftHours <= 0 || remainingSeconds < 0 || sameRollDiffSpecMinutes < 0
                || diffRollSameSpecMinutes < 0 || diffRollDiffSpecMinutes < 0) {
            throw new IllegalArgumentException("班次、剩余时间和切换时间参数不合法");
        }
        int fullShiftSeconds = Math.multiplyExact(shiftHours, SECONDS_PER_HOUR.intValue());
        BigDecimal speed = quota.divide(BigDecimal.valueOf(fullShiftSeconds), 12, RoundingMode.HALF_UP);
        Cd90ChangeoverType changeoverType = resolveChangeover(previousTail, currentTail);
        int changeSeconds = changeMinutes(changeoverType, sameRollDiffSpecMinutes,
                diffRollSameSpecMinutes, diffRollDiffSpecMinutes) * 60;
        int productionAvailableSeconds = Math.max(0, remainingSeconds - changeSeconds);
        BigDecimal capacityQuantity = quota.multiply(BigDecimal.valueOf(productionAvailableSeconds))
                .divide(BigDecimal.valueOf(fullShiftSeconds), 10, RoundingMode.DOWN);
        BigDecimal schedulableQuantity = requestedQuantity.min(capacityQuantity);
        int productionSeconds = schedulableQuantity.signum() == 0 ? 0
                : schedulableQuantity.multiply(BigDecimal.valueOf(fullShiftSeconds))
                        .divide(quota, 0, RoundingMode.CEILING).intValueExact();
        return Cd90MachineCapacityTrial.builder().machineSpeed(speed)
                .changeSeconds(changeSeconds).productionSeconds(productionSeconds)
                .capacityQuantity(normalize(schedulableQuantity))
                .remainingSeconds(Math.max(0, remainingSeconds - changeSeconds - productionSeconds))
                .fullyAccommodated(schedulableQuantity.compareTo(requestedQuantity) >= 0).build();
    }

    /** 判断前后任务的大卷和直裁规格组合。 */
    public Cd90ChangeoverType resolveChangeover(Cd90MachineTailState previousTail,
                                                Cd90MachineTailState currentTail) {
        if (previousTail == null || currentTail == null) {
            return Cd90ChangeoverType.NONE;
        }
        boolean sameRoll = Objects.equals(previousTail.getBigRollCode(), currentTail.getBigRollCode());
        boolean sameSpec = Objects.equals(previousTail.getClothCode(), currentTail.getClothCode());
        if (sameRoll && sameSpec) {
            return Cd90ChangeoverType.NONE;
        }
        if (sameRoll) {
            return Cd90ChangeoverType.SAME_ROLL_DIFF_SPEC;
        }
        return sameSpec ? Cd90ChangeoverType.DIFF_ROLL_SAME_SPEC
                : Cd90ChangeoverType.DIFF_ROLL_DIFF_SPEC;
    }

    private int changeMinutes(Cd90ChangeoverType type, int sameRollDiffSpecMinutes,
                              int diffRollSameSpecMinutes, int diffRollDiffSpecMinutes) {
        if (type == Cd90ChangeoverType.SAME_ROLL_DIFF_SPEC) {
            return sameRollDiffSpecMinutes;
        }
        if (type == Cd90ChangeoverType.DIFF_ROLL_SAME_SPEC) {
            return diffRollSameSpecMinutes;
        }
        if (type == Cd90ChangeoverType.DIFF_ROLL_DIFF_SPEC) {
            return diffRollDiffSpecMinutes;
        }
        return 0;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.stripTrailingZeros().scale() < 0 ? value.setScale(0) : value.stripTrailingZeros();
    }

    private void requirePositive(BigDecimal value, String name) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(name + "必须大于0");
        }
    }
}
