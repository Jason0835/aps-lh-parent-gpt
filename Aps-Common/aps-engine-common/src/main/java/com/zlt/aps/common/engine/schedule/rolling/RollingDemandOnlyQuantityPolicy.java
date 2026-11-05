package com.zlt.aps.common.engine.schedule.rolling;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 定时滚动按需生产数量算子；启用条件由领域来源证明提供。
 */
public final class RollingDemandOnlyQuantityPolicy {
    private RollingDemandOnlyQuantityPolicy() {
    }

    /**
     * 正需求执行最低量和整数米，零保持零。
     *
     * @param desired 损耗后的目标
     * @param minimum 最低生产量
     * @return 合法目标
     */
    public static BigDecimal target(BigDecimal desired, BigDecimal minimum) {
        return desired.signum() <= 0 ? BigDecimal.ZERO : desired.max(minimum).setScale(0, RoundingMode.CEILING);
    }

    /**
     * 非整卷让位保持最低量或整行清零，安全上限绝不突破。
     *
     * @param current 当前量
     * @param maximum 可安全扣减量
     * @param gap     需要释放量
     * @param minimum 最低保留量
     * @return 整数米安全释放量
     */
    public static BigDecimal yieldQty(BigDecimal current, BigDecimal maximum, BigDecimal gap, BigDecimal minimum) {
        BigDecimal limit = maximum.min(current).max(BigDecimal.ZERO);
        BigDecimal keepLimit = current.subtract(minimum).max(BigDecimal.ZERO);
        BigDecimal reduce = gap.setScale(0, RoundingMode.CEILING).min(limit).min(keepLimit)
                .setScale(0, RoundingMode.FLOOR);
        if (reduce.signum() > 0) {
            return reduce;
        }
        return gap.signum() > 0 && maximum.compareTo(current) >= 0 ? current : BigDecimal.ZERO;
    }
}
