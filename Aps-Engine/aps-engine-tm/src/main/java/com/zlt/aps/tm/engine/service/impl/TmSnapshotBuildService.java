package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zlt.aps.tm.api.enums.TmMachineAssignStatusEnum;
import com.zlt.aps.tm.api.enums.TmScheduleRuleCodeEnum;
import com.zlt.aps.tm.api.enums.TmScheduleRuleResultEnum;
import com.zlt.aps.tm.api.enums.TmScheduleTaskStatusEnum;
import com.zlt.aps.tm.engine.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
            TmRuleTrace ruleTrace = context.getRuleTraceMap().get(task.getBusinessKey());
            result.setRuleHitJson(buildRuleHitJson(ruleTrace));
            List<TmMachineCandidate> candidates = context.getCandidateTraceMap().get(task.getBusinessKey());
            result.setCandidateMachineJson(buildCandidateMachineJson(candidates));
            String assignStatus = resolveAssignStatus(task);
            result.setSelectedMachineScore(resolveSelectedMachineScore(task, candidates));
            result.setMachineSelectReason(buildMachineSelectReason(task, result.getSelectedMachineScore(), assignStatus));
            result.setAssignStatus(assignStatus);
            if (isUnplannedTask(task)) {
                result.setUnplannedEvidenceJson(buildUnplannedEvidenceJson(ruleTrace, task));
            }
        }
        result.setSysAnalysis(task == null ? "任务为空" : "已生成任务规则、候选机台和选机解释");
        return result;
    }

    /**
     * 解析解释表分配状态。
     *
     * @param task 任务草稿
     * @return 分配状态编码
     */
    private String resolveAssignStatus(TmTaskDraft task) {
        if (task == null || isUnplannedTask(task)) {
            return TmMachineAssignStatusEnum.UNPLANNED.getCode();
        }
        if (isNoProductionNeeded(task)) {
            return TmScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode();
        }
        return TmScheduleTaskStatusEnum.PLANNED.getCode();
    }

    /**
     * 判断任务是否属于未排任务。
     *
     * @param task 任务草稿
     * @return true 表示任务需要进入未排语义
     */
    private boolean isUnplannedTask(TmTaskDraft task) {
        if (task == null) {
            return false;
        }
        if (StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
            return true;
        }
        return task.isUnassigned() && task.getPlanQty() != null
                && task.getPlanQty().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断任务是否无需生产。
     *
     * @param task 任务草稿
     * @return true 表示最终计划量为空或小于等于 0，且不是未排任务
     */
    private boolean isNoProductionNeeded(TmTaskDraft task) {
        return task != null && !isUnplannedTask(task)
                && (task.getPlanQty() == null || task.getPlanQty().compareTo(BigDecimal.ZERO) <= 0);
    }

    /**
     * 解析选中机台评分。
     *
     * @param task       任务草稿
     * @param candidates 候选机台列表
     * @return 选中机台评分；未排或无需生产时返回 null
     */
    private BigDecimal resolveSelectedMachineScore(TmTaskDraft task, List<TmMachineCandidate> candidates) {
        if (task == null || isUnplannedTask(task) || isNoProductionNeeded(task) || CollUtil.isEmpty(candidates)) {
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
     * @param assignStatus         分配状态编码
     * @return 选机说明
     */
    private String buildMachineSelectReason(TmTaskDraft task, BigDecimal selectedMachineScore, String assignStatus) {
        if (task == null) {
            return "任务为空，无法选机";
        }
        if (isUnplannedTask(task)) {
            String reason = task.getUnplannedReasonDesc() == null ? task.getUnplannedReasonCode() : task.getUnplannedReasonDesc();
            return "未选中机台：" + reason;
        }
        if (TmScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode().equals(assignStatus)) {
            return "无需排产：最终计划量为0，保留任务解释但不占用机台产能";
        }
        return "选中机台 " + task.getMachineCode() + "，评分=" + selectedMachineScore + "，按默认过滤和评分规则选择";
    }

    /**
     * 构建未排证据 JSON。
     *
     * <p>从规则证据中提取候选机台过滤、工装/产能溢出和选机拒绝等未排相关证据，组装为精简 JSON，
     * 写入解释表和未排表的 UNPLANNED_EVIDENCE_JSON 字段，便于未排原因追溯。</p>
     *
     * @param ruleTrace  规则证据
     * @param task       未排任务
     * @return 未排证据 JSON 文本；无证据时返回仅含原因码的 JSON
     */
    private String buildUnplannedEvidenceJson(TmRuleTrace ruleTrace, TmTaskDraft task) {
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
                    rejectedCandidates.add(buildFilterEvidenceObject(item.getEvidence()));
                } else if (TmScheduleRuleCodeEnum.TOOL_LIMIT_UNPLANNED.getCode().equals(ruleCode)
                        || TmScheduleRuleCodeEnum.CAPACITY_OVERFLOW_UNPLANNED.getCode().equals(ruleCode)
                        || (TmScheduleRuleCodeEnum.MACHINE_ASSIGN.getCode().equals(ruleCode)
                        && TmScheduleRuleResultEnum.REJECT.getCode().equals(item.getResult()))) {
                    JSONObject evObj = new JSONObject();
                    evObj.set("ruleCode", ruleCode);
                    evObj.set("result", item.getResult());
                    evObj.set("evidence", item.getEvidence());
                    unplannedEvidences.add(evObj);
                }
            }
        }
        obj.set("rejectedCandidates", rejectedCandidates);
        obj.set("unplannedEvidences", unplannedEvidences);
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 将机台过滤证据 Map 转换为精简 JSON 对象。
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
