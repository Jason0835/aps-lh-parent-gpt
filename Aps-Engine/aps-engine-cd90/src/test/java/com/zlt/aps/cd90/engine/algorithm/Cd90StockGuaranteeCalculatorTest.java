package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90StockGuaranteeResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 库存保证班数计算测试。
 */
public class Cd90StockGuaranteeCalculatorTest {

    private final Cd90StockGuaranteeCalculator calculator = new Cd90StockGuaranteeCalculator();

    /**
     * 250米库存可保证100、120及第三班150米中的0.2班。
     */
    @Test
    public void shouldCalculateFractionalGuaranteedShiftAndHours() {
        Cd90StockGuaranteeResult result = calculator.calculate(
                new BigDecimal("250"), Arrays.asList(
                        demand("100"), demand("120"), demand("150")
                ));

        assertEquals(new BigDecimal("2.2"), result.getGuaranteedShifts());
        assertEquals(new BigDecimal("17.6"), result.getSupplyHours());
    }

    private Cd90DemandShift demand(String quantity) {
        return Cd90DemandShift.builder()
                .clothDemandQuantity(new BigDecimal(quantity))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }
}
