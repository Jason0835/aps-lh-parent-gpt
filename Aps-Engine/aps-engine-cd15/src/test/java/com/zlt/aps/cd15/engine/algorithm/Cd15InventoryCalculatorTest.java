package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15InventoryProjection;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁预计库存计算测试。
 */
public class Cd15InventoryCalculatorTest {

    private final Cd15InventoryCalculator calculator = new Cd15InventoryCalculator();

    /**
     * T-01：负库存转为累计缺料量，不作为实物库存继续使用。
     */
    @Test
    public void negativeBalanceShouldBecomeShortage() {
        Cd15InventoryProjection result = calculator.project(
                new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("50"));

        assertEquals(new BigDecimal("-100"), result.getInventoryBalance());
        assertEquals(BigDecimal.ZERO, result.getExpectedAvailableStock());
        assertEquals(new BigDecimal("100"), result.getAccumulatedShortageQuantity());
    }
}
