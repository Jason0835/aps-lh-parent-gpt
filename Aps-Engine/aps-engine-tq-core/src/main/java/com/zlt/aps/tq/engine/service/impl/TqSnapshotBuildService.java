package com.zlt.aps.tq.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tq.engine.context.TqScheduleContext;
import com.zlt.aps.tq.engine.domain.TqMachineCandidate;
import com.zlt.aps.tq.engine.domain.TqRuleTrace;
import com.zlt.aps.tq.engine.domain.TqRuleTraceItem;
import com.zlt.aps.tq.engine.domain.TqSnapshotBuildResult;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleCodeEnum;
import com.zlt.aps.tq.engine.enums.TqScheduleRuleResultEnum;
import com.zlt.aps.tq.engine.vo.TqScheduleResultVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 胎圈解释快照构建服务。
 *
 * <p>Phase 4 重构新增：对齐胎侧 {@code TcSnapshotBuildService}，用于把运行态排程结果、规则证据转换为
 * 版本化 JSON 快照。Phase 2 已实现 {@code TqRuleTrace.toExplainJson()} 序列化规则证据，本服务在此基础上
 * 补充候选机台、机台选择、未排证据和异常汇总，形成完整的解释快照。</p>
 *
 * <p>与胎侧的差异：</p>
 * <ul>
 *   <li>胎侧解释快照独立写入解释表 {@code T_TC_SCHEDULE_RESULT_EXPLAIN}；胎圈复用 Phase 2 已添加的
 *       {@code T_TQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段，将快照整体序列化为 JSON 落库</li>
 *   <li>胎圈没有 {@code candidateTraceMap}，候选机台信息从规则证据 {@code MACHINE_FILTER} 中提取</li>
 *   <li>胎圈没有 {@code IssueCollector}，异常信息从规则证据 {@code QUOTA_EXCEED_DEFER} 等异常类规则中提取</li>
 * </ul>
 *
 * @author APS
 */
@Service
public class TqSnapshotBuildService {

    /** 解释快照 schema 版本号 */
    private static final String SCHEMA_VERSION = "1";

    /**
     * 构建单个胎圈规格的解释快照。
     *
     * @param scheduleVo 排程结果（按 beadCode 区分）
     * @param context    胎圈排程上下文
     * @return 解释快照结果
     */
    public TqSnapshotBuildResult buildTaskExplain(TqScheduleResultVo scheduleVo, TqScheduleContext context) {
        TqSnapshotBuildResult result = new TqSnapshotBuildResult();
        if (scheduleVo == null || context == null) {
            result.setSysAnalysis("排程结果或上下文为空，跳过快照构建");
            return result;
        }

        String beadCode = scheduleVo.getBeadCode();
        TqRuleTrace ruleTrace = context.getRuleTrace(beadCode);
        List<TqMachineCandidate> candidateList = context.getCandidateTraceMap().get(beadCode);

        // 1. 规则命中 JSON
        result.setRuleHitJson(buildRuleHitJson(ruleTrace));

        // 2. 候选机台 JSON（Phase 3 重构：优先从 candidateTraceMap 提取，无数据时回退到规则证据）
        result.setCandidateMachineJson(buildCandidateMachineJson(scheduleVo, candidateList, ruleTrace));

        // 3. 选中机台编码
        result.setSelectedMachineCode(scheduleVo.getMachineCode());

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
     * 解析分配状态。
     *
     * @param scheduleVo 排程结果
     * @return 分配状态编码：SUCCESS（已分配）/ UNPLANNED（未排）/ PARTIAL（部分班次未排）
     */
    private String resolveAssignStatus(TqScheduleResultVo scheduleVo) {
        if (isAllClassPlanEmpty(scheduleVo)) {
            return "UNPLANNED";
        }
        if (StringUtils.isEmpty(scheduleVo.getMachineCode())) {
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
    private boolean isUnplannedSchedule(TqScheduleResultVo scheduleVo) {
        return isAllClassPlanEmpty(scheduleVo);
    }

    /**
     * 判断6个班次计划量是否全部为空或0。
     */
    private boolean isAllClassPlanEmpty(TqScheduleResultVo scheduleVo) {
        return isPlanEmpty(scheduleVo.getClass1PlanQty())
                && isPlanEmpty(scheduleVo.getClass2PlanQty())
                && isPlanEmpty(scheduleVo.getClass3PlanQty())
                && isPlanEmpty(scheduleVo.getClass4PlanQty())
                && isPlanEmpty(scheduleVo.getClass5PlanQty())
                && isPlanEmpty(scheduleVo.getClass6PlanQty());
    }

    /**
     * 判断是否为部分班次未排（部分班次有计划量，部分为空）。
     */
    private boolean isPartialClassPlan(TqScheduleResultVo scheduleVo) {
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
    private String buildMachineSelectReason(TqScheduleResultVo scheduleVo, String assignStatus,
                                            List<TqMachineCandidate> candidateList) {
        String beadCode = scheduleVo.getBeadCode();
        if ("UNPLANNED".equals(assignStatus)) {
            return "胎圈代码：" + beadCode + " 未分配机台（6个班次均无计划量）";
        }
        String machineCode = scheduleVo.getMachineCode();
        // 从候选机台追踪集合中查找选中机台的评分描述
        String scoreResult = null;
        if (CollUtil.isNotEmpty(candidateList)) {
            for (TqMachineCandidate candidate : candidateList) {
                if (Objects.equals(candidate.getMachineCode(), machineCode)) {
                    scoreResult = candidate.getScoreResult();
                    break;
                }
            }
        }
        if ("PARTIAL".equals(assignStatus)) {
            return "胎圈代码：" + beadCode
                    + " 分配到机台：" + machineCode
                    + "（部分班次因定额/停产原因未排）"
                    + (scoreResult != null ? "，" + scoreResult : "");
        }
        return "胎圈代码：" + beadCode
                + " 分配到机台：" + machineCode
                + (scoreResult != null ? "（" + scoreResult + "）" : "");
    }

    /**
     * 构建规则命中 JSON。
     *
     * @param trace 规则证据
     * @return 规则命中 JSON 文本
     */
    public String buildRuleHitJson(TqRuleTrace trace) {
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
     * <p>输出格式：</p>
     * <ul>
     *   <li>candidates: 全部候选机台列表（含被过滤机台）</li>
     *   <li>selected: 选中机台信息（machineCode、score、reason）</li>
     *   <li>filterEvidences: Phase 2 兼容字段，MACHINE_FILTER 规则证据</li>
     * </ul>
     *
     * @param scheduleVo    排程结果（用于标识实际选中机台）
     * @param candidateList 候选机台追踪列表
     * @param ruleTrace     规则证据（Phase 2 兼容回退）
     * @return 候选机台 JSON 文本
     */
    public String buildCandidateMachineJson(TqScheduleResultVo scheduleVo, List<TqMachineCandidate> candidateList,
                                            TqRuleTrace ruleTrace) {
        JSONObject result = new JSONObject();
        result.set("schemaVersion", SCHEMA_VERSION);

        // 1. 优先从 candidateTraceMap 构建候选机台详情
        if (CollUtil.isNotEmpty(candidateList)) {
            JSONArray array = new JSONArray();
            JSONObject selected = null;
            int rank = 0;
            for (TqMachineCandidate candidate : candidateList) {
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
                obj.set("score", candidate.getScore());
                obj.set("scoreResult", candidate.getScoreResult());
                obj.set("rank", candidate.getRank() > 0 ? candidate.getRank() : rank);
                // Phase 5 重构新增：任务链连续性上下文（前置规格/班次/切换时长/连续性得分）
                obj.set("lastBeadCode", candidate.getLastBeadCode());
                obj.set("lastClassIndex", candidate.getLastClassIndex());
                obj.set("switchTime", candidate.getSwitchTime());
                obj.set("continuityScore", candidate.getContinuityScore());
                array.add(obj);
                // 标识实际选中机台（落库的 machineCode）
                if (selected == null && scheduleVo != null
                        && Objects.equals(scheduleVo.getMachineCode(), candidate.getMachineCode())) {
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
            for (TqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                if (TqScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(item.getRuleCode())) {
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
    private String buildUnplannedEvidenceJson(TqRuleTrace ruleTrace, TqScheduleResultVo scheduleVo,
                                              List<TqMachineCandidate> candidateList) {
        JSONObject obj = new JSONObject();
        obj.set("schemaVersion", SCHEMA_VERSION);
        obj.set("beadCode", scheduleVo.getBeadCode());
        obj.set("reasonCode", "ALL_CLASS_PLAN_EMPTY");
        obj.set("reasonDesc", "6个班次均无计划量，未排入任何机台");

        // 1. 从候选机台追踪集合提取被过滤机台列表
        JSONArray rejectedCandidates = new JSONArray();
        if (CollUtil.isNotEmpty(candidateList)) {
            for (TqMachineCandidate candidate : candidateList) {
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
            for (TqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                String ruleCode = item.getRuleCode();
                // 提取与未排相关的规则证据
                if (TqScheduleRuleCodeEnum.MACHINE_FILTER.getCode().equals(ruleCode)
                        || TqScheduleRuleCodeEnum.MACHINE_QUOTA_LIMIT.getCode().equals(ruleCode)
                        || TqScheduleRuleCodeEnum.QUOTA_EXCEED_DEFER.getCode().equals(ruleCode)) {
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
     * 汇总当前胎圈规格关联的结构化异常。
     *
     * <p>从规则证据中提取异常类规则（结果为 SKIP 或 MISS 的），组装为异常 JSON。</p>
     *
     * @param result    解释快照
     * @param ruleTrace 规则证据
     */
    private void fillTaskIssues(TqSnapshotBuildResult result, TqRuleTrace ruleTrace) {
        JSONArray issues = new JSONArray();
        if (ruleTrace != null && CollUtil.isNotEmpty(ruleTrace.getRuleHits())) {
            for (TqRuleTraceItem item : ruleTrace.getRuleHits()) {
                if (item == null) {
                    continue;
                }
                // 异常类规则：结果为 SKIP 或 MISS 的规则视为异常
                if (TqScheduleRuleResultEnum.SKIP.getCode().equals(item.getResult())
                        || TqScheduleRuleResultEnum.MISS.getCode().equals(item.getResult())) {
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
    private String buildSysAnalysis(TqScheduleResultVo scheduleVo, TqRuleTrace ruleTrace, String assignStatus,
                                    List<TqMachineCandidate> candidateList) {
        StringBuilder sb = new StringBuilder();
        sb.append("胎圈代码：").append(scheduleVo.getBeadCode());
        sb.append("，分配状态：").append(assignStatus);

        // Phase 3 重构新增：候选机台统计
        if (CollUtil.isNotEmpty(candidateList)) {
            int totalCount = candidateList.size();
            long filteredCount = candidateList.stream()
                    .filter(TqMachineCandidate::isFiltered)
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
                    .filter(item -> TqScheduleRuleResultEnum.HIT.getCode().equals(item.getResult()))
                    .count();
            long adjustCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> TqScheduleRuleResultEnum.ADJUST.getCode().equals(item.getResult()))
                    .count();
            long triggerCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> TqScheduleRuleResultEnum.TRIGGER.getCode().equals(item.getResult()))
                    .count();
            long missCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> TqScheduleRuleResultEnum.MISS.getCode().equals(item.getResult()))
                    .count();
            long skipCount = ruleTrace.getRuleHits().stream()
                    .filter(item -> TqScheduleRuleResultEnum.SKIP.getCode().equals(item.getResult()))
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
     * <p>用于落库到 {@code T_TQ_SCHEDULE_RESULT.EXPLAIN_JSON} 字段。
     * Phase 2 仅序列化 {@code TqRuleTrace}，Phase 4 扩展为序列化完整解释快照。</p>
     *
     * @param snapshot 解释快照
     * @return JSON 文本
     */
    public String toExplainJson(TqSnapshotBuildResult snapshot) {
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
