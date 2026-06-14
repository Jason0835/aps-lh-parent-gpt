package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90MultiShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleAttemptTrace;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftExecutionResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cd90.engine.service.Cd90ShiftDemandProvider;
import com.zlt.aps.cd90.engine.service.Cd90SingleShiftScheduleService;
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
public class Cd90MultiShiftScheduleExecutorTest {

    @Test
    public void shouldReloadInputAndExecuteCandidatesForEveryShift() {
        AtomicInteger loadCount = new AtomicInteger();
        AtomicInteger executeCount = new AtomicInteger();
        Cd90AutoScheduleInputService inputService = (factory, date, classField, shiftCode) -> {
            loadCount.incrementAndGet();
            return Cd90AutoScheduleInput.builder()
                    .storageLanesAtSix(Collections.singletonList(Cd90StorageLaneState.builder()
                            .laneCode("L1").vehicleCount(0).maxVehicleCount(7).build()))
                    .build();
        };
        Cd90SingleShiftScheduleService singleShift = (context, input, shift, state, rolling) -> {
            executeCount.incrementAndGet();
            return Cd90ShiftExecutionResult.builder().shift(shift).state(state)
                    .tasks(Collections.emptyList()).failures(Collections.emptyMap())
                    .attemptTraces(Collections.singletonList(Cd90ScheduleAttemptTrace.builder()
                            .classField(shift.getClassField()).shiftCode(shift.getShiftCode())
                            .clothCode("C1").netDemandQuantity(new BigDecimal("100"))
                            .scheduledQuantity(new BigDecimal("20"))
                            .failureReason("ROLL_TOOL_LIMIT")
                            .sequence(executeCount.get()).build()))
                    .build();
        };
        Cd90ShiftDemandProvider demandProvider = new Cd90ShiftDemandProvider() {
            @Override
            public com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision resolve(
                    Cd90AutoScheduleContext context, Cd90AutoScheduleInput input,
                    Cd90ShiftDescriptor shift,
                    com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate candidate,
                    com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext rolling) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BigDecimal cumulativeConsumptionBeforeShift(Cd90AutoScheduleContext context,
                                                               Cd90AutoScheduleInput input,
                                                               Cd90ShiftDescriptor shift) {
                return BigDecimal.ZERO;
            }
        };
        Cd90MultiShiftScheduleExecutor executor = new Cd90MultiShiftScheduleExecutor(
                inputService, singleShift, demandProvider,
                new Cd90RollingScheduleContextManager(new Cd90ResourceSnapshotBuilder(
                        new Cd90StorageLaneConsumptionCalculator(), new Cd90InboundResolver())),
                new Cd90UnscheduledResultAggregator(new Cd90UnscheduledReasonResolver()),
                new Cd90AutoScheduleRuntimeGuard());
        List<Integer> progressValues = new ArrayList<>();

        Cd90MultiShiftExecutionResult result = executor.execute(context(),
                (progress, stage, stageName, shift) -> progressValues.add(progress));

        assertEquals(2, loadCount.get());
        assertEquals(2, executeCount.get());
        assertEquals(2, result.getShiftResults().size());
        assertEquals(2, result.getAttemptTraces().size());
        assertEquals(2, result.getUnscheduledResults().size());
        assertEquals(new BigDecimal("60"),
                result.getUnscheduledResults().get(0).getUnscheduledQuantity());
        assertEquals(Arrays.asList(20, 52, 52, 85), progressValues);
    }

    private Cd90AutoScheduleContext context() {
        return Cd90AutoScheduleContext.builder().factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .startTime(LocalDateTime.now())
                .parameters(Cd90AutoScheduleParameters.builder()
                        .rollCoilMeter(new BigDecimal("100")).rollTotalCount(10)
                        .taskTimeoutMinutes(30).build())
                .shifts(Arrays.asList(shift("CLASS1", "S1", 14),
                        shift("CLASS2", "S2", 22))).build();
    }

    private Cd90ShiftDescriptor shift(String classField, String shiftCode, int hour) {
        LocalDateTime start = LocalDateTime.of(2026, 6, 12, 0, 0).plusHours(hour);
        return Cd90ShiftDescriptor.builder().classField(classField).shiftCode(shiftCode)
                .startTime(start).endTime(start.plusHours(8)).durationSeconds(28800).build();
    }
}
