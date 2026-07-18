package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 新增规格提前生产规则解析测试。
 */
public class Cd15NewSpecAdvanceResolverTest {

    private final Cd15NewSpecAdvanceResolver resolver = new Cd15NewSpecAdvanceResolver();

    /** 验证前瞻窗口内全部需求被归并，窗口外需求保留。 */
    @Test
    public void shouldMoveAllDemandInAdvanceWindowAndKeepDemandOutsideWindow() {
        List<Cd15DemandShift> demands = Arrays.asList(
                this.demand("C01", "D1", "2026-07-09T06:00:00", "100"),
                this.demand("C01", "D2", "2026-07-10T14:00:00", "200"),
                this.demand("C01", "D3", "2026-07-11T06:00:00", "300"));

        Cd15NewSpecAdvanceResult result = this.resolver.resolve(
                LocalDate.of(2026, 7, 9), 10, 2, demands, Collections.emptySet());

        Cd15NewSpecAdvanceInfo info = result.getAdvanceInfoBySteelStrip().get("C01");
        assertEquals(new BigDecimal("300"), info.getAdvanceDemandQuantity());
        assertEquals(LocalDate.of(2026, 7, 8), info.getTargetProductionDate());
        assertEquals(Arrays.asList(LocalDate.of(2026, 7, 9), LocalDate.of(2026, 7, 10)),
                info.getSourceDemandDates());
        assertEquals(BigDecimal.ZERO, this.quantity(result.getAdjustedDemandShifts(), "D1"));
        assertEquals(BigDecimal.ZERO, this.quantity(result.getAdjustedDemandShifts(), "D2"));
        assertEquals(new BigDecimal("300"), this.quantity(result.getAdjustedDemandShifts(), "D3"));
    }

    /** 验证历史回看窗口内已有实际生产的钢带不提前。 */
    @Test
    public void shouldNotAdvanceWhenClothHasActualFinishInLookbackWindow() {
        List<Cd15DemandShift> demands = Collections.singletonList(
                this.demand("C01", "D1", "2026-07-09T06:00:00", "100"));

        Cd15NewSpecAdvanceResult result = this.resolver.resolve(
                LocalDate.of(2026, 7, 9), 10, 2, demands, Collections.singleton("C01"));

        assertFalse(result.getAdvanceInfoBySteelStrip().containsKey("C01"));
        assertEquals(new BigDecimal("100"), result.getAdjustedDemandShifts().get(0)
                .getSteelStripDemandQuantity());
    }

    /** 验证后续班次可按首班快照重新生成去重计划需求。 */
    @Test
    public void shouldApplyFirstShiftSnapshotToReloadedDemand() {
        List<Cd15DemandShift> first = Collections.singletonList(
                this.demand("C01", "D1", "2026-07-09T06:00:00", "100"));
        Cd15NewSpecAdvanceResult snapshot = this.resolver.resolve(
                LocalDate.of(2026, 7, 9), 10, 2, first, Collections.emptySet());
        List<Cd15DemandShift> reloaded = Collections.singletonList(
                this.demand("C01", "D1", "2026-07-09T06:00:00", "120"));

        List<Cd15DemandShift> adjusted = this.resolver.applySnapshot(
                reloaded, snapshot.getAdvanceInfoBySteelStrip());

        assertEquals(BigDecimal.ZERO, adjusted.get(0).getSteelStripDemandQuantity());
        assertFalse(adjusted.get(0).isIncluded());
        assertEquals(new BigDecimal("120"), reloaded.get(0).getSteelStripDemandQuantity());
    }

    /** 构建成型需求明细。 */
    private Cd15DemandShift demand(String steelStripCode, String shiftKey,
                                   String startTime, String quantity) {
        return Cd15DemandShift.builder()
                .steelStripCode(steelStripCode).classField("CLASS1").shiftKey(shiftKey)
                .startTime(LocalDateTime.parse(startTime))
                .formingQuantity(BigDecimal.ONE)
                .steelStripDemandQuantity(new BigDecimal(quantity))
                .shiftHours(new BigDecimal("8"))
                .windowWeight(BigDecimal.ONE)
                .included(true).stopped(false).build();
    }

    /** 按班次键取调整后需求量。 */
    private BigDecimal quantity(List<Cd15DemandShift> demands, String shiftKey) {
        return demands.stream()
                .filter(item -> shiftKey.equals(item.getShiftKey()))
                .map(Cd15DemandShift::getSteelStripDemandQuantity)
                .findFirst().orElse(null);
    }
}
