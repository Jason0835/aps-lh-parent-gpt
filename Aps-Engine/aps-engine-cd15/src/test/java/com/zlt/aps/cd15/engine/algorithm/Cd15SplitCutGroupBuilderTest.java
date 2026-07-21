package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 单规格分裁双宽资格判断测试。 */
public class Cd15SplitCutGroupBuilderTest {

    private final Cd15SplitCutGroupBuilder builder =
            new Cd15SplitCutGroupBuilder();

    /** 两倍斜裁宽度等于角度上限时允许单规格一出二。 */
    @Test
    public void shouldAllowSingleSpecSplitWhenDoubleWidthMatchesLimit() {
        boolean result = builder.canSingleSpecSplit(
                candidate(), Collections.singletonMap(
                        "18", new BigDecimal("180")));

        assertTrue(result);
    }

    /** 两倍斜裁宽度超过角度上限时禁止单规格一出二。 */
    @Test
    public void shouldRejectSingleSpecSplitWhenDoubleWidthExceedsLimit() {
        boolean result = builder.canSingleSpecSplit(
                candidate(), Collections.singletonMap(
                        "18", new BigDecimal("179.99")));

        assertFalse(result);
    }

    private Cd15ScheduleCandidate candidate() {
        return Cd15ScheduleCandidate.builder()
                .materialKey("CSS14016|BR1|18|90|127.3|87|false")
                .steelStripCode("CSS14016")
                .bigRollCode("BR1")
                .cuttingAngle("18")
                .craftWidth(new BigDecimal("90"))
                .build();
    }
}
