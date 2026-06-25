package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90ToolingTrial;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 直裁自动排程工装试算测试。
 */
public class Cd90ToolingCalculatorTest {

    private final Cd90ToolingCalculator calculator = new Cd90ToolingCalculator();

    /**
     * T-06：无可用工装时本轮可排量为0。
     */
    @Test
    public void noAvailableToolingShouldScheduleZero() {
        Cd90ToolingTrial result = calculator.calculate(
                new BigDecimal("126"), 10, 10, new BigDecimal("87"));

        assertEquals(0, result.getAvailableToolingCount());
        assertEquals(BigDecimal.ZERO, result.getSchedulableQuantity());
        assertEquals(new BigDecimal("126"), result.getLimitedQuantity());
    }

    /**
     * T-06：仅一个可用工装时最多可排一个卷曲长度。
     */
    @Test
    public void oneAvailableToolingShouldLimitToOneCoil() {
        Cd90ToolingTrial result = calculator.calculate(
                new BigDecimal("126"), 10, 9, new BigDecimal("87"));

        assertEquals(1, result.getAvailableToolingCount());
        assertEquals(new BigDecimal("87"), result.getSchedulableQuantity());
        assertEquals(new BigDecimal("39"), result.getLimitedQuantity());
    }
}
