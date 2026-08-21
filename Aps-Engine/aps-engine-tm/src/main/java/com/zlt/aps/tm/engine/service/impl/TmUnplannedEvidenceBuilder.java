package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.tm.api.enums.TmScheduleRuleCodeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleRuleResultEnum;
import com.zlt.aps.tm.engine.domain.TmRuleTrace;
import com.zlt.aps.tm.engine.domain.TmRuleTraceItem;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;

import java.util.Map;

/**
 * 胎面未排解释证据 JSON 构建器。
 *
 * <p>本类只读取既有规则命中记录生成未排证据文本，不采集规则、不写入上下文或持久化对象；
 * 解释快照服务继续在原调用点立即接收构建结果。</p>
 */
final class TmUnplannedEvidenceBuilder {

    /**
     * 从规则命中记录构建未排证据 JSON。
     *
     * @param ruleTrace 规则证据
     * @param task 未排任务
     * @return 未排证据 JSON 文本；无证据时返回仅含原因码的 JSON
     */
    String build(TmRuleTrace ruleTrace, TmTaskDraft task) {
        JSONObject obj = new JSONObject();
        obj.set("reasonCode", task.getUnplannedReasonCode());
        obj.set("reasonDesc", task.getUnplannedReasonDesc());
        JSONArray rejectedCandidates = new JSONArray();
        JSONArray unplannedEvidences = new JSONArray();
        if (ruleTrace != null && ruleTrace.getRuleHits() != null) {
            for (TmRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                String ruleCode = item.getRuleCode();
                if (TmScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(ruleCode)
                        && TmScheduleRuleResultEnum.REJECT.getCode().equals(item.getResult())) {
                    rejectedCandidates.add(this.buildFilterEvidenceObject(item.getEvidence()));
                    continue;
                }
                if (this.isUnplannedEvidence(ruleCode, item.getResult())) {
                    JSONObject evidenceObject = new JSONObject();
                    evidenceObject.set("ruleCode", ruleCode);
                    evidenceObject.set("result", item.getResult());
                    evidenceObject.set("evidence", item.getEvidence());
                    unplannedEvidences.add(evidenceObject);
                }
            }
        }
        obj.set("rejectedCandidates", rejectedCandidates);
        obj.set("unplannedEvidences", unplannedEvidences);
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 判断规则命中是否需要写入未排证据列表。
     *
     * @param ruleCode 规则编码
     * @param ruleResult 规则结果编码
     * @return true 表示需要写入未排证据
     */
    private boolean isUnplannedEvidence(String ruleCode, String ruleResult) {
        return TmScheduleRuleCodeEnum.TOOL_LIMIT_UNPLANNED.getCode().equals(ruleCode)
                || TmScheduleRuleCodeEnum.CAPACITY_OVERFLOW_UNPLANNED.getCode().equals(ruleCode)
                || TmScheduleRuleCodeEnum.CAPACITY_BLOCKED_CARRYOVER.getCode().equals(ruleCode)
                || TmScheduleRuleCodeEnum.MACHINE_SHIFT_BLOCKED_CARRYOVER.getCode().equals(ruleCode)
                || (TmScheduleRuleCodeEnum.MACHINE_ASSIGN.getCode().equals(ruleCode)
                && TmScheduleRuleResultEnum.REJECT.getCode().equals(ruleResult));
    }

    /**
     * 将机台过滤证据转换为精简 JSON 对象。
     *
     * @param evidence 过滤证据
     * @return 精简 JSON 对象
     */
    private JSONObject buildFilterEvidenceObject(Object evidence) {
        JSONObject obj = new JSONObject();
        if (evidence instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) evidence;
            obj.set("machineCode", map.get("machineCode"));
            obj.set("filterReasonCode", map.get("reasonCode"));
            obj.set("filterReasonDesc", map.get("reasonDesc"));
        }
        return obj;
    }
}
