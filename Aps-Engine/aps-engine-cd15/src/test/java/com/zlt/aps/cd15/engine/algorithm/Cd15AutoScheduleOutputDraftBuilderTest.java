package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleOutputDraft;
import com.zlt.aps.cd15.engine.model.Cd15LaneAllocationDraft;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledResultModel;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 自动排程最终输出草稿归并测试。 */
public class Cd15AutoScheduleOutputDraftBuilderTest {

    private final Cd15AutoScheduleOutputDraftBuilder builder =
            new Cd15AutoScheduleOutputDraftBuilder();

    @Test
    public void shouldMergeSameMaterialMachineAcrossShiftsAndSplitDifferentMachine() {
        Cd15ShiftScheduleTask class1 = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd15ShiftScheduleTask class2 = task("CLASS2", "M1", "60", 1,
                allocation("L1", 1));
        Cd15ShiftScheduleTask anotherMachine = task("CLASS2", "M2", "40", 1,
                allocation("L2", 1));
        Cd15UnscheduledResultModel unscheduled = Cd15UnscheduledResultModel.builder()
                .steelStripCode("C9").reasonCode("SCHEDULE_WINDOW_LIMIT").build();

        Cd15AutoScheduleOutputDraft result = builder.build(
                context(), execution(class1, class2, anotherMachine, unscheduled));

        assertEquals(2, result.getScheduleResults().size());
        Cd15ScheduleResultDraft m1 = result.getScheduleResults().get(0);
        assertEquals("M1", m1.getMachineCode());
        assertEquals("L1", m1.getPrimaryLaneCode());
        assertEquals(2, m1.getShiftSlots().size());
        assertEquals(new BigDecimal("100"), m1.getShiftSlots().get(0).getPlanQuantity());
        assertEquals(LocalDate.of(2026, 6, 12), m1.getShiftSlots().get(0).getScheduleDate());
        assertEquals(new BigDecimal("60"), m1.getShiftSlots().get(1).getPlanQuantity());
        assertEquals(LocalDate.of(2026, 6, 13), m1.getShiftSlots().get(1).getScheduleDate());
        assertEquals(2, result.getExplainLogs().size());
        assertSame(unscheduled, result.getUnscheduledResults().get(0));
    }

    @Test
    public void shouldMergeTaskSegmentsAndAllocateQuantityByVehicleRatio() {
        Cd15ShiftScheduleTask first = task("CLASS1", "M1", "90", 2,
                allocation("L1", 1), allocation("L2", 1));
        first.setProduceOrder(2);
        first.setExpectedStartTime(LocalDateTime.of(2026, 6, 12, 15, 0));
        first.setExpectedEndTime(LocalDateTime.of(2026, 6, 12, 17, 0));
        Cd15ShiftScheduleTask second = task("CLASS1", "M1", "30", 1,
                allocation("L1", 1));
        second.setProduceOrder(1);
        second.setExpectedStartTime(LocalDateTime.of(2026, 6, 12, 14, 0));
        second.setExpectedEndTime(LocalDateTime.of(2026, 6, 12, 18, 0));

        Cd15AutoScheduleOutputDraft result = builder.build(context(), execution(first, second));

        Cd15ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals(new BigDecimal("120"), draft.getShiftSlots().get(0).getPlanQuantity());
        assertEquals(1, draft.getShiftSlots().get(0).getProduceOrder());
        assertEquals(LocalDateTime.of(2026, 6, 12, 14, 0),
                draft.getShiftSlots().get(0).getExpectedStartTime());
        assertEquals(LocalDateTime.of(2026, 6, 12, 18, 0),
                draft.getShiftSlots().get(0).getExpectedEndTime());
        assertEquals(2, result.getLaneAllocations().size());
        Cd15LaneAllocationDraft l1 = result.getLaneAllocations().get(0);
        assertEquals("L1", l1.getLaneCode());
        assertEquals(new BigDecimal("75.0000000000"), l1.getAllocationQuantity());
        assertEquals(2, l1.getVehicleCount());
        assertEquals(new BigDecimal("45"), result.getLaneAllocations().get(1).getAllocationQuantity());
    }

    @Test
    public void shouldWritePriorFailureReasonsToSuccessfulShiftAnalysis() {
        Cd15ShiftScheduleTask class4 = task("CLASS4", "M1", "609", 7,
                allocation("L1", 7));

        Cd15AutoScheduleOutputDraft result = builder.build(contextWithFourShifts(),
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
    public void shouldUseShiftDisplayNameInFailureAndPartialAnalysis() {
        Cd15ShiftScheduleTask class2 = task("CLASS2", "M1", "400", 5,
                allocation("L1", 5));

        Cd15AutoScheduleOutputDraft result = builder.build(context(),
                executionWithTraces(Collections.singletonList(class2),
                        trace("CLASS1", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 1,
                                "中班06/12"),
                        partialTrace("CLASS2", "STORAGE_LANE_LIMIT",
                                new BigDecimal("400"), 2, new BigDecimal("600"),
                                "夜班06/13")));

        assertEquals("中班06/12：库排容量不足</br>"
                        + "夜班06/13：库排容量不足，仅部分排400m，剩余200m转后续班次重算",
                result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis());
    }

    @Test
    public void shouldWritePartialScheduleReasonToSuccessfulShiftAnalysis() {
        Cd15ShiftScheduleTask class4 = task("CLASS4", "M1", "400", 5,
                allocation("L1", 5));

        Cd15AutoScheduleOutputDraft result = builder.build(contextWithFourShifts(),
                executionWithTraces(Collections.singletonList(class4),
                        trace("CLASS1", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 1),
                        trace("CLASS2", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 2),
                        trace("CLASS3", "STORAGE_LANE_LIMIT", BigDecimal.ZERO, 3),
                        partialTrace("CLASS4", "STORAGE_LANE_LIMIT", new BigDecimal("400"), 4)));

        String analysis = result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis();
        assertEquals("CLASS1：库排容量不足</br>CLASS2：库排容量不足</br>CLASS3：库排容量不足</br>"
                        + "CLASS4：库排容量不足，仅部分排400m，剩余200m转后续班次重算",
                analysis);
        assertEquals(analysis, result.getExplainLogs().get(0).getShiftDetails().get(0).getAnalysis());
    }

    @Test
    public void shouldWriteToolingLimitForSuccessfulPartialSchedule() {
        Cd15ShiftScheduleTask class1 = task("CLASS1", "M1", "1280", 16,
                allocation("L1", 16));

        Cd15AutoScheduleOutputDraft result = builder.build(context(),
                executionWithTraces(Collections.singletonList(class1),
                        partialTrace("CLASS1", "TOOLING_LIMIT", new BigDecimal("1280"), 1,
                                new BigDecimal("3250"))));

        assertEquals("CLASS1：工装不足，仅部分排1280m，剩余1970m转后续班次重算",
                result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis());
    }
    /** 验证新增规格提前生产原因会追加到实际成功班次分析。 */
    @Test
    public void shouldAppendNewSpecAdvanceAnalysisToSuccessfulShift() {
        Cd15ShiftScheduleTask class1 = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        String analysis = "新增规格：回看2026-06-29至2026-07-08无历史排程计划，"
                + "原需求日2026-07-09、2026-07-10，提前至2026-07-08生产";
        Cd15MultiShiftExecutionResult execution = Cd15MultiShiftExecutionResult.builder()
                .rollingContext(Cd15RollingScheduleContext.builder()
                        .committedTasks(Collections.singletonList(class1))
                        .newSpecAdvanceInfoBySteelStrip(Collections.singletonMap("C1",
                                Cd15NewSpecAdvanceInfo.builder()
                                        .analysis(analysis).build()))
                        .build())
                .unscheduledResults(Collections.emptyList()).build();

        Cd15AutoScheduleOutputDraft result = this.builder.build(context(), execution);

        assertEquals(analysis,
                result.getScheduleResults().get(0).getShiftSlots().get(0).getAnalysis());
        assertEquals(analysis,
                result.getExplainLogs().get(0).getShiftDetails().get(0).getAnalysis());
    }
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectVehicleCountMismatch() {
        Cd15ShiftScheduleTask invalid = task("CLASS1", "M1", "100", 2,
                allocation("L1", 1));

        builder.build(context(), execution(invalid));
    }

    @Test
    public void shouldJoinMultipleLaneCodesAsPrimaryLaneCode() {
        // 单任务跨多个库排时,主表 primaryLaneCode 用逗号拼接去重
        Cd15ShiftScheduleTask task = task("CLASS1", "M1", "500", 5,
                allocation("L1", 2), allocation("L2", 3));

        Cd15AutoScheduleOutputDraft result = builder.build(context(), execution(task));

        Cd15ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals("L1,L2", draft.getPrimaryLaneCode());
    }

    @Test
    public void shouldDeduplicateLaneCodesWhenSameLaneAcrossAllocations() {
        // 同一库排在同一任务的多条分配中出现时,主表 primaryLaneCode 去重
        Cd15ShiftScheduleTask first = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd15ShiftScheduleTask second = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        second.setProduceOrder(2);

        Cd15AutoScheduleOutputDraft result = builder.build(context(), execution(first, second));

        Cd15ScheduleResultDraft draft = result.getScheduleResults().get(0);
        assertEquals("L1", draft.getPrimaryLaneCode());
    }

    @Test
    public void shouldAttachSameSteelStripSourceTraceToResultsOnDifferentMachines() {
        Cd15ShiftScheduleTask first = task("CLASS1", "M1", "100", 1,
                allocation("L1", 1));
        Cd15ShiftScheduleTask second = task("CLASS2", "M2", "80", 1,
                allocation("L2", 1));
        Cd15SteelStripSourceTrace trace = Cd15SteelStripSourceTrace.builder()
                .cxBatchNo("B1,B2").cxMachineCodes("CX01,CX02")
                .planSurplusQty(new BigDecimal("30")).build();
        Cd15MultiShiftExecutionResult execution = Cd15MultiShiftExecutionResult.builder()
                .rollingContext(Cd15RollingScheduleContext.builder()
                        .committedTasks(Arrays.asList(first, second)).build())
                .steelStripSourceTraceBySteelStrip(Collections.singletonMap("C1", trace))
                .unscheduledResults(Collections.emptyList()).build();

        Cd15AutoScheduleOutputDraft result = this.builder.build(context(), execution);

        assertEquals(2, result.getScheduleResults().size());
        result.getScheduleResults().forEach(draft -> {
            assertEquals("B1,B2", draft.getCxBatchNo());
            assertEquals("CX01,CX02", draft.getCxMachineCodes());
            assertEquals(new BigDecimal("30"), draft.getPlanSurplusQty());
        });
    }
    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder().shifts(Arrays.asList(
                shift("CLASS1", LocalDateTime.of(2026, 6, 12, 14, 0)),
                shift("CLASS2", LocalDateTime.of(2026, 6, 12, 22, 0)))).build();
    }

    private Cd15AutoScheduleContext contextWithFourShifts() {
        return Cd15AutoScheduleContext.builder().shifts(Arrays.asList(
                shift("CLASS1", LocalDateTime.of(2026, 6, 12, 14, 0)),
                shift("CLASS2", LocalDateTime.of(2026, 6, 12, 22, 0)),
                shift("CLASS3", LocalDateTime.of(2026, 6, 13, 6, 0)),
                shift("CLASS4", LocalDateTime.of(2026, 6, 13, 14, 0)))).build();
    }

    private Cd15ShiftDescriptor shift(String classField, LocalDateTime start) {
        LocalDateTime end = start.plusHours(8);
        return Cd15ShiftDescriptor.builder().classField(classField)
                .shiftCode(classField + "_SHIFT").scheduleDate(end.toLocalDate())
                .startTime(start).endTime(end).build();
    }

    private Cd15MultiShiftExecutionResult execution(Cd15ShiftScheduleTask... tasks) {
        return Cd15MultiShiftExecutionResult.builder()
                .rollingContext(Cd15RollingScheduleContext.builder()
                        .committedTasks(Arrays.asList(tasks)).build())
                .unscheduledResults(Collections.emptyList()).build();
    }

    private Cd15MultiShiftExecutionResult executionWithTraces(
            java.util.List<Cd15ShiftScheduleTask> tasks,
            Cd15ScheduleAttemptTrace... traces) {
        return Cd15MultiShiftExecutionResult.builder()
                .rollingContext(Cd15RollingScheduleContext.builder()
                        .committedTasks(tasks).build())
                .attemptTraces(Arrays.asList(traces))
                .unscheduledResults(Collections.emptyList()).build();
    }

    private Cd15MultiShiftExecutionResult execution(
            Cd15ShiftScheduleTask first, Cd15ShiftScheduleTask second,
            Cd15ShiftScheduleTask third, Cd15UnscheduledResultModel unscheduled) {
        return Cd15MultiShiftExecutionResult.builder()
                .rollingContext(Cd15RollingScheduleContext.builder()
                        .committedTasks(Arrays.asList(first, second, third)).build())
                .unscheduledResults(Collections.singletonList(unscheduled)).build();
    }

    private Cd15ShiftScheduleTask task(String classField, String machineCode,
                                       String quantity, int vehicleCount,
                                       Cd15StorageLaneAllocation... allocations) {
        return Cd15ShiftScheduleTask.builder().classField(classField)
                .materialKey("C1|BR1|15|80|80|80|false")
                .steelStripCode("C1").bigRollCode("BR1")
                .cuttingAngle("15").cordSpec("SPEC1")
                .machineCode(machineCode).planQuantity(new BigDecimal(quantity))
                .vehicleCount(vehicleCount).produceOrder(1)
                .expectedStartTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .expectedEndTime(LocalDateTime.of(2026, 6, 12, 16, 0))
                .laneAllocations(Arrays.asList(allocations)).build();
    }

    private Cd15StorageLaneAllocation allocation(String laneCode, int vehicleCount) {
        return Cd15StorageLaneAllocation.builder()
                .laneCode(laneCode).vehicleCount(vehicleCount).build();
    }

    private Cd15ScheduleAttemptTrace trace(String classField, String reason,
                                           BigDecimal scheduledQuantity, int sequence) {
        return trace(classField, reason, scheduledQuantity, sequence, null);
    }

    private Cd15ScheduleAttemptTrace trace(String classField, String reason,
                                           BigDecimal scheduledQuantity, int sequence,
                                           String shiftDisplayName) {
        return Cd15ScheduleAttemptTrace.builder().classField(classField)
                .shiftDisplayName(shiftDisplayName)
                .steelStripCode("C1").cuttingAngle("15")
                .shiftCode(classField + "_SHIFT").bigRollCode("BR1")
                .netDemandQuantity(new BigDecimal("600"))
                .scheduledQuantity(scheduledQuantity).failureReason(reason)
                .sequence(sequence).build();
    }

    private Cd15ScheduleAttemptTrace partialTrace(String classField, String partialReason,
                                                  BigDecimal scheduledQuantity, int sequence) {
        return partialTrace(classField, partialReason, scheduledQuantity, sequence,
                new BigDecimal("600"));
    }

    private Cd15ScheduleAttemptTrace partialTrace(String classField, String partialReason,
                                                  BigDecimal scheduledQuantity, int sequence,
                                                  BigDecimal netDemandQuantity) {
        return partialTrace(classField, partialReason, scheduledQuantity, sequence,
                netDemandQuantity, null);
    }

    private Cd15ScheduleAttemptTrace partialTrace(String classField, String partialReason,
                                                  BigDecimal scheduledQuantity, int sequence,
                                                  BigDecimal netDemandQuantity,
                                                  String shiftDisplayName) {
        return Cd15ScheduleAttemptTrace.builder().classField(classField)
                .shiftDisplayName(shiftDisplayName)
                .steelStripCode("C1").cuttingAngle("15")
                .shiftCode(classField + "_SHIFT").bigRollCode("BR1")
                .netDemandQuantity(netDemandQuantity).scheduledQuantity(scheduledQuantity)
                .partialReason(partialReason).sequence(sequence).build();
    }
}
