package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.core.utils.BigDecimalUtils;

import java.math.BigDecimal;

/**
 * 排程速度单位边界；资料采用米/分钟，任务链及容量计算统一采用米/小时。
 */
public final class ScheduleSpeedUnits {

    /**
     * 一小时包含的分钟数。
     */
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimalUtils.valueOf(60);

    private ScheduleSpeedUnits() {
    }

    /**
     * 将原始速度资料转为内部小时速度。仅用于资料读取，禁止对运行态速度重复调用。
     *
     * @param metersPerMinute 原始米/分钟速度，允许为空或非正
     * @return 米/小时速度；无效资料返回0，由调用方沿用原缺失速度处理
     */
    public static BigDecimal fromMetersPerMinute(BigDecimal metersPerMinute) {
        return metersPerMinute == null || metersPerMinute.signum() <= 0
                ? BigDecimal.ZERO : metersPerMinute.multiply(MINUTES_PER_HOUR);
    }

    /**
     * 为本次解析保存真实来源证明，不改变已经归一的内部速度。
     *
     * @param task               当前隔离任务
     * @param normalizedSpeed    内部米/小时速度
     * @param rawMetersPerMinute 可选资料原值，缺失时记录内部小时值
     * @param source             资料或产能回退来源标记
     * @return 原内部速度，不再次乘60
     */
    public static BigDecimal withProof(ScheduleTaskDraftModel task, BigDecimal normalizedSpeed,
                                       BigDecimal rawMetersPerMinute, String source) {
        boolean sourceMatches = rawMetersPerMinute != null && rawMetersPerMinute.signum() > 0
                && normalizedSpeed != null
                && fromMetersPerMinute(rawMetersPerMinute).compareTo(normalizedSpeed) == 0;
        task.setMachineSpeedRawValue(sourceMatches ? rawMetersPerMinute : normalizedSpeed);
        task.setMachineSpeedRawUnit(sourceMatches ? "M_PER_MINUTE" : "M_PER_HOUR");
        task.setMachineSpeedSource(source);
        return normalizedSpeed;
    }
}
