package com.zlt.aps.cd15.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

/** CD15自动排程来源追溯持久化映射测试。 */
public class Cd15AutoScheduleSourceTraceDraftMapperTest {

    @Test
    public void shouldMapSourceTraceFieldsToScheduleResult() {
        Cd15ScheduleResultDraft draft = Cd15ScheduleResultDraft.builder()
                .steelStripCode("S1").bigRollCode("BR1").machineCode("G1201")
                .classField("CLASS1").classIndex(1).scheduleDate(new java.util.Date())
                .cxBatchNo("B1,B2").cxMachineCodes("M1,M2")
                .planSurplusQty(new BigDecimal("30")).build();

        Cd15ScheduleResult result = new Cd15AutoScheduleDraftMapper(new ObjectMapper())
                .toScheduleResult("116", LocalDate.of(2026, 7, 13), "CD1520260713001", draft);

        assertEquals("B1,B2", result.getCxBatchNo());
        assertEquals("M1,M2", result.getCxMachineCodes());
        assertEquals(new BigDecimal("30"), result.getPlanSurplusQty());
    }
}
