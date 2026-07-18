package com.zlt.aps.cd15.engine.algorithm;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁自动排程数量计算测试。
 */
public class Cd15ScheduleQuantityCalculatorTest {

    private final Cd15ScheduleQuantityCalculator calculator = new Cd15ScheduleQuantityCalculator();

    /**
     * T-04：收尾规格只叠加一次损耗，不补最小起排量和卷曲整倍数。
     */
    @Test
    public void closeOutQuantityShouldNotRoundOrFillMinimum() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), true, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("126"), result);
    }

    /**
     * T-05：非收尾规格先补最小起排量，再按卷曲长度向上取整。
     */
    @Test
    public void normalQuantityShouldFillMinimumAndRoundUp() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("120"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("87"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("348"), result);
    }
    /**
     * T-06：非收尾规格净需求量超过均分阈值时，先按净需求量除以2，再叠加损耗并按整卷向上取整。
     */
    @Test
    public void normalQuantityShouldShareWhenNetDemandExceedsThreshold() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("2500"), false, new BigDecimal("5"),
                new BigDecimal("300"), new BigDecimal("80"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("1360"), result);
    }
    @Test
    public void normalQuantityShouldRoundByVehiclePlanQuantity() {
        BigDecimal result = calculator.calculateActualQuantity(
                new BigDecimal("1305"), false, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("31.32"), new BigDecimal("2000"));

        assertEquals(new BigDecimal("1315.44"), result);
    }

}
