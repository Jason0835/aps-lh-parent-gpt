package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90ShiftResourceState;
import com.zlt.aps.cd90.engine.model.Cd90ShiftScheduleTask;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocation;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/** 多班滚动上下文和计划入库测试。 */
public class Cd90RollingScheduleContextManagerTest {

    private final Cd90RollingScheduleContextManager manager =
            new Cd90RollingScheduleContextManager(new Cd90ResourceSnapshotBuilder(
                    new Cd90StorageLaneConsumptionCalculator(), new Cd90InboundResolver()));

    @Test
    public void shouldRebuildEveryShiftFromSixOClockSnapshotAndRollPlannedInboundForward() {
        Cd90RollingScheduleContext context = manager.initialize(Collections.singletonList(
                Cd90StorageLaneState.builder().laneCode("L1").clothCode("C1")
                        .vehicleCount(1).maxVehicleCount(7).build()));
        manager.updateCumulativeConsumption(context, Collections.singletonMap("C1", new BigDecimal("87")));

        Cd90ShiftResourceState first = manager.openShift(context,
                descriptor("CLASS1", 14, 22), Collections.emptyMap(), new BigDecimal("87"),
                10, Collections.singletonList("M1"));
        first.getTasks().add(task("CLASS1", "M1", 20, 2));
        first.getTailSpecByMachine().put("M1", "SPEC-A");
        manager.completeShift(context, first);

        Cd90ShiftResourceState second = manager.openShift(context,
                descriptor("CLASS2", 22, 30), Collections.emptyMap(), new BigDecimal("87"),
                10, Collections.singletonList("M1"));

        assertEquals(2, second.getLanes().get(0).getVehicleCount());
        assertEquals(Integer.valueOf(28800), second.getRemainingSecondsByMachine().get("M1"));
        assertEquals("SPEC-A", second.getTailSpecByMachine().get("M1"));
        assertEquals(1, context.getPlannedInboundRecords().size());
        assertEquals(new BigDecimal("174"),
                context.getPlannedInboundRecords().get(0).getInboundQuantity());
        assertEquals(1, context.getCommittedTasks().size());
    }

    private Cd90ShiftDescriptor descriptor(String classField, int startHour, int endHour) {
        LocalDateTime base = LocalDateTime.of(2026, 6, 12, 0, 0);
        return Cd90ShiftDescriptor.builder().shiftCode(classField + "_CODE")
                .classField(classField).startTime(base.plusHours(startHour))
                .endTime(base.plusHours(endHour)).durationSeconds(28800).build();
    }

    private Cd90ShiftScheduleTask task(String classField, String machineCode,
                                       int endHour, int vehicleCount) {
        return Cd90ShiftScheduleTask.builder().classField(classField).clothCode("C1")
                .cordSpec("SPEC-A").machineCode(machineCode)
                .planQuantity(new BigDecimal("174")).vehicleCount(vehicleCount)
                .expectedEndTime(LocalDateTime.of(2026, 6, 12, endHour, 0))
                .laneAllocations(Collections.singletonList(Cd90StorageLaneAllocation.builder()
                        .laneCode("L1").vehicleCount(vehicleCount).build())).build();
    }
}
