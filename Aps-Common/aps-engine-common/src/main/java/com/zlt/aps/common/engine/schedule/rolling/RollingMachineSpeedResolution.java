package com.zlt.aps.common.engine.schedule.rolling;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 解析自动滚动任务预计耗时使用的机台速度，并保留实际采用来源。
 */
@Getter
public class RollingMachineSpeedResolution {

    private final BigDecimal speed;
    private final String source;
    private final BigDecimal specificSpeed;
    private final BigDecimal machineSpeed;
    private final BigDecimal maxCapacity;
    private final BigDecimal shiftHours;

    private RollingMachineSpeedResolution(BigDecimal speed, String source, BigDecimal specificSpeed,
                                          BigDecimal machineSpeed, BigDecimal maxCapacity,
                                          BigDecimal shiftHours) {
        this.speed = speed;
        this.source = source;
        this.specificSpeed = specificSpeed;
        this.machineSpeed = machineSpeed;
        this.maxCapacity = maxCapacity;
        this.shiftHours = shiftHours;
    }

    /**
     * 按规格速度、机台速度、班产能推导速度的顺序解析预计时间速度。
     *
     * @param specificSpeed 机台与物料规格对应的速度
     * @param machineSpeed  机台通用速度
     * @param maxCapacity   当前物理班最大产能
     * @param shiftHours    当前物理班配置小时数
     * @param scale         班产能推导速度的小数位数
     * @return 解析结果；所有来源均无正值时速度为空，调用方应跳过预计时间计算
     * @throws IllegalArgumentException scale 为负数时抛出
     */
    public static RollingMachineSpeedResolution resolve(BigDecimal specificSpeed, BigDecimal machineSpeed,
                                                        BigDecimal maxCapacity, BigDecimal shiftHours,
                                                        int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("速度计算精度不能小于零");
        }
        if (AutomaticRollingPlanner.positive(specificSpeed)) {
            return new RollingMachineSpeedResolution(specificSpeed, "SPECIFIC", specificSpeed,
                    machineSpeed, maxCapacity, shiftHours);
        }
        if (AutomaticRollingPlanner.positive(machineSpeed)) {
            return new RollingMachineSpeedResolution(machineSpeed, "MACHINE_DEFAULT", specificSpeed,
                    machineSpeed, maxCapacity, shiftHours);
        }
        if (AutomaticRollingPlanner.positive(maxCapacity) && AutomaticRollingPlanner.positive(shiftHours)) {
            BigDecimal capacitySpeed = maxCapacity.divide(shiftHours, scale, RoundingMode.HALF_UP);
            if (AutomaticRollingPlanner.positive(capacitySpeed)) {
                return new RollingMachineSpeedResolution(capacitySpeed, "SHIFT_CAPACITY", specificSpeed,
                        machineSpeed, maxCapacity, shiftHours);
            }
        }
        return new RollingMachineSpeedResolution(null, "MISSING", specificSpeed,
                machineSpeed, maxCapacity, shiftHours);
    }
}
