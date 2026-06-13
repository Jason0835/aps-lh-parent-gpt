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
                Collections.singletonList(original), new BigDecimal("87"), new BigDecimal("87"),
                Collections.singletonList(Cd90InboundRecord.builder()
                        .taskKey("T1").actual(false).clothCode("C1")
                        .laneCode("L1").vehicleCount(2).build()));

        assertEquals(1, original.getVehicleCount());
        assertEquals(2, result.getLanes().get(0).getVehicleCount());
        assertEquals(7, result.getLanes().get(0).getMaxVehicleCount());
        assertEquals(2, result.getOccupiedVehicleCount());
    }

    private Cd90StorageLaneState lane(String code, int vehicles) {
        return Cd90StorageLaneState.builder()
                .laneCode(code).clothCode("C1").vehicleCount(vehicles)
                .maxVehicleCount(7).build();
    }
}
