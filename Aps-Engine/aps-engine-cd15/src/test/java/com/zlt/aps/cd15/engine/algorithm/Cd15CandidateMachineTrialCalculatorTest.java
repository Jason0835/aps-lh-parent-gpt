package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15CandidateMachineTrialInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 候选机台组合试算测试。
 */
public class Cd15CandidateMachineTrialCalculatorTest {

    private final Cd15CandidateMachineTrialCalculator calculator =
            new Cd15CandidateMachineTrialCalculator(
                    new Cd15LossRateResolver(),
                    new Cd15ScheduleQuantityCalculator(),
                    new Cd15ToolingCalculator(),
                    new Cd15MachineCapacityCalculator(),
                    new Cd15BigRollAgingAllocator(),
                    new Cd15BigRollMeterCalculator());

    /**
     * 最终可排量取实际排产量、工装可排量和机台产能可排量的最小值。
     */
    @Test
    public void finalQuantityShouldUseMinimumConstraint() {
        Cd15CandidateMachineTrialInput input = Cd15CandidateMachineTrialInput.builder()
                
                .machineCode("M1")
                .netDemandQuantity(new BigDecimal("120"))
                .closeOut(false)
                .minimumStartQuantity(new BigDecimal("300"))
                .equalShareThreshold(new BigDecimal("2000"))
                .vehiclePlanQuantity(new BigDecimal("87"))
                .totalToolingCount(10)
                .occupiedVehicleCount(9)
                .shiftCapacity(new BigDecimal("2400"))
                .shiftHours(8)
                .remainingSeconds(28800)
                .previousSpec("A")
                .currentSpec("B")
                .specChangeMinutes(5)
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(new BigDecimal("5"))
                        .build()))
                .build();

        Cd15MachineTrial result = calculator.calculate(input);

        assertEquals(new BigDecimal("348"), result.getActualQuantity());
        assertEquals(new BigDecimal("87"), result.getToolingQuantity());
        assertEquals(new BigDecimal("87"), result.getFinalSchedulableQuantity());
        assertFalse(result.isFullyAccommodated());
    }

    /** 单规格一出二完整排程必须把奇数片需求补齐为偶数片。 */
    @Test
    public void shouldRoundSingleSpecSplitDemandUpToCompletePair() {
        Cd15CandidateMachineTrialInput input = singleSpecSplitInput("309", "1000");
        Cd15MachineTrial result = calculator.calculate(input);
        assertEquals(new BigDecimal("310"), result.getActualQuantity());
        assertEquals(new BigDecimal("310"), result.getFinalSchedulableQuantity());
    }

    /** 单规格分裁超过均分阈值时，试排必须携带首班量和下一班精确余量。 */
    @Test
    public void shouldExposeSingleSpecSplitEqualShareRemainder() {
        Cd15CandidateMachineTrialInput input = Cd15CandidateMachineTrialInput.builder()
                .machineCode("G1101")
                .netDemandQuantity(new BigDecimal("45.384"))
                .closeOut(false)
                .singleSpecSplit(true)
                .minimumStartQuantity(new BigDecimal("300"))
                .equalShareThreshold(new BigDecimal("2000"))
                .vehiclePlanQuantity(new BigDecimal("5305.5384"))
                .craftWidth(new BigDecimal("37.2"))
                .totalToolingCount(10)
                .occupiedVehicleCount(0)
                .shiftCapacity(new BigDecimal("20000"))
                .shiftHours(8)
                .remainingSeconds(28800)
                .previousSpec("CSSC6020")
                .currentSpec("CSSC6020")
                .specChangeMinutes(0)
                .lossRateRules(Collections.singletonList(
                        Cd15LossRateRule.builder()
                                .lossRatePercent(BigDecimal.ZERO)
                                .build()))
                .build();

        Cd15MachineTrial result = calculator.calculate(input);

        assertTrue(result.isEqualShareApplied());
        assertEquals(new BigDecimal("2652.8064"), result.getActualQuantity());
        assertEquals(new BigDecimal("2652.732"),
                result.getEqualShareRemainderQuantity());
    }

    /** 单规格一出二受产能限制时必须向下取完整的双片步长。 */
    @Test
    public void shouldRoundSingleSpecSplitPartialQuantityDownToCompletePair() {
        Cd15CandidateMachineTrialInput input = singleSpecSplitInput("309", "309");
        Cd15MachineTrial result = calculator.calculate(input);
        assertEquals(new BigDecimal("310"), result.getActualQuantity());
        assertEquals(new BigDecimal("308"), result.getFinalSchedulableQuantity());
        assertFalse(result.isFullyAccommodated());
    }

    private Cd15CandidateMachineTrialInput singleSpecSplitInput(
            String demandQuantity, String shiftCapacity) {
        return Cd15CandidateMachineTrialInput.builder()
                .machineCode("M1")
                .netDemandQuantity(new BigDecimal(demandQuantity))
                .closeOut(true)
                .singleSpecSplit(true)
                .minimumStartQuantity(BigDecimal.ONE)
                .equalShareThreshold(new BigDecimal("2000"))
                .vehiclePlanQuantity(new BigDecimal("100"))
                .craftWidth(new BigDecimal("1000"))
                .totalToolingCount(10)
                .occupiedVehicleCount(0)
                .shiftCapacity(new BigDecimal(shiftCapacity))
                .shiftHours(8)
                .remainingSeconds(28800)
                .previousSpec("A")
                .currentSpec("A")
                .specChangeMinutes(0)
                .lossRateRules(Collections.singletonList(
                        Cd15LossRateRule.builder()
                                .lossRatePercent(BigDecimal.ZERO)
                                .build()))
                .build();
    }

    @Test
    public void shouldDelayTrialStartByAgingAllocation() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 13, 8, 0);
        Cd15CandidateMachineTrialInput input = Cd15CandidateMachineTrialInput.builder()
                
                .machineCode("M1")
                .netDemandQuantity(new BigDecimal("120"))
                .closeOut(true)
                .bigRollCode("BR1")
                .minimumStartQuantity(new BigDecimal("10"))
                .equalShareThreshold(new BigDecimal("2000"))
                .vehiclePlanQuantity(new BigDecimal("80"))
                .craftWidth(new BigDecimal("1000"))
                .unitConsumeMillimeter(new BigDecimal("1000"))
                .cordWidth(new BigDecimal("1000"))
                .totalToolingCount(10)
                .occupiedVehicleCount(0)
                .shiftCapacity(new BigDecimal("800"))
                .shiftHours(8)
                .remainingSeconds(28800)
                .originalStartTime(start)
                .bigRollAgingStocks(Arrays.asList(
                        Cd15BigRollAgingStock.builder().bigRollCode("BR1")
                                .availableQuantity(new BigDecimal("80"))
                                .allocatedQuantity(BigDecimal.ZERO)
                                .releaseTime(start).build(),
                        Cd15BigRollAgingStock.builder().bigRollCode("BR1")
                                .availableQuantity(new BigDecimal("80"))
                                .allocatedQuantity(BigDecimal.ZERO)
                                .releaseTime(start.plusHours(3)).build()))
                .previousSpec("A")
                .currentSpec("A")
                .specChangeMinutes(0)
                .lossRateRules(Collections.singletonList(Cd15LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO)
                        .build()))
                .build();

        Cd15MachineTrial result = calculator.calculate(input);

        assertEquals(start.plusHours(3), result.getTaskStartTime());
        assertEquals(10800, result.getAgingDelaySeconds());
        assertEquals(new BigDecimal("120"), result.getFinalSchedulableQuantity());
    }
}
