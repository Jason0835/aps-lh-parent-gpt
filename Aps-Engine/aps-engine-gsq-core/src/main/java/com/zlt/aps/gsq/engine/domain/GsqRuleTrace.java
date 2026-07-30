package com.zlt.aps.gsq.engine.domain;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 钢丝圈规则命中证据对象。
 *
 * <p>用于汇总参数命中、规则过滤、评分和未排证据，最终可写入排程结果表的解释 JSON 字段。
 * 该对象只追加证据，不修改任务链。</p>
 *
 * <p>对齐胎圈 {@code TqRuleTrace}，结构完全相同。</p>
 *
 * <p>使用方式：</p>
 * <pre>
 * GsqRuleTrace trace = context.getRuleTrace(steelRingCode);
 * trace.addRuleHit(GsqScheduleRuleCodeEnum.BACKUP_TRIGGER, GsqScheduleRuleResultEnum.TRIGGER,
 *         ImmutableMap.of("triggerClass", 1, "backupShiftCount", 5));
 * </pre>
 *
 * @author APS
 */
@Getter
public class GsqRuleTrace {

    /** 规则命中明细列表（按追加顺序保留） */
    private final List<GsqRuleTraceItem> ruleHits = new ArrayList<>();

    /**
     * 追加规则证据。
     *
     * @param ruleCode 规则编码
     * @param result   规则结果
     * @param evidence 规则证据（任意可序列化为 JSON 的对象）
     */
    public void addRuleHit(GsqScheduleRuleCodeEnum ruleCode, GsqScheduleRuleResultEnum result, Object evidence) {
        ruleHits.add(new GsqRuleTraceItem(ruleCode.getCode(), result.getCode(), evidence));
    }

    /**
     * 追加规则证据（使用自定义编码，用于未在枚举中定义的临时规则）。
     *
     * @param ruleCode 规则编码字符串
     * @param result   规则结果
     * @param evidence 规则证据
     */
    public void addRuleHit(String ruleCode, String result, Object evidence) {
        ruleHits.add(new GsqRuleTraceItem(ruleCode, result, evidence));
    }

    /**
     * 转换为解释 JSON 文本（使用 hutool JSONUtil，保留 null 字段）。
     *
     * @return JSON 文本，用于写入排程结果表 explain_json 字段；无证据时返回空数组 JSON
     */
    public String toExplainJson() {
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreNullValue(false);
        JSONArray array = JSONUtil.createArray(jsonConfig);
        for (GsqRuleTraceItem item : ruleHits) {
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

    /**
     * 构建证据 JSON 节点：Map 转 JSONObject，其他类型原样返回。
     *
     * @param evidence    原始证据对象
     * @param jsonConfig JSON 配置
     * @return JSON 可序列化对象
     */
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
