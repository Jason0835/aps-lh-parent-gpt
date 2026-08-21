package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleIssueVo;
import com.zlt.aps.tc.api.enums.TcMachineAssignStatusEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleResultEnum;
import com.zlt.aps.tc.api.enums.TcScheduleTaskStatusEnum;
import com.zlt.aps.tc.engine.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧解释快照构建服务。
 *
 * <p>用于把运行态任务、规则证据和候选机台转换为解释表可保存的版本化 JSON，
 * 业务阶段采集的计划量、过滤、评分、选机、未排和异常证据均在此形成落库快照。</p>
 */
@Service
public class TcSnapshotBuildService {

    /**
     * 构建单任务解释快照。
     *
     * @param task    待解释任务
     * @param context 胎侧排程上下文
     * @return 解释快照结果
     */
    public TcSnapshotBuildResult buildTaskExplain(TcTaskDraft task, TcScheduleContext context) {
        TcSnapshotBuildResult result = new TcSnapshotBuildResult();
        if (context != null && task != null) {
            TcRuleTrace ruleTrace = context.getRuleTraceMap().get(task.getBusinessKey());
            result.setRuleHitJson(buildRuleHitJson(ruleTrace));
            List<TcMachineCandidate> candidates = context.getCandidateTraceMap().get(task.getBusinessKey());
            result.setCandidateMachineJson(this.buildCandidateMachineJson(task, candidates));
            String assignStatus = resolveAssignStatus(task);
            result.setSelectedMachineScore(resolveSelectedMachineScore(task, candidates));
            result.setMachineSelectReason(buildMachineSelectReason(task, result.getSelectedMachineScore(), assignStatus));
            result.setAssignStatus(assignStatus);
            if (isUnplannedTask(task)) {
                result.setUnplannedEvidenceJson(buildUnplannedEvidenceJson(ruleTrace, task));
            }
            this.fillTaskIssues(result, task, context);
        }
        result.setSysAnalysis(I18nUtil.getMessage(task == null
                ? "ui.tc.schedule.snapshotTaskEmpty" : "ui.tc.schedule.snapshotGenerated"));
        return result;
    }

    /**
     * 解析解释表分配状态。
     *
     * @param task 任务草稿
     * @return 分配状态编码
     */
    private String resolveAssignStatus(TcTaskDraft task) {
        if (task == null || isUnplannedTask(task)) {
            return TcMachineAssignStatusEnum.UNPLANNED.getCode();
        }
        if (isNoProductionNeeded(task)) {
            return TcScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode();
        }
        return TcScheduleTaskStatusEnum.PLANNED.getCode();
    }

    /**
     * 判断任务是否属于未排任务。
     *
     * @param task 任务草稿
     * @return true 表示任务需要进入未排语义
     */
    private boolean isUnplannedTask(TcTaskDraft task) {
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
    private boolean isNoProductionNeeded(TcTaskDraft task) {
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
    private BigDecimal resolveSelectedMachineScore(TcTaskDraft task, List<TcMachineCandidate> candidates) {
        if (task == null || isUnplannedTask(task) || isNoProductionNeeded(task) || CollUtil.isEmpty(candidates)) {
            return null;
        }
        for (TcMachineCandidate candidate : candidates) {
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
    private String buildMachineSelectReason(TcTaskDraft task, BigDecimal selectedMachineScore, String assignStatus) {
        if (task == null) {
            return I18nUtil.getMessage("ui.tc.schedule.machineSelectTaskEmpty");
        }
        if (isUnplannedTask(task)) {
            String reason = task.getUnplannedReasonDesc() == null ? task.getUnplannedReasonCode() : task.getUnplannedReasonDesc();
            return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.machineNotSelected"), reason);
        }
        if (TcScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode().equals(assignStatus)) {
            return I18nUtil.getMessage("ui.tc.schedule.noProductionNeeded");
        }
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.machineSelected"),
                task.getMachineCode(), selectedMachineScore);
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
    private String buildUnplannedEvidenceJson(TcRuleTrace ruleTrace, TcTaskDraft task) {
        JSONObject obj = new JSONObject();
        obj.set("schemaVersion", "1");
        obj.set("reasonCode", task.getUnplannedReasonCode());
        obj.set("reasonDesc", task.getUnplannedReasonDesc());
        JSONArray rejectedCandidates = new JSONArray();
        JSONArray unplannedEvidences = new JSONArray();
        if (ruleTrace != null && ruleTrace.getRuleHits() != null) {
            for (TcRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                String ruleCode = item.getRuleCode();
                if (TcScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(ruleCode)
                        && TcScheduleRuleResultEnum.REJECT.getCode().equals(item.getResult())) {
                    rejectedCandidates.add(buildFilterEvidenceObject(item.getEvidence()));
                } else if (TcScheduleRuleCodeEnum.TOOL_LIMIT_UNPLANNED.getCode().equals(ruleCode)
                        || TcScheduleRuleCodeEnum.CAPACITY_OVERFLOW_UNPLANNED.getCode().equals(ruleCode)
                        || TcScheduleRuleCodeEnum.CAPACITY_BLOCKED_CARRYOVER.getCode().equals(ruleCode)
                        || (TcScheduleRuleCodeEnum.MACHINE_ASSIGN.getCode().equals(ruleCode)
                        && TcScheduleRuleResultEnum.REJECT.getCode().equals(item.getResult()))) {
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
     * @param task 当前任务，用于准确标识实际选中机台
     * @param candidates 候选机台列表
     * @return 候选机台 JSON 文本
     */
    public String buildCandidateMachineJson(TcTaskDraft task, List<TcMachineCandidate> candidates) {
        JSONObject result = new JSONObject();
        result.set("schemaVersion", "1");
        if (CollUtil.isEmpty(candidates)) {
            result.set("selected", null);
            result.set("candidates", new JSONArray());
            return JSONUtil.toJsonPrettyStr(result);
        }
        JSONArray array = new JSONArray();
        JSONObject selected = null;
        int rank = 0;
        for (TcMachineCandidate candidate : candidates) {
            rank++;
            JSONObject obj = new JSONObject();
            obj.set("machineCode", candidate.getMachineCode());
            obj.set("remainCapacity", candidate.getRemainCapacity());
            obj.set("filtered", candidate.isFiltered());
            obj.set("filterReasonCode", candidate.getFilterReasonCode());
            obj.set("filterReasonDesc", candidate.getFilterReasonDesc());
            obj.set("filterEvidence", candidate.getFilterEvidence());
            obj.set("scoreResult", candidate.getScoreResult());
            obj.set("rank", rank);
            array.add(obj);
            if (!candidate.isFiltered() && selected == null && task != null
                    && Objects.equals(task.getMachineCode(), candidate.getMachineCode())) {
                selected = new JSONObject();
                selected.set("machineCode", candidate.getMachineCode());
                selected.set("score", candidate.getScore());
                selected.set("reason", candidate.getScoreResult() == null
                        ? null : candidate.getScoreResult().getDescription());
            }
        }
        result.set("selected", selected);
        result.set("candidates", array);
        return JSONUtil.toJsonPrettyStr(result);
    }

    /**
     * 构建规则命中 JSON。
     *
     * @param trace 规则证据
     * @return 规则命中 JSON 文本
     */
    public String buildRuleHitJson(TcRuleTrace trace) {
        if (trace != null) {
            return trace.toExplainJson();
        }
        JSONObject result = new JSONObject();
        result.set("schemaVersion", "1");
        result.set("hits", new JSONArray());
        return JSONUtil.toJsonPrettyStr(result);
    }

    /**
     * 构建来源解释行可直接展示的顺延原因和实际承接摘要。
     *
     * @param task    来源解释任务
     * @param context 胎侧排程上下文
     * @return 顺延解释对象；没有顺延证据时返回空Map
     */
    public Map<String, Object> buildCarryoverExplanation(TcTaskDraft task, TcScheduleContext context) {
        TcRuleTrace trace = context == null || task == null || context.getRuleTraceMap() == null
                ? null : context.getRuleTraceMap().get(task.getBusinessKey());
        Map<String, Object> explanation = new LinkedHashMap<>();
        List<Map<String, Object>> targetAssignments = new ArrayList<>();
        if (trace == null || CollUtil.isEmpty(trace.getRuleHits())) {
            return explanation;
        }
        for (TcRuleTraceItem item : trace.getRuleHits()) {
            if (item == null || !(item.getEvidence() instanceof Map)) {
                continue;
            }
            String ruleCode = item.getRuleCode();
            if (!TcScheduleRuleCodeEnum.CAPACITY_BLOCKED_CARRYOVER.getCode().equals(ruleCode)
                    && !TcScheduleRuleCodeEnum.MACHINE_SHIFT_BLOCKED_CARRYOVER.getCode().equals(ruleCode)
                    && !TcScheduleRuleCodeEnum.PLAN_QTY_CARRYOVER.getCode().equals(ruleCode)) {
                continue;
            }
            Map<?, ?> evidence = (Map<?, ?>) item.getEvidence();
            if (evidence.get("sourceReasonDesc") != null) {
                explanation.putIfAbsent("sourceShiftOrder", evidence.get("sourceShiftOrder"));
                explanation.putIfAbsent("sourceReasonCode", evidence.get("sourceReasonCode"));
                explanation.putIfAbsent("sourceReasonDesc", evidence.get("sourceReasonDesc"));
                explanation.putIfAbsent("rejectReasonSummary", evidence.get("rejectReasonSummary"));
                explanation.putIfAbsent("candidateDetails", evidence.get("candidateDetails"));
                explanation.putIfAbsent("nextStep", evidence.get("nextStep"));
                explanation.putIfAbsent("planCalcOrderIndex", evidence.get("planCalcOrderIndex"));
                explanation.putIfAbsent("baseSortIndex", evidence.get("baseSortIndex"));
                explanation.putIfAbsent("machineAssignmentSequence", evidence.get("machineAssignmentSequence"));
            }
            if (evidence.get("targetShiftOrder") != null) {
                Map<String, Object> targetAssignment = new LinkedHashMap<>();
                targetAssignment.put("targetShiftOrder", evidence.get("targetShiftOrder"));
                targetAssignment.put("targetMachineCode", evidence.get("targetMachineCode"));
                targetAssignment.put("carryoverQty", evidence.get("carryoverQty"));
                targetAssignment.put("machineAssignmentSequence", evidence.get("machineAssignmentSequence"));
                targetAssignment.put("planCalcOrderIndex", evidence.get("planCalcOrderIndex"));
                targetAssignment.put("baseSortIndex", evidence.get("baseSortIndex"));
                targetAssignments.add(targetAssignment);
            }
        }
        if (!explanation.isEmpty() || !targetAssignments.isEmpty()) {
            explanation.put("targetAssignments", targetAssignments);
        }
        return explanation;
    }

    /**
     * 汇总当前任务关联的结构化异常。
     *
     * @param result 解释快照
     * @param task 当前任务
     * @param context 排程上下文
     */
    private void fillTaskIssues(TcSnapshotBuildResult result, TcTaskDraft task, TcScheduleContext context) {
        List<TcAutoScheduleIssueVo> taskIssues = context.getIssueCollector().getIssues().stream()
                .filter(issue -> issue.getSourceOrderNo() == null
                        || (task.getSourceOrderNos() != null
                        && task.getSourceOrderNos().contains(issue.getSourceOrderNo()))
                        || (issue.getSidewallCode() != null
                        && issue.getSidewallCode().equals(task.getSidewallCode())))
                .collect(Collectors.toList());
        JSONObject issueObject = new JSONObject();
        issueObject.set("schemaVersion", "1");
        issueObject.set("issues", taskIssues);
        result.setIssueJson(JSONUtil.toJsonPrettyStr(issueObject));
        result.setIssueLevel(taskIssues.stream()
                .map(TcAutoScheduleIssueVo::getLevel)
                .filter(StrUtil::isNotBlank)
                .min(Comparator.comparing(level -> "ERROR".equals(level) ? 0 : 1))
                .orElse(null));
    }
}
