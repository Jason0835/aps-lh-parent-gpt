package com.zlt.aps.tm.engine.domain;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 胎面规则命中证据对象。
 *
 * <p>用于汇总参数命中、规则过滤、评分和未排证据，最终可写入解释表的 JSON 字段。
 * 该对象只追加证据，不修改任务链。</p>
 */
@Getter
public class TmRuleTrace {

    /** 规则命中明细 */
    private final List<TmRuleTraceItem> ruleHits = new ArrayList<>();

    /**
     * 追加规则证据。
     *
     * @param ruleCode 规则编码
     * @param result   规则结果
     * @param evidence 规则证据
     */
    public void addRuleHit(String ruleCode, String result, Object evidence) {
        ruleHits.add(new TmRuleTraceItem(ruleCode, result, evidence));
    }

    /**
     * 转换为解释 JSON 文本（使用 hutool JSONUtil）。
     *
     * @return JSON 文本，用于写入解释表字段
     */
    public String toExplainJson() {
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreNullValue(false);
        JSONArray array = JSONUtil.createArray(jsonConfig);
        for (TmRuleTraceItem item : ruleHits) {
            JSONObject obj = JSONUtil.createObj(jsonConfig);
            obj.set("ruleCode", item.getRuleCode());
            obj.set("result", item.getResult());
            obj.set("evidence", buildEvidence(item.getEvidence(), jsonConfig));
            array.add(obj);
        }
        return JSONUtil.toJsonPrettyStr(array);
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
