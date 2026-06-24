package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90StorageLaneAllocationResult;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 当前班次库排分配测试。
 */
public class Cd90StorageLaneAllocatorTest {

    private final Cd90StorageLaneAllocator allocator = new Cd90StorageLaneAllocator();

    @Test
    public void shouldOnlyUseSameClothLanesAndSplitAcrossLanes() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 5, 7),
                lane("L2", "CF001", 0, 7),
                lane("L3", "CF002", 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("500"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getRequiredVehicleCount());
        assertEquals(2, result.getAllocations().size());
        assertEquals("L1", result.getAllocations().get(0).getLaneCode());
        assertEquals(2, result.getAllocations().get(0).getVehicleCount());
        assertEquals("L2", result.getAllocations().get(1).getLaneCode());
        assertEquals(3, result.getAllocations().get(1).getVehicleCount());
        assertEquals("CF002", result.getLanes().get(2).getClothCode());
        assertEquals(0, result.getLanes().get(2).getVehicleCount());
    }

    @Test
    public void shouldRejectZeroVehicleLaneWhenClothCodeDoesNotMatch() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 7, 7),
                lane("L2", "CF002", 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("100"), new BigDecimal("100"), original);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals("CF002", original.get(1).getClothCode());
        assertEquals(0, original.get(1).getVehicleCount());
    }

    @Test
    public void insufficientCapacityShouldNotModifyOriginalLanes() {
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 6, 7), lane("L2", "CF002", 0, 1));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), original);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
        assertEquals(6, original.get(0).getVehicleCount());
        assertEquals(0, original.get(1).getVehicleCount());
        assertEquals("CF002", original.get(1).getClothCode());
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

    @Test
    public void shouldFallbackToEmptyLaneWhenNoSameSpecAvailable() {
        // 无同规格库排时,空库排(clothCode 空 且 vehicleCount=0)兜底
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 7, 7),
                lane("L2", null, 0, 7),
                lane("L3", null, 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getRequiredVehicleCount());
        assertEquals(3, result.getAllocatedVehicleCount());
        // 按 laneCode 稳定排序,L2 优先
        assertEquals("L2", result.getAllocations().get(0).getLaneCode());
        assertEquals(3, result.getAllocations().get(0).getVehicleCount());
        // 空库排被分配后 clothCode 置位
        assertEquals("CF001", result.getLanes().get(1).getClothCode());
        assertEquals(3, result.getLanes().get(1).getVehicleCount());
        // L3 未被使用,仍是空库排
        assertNull(result.getLanes().get(2).getClothCode());
        assertEquals(0, result.getLanes().get(2).getVehicleCount());
    }

    @Test
    public void shouldPreferSameSpecOverEmptyLane() {
        // 同规格库排和空库排并存时,优先消耗同规格
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF001", 5, 7),
                lane("L2", null, 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("500"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getRequiredVehicleCount());
        // L1 同规格优先,放 2 车;L2 空库排兜底,放 3 车
        assertEquals(2, result.getAllocations().size());
        assertEquals("L1", result.getAllocations().get(0).getLaneCode());
        assertEquals(2, result.getAllocations().get(0).getVehicleCount());
        assertEquals("L2", result.getAllocations().get(1).getLaneCode());
        assertEquals(3, result.getAllocations().get(1).getVehicleCount());
        // L2 被分配后 clothCode 置位
        assertEquals("CF001", result.getLanes().get(1).getClothCode());
    }

    @Test
    public void emptyLaneAllocatedBecomesSameSpecForNextShift() {
        // 空库排被部分分配后,剩余容量仍可被同规格继续填
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", null, 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getAllocatedVehicleCount());
        assertEquals("CF001", result.getLanes().get(0).getClothCode());
        assertEquals(3, result.getLanes().get(0).getVehicleCount());

        // 第二次分配同规格,此时 L1 已绑定 CF001,作为同规格候选
        Cd90StorageLaneAllocationResult result2 = allocator.allocate(
                "CF001", new BigDecimal("300"), new BigDecimal("100"), result.getLanes());

        assertTrue(result2.isSuccess());
        assertEquals(3, result2.getAllocatedVehicleCount());
        assertEquals("L1", result2.getAllocations().get(0).getLaneCode());
        assertEquals(6, result2.getLanes().get(0).getVehicleCount());
    }

    @Test
    public void shouldSplitAcrossMultipleEmptyLanesByLaneCodeOrder() {
        // 需求超过单个空库排容量,跨多个空库排分配,按 laneCode 排序
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L3", null, 0, 3),
                lane("L1", null, 0, 3),
                lane("L2", null, 0, 3));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("700"), new BigDecimal("100"), original);

        assertTrue(result.isSuccess());
        assertEquals(7, result.getRequiredVehicleCount());
        assertEquals(7, result.getAllocatedVehicleCount());
        // 按 laneCode 排序:L1(3) + L2(3) + L3(1)
        assertEquals(3, result.getAllocations().size());
        assertEquals("L1", result.getAllocations().get(0).getLaneCode());
        assertEquals(3, result.getAllocations().get(0).getVehicleCount());
        assertEquals("L2", result.getAllocations().get(1).getLaneCode());
        assertEquals(3, result.getAllocations().get(1).getVehicleCount());
        assertEquals("L3", result.getAllocations().get(2).getLaneCode());
        assertEquals(1, result.getAllocations().get(2).getVehicleCount());
    }

    @Test
    public void shouldRejectWhenOnlyEmptyLaneWithClothCodeMismatch() {
        // 空库排 clothCode 必须为空才进入候选;有 clothCode 但 vehicleCount=0 的不算空库排
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", "CF002", 0, 7));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("100"), new BigDecimal("100"), original);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
    }

    @Test
    public void shouldRejectWhenMaxCarNumIsZeroForEmptyLane() {
        // 空库排 maxVehicleCount=0 时不可分配
        List<Cd90StorageLaneState> original = Arrays.asList(
                lane("L1", null, 0, 0));

        Cd90StorageLaneAllocationResult result = allocator.allocate(
                "CF001", new BigDecimal("100"), new BigDecimal("100"), original);

        assertFalse(result.isSuccess());
        assertEquals("STORAGE_LANE_LIMIT", result.getFailureReason());
    }

    private Cd90StorageLaneState lane(String code, String clothCode, int count, int max) {
        return Cd90StorageLaneState.builder().laneCode(code).clothCode(clothCode)
                .vehicleCount(count).maxVehicleCount(max).build();
    }
}