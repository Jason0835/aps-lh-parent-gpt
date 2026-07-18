package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import com.zlt.aps.cd15.engine.model.Cd15ResourceSnapshot;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 当前班次资源快照重建测试。
 */
public class Cd15ResourceSnapshotBuilderTest {

    private final Cd15ResourceSnapshotBuilder builder = new Cd15ResourceSnapshotBuilder(
            new Cd15StorageLaneConsumptionCalculator(), new Cd15InboundResolver());

    /**
     * T-16：先按累计消耗释放，再加回前序有效斜裁入库，且不修改6点原始快照。
     */
    @Test
    public void shouldRebuildFromOriginalSnapshotForEveryShift() {
        Cd15StorageLaneState original = lane("L1", 1);
        Cd15ResourceSnapshot result = builder.build(
                Collections.singletonList(original), Collections.singletonMap("C1", new BigDecimal("87")),
                Collections.emptyMap(), new BigDecimal("87"),
                Collections.singletonList(Cd15InboundRecord.builder()
                        .taskKey("T1").actual(false).steelStripCode("C1")
                        .laneCode("L1").vehicleCount(2).build()));

        assertEquals(1, original.getVehicleCount());
        assertEquals(2, result.getLanes().get(0).getVehicleCount());
        assertEquals(7, result.getLanes().get(0).getMaxVehicleCount());
        assertEquals(2, result.getOccupiedVehicleCount());
    }

    /**
     * C01累计消耗释放资源时，不能释放C02已经占用的L02。
     */
    @Test
    public void shouldKeepOtherClothLaneWhenRebuildingSnapshot() {
        Cd15ResourceSnapshot result = builder.build(
                Arrays.asList(lane("L1", "C1", 1), lane("L2", "C2", 1)),
                Collections.singletonMap("C1", new BigDecimal("87")), Collections.emptyMap(),
                new BigDecimal("87"), Collections.emptyList());

        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(1, result.getLanes().get(1).getVehicleCount());
        assertEquals("C2", result.getLanes().get(1).getSteelStripCode());
    }

    /** ?????????????????????? */
    @Test
    public void shouldUseClothCurlLengthWhenRebuildingSnapshot() {
        Cd15ResourceSnapshot result = builder.build(
                Arrays.asList(lane("L1", "C1", 1), lane("L2", "C1", 1)),
                Collections.singletonMap("C1", new BigDecimal("160")),
                Collections.singletonMap("C1", new BigDecimal("80")), new BigDecimal("100"),
                Collections.emptyList());

        assertEquals(2, result.getReleasedVehicleCount());
        assertEquals(0, result.getOccupiedVehicleCount());
    }

    /**
     * 前序斜裁计划入库在后续班次开班前也可能已经被成型消耗，
     * 重建库排时必须先合并有效入库，再按累计消耗扣减。
     */
    @Test
    public void shouldConsumePlannedInboundWhenRebuildingSnapshot() {
        Cd15ResourceSnapshot result = builder.build(
                Collections.singletonList(lane("L1", "C1", 0)),
                Collections.singletonMap("C1", new BigDecimal("240")),
                Collections.singletonMap("C1", new BigDecimal("80")), new BigDecimal("100"),
                Collections.singletonList(Cd15InboundRecord.builder()
                        .taskKey("T1").actual(false).steelStripCode("C1")
                        .laneCode("L1").vehicleCount(5).build()));

        assertEquals(3, result.getReleasedVehicleCount());
        assertEquals(2, result.getOccupiedVehicleCount());
        assertEquals(2, result.getLanes().get(0).getVehicleCount());
    }
    private Cd15StorageLaneState lane(String code, int vehicles) {
        return lane(code, "C1", vehicles);
    }

    private Cd15StorageLaneState lane(String code, String steelStripCode, int vehicles) {
        return Cd15StorageLaneState.builder()
                .laneCode(code).steelStripCode(steelStripCode).vehicleCount(vehicles)
                .maxVehicleCount(7).build();
    }
}