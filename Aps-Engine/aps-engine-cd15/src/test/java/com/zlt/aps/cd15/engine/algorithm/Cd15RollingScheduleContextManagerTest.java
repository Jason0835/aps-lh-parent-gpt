package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15RollingPendingTask;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15ShiftResourceState;
import com.zlt.aps.cd15.engine.model.Cd15ShiftScheduleTask;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneAllocation;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 多班滚动上下文和计划入库测试。 */
public class Cd15RollingScheduleContextManagerTest {

    private final Cd15RollingScheduleContextManager manager =
            new Cd15RollingScheduleContextManager(new Cd15ResourceSnapshotBuilder(
                    new Cd15StorageLaneConsumptionCalculator(), new Cd15InboundResolver()));
    /** 验证首班按新增规格证据初始化提前需求剩余量。 */
    @Test
    public void shouldInitializeNewSpecAdvanceRemainingDemand() {
        Cd15NewSpecAdvanceInfo info = Cd15NewSpecAdvanceInfo.builder()
                .steelStripCode("C01").advanceDemandQuantity(new BigDecimal("300"))
                .sourceDemandDates(Collections.emptyList())
                .sourceDemandKeys(Collections.emptyList()).build();

        Cd15RollingScheduleContext result = this.manager.initialize(
                Collections.emptyList(), Collections.singletonMap("C01", info));

        assertEquals(new BigDecimal("300"),
                result.getNewSpecAdvanceRemainingBySteelStrip().get("C01"));
        assertEquals(info.getAnalysis(), result.getNewSpecAdvanceInfoBySteelStrip()
                .get("C01").getAnalysis());
        assertTrue(result.getNormalizedNewSpecAdvanceSteelStripCodes().isEmpty());
        assertTrue(result.getPendingTasks().isEmpty());
    }

    /** 插单跨班节点必须保留来源任务、原顺序和剩余量。 */
    @Test
    public void shouldKeepTaskIdentityWhenCarriedToNextShift() {
        Cd15RollingScheduleContext context = manager.initialize(Collections.emptyList());
        Cd15RollingPendingTask pendingTask = Cd15RollingPendingTask.builder()
                .taskKey("1001|CLASS1|2").sourceResultId(1001L)
                .originalClassField("CLASS1").originalProduceOrder(2)
                .targetClassField("CLASS2").remainingQuantity(new BigDecimal("80"))
                .requiredMachineCode("M1").hardInsert(false).locked(false).build();

        context.getPendingTasks().add(pendingTask);

        assertEquals(Long.valueOf(1001L), context.getPendingTasks().get(0).getSourceResultId());
        assertEquals("CLASS1", context.getPendingTasks().get(0).getOriginalClassField());
        assertEquals(Integer.valueOf(2), context.getPendingTasks().get(0).getOriginalProduceOrder());
        assertEquals(new BigDecimal("80"), context.getPendingTasks().get(0).getRemainingQuantity());
    }

    @Test
    public void shouldRebuildEveryShiftFromSixOClockSnapshotAndRollPlannedInboundForward() {
        Cd15RollingScheduleContext context = manager.initialize(Collections.singletonList(
                Cd15StorageLaneState.builder().laneCode("L1").steelStripCode("C1")
                        .vehicleCount(1).maxVehicleCount(7).build()));
        manager.updateCumulativeConsumption(context, Collections.singletonMap("C1", new BigDecimal("87")));

        Cd15ShiftResourceState first = manager.openShift(context,
                descriptor("CLASS1", 14, 22), Collections.emptyMap(), new BigDecimal("87"),
                10, Collections.singletonList("M1"));
        first.getTasks().add(task("CLASS1", "M1", 20, 2));
        first.getTailSpecByMachine().put("M1", "SPEC-A");
        manager.completeShift(context, first);

        Cd15ShiftResourceState second = manager.openShift(context,
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

    @Test
    public void shouldRestoreAllocatedQuantityWhenNextShiftReloadsStock() {
        Cd15RollingScheduleContext context = manager.initialize(Collections.emptyList());
        Cd15BigRollAgingStock consumed = Cd15BigRollAgingStock.builder()
                .sourceType("ACTUAL_STOCK").sourceId("ACTUAL:BARCODE-1").bigRollCode("BR1")
                .availableQuantity(new BigDecimal("200")).allocatedQuantity(new BigDecimal("80"))
                .build();
        Cd15ShiftResourceState completed = Cd15ShiftResourceState.builder()
                .tasks(Collections.emptyList())
                .bigRollAgingStocks(Collections.singletonList(consumed))
                .build();
        manager.completeShift(context, completed);
        Cd15BigRollAgingStock reloaded = Cd15BigRollAgingStock.builder()
                .sourceType("ACTUAL_STOCK").sourceId("ACTUAL:BARCODE-1").bigRollCode("BR1")
                .availableQuantity(new BigDecimal("200")).allocatedQuantity(BigDecimal.ZERO)
                .build();

        Cd15BigRollAgingStock restored = manager.restoreBigRollAllocations(
                context, Collections.singletonList(reloaded)).get(0);

        assertEquals(new BigDecimal("80"), restored.getAllocatedQuantity());
        assertEquals(new BigDecimal("120"), restored.getRemainingQuantity());
    }


    private Cd15ShiftDescriptor descriptor(String classField, int startHour, int endHour) {
        LocalDateTime base = LocalDateTime.of(2026, 6, 12, 0, 0);
        return Cd15ShiftDescriptor.builder().shiftCode(classField + "_CODE")
                .classField(classField).startTime(base.plusHours(startHour))
                .endTime(base.plusHours(endHour)).durationSeconds(28800).build();
    }

    private Cd15ShiftScheduleTask task(String classField, String machineCode,
                                       int endHour, int vehicleCount) {
        return Cd15ShiftScheduleTask.builder().classField(classField)
                .materialKey("C1|BR1|15|80|80|87|false")
                .steelStripCode("C1").bigRollCode("BR1").cuttingAngle("15")
                .cordSpec("SPEC-A").machineCode(machineCode)
                .planQuantity(new BigDecimal("174")).vehicleCount(vehicleCount)
                .expectedEndTime(LocalDateTime.of(2026, 6, 12, endHour, 0))
                .laneAllocations(Collections.singletonList(Cd15StorageLaneAllocation.builder()
                        .laneCode("L1").vehicleCount(vehicleCount).build())).build();
    }
}
