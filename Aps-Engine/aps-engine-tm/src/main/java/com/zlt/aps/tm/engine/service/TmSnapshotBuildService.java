package com.zlt.aps.tm.engine.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.tm.engine.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 胎面解释快照构建服务。
 *
 * <p>用于把运行态任务、规则证据和候选机台转换为解释表可保存的文本。骨架阶段只提供
 * JSON 文本拼装入口，不实现完整业务证据采集。</p>
 */
@Service
public class TmSnapshotBuildService {

    /**
     * 构建单任务解释快照。
     *
     * @param task    待解释任务
     * @param context 胎面排程上下文
     * @return 解释快照结果
     */
    public TmSnapshotBuildResult buildTaskExplain(TmTaskDraft task, TmScheduleContext context) {
        TmSnapshotBuildResult result = new TmSnapshotBuildResult();
        result.setSysAnalysis(task == null ? "任务为空" : "骨架阶段已生成任务解释入口");
        return result;
    }

    /**
     * 构建候选机台 JSON（使用 hutool JSONUtil）。
     *
     * @param candidates 候选机台列表
     * @return 候选机台 JSON 文本
     */
    public String buildCandidateMachineJson(List<TmMachineCandidate> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return "[]";
        }
        JSONArray array = new JSONArray();
        for (TmMachineCandidate candidate : candidates) {
            JSONObject obj = new JSONObject();
            obj.set("machineCode", candidate.getMachineCode());
            obj.set("remainCapacity", candidate.getRemainCapacity());
            obj.set("filtered", candidate.isFiltered());
            obj.set("filterReasonCode", candidate.getFilterReasonCode());
            obj.set("filterReasonDesc", candidate.getFilterReasonDesc());
            obj.set("filterEvidence", candidate.getFilterEvidence());
            obj.set("scoreResult", candidate.getScoreResult());
            array.add(obj);
        }
        return JSONUtil.toJsonPrettyStr(array);
    }

    /**
     * 构建规则命中 JSON。
     *
     * @param trace 规则证据
     * @return 规则命中 JSON 文本
     */
    public String buildRuleHitJson(TmRuleTrace trace) {
        return trace == null ? "[]" : trace.toExplainJson();
    }
}
