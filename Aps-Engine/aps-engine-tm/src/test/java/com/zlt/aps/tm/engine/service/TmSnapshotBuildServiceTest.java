package com.zlt.aps.tm.engine.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.tm.engine.domain.TmMachineCandidate;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * 胎面解释快照构建服务测试。
 *
 * <p>验证候选机台 JSON 使用 Hutool JSON 能力生成，字段覆盖过滤原因、证据和评分结果。</p>
 */
public class TmSnapshotBuildServiceTest {

    @Test
    public void buildCandidateMachineJsonShouldReturnEmptyArrayWhenCandidateListEmpty() {
        TmSnapshotBuildService service = new TmSnapshotBuildService();

        assertEquals("[]", service.buildCandidateMachineJson(Collections.emptyList()));
        assertEquals("[]", service.buildCandidateMachineJson(null));
    }

    @Test
    public void buildCandidateMachineJsonShouldIncludeFilterEvidenceAndScore() {
        TmSnapshotBuildService service = new TmSnapshotBuildService();
        TmMachineCandidate candidate = new TmMachineCandidate();
        candidate.setMachineCode("TM01");
        candidate.setRemainCapacity(new BigDecimal("12.5"));
        candidate.markFiltered("NO_CAPACITY", "产能不足", Collections.singletonMap("remain", "12.5"));
        ScheduleScoreResult scoreResult = new ScheduleScoreResult();
        scoreResult.setStrategyCode("DEFAULT");
        scoreResult.setTotalScore(new BigDecimal("88"));
        candidate.applyScore(scoreResult);

        String json = service.buildCandidateMachineJson(Collections.singletonList(candidate));

        JSONArray array = JSONUtil.parseArray(json);
        JSONObject item = array.getJSONObject(0);
        assertEquals("TM01", item.getStr("machineCode"));
        assertEquals(new BigDecimal("12.5"), item.getBigDecimal("remainCapacity"));
        assertTrue(item.getBool("filtered"));
        assertEquals("NO_CAPACITY", item.getStr("filterReasonCode"));
        assertEquals("产能不足", item.getStr("filterReasonDesc"));
        assertEquals("12.5", item.getJSONObject("filterEvidence").getStr("remain"));
        assertFalse(item.getJSONObject("scoreResult").isEmpty());
    }
}
