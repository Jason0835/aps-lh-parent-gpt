package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import com.zlt.aps.cd90.engine.model.Cd90ResourceSnapshot;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 当前班次资源快照重建测试。
 */
public class Cd90ResourceSnapshotBuilderTest {

    private final Cd90ResourceSnapshotBuilder builder = new Cd90ResourceSnapshotBuilder(
            new Cd90StorageLaneConsumptionCalculator(), new Cd90InboundResolver());

    /**
     * T-16：先按累计消耗释放，再加回前序有效直裁入库，且不修改6点原始快照。
     */
    @Test
    public void shouldRebuildFromOriginalSnapshotForEveryShift() {
        Cd90StorageLaneState original = lane("L1", 1);
        Cd90ResourceSnapshot result = builder.build(
                Collections.singletonList(original), Collections.singletonMap("C1", new BigDecimal("87")),
                Collections.emptyMap(), new BigDecimal("87"),
                Collections.singletonList(Cd90InboundRecord.builder()
                        .taskKey("T1").actual(false).clothCode("C1")
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
        Cd90ResourceSnapshot result = builder.build(
                Arrays.asList(lane("L1", "C1", 1), lane("L2", "C2", 1)),
                Collections.singletonMap("C1", new BigDecimal("87")), Collections.emptyMap(),
                new BigDecimal("87"), Collections.emptyList());

        assertEquals(0, result.getLanes().get(0).getVehicleCount());
        assertEquals(1, result.getLanes().get(1).getVehicleCount());
        assertEquals("C2", result.getLanes().get(1).getClothCode());
    }

    /** ?????????????????????? */
    @Test
    public void shouldUseClothCurlLengthWhenRebuildingSnapshot() {
        Cd90ResourceSnapshot result = builder.build(
                Arrays.asList(lane("L1", "C1", 1), lane("L2", "C1", 1)),
                Collections.singletonMap("C1", new BigDecimal("160")),
                Collections.singletonMap("C1", new BigDecimal("80")), new BigDecimal("100"),
                Collections.emptyList());

        assertEquals(2, result.getReleasedVehicleCount());
        assertEquals(0, result.getOccupiedVehicleCount());
    }

    private Cd90StorageLaneState lane(String code, int vehicles) {
        return lane(code, "C1", vehicles);
    }

    private Cd90StorageLaneState lane(String code, String clothCode, int vehicles) {
        return Cd90StorageLaneState.builder()
                .laneCode(code).clothCode(clothCode).vehicleCount(vehicles)
                .maxVehicleCount(7).build();
    }
}