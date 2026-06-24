package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90LossRateSelection;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 直裁损耗率优先级测试。
 */
public class Cd90LossRateResolverTest {

    private final Cd90LossRateResolver resolver = new Cd90LossRateResolver();

    /**
     * 帘布加机台规则优先于帘布、机台和通用规则。
     */
    @Test
    public void clothAndMachineRuleShouldHaveHighestPriority() {
        Cd90LossRateSelection result = resolver.resolve("C1", "M1", Arrays.asList(
                rule(null, null, "4"),
                rule("C1", null, "3"),
                rule(null, "M1", "2"),
                rule("C1", "M1", "1")
        ));

        assertEquals(new BigDecimal("1"), result.getLossRatePercent());
        assertEquals("CLOTH_MACHINE", result.getMatchedLevel());
    }

    /**
     * 四层优先级均未命中且未提供兜底时仍抛异常，保持原有强校验语义。
     */
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenNoRuleAndNoFallback() {
        resolver.resolve("C1", "M1", Arrays.asList(), null);
    }

    /**
     * 四层优先级均未命中时使用参数 SYS0701003 兜底损耗率，命中层级标记为 FALLBACK。
     */
    @Test
    public void shouldUseFallbackWhenNoRuleMatched() {
        Cd90LossRateSelection result = resolver.resolve("C1", "M1", Arrays.asList(),
                new BigDecimal("5"));

        assertEquals(new BigDecimal("5"), result.getLossRatePercent());
        assertEquals("FALLBACK", result.getMatchedLevel());
    }

    private Cd90LossRateRule rule(String clothCode, String machineCode, String rate) {
        return Cd90LossRateRule.builder()
                .clothCode(clothCode)
                .machineCode(machineCode)
                .lossRatePercent(new BigDecimal(rate))
                .build();
    }
}
