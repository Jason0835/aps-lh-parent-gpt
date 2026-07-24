package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrial;
import com.zlt.aps.cd15.engine.model.Cd15MachineTrialPlan;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitRequest;
import com.zlt.aps.cd15.engine.model.Cd15ShiftCommitResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 单班资源原子提交和候选机台回退测试。
 */
public class Cd15ShiftResourceCommitterTest {

    private final Cd15ShiftResourceCommitter committer =
            new Cd15ShiftResourceCommitter(new Cd15StorageLaneAllocator(), new Cd15MachineTrialSelector(),
                    new Cd15BigRollAgingAllocator(), new Cd15BigRollMeterCalculator());

    @Test
    public void shouldCommitLaneToolingMachineAndTaskChainAtomically() {
        Cd15ShiftResourceState state = state(3);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
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
        Cd15ShiftResourceState state = state(7);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "800", 20000, true))), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("700"), result.getTask().getPlanQuantity());
        assertEquals(7, result.getTask().getVehicleCount());
        assertEquals(7, result.getTask().getLaneAllocations().get(0).getVehicleCount());
        assertEquals(7, result.getState().getOccupiedToolingCount());
        assertEquals("STORAGE_LANE_LIMIT", result.getPartialReason());
    }
    @Test
    public void shouldRoundCommittedQuantityUpToIntegerMeters() {
        Cd15ShiftResourceState state = state(4);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "261.8055555555", 20000, true))), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("262"), result.getTask().getPlanQuantity());
        assertEquals(3, result.getTask().getVehicleCount());
    }

    @Test
    public void shouldNotRoundBeyondSameSteelStripRemainingShiftQuantity() {
        Cd15ShiftResourceState state = state(4);
        Cd15MachineTrial trial = trial(
                "M1", "261.8055555555", 20000, true);
        trial.setRemainingSpecShiftQuantity(
                new BigDecimal("261.8055555555"));

        Cd15ShiftCommitResult result = committer.commit(
                request(plan(trial)), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("261.8055555555"),
                result.getTask().getPlanQuantity());
    }
    @Test
    public void shouldKeepToolingLimitWhenLaneAllocationIsComplete() {
        Cd15ShiftResourceState state = state(10);
        Cd15MachineTrial trial = trial("M1", "500", 20000, true);
        trial.setLimitReason("TOOLING_LIMIT");

        Cd15ShiftCommitResult result = committer.commit(request(plan(trial)), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("500"), result.getTask().getPlanQuantity());
        assertEquals("TOOLING_LIMIT", result.getPartialReason());
    }

    @Test
    public void shouldCommitPartialLaneAllocationWhenAssignedVehiclesReachParamThreshold() {
        Cd15ShiftResourceState state = state(3);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "5500", 20000, true)), 3), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("300"), result.getTask().getPlanQuantity());
        assertEquals(3, result.getTask().getVehicleCount());
        assertEquals(3, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldRejectTinyInitialPartialLaneAllocation() {
        Cd15ShiftResourceState state = state(1);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "640", 20000, true))), state);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(0, result.getState().getTasks().size());
        assertEquals(0, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldFallbackToNextTrialWhenFirstTinyPartialLaneCapacity() {
        Cd15ShiftResourceState state = state(1);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "300", 25000, true), trial("M2", "100", 26000, false))), state);

        assertTrue(result.isSuccess());
        assertEquals("M2", result.getTask().getMachineCode());
        assertEquals(new BigDecimal("100"), result.getTask().getPlanQuantity());
    }

    @Test
    public void allTrialsFailedShouldKeepOriginalStateAndReturnStableReason() {
        Cd15ShiftResourceState state = state(0);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                trial("M1", "100", 25000, true))), state);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(0, state.getTasks().size());
        assertEquals(0, state.getOccupiedToolingCount());
    }

    @Test
    public void zeroQuantityTrialShouldReturnLimitReasonWithoutOverwrite() {
        Cd15ShiftResourceState state = state(10);

        Cd15ShiftCommitResult result = committer.commit(request(plan(
                limitedTrial("M1", "CAPACITY_LIMIT", 0),
                limitedTrial("M2", "TOOLING_LIMIT", 1))), state);

        assertFalse(result.isSuccess());
        assertEquals("TOOLING_LIMIT", result.getFailureReason());
        assertEquals(0, result.getState().getTasks().size());
        assertEquals(0, result.getState().getOccupiedToolingCount());
    }

    @Test
    public void shouldNotCountAgingDelayTwiceInTaskEndTime() {
        Cd15ShiftResourceState state = state(2);
        state.setBigRollAgingStocks(Collections.singletonList(
                agingStock("BR001", LocalDateTime.of(2026, 6, 12, 15, 0), "100")));
        Cd15MachineTrial trial = Cd15MachineTrial.builder().machineCode("M1")
                .finalSchedulableQuantity(new BigDecimal("100"))
                .vehiclePlanQuantity(new BigDecimal("100"))
                .fullyAccommodated(true).preferredMachine(true).priorityOrder(0)
                .changeSeconds(0).productionSeconds(3600).agingDelaySeconds(3600)
                .remainingSeconds(21600).build();

        Cd15ShiftCommitResult result = committer.commit(request(plan(trial)), state);

        assertTrue(result.isSuccess());
        assertEquals(LocalDateTime.of(2026, 6, 12, 15, 0), result.getTask().getExpectedStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 16, 0), result.getTask().getExpectedEndTime());
        assertEquals(Integer.valueOf(21600), result.getState().getRemainingSecondsByMachine().get("M1"));
    }

    @Test
    public void shouldDeductAgingDelayWhenPartialQuantityIsCommitted() {
        Cd15ShiftResourceState state = state(3);
        state.setBigRollAgingStocks(Collections.singletonList(
                agingStock("BR001", LocalDateTime.of(2026, 6, 12, 15, 0), "300")));
        Cd15MachineTrial trial = Cd15MachineTrial.builder().machineCode("M1")
                .finalSchedulableQuantity(new BigDecimal("500"))
                .vehiclePlanQuantity(new BigDecimal("100"))
                .fullyAccommodated(true).preferredMachine(true).priorityOrder(0)
                .changeSeconds(0).productionSeconds(5000).agingDelaySeconds(3600)
                .remainingSeconds(20200).build();

        Cd15ShiftCommitResult result = committer.commit(request(plan(trial), 3), state);

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("300"), result.getTask().getPlanQuantity());
        assertEquals(LocalDateTime.of(2026, 6, 12, 15, 50), result.getTask().getExpectedEndTime());
        assertEquals(Integer.valueOf(22200), result.getState().getRemainingSecondsByMachine().get("M1"));
    }

    @Test
    public void shouldAllocateVehiclesBySteelStripCurlLength() {
        Cd15ShiftResourceState state = state(2);
        Cd15MachineTrial trial = trial("M1", "303.8", 20000, true);
        trial.setVehiclePlanQuantity(new BigDecimal("181.3"));

        Cd15ShiftCommitResult result = committer.commit(request(plan(trial)), state);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTask().getVehicleCount());
        assertEquals(new BigDecimal("304"), result.getTask().getPlanQuantity());
    }

    @Test
    public void shouldCommitSplitPairWithSharedOrderAndCapacityOnce() {
        Cd15ShiftResourceState state = splitState(2, 2);

        com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult result =
                committer.commitSplit(
                        splitRequest("C1", trial("M1", "200", 20000, true)),
                        splitRequest("C2", trial("M1", "200", 20000, true)),
                        state);

        assertTrue(result.isSuccess());
        assertEquals(result.getFirstTask().getProduceOrder(),
                result.getSecondTask().getProduceOrder());
        assertEquals(result.getFirstTask().getMachineCode(),
                result.getSecondTask().getMachineCode());
        assertEquals(Integer.valueOf(20000),
                result.getState().getRemainingSecondsByMachine().get("M1"));
        assertEquals(4, result.getState().getOccupiedToolingCount());
        assertEquals(2, result.getState().getTasks().size());
        assertEquals("L1", result.getFirstTask()
                .getLaneAllocations().get(0).getLaneCode());
        assertEquals("L2", result.getSecondTask()
                .getLaneAllocations().get(0).getLaneCode());
        assertEquals(0, state.getOccupiedToolingCount());
        assertTrue(state.getTasks().isEmpty());
    }

    @Test
    public void shouldKeepOriginalStateWhenSecondSplitLaneFails() {
        Cd15ShiftResourceState state = splitState(2);

        com.zlt.aps.cd15.engine.model.Cd15SplitShiftCommitResult result =
                committer.commitSplit(
                        splitRequest("C1", trial("M1", "200", 20000, true)),
                        splitRequest("C2", trial("M1", "200", 20000, true)),
                        state);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertSame(state, result.getState());
        assertEquals(0, state.getLanes().get(0).getVehicleCount());
        assertEquals(null, state.getLanes().get(0).getSteelStripCode());
        assertTrue(state.getTasks().isEmpty());
    }

    /** 单规格一出二只生成一条任务，但必须按两路同时占用小车和工装。 */
    @Test
    public void shouldCommitSingleSpecSplitAsOneTaskWithTwoReceivingStreams() {
        Cd15ShiftResourceState state = splitState(2);
        Cd15ShiftCommitResult result = committer.commitSingleSpecSplit(
                splitRequest("C1", trial("M1", "200", 20000, true)), state);
        assertTrue(result.isSuccess());
        assertEquals("SPLIT", result.getTask().getCutMode());
        assertEquals(new BigDecimal("200"), result.getTask().getPlanQuantity());
        assertEquals(2, result.getTask().getVehicleCount());
        assertEquals(2, result.getState().getOccupiedToolingCount());
        assertEquals(1, result.getState().getTasks().size());
        assertEquals(new BigDecimal("200"),
                result.getTask().getBigRollConsumeQuantity());
        assertEquals(0, state.getOccupiedToolingCount());
        assertTrue(state.getTasks().isEmpty());
    }

    /** 收尾分裁量不足整车时，两路仍各占一辆小车和一个工装。 */
    @Test
    public void closeOutSingleSpecSplitShouldKeepActualQuantityAndUseTwoVehicles() {
        Cd15MachineTrial trial = trial("M1", "303.8", 20000, true);
        trial.setVehiclePlanQuantity(new BigDecimal("181.3"));
        Cd15ShiftCommitRequest request = splitRequest("211500013", trial);
        request.setCloseOut(true);

        Cd15ShiftCommitResult result =
                committer.commitSingleSpecSplit(request, splitState(2));

        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("303.8"), result.getTask().getPlanQuantity());
        assertEquals(2, result.getTask().getVehicleCount());
    }

    /** 单规格分裁均分提交后必须把精确余量交给班次执行器。 */
    @Test
    public void shouldReturnExactRemainderAfterSingleSpecSplitEqualShare() {
        Cd15MachineTrial trial = Cd15MachineTrial.builder()
                .machineCode("M1")
                .actualQuantity(new BigDecimal("2652.8064"))
                .finalSchedulableQuantity(new BigDecimal("2652.8064"))
                .vehiclePlanQuantity(new BigDecimal("5305.5384"))
                .equalShareApplied(true)
                .equalShareRemainderQuantity(new BigDecimal("2652.732"))
                .fullyAccommodated(true)
                .preferredMachine(true)
                .priorityOrder(0)
                .changeSeconds(0)
                .productionSeconds(1000)
                .remainingSeconds(27800)
                .build();

        Cd15ShiftCommitResult result = committer.commitSingleSpecSplit(
                splitRequest("211500012", trial), splitState(2));

        assertTrue(result.isSuccess());
        assertEquals("EQUAL_SHARE", result.getPartialReason());
        assertEquals(new BigDecimal("2652.732"),
                result.getEqualShareRemainderQuantity());
    }

    /** 单规格一出二第二路无法分配时必须丢弃工作副本。 */
    @Test
    public void shouldKeepOriginalStateWhenSecondSingleSpecSplitLaneFails() {
        Cd15ShiftResourceState state = splitState(1);
        Cd15ShiftCommitResult result = committer.commitSingleSpecSplit(
                splitRequest("C1", trial("M1", "200", 20000, true)), state);
        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertSame(state, result.getState());
        assertEquals(0, state.getLanes().get(0).getVehicleCount());
        assertEquals(0, state.getOccupiedToolingCount());
        assertTrue(state.getTasks().isEmpty());
    }

    private Cd15ShiftCommitRequest request(Cd15MachineTrialPlan plan) {
        return request(plan, 4);
    }

    private Cd15ShiftCommitRequest request(Cd15MachineTrialPlan plan, int partialMinVehicleCount) {
        return Cd15ShiftCommitRequest.builder()
                .materialKey("C1|BR001|15|100|100|100|false")
                .steelStripCode("C1").bigRollCode("BR001").cuttingAngle("15")
                .cordSpec("BR001").classField("CLASS1")
                .craftWidth(new BigDecimal("100"))
                .unitConsumeMillimeter(new BigDecimal("100"))
                .cordWidth(new BigDecimal("100"))
                .shiftStart(LocalDateTime.of(2026, 6, 12, 14, 0))
                .shiftEnd(LocalDateTime.of(2026, 6, 12, 22, 0))
                .closeOut(false)
                .partialMinVehicleCount(partialMinVehicleCount).trialPlan(plan).build();
    }

    private Cd15ShiftCommitRequest splitRequest(
            String steelStripCode, Cd15MachineTrial machineTrial) {
        return Cd15ShiftCommitRequest.builder()
                .materialKey(steelStripCode + "|BR001|15|100|100|100|false")
                .steelStripCode(steelStripCode).bigRollCode("BR001")
                .cuttingAngle("15").cordSpec("BR001").classField("CLASS1")
                .craftWidth(new BigDecimal("100"))
                .unitConsumeMillimeter(new BigDecimal("100"))
                .cordWidth(new BigDecimal("100"))
                .shiftStart(LocalDateTime.of(2026, 6, 12, 14, 0))
                .shiftEnd(LocalDateTime.of(2026, 6, 12, 22, 0))
                .cutMode("SPLIT").splitGroupKey("GROUP-1")
                .partialMinVehicleCount(2).trialPlan(plan(machineTrial)).build();
    }

    private Cd15MachineTrialPlan plan(Cd15MachineTrial... trials) {
        return Cd15MachineTrialPlan.builder().trials(Arrays.asList(trials))
                .selectedTrial(trials[0]).build();
    }

    private Cd15MachineTrial limitedTrial(String machineCode, String limitReason, int priorityOrder) {
        return Cd15MachineTrial.builder().machineCode(machineCode)
                .finalSchedulableQuantity(BigDecimal.ZERO)
                .priorityOrder(priorityOrder).remainingSeconds(0)
                .limitReason(limitReason).build();
    }

    private Cd15MachineTrial trial(String machineCode, String quantity,
                                   int remainingSeconds, boolean preferred) {
        return Cd15MachineTrial.builder().machineCode(machineCode)
                .finalSchedulableQuantity(new BigDecimal(quantity))
                .vehiclePlanQuantity(new BigDecimal("100"))
                .fullyAccommodated(true).preferredMachine(preferred)
                .priorityOrder(preferred ? 0 : 1).sameTailSpec(false)
                .changeSeconds(0).productionSeconds(28800 - remainingSeconds)
                .remainingSeconds(remainingSeconds).build();
    }

    private Cd15ShiftResourceState state(int laneCapacity) {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("M1", 28800);
        seconds.put("M2", 28800);
        return Cd15ShiftResourceState.builder()
                .lanes(Collections.singletonList(Cd15StorageLaneState.builder()
                        .laneCode("L1").vehicleCount(0).maxVehicleCount(laneCapacity).build()))
                .occupiedToolingCount(0).totalToolingCount(10)
                .remainingSecondsByMachine(seconds).tailSpecByMachine(new HashMap<>())
                .tasks(new java.util.ArrayList<>()).build();
    }

    private Cd15ShiftResourceState splitState(int... laneCapacities) {
        HashMap<String, Integer> seconds = new HashMap<>();
        seconds.put("M1", 28800);
        List<Cd15StorageLaneState> lanes = new ArrayList<>();
        for (int index = 0; index < laneCapacities.length; index++) {
            lanes.add(Cd15StorageLaneState.builder()
                    .laneCode("L" + (index + 1)).vehicleCount(0)
                    .maxVehicleCount(laneCapacities[index]).build());
        }
        return Cd15ShiftResourceState.builder().lanes(lanes)
                .occupiedToolingCount(0).totalToolingCount(10)
                .remainingSecondsByMachine(seconds)
                .tailSpecByMachine(new HashMap<>())
                .tailByMachine(new HashMap<>())
                .tasks(new ArrayList<>()).build();
    }

    private Cd15BigRollAgingStock agingStock(String bigRollCode, LocalDateTime releaseTime,
                                             String quantity) {
        return Cd15BigRollAgingStock.builder()
                .sourceType("ACTUAL_STOCK").sourceId("STOCK-1")
                .bigRollCode(bigRollCode)
                .availableQuantity(new BigDecimal(quantity)).allocatedQuantity(BigDecimal.ZERO)
                .stockInTime(releaseTime.minusHours(24)).releaseTime(releaseTime).build();
    }
}
