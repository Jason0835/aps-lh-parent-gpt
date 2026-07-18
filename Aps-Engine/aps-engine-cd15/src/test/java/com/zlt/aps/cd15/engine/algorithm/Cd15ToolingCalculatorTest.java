package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ToolingTrial;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * 斜裁自动排程工装试算测试。
 */
public class Cd15ToolingCalculatorTest {

    private final Cd15ToolingCalculator calculator = new Cd15ToolingCalculator();

    /**
     * T-06：无可用工装时本轮可排量为0。
     */
    @Test
    public void noAvailableToolingShouldScheduleZero() {
        Cd15ToolingTrial result = calculator.calculate(
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
        Cd15ToolingTrial result = calculator.calculate(
                new BigDecimal("126"), 10, 9, new BigDecimal("87"));

        assertEquals(1, result.getAvailableToolingCount());
        assertEquals(new BigDecimal("87"), result.getSchedulableQuantity());
        assertEquals(new BigDecimal("39"), result.getLimitedQuantity());
    }
}
