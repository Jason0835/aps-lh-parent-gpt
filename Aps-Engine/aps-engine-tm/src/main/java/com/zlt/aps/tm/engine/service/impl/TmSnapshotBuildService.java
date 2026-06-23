package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.tm.engine.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        if (context != null && task != null) {
            result.setRuleHitJson(buildRuleHitJson(context.getRuleTraceMap().get(task.getBusinessKey())));
            List<TmMachineCandidate> candidates = context.getCandidateTraceMap().get(task.getBusinessKey());
            result.setCandidateMachineJson(buildCandidateMachineJson(candidates));
            result.setSelectedMachineScore(resolveSelectedMachineScore(task, candidates));
            result.setMachineSelectReason(buildMachineSelectReason(task, result.getSelectedMachineScore()));
            result.setAssignStatus(task.isUnassigned() ? "UNPLANNED" : "PLANNED");
        }
        result.setSysAnalysis(task == null ? "任务为空" : "已生成任务规则、候选机台和选机解释");
        return result;
    }

    /**
     * 解析选中机台评分。
     *
     * @param task       任务草稿
     * @param candidates 候选机台列表
     * @return 选中机台评分；未排时返回 null
     */
    private BigDecimal resolveSelectedMachineScore(TmTaskDraft task, List<TmMachineCandidate> candidates) {
        if (task == null || task.isUnassigned() || CollUtil.isEmpty(candidates)) {
            return null;
        }
        for (TmMachineCandidate candidate : candidates) {
            if (task.getMachineCode().equals(candidate.getMachineCode())) {
                return candidate.getScore();
            }
        }
        return null;
    }

    /**
     * 构建最终选机说明。
     *
     * @param task                 任务草稿
     * @param selectedMachineScore 选中机台评分
     * @return 选机说明
     */
    private String buildMachineSelectReason(TmTaskDraft task, BigDecimal selectedMachineScore) {
        if (task == null) {
            return "任务为空，无法选机";
        }
        if (task.isUnassigned()) {
            String reason = task.getUnplannedReasonDesc() == null ? task.getUnplannedReasonCode() : task.getUnplannedReasonDesc();
            return "未选中机台：" + reason;
        }
        return "选中机台 " + task.getMachineCode() + "，评分=" + selectedMachineScore + "，按默认过滤和评分规则选择";
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
