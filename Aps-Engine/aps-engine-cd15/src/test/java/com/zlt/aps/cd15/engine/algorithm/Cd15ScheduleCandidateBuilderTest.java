package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 当前斜裁班次候选规格构建测试。
 */
public class Cd15ScheduleCandidateBuilderTest {

    private final Cd15ScheduleCandidateBuilder builder = new Cd15ScheduleCandidateBuilder(
            new Cd15InventoryCalculator(),
            new Cd15StockGuaranteeCalculator(),
            new Cd15ScheduleCandidateSorter(),
            new Cd15FractionalDemandWindowSelector());

    /** 当前供应班次前的成型消耗需要先从6点库存扣除。 */
    @Test
    public void shouldDeductConsumptionBeforeCurrentWindow() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd15ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF001", "2026-06-12T06:00:00", "30"),
                shift("CF001", "2026-06-12T14:00:00", "30"),
                shift("CF001", "2026-06-12T22:00:00", "50"),
                shift("CF001", "2026-06-13T06:00:00", "20")
        ), Collections.singletonList(stock("CF001", "100")), currentStart,
                Collections.singletonMap("CF001", new BigDecimal("2")),
                Collections.emptyMap());

        assertEquals(1, result.size());
        assertTrue(result.get(0).isShortageInCurrentShift());
        assertEquals(currentStart, result.get(0).getEarliestShortageTime());
        assertEquals(new BigDecimal("6.4"), result.get(0).getStockSupplyHours());
    }

    /** 库存能满足当前班但不能满足下一班时，记录下一班为最早缺料时点。 */
    @Test
    public void shouldFindEarliestShortageShift() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd15ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF001", "2026-06-12T22:00:00", "40"),
                shift("CF001", "2026-06-13T06:00:00", "40")
        ), Collections.singletonList(stock("CF001", "60")), currentStart,
                Collections.singletonMap("CF001", new BigDecimal("2")),
                Collections.emptyMap());

        assertFalse(result.get(0).isShortageInCurrentShift());
        assertEquals(LocalDateTime.of(2026, 6, 13, 6, 0),
                result.get(0).getEarliestShortageTime());
        assertEquals(new BigDecimal("12"), result.get(0).getStockSupplyHours());
    }

    /** 当前窗口没有正需求的钢带不生成候选，也不强制要求该钢带深度。 */
    @Test
    public void shouldSkipClothWithoutPositiveDemandInWindow() {
        List<Cd15ScheduleCandidate> result = builder.build(Collections.singletonList(
                shift("CF001", "2026-06-12T22:00:00", "0")
        ), Collections.singletonList(stock("CF001", "60")),
                LocalDateTime.of(2026, 6, 12, 22, 0), Collections.emptyMap(),
                Collections.emptyMap());

        assertEquals(0, result.size());
    }

    /** 构建结果直接使用稳定排序规则，当前班缺料规格排在前面。 */
    @Test
    public void shouldReturnSortedCandidates() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        List<Cd15ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF002", "2026-06-12T22:00:00", "30"),
                shift("CF001", "2026-06-12T22:00:00", "30")
        ), Arrays.asList(stock("CF001", "100"), stock("CF002", "10")), currentStart,
                depths("CF001", "CF002"), Collections.emptyMap());

        assertEquals("CF002", result.get(0).getSteelStripCode());
        assertEquals("CF001", result.get(1).getSteelStripCode());
    }

    /** 累计成型消耗 > 0 的规格标记为续作，不在累计 map 或累计为 0 的规格不标记。 */
    @Test
    public void shouldMarkContinueFromPreviousShift() {
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 12, 22, 0);
        Map<String, BigDecimal> cumulative = new java.util.HashMap<>();
        cumulative.put("CF001", new BigDecimal("130"));
        cumulative.put("CF002", BigDecimal.ZERO);
        List<Cd15ScheduleCandidate> result = builder.build(Arrays.asList(
                shift("CF001", "2026-06-12T22:00:00", "30"),
                shift("CF002", "2026-06-12T22:00:00", "30"),
                shift("CF003", "2026-06-12T22:00:00", "30")
        ), Arrays.asList(stock("CF001", "100"), stock("CF002", "100"), stock("CF003", "100")),
                currentStart, depths("CF001", "CF002", "CF003"), cumulative);

        Cd15ScheduleCandidate cf001 = result.stream()
                .filter(item -> "CF001".equals(item.getSteelStripCode())).findFirst().orElse(null);
        Cd15ScheduleCandidate cf002 = result.stream()
                .filter(item -> "CF002".equals(item.getSteelStripCode())).findFirst().orElse(null);
        Cd15ScheduleCandidate cf003 = result.stream()
                .filter(item -> "CF003".equals(item.getSteelStripCode())).findFirst().orElse(null);
        assertTrue("CF001 累计消耗 130 应标记为续作", cf001.isContinueFromPreviousShift());
        assertFalse("CF002 累计消耗为 0 不应标记为续作", cf002.isContinueFromPreviousShift());
        assertFalse("CF003 不在累计 map 不应标记为续作", cf003.isContinueFromPreviousShift());
    }

    private Map<String, BigDecimal> depths(String... steelStripCodes) {
        return Stream.of(steelStripCodes).collect(Collectors.toMap(
                steelStripCode -> steelStripCode, steelStripCode -> BigDecimal.ONE));
    }

    private Cd15DemandShift shift(String steelStripCode, String startTime, String demand) {
        return Cd15DemandShift.builder()
                .steelStripCode(steelStripCode).materialKey(steelStripCode)
                .startTime(LocalDateTime.parse(startTime))
                .steelStripDemandQuantity(new BigDecimal(demand))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }

    private Cd15StockSource stock(String steelStripCode, String quantity) {
        return Cd15StockSource.builder()
                .stockDate(LocalDate.of(2026, 6, 12))
                .steelStripCode(steelStripCode)
                .stockQuantity(new BigDecimal(quantity))
                .build();
    }
}
