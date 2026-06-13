package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90CandidateMachineTrialInput;
import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 候选机台组合试算测试。
 */
public class Cd90CandidateMachineTrialCalculatorTest {

    private final Cd90CandidateMachineTrialCalculator calculator =
            new Cd90CandidateMachineTrialCalculator(
                    new Cd90LossRateResolver(),
                    new Cd90ScheduleQuantityCalculator(),
                    new Cd90ToolingCalculator(),
                    new Cd90MachineCapacityCalculator());

    /**
     * 最终可排量取实际排产量、工装可排量和机台产能可排量的最小值。
     */
    @Test
    public void finalQuantityShouldUseMinimumConstraint() {
        Cd90CandidateMachineTrialInput input = Cd90CandidateMachineTrialInput.builder()
                .clothCode("C1")
                .machineCode("M1")
                .netDemandQuantity(new BigDecimal("120"))
                .closeOut(false)
                .minimumStartQuantity(new BigDecimal("300"))
                .coilMeter(new BigDecimal("87"))
                .totalToolingCount(10)
                .occupiedVehicleCount(9)
                .quota(new BigDecimal("2400"))
                .shiftHours(8)
                .remainingSeconds(28800)
                .previousSpec("A")
                .currentSpec("B")
                .specChangeMinutes(5)
                .lossRateRules(Collections.singletonList(Cd90LossRateRule.builder()
                        .lossRatePercent(new BigDecimal("5"))
                        .build()))
                .build();

        Cd90MachineTrial result = calculator.calculate(input);

        assertEquals(new BigDecimal("348"), result.getActualQuantity());
        assertEquals(new BigDecimal("87"), result.getToolingQuantity());
        assertEquals(new BigDecimal("87"), result.getFinalSchedulableQuantity());
        assertFalse(result.isFullyAccommodated());
    }
}
