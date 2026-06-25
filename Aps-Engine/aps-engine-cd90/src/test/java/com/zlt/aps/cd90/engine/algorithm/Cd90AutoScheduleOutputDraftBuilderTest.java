package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleOutputDraft;
import com.zlt.aps.cd90.engine.model.Cd90LaneAllocationDraft;
import com.zlt.aps.cd90.engine.model.Cd90MultiShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleResultDraft;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledResultModel;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 自动排程最终输出草稿归并测试。 */
public class Cd90AutoScheduleOutputDraftBuilderTest {

    private final Cd90AutoScheduleOutputDraftBuilder builder =
            new Cd90AutoScheduleOutputDraftBuilder();

    @Test
    public void shouldMergeSameMaterialMachineAcrossShiftsAndSplitDifferentMachine() {
        Cd90ShiftScheduleTask class1 = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd90ShiftScheduleTask class2 = task("CLASS2", "M1", "60", 1,
                allocation("L1", 1));
        Cd90ShiftScheduleTask anotherMachine = task("CLASS2", "M2", "40", 1,
                allocation("L2", 1));
        Cd90UnscheduledResultModel unscheduled = Cd90UnscheduledResultModel.builder()
                .clothCode("C9").reasonCode("SCHEDULE_WINDOW_LIMIT").build();

        Cd90AutoScheduleOutputDraft result = builder.build(
                context(), execution(class1, class2, anotherMachine, unscheduled));

        assertEquals(2, result.getScheduleResults().size());
        Cd90ScheduleResultDraft m1 = result.getScheduleResults().get(0);
        assertEquals("M1", m1.getMachineCode());
        assertEquals("L1", m1.getPrimaryLaneCode());
        assertEquals(2, m1.getShiftSlots().size());
        assertEquals(new BigDecimal("100"), m1.getShiftSlots().get(0).getPlanQuantity());
        assertEquals(LocalDate.of(2026, 6, 12), m1.getShiftSlots().get(0).getScheduleDate());
        assertEquals(new BigDecimal("60"), m1.getShiftSlots().get(1).getPlanQuantity());
        assertEquals(2, result.getExplainLogs().size());
        assertSame(unscheduled, result.getUnscheduledResults().get(0));
    }

    @Test
    public void shouldMergeTaskSegmentsAndAllocateQuantityByVehicleRatio() {
        Cd90ShiftScheduleTask first = task("CLASS1", "M1", "90", 2,
                allocation("L1", 1), allocation("L2", 1));
        first.setProduceOrder(2);
        first.setExpectedStartTime(LocalDateTime.of(2026, 6, 12, 15, 0));
        first.setExpectedEndTime(LocalDateTime.of(2026, 6, 12, 17, 0));
        Cd90ShiftScheduleTask second = task("CLASS1", "M1", "30", 1,
                allocation("L1", 1));
        second.setProduceOrder(1);
        second.setExpectedStartTime(LocalDateTime.of(2026, 6, 12, 14, 0));
        second.setExpectedEndTime(LocalDateTime.of(2026, 6, 12, 18, 0));

        Cd90AutoScheduleOutputDraft result = builder.build(context(), execution(first, second));

        Cd90ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals(new BigDecimal("120"), draft.getShiftSlots().get(0).getPlanQuantity());
        assertEquals(1, draft.getShiftSlots().get(0).getProduceOrder());
        assertEquals(LocalDateTime.of(2026, 6, 12, 14, 0),
                draft.getShiftSlots().get(0).getExpectedStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 18, 0),
                draft.getShiftSlots().get(0).getExpectedEndTime());
        assertEquals(2, result.getLaneAllocations().size());
        Cd90LaneAllocationDraft l1 = result.getLaneAllocations().get(0);
        assertEquals("L1", l1.getLaneCode());
        assertEquals(new BigDecimal("75.0000000000"), l1.getAllocationQuantity());
        assertEquals(2, l1.getVehicleCount());
        assertEquals(new BigDecimal("45"), result.getLaneAllocations().get(1).getAllocationQuantity());
    }

    @Test
    public void shouldWritePriorFailureReasonsToSuccessfulShiftAnalysis() {
        Cd90ShiftScheduleTask class4 = task("CLASS4", "M1", "609", 7,
                allocation("L1", 7));

        Cd90AutoScheduleOutputDraft result = builder.build(contextWithFourShifts(),
                executionWithTraces(Collections.singletonList(class4),
                        trace("CLASS1", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 1),
                        trace("CLASS2", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 2),
                        trace("CLASS3", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 3),
                        trace("CLASS4", null, new BigDecimal("609"), 4)));

        assertEquals("CLASS1：库排容量不足</br>CLASS2：库排容量不足</br>CLASS3：库排容量不足",
                result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis());
        assertEquals(result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis(),
                result.getExplainLogs().get(0).getShiftDetails().get(0).getAnalysis());
    }

    @Test
    public void shouldWritePartialScheduleReasonToSuccessfulShiftAnalysis() {
        Cd90ShiftScheduleTask class4 = task("CLASS4", "M1", "400", 5,
                allocation("L1", 5));

        Cd90AutoScheduleOutputDraft result = builder.build(contextWithFourShifts(),
                executionWithTraces(Collections.singletonList(class4),
                        trace("CLASS1", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 1),
                        trace("CLASS2", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 2),
                        trace("CLASS3", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 3),
                        trace("CLASS4", null, new BigDecimal("400"), 4)));

        String analysis = result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis();
        assertEquals("CLASS1：库排容量不足</br>CLASS2：库排容量不足</br>CLASS3：库排容量不足</br>"
                        + "CLASS4：库排容量不足，仅部分排400m，剩余200m转后续班次重算",
                analysis);
        assertEquals(analysis, result.getExplainLogs().get(0).getShiftDetails().get(0).getAnalysis());
    }
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectVehicleCountMismatch() {
        Cd90ShiftScheduleTask invalid = task("CLASS1", "M1", "100", 2,
                allocation("L1", 1));

        builder.build(context(), execution(invalid));
    }

    @Test
    public void shouldJoinMultipleLaneCodesAsPrimaryLaneCode() {
        // 单任务跨多个库排时,主表 primaryLaneCode 用逗号拼接去重
        Cd90ShiftScheduleTask task = task("CLASS1", "M1", "500", 5,
                allocation("L1", 2), allocation("L2", 3));

        Cd90AutoScheduleOutputDraft result = builder.build(context(), execution(task));

        Cd90ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals("L1,L2", draft.getPrimaryLaneCode());
    }

    @Test
    public void shouldDeduplicateLaneCodesWhenSameLaneAcrossAllocations() {
        // 同一库排在同一任务的多条分配中出现时,主表 primaryLaneCode 去重
        Cd90ShiftScheduleTask first = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd90ShiftScheduleTask second = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        second.setProduceOrder(2);

        Cd90AutoScheduleOutputDraft result = builder.build(context(), execution(first, second));

        Cd90ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals("L1", draft.getPrimaryLaneCode());
    }

    @Test
    public void shouldMergeLaneCodesAcrossTasksOfSameScheduleResult() {
        Cd90ShiftScheduleTask first = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd90ShiftScheduleTask second = task("CLASS2", "M1", "100", 1,
                allocation("L2", 1));

        Cd90AutoScheduleOutputDraft result = builder.build(context(), execution(first, second));

        Cd90ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals("L1,L2", draft.getPrimaryLaneCode());
    }

    private Cd90AutoScheduleContext context() {
        return Cd90AutoScheduleContext.builder().shifts(Arrays.asList(
                shift("CLASS1", LocalDateTime.of(2026, 6, 12, 14, 0)),
                shift("CLASS2", LocalDateTime.of(2026, 6, 12, 22, 0)))).build();
    }

    private Cd90AutoScheduleContext contextWithFourShifts() {
        return Cd90AutoScheduleContext.builder().shifts(Arrays.asList(
                shift("CLASS1", LocalDateTime.of(2026, 6, 12, 14, 0)),
                shift("CLASS2", LocalDateTime.of(2026, 6, 12, 22, 0)),
                shift("CLASS3", LocalDateTime.of(2026, 6, 13, 6, 0)),
                shift("CLASS4", LocalDateTime.of(2026, 6, 13, 14, 0)))).build();
    }

    private Cd90ShiftDescriptor shift(String classField, LocalDateTime start) {
        return Cd90ShiftDescriptor.builder().classField(classField)
                .shiftCode(classField + "_SHIFT").startTime(start)
                .endTime(start.plusHours(8)).build();
    }

    private Cd90MultiShiftExecutionResult execution(Cd90ShiftScheduleTask... tasks) {
        return Cd90MultiShiftExecutionResult.builder()
                .rollingContext(Cd90RollingScheduleContext.builder()
                        .committedTasks(Arrays.asList(tasks)).build())
                .unscheduledResults(Collections.emptyList()).build();
    }

    private Cd90MultiShiftExecutionResult executionWithTraces(
            java.util.List<Cd90ShiftScheduleTask> tasks,
            Cd90ScheduleAttemptTrace... traces) {
        return Cd90MultiShiftExecutionResult.builder()
                .rollingContext(Cd90RollingScheduleContext.builder()
                        .committedTasks(tasks).build())
                .attemptTraces(Arrays.asList(traces))
                .unscheduledResults(Collections.emptyList()).build();
    }

    private Cd90MultiShiftExecutionResult execution(
            Cd90ShiftScheduleTask first, Cd90ShiftScheduleTask second,
            Cd90ShiftScheduleTask third, Cd90UnscheduledResultModel unscheduled) {
        return Cd90MultiShiftExecutionResult.builder()
                .rollingContext(Cd90RollingScheduleContext.builder()
                        .committedTasks(Arrays.asList(first, second, third)).build())
                .unscheduledResults(Collections.singletonList(unscheduled)).build();
    }

    private Cd90ShiftScheduleTask task(String classField, String machineCode,
                                       String quantity, int vehicleCount,
                                       Cd90StorageLaneAllocation... allocations) {
        return Cd90ShiftScheduleTask.builder().classField(classField)
                .clothCode("C1").bigRollCode("BR1").cordSpec("SPEC1")
                .machineCode(machineCode).planQuantity(new BigDecimal(quantity))
                .vehicleCount(vehicleCount).produceOrder(1)
                .expectedStartTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .expectedEndTime(LocalDateTime.of(2026, 6, 12, 16, 0))
                .laneAllocations(Arrays.asList(allocations)).build();
    }

    private Cd90StorageLaneAllocation allocation(String laneCode, int vehicleCount) {
        return Cd90StorageLaneAllocation.builder()
                .laneCode(laneCode).vehicleCount(vehicleCount).build();
    }

    private Cd90ScheduleAttemptTrace trace(String classField, String reason,
                                           BigDecimal scheduledQuantity, int sequence) {
        return Cd90ScheduleAttemptTrace.builder().classField(classField)
                .shiftCode(classField + "_SHIFT").clothCode("C1").bigRollCode("BR1")
                .netDemandQuantity(new BigDecimal("600"))
                .scheduledQuantity(scheduledQuantity).failureReason(reason)
                .sequence(sequence).build();
    }
}
