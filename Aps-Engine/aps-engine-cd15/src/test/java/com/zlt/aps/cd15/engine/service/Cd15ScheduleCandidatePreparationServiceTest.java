package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.algorithm.Cd15FractionalDemandWindowSelector;
import com.zlt.aps.cd15.engine.algorithm.Cd15InventoryCalculator;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15ScheduleCandidateSorter;
import com.zlt.aps.cd15.engine.algorithm.Cd15StockGuaranteeCalculator;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceInfo;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.service.impl.Cd15ScheduleCandidatePreparationServiceImpl;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 当前班次候选规格内存准备服务测试。
 */
public class Cd15ScheduleCandidatePreparationServiceTest {

    private final Cd15ScheduleCandidatePreparationService service =
            new Cd15ScheduleCandidatePreparationServiceImpl(new Cd15ScheduleCandidateBuilder(
                    new Cd15InventoryCalculator(),
                    new Cd15StockGuaranteeCalculator(),
                    new Cd15ScheduleCandidateSorter(),
                    new Cd15FractionalDemandWindowSelector()),
                    new Cd15ScheduleCandidateSorter());

    /**
     * 斜裁CLASS1从排程日前一日中班开始生产，对应首个成型供应班次为当日夜班22:00。
     */
    @Test
    public void class1ShouldStartFromPreviousDayNightFormingDemand() {
        List<Cd15ScheduleCandidate> result = service.prepare(context(), input("2",
                shift("2026-06-12T14:00:00", "10"),
                shift("2026-06-12T22:00:00", "20"),
                shift("2026-06-13T06:00:00", "20")), "CLASS1", null);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("12"), result.get(0).getStockSupplyHours());
    }

    /**
     * 后续斜裁班次每推进一班，成型供应起点同步推进8小时。
     */
    @Test
    public void class2ShouldAdvanceEightHours() {
        List<Cd15ScheduleCandidate> result = service.prepare(context(), input("1",
                shift("2026-06-12T22:00:00", "20"),
                shift("2026-06-13T06:00:00", "20")), "CLASS2", null);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("8"), result.get(0).getStockSupplyHours());
    }

    /** 验证常规计划需求已去重时仍从滚动剩余量注入新增规格候选。 */
    @Test
    public void shouldAppendNewSpecAdvanceCandidateWithoutRegularDemand() {
        Cd15AutoScheduleInput input = input("1",
                shift("2026-06-13T06:00:00", "20"));
        input.setPlanningDemandShifts(Collections.emptyList());
        Cd15NewSpecAdvanceInfo info = Cd15NewSpecAdvanceInfo.builder()
                .analysis("新增规格提前生产").build();
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .continueDemandBySteelStrip(Collections.emptyMap())
                .newSpecAdvanceInfoBySteelStrip(Collections.singletonMap("CF001", info))
                .newSpecAdvanceRemainingBySteelStrip(Collections.singletonMap(
                        "CF001", new BigDecimal("300")))
                .normalizedNewSpecAdvanceSteelStripCodes(Collections.singleton("CF001"))
                .build();

        List<Cd15ScheduleCandidate> result = this.service.prepare(
                context(), input, "CLASS1", rolling);

        assertEquals(1, result.size());
        assertEquals("CF001", result.get(0).getSteelStripCode());
        assertEquals(true, result.get(0).isNewSpecAdvance());
        assertEquals(true, result.get(0).isNewSpecAdvanceQuantityNormalized());
        assertEquals("新增规格提前生产", result.get(0).getNewSpecAdvanceAnalysis());
    }
    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder()
                .factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd15AutoScheduleParameters.builder().build())
                .build();
    }

    private Cd15AutoScheduleInput input(String depth, Cd15DemandShift... shifts) {
        return Cd15AutoScheduleInput.builder()
                .demandShifts(java.util.Arrays.asList(shifts))
                .depthClassQtyBySteelStrip(Collections.singletonMap("CF001", new BigDecimal(depth)))
                .stocksAtSix(Collections.singletonList(Cd15StockSource.builder()
                        .stockDate(LocalDate.of(2026, 6, 12))
                        .steelStripCode("CF001")
                        .stockQuantity(new BigDecimal("40"))
                        .build()))
                .build();
    }

    private Cd15DemandShift shift(String startTime, String demand) {
        return Cd15DemandShift.builder()
                .steelStripCode("CF001").materialKey("CF001")
                .startTime(LocalDateTime.parse(startTime))
                .steelStripDemandQuantity(new BigDecimal(demand))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }
}
