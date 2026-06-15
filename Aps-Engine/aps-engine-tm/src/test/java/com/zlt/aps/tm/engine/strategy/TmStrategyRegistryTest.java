package com.zlt.aps.tm.engine.strategy;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 胎面策略注册表测试。
 *
 * <p>验证策略缺失时明确抛出异常，避免主流程吞异常或静默走默认算法。</p>
 */
public class TmStrategyRegistryTest {

    @Test(expected = IllegalArgumentException.class)
    public void getDemandQtyStrategyShouldRejectMissingStrategy() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        registry.getDemandQtyStrategy("UNKNOWN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPlanQtyStrategyShouldRejectMissingStrategy() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        registry.getPlanQtyStrategy("UNKNOWN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMachineFilterRuleShouldRejectMissingRule() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        registry.getMachineFilterRule("UNKNOWN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMachineScoreStrategyShouldRejectMissingStrategy() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        registry.getMachineScoreStrategy("UNKNOWN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTaskSortStrategyShouldRejectMissingStrategy() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        registry.getTaskSortStrategy("UNKNOWN");
    }

    @Test
    public void getDemandQtyStrategyShouldReturnRegisteredStrategy() {
        ITmDemandQtyStrategy strategy = new FixedDemandQtyStrategy();
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.singletonList(strategy), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        assertEquals(strategy, registry.getDemandQtyStrategy("A"));
    }
}
