package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁净需求量计算测试。
 */
public class Cd15DemandCalculatorTest {

    private final Cd15DemandCalculator calculator = new Cd15DemandCalculator();

    /**
     * 净需求量扣减预计库存和后续有效已排计划，最低为0。
     */
    @Test
    public void netDemandShouldNotBeNegative() {
        BigDecimal result = calculator.calculateNetDemand(
                new BigDecimal("100"), new BigDecimal("20"),
                new BigDecimal("80"), new BigDecimal("50"));

        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * 累计缺料量应加入当前需求。
     */
    @Test
    public void shortageShouldIncreaseNetDemand() {
        BigDecimal result = calculator.calculateNetDemand(
                new BigDecimal("100"), new BigDecimal("30"),
                new BigDecimal("20"), new BigDecimal("10"));

        assertEquals(new BigDecimal("100"), result);
    }
}
