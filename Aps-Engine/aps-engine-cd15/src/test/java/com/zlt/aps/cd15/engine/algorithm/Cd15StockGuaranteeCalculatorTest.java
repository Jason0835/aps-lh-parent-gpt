package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15StockGuaranteeResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 库存保证班数计算测试。
 */
public class Cd15StockGuaranteeCalculatorTest {

    private final Cd15StockGuaranteeCalculator calculator = new Cd15StockGuaranteeCalculator();

    /**
     * 250米库存可保证100、120及第三班150米中的0.2班。
     */
    @Test
    public void shouldCalculateFractionalGuaranteedShiftAndHours() {
        Cd15StockGuaranteeResult result = calculator.calculate(
                new BigDecimal("250"), Arrays.asList(
                        demand("100"), demand("120"), demand("150")
                ));

        assertEquals(new BigDecimal("2.2"), result.getGuaranteedShifts());
        assertEquals(new BigDecimal("17.6"), result.getSupplyHours());
    }

    /** 半班窗口完整满足时只累计0.5班和4小时。 */
    @Test
    public void shouldRespectFractionalWindowWeight() {
        Cd15DemandShift halfShift = demand("40");
        halfShift.setWindowWeight(new BigDecimal("0.5"));

        Cd15StockGuaranteeResult result = calculator.calculate(
                new BigDecimal("40"), Arrays.asList(halfShift));

        assertEquals(new BigDecimal("0.5"), result.getGuaranteedShifts());
        assertEquals(new BigDecimal("4"), result.getSupplyHours());
    }

    private Cd15DemandShift demand(String quantity) {
        return Cd15DemandShift.builder()
                .steelStripDemandQuantity(new BigDecimal(quantity))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }
}
