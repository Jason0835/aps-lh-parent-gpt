package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 候选机台方案选择测试。
 */
public class Cd15MachineTrialSelectorTest {

    private final Cd15MachineTrialSelector selector = new Cd15MachineTrialSelector();

    /**
     * 完整容纳优先于只能部分容纳的定点机台。
     */
    @Test
    public void fullAccommodationShouldWinBeforePreferredMachine() {
        Cd15MachineTrial selected = selector.select(Arrays.asList(
                trial("M1", true, false, 0, false, "100", 200).build(),
                trial("M2", false, true, 1, true, "80", 10).build()
        ));

        assertEquals("M1", selected.getMachineCode());
    }

    /**
     * 同等完整容纳条件下依次比较定点、配置优先级和链尾同规格。
     */
    @Test
    public void shouldUseStableBusinessPriorityOrder() {
        Cd15MachineTrial selected = selector.select(Arrays.asList(
                trial("M1", true, true, 2, true, "100", 20).build(),
                trial("M2", true, true, 1, false, "100", 10).build(),
                trial("M3", true, true, 1, true, "100", 30).build()
        ));

        assertEquals("M3", selected.getMachineCode());
    }

    @Test
    public void zeroQuantityTrialsShouldUseStableLimitReasonPriority() {
        Cd15MachineTrial selected = selector.select(Arrays.asList(
                limitedTrial("M1", "CAPACITY_LIMIT", 0),
                limitedTrial("M2", "TOOLING_LIMIT", 1)
        ));

        assertEquals("M2", selected.getMachineCode());
        assertEquals("TOOLING_LIMIT", selected.getLimitReason());
    }
    /**
     * 续作或历史生产机台只要还有可排量，应优先留在原机台，不能因为其他机台可排更多就立刻换机。
     */
    @Test
    public void historyMachineShouldWinWhenItStillHasCapacity() {
        Cd15MachineTrial selected = selector.select(Arrays.asList(
                trial("G1301", false, false, 1, false, "320", 120).historyMachine(true).build(),
                trial("G1302", true, false, 0, false, "1040", 100).historyMachine(false).build()
        ));

        assertEquals("G1301", selected.getMachineCode());
    }
    private Cd15MachineTrial limitedTrial(String machineCode, String limitReason, int priorityOrder) {
        return Cd15MachineTrial.builder()
                .machineCode(machineCode)
                .finalSchedulableQuantity(BigDecimal.ZERO)
                .priorityOrder(priorityOrder)
                .remainingSeconds(0)
                .limitReason(limitReason)
                .build();
    }
    private Cd15MachineTrial.Cd15MachineTrialBuilder trial(String machineCode,
                                   boolean fullyAccommodated,
                                   boolean preferredMachine,
                                   int priorityOrder,
                                   boolean sameTailSpec,
                                   String finalQuantity,
                                   int remainingSeconds) {
        return Cd15MachineTrial.builder()
                .machineCode(machineCode)
                .fullyAccommodated(fullyAccommodated)
                .preferredMachine(preferredMachine)
                .priorityOrder(priorityOrder)
                .sameTailSpec(sameTailSpec)
                .finalSchedulableQuantity(new BigDecimal(finalQuantity))
                .remainingSeconds(remainingSeconds);
    }
}
