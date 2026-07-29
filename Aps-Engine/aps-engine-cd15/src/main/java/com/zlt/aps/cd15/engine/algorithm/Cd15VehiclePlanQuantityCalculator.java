package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 小车钢带承载米数计算器。 */
@Component
public class Cd15VehiclePlanQuantityCalculator {

    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    /**
     * 计算一辆小车可承载的完整钢带米数。
     *
     * @param unitLengthMillimeter 单片胎体长度，单位毫米/片
     * @param coilMeter 单车卷取长度，单位米
     * @return 单车可承载的钢带米数
     */
    public BigDecimal calculate(BigDecimal unitLengthMillimeter,
                                BigDecimal coilMeter) {
        this.requirePositive(unitLengthMillimeter, "单片胎体长度");
        this.requirePositive(coilMeter, "单车卷取长度");

        BigDecimal unitLengthMeter = unitLengthMillimeter.divide(
                ONE_THOUSAND, 10, RoundingMode.HALF_UP);
        BigDecimal piecesPerVehicle = coilMeter.divide(
                unitLengthMeter, 0, RoundingMode.FLOOR);
        if (piecesPerVehicle.signum() <= 0) {
            throw new IllegalArgumentException("单片胎体长度不能大于单车卷取长度");
        }
        return this.normalize(piecesPerVehicle.multiply(unitLengthMeter));
    }

    private void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(name + "必须大于0");
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0) : normalized;
    }
}
