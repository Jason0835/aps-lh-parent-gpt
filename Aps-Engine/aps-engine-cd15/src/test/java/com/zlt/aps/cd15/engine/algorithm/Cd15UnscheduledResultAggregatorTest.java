package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.engine.model.Cd15ScheduleAttemptTrace;
import com.zlt.aps.cd15.engine.model.Cd15UnscheduledResultModel;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 窗口结束未排结果内存汇总测试。 */
public class Cd15UnscheduledResultAggregatorTest {

    private final Cd15UnscheduledResultAggregator aggregator =
            new Cd15UnscheduledResultAggregator(new Cd15UnscheduledReasonResolver());

    @Test
    public void shouldKeepReasonOrderDeduplicateAndAppendWindowLimit() {
        List<Cd15UnscheduledResultModel> result = aggregator.aggregate(Arrays.asList(
                trace("CLASS1", "C1", "BR1", "100", "0", "ROLL_TOOL_LIMIT", 1),
                trace("CLASS2", "C1", "BR1", "60", "20", "ROLL_TOOL_LIMIT", 2),
                trace("CLASS3", "C1", "BR1", "40", "10", "STORAGE_LANE_LIMIT", 3)
        ));

        assertEquals(3, result.size());
        assertEquals("ROLL_TOOL_LIMIT", result.get(0).getReasonCode());
        assertEquals(1, result.get(0).getReasonOrder());
        assertTrue(result.get(0).isPrimaryReason());
        assertEquals("STORAGE_LANE_LIMIT", result.get(1).getReasonCode());
        assertEquals("SCHEDULE_WINDOW_LIMIT", result.get(2).getReasonCode());
        assertEquals(new BigDecimal("100"), result.get(0).getDemandQuantity());
        assertEquals(new BigDecimal("30"), result.get(0).getScheduledQuantity());
        assertEquals(new BigDecimal("70"), result.get(0).getUnscheduledQuantity());
        assertEquals("ROLL_TOOL", result.get(2).getFailStage());
    }

    @Test
    public void shouldNotGenerateResultWhenDemandIsFullyScheduled() {
        List<Cd15UnscheduledResultModel> result = aggregator.aggregate(Arrays.asList(
                trace("CLASS1", "C1", "BR1", "100", "60", null, 1),
                trace("CLASS2", "C1", "BR1", "40", "40", null, 2)
        ));

        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldUseFirstPositiveDemandInsteadOfSummingRecalculatedDemand() {
        List<Cd15UnscheduledResultModel> result = aggregator.aggregate(Arrays.asList(
                trace("CLASS1", "C1", "BR1", "100", "40", null, 1),
                trace("CLASS2", "C1", "BR1", "60", "20", null, 2)
        ));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100"), result.get(0).getDemandQuantity());
        assertEquals(new BigDecimal("60"), result.get(0).getScheduledQuantity());
        assertEquals(new BigDecimal("40"), result.get(0).getUnscheduledQuantity());
        assertEquals("SCHEDULE_WINDOW_LIMIT", result.get(0).getReasonCode());
        assertEquals("SCHEDULE_WINDOW", result.get(0).getFailStage());
    }

    @Test
    public void shouldMapConstructionMissingToStableDataReason() {
        List<Cd15UnscheduledResultModel> result = aggregator.aggregate(Collections.singletonList(
                trace("CLASS1", "C1", null, "80", "0", "CONSTRUCTION_MISSING", 1)));

        assertEquals("DATA_MISSING", result.get(0).getReasonCode());
        assertEquals("DATA_PREPARATION", result.get(0).getFailStage());
        assertEquals("施工信息或必要基础数据缺失", result.get(0).getReasonDescription());
    }

    private Cd15ScheduleAttemptTrace trace(String classField, String steelStripCode,
                                           String bigRollCode, String demand,
                                           String scheduled, String reason,
                                           int sequence) {
        return Cd15ScheduleAttemptTrace.builder()
                .classField(classField).shiftCode(classField + "_SHIFT")
                .steelStripCode(steelStripCode).bigRollCode(bigRollCode)
                .netDemandQuantity(new BigDecimal(demand))
                .scheduledQuantity(new BigDecimal(scheduled))
                .failureReason(reason).sequence(sequence).build();
    }
}
