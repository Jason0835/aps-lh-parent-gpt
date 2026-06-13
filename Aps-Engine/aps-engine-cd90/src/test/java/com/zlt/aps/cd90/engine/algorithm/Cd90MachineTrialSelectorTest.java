package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * 候选机台方案选择测试。
 */
public class Cd90MachineTrialSelectorTest {

    private final Cd90MachineTrialSelector selector = new Cd90MachineTrialSelector();

    /**
     * 完整容纳优先于只能部分容纳的定点机台。
     */
    @Test
    public void fullAccommodationShouldWinBeforePreferredMachine() {
        Cd90MachineTrial selected = selector.select(Arrays.asList(
                trial("M1", false, true, 0, false, "100", 200),
                trial("M2", true, false, 1, true, "80", 10)
        ));

        assertEquals("M1", selected.getMachineCode());
    }

    /**
     * 同等完整容纳条件下依次比较定点、配置优先级和链尾同规格。
     */
    @Test
    public void shouldUseStableBusinessPriorityOrder() {
        Cd90MachineTrial selected = selector.select(Arrays.asList(
                trial("M1", true, true, 2, true, "100", 20),
                trial("M2", true, true, 1, false, "100", 10),
                trial("M3", true, true, 1, true, "100", 30)
        ));

        assertEquals("M3", selected.getMachineCode());
    }

    private Cd90MachineTrial trial(String machineCode,
                                   boolean fullyAccommodated,
                                   boolean preferredMachine,
                                   int priorityOrder,
                                   boolean sameTailSpec,
                                   String finalQuantity,
                                   int remainingSeconds) {
        return Cd90MachineTrial.builder()
                .machineCode(machineCode)
                .fullyAccommodated(fullyAccommodated)
                .preferredMachine(preferredMachine)
                .priorityOrder(priorityOrder)
                .sameTailSpec(sameTailSpec)
                .finalSchedulableQuantity(new BigDecimal(finalQuantity))
                .remainingSeconds(remainingSeconds)
                .build();
    }
}
