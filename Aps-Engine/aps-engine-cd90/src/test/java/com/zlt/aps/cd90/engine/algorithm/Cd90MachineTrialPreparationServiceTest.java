package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90MachineRestriction;
import com.zlt.aps.cd90.engine.model.Cd90MachineRollBinding;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialPlan;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialRequest;
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
public class Cd90MachineTrialPreparationServiceTest {

    private final Cd90MachineTrialPreparationService service =
            new Cd90MachineTrialPreparationService(
                    new Cd90MachineCandidateResolver(),
                    new Cd90CandidateMachineTrialCalculator(
                            new Cd90LossRateResolver(),
                            new Cd90ScheduleQuantityCalculator(),
                            new Cd90ToolingCalculator(),
                            new Cd90MachineCapacityCalculator()),
                    new Cd90MachineTrialSelector());

    @Test
    public void shouldBuildTrialsAndSelectPreferredMachine() {
        Cd90MachineTrialPlan result = service.prepare(request(), snapshot());

        assertEquals(2, result.getTrials().size());
        assertEquals("M2", result.getSelectedTrial().getMachineCode());
        assertEquals("CLOTH_MACHINE", result.getSelectedTrial().getLossRateLevel());
        assertEquals(new BigDecimal("160"), result.getSelectedTrial().getFinalSchedulableQuantity());
    }

    private Cd90MachineTrialRequest request() {
        HashMap<String, Integer> remainingSeconds = new HashMap<>();
        remainingSeconds.put("M1", 28800);
        remainingSeconds.put("M2", 28800);
        return Cd90MachineTrialRequest.builder()
                .clothCode("CF001").bigRollCode("BR001").cordSpec("BR001")
                .shiftCode("NIGHT")
                .shiftStart(LocalDateTime.of(2026, 6, 12, 22, 0))
                .shiftEnd(LocalDateTime.of(2026, 6, 13, 6, 0))
                .curlLength(new BigDecimal("80"))
                .netDemandQuantity(new BigDecimal("100"))
                .shiftHours(8).remainingSecondsByMachine(remainingSeconds)
                .previousSpecByMachine(Collections.emptyMap())
                .parameters(Cd90AutoScheduleParameters.builder()
                        .minStartQty(new BigDecimal("10"))
                        .rollCoilMeter(new BigDecimal("100"))
                        .rollTotalCount(10).specChangeMinutes(10)
                        .machinePriority(Arrays.asList("M1", "M2")).build())
                .build();
    }

    private Cd90MachineResourceSnapshot snapshot() {
        return Cd90MachineResourceSnapshot.builder()
                .machines(Arrays.asList(machine("M1"), machine("M2")))
                .bindings(Arrays.asList(binding("M1"), binding("M2")))
                .restrictions(Collections.singletonList(Cd90MachineRestriction.builder()
                        .clothCode("CF001").machineCode("M2").jobType("0").build()))
                .lossRateRules(Arrays.asList(
                        Cd90LossRateRule.builder().clothCode("CF001").machineCode("M2")
                                .lossRatePercent(new BigDecimal("2")).build(),
                        Cd90LossRateRule.builder().lossRatePercent(BigDecimal.ZERO).build()))
                .build();
    }

    private Cd90MachineResource machine(String code) {
        return Cd90MachineResource.builder().machineCode(code).status("1")
                .openMachineClass("NIGHT").quota(new BigDecimal("1000")).build();
    }

    private Cd90MachineRollBinding binding(String machineCode) {
        return Cd90MachineRollBinding.builder().bigRollCode("BR001")
                .clothCode("CF001").machineCode(machineCode).build();
    }
}
