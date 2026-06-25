package com.zlt.aps.cd90.service;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleResultDraft;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleShiftSlotDraft;
import com.zlt.aps.cd90.engine.model.Cd90UnscheduledResultModel;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/** 自动排程输出草稿持久化映射测试。 */
public class Cd90AutoScheduleDraftMapperTest {

    private final Cd90AutoScheduleDraftMapper mapper = new Cd90AutoScheduleDraftMapper();

    @Test
    public void shouldMapFirstAndEighthShiftAndPersistenceDefaults() {
        Cd90ScheduleResultDraft draft = Cd90ScheduleResultDraft.builder()
                .resultKey("C1|BR1|M1").clothCode("C1").bigRollCode("BR1")
                .machineCode("M1").primaryLaneCode("L1").dataSource("0")
                .shiftSlots(Arrays.asList(slot("CLASS1", "10"), slot("CLASS8", "80"))).build();

        Cd90ScheduleResult result = mapper.toScheduleResult(
                "116", LocalDate.of(2026, 6, 14), "CD9020260614001",
                "CD90202606140010001", draft);

        assertEquals("L1", result.getStorageLaneCode());
        assertEquals(Double.valueOf(10), result.getClass1PlanQty());
        assertEquals(Double.valueOf(80), result.getClass8PlanQty());
        assertEquals("0", result.getIsRelease());
        assertEquals(Integer.valueOf(0), result.getIsLocked());
    }

    @Test
    public void shouldMapUnscheduledReasonWithoutRecalculation() {
        Cd90UnscheduledResultModel source = Cd90UnscheduledResultModel.builder()
                .clothCode("C9").bigRollCode("BR9")
                .demandQuantity(new BigDecimal("100"))
                .scheduledQuantity(new BigDecimal("40"))
                .unscheduledQuantity(new BigDecimal("60"))
                .reasonCode("STORAGE_LANE_LIMIT").reasonOrder(1)
                .primaryReason(true).failStage("STORAGE_LANE")
                .reasonDescription("库排容量不足").build();

        Cd90UnscheduleResult result = mapper.toUnscheduleResult(
                "116", new Date(), "CD9020260614001", source);

        assertEquals(Double.valueOf(60), result.getUnscheduledQty());
        assertEquals("1", result.getPrimaryReason());
        assertEquals("STORAGE_LANE_LIMIT", result.getReasonCode());
    }

    private Cd90ScheduleShiftSlotDraft slot(String classField, String quantity) {
        return Cd90ScheduleShiftSlotDraft.builder().classField(classField)
                .scheduleDate(LocalDate.of(2026, 6, 14))
                .planQuantity(new BigDecimal(quantity)).finishQuantity(BigDecimal.ZERO)
                .produceOrder(1).finishRate(BigDecimal.ZERO).build();
    }
}
