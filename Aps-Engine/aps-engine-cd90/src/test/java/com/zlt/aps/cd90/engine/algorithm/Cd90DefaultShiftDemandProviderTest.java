package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleParameters;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90InboundRecord;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** 默认班次需求提供器测试。 */
public class Cd90DefaultShiftDemandProviderTest {

    private final Cd90DefaultShiftDemandProvider provider =
            new Cd90DefaultShiftDemandProvider(new Cd90DemandCalculator(), new Cd90InboundResolver());

    @Test
    public void shouldCalculateNetDemandWithStockAndPriorPlannedInbound() {
        Cd90RollingScheduleContext rolling = Cd90RollingScheduleContext.builder()
                .actualInboundRecords(Collections.emptyList())
                .plannedInboundRecords(Collections.singletonList(Cd90InboundRecord.builder()
                        .taskKey("T1").clothCode("C1").laneCode("L1").vehicleCount(1)
                        .inboundQuantity(new BigDecimal("60"))
                        .inboundTime(LocalDateTime.of(2026, 6, 12, 21, 0)).build()))
                .build();

        Cd90ShiftDemandDecision result = provider.resolve(
                context(), input(), shift(), Cd90ScheduleCandidate.builder()
                        .clothCode("C1").build(), rolling);

        assertEquals(new BigDecimal("190"), result.getNetDemandQuantity());
    }

    @Test
    public void shouldSumConsumptionBeforeDirectCutShift() {
        BigDecimal result = provider.cumulativeConsumptionBeforeShift(
                context(), input(), shift());

        assertEquals(new BigDecimal("100"), result);
    }

    private Cd90AutoScheduleContext context() {
        return Cd90AutoScheduleContext.builder().scheduleDate(LocalDate.of(2026, 6, 13))
                .parameters(Cd90AutoScheduleParameters.builder().demandWindow(2)
                        .demandCalcMode("SUM").rollCoilMeter(new BigDecimal("100")).build())
                .build();
    }

    private Cd90AutoScheduleInput input() {
        return Cd90AutoScheduleInput.builder()
                .stocksAtSix(Collections.singletonList(Cd90StockSource.builder()
                        .clothCode("C1").stockQuantity(new BigDecimal("50")).build()))
                .demandShifts(Arrays.asList(
                        demand(LocalDateTime.of(2026, 6, 12, 6, 0), "100"),
                        demand(LocalDateTime.of(2026, 6, 12, 22, 0), "100"),
                        demand(LocalDateTime.of(2026, 6, 13, 6, 0), "100")))
                .build();
    }

    private Cd90DemandShift demand(LocalDateTime start, String quantity) {
        return Cd90DemandShift.builder().clothCode("C1").startTime(start)
                .clothDemandQuantity(new BigDecimal(quantity)).included(true)
                .shiftHours(new BigDecimal("8")).build();
    }

    private Cd90ShiftDescriptor shift() {
        return Cd90ShiftDescriptor.builder().classField("CLASS1").shiftCode("S1")
                .startTime(LocalDateTime.of(2026, 6, 12, 14, 0))
                .endTime(LocalDateTime.of(2026, 6, 12, 22, 0)).build();
    }
}
