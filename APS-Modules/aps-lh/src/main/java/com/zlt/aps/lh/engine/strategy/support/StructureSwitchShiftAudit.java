package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构分班只读审计。采集现有决策快照，按动态S0/S1输出上限、目标及资源证据。
 * 不重新准入、选机、计算首检、占用模具或修改结果；记录留在原请求上下文，不参与回滚。
 */
@Slf4j
public final class StructureSwitchShiftAudit {
    /** 已通过现有全部预演约束的阶段。 */
    private static final String GENERATED = "PROPOSAL_GENERATED";
    /** 尚未进入机台扫描的候选快照。 */
    private static final String CANDIDATE = "CANDIDATE_ADMISSION";
    /** 仅观测到提交，未经过机台扫描的独立组合。 */
    private static final String NOT_OBSERVED = "NOT_OBSERVED";
    /** 明确标识为等结果。 */
    private static final int WAIT_NOTIFY = 1;
    /** 首班结构上限。 */
    private static final int FIRST_LIMIT = 1;
    /** 等结果第二班结构上限。 */
    private static final int SECOND_LIMIT = 2;
    /** 单条汇总最多展开的机台明细数；总数和分类汇总始终完整。 */
    private static final int DETAIL_LIMIT = 12;

    private StructureSwitchShiftAudit() {
    }

    /**
     * 候选构建结束时记录准入状态，不把零目标或未进入扫描解释成无资源。
     * @param context 原排程上下文
     * @param day 当前业务日及阶段
     * @param candidate 已有候选及准入原因
     */
    public static void recordCandidate(LhScheduleContext context, DayScheduleContext day,
            DailyNewSpecCandidate candidate) {
        if (!enabled(context)) {
            return;
        }
        recordAdmission(context, day, candidate.getSku(), new StringBuilder(128).append("准入=")
                .append(!candidate.getReasons().isEmpty()).append("，原因=").append(candidate.getReasons())
                .append("，原始日计划=").append(candidate.getOriginalDayPlanQty())
                .append("，已有绑定=").append(candidate.isBoundOnMachine()).toString());
    }

    /**
     * 保存阶段准入原始依据，包括候选对象构建前已被拒绝的情况。
     * @param context 原上下文
     * @param day 当前阶段及班次
     * @param sku 原始SKU
     * @param reason 已有准入依据，不重新执行规则
     */
    public static void recordAdmission(LhScheduleContext context, DayScheduleContext day,
            SkuScheduleDTO sku, String reason) {
        for (LhShiftConfigVO shift : day.getDayShifts()) {
            StructureSwitchShiftAuditEntry entry = entry(context, day, shift, sku, null);
            if (Objects.nonNull(entry)) {
                entry.setDecisionStage(CANDIDATE);
                entry.setReason(reason);
            }
        }
    }

    /**
     * 复用候选扫描已取得的完整时间轴；无效计划清空旧时间，防止沿用上一轮可行性。
     * @param context 原排程上下文
     * @param day 当前业务日
     * @param shift 当前竞争班次
     * @param candidate 当前候选
     * @param machineCode 当前机台
     * @param plan 既有解析器返回的正式可用计划
     */
    public static void recordPlan(LhScheduleContext context, DayScheduleContext day, LhShiftConfigVO shift,
            DailyNewSpecCandidate candidate, String machineCode, NewSpecMachineAvailabilityPlan plan) {
        StructureSwitchShiftAuditEntry entry = entry(context, day, shift, candidate.getSku(), machineCode);
        if (Objects.isNull(entry)) {
            return;
        }
        entry.setTargetMachineCount(candidate.getTargetMachineCount());
        entry.setDecisionStage("TIMELINE_PREVIEW");
        clearPlan(entry);
        if (Objects.nonNull(plan) && plan.isAvailable()) {
            entry.setChangeoverStart(LhScheduleTimeUtil.formatDateTime(plan.getChangeoverStartTime()));
            entry.setOccupationStart(LhScheduleTimeUtil.formatDateTime(plan.getProductionOccupationStartTime()));
            entry.setFormalStart(LhScheduleTimeUtil.formatDateTime(plan.getFormalAvailableProductionTime()));
            LhShiftConfigVO occupationShift = plan.getProductionOccupationShift();
            if (Objects.nonNull(occupationShift)) {
                entry.setOccupationShiftStartMillis(occupationShift.getShiftStartDateTime().getTime());
            }
            if (Objects.nonNull(plan.getFirstInspectionTimelinePlan())) {
                entry.setTimingMode(String.valueOf(plan.getFirstInspectionTimelinePlan().getTimingMode()));
            }
        }
    }

    /**
     * 保存真实扫描阶段和原因，独立于DEBUG开关及旧日志去重；不将生成提案当作提交。
     * @param context 原上下文
     * @param day 当前业务日
     * @param shift 竞争班次
     * @param candidate 既有候选
     * @param machineCode 机台编码
     * @param stage 已有决策阶段
     * @param reason 已有决策原因
     */
    public static void recordDecision(LhScheduleContext context, DayScheduleContext day, LhShiftConfigVO shift,
            DailyNewSpecCandidate candidate, String machineCode, String stage, String reason) {
        StructureSwitchShiftAuditEntry entry = entry(context, day, shift, candidate.getSku(), machineCode);
        if (Objects.isNull(entry)) {
            return;
        }
        entry.setTargetMachineCount(candidate.getTargetMachineCount());
        entry.setPoolDate(Objects.toString(candidate.getPoolDate(), null));
        entry.setDecisionStage(stage);
        entry.setReason(reason);
        if (StringUtils.equals("CANDIDATE_STATE", stage) || StringUtils.equals("HARD_MATCH", stage)
                || StringUtils.equals("MACHINE_SCOPE", stage) || StringUtils.equals("READ_ONLY_ELIGIBILITY", stage)
                || StringUtils.equals("FAILED_ASSIGNMENT_CACHE", stage)) {
            // 本次在时间轴计算前已拒绝，不把上一轮的时间标成当前可执行时间。
            clearPlan(entry);
        }
        if (StringUtils.equals(GENERATED, stage)
                && Objects.equals(entry.getOccupationShiftStartMillis(), entry.getShiftStartMillis())) {
            entry.setSameShiftProposalGenerated(true);
        }
    }

    /**
     * 清除最近决策已经不再具备的计划字段，保留曾合格和真实提交的历史事实。
     * @param entry 独立审计标量快照
     */
    private static void clearPlan(StructureSwitchShiftAuditEntry entry) {
        entry.setChangeoverStart(null);
        entry.setOccupationStart(null);
        entry.setFormalStart(null);
        entry.setOccupationShiftStartMillis(null);
        entry.setTimingMode(null);
    }

    /**
     * 在既有提交返回后记录结果，失败轨迹保留，但上机数始终只取正量结果。
     * @param context 原上下文
     * @param day 当前业务日
     * @param assignment 已选中的冻结提案
     * @param outcome 既有提交返回结果描述
     */
    public static void recordSubmission(LhScheduleContext context, DayScheduleContext day,
            NewSpecMachineAssignmentPlan assignment, String outcome) {
        if (!enabled(context)) {
            return;
        }
        LhShiftConfigVO shift = context.getScheduleWindowShifts().stream()
                .filter(item -> Objects.equals(item.getShiftIndex(), assignment.getTargetShiftIndex()))
                .findFirst().orElse(null);
        StructureSwitchShiftAuditEntry entry = entry(context, day, shift, assignment.getCandidate().getSku(),
                assignment.getMatchResult().getMachine().getMachineCode());
        if (Objects.nonNull(entry)) {
            entry.setSubmissionOutcome(outcome);
        }
    }

    /**
     * 按物理机台合并标量证据；仅观察原窗口中的已加载结构，不执行带副作用的规则。
     * @param context 原上下文
     * @param day 竞争阶段
     * @param shift 竞争班次
     * @param sku 候选SKU
     * @param machineCode 运行侧或物理机台
     * @return 独立审计快照，不适用时为空
     */
    private static StructureSwitchShiftAuditEntry entry(LhScheduleContext context, DayScheduleContext day,
            LhShiftConfigVO shift, SkuScheduleDTO sku, String machineCode) {
        if (!enabled(context) || context.isStructureSwitchSelectionProbe()
                || Objects.isNull(shift) || Objects.isNull(shift.getShiftStartDateTime())) {
            return null;
        }
        CxEmbryoLhTime source = context.getStructureSwitchSourceMap().get(sku.getStructureName());
        if (Objects.isNull(source)) {
            return null;
        }
        String physical = StringUtils.isEmpty(machineCode) ? null
                : LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        String key = new StringBuilder(128).append(source.getId()).append('|').append(sku.getStructureName())
                .append('|').append(shift.getShiftStartDateTime().getTime()).append('|').append(day.getCurrentPhase())
                .append('|').append(sku.getMaterialCode()).append('|').append(sku.getProductStatus())
                .append('|').append(physical).toString();
        return context.getStructureSwitchShiftAuditMap().computeIfAbsent(key, ignored -> {
            StructureSwitchShiftAuditEntry entry = new StructureSwitchShiftAuditEntry();
            entry.setSourceId(source.getId());
            entry.setStructureName(sku.getStructureName());
            entry.setShiftStartMillis(shift.getShiftStartDateTime().getTime());
            entry.setPhase(String.valueOf(day.getCurrentPhase()));
            entry.setMaterialCode(sku.getMaterialCode());
            entry.setProductStatus(sku.getProductStatus());
            entry.setMachineCode(physical);
            // 部分固定组合只经过提交入口，明确保留未观测状态，不能伪造扫描成功。
            entry.setDecisionStage(NOT_OBSERVED);
            return entry;
        });
    }

    /**
     * 阶段收敛时复核当天各班，不把遍历一次班次误认为该班已最终结束。
     * @param context 原上下文
     * @param day 已收敛业务日
     */
    public static void appendDaySummary(LhScheduleContext context, DayScheduleContext day) {
        if (enabled(context)) {
            append(context, day.getDayShifts(), "阶段收敛/" + day.getCurrentPhase());
        }
    }

    /**
     * 所有数量后处理结束、原子保存之前按最终结果审计；不修改结构状态或阻断保存。
     * @param context 本次原窗口上下文
     */
    public static void appendFinalSummary(LhScheduleContext context) {
        if (enabled(context)) {
            append(context, StructureSwitchSchedulingPolicy.orderedShifts(context), "保存前最终结果");
        }
    }

    /**
     * 对每次真正提交过的结构按最终正量定位首班，并保留未产生正量的审计结论。
     * @param context 原上下文
     * @param reportShifts 本次输出班次
     * @param phase 明确区分阶段快照和最终结果
     */
    private static void append(LhScheduleContext context, List<LhShiftConfigVO> reportShifts, String phase) {
        List<LhShiftConfigVO> window = StructureSwitchSchedulingPolicy.orderedShifts(context);
        for (CxEmbryoLhTime source : context.getStructureSwitchSourceMap().values()) {
            List<LhScheduleResult> switchResults = context.getScheduleResultList().stream()
                    .filter(result -> Objects.nonNull(context.getStructureSwitchResultPlanMap().get(result)))
                    .filter(result -> Objects.equals(source.getId(), context.getStructureSwitchResultPlanMap()
                            .get(result).getSource().getId())).collect(Collectors.toList());
            LhShiftConfigVO first = window.stream().filter(shift -> switchResults.stream()
                    .anyMatch(result -> StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex()) > 0))
                    .findFirst().orElse(null);
            if (Objects.isNull(first)) {
                if (context.getStructureSwitchShiftAuditMap().values().stream()
                        .anyMatch(entry -> Objects.equals(entry.getSourceId(), source.getId()))) {
                    String detail = new StringBuilder(128).append("batchNo=").append(context.getBatchNo())
                            .append("，时点=").append(phase).append("，sourceId=").append(source.getId())
                            .append("，结构=").append(source.getNextStructureName())
                            .append("，无最终正量，不建立S0/S1；试算或回裁为零不计上机").toString();
                    PriorityTraceLogHelper.appendProcessLog(context, "结构切换逐班审计", detail);
                    log.info("结构切换逐班审计, {}", detail);
                }
                continue;
            }
            LhShiftConfigVO second = window.stream().filter(shift -> shift.getShiftStartDateTime()
                    .after(first.getShiftStartDateTime())).findFirst().orElse(null);
            LhScheduleResult firstResult = switchResults.stream()
                    .filter(result -> StructureSwitchSchedulingPolicy.quantity(result, first.getShiftIndex()) > 0)
                    .min(Comparator.comparing(result -> actualStart(result, first))).get();
            for (LhShiftConfigVO shift : reportShifts) {
                if (!shift.getShiftStartDateTime().before(first.getShiftStartDateTime())) {
                    appendShift(context, source, firstResult, first, second, shift, phase);
                }
            }
        }
    }

    /**
     * 生成一个结构班次的上限、首台SKU目标和机台证据；普通班次不擅自声称普通上限通过。
     * @param context 原上下文
     * @param source 结构来源
     * @param firstResult 最终首台SKU
     * @param first 动态S0
     * @param second 紧邻S1
     * @param shift 被审计班次
     * @param phase 审计时点
     */
    private static void appendShift(LhScheduleContext context, CxEmbryoLhTime source, LhScheduleResult firstResult,
            LhShiftConfigVO first, LhShiftConfigVO second, LhShiftConfigVO shift, String phase) {
        boolean isFirst = Objects.equals(first.getShiftStartDateTime(), shift.getShiftStartDateTime());
        boolean isSecond = Objects.nonNull(second)
                && Objects.equals(second.getShiftStartDateTime(), shift.getShiftStartDateTime());
        Integer limit = null;
        String shiftStage = "后续班";
        if (isFirst) {
            limit = FIRST_LIMIT;
            shiftStage = "S0";
        } else if (isSecond) {
            shiftStage = "S1";
            if (Objects.equals(source.getIsWaitNotify(), WAIT_NOTIFY)) {
                limit = SECOND_LIMIT;
            }
        }
        Set<String> machines = machines(context, source.getNextStructureName(), shift, null);
        String limitConclusion = "沿用普通约束，本审计不重复裁决";
        if (Objects.nonNull(limit)) {
            limitConclusion = machines.size() <= limit ? "通过" : "违规";
        }
        Set<String> skuMachines = machines(context, source.getNextStructureName(), shift, firstResult);
        List<StructureSwitchShiftAuditEntry> entries = context.getStructureSwitchShiftAuditMap().values().stream()
                .filter(entry -> Objects.equals(source.getId(), entry.getSourceId()))
                .filter(entry -> entry.getShiftStartMillis() == shift.getShiftStartDateTime().getTime())
                .filter(entry -> StringUtils.equals(entry.getMaterialCode(), firstResult.getMaterialCode())
                        && StringUtils.equals(entry.getProductStatus(), firstResult.getProductStatus()))
                .collect(Collectors.toList());
        Set<Integer> targets = entries.stream().map(StructureSwitchShiftAuditEntry::getTargetMachineCount)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        long generated = entries.stream().filter(StructureSwitchShiftAuditEntry::isSameShiftProposalGenerated)
                .map(StructureSwitchShiftAuditEntry::getMachineCode).distinct().count();
        Map<String, Long> stages = entries.stream().filter(entry -> StringUtils.isNotEmpty(entry.getMachineCode()))
                .filter(entry -> !StringUtils.equals(NOT_OBSERVED, entry.getDecisionStage()))
                .collect(Collectors.groupingBy(StructureSwitchShiftAuditEntry::getDecisionStage,
                        LinkedHashMap::new, Collectors.counting()));
        String goal = targets.size() == 1 && targets.iterator().next() > 0
                ? "缺口=" + Math.max(0, targets.iterator().next() - skuMachines.size())
                : "目标证据不足或快照发生变化";
        String detail = new StringBuilder(768).append("batchNo=").append(context.getBatchNo())
                .append("，factoryCode=").append(context.getFactoryCode()).append("，时点=").append(phase)
                .append("，sourceId=").append(source.getId()).append("，结构=").append(source.getNextStructureName())
                .append("，S0=").append(LhScheduleTimeUtil.formatDateTime(first.getShiftStartDateTime()))
                .append("，S1=").append(Objects.isNull(second) ? "窗口外" : LhScheduleTimeUtil.formatDateTime(second.getShiftStartDateTime()))
                .append("，当前班=class").append(shift.getShiftIndex()).append("，阶段=")
                .append(shiftStage)
                .append("，结构物理机台=").append(machines).append("，上限结论=")
                .append(limitConclusion)
                .append("，特殊上限=").append(limit).append("，首台SKU=").append(firstResult.getMaterialCode())
                .append('/').append(firstResult.getProductStatus()).append("，SKU实际台数=").append(skuMachines.size())
                .append("，扫描目标快照=").append(targets).append("，").append(goal)
                .append("，曾生成同班合格提案物理机台数=").append(generated)
                .append("，最近扫描阶段汇总=").append(stages)
                .append("，证据说明=").append(stages.isEmpty() ? "未观测到机台扫描，不能判定资源不足"
                        : "扫描记录不是全资源穷举证明；合格提案不等于提交，实际台数以本时点正量结果为准")
                .append("\n").append(details(entries)).toString();
        PriorityTraceLogHelper.appendProcessLog(context, "结构切换逐班审计", detail);
        log.info("结构切换逐班审计, {}", detail);
    }

    /**
     * 按结构、班次及可选SKU收集正量物理机台，覆盖新增和换活字块，不计零量及纯准备。
     * @param context 最终结果上下文
     * @param structure 后结构
     * @param shift 真实班次
     * @param skuResult 可选的SKU身份
     * @return 去重物理机台
     */
    private static Set<String> machines(LhScheduleContext context, String structure, LhShiftConfigVO shift,
            LhScheduleResult skuResult) {
        return context.getScheduleResultList().stream()
                .filter(result -> StringUtils.equals(structure, result.getStructureName()))
                .filter(result -> StructureSwitchSchedulingPolicy.quantity(result, shift.getShiftIndex()) > 0)
                .filter(result -> Objects.isNull(skuResult) || StringUtils.equals(result.getMaterialCode(), skuResult.getMaterialCode())
                        && StringUtils.equals(result.getProductStatus(), skuResult.getProductStatus()))
                .map(result -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 输出有界的逐机快照，显式标明省略数量，避免把部分明细误当全部候选。
     * @param entries 已按结构、班次、首台SKU过滤的快照
     * @return 中文明细
     */
    private static String details(List<StructureSwitchShiftAuditEntry> entries) {
        StringBuilder detail = new StringBuilder(1024);
        entries.stream().limit(DETAIL_LIMIT).forEach(entry -> detail.append("阶段=").append(entry.getPhase())
                .append("，机台=").append(entry.getMachineCode()).append("，决策=").append(entry.getDecisionStage())
                .append("，原因=").append(entry.getReason()).append("，日期池=").append(entry.getPoolDate())
                .append("，完整预演模式=").append(entry.getTimingMode()).append("，换模开始=").append(entry.getChangeoverStart())
                .append("，首检或生产占用开始=").append(entry.getOccupationStart())
                .append("，普通生产开始=").append(entry.getFormalStart())
                .append("，提交观测=").append(entry.getSubmissionOutcome())
                .append("，曾有同班合格提案=").append(entry.isSameShiftProposalGenerated()).append('\n'));
        detail.append("明细总数=").append(entries.size()).append("，省略数=")
                .append(Math.max(0, entries.size() - DETAIL_LIMIT));
        return detail.toString();
    }

    /**
     * 只读选择同班首台；没有结果起点时沿用该有量班次起点，不影响任何生产字段。
     * @param result 有量结果
     * @param shift 已确认有量的班次
     * @return 审计排序时间
     */
    private static Date actualStart(LhScheduleResult result, LhShiftConfigVO shift) {
        Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
        return Objects.nonNull(start) ? start : shift.getShiftStartDateTime();
    }

    /**
     * 独立班次9副本不共享原窗口审计记录，避免重编号班次污染主批次证据。
     * @param context 原排程上下文
     * @return 是否采集本窗口审计
     */
    private static boolean enabled(LhScheduleContext context) {
        return Objects.nonNull(context) && !context.isIsolatedNextShiftPlan()
                && StructureSwitchSchedulingPolicy.isEnabled(context);
    }
}
