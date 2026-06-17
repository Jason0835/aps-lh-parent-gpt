package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90SpecialBigRollDecision;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 特殊大卷上机耗尽策略测试。 */
public class Cd90SpecialBigRollPolicyTest {

    private final Cd90SpecialBigRollPolicy policy = new Cd90SpecialBigRollPolicy();

    @Test
    public void shouldDetectConfiguredUseUpBigRoll() {
        Cd90SpecialBigRollDecision result = policy.decide(" CSTB5126 ", parameters());

        assertTrue(result.isSpecial());
        assertTrue(result.isConsumeAfterMounted());
        assertEquals(6, result.getLookaheadShifts());
        assertEquals(new BigDecimal("100"), result.getExtraStockLimit());
    }

    @Test
    public void shouldKeepNormalRollUnchanged() {
        Cd90SpecialBigRollDecision result = policy.decide("NORMAL_ROLL", parameters());

        assertFalse(result.isSpecial());
        assertFalse(result.isConsumeAfterMounted());
        assertEquals(0, result.getLookaheadShifts());
        assertEquals(BigDecimal.ZERO, result.getExtraStockLimit());
    }

    @Test
    public void shouldUseEmptyDecisionWhenParameterMissing() {
        Cd90SpecialBigRollDecision result = policy.decide("CSTB5126",
                Cd90AutoScheduleParameters.builder().specialRollUseUpCodes(Collections.emptyList()).build());

        assertFalse(result.isSpecial());
    }

    private Cd90AutoScheduleParameters parameters() {
        return Cd90AutoScheduleParameters.builder()
                .specialRollUseUpCodes(Arrays.asList("CSTB5126", "CSTA623"))
                .specialRollLookaheadShifts(6)
                .specialRollExtraStockLimit(new BigDecimal("100"))
                .build();
    }
}
