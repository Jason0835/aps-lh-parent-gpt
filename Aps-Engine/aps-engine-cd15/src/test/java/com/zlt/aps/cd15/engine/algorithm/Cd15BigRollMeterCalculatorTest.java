package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 大卷消耗米数计算测试。
 */
public class Cd15BigRollMeterCalculatorTest {

    private final Cd15BigRollMeterCalculator calculator = new Cd15BigRollMeterCalculator();

    /**
     * 卷曲总长度乘斜裁宽度后除以大卷幅宽，得到大卷消耗米数。
     */
    @Test
    public void shouldCalculateBigRollMeterByCutArea() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("87"), new BigDecimal("280"), new BigDecimal("1400"));

        assertEquals(new BigDecimal("17.4"), result);
    }

    /**
     * 缺少斜裁宽度时不能继续计算。
     */
    @Test(expected = IllegalArgumentException.class)
    public void missingCraftWidthShouldFail() {
        calculator.calculate(new BigDecimal("87"), null, new BigDecimal("1400"));
    }
}
