package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CloseOutDecision;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoCloseOutItem;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

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

    /** 所有关联胎胚都达到各自月计划剩余量时才收尾。 */
    @Test
    public void allEmbryosMustReachTheirOwnSurplus() {
        Cd90CloseOutDecision notCloseOut = calculator.decide(Arrays.asList(
                item("E1", "100", "80"), item("E2", "40", "50")));
        assertFalse(notCloseOut.isCloseOut());

        Cd90CloseOutDecision closeOut = calculator.decide(Arrays.asList(
                item("E1", "100", "80"), item("E2", "50", "50")));
        assertTrue(closeOut.isCloseOut());
    }

    private Cd90EmbryoCloseOutItem item(String embryoCode, String plan, String surplus) {
        return Cd90EmbryoCloseOutItem.builder().embryoCode(embryoCode)
                .calculatedPlanQuantity(new BigDecimal(plan))
                .planSurplusQuantity(new BigDecimal(surplus)).build();
    }
}
