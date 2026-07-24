package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15MultiShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftExecutionResult;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cd15.engine.service.Cd15ShiftDemandProvider;
import com.zlt.aps.cd15.engine.service.Cd15SingleShiftScheduleService;
import com.zlt.aps.cd15.engine.service.impl.Cd15NewSpecAdvanceInputPreparer;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/** 多班执行器重新加载和稳定循环测试。 */
public class Cd15MultiShiftScheduleExecutorTest {

    @Test
    public void shouldReloadInputAndExecuteCandidatesForEveryShift() {
        AtomicInteger loadCount = new AtomicInteger();
        AtomicInteger executeCount = new AtomicInteger();
        List<LocalDate> resourceBaselineDates = new ArrayList<>();
        List<String> resourceBaselineShiftCodes = new ArrayList<>();
        Cd15AutoScheduleInputService inputService = (factory, date, classField, shiftCode,
                                                      resourceBaselineDate,
                                                      resourceBaselineShiftCode,
                                                      agingHours) -> {
            resourceBaselineDates.add(resourceBaselineDate);
            resourceBaselineShiftCodes.add(resourceBaselineShiftCode);
            int loadIndex = loadCount.incrementAndGet();
            Cd15SteelStripSourceTrace trace = Cd15SteelStripSourceTrace.builder()
                    .cxBatchNo(loadIndex == 1 ? "FIRST" : "SECOND").build();
            return Cd15AutoScheduleInput.builder()
                    .steelStripSourceTraceBySteelStrip(Collections.singletonMap("C1", trace))
                    .storageLanesAtSix(Collections.singletonList(Cd15StorageLaneState.builder()
                            .laneCode("L1").vehicleCount(0).maxVehicleCount(7).build()))
                    .build();
        };
        Cd15SingleShiftScheduleService singleShift = (context, input, shift, state, rolling) -> {
            executeCount.incrementAndGet();
            return Cd15ShiftExecutionResult.builder().shift(shift).state(state)
                    .tasks(Collections.emptyList()).failures(Collections.emptyMap())
                    .attemptTraces(Collections.singletonList(Cd15ScheduleAttemptTrace.builder()
                            .steelStripCode("C1").bigRollCode("BR1").cuttingAngle("15")
                            .classField(shift.getClassField()).shiftCode(shift.getShiftCode())
                            .netDemandQuantity(new BigDecimal("100"))
                            .scheduledQuantity(new BigDecimal("20"))
                            .failureReason("ROLL_TOOL_LIMIT")
                            .sequence(executeCount.get()).build()))
                    .build();
        };
        Cd15ShiftDemandProvider demandProvider = new Cd15ShiftDemandProvider() {
            @Override
            public com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision resolve(
                    Cd15AutoScheduleContext context, Cd15AutoScheduleInput input,
                    Cd15ShiftDescriptor shift,
                    com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate candidate,
                    com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext rolling) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BigDecimal cumulativeConsumptionBeforeShift(Cd15AutoScheduleContext context,
                                                               Cd15AutoScheduleInput input,
                                                               Cd15ShiftDescriptor shift) {
                return BigDecimal.ZERO;
            }
        };
        Cd15MultiShiftScheduleExecutor executor = new Cd15MultiShiftScheduleExecutor(
                inputService, new Cd15NewSpecAdvanceInputPreparer(
                        this.emptyHistoryMapper(), new Cd15NewSpecAdvanceResolver()),
                singleShift, demandProvider,
                new Cd15RollingScheduleContextManager(new Cd15ResourceSnapshotBuilder(
                        new Cd15StorageLaneConsumptionCalculator(), new Cd15InboundResolver())),
                new Cd15UnscheduledResultAggregator(new Cd15UnscheduledReasonResolver()),
                new Cd15AutoScheduleRuntimeGuard());
        List<Integer> progressValues = new ArrayList<>();
        List<String> progressStageNames = new ArrayList<>();

        Cd15MultiShiftExecutionResult result = executor.execute(context(),
                (progress, stage, stageName, shift) -> {
                    progressValues.add(progress);
                    progressStageNames.add(stageName);
                });

        assertEquals(2, loadCount.get());
        assertEquals(2, executeCount.get());
        assertEquals(Arrays.asList(LocalDate.of(2026, 6, 11),
                LocalDate.of(2026, 6, 11)), resourceBaselineDates);
        assertEquals(Arrays.asList("CURRENT", "CURRENT"), resourceBaselineShiftCodes);
        assertEquals(2, result.getShiftResults().size());
        assertEquals(2, result.getAttemptTraces().size());
        assertEquals(2, result.getUnscheduledResults().size());
        assertEquals(new BigDecimal("60"),
                result.getUnscheduledResults().get(0).getUnscheduledQuantity());
        assertEquals(Arrays.asList(20, 52, 52, 85), progressValues);
        assertEquals(Arrays.asList("中班06/12班次开始", "中班06/12班次完成",
                "夜班06/13班次开始", "夜班06/13班次完成"), progressStageNames);
        assertEquals("FIRST",
                result.getSteelStripSourceTraceBySteelStrip().get("C1").getCxBatchNo());
    }

    /** 构造不返回历史生产记录的Mapper，避免测试依赖Mockito。 */
    private Cd15EngineScheduleResultMapper emptyHistoryMapper() {
        return (Cd15EngineScheduleResultMapper) java.lang.reflect.Proxy.newProxyInstance(
                Cd15EngineScheduleResultMapper.class.getClassLoader(),
                new Class<?>[]{Cd15EngineScheduleResultMapper.class},
                (proxy, method, arguments) -> "selectList".equals(method.getName())
                        ? Collections.emptyList() : null);
    }
    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder().factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .startTime(LocalDateTime.now())
                .resourceBaselineDate(LocalDate.of(2026, 6, 11))
                .resourceBaselineShiftCode("CURRENT")
                .parameters(Cd15AutoScheduleParameters.builder()
                        .rollCoilMeter(new BigDecimal("100")).rollTotalCount(10)
                        .newSpecLookbackDays(10).newSpecAdvanceDays(2)
                        .taskTimeoutMinutes(30).build())
                .shifts(Arrays.asList(shift("CLASS1", "S1", "中班06/12", 14),
                        shift("CLASS2", "S2", "夜班06/13", 22))).build();
    }

    private Cd15ShiftDescriptor shift(String classField, String shiftCode, String shiftDisplayName, int hour) {
        LocalDateTime start = LocalDateTime.of(2026, 6, 12, 0, 0).plusHours(hour);
        return Cd15ShiftDescriptor.builder().classField(classField).shiftCode(shiftCode)
                .shiftDisplayName(shiftDisplayName).scheduleDate(start.toLocalDate())
                .startTime(start).endTime(start.plusHours(8)).durationSeconds(28800).build();
    }
}
