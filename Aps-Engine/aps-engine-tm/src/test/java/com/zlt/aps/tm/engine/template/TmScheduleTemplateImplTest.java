package com.zlt.aps.tm.engine.template;

import com.zlt.aps.tm.api.domain.vo.TmAutoScheduleResponseVo;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 胎面排程模板实现测试。
 *
 * <p>验证模板只负责编排步骤，并按第16章固定顺序调用各步骤服务。</p>
 */
public class TmScheduleTemplateImplTest {

    @Test
    public void executeShouldCallStepsInDesignedOrder() {
        List<String> calls = new ArrayList<>();
        TmScheduleTemplateImpl template = new TmScheduleTemplateImpl(
                context -> calls.add("bootstrap"),
                context -> calls.add("inventory"),
                context -> calls.add("plan"),
                context -> calls.add("sort"),
                context -> calls.add("assign"),
                context -> calls.add("snapshot"),
                null
        );
        TmScheduleContext context = new TmScheduleContext();
        context.setBatchNo("BATCH-001");
        context.setTraceId("TRACE-001");

        TmAutoScheduleResponseVo response = template.execute(context);

        assertEquals(Arrays.asList("bootstrap", "inventory", "plan", "sort", "assign", "snapshot"), calls);
        assertTrue(response.getSuccess());
        assertEquals("BATCH-001", response.getBatchNo());
    }
}
