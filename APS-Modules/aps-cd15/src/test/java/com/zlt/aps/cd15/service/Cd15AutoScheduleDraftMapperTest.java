package com.zlt.aps.cd15.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleResultDraft;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 自动排程草稿分裁组号映射测试。 */
public class Cd15AutoScheduleDraftMapperTest {

    private final Cd15AutoScheduleDraftMapper mapper =
            new Cd15AutoScheduleDraftMapper();

    /** 单规格分裁虽无异规格稳定组键，落库时仍以工单号作为组号。 */
    @Test
    public void shouldUseOrderNumberAsSingleSpecSplitGroupNumber() {
        Cd15ScheduleResult result = mapper.toScheduleResult(
                "116", LocalDate.of(2026, 7, 18), "BATCH-1", "ORDER-1",
                Cd15ScheduleResultDraft.builder()
                        .cutMode("SPLIT")
                        .steelStripCode("CSS14016")
                        .build());

        assertEquals("ORDER-1", result.getGroupNo());
    }

    /** 普通单裁仍不生成分裁组号。 */
    @Test
    public void shouldKeepSingleCutGroupNumberEmpty() {
        Cd15ScheduleResult result = mapper.toScheduleResult(
                "116", LocalDate.of(2026, 7, 18), "BATCH-1", "ORDER-1",
                Cd15ScheduleResultDraft.builder()
                        .cutMode("SINGLE")
                        .steelStripCode("CSS14016")
                        .build());

        assertNull(result.getGroupNo());
    }
}
