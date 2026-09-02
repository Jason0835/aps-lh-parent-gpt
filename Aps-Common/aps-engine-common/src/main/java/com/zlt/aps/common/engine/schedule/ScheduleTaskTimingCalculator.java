package com.zlt.aps.common.engine.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * TM/TC 共用的任务链时间纯计算器。
 *
 * <p>只处理切换小时、计划量和生产速度到时间区间的换算，不读取上下文、不修改任务链节点。</p>
 */
public final class ScheduleTaskTimingCalculator {

    /** 一小时包含的秒数。 */
    private static final long SECONDS_PER_HOUR = 3600L;

    private ScheduleTaskTimingCalculator() {
    }

    /**
     * 按游标时间、切换时长、计划量和生产速度计算节点起止时间。
     *
     * @param cursorTime 前一节点结束时间或班次开始时间
     * @param planQty 当前节点计划量，单位米
     * @param machineSpeed 当前节点生产速度，单位米/小时
     * @param switchHours 当前节点切换时长，单位小时
     * @return 节点时间计算结果；关键输入无效时返回无效结果
     */
    public static ScheduleTaskTimingResult calculate(Date cursorTime, BigDecimal planQty,
                                                      BigDecimal machineSpeed, BigDecimal switchHours) {
        BigDecimal normalizedPlanQty = nonNegative(planQty);
        BigDecimal normalizedMachineSpeed = nonNegative(machineSpeed);
        if (cursorTime == null || normalizedPlanQty.compareTo(BigDecimal.ZERO) <= 0
                || normalizedMachineSpeed.compareTo(BigDecimal.ZERO) <= 0) {
            return new ScheduleTaskTimingResult(null, null, 0L, 0L);
        }
        long switchSeconds = nonNegative(switchHours)
                .multiply(BigDecimal.valueOf(SECONDS_PER_HOUR))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        Date startTime = new Date(cursorTime.getTime() + switchSeconds * 1000L);
        long productionSeconds = normalizedPlanQty.multiply(BigDecimal.valueOf(SECONDS_PER_HOUR))
                .divide(normalizedMachineSpeed, 0, RoundingMode.CEILING)
                .longValue();
        if (productionSeconds < 1L) {
            productionSeconds = 1L;
        }
        Date endTime = new Date(startTime.getTime() + productionSeconds * 1000L);
        return new ScheduleTaskTimingResult(startTime, endTime, switchSeconds, productionSeconds);
    }

    /**
     * 将空值或负值归零。
     *
     * @param value 原始数值
     * @return 非负数值
     */
    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
