package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 当前班次库排分配测试。
 */
public class Cd90StorageLaneAllocatorTest {

    private final Cd90StorageLaneAllocator allocator = new Cd90StorageLaneAllocator();

    @Test
    public void shouldPreferSameClothAndSplitAcrossLanes() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 5, 7),
                lane("L2", null, 0, 7),
                lane("L3", "CF002", 1, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("500"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getRequiredVehicleCount());
        assertEquals(2, result.getAllocations().size());
        assertEquals("L1", result.getAllocations().get(0).getLaneCode());
        assertEquals(2, result.getAllocations().get(0).getVehicleCount());
        assertEquals("L2", result.getAllocations().get(1).getLaneCode());
        assertEquals(3, result.getAllocations().get(1).getVehicleCount());
        assertEquals("CF001", result.getLanes().get(1).getClothCode());
    }

    @Test
    public void insufficientCapacityShouldNotModifyOriginalLanes() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 6, 7), lane("L2", null, 0, 1));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), original);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(6, original.get(0).getVehicleCount());
        assertEquals(0, original.get(1).getVehicleCount());
        assertEquals(null, original.get(1).getClothCode());
    }

    @Test
    public void sameClothLanesShouldUseHigherVehicleCountFirst() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 2, 7),
                lane("L2", "CF001", 5, 7),
                lane("L3", "CF001", 3, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), original);

        assertEquals("L2", result.getAllocations().get(0).getLaneCode());
        assertEquals("L3", result.getAllocations().get(1).getLaneCode());
    }

    private Cd90StorageLaneState lane(String code, String clothCode, int count, int max) {
        return Cd90StorageLaneState.builder().laneCode(code).clothCode(clothCode)
                .vehicleCount(count).maxVehicleCount(max).build();
    }
}
