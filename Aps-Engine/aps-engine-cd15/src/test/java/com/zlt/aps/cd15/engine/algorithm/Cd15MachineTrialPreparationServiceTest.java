package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15LossRateRule;
import com.zlt.aps.cd15.engine.model.Cd15MachineResource;
import com.zlt.aps.cd15.engine.model.Cd15MachineResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15MachineRestriction;
import com.zlt.aps.cd15.engine.model.Cd15MachineRollBinding;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialPlan;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialRequest;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * 规格候选机台试算编排测试。
 */
public class Cd15MachineTrialPreparationServiceTest {

    private final Cd15MachineTrialPreparationService service =
            new Cd15MachineTrialPreparationService(
                    new Cd15MachineCandidateResolver(),
                    new Cd15CandidateMachineTrialCalculator(
                            new Cd15LossRateResolver(),
                            new Cd15ScheduleQuantityCalculator(),
                            new Cd15ToolingCalculator(),
                            new Cd15MachineCapacityCalculator(), new Cd15BigRollAgingAllocator()),
                    new Cd15MachineTrialSelector(),
                    new Cd15VehiclePlanQuantityCalculator());

    @Test
    public void shouldBuildTrialsAndSelectPreferredMachine() {
        Cd15MachineTrialPlan result = service.prepare(request(), snapshot());

        assertEquals(2, result.getTrials().size());
        assertEquals("M2", result.getSelectedTrial().getMachineCode());
        assertEquals("STEEL_STRIP_MACHINE", result.getSelectedTrial().getLossRateLevel());
        assertEquals(new BigDecimal("160"), result.getSelectedTrial().getFinalSchedulableQuantity());
    }

    @Test
    public void shouldOnlyUseMachineSupportingSplitCutForOrdinarySplit() {
        Cd15MachineTrialRequest request = request();
        request.setSplitCut(true);
        Cd15MachineResourceSnapshot snapshot = snapshot();
        snapshot.getMachines().get(1).setSplitCutSupported(true);

        Cd15MachineTrialPlan result = service.prepare(request, snapshot);

        assertEquals(1, result.getTrials().size());
        assertEquals("M2", result.getSelectedTrial().getMachineCode());
    }

    @Test
    public void shouldOnlyUseG1201ForReinforcementFixedSplit() {
        Cd15MachineTrialRequest request = request();
        request.setSplitCut(true);
        request.setReinforcement(true);

        Cd15MachineTrialPlan result = service.prepare(request, modeSnapshot());

        assertEquals(1, result.getTrials().size());
        assertEquals("G1201", result.getSelectedTrial().getMachineCode());
    }

    @Test
    public void shouldExcludeG1201FromOrdinarySingleCut() {
        Cd15MachineTrialPlan result = service.prepare(request(), modeSnapshot());

        assertEquals(1, result.getTrials().size());
        assertEquals("G1401", result.getSelectedTrial().getMachineCode());
    }

    private Cd15MachineTrialRequest request() {
        HashMap<String, Integer> remainingSeconds = new HashMap<>();
        remainingSeconds.put("M1", 28800);
        remainingSeconds.put("M2", 28800);
        return Cd15MachineTrialRequest.builder()
                .materialKey("C1|BR001|15|1000|1000|80|false")
                .steelStripCode("C1").bigRollCode("BR001")
                .cuttingAngle("15").cordSpec("BR001")
                .shiftCode("NIGHT")
                .shiftStart(LocalDateTime.of(2026, 6, 12, 22, 0))
                .shiftEnd(LocalDateTime.of(2026, 6, 13, 6, 0))
                .curlLength(new BigDecimal("80"))
                .unitConsumeMillimeter(new BigDecimal("1000"))
                .craftWidth(new BigDecimal("1000"))
                .netDemandQuantity(new BigDecimal("100"))
                .shiftHours(8).remainingSecondsByMachine(remainingSeconds)
                .previousSpecByMachine(Collections.emptyMap())
                .parameters(Cd15AutoScheduleParameters.builder()
                        .minStartQty(new BigDecimal("10"))
                        .equalShareThreshold(new BigDecimal("2000"))
                        .rollCoilMeter(new BigDecimal("100"))
                        .rollTotalCount(10).specChangeMinutes(10)
                        .machinePriority(Arrays.asList("M1", "M2")).build())
                .build();
    }

    private Cd15MachineResourceSnapshot snapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Arrays.asList(machine("M1"), machine("M2")))
                .bindings(Arrays.asList(binding("M1"), binding("M2")))
                .restrictions(Collections.singletonList(Cd15MachineRestriction.builder()
                        .steelStripCode("C1").machineCode("M2").jobType("0").build()))
                .lossRateRules(Arrays.asList(
                        Cd15LossRateRule.builder().steelStripCode("C1").machineCode("M2")
                                .lossRatePercent(new BigDecimal("2")).build(),
                        Cd15LossRateRule.builder().lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1200")))
                .build();
    }

    private Cd15MachineResourceSnapshot modeSnapshot() {
        return Cd15MachineResourceSnapshot.builder()
                .machines(Arrays.asList(
                        modeMachine("G1201"), modeMachine("G1401")))
                .bindings(Arrays.asList(
                        binding("G1201"), binding("G1401")))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(
                        Cd15LossRateRule.builder()
                                .lossRatePercent(BigDecimal.ZERO).build()))
                .angleWidthMaxByAngle(Collections.singletonMap(
                        "15", new BigDecimal("1200")))
                .build();
    }

    private Cd15MachineResource modeMachine(String code) {
        return Cd15MachineResource.builder().machineCode(code).status("1")
                .openMachineClass("NIGHT").splitCutSupported(true)
                .quota(new BigDecimal("1000")).build();
    }

    private Cd15MachineResource machine(String code) {
        return Cd15MachineResource.builder().machineCode(code).status("1")
                .openMachineClass("NIGHT").quota(new BigDecimal("1000")).build();
    }

    private Cd15MachineRollBinding binding(String machineCode) {
        return Cd15MachineRollBinding.builder().bigRollCode("BR001")
                .machineCode(machineCode).shiftCode("NIGHT").build();
    }
}
