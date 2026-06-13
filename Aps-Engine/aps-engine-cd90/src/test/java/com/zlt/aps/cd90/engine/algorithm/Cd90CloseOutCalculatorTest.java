package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 直裁收尾规格判定测试。
 */
public class Cd90CloseOutCalculatorTest {

    private final Cd90CloseOutCalculator calculator = new Cd90CloseOutCalculator();

    /**
     * 月计划剩余量小于等于净需求量时判定为收尾。
     */
    @Test
    public void planSurplusWithinDemandShouldBeCloseOut() {
        Cd90CloseOutDecision result = calculator.decide(
                new BigDecimal("100"), new BigDecimal("120"));

        assertTrue(result.isCloseOut());
        assertFalse(result.isMissingPlanSurplusWarning());
    }

    /**
     * 月计划剩余量缺失时按非收尾继续，并记录告警标识。
     */
    @Test
    public void missingPlanSurplusShouldContinueAsNormalWithWarning() {
        Cd90CloseOutDecision result = calculator.decide(null, new BigDecimal("120"));

        assertFalse(result.isCloseOut());
        assertTrue(result.isMissingPlanSurplusWarning());
    }
}
