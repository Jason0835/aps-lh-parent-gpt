package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90MachineTrial;
import com.zlt.aps.cd90.engine.model.Cd90MachineTrialPlan;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitRequest;
import com.zlt.aps.cd90.engine.model.Cd90ShiftCommitResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 单班资源原子提交和候选机台回退测试。
 */
public class Cd90ShiftResourceCommitterTest {

    private final Cd90ShiftResourceCommitter committer =
            new Cd90ShiftResourceCommitter(new Cd90StorageLaneAllocator(), new Cd90MachineTrialSelector());

    @Test
    public void shouldCommitLaneToolingMachineAndTaskChainAtomically() {
        Cd90ShiftResourceState state = state(3);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "200", 25000, true), trial("M2", "100", 26000, false))), state);

        assertTrue(result.isSuccess());
        assertEquals("M1", result.getTask().getMachineCode());
        assertEquals(2, result.getTask().getVehicleCount());
        assertEquals(2, result.getState().getOccupiedToolingCount());
        assertEquals(Integer.valueOf(25000), result.getState().getRemainingSecondsByMachine().get("M1"));
        assertEquals("BR001", result.getState().getTailSpecByMachine().get("M1"));
        assertEquals(1, result.getTask().getProduceOrder());
        assertEquals(1, result.getState().getTasks().size());
        assertEquals(0, state.getOccupiedToolingCount());
        assertEquals(0, state.getTasks().size());
    }

    @Test
    public void shouldCommitLargePartialLaneAllocationByActualVehicles() {
        Cd90ShiftResourceState state = state(7);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "640", 20000, true))), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("560"), result.getTask().getPlanQuantity());
        assertEquals(7, result.getTask().getVehicleCount());
        assertEquals(7, result.getTask().getLaneAllocations().get(0).getVehicleCount());
        assertEquals(7, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldCommitPartialLaneAllocationWhenAssignedVehiclesReachParamThreshold() {
        Cd90ShiftResourceState state = state(3);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "5500", 20000, true)), 3), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("300"), result.getTask().getPlanQuantity());
        assertEquals(3, result.getTask().getVehicleCount());
        assertEquals(3, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldRejectTinyInitialPartialLaneAllocation() {
        Cd90ShiftResourceState state = state(1);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "640", 20000, true))), state);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(0, result.getState().getTasks().size());
        assertEquals(0, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldFallbackToNextTrialWhenFirstTinyPartialLaneCapacity() {
        Cd90ShiftResourceState state = state(1);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "300", 25000, true), trial("M2", "100", 26000, false))), state);

        assertTrue(result.isSuccess());
        assertEquals("M2", result.getTask().getMachineCode());
        assertEquals(new BigDecimal("100"), result.getTask().getPlanQuantity());
    }

    @Test
    public void allTrialsFailedShouldKeepOriginalStateAndReturnStableReason() {
        Cd90ShiftResourceState state = state(0);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "100", 25000, true))), state);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(0, state.getTasks().size());
        assertEquals(0, state.getOccupiedToolingCount());
    }

    @Test
    public void zeroQuantityTrialShouldReturnLimitReasonWithoutOverwrite() {
        Cd90ShiftResourceState state = state(10);

        Cd90ShiftCommitResult result = committer.commit(request(plan(
                limitedTrial("M1", "CAPACITY_LIMIT", 0),
                limitedTrial("M2", "TOOLING_LIMIT", 1))), state);

        assertFalse(result.isSuccess());
        assertEquals("TOOLING_LIMIT", result.getFailureReason());
        assertEquals(0, result.getState().getTasks().size());
        assertEquals(0, result.getState().getOccupiedToolingCount());
    }

    private Cd90ShiftCommitRequest request(Cd90MachineTrialPlan plan) {
        return request(plan, 4);
    }

    private Cd90ShiftCommitRequest request(Cd90MachineTrialPlan plan, int partialMinVehicleCount) {
        return Cd90ShiftCommitRequest.builder().clothCode("CF001").bigRollCode("BR001")
                .cordSpec("BR001").classField("CLASS1")
                .shiftStart(LocalDateTime.of(2026, 6, 12, 14, 0))
                .shiftEnd(LocalDateTime.of(2026, 6, 12, 22, 0))
                .coilMeter(new BigDecimal("100")).closeOut(false)
                .partialMinVehicleCount(partialMinVehicleCount).trialPlan(plan).build();
    }

    private Cd90MachineTrialPlan plan(Cd90MachineTrial... trials) {
        return Cd90MachineTrialPlan.builder().trials(Arrays.asList(trials))
                .selectedTrial(trials[0]).build();
    }

    private Cd90MachineTrial limitedTrial(String machineCode, String limitReason, int priorityOrder) {
        return Cd90MachineTrial.builder().machineCode(machineCode)
                .finalSchedulableQuantity(BigDecimal.ZERO)
                .priorityOrder(priorityOrder).remainingSeconds(0)
                .limitReason(limitReason).build();
    }

    private Cd90MachineTrial trial(String machineCode, String quantity,
                                   int remainingSeconds, boolean preferred) {
        return Cd90MachineTrial.builder().machineCode(machineCode)
                .finalSchedulableQuantity(new BigDecimal(quantity))
                .fullyAccommodated(true).preferredMachine(preferred)
                .priorityOrder(preferred ? 0 : 1).sameTailSpec(false)
                .changeSeconds(0).productionSeconds(28800 - remainingSeconds)
                .remainingSeconds(remainingSeconds).build();
    }

    private Cd90ShiftResourceState state(int laneCapacity) {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("M1", 28800);
        seconds.put("M2", 28800);
        return Cd90ShiftResourceState.builder()
                .lanes(Collections.singletonList(Cd90StorageLaneState.builder()
                        .laneCode("L1").clothCode("CF001").vehicleCount(0).maxVehicleCount(laneCapacity).build()))
                .occupiedToolingCount(0).totalToolingCount(10)
                .remainingSecondsByMachine(seconds).tailSpecByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }
}