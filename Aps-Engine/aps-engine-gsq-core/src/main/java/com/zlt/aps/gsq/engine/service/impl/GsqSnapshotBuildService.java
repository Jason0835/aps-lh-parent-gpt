package com.zlt.aps.gsq.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.gsq.engine.context.GsqScheduleContext;
import com.zlt.aps.gsq.engine.domain.GsqMachineCandidate;
import com.zlt.aps.gsq.engine.domain.GsqRuleTrace;
import com.zlt.aps.gsq.engine.domain.GsqRuleTraceItem;
import com.zlt.aps.gsq.engine.domain.GsqSnapshotBuildResult;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleCodeEnum;
import com.zlt.aps.gsq.engine.enums.GsqScheduleRuleResultEnum;
import com.zlt.aps.gsq.engine.vo.GsqScheduleResultVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 钢丝圈解释快照构建服务。
 *
 * <p>Phase 4 重构新增：对齐胎圈 {@code TqSnapshotBuildService}，用于把运行态排程结果、规则证据转换为
 * 版本化 JSON 快照。Phase 2 已实现 {@code GsqRuleTrace.toExplainJson()} 序列化规则证据，本服务在此基础上
 * 补充候选机台、机台选择、未排证据和异常汇总，形成完整的解释快照。</p>
 *
 * <p>与胎圈的差异：</p>
 * <ul>
 *   <li>胎圈和钢丝圈均复用 Phase 2 已添加的 EXPLAIN_JSON 字段，将快照整体序列化为 JSON 落库</li>
 *   <li>钢丝圈有换盘判断（WIRE_COIL_SWITCH）、钢丝直径过滤、产线过滤等独有策略</li>
 *   <li>钢丝圈一个规格可能在6个班次分别分配不同机台，候选机台追踪按班次覆盖</li>
 * </ul>
 *
 * @author APS
 */
@Service
public class GsqSnapshotBuildService {

    /** 解释快照 schema 版本号 */
    private static final String SCHEMA_VERSION = "1";

    /**
     * 构建单个钢丝圈规格的解释快照。
     *
     * @param scheduleVo 排程结果（按 steelRingCode 区分）
     * @param context    钢丝圈排程上下文
     * @return 解释快照结果
     */
    public GsqSnapshotBuildResult buildTaskExplain(GsqScheduleResultVo scheduleVo, GsqScheduleContext context) {
        GsqSnapshotBuildResult result = new GsqSnapshotBuildResult();
        if (scheduleVo == null || context == null) {
            result.setSysAnalysis("排程结果或上下文为空，跳过快照构建");
            return result;
        }

        String steelRingCode = scheduleVo.getSteelRingCode();
        GsqRuleTrace ruleTrace = context.getRuleTrace(steelRingCode);
        List<GsqMachineCandidate> candidateList = context.getCandidateTraceMap().get(steelRingCode);

        // 1. 规则命中 JSON
        result.setRuleHitJson(buildRuleHitJson(ruleTrace));

        // 2. 候选机台 JSON（Phase 3 重构：优先从 candidateTraceMap 提取，无数据时回退到规则证据）
        result.setCandidateMachineJson(buildCandidateMachineJson(scheduleVo, candidateList, ruleTrace));

        // 3. 选中机台编码（钢丝圈6个班次可能分配不同机台，拼接去重）
        result.setSelectedMachineCode(buildSelectedMachineCode(scheduleVo));

        // 4. 分配状态
        String assignStatus = resolveAssignStatus(scheduleVo);
        result.setAssignStatus(assignStatus);

        // 5. 机台分配说明
        result.setMachineSelectReason(buildMachineSelectReason(scheduleVo, assignStatus, candidateList));

        // 6. 未排证据 JSON（仅未排任务填充）
        if (isUnplannedSchedule(scheduleVo)) {
            result.setUnplannedEvidenceJson(buildUnplannedEvidenceJson(ruleTrace, scheduleVo, candidateList));
        }

        // 7. 异常级别和异常 JSON（从异常类规则证据中提取）
        fillTaskIssues(result, ruleTrace);

        // 8. 系统分析说明
        result.setSysAnalysis(buildSysAnalysis(scheduleVo, ruleTrace, assignStatus, candidateList));

        return result;
    }

    /**
     * 构建选中机台编码（6个班次可能分配不同机台，拼接去重后逗号分隔）。
     */
    private String buildSelectedMachineCode(GsqScheduleResultVo scheduleVo) {
        java.util.Set<String> machineCodes = new java.util.LinkedHashSet<>();
        addIfNotEmpty(machineCodes, scheduleVo.getClass1MachineCode());
        addIfNotEmpty(machineCodes, scheduleVo.getClass2MachineCode());
        addIfNotEmpty(machineCodes, scheduleVo.getClass3MachineCode());
        addIfNotEmpty(machineCodes, scheduleVo.getClass4MachineCode());
        addIfNotEmpty(machineCodes, scheduleVo.getClass5MachineCode());
        addIfNotEmpty(machineCodes, scheduleVo.getClass6MachineCode());
        return machineCodes.isEmpty() ? scheduleVo.getMachineCode() : String.join(",", machineCodes);
    }

    private void addIfNotEmpty(java.util.Set<String> set, String value) {
        if (StringUtils.isNotEmpty(value)) {
            set.add(value);
        }
    }

    /**
     * 解析分配状态。
     *
     * @param scheduleVo 排程结果
     * @return 分配状态编码：SUCCESS（已分配）/ UNPLANNED（未排）/ PARTIAL（部分班次未排）
     */
    private String resolveAssignStatus(GsqScheduleResultVo scheduleVo) {
        if (isAllClassPlanEmpty(scheduleVo)) {
            return "UNPLANNED";
        }
        if (isAllMachineEmpty(scheduleVo)) {
            return "UNPLANNED";
        }
        // 至少一个班次有计划量，但部分班次可能因定额/停产等原因延后到下一班
        if (isPartialClassPlan(scheduleVo)) {
            return "PARTIAL";
        }
        return "SUCCESS";
    }

    /**
     * 判断是否为未排任务（6个班次全部无计划量）。
     */
    private boolean isUnplannedSchedule(GsqScheduleResultVo scheduleVo) {
        return isAllClassPlanEmpty(scheduleVo);
    }

    /**
     * 判断6个班次计划量是否全部为空或0。
     */
    private boolean isAllClassPlanEmpty(GsqScheduleResultVo scheduleVo) {
        return isPlanEmpty(scheduleVo.getClass1PlanQty())
                && isPlanEmpty(scheduleVo.getClass2PlanQty())
                && isPlanEmpty(scheduleVo.getClass3PlanQty())
                && isPlanEmpty(scheduleVo.getClass4PlanQty())
                && isPlanEmpty(scheduleVo.getClass5PlanQty())
                && isPlanEmpty(scheduleVo.getClass6PlanQty());
    }

    /**
     * 判断6个班次是否都没有分配机台。
     */
    private boolean isAllMachineEmpty(GsqScheduleResultVo scheduleVo) {
        return StringUtils.isEmpty(scheduleVo.getClass1MachineCode())
                && StringUtils.isEmpty(scheduleVo.getClass2MachineCode())
                && StringUtils.isEmpty(scheduleVo.getClass3MachineCode())
                && StringUtils.isEmpty(scheduleVo.getClass4MachineCode())
                && StringUtils.isEmpty(scheduleVo.getClass5MachineCode())
                && StringUtils.isEmpty(scheduleVo.getClass6MachineCode());
    }

    /**
     * 判断是否为部分班次未排（部分班次有计划量，部分为空）。
     */
    private boolean isPartialClassPlan(GsqScheduleResultVo scheduleVo) {
        boolean hasPlan = !isPlanEmpty(scheduleVo.getClass1PlanQty())
                || !isPlanEmpty(scheduleVo.getClass2PlanQty())
                || !isPlanEmpty(scheduleVo.getClass3PlanQty())
                || !isPlanEmpty(scheduleVo.getClass4PlanQty())
                || !isPlanEmpty(scheduleVo.getClass5PlanQty())
                || !isPlanEmpty(scheduleVo.getClass6PlanQty());
        boolean hasEmpty = isPlanEmpty(scheduleVo.getClass1PlanQty())
                || isPlanEmpty(scheduleVo.getClass2PlanQty())
                || isPlanEmpty(scheduleVo.getClass3PlanQty())
                || isPlanEmpty(scheduleVo.getClass4PlanQty())
                || isPlanEmpty(scheduleVo.getClass5PlanQty())
                || isPlanEmpty(scheduleVo.getClass6PlanQty());
        return hasPlan && hasEmpty;
    }

    /**
     * 判断单个班次计划量是否为空或0。
     */
    private boolean isPlanEmpty(Double planQty) {
        return planQty == null || planQty <= 0;
    }

    /**
     * 构建机台分配说明。
     *
     * <p>Phase 3 重构增强：从候选机台追踪集合提取选中机台的评分描述，丰富分配说明。</p>
     *
     * @param scheduleVo    排程结果
     * @param assignStatus  分配状态
     * @param candidateList 候选机台追踪列表
     * @return 机台分配说明文本
     */
    private String buildMachineSelectReason(GsqScheduleResultVo scheduleVo, String assignStatus,
                                            List<GsqMachineCandidate> candidateList) {
        String steelRingCode = scheduleVo.getSteelRingCode();
        if ("UNPLANNED".equals(assignStatus)) {
            return "钢丝圈代码：" + steelRingCode + " 未分配机台（6个班次均无计划量）";
        }
        String machineCode = buildSelectedMachineCode(scheduleVo);
        // 从候选机台追踪集合中查找选中机台的评分描述
        String scoreResult = null;
        if (CollUtil.isNotEmpty(candidateList)) {
            for (GsqMachineCandidate candidate : candidateList) {
                if (candidate != null && candidate.isSelected() && candidate.getScoreResult() != null) {
                    scoreResult = candidate.getScoreResult();
                    break;
                }
            }
        }
        if ("PARTIAL".equals(assignStatus)) {
            return "钢丝圈代码：" + steelRingCode
                    + " 分配到机台：" + machineCode
                    + "（部分班次因定额/停产原因未排）"
                    + (scoreResult != null ? "，" + scoreResult : "");
        }
        return "钢丝圈代码：" + steelRingCode
                + " 分配到机台：" + machineCode
                + (scoreResult != null ? "（" + scoreResult + "）" : "");
    }

    /**
     * 构建规则命中 JSON。
     *
     * @param trace 规则证据
     * @return 规则命中 JSON 文本
     */
    public String buildRuleHitJson(GsqRuleTrace trace) {
        if (trace != null) {
            return trace.toExplainJson();
        }
        JSONObject result = new JSONObject();
        result.set("schemaVersion", SCHEMA_VERSION);
        result.set("hits", new JSONArray());
        return JSONUtil.toJsonPrettyStr(result);
    }

    /**
     * 构建候选机台 JSON。
     *
     * <p>Phase 3 重构增强：优先从 {@code candidateTraceMap} 提取候选机台详情（含过滤状态、过滤原因、评分、排名），
     * 无数据时回退到从规则证据 {@code MACHINE_FILTER} 中提取（Phase 2 兼容）。</p>
     *
     * @param scheduleVo    排程结果（用于标识实际选中机台）
     * @param candidateList 候选机台追踪列表
     * @param ruleTrace     规则证据（Phase 2 兼容回退）
     * @return 候选机台 JSON 文本
     */
    public String buildCandidateMachineJson(GsqScheduleResultVo scheduleVo, List<GsqMachineCandidate> candidateList,
                                            GsqRuleTrace ruleTrace) {
        JSONObject result = new JSONObject();
        result.set("schemaVersion", SCHEMA_VERSION);

        // 1. 优先从 candidateTraceMap 构建候选机台详情
        if (CollUtil.isNotEmpty(candidateList)) {
            JSONArray array = new JSONArray();
            JSONObject selected = null;
            int rank = 0;
            for (GsqMachineCandidate candidate : candidateList) {
                if (candidate == null) {
                    continue;
                }
                rank++;
                JSONObject obj = new JSONObject();
                obj.set("machineCode", candidate.getMachineCode());
                obj.set("machineName", candidate.getMachineName());
                obj.set("filtered", candidate.isFiltered());
                obj.set("filterStrategy", candidate.getFilterStrategy());
                obj.set("filterReasonCode", candidate.getFilterReasonCode());
                obj.set("filterReasonDesc", candidate.getFilterReasonDesc());
                obj.set("remainCapacity", candidate.getRemainCapacity());
                obj.set("taskChainSize", candidate.getTaskChainSize());
                obj.set("score", candidate.getScore());
                obj.set("scoreResult", candidate.getScoreResult());
                obj.set("rank", candidate.getRank() > 0 ? candidate.getRank() : rank);
                array.add(obj);
                // 标识实际选中机台
                if (selected == null && candidate.isSelected()) {
                    selected = new JSONObject();
                    selected.set("machineCode", candidate.getMachineCode());
                    selected.set("score", candidate.getScore());
                    selected.set("reason", candidate.getScoreResult());
                }
            }
            result.set("selected", selected);
            result.set("candidates", array);
            return JSONUtil.toJsonPrettyStr(result);
        }

        // 2. 回退：从规则证据 MACHINE_FILTER 中提取（Phase 2 兼容）
        JSONArray filterEvidences = new JSONArray();
        if (ruleTrace != null && CollUtil.isNotEmpty(ruleTrace.getRuleHits())) {
            for (GsqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                if (GsqScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(item.getRuleCode())) {
                    JSONObject evObj = new JSONObject();
                    evObj.set("ruleCode", item.getRuleCode());
                    evObj.set("result", item.getResult());
                    evObj.set("evidence", item.getEvidence());
                    filterEvidences.add(evObj);
                }
            }
        }
        result.set("selected", null);
        result.set("candidates", new JSONArray());
        result.set("filterEvidences", filterEvidences);
        return JSONUtil.toJsonPrettyStr(result);
    }

    /**
     * 构建未排证据 JSON。
     *
     * <p>Phase 3 重构增强：从候选机台追踪集合提取被过滤机台列表，从规则证据提取定额超出、停产协调等
     * 未排相关证据，组装为精简 JSON。</p>
     *
     * @param ruleTrace     规则证据
     * @param scheduleVo    未排结果
     * @param candidateList 候选机台追踪列表
     * @return 未排证据 JSON 文本
     */
    private String buildUnplannedEvidenceJson(GsqRuleTrace ruleTrace, GsqScheduleResultVo scheduleVo,
                                              List<GsqMachineCandidate> candidateList) {
        JSONObject obj = new JSONObject();
        obj.set("schemaVersion", SCHEMA_VERSION);
        obj.set("steelRingCode", scheduleVo.getSteelRingCode());
        obj.set("reasonCode", "ALL_CLASS_PLAN_EMPTY");
        obj.set("reasonDesc", "6个班次均无计划量，未排入任何机台");

        // 1. 从候选机台追踪集合提取被过滤机台列表
        JSONArray rejectedCandidates = new JSONArray();
        if (CollUtil.isNotEmpty(candidateList)) {
            for (GsqMachineCandidate candidate : candidateList) {
                if (candidate != null && candidate.isFiltered()) {
                    JSONObject rejectObj = new JSONObject();
                    rejectObj.set("machineCode", candidate.getMachineCode());
                    rejectObj.set("filterStrategy", candidate.getFilterStrategy());
                    rejectObj.set("filterReasonCode", candidate.getFilterReasonCode());
                    rejectObj.set("filterReasonDesc", candidate.getFilterReasonDesc());
                    rejectedCandidates.add(rejectObj);
                }
            }
        }
        obj.set("rejectedCandidates", rejectedCandidates);

        // 2. 从规则证据提取未排相关规则
        JSONArray unplannedEvidences = new JSONArray();
        if (ruleTrace != null && CollUtil.isNotEmpty(ruleTrace.getRuleHits())) {
            for (GsqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                String ruleCode = item.getRuleCode();
                // 提取与未排相关的规则证据
                if (GsqScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(ruleCode)
                        || GsqScheduleRuleCodeEnum.MACHINE_QUOTA_LIMIT.getCode().equals(ruleCode)
                        || GsqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER.getCode().equals(ruleCode)) {
                    JSONObject evObj = new JSONObject();
                    evObj.set("ruleCode", ruleCode);
                    evObj.set("result", item.getResult());
                    evObj.set("evidence", item.getEvidence());
                    unplannedEvidences.add(evObj);
                }
            }
        }
        obj.set("unplannedEvidences", unplannedEvidences);
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 汇总当前钢丝圈规格关联的结构化异常。
     *
     * <p>从规则证据中提取异常类规则（结果为 SKIP 或 MISS 的），组装为异常 JSON。</p>
     *
     * @param result    解释快照
     * @param ruleTrace 规则证据
     */
    private void fillTaskIssues(GsqSnapshotBuildResult result, GsqRuleTrace ruleTrace) {
        JSONArray issues = new JSONArray();
        if (ruleTrace != null && CollUtil.isNotEmpty(ruleTrace.getRuleHits())) {
            for (GsqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                // 异常类规则：结果为 SKIP 或 MISS 的规则视为异常
                if (GsqScheduleRuleResultEnum.SKIP.getCode().equals(item.getResult())
                        || GsqScheduleRuleResultEnum.MISS.getCode().equals(item.getResult())) {
                    JSONObject issueObj = new JSONObject();
                    issueObj.set("ruleCode", item.getRuleCode());
                    issueObj.set("result", item.getResult());
                    issueObj.set("evidence", item.getEvidence());
                    issues.add(issueObj);
                }
            }
        }
        JSONObject issueObject = new JSONObject();
        issueObject.set("schemaVersion", SCHEMA_VERSION);
        issueObject.set("issues", issues);
        result.setIssueJson(JSONUtil.toJsonPrettyStr(issueObject));

        // 异常级别：有异常时为 WARN，否则为 INFO
        result.setIssueLevel(issues.isEmpty() ? "INFO" : "WARN");
    }

    /**
     * 构建系统分析说明（综合规则命中、候选机台和分配结果的人类可读文本）。
     *
     * <p>Phase 3 重构增强：增加候选机台统计信息（总数、被过滤数、通过过滤数）。</p>
     *
     * @param scheduleVo    排程结果
     * @param ruleTrace     规则证据
     * @param assignStatus  分配状态
     * @param candidateList 候选机台追踪列表
     * @return 系统分析说明文本
     */
    private String buildSysAnalysis(GsqScheduleResultVo scheduleVo, GsqRuleTrace ruleTrace, String assignStatus,
                                    List<GsqMachineCandidate> candidateList) {
        StringBuilder sb = new StringBuilder();
        sb.append("钢丝圈代码：").append(scheduleVo.getSteelRingCode());
        sb.append("，分配状态：").append(assignStatus);

        // Phase 3 重构新增：候选机台统计
        if (CollUtil.isNotEmpty(candidateList)) {
            int totalCount = candidateList.size();
            long filteredCount = candidateList.stream()
                    .filter(GsqMachineCandidate::isFiltered)
                    .count();
            int passedCount = totalCount - (int) filteredCount;
            sb.append("，候选机台：").append(totalCount)
                    .append("（通过").append(passedCount).append("，被过滤").append(filteredCount).append("）");
        }

        if (ruleTrace != null && CollUtil.isNotEmpty(ruleTrace.getRuleHits())) {
            int hitCount = ruleTrace.getRuleHits().size();
            sb.append("，规则命中数：").append(hitCount);

            // 统计各类规则结果
            long hitCount2 = ruleTrace.getRuleHits().stream()
                    .filter(item -> GsqScheduleRuleResultEnum.HIT.getCode().equals(item.getResult()))
                    .count();
            long adjustCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> GsqScheduleRuleResultEnum.ADJUST.getCode().equals(item.getResult()))
                    .count();
            long triggerCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> GsqScheduleRuleResultEnum.TRIGGER.getCode().equals(item.getResult()))
                    .count();
            long missCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> GsqScheduleRuleResultEnum.MISS.getCode().equals(item.getResult()))
                    .count();
            long skipCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> GsqScheduleRuleResultEnum.SKIP.getCode().equals(item.getResult()))
                    .count();
            sb.append("（HIT:").append(hitCount2)
                    .append(", ADJUST:").append(adjustCount)
                    .append(", TRIGGER:").append(triggerCount)
                    .append(", MISS:").append(missCount)
                    .append(", SKIP:").append(skipCount)
                    .append("）");
        } else {
            sb.append("，无规则命中");
        }
        return sb.toString();
    }

    /**
     * 将解释快照序列化为 JSON 文本。
     *
     * <p>用于落库到 {@code T_GSQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。
     * Phase 2 仅序列化 {@code GsqRuleTrace}，Phase 4 扩展为序列化完整解释快照。</p>
     *
     * @param snapshot 解释快照
     * @return JSON 文本
     */
    public String toExplainJson(GsqSnapshotBuildResult snapshot) {
        if (snapshot == null) {
            return null;
        }
        JSONObject obj = new JSONObject();
        obj.set("schemaVersion", SCHEMA_VERSION);
        obj.set("ruleHitJson", parseJsonObject(snapshot.getRuleHitJson()));
        obj.set("candidateMachineJson", parseJsonObject(snapshot.getCandidateMachineJson()));
        obj.set("selectedMachineCode", snapshot.getSelectedMachineCode());
        obj.set("machineSelectReason", snapshot.getMachineSelectReason());
        obj.set("assignStatus", snapshot.getAssignStatus());
        obj.set("unplannedEvidenceJson", parseJsonObject(snapshot.getUnplannedEvidenceJson()));
        obj.set("sysAnalysis", snapshot.getSysAnalysis());
        obj.set("issueLevel", snapshot.getIssueLevel());
        obj.set("issueJson", parseJsonObject(snapshot.getIssueJson()));
        return JSONUtil.toJsonPrettyStr(obj);
    }

    /**
     * 安全解析 JSON 字符串为 JSON 对象，解析失败时返回原字符串。
     *
     * @param jsonStr JSON 字符串
     * @return JSON 对象或原字符串
     */
    private Object parseJsonObject(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.parse(jsonStr);
        } catch (Exception e) {
            // 解析失败时返回原字符串，避免序列化异常
            return jsonStr;
        }
    }
}
