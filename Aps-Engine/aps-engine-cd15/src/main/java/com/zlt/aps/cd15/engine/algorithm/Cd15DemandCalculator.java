package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 斜裁自动排程净需求量计算器。
 */
@Component
public class Cd15DemandCalculator {

    /**
     * 计算当前规格净需求量。
     *
     * @param demandQuantity 需求计算结果
     * @param accumulatedShortageQuantity 累计缺料量
     * @param expectedAvailableStock 预计可用库存
     * @param futureEffectiveScheduledQuantity 后续有效已排计划量
     * @return 不小于0的净需求量
     */
    public BigDecimal calculateNetDemand(BigDecimal demandQuantity,
                                         BigDecimal accumulatedShortageQuantity,
                                         BigDecimal expectedAvailableStock,
                                         BigDecimal futureEffectiveScheduledQuantity) {
        requireNonNegative(demandQuantity, "需求计算结果");
        requireNonNegative(accumulatedShortageQuantity, "累计缺料量");
        requireNonNegative(expectedAvailableStock, "预计可用库存");
        requireNonNegative(futureEffectiveScheduledQuantity, "后续有效已排计划量");

        return demandQuantity
                .add(accumulatedShortageQuantity)
                .subtract(expectedAvailableStock)
                .subtract(futureEffectiveScheduledQuantity)
                .max(BigDecimal.ZERO);
    }

    private void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(name + "不能小于0");
        }
    }
}
