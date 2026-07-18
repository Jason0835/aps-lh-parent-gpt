package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15DemandWindowResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 成型需求窗口计算测试。
 */
public class Cd15DemandWindowCalculatorTest {

    private final Cd15DemandWindowCalculator calculator = new Cd15DemandWindowCalculator();

    /**
     * T-02：AVERAGE按有效且需求大于0的班次数计算平均值。
     */
    @Test
    public void averageShouldUsePositiveEffectiveShiftCount() {
        Cd15DemandWindowResult result = calculator.calculate(Arrays.asList(
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
        Cd15DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("120", true), shift("140", true), shift("160", true)
        ), new BigDecimal("1000"), "SUM");

        assertEquals(new BigDecimal("520"), result.getDemandQuantity());
    }

    /**
     * 停产0值班次保留明细，但不进入AVERAGE除数。
     */
    @Test
    public void stoppedZeroShiftShouldNotEnterAverageDivisor() {
        Cd15DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("0", true), shift("200", true)
        ), new BigDecimal("1000"), "AVERAGE");

        assertEquals(new BigDecimal("150"), result.getDemandQuantity());
        assertEquals(2, result.getEffectiveShiftCount());
        assertEquals(3, result.getShiftDetails().size());
    }

    /** AVERAGE模式按2.5班权重计算，不能把半班当成完整第三班。 */
    @Test
    public void averageShouldUseFractionalWindowWeight() {
        Cd15DemandShift halfShift = shift("40", true);
        halfShift.setWindowWeight(new BigDecimal("0.5"));
        Cd15DemandWindowResult result = calculator.calculate(Arrays.asList(
                shift("100", true), shift("120", true), halfShift
        ), new BigDecimal("1000"), "AVERAGE");

        assertEquals(new BigDecimal("104"), result.getDemandQuantity());
    }

    private Cd15DemandShift shift(String quantity, boolean included) {
        return Cd15DemandShift.builder()
                .formingQuantity(new BigDecimal(quantity))
                .included(included)
                .shiftHours(new BigDecimal("8"))
                .build();
    }
}
