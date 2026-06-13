package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InventoryProjection;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 直裁预计库存计算测试。
 */
public class Cd90InventoryCalculatorTest {

    private final Cd90InventoryCalculator calculator = new Cd90InventoryCalculator();

    /**
     * T-01：负库存转为累计缺料量，不作为实物库存继续使用。
     */
    @Test
    public void negativeBalanceShouldBecomeShortage() {
        Cd90InventoryProjection result = calculator.project(
                new BigDecimal("100"), new BigDecimal("250"), new BigDecimal("50"));

        assertEquals(new BigDecimal("-100"), result.getInventoryBalance());
        assertEquals(BigDecimal.ZERO, result.getExpectedAvailableStock());
        assertEquals(new BigDecimal("100"), result.getAccumulatedShortageQuantity());
    }
}
