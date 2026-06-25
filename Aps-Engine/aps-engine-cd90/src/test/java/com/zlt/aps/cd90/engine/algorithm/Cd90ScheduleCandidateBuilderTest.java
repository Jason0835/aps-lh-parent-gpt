package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 当前直裁班次候选规格构建测试。
 */
public class Cd90ScheduleCandidateBuilderTest {

    private final Cd90ScheduleCandidateBuilder builder = new Cd90ScheduleCandidateBuilder(
            new Cd90InventoryCalculator(),
            new Cd90StockGuaranteeCalculator(),
            new Cd90ScheduleCandidateSorter());

    /**
     * 当前供应班次前的成型消耗需要先从6点库存扣除。
     */
    @Test
    public void shouldDeductConsumptionBeforeCurrentWindow() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd90ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF001", "2026-06-12T06:00:00", "30"),
                shift("CF001", "2026-06-12T14:00:00", "30"),
                shift("CF001", "2026-06-12T22:00:00", "50"),
                shift("CF001", "2026-06-13T06:00:00", "20")
        ), Collections.singletonList(stock("CF001", "100")), currentStart, 2);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isShortageInCurrentShift());
        assertEquals(currentStart, result.get(0).getEarliestShortageTime());
        assertEquals(new BigDecimal("6.4"), result.get(0).getStockSupplyHours());
    }

    /**
     * 库存能满足当前班但不能满足下一班时，记录下一班为最早缺料时点。
     */
    @Test
    public void shouldFindEarliestShortageShift() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd90ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF001", "2026-06-12T22:00:00", "40"),
                shift("CF001", "2026-06-13T06:00:00", "40")
        ), Collections.singletonList(stock("CF001", "60")), currentStart, 2);

        assertFalse(result.get(0).isShortageInCurrentShift());
        assertEquals(LocalDateTime.of(2026, 6, 13, 6, 0),
                result.get(0).getEarliestShortageTime());
        assertEquals(new BigDecimal("12"), result.get(0).getStockSupplyHours());
    }

    /**
     * 当前窗口没有正需求的帘布不生成候选。
     */
    @Test
    public void shouldSkipClothWithoutPositiveDemandInWindow() {
        List<Cd90ScheduleCandidate> result = builder.build(Collections.singletonList(
                shift("CF001", "2026-06-12T22:00:00", "0")
        ), Collections.singletonList(stock("CF001", "60")),
                LocalDateTime.of(2026, 6, 12, 22, 0), 2);

        assertEquals(0, result.size());
    }

    /**
     * 构建结果直接使用稳定排序规则，当前班缺料规格排在前面。
     */
    @Test
    public void shouldReturnSortedCandidates() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd90ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF002", "2026-06-12T22:00:00", "30"),
                shift("CF001", "2026-06-12T22:00:00", "30")
        ), Arrays.asList(stock("CF001", "100"), stock("CF002", "10")), currentStart, 1);

        assertEquals("CF002", result.get(0).getClothCode());
        assertEquals("CF001", result.get(1).getClothCode());
    }

    private Cd90DemandShift shift(String clothCode, String startTime, String demand) {
        return Cd90DemandShift.builder()
                .clothCode(clothCode)
                .startTime(LocalDateTime.parse(startTime))
                .clothDemandQuantity(new BigDecimal(demand))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }

    private Cd90StockSource stock(String clothCode, String quantity) {
        return Cd90StockSource.builder()
                .stockDate(LocalDate.of(2026, 6, 12))
                .clothCode(clothCode)
                .stockQuantity(new BigDecimal(quantity))
                .build();
    }
}
