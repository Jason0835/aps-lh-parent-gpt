package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90LossRateRule;
import com.zlt.aps.cd90.engine.model.Cd90MachineResource;
import com.zlt.aps.cd90.engine.model.Cd90MachineResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90MachineRollBinding;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90MachineResourceService;
import com.zlt.aps.cd90.engine.service.Cd90ScheduleCandidatePreparationService;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 单班执行器规格失败隔离测试。 */
public class Cd90SingleShiftScheduleExecutorTest {

    @Test
    public void shouldContinueNextCandidateWhenPreviousConstructionIsMissing() {
        Cd90ScheduleCandidatePreparationService candidates = (context, input, classField) ->
                Arrays.asList(candidate("C1"), candidate("C2"));
        Cd90ShiftDemandProvider demandProvider = (context, input, shift, candidate, rolling) ->
                Cd90ShiftDemandDecision.builder().netDemandQuantity(new BigDecimal("100"))
                        .planSurplusQuantity(null).build();
        Cd90MachineResourceService machineService = (factoryCode, start, end) -> machineSnapshot();
        Cd90SingleShiftScheduleExecutor executor = new Cd90SingleShiftScheduleExecutor(
                candidates, demandProvider, machineService, trialPreparation(), committer(),
                new Cd90CloseOutCalculator(), new Cd90ScheduleCandidateSorter());

        Cd90ShiftExecutionResult result = executor.execute(context(), input(), shift(), state(), null);

        assertEquals(1, result.getTasks().size());
        assertEquals("C2", result.getTasks().get(0).getClothCode());
        assertEquals("CONSTRUCTION_MISSING", result.getFailures().get("C1"));
        assertEquals(2, result.getAttemptTraces().size());
        assertEquals(new BigDecimal("100"),
                result.getAttemptTraces().get(0).getNetDemandQuantity());
        assertEquals("CONSTRUCTION_MISSING",
                result.getAttemptTraces().get(0).getFailureReason());
        assertEquals(new BigDecimal("160"),
                result.getAttemptTraces().get(1).getScheduledQuantity());
        assertEquals(2, result.getTasks().get(0).getVehicleCount());
    }

    private Cd90MachineTrialPreparationService trialPreparation() {
        return new Cd90MachineTrialPreparationService(
                new Cd90MachineCandidateResolver(),
                new Cd90CandidateMachineTrialCalculator(
                        new Cd90LossRateResolver(), new Cd90ScheduleQuantityCalculator(),
                        new Cd90ToolingCalculator(), new Cd90MachineCapacityCalculator()),
                new Cd90MachineTrialSelector());
    }

    private Cd90ShiftResourceCommitter committer() {
        return new Cd90ShiftResourceCommitter(
                new Cd90StorageLaneAllocator(), new Cd90MachineTrialSelector());
    }

    private Cd90MachineResourceSnapshot machineSnapshot() {
        return Cd90MachineResourceSnapshot.builder()
                .machines(Collections.singletonList(Cd90MachineResource.builder()
                        .machineCode("M1").status("1").openMachineClass("SHIFT1")
                        .quota(new BigDecimal("800")).build()))
                .bindings(Collections.singletonList(Cd90MachineRollBinding.builder()
                        .machineCode("M1").bigRollCode("BR2").build()))
                .restrictions(Collections.emptyList())
                .lossRateRules(Collections.singletonList(Cd90LossRateRule.builder()
                        .lossRatePercent(BigDecimal.ZERO).build()))
                .build();
    }

    private Cd90AutoScheduleInput input() {
        return Cd90AutoScheduleInput.builder()
                .constructionMaterials(Collections.singletonList(Cd90ConstructionMaterial.builder()
                        .clothCode("C2").bigRollCode("BR2").cordSpec("SPEC2")
                        .unitConsumeMillimeter(BigDecimal.ONE)
                        .curlLength(new BigDecimal("80")).build()))
                .build();
    }

    private Cd90ShiftResourceState state() {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("M1", 28800);
        return Cd90ShiftResourceState.builder()
                .lanes(Collections.singletonList(Cd90StorageLaneState.builder()
                        .laneCode("L1").clothCode("C2").vehicleCount(0).maxVehicleCount(10).build()))
                .totalToolingCount(10).occupiedToolingCount(0)
                .remainingSecondsByMachine(seconds).tailSpecByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }

    private Cd90AutoScheduleContext context() {
        return Cd90AutoScheduleContext.builder().factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd90AutoScheduleParameters.builder()
                        .demandWindow(4).minStartQty(new BigDecimal("100"))
                        .rollCoilMeter(new BigDecimal("100")).rollTotalCount(10)
                        .machinePriority(Collections.singletonList("M1"))
                        .specChangeMinutes(10).build()).build();
    }

    private Cd90ShiftDescriptor shift() {
        return Cd90ShiftDescriptor.builder().shiftCode("SHIFT1").classField("CLASS1")
                .startTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .endTime(LocalDateTime.of(2026, 6, 12, 22, 0))
                .durationSeconds(28800).build();
    }

    private Cd90ScheduleCandidate candidate(String clothCode) {
        return Cd90ScheduleCandidate.builder().clothCode(clothCode).build();
    }
}
