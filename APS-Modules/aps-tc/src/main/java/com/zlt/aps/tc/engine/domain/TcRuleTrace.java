package com.zlt.aps.tc.engine.domain;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleResultEnum;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 胎侧规则命中证据对象。
 *
 * <p>用于汇总参数命中、规则过滤、评分和未排证据，最终可写入解释表的 JSON 字段。
 * 该对象只追加证据，不修改任务链。</p>
 */
@Getter
public class TcRuleTrace {

    /** 规则命中明细 */
    private final List<TcRuleTraceItem> ruleHits = new ArrayList<>();

    /**
     * 追加规则证据。
     *
     * @param ruleCode 规则编码
     * @param result   规则结果
     * @param evidence 规则证据
     */
    public void addRuleHit(TcScheduleRuleCodeEnum ruleCode, TcScheduleRuleResultEnum result, Object evidence) {
        ruleHits.add(new TcRuleTraceItem(ruleCode.getCode(), result.getCode(), evidence));
    }

    /**
     * 转换为解释 JSON 文本（使用 hutool JSONUtil）。
     *
     * @return JSON 文本，用于写入解释表字段
     */
    public String toExplainJson() {
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreNullValue(false);
        JSONArray array = JSONUtil.createArray(jsonConfig);
        for (TcRuleTraceItem item : ruleHits) {
            JSONObject obj = JSONUtil.createObj(jsonConfig);
            obj.set("ruleCode", item.getRuleCode());
            obj.set("result", item.getResult());
            obj.set("evidence", buildEvidence(item.getEvidence(), jsonConfig));
            array.add(obj);
        }
        JSONObject result = JSONUtil.createObj(jsonConfig);
        result.set("schemaVersion", "1");
        result.set("hits", array);
        return JSONUtil.toJsonPrettyStr(result);
    }

    private Object buildEvidence(Object evidence, JSONConfig jsonConfig) {
        if (evidence == null) {
            return null;
        }
        if (evidence instanceof Map) {
            return JSONUtil.parseObj(evidence, jsonConfig);
        }
        return evidence;
    }

}
