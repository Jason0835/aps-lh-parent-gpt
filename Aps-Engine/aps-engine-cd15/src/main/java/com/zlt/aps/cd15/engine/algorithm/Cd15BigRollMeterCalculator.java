package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CD15 大卷消耗米数计算。
 */
@Component
public class Cd15BigRollMeterCalculator {

    private static final BigDecimal MILLIMETER_PER_METER = BigDecimal.valueOf(1000L);

    /**
     * 按面积守恒计算大卷消耗：片数 * BELT_LENGTH * BELT_CRAFT / CORD_WIDTH / 1000。
     * CORD_WIDTH 为空或非正时，调用方可使用净需求米数作为当前口径兜底。
     */
    public BigDecimal calculateBigRollConsumeMeters(BigDecimal pieceCount,
                                                    BigDecimal unitConsumeMillimeter,
                                                    BigDecimal craftWidthMillimeter,
                                                    BigDecimal cordWidthMillimeter,
                                                    BigDecimal fallbackNetDemandMeters) {
        if (!this.isPositive(pieceCount) || !this.isPositive(unitConsumeMillimeter)
                || !this.isPositive(craftWidthMillimeter) || !this.isPositive(cordWidthMillimeter)) {
            return fallbackNetDemandMeters == null ? BigDecimal.ZERO : fallbackNetDemandMeters;
        }
        return pieceCount.multiply(unitConsumeMillimeter).multiply(craftWidthMillimeter)
                .divide(cordWidthMillimeter, 8, RoundingMode.HALF_UP)
                .divide(MILLIMETER_PER_METER, 4, RoundingMode.HALF_UP);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}