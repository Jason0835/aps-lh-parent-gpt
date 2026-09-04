package com.zlt.aps.lh.util;

import java.util.Objects;

/**
 * 首检时间轴模式决策结果。
 */
public final class FirstInspectionTimingModeDecision {

    /** 集中解析出的时间模式。 */
    private final FirstInspectionTimingMode timingMode;

    /** 模式选择原因，直接用于预演、提交和对账日志。 */
    private final String modeReason;

    private FirstInspectionTimingModeDecision(FirstInspectionTimingMode timingMode,
                                              String modeReason) {
        this.timingMode = Objects.requireNonNull(timingMode, "timingMode不能为空");
        this.modeReason = Objects.requireNonNull(modeReason, "modeReason不能为空");
    }

    /**
     * 创建模式决策。
     *
     * @param timingMode 时间模式
     * @param modeReason 模式原因
     * @return 不可变决策结果
     */
    public static FirstInspectionTimingModeDecision of(FirstInspectionTimingMode timingMode,
                                                       String modeReason) {
        return new FirstInspectionTimingModeDecision(timingMode, modeReason);
    }

    public FirstInspectionTimingMode getTimingMode() {
        return timingMode;
    }

    public String getModeReason() {
        return modeReason;
    }
}
