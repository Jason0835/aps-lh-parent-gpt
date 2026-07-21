package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/** 小车卷取方向与斜裁排程方向换算测试。 */
public class Cd15VehiclePlanQuantityCalculatorTest {

    private final Cd15VehiclePlanQuantityCalculator calculator =
            new Cd15VehiclePlanQuantityCalculator();

    @Test
    public void shouldConvertLengthDirectionCapacityToCraftDirectionQuantity() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("2600"), new BigDecimal("1044"), new BigDecimal("80"));

        assertEquals(new BigDecimal("31.32"), result);
    }
}
