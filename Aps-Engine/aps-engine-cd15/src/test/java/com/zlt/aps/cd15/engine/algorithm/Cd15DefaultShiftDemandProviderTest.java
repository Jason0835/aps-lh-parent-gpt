package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15InboundRecord;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 默认班次需求提供器测试。 */
public class Cd15DefaultShiftDemandProviderTest {

    private final Cd15DefaultShiftDemandProvider provider =
            new Cd15DefaultShiftDemandProvider(new Cd15DemandCalculator(),
                    new Cd15InboundResolver(), new Cd15FractionalDemandWindowSelector());

    @Test
    public void shouldCalculateNetDemandWithStockAndPriorPlannedInbound() {
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .actualInboundRecords(Collections.emptyList())
                .plannedInboundRecords(Collections.singletonList(Cd15InboundRecord.builder()
                        .taskKey("T1").steelStripCode("C1")
                        .laneCode("L1").vehicleCount(1)
                        .inboundQuantity(new BigDecimal("60"))
                        .inboundTime(LocalDateTime.of(2026, 6, 12, 21, 0)).build()))
                .build();

        Cd15ShiftDemandDecision result = provider.resolve(
                context(), input(), shift(), Cd15ScheduleCandidate.builder()
                        .steelStripCode("C1").materialKey("C1").build(), rolling);

        assertEquals(new BigDecimal("190"), result.getNetDemandQuantity());
    }

    @Test
    public void shouldConvertVehicleInboundByStandardCurlLengthBeforeFallbackParameter() {
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .actualInboundRecords(Collections.emptyList())
                .plannedInboundRecords(Collections.singletonList(Cd15InboundRecord.builder()
                        .taskKey("T1").steelStripCode("C1")
                        .laneCode("L1").vehicleCount(1)
                        .inboundTime(LocalDateTime.of(2026, 6, 12, 21, 0)).build()))
                .build();

        Cd15ShiftDemandDecision result = provider.resolve(
                context(), input(), shift(), Cd15ScheduleCandidate.builder()
                        .steelStripCode("C1").materialKey("C1").build(), rolling);

        assertEquals(new BigDecimal("170"), result.getNetDemandQuantity());
    }

    @Test
    public void shouldSumConsumptionBeforeDirectCutShift() {
        BigDecimal result = provider.cumulativeConsumptionBeforeShift(
                context(), input(), shift());

        assertEquals(new BigDecimal("100"), result);
    }


    @Test
    public void shouldForecastAverageDemandWhenDepthExceedsAvailableShifts() {
        Cd15ShiftDemandDecision result = provider.resolve(
                context("AVERAGE"), input("4", "100", "120"), shift(),
                Cd15ScheduleCandidate.builder().steelStripCode("C1").materialKey("C1").build(),
                Cd15RollingScheduleContext.builder().actualInboundRecords(Collections.emptyList())
                        .plannedInboundRecords(Collections.emptyList()).build());

        assertEquals(new BigDecimal("440"), result.getNetDemandQuantity());
    }

    @Test
    public void shouldForecastSumDemandWithMaxShiftWhenDepthExceedsAvailableShifts() {
        Cd15ShiftDemandDecision result = provider.resolve(
                context("SUM"), input("4", "100", "120"), shift(),
                Cd15ScheduleCandidate.builder().steelStripCode("C1").materialKey("C1").build(),
                Cd15RollingScheduleContext.builder().actualInboundRecords(Collections.emptyList())
                        .plannedInboundRecords(Collections.emptyList()).build());

        assertEquals(new BigDecimal("460"), result.getNetDemandQuantity());
    }
    /** 验证新增规格候选直接使用滚动快照中的提前需求剩余量。 */
    @Test
    public void shouldUseAdvanceRemainingAsNetDemand() {
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .newSpecAdvanceRemainingBySteelStrip(Collections.singletonMap(
                        "C1", new BigDecimal("300")))
                .build();

        Cd15ShiftDemandDecision result = this.provider.resolve(
                context(), input(), shift(), Cd15ScheduleCandidate.builder()
                        .steelStripCode("C1").materialKey("C1")
                        .newSpecAdvance(true).build(), rolling);

        assertEquals(new BigDecimal("300"), result.getNetDemandQuantity());
    }

    /** 验证计划需求视图去重后，原始成型需求仍参与累计消耗。 */
    @Test
    public void shouldKeepOriginalDemandForCumulativeConsumption() {
        Cd15AutoScheduleInput input = input();
        input.setPlanningDemandShifts(Collections.emptyList());

        BigDecimal result = this.provider.cumulativeConsumptionBeforeShift(
                context(), input, shift());

        assertEquals(new BigDecimal("100"), result);
    }

    /** 同一钢带下两个材料按稳定顺序共享库存，不能各自重复抵扣完整库存。 */
    @Test
    public void shouldAllocateSharedSteelStripStockOnceAcrossMaterials() {
        Cd15AutoScheduleInput input = Cd15AutoScheduleInput.builder()
                .stocksAtSix(Collections.singletonList(Cd15StockSource.builder()
                        .steelStripCode("C1")
                        .stockQuantity(new BigDecimal("150")).build()))
                .constructionMaterials(Collections.singletonList(
                        Cd15ConstructionMaterial.builder()
                                .steelStripCode("C1")
                                .curlLength(new BigDecimal("80")).build()))
                .depthClassQtyBySteelStrip(Collections.singletonMap(
                        "C1", BigDecimal.ONE))
                .demandShifts(Arrays.asList(
                        demand("M2", LocalDateTime.of(2026, 6, 12, 22, 0), "100"),
                        demand("M1", LocalDateTime.of(2026, 6, 12, 22, 0), "100")))
                .build();
        Cd15RollingScheduleContext rolling = Cd15RollingScheduleContext.builder()
                .actualInboundRecords(Collections.emptyList())
                .plannedInboundRecords(Collections.emptyList()).build();

        Cd15ShiftDemandDecision first = provider.resolve(
                context(), input, shift(), Cd15ScheduleCandidate.builder()
                        .steelStripCode("C1").materialKey("M1").build(), rolling);
        Cd15ShiftDemandDecision second = provider.resolve(
                context(), input, shift(), Cd15ScheduleCandidate.builder()
                        .steelStripCode("C1").materialKey("M2").build(), rolling);

        assertEquals(BigDecimal.ZERO, first.getNetDemandQuantity());
        assertEquals(new BigDecimal("50"), second.getNetDemandQuantity());
    }

    private Cd15AutoScheduleContext context() {
        return context("SUM");
    }

    private Cd15AutoScheduleContext context(String demandCalcMode) {
        return Cd15AutoScheduleContext.builder().scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd15AutoScheduleParameters.builder()
                        .demandCalcMode(demandCalcMode).rollCoilMeter(new BigDecimal("100")).build())
                .build();
    }

    private Cd15AutoScheduleInput input() {
        return Cd15AutoScheduleInput.builder()
                .stocksAtSix(Collections.singletonList(Cd15StockSource.builder()
                        .steelStripCode("C1").stockQuantity(new BigDecimal("50")).build()))
                .constructionMaterials(Collections.singletonList(Cd15ConstructionMaterial.builder()
                        .steelStripCode("C1").curlLength(new BigDecimal("80")).build()))
                .depthClassQtyBySteelStrip(Collections.singletonMap("C1", new BigDecimal("2")))
                .demandShifts(Arrays.asList(
                        demand(LocalDateTime.of(2026, 6, 12, 6, 0), "100"),
                        demand(LocalDateTime.of(2026, 6, 12, 22, 0), "100"),
                        demand(LocalDateTime.of(2026, 6, 13, 6, 0), "100")))
                .build();
    }


    private Cd15AutoScheduleInput input(String depthClassQty, String... quantities) {
        return Cd15AutoScheduleInput.builder()
                .stocksAtSix(Collections.emptyList())
                .constructionMaterials(Collections.singletonList(Cd15ConstructionMaterial.builder()
                        .steelStripCode("C1").curlLength(new BigDecimal("80")).build()))
                .depthClassQtyBySteelStrip(Collections.singletonMap("C1", new BigDecimal(depthClassQty)))
                .demandShifts(java.util.stream.IntStream.range(0, quantities.length)
                        .mapToObj(index -> demand(LocalDateTime.of(2026, 6, 12, 22, 0)
                                .plusHours(index * 8L), quantities[index]))
                        .collect(java.util.stream.Collectors.toList()))
                .build();
    }
    private Cd15DemandShift demand(LocalDateTime start, String quantity) {
        return Cd15DemandShift.builder().startTime(start)
                .steelStripCode("C1").materialKey("C1")
                .steelStripDemandQuantity(new BigDecimal(quantity)).included(true)
                .shiftHours(new BigDecimal("8")).build();
    }

    private Cd15DemandShift demand(
            String materialKey, LocalDateTime start, String quantity) {
        Cd15DemandShift demand = this.demand(start, quantity);
        demand.setMaterialKey(materialKey);
        return demand;
    }

    private Cd15ShiftDescriptor shift() {
        return Cd15ShiftDescriptor.builder().classField("CLASS1").shiftCode("S1")
                .startTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .endTime(LocalDateTime.of(2026, 6, 12, 22, 0)).build();
    }
}
