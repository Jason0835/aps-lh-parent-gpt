package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.algorithm.Cd90InventoryCalculator;
import com.zlt.aps.cd90.engine.algorithm.Cd90ScheduleCandidateBuilder;
import com.zlt.aps.cd90.engine.algorithm.Cd90ScheduleCandidateSorter;
import com.zlt.aps.cd90.engine.algorithm.Cd90StockGuaranteeCalculator;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.service.impl.Cd90ScheduleCandidatePreparationServiceImpl;
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
public class Cd90ScheduleCandidatePreparationServiceTest {

    private final Cd90ScheduleCandidatePreparationService service =
            new Cd90ScheduleCandidatePreparationServiceImpl(new Cd90ScheduleCandidateBuilder(
                    new Cd90InventoryCalculator(),
                    new Cd90StockGuaranteeCalculator(),
                    new Cd90ScheduleCandidateSorter()));

    /**
     * 直裁CLASS1从排程日前一日中班开始生产，对应首个成型供应班次为当日夜班22:00。
     */
    @Test
    public void class1ShouldStartFromPreviousDayNightFormingDemand() {
        List<Cd90ScheduleCandidate> result = service.prepare(context(2), input(
                shift("2026-06-12T14:00:00", "10"),
                shift("2026-06-12T22:00:00", "20"),
                shift("2026-06-13T06:00:00", "20")), "CLASS1");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("12"), result.get(0).getStockSupplyHours());
    }

    /**
     * 后续直裁班次每推进一班，成型供应起点同步推进8小时。
     */
    @Test
    public void class2ShouldAdvanceEightHours() {
        List<Cd90ScheduleCandidate> result = service.prepare(context(1), input(
                shift("2026-06-12T22:00:00", "20"),
                shift("2026-06-13T06:00:00", "20")), "CLASS2");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("8"), result.get(0).getStockSupplyHours());
    }

    private Cd90AutoScheduleContext context(int demandWindow) {
        return Cd90AutoScheduleContext.builder()
                .factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd90AutoScheduleParameters.builder().demandWindow(demandWindow).build())
                .build();
    }

    private Cd90AutoScheduleInput input(Cd90DemandShift... shifts) {
        return Cd90AutoScheduleInput.builder()
                .demandShifts(java.util.Arrays.asList(shifts))
                .stocksAtSix(Collections.singletonList(Cd90StockSource.builder()
                        .stockDate(LocalDate.of(2026, 6, 12))
                        .clothCode("CF001")
                        .stockQuantity(new BigDecimal("40"))
                        .build()))
                .build();
    }

    private Cd90DemandShift shift(String startTime, String demand) {
        return Cd90DemandShift.builder()
                .clothCode("CF001")
                .startTime(LocalDateTime.parse(startTime))
                .clothDemandQuantity(new BigDecimal(demand))
                .shiftHours(new BigDecimal("8"))
                .included(true)
                .build();
    }
}
