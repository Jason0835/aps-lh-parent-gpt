package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingAllocation;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingStock;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 大卷静置成熟流水分配测试。
 */
public class Cd15BigRollAgingAllocatorTest {

    private final Cd15BigRollAgingAllocator allocator = new Cd15BigRollAgingAllocator();

    /**
     * 同一任务整体等待本次所需全部大卷成熟后开裁。
     */
    @Test
    public void shouldDelayWholeTaskUntilSelectedRollsAreAllReleased() {
        LocalDateTime originalStartTime = LocalDateTime.of(2026, 6, 13, 8, 0);
        List<Cd15BigRollAgingStock> stocks = Arrays.asList(
                stock("ROLL-A", "2026-06-13T08:00:00", "80"),
                stock("ROLL-B", "2026-06-13T11:00:00", "80"));

        Cd15BigRollAgingAllocation allocation = allocator.allocate(
                stocks, "BR001", new BigDecimal("120"), originalStartTime);

        assertTrue(allocation.isSuccess());
        assertEquals(LocalDateTime.of(2026, 6, 13, 11, 0), allocation.getTaskStartTime());
        assertEquals(10800, allocation.getDelaySeconds());
        assertEquals(new BigDecimal("120"), allocation.getAllocatedQuantity());
        assertEquals(new BigDecimal("80"), stocks.get(0).getAllocatedQuantity());
        assertEquals(new BigDecimal("40"), stocks.get(1).getAllocatedQuantity());
    }

    /**
     * 解封早的大卷被前序任务扣减后，后续任务只能使用剩余流水重新计算成熟时间。
     */
    @Test
    public void shouldExcludeAlreadyAllocatedQuantityWhenCalculatingNextTaskReleaseTime() {
        LocalDateTime originalStartTime = LocalDateTime.of(2026, 6, 13, 8, 0);
        List<Cd15BigRollAgingStock> stocks = Arrays.asList(
                stock("ROLL-A", "2026-06-13T08:00:00", "80"),
                stock("ROLL-B", "2026-06-13T11:00:00", "80"),
                stock("ROLL-C", "2026-06-13T13:00:00", "80"));

        Cd15BigRollAgingAllocation first = allocator.allocate(
                stocks, "BR001", new BigDecimal("120"), originalStartTime);
        Cd15BigRollAgingAllocation second = allocator.allocate(
                stocks, "BR001", new BigDecimal("80"), originalStartTime);

        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        assertEquals(LocalDateTime.of(2026, 6, 13, 13, 0), second.getTaskStartTime());
        assertEquals(new BigDecimal("80"), stocks.get(0).getAllocatedQuantity());
        assertEquals(new BigDecimal("80"), stocks.get(1).getAllocatedQuantity());
        assertEquals(new BigDecimal("40"), stocks.get(2).getAllocatedQuantity());
    }

    /**
     * 没有足够成熟流水时，返回静置期受限原因。
     */
    @Test
    public void shouldReturnAgingLimitWhenAvailableStocksCannotCoverTask() {
        Cd15BigRollAgingAllocation allocation = allocator.allocate(
                Arrays.asList(stock("ROLL-A", "2026-06-13T08:00:00", "50")),
                "BR001", new BigDecimal("80"), LocalDateTime.of(2026, 6, 13, 8, 0));

        assertFalse(allocation.isSuccess());
        assertEquals("AGING_PERIOD_LIMIT", allocation.getFailureReason());
    }

    private Cd15BigRollAgingStock stock(String sourceId, String releaseTime, String quantity) {
        return Cd15BigRollAgingStock.builder()
                .sourceId(sourceId)
                
                .bigRollCode("BR001")
                .availableQuantity(new BigDecimal(quantity))
                .allocatedQuantity(BigDecimal.ZERO)
                .releaseTime(LocalDateTime.parse(releaseTime))
                .build();
    }
}
