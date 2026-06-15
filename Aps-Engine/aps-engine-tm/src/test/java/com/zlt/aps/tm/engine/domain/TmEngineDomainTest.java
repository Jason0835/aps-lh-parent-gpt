package com.zlt.aps.tm.engine.domain;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 胎面引擎领域对象测试。
 *
 * <p>验证骨架阶段的业务键、候选机台过滤状态和规则证据 JSON 输出稳定。</p>
 */
public class TmEngineDomainTest {

    @Test
    public void taskDraftBusinessKeyShouldBeStable() {
        TmTaskDraft draft = new TmTaskDraft();
        draft.setOrderNo("ORD-1");
        draft.setTreadCode("TR-1");
        draft.setGlueCode("GL-1");
        draft.setMouthPlateCode("MP-1");

        assertEquals("ORD-1|TR-1|GL-1|MP-1", draft.getBusinessKey());
        assertTrue(draft.isUnassigned());
    }

    @Test
    public void machineCandidateShouldRecordFilterAndScoreResult() {
        TmMachineCandidate candidate = new TmMachineCandidate();
        candidate.setMachineCode("TM01");
        ScheduleScoreResult scoreResult = new ScheduleScoreResult();
        scoreResult.setTotalScore(BigDecimal.TEN);

        candidate.markFiltered("NO_SHIFT", "未开班", "shift closed");
        candidate.applyScore(scoreResult);

        assertTrue(candidate.isFiltered());
        assertEquals("NO_SHIFT", candidate.getFilterReasonCode());
        assertEquals(BigDecimal.TEN, candidate.getScoreResult().getTotalScore());
    }

    @Test
    public void ruleTraceShouldBuildJsonText() {
        TmRuleTrace trace = new TmRuleTrace();

        trace.addRuleHit("RULE_A", "PASS", "ok");
        String json = trace.toExplainJson();

        assertFalse(json.isEmpty());
        assertTrue(json.contains("RULE_A"));
        assertTrue(json.contains("PASS"));
    }

    @Test
    public void ruleTraceShouldUseJsonUtilityAndEscapeSpecialText() {
        TmRuleTrace trace = new TmRuleTrace();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("text", "A\"B\\C");
        evidence.put("emptyValue", null);

        trace.addRuleHit("RULE_JSON", "REJECT", evidence);
        String json = trace.toExplainJson();

        JSONArray array = JSONUtil.parseArray(json);
        JSONObject item = array.getJSONObject(0);
        assertEquals("RULE_JSON", item.getStr("ruleCode"));
        assertEquals("REJECT", item.getStr("result"));
        assertEquals("A\"B\\C", item.getJSONObject("evidence").getStr("text"));
        assertTrue(json.contains("\"emptyValue\": null"));
    }
}
