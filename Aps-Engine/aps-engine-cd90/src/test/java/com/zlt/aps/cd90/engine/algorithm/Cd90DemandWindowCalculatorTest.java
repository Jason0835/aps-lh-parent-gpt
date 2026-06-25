package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90DemandWindowResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 成型需求窗口计算测试。
 */
public class Cd90DemandWindowCalculatorTest {

    private final Cd90DemandWindowCalculator calculator = new Cd90DemandWindowCalculator();

    /**
     * T-02：AVERAGE按有效且需求大于0的班次数计算平均值。
     */
    @Test
    public void averageShouldUsePositiveEffectiveShiftCount() {
        Cd90DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("120", true), shift("140", true), shift("160", true)
        ), new BigDecimal("1000"), "AVERAGE");

        assertEquals(new BigDecimal("130"), result.getDemandQuantity());
        assertEquals(4, result.getEffectiveShiftCount());
    }

    /**
     * T-02：SUM直接累计各有效班次需求。
     */
    @Test
    public void sumShouldAccumulateAllEffectiveShifts() {
        Cd90DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("120", true), shift("140", true), shift("160", true)
        ), new BigDecimal("1000"), "SUM");

        assertEquals(new BigDecimal("520"), result.getDemandQuantity());
    }

    /**
     * 停产0值班次保留明细，但不进入AVERAGE除数。
     */
    @Test
    public void stoppedZeroShiftShouldNotEnterAverageDivisor() {
        Cd90DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("0", true), shift("200", true)
        ), new BigDecimal("1000"), "AVERAGE");

        assertEquals(new BigDecimal("150"), result.getDemandQuantity());
        assertEquals(2, result.getEffectiveShiftCount());
        assertEquals(3, result.getShiftDetails().size());
    }

    private Cd90DemandShift shift(String quantity, boolean included) {
        return Cd90DemandShift.builder()
                .formingQuantity(new BigDecimal(quantity))
                .included(included)
                .shiftHours(new BigDecimal("8"))
                .build();
    }
}
