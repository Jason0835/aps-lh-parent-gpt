package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingAllocationSnapshot;
import com.zlt.aps.lh.engine.strategy.support.ContinuationMachineFinishPlan;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanArrangement;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanTimeline;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.engine.strategy.impl.ContinuousProductionStrategy;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 轻量设备计划公共编排。集中处理输入、复评、物理归集、时间适配和安排状态。
 * SKU选择、班次排量、截量以及资源消费仍由原排产策略执行。
 */
@Component
@Slf4j
public class LhEquipmentPlanScheduleService {

    /** 唯一精度业务判定入口。 */
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService = new LhMaintenanceScheduleService();
    /** 纯时间适配，不解释精度业务条件。 */
    @Resource
    private LhEquipmentPlanTimelineResolver timelineResolver = new LhEquipmentPlanTimelineResolver();
    /** 晚阶段只将受影响续作交回原策略恢复并重算，公共层不操作生产账本。 */
    @Resource
    @Lazy
    private ContinuousProductionStrategy continuousProductionStrategy;

    /**
     * 续作准备、裁量和降模完成后，正式扣账前集中建立长期在机强制约束。
     * 仅抽取准备结果，不重新运行续作，也不把机台初始化时间作为确认收尾。
     * @param context 已形成待提交续作结果的上下文
     * @return 是否发布了新的约束
     */
    public boolean prepareBeforeContinuousCommit(LhScheduleContext context) {
        return this.evaluateChangedInputs(context, true);
    }

    /**
     * 续作收口后及后续真实收尾变化时受控复评；0不撤销任何有效安排。
     * @param context 已提交当前生产事实的上下文
     * @return 是否改变约束，调用方须失效本轮候选、容量及时间缓存
     */
    public boolean reviewConfirmedEndings(LhScheduleContext context) {
        if (context.isTimedMachineOffLoadPreview() || context.isNewSpecProposalPreview()) {
            return false;
        }
        boolean changed = this.restoreUnboundParallelInspections(context);
        changed = this.evaluateChangedInputs(context, false) || changed;
        if (changed && context.isContinuousDailyQuotaSynced() && this.hasUnreconciledForceConstraint(context)) {
            if (Objects.isNull(continuousProductionStrategy)) {
                throw new IllegalStateException("精度复评需要续作消费恢复服务，禁止直接改写已扣账结果");
            }
            continuousProductionStrategy.reconcileEquipmentPlanConstraints(context);
        }
        return changed;
    }

    /**
     * 原机原模无切换恢复时，允许明确的SKU承接已冻结的精度首检。
     * 只返回未被实际结果消费的时间计划；普通换模/换活字块不得据此合并首检。
     * @param context 排程上下文
     * @param machine 当前机台
     * @param readyTime 原模恢复的就绪时刻
     * @return 待分配数量的固定时间计划，无待分配首检时为空
     */
    public EquipmentPlanTimeline resolveUnallocatedPrecisionInspection(LhScheduleContext context,
            MachineScheduleDTO machine, Date readyTime) {
        if (Objects.isNull(machine) || Objects.isNull(readyTime)) {
            return null;
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
        return context.getEquipmentPlanRuntimeState().getArrangementMap().values().stream()
                .filter(EquipmentPlanArrangement::isEffective).map(EquipmentPlanArrangement::getTimeline)
                .filter(timeline -> StringUtils.equals(physical, timeline.getPhysicalMachineCode()))
                .filter(timeline -> !readyTime.before(timeline.getProductionResumeTime()))
                .filter(timeline -> context.getScheduleResultList().stream()
                        .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0).noneMatch(result ->
                        Objects.nonNull(context.getFirstInspectionResultPlanMap().get(result))
                                && Objects.equals(timeline.getPrecisionPlanId(), context.getFirstInspectionResultPlanMap()
                                        .get(result).getEquipmentPlanId())))
                .findFirst().orElse(null);
    }

    /**
     * 原策略完成结果、首检和数量账本提交后，发布已冻结的公共首检时间，不重新排量或消费额度。
     * 候选预演不调用本方法；安排和实际窗口均由既有快照深拷贝恢复。
     * @param context 已提交结果的上下文
     * @param result 本次正式结果（L/R使用同一首检计划，只发布一次物理约束）
     */
    public void commitPrecisionInspection(LhScheduleContext context, LhScheduleResult result) {
        FirstInspectionAllocationPlan inspection = context.getFirstInspectionResultPlanMap().get(result);
        if (Objects.isNull(inspection) || Objects.isNull(inspection.getEquipmentPlanId())) {
            return;
        }
        EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState().getArrangementMap()
                .get(inspection.getEquipmentPlanId());
        if (Objects.isNull(arrangement) || !arrangement.isEffective() || !inspection.isValid()
                || !context.getScheduleResultList().contains(result) || ShiftFieldUtil.resolveScheduledQty(result) <= 0
                || inspection.getInspectionDurationSeconds() != LhEquipmentPlanTimelineResolver.PRECISION_INSPECTION_SECONDS
                || !StringUtils.equals(arrangement.getTimeline().getPhysicalMachineCode(),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))) {
            throw new IllegalStateException("精度公共首检缺少同一物理机台的有效安排或固定时间计划");
        }
        EquipmentPlanTimeline combined = arrangement.getTimeline().withInspection(
                inspection.getInspectionStartTime(), inspection.getInspectionEndTime());
        this.replaceInspectionTimeline(context, arrangement, combined, "切换与精度并行，共用固定2小时首检");
        if ("1".equals(result.getIsChangeMould()) || "1".equals(result.getIsTypeBlock())) {
            Date changeEnd = LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(), "1".equals(result.getIsTypeBlock())
                    ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context) : LhScheduleTimeUtil.getMouldChangeTotalHours(context));
            PriorityTraceLogHelper.appendProcessLog(context, "精度与切换并行时间轴",
                    "批次=" + context.getBatchNo() + "，工厂=" + context.getFactoryCode()
                            + "，物料=" + result.getMaterialCode() + "，产品状态=" + result.getProductStatus()
                            + "，物理机台=" + combined.getPhysicalMachineCode() + "，计划主键=" + combined.getPrecisionPlanId()
                            + "，动作=" + ("1".equals(result.getIsTypeBlock()) ? "换活字块" : "换模")
                            + "，切换开始=" + LhScheduleTimeUtil.formatDateTime(result.getMouldChangeStartTime())
                            + "，切换结束=" + LhScheduleTimeUtil.formatDateTime(changeEnd)
                            + "，保养开始=" + LhScheduleTimeUtil.formatDateTime(combined.getMaintenanceStartTime())
                            + "，保养结束=" + LhScheduleTimeUtil.formatDateTime(combined.getMaintenanceEndTime())
                            + "，公共首检开始=" + LhScheduleTimeUtil.formatDateTime(combined.getInspectionStartTime())
                            + "，公共首检量=" + inspection.getInspectionQty()
                            + "，恢复生产=" + LhScheduleTimeUtil.formatDateTime(combined.getProductionResumeTime()));
        }
    }

    /** 只替换首检约束；精度每日额度、原始判定和维护主键不重复登记。 */
    private void replaceInspectionTimeline(LhScheduleContext context, EquipmentPlanArrangement arrangement,
            EquipmentPlanTimeline timeline, String reason) {
        EquipmentPlanTimeline previous = arrangement.getTimeline();
        if (Objects.equals(previous.getInspectionStartTime(), timeline.getInspectionStartTime())
                && Objects.equals(previous.getProductionResumeTime(), timeline.getProductionResumeTime())) {
            return;
        }
        for (MachineScheduleDTO side : this.physicalMachines(context, timeline.getPhysicalMachineCode())) {
            side.getMaintenanceWindowList().replaceAll(window -> Objects.nonNull(window) && window.isEquipmentPlanManaged()
                    && Objects.equals(window.getPrecisionPlanId(), timeline.getPrecisionPlanId())
                    ? timeline.toMaintenanceWindow(side.getMachineCode()) : window);
        }
        context.getEquipmentPlanRuntimeState().getArrangementMap().put(timeline.getPrecisionPlanId(),
                arrangement.withTimeline(timeline, reason));
        context.getEquipmentPlanRuntimeState().constraintsChanged();
    }

    /**
     * 后置撤销了全部承接结果时，恢复独立精度首检；独立精度本身继续有效。
     * 仅扫描本批有效安排与已提交结果，不重排整批，也不恢复已由策略回退的生产账本。
     * @param context 当前上下文 @return 是否恢复了组合约束
     */
    private boolean restoreUnboundParallelInspections(LhScheduleContext context) {
        long version = context.getEquipmentPlanRuntimeState().getConstraintVersion();
        for (EquipmentPlanArrangement arrangement : new ArrayList<>(
                context.getEquipmentPlanRuntimeState().getArrangementMap().values())) {
            EquipmentPlanTimeline timeline = arrangement.getTimeline();
            if (!arrangement.isEffective() || Objects.equals(timeline.getInspectionStartTime(), timeline.getMaintenanceEndTime())) {
                continue;
            }
            boolean bound = context.getScheduleResultList().stream()
                    .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                    .map(result -> context.getFirstInspectionResultPlanMap().get(result)).filter(Objects::nonNull)
                    .anyMatch(plan -> Objects.equals(timeline.getPrecisionPlanId(), plan.getEquipmentPlanId()));
            if (!bound) {
                Date inspectionStart = timeline.getMaintenanceEndTime();
                Date inspectionEnd = new Date(inspectionStart.getTime()
                        + LhEquipmentPlanTimelineResolver.PRECISION_INSPECTION_SECONDS * 1000L);
                this.replaceInspectionTimeline(context, arrangement, timeline.withInspection(inspectionStart, inspectionEnd),
                        "切换承接结果已撤销，恢复独立精度公共首检");
                PriorityTraceLogHelper.appendProcessLog(context, "精度并行安排恢复",
                        "批次=" + context.getBatchNo() + "，物理机台=" + timeline.getPhysicalMachineCode()
                                + "，计划主键=" + timeline.getPrecisionPlanId() + "，原因=有效承接结果已撤销"
                                + "，恢复生产=" + LhScheduleTimeUtil.formatDateTime(inspectionEnd));
            }
        }
        return version != context.getEquipmentPlanRuntimeState().getConstraintVersion();
    }

    /** @param context 已扣账上下文 @return 是否仍有续作生产穿过新强制截止 */
    private boolean hasUnreconciledForceConstraint(LhScheduleContext context) {
        return context.getScheduleResultList().stream().filter(result -> "01".equals(result.getScheduleType())
                        && !"1".equals(result.getIsTypeBlock()) && !"1".equals(result.getIsChangeMould()))
                .anyMatch(result -> {
                    MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
                    Date cutoff = maintenanceScheduleService.resolveForceDownCutoffTime(machine);
                    Date end = ShiftFieldUtil.getShiftEndTime(result, ShiftFieldUtil.resolveLastPlannedShiftIndex(result));
                    Date start = ShiftFieldUtil.getShiftStartTime(result, ShiftFieldUtil.resolveFirstPlannedShiftIndex(result));
                    return Objects.nonNull(cutoff) && Objects.nonNull(start)
                            && (start.before(cutoff) || context.getContinuousProductionConsumptionMap().containsKey(result))
                            && Objects.nonNull(end) && end.after(cutoff);
                });
    }

    /**
     * 同一稳定输入只判断一次；已接受安排通过独立生命周期管理，不参加重复判定。
     * @param context 排程上下文
     * @param beforeCommit 是否为最终续作扣账前
     * @return 约束是否变化
     */
    private boolean evaluateChangedInputs(LhScheduleContext context, boolean beforeCommit) {
        if (Objects.isNull(context) || context.getMaintenancePlanMap().isEmpty()) {
            return false;
        }
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        long startedNanos = System.nanoTime();
        long previousVersion = state.getConstraintVersion();
        Map<String, Date> predictions = new LinkedHashMap<>();
        Map<String, Date> confirmed = new LinkedHashMap<>();
        Map<String, String> versions = new LinkedHashMap<>();
        String quotaVersion = this.quotaFingerprint(context);
        List<MachineScheduleDTO> machines = context.getMachineScheduleMap().values().stream()
                .filter(Objects::nonNull).filter(machine -> StringUtils.isNotBlank(machine.getMachineCode()))
                .filter(machine -> Objects.nonNull(maintenanceScheduleService.resolveMaintenancePlan(context, machine.getMachineCode())))
                .sorted(Comparator.comparing(MachineScheduleDTO::getMachineCode)).collect(Collectors.toList());
        Set<String> changedPhysicalMachines = new LinkedHashSet<>();
        for (MachineScheduleDTO machine : machines) {
            String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
            if (this.hasEffectiveArrangement(context, physicalMachine)
                    || (beforeCommit && !maintenanceScheduleService.shouldCheckLongOnlineMaintenance(context, machine))) {
                continue;
            }
            Date prediction = beforeCommit ? this.resolvePreparedPhysicalEnding(context, machine) : null;
            Date ending = beforeCommit ? this.resolveInitiallyIdleEnding(context, machine)
                    : this.resolveConfirmedPhysicalEnding(context, machine);
            String version = this.inputFingerprint(context, machine, prediction, ending, beforeCommit, quotaVersion);
            predictions.put(machine.getMachineCode(), prediction);
            confirmed.put(machine.getMachineCode(), ending);
            versions.put(machine.getMachineCode(), version);
            // 指纹按运行态机台保存，判定结果按物理归集，避免L/R的0覆盖有效一侧。
            if (!Objects.equals(state.getInputVersionMap().get(machine.getMachineCode()), version)) {
                changedPhysicalMachines.add(physicalMachine);
            }
        }
        // 任一侧事实变化时整台复评，仍由唯一判定入口按精度优先级选择有效计划主键。
        List<MachineScheduleDTO> changedMachines = machines.stream()
                .filter(machine -> versions.containsKey(machine.getMachineCode()))
                .filter(machine -> changedPhysicalMachines.contains(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode())))
                .collect(Collectors.toList());
        if (changedMachines.isEmpty()) {
            return false;
        }
        changedMachines.forEach(machine -> state.getInputVersionMap().put(machine.getMachineCode(), versions.get(machine.getMachineCode())));
        Map<String, PrecisionMaintenanceJudgeDTO> physicalDecisions = this.resolveSupportedDecisions(
                context, changedMachines, predictions, confirmed, versions);
        for (Map.Entry<String, PrecisionMaintenanceJudgeDTO> entry : physicalDecisions.entrySet()) {
            PrecisionMaintenanceJudgeDTO decision = entry.getValue();
            state.getDecisionMap().put(entry.getKey(), EquipmentPlanRuntimeState.copyDecision(decision));
            if (decision.getDurationHours() <= 0) {
                state.getPendingPhysicalMachines().add(entry.getKey());
                state.getCandidateTimelineMap().remove(entry.getKey());
                continue;
            }
            if (Objects.isNull(decision.getPrecisionPlanId())) {
                this.recordUnsupported(context, entry.getKey(), "有效精度判定未返回计划主键，不能登记安排");
                continue;
            }
            EquipmentPlanTimeline timeline = timelineResolver.resolvePrecision(context, decision,
                    versions.get(decision.getMachineCode()));
            state.getCandidateTimelineMap().put(entry.getKey(), timeline);
            state.getPendingPhysicalMachines().add(entry.getKey());
            this.acceptTimeline(context, timeline);
        }
        log.info("设备计划受控复评完成，批次={}，工厂={}，阶段={}，物理机台数={}，新增约束数={}，耗时毫秒={}",
                context.getBatchNo(), context.getFactoryCode(), beforeCommit ? "续作扣账前" : "确认收尾后",
                changedPhysicalMachines.size(), state.getConstraintVersion() - previousVersion,
                (System.nanoTime() - startedNanos) / 1000000L);
        return previousVersion != state.getConstraintVersion();
    }

    /**
     * 未覆盖组合不能继续占用纯判定的批内预留。每轮剔除至少一台拒绝机台后重新判断其余机台，
     * 包括因先前临时额度而返回0的长期预测；只重评设备计划，不重跑续作或改生产账本。
     * @param context 上下文
     * @param machines 本次输入变化的物理组
     * @param predictions 准备预测
     * @param confirmed 确认事实
     * @param versions 稳定输入版本
     * @return 没有被未接受预留阻挡的最终判定
     */
    private Map<String, PrecisionMaintenanceJudgeDTO> resolveSupportedDecisions(LhScheduleContext context,
            List<MachineScheduleDTO> machines, Map<String, Date> predictions,
            Map<String, Date> confirmed, Map<String, String> versions) {
        List<MachineScheduleDTO> remaining = new ArrayList<>(machines);
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        while (!remaining.isEmpty()) {
            Map<String, PrecisionMaintenanceJudgeDTO> physicalDecisions = new LinkedHashMap<>();
            for (PrecisionMaintenanceJudgeDTO decision : maintenanceScheduleService.resolvePrecisionMaintenanceList(
                    context, remaining, predictions, confirmed)) {
                String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(decision.getMachineCode());
                PrecisionMaintenanceJudgeDTO previous = physicalDecisions.get(physical);
                if (Objects.isNull(previous) || (previous.getDurationHours() <= 0 && decision.getDurationHours() > 0)) {
                    physicalDecisions.put(physical, decision);
                }
            }
            Set<String> rejected = new LinkedHashSet<>();
            for (Map.Entry<String, PrecisionMaintenanceJudgeDTO> entry : physicalDecisions.entrySet()) {
                PrecisionMaintenanceJudgeDTO decision = entry.getValue();
                if (decision.getDurationHours() <= 0) {
                    continue;
                }
                String reason = "有效精度判定未返回计划主键，不能登记安排";
                if (Objects.nonNull(decision.getPrecisionPlanId())) {
                    EquipmentPlanTimeline timeline = timelineResolver.resolvePrecision(context, decision,
                            versions.get(decision.getMachineCode()));
                    state.getCandidateTimelineMap().put(entry.getKey(), timeline);
                    reason = this.resolveAcceptanceRejectReason(context, timeline);
                }
                if (StringUtils.isNotBlank(reason)) {
                    rejected.add(entry.getKey());
                    state.getDecisionMap().put(entry.getKey(), EquipmentPlanRuntimeState.copyDecision(decision));
                    state.getPendingPhysicalMachines().add(entry.getKey());
                    this.recordUnsupported(context, entry.getKey(), reason);
                }
            }
            if (rejected.isEmpty()) {
                return physicalDecisions;
            }
            remaining.removeIf(machine -> rejected.contains(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode())));
        }
        return java.util.Collections.emptyMap();
    }

    /** @param context 上下文 @param timeline 候选时间计划 @return 正式接受门禁的拒绝原因 */
    private String resolveAcceptanceRejectReason(LhScheduleContext context, EquipmentPlanTimeline timeline) {
        String conflict = this.resolveUnsupportedCombination(context, timeline);
        if (StringUtils.isNotBlank(conflict)) {
            return conflict;
        }
        boolean inWindow = context.getScheduleWindowShifts().stream().anyMatch(shift ->
                this.overlaps(timeline.getMaintenanceStartTime(), timeline.getMaintenanceEndTime(),
                        shift.getShiftStartDateTime(), shift.getShiftEndDateTime()));
        return inWindow ? null : "精度候选不在本次班次覆盖范围，不登记安排或回填";
    }

    /**
     * 抽取物理机台全部在机侧准备结果的最晚自然收尾；缺少任一侧证明即返回未知。
     * @param context 续作准备上下文
     * @param machine 任一运行态侧
     * @return 预测事实，仅供长期在机等待复评
     */
    private Date resolvePreparedPhysicalEnding(LhScheduleContext context, MachineScheduleDTO machine) {
        Date latestEnding = context.getScheduleDate();
        for (MachineScheduleDTO side : this.physicalMachines(context, machine.getMachineCode())) {
            List<LhScheduleResult> results = context.getScheduleResultList().stream()
                    .filter(result -> StringUtils.equals(side.getMachineCode(), result.getLhMachineCode()))
                    .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0).collect(Collectors.toList());
            if (results.isEmpty()) {
                if (StringUtils.isBlank(side.getCurrentMaterialCode())) {
                    continue;
                }
                if (!context.getReleasedContinuousMachineCodeSet().contains(side.getMachineCode())) {
                    return null;
                }
                latestEnding = this.later(latestEnding, side.getEstimatedEndTime());
                continue;
            }
            LhScheduleResult last = results.stream().max(Comparator.comparing(
                    LhScheduleResult::getSpecEndTime, Comparator.nullsFirst(Date::compareTo))).orElse(null);
            ContinuationEndingAllocationSnapshot snapshot = context.getContinuationSurplusEndingSnapshotMap().get(last);
            boolean completedSnapshot = Objects.nonNull(snapshot)
                    && Objects.nonNull(snapshot.getProductionEndTime())
                    && Objects.equals(snapshot.getProductionEndTime(), ShiftFieldUtil.getShiftEndTime(last,
                            ShiftFieldUtil.resolveLastPlannedShiftIndex(last)))
                    && Objects.nonNull(snapshot.getShiftQuantities())
                    && snapshot.getShiftQuantities().length == LhScheduleConstant.MAX_SHIFT_SLOT_COUNT
                    && IntStream.rangeClosed(1, snapshot.getShiftQuantities().length).allMatch(index ->
                            snapshot.getShiftQuantities()[index - 1] == (Objects.isNull(ShiftFieldUtil.getShiftPlanQty(last, index))
                                    ? 0 : ShiftFieldUtil.getShiftPlanQty(last, index)))
                    && snapshot.getFinishState() == ContinuationMachineFinishPlan.FinishState.SURPLUS_COMPLETED;
            if (!completedSnapshot && (!"1".equals(last.getIsEnd()) || Objects.isNull(last.getSpecEndTime()))) {
                return null;
            }
            latestEnding = this.later(latestEnding, completedSnapshot ? snapshot.getProductionEndTime() : last.getSpecEndTime());
        }
        return latestEnding;
    }

    /** @param context 排程上下文 @param machine 任一侧 @return 全部侧确实空闲时的已知起点 */
    private Date resolveInitiallyIdleEnding(LhScheduleContext context, MachineScheduleDTO machine) {
        boolean idle = this.physicalMachines(context, machine.getMachineCode()).stream()
                .allMatch(side -> StringUtils.isBlank(side.getCurrentMaterialCode())
                        && context.getScheduleResultList().stream().noneMatch(result ->
                                StringUtils.equals(side.getMachineCode(), result.getLhMachineCode())));
        return idle ? context.getScheduleDate() : null;
    }

    /**
     * 只有有效释放事件、确认收尾快照或最终正量收尾结果才能提供确认事实。
     * 运行态ending和初始化estimatedEndTime本身不能证明在机SKU已经收尾。
     * @param context 当前已提交上下文
     * @param machine 任一物理侧
     * @return 全部活跃侧均被证明后的最晚释放时间
     */
    private Date resolveConfirmedPhysicalEnding(LhScheduleContext context, MachineScheduleDTO machine) {
        for (MachineScheduleDTO side : this.physicalMachines(context, machine.getMachineCode())) {
            if (StringUtils.isBlank(side.getCurrentMaterialCode())
                    || context.getReleasedContinuousMachineCodeSet().contains(side.getMachineCode())) {
                continue;
            }
            boolean completed = context.getScheduleResultList().stream()
                    .filter(result -> StringUtils.equals(side.getMachineCode(), result.getLhMachineCode()))
                    .filter(result -> StringUtils.equals(side.getCurrentMaterialCode(), result.getMaterialCode()))
                    .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                    .anyMatch(result -> "1".equals(result.getIsEnd())
                            && Objects.nonNull(side.getEstimatedEndTime())
                            && Objects.nonNull(result.getSpecEndTime())
                            && !result.getSpecEndTime().after(side.getEstimatedEndTime()));
            if (!completed) {
                return null;
            }
        }
        return maintenanceScheduleService.resolvePhysicalKnownEndingTime(context, machine);
    }

    /**
     * 正式接受单场景时间计划，同时发布L/R实际窗口与物理额度；未覆盖组合明确拒绝。
     * @param context 排程上下文
     * @param timeline 已适配的候选时间计划
     */
    private void acceptTimeline(LhScheduleContext context, EquipmentPlanTimeline timeline) {
        if (this.hasEffectiveArrangement(context, timeline.getPhysicalMachineCode())) {
            return;
        }
        String conflict = this.resolveAcceptanceRejectReason(context, timeline);
        if (StringUtils.isNotBlank(conflict)) {
            this.recordUnsupported(context, timeline.getPhysicalMachineCode(), conflict);
            return;
        }
        for (MachineScheduleDTO side : this.physicalMachines(context, timeline.getPhysicalMachineCode())) {
            side.getMaintenanceWindowList().add(timeline.toMaintenanceWindow(side.getMachineCode()));
            side.setHasMaintenancePlan(true);
            side.setMaintenancePlanTime(LhScheduleTimeUtil.clearTime(timeline.getMaintenanceStartTime()));
        }
        String dateKey = LhScheduleTimeUtil.formatDate(timeline.getMaintenanceStartTime());
        Set<String> machines = context.getDailyMaintenancePhysicalMachineSetMap()
                .computeIfAbsent(dateKey, key -> new LinkedHashSet<>());
        int legacyCount = Math.max(0, context.getDailyMaintenanceCountMap().getOrDefault(dateKey, 0) - machines.size());
        machines.add(timeline.getPhysicalMachineCode());
        context.getDailyMaintenanceCountMap().put(dateKey, machines.size() + legacyCount);
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        state.getArrangementMap().put(timeline.getPrecisionPlanId(), new EquipmentPlanArrangement(
                timeline, EquipmentPlanArrangement.Status.ACCEPTED, "单场景时间计划正式接受",
                context.getScheduleResultList().stream().filter(result -> StringUtils.equals(
                        timeline.getPhysicalMachineCode(), LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                        .collect(Collectors.toList())));
        state.getPendingPhysicalMachines().remove(timeline.getPhysicalMachineCode());
        state.constraintsChanged();
        PriorityTraceLogHelper.appendProcessLog(context, "精准计划最终安排",
                "批次=" + context.getBatchNo() + "，工厂=" + context.getFactoryCode()
                        + "，物理机台=" + timeline.getPhysicalMachineCode() + "，计划主键=" + timeline.getPrecisionPlanId()
                        + "，保养开始=" + LhScheduleTimeUtil.formatDateTime(timeline.getMaintenanceStartTime())
                        + "，保养结束=" + LhScheduleTimeUtil.formatDateTime(timeline.getMaintenanceEndTime())
                        + "，固定首检结束=" + LhScheduleTimeUtil.formatDateTime(timeline.getProductionResumeTime())
                        + "，强制下机=" + timeline.isForceDown());
    }

    /**
     * 撤销只移除本安排发布的窗口及额度；生产撤销由策略按消费记录完成后调用。
     * @param context 排程上下文
     * @param planId 有效判定主键
     * @param reason 撤销原因
     */
    public void revokeArrangement(LhScheduleContext context, Long planId, String reason) {
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        EquipmentPlanArrangement arrangement = state.getArrangementMap().get(planId);
        if (Objects.isNull(arrangement) || !arrangement.isEffective()) {
            return;
        }
        EquipmentPlanTimeline timeline = arrangement.getTimeline();
        for (MachineScheduleDTO side : this.physicalMachines(context, timeline.getPhysicalMachineCode())) {
            side.getMaintenanceWindowList().removeIf(window -> window.isEquipmentPlanManaged()
                    && Objects.equals(planId, window.getPrecisionPlanId()));
            side.setHasMaintenancePlan(!side.getMaintenanceWindowList().isEmpty());
            if (!side.isHasMaintenancePlan()) {
                side.setMaintenancePlanTime(null);
            }
            state.getInputVersionMap().remove(side.getMachineCode());
        }
        String dateKey = LhScheduleTimeUtil.formatDate(timeline.getMaintenanceStartTime());
        Set<String> occupied = context.getDailyMaintenancePhysicalMachineSetMap().get(dateKey);
        if (Objects.nonNull(occupied) && occupied.remove(timeline.getPhysicalMachineCode())) {
            context.getDailyMaintenanceCountMap().computeIfPresent(dateKey,
                    (key, count) -> Math.max(occupied.size(), count - 1));
        }
        state.getArrangementMap().put(planId, arrangement.withStatus(EquipmentPlanArrangement.Status.REVOKED, reason));
        state.getCandidateTimelineMap().remove(timeline.getPhysicalMachineCode());
        state.getPendingPhysicalMachines().add(timeline.getPhysicalMachineCode());
        state.constraintsChanged();
        PriorityTraceLogHelper.appendProcessLog(context, "精准计划安排撤销",
                "计划主键=" + planId + "，物理机台=" + timeline.getPhysicalMachineCode() + "，原因=" + reason);
    }

    /**
     * 所有后置裁量结束后校验安排与实际窗口一致，再形成回填清单；独立安排不依赖后续SKU。
     * @param context 最终结果上下文
     * @return 按有效判定主键去重的安排日期
     */
    public Map<Long, Date> finalizeArrangements(LhScheduleContext context) {
        this.restoreUnboundParallelInspections(context);
        Map<Long, Date> result = new LinkedHashMap<>();
        for (EquipmentPlanArrangement arrangement : new ArrayList<>(
                context.getEquipmentPlanRuntimeState().getArrangementMap().values())) {
            if (!arrangement.isEffective()) {
                continue;
            }
            EquipmentPlanTimeline timeline = arrangement.getTimeline();
            List<MachineScheduleDTO> sides = this.physicalMachines(context, timeline.getPhysicalMachineCode());
            boolean valid = !sides.isEmpty() && sides.stream().allMatch(side ->
                    side.getMaintenanceWindowList().stream().anyMatch(window -> window.isEquipmentPlanManaged()
                            && Objects.equals(window.getPrecisionPlanId(), timeline.getPrecisionPlanId())
                            && Objects.equals(window.getMaintenanceStartTime(), timeline.getMaintenanceStartTime())
                            && Objects.equals(window.getMaintenanceEndTime(), timeline.getMaintenanceEndTime())
                            && Objects.equals(window.getFirstInspectionStartTime(), timeline.getInspectionStartTime())
                            && Objects.equals(window.getFirstInspectionEndTime(), timeline.getProductionResumeTime())
                            && Objects.equals(window.getProductionResumeTime(), timeline.getProductionResumeTime())));
            if (!valid) {
                this.revokeArrangement(context, timeline.getPrecisionPlanId(), "最终实际窗口已撤销或与接受时间计划不一致");
                continue;
            }
            this.validateFinalProduction(context, timeline);
            context.getEquipmentPlanRuntimeState().getArrangementMap().put(timeline.getPrecisionPlanId(),
                    arrangement.withStatus(EquipmentPlanArrangement.Status.FINALIZED, "后置裁量及安排一致性收口完成"));
            result.put(timeline.getPrecisionPlanId(), LhScheduleTimeUtil.clearTime(timeline.getMaintenanceStartTime()));
        }
        return result;
    }

    /**
     * 后置回裁完成后，按正式排量同一维护容量窗口核对受影响班次；不通过修改已扣账数量掩盖穿透。
     * @param context 最终上下文
     * @param timeline 仍有效的安排
     */
    private void validateFinalProduction(LhScheduleContext context, EquipmentPlanTimeline timeline) {
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!StringUtils.equals(timeline.getPhysicalMachineCode(),
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))) {
                continue;
            }
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
            EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState().getArrangementMap().get(timeline.getPrecisionPlanId());
            Date lastEnd = ShiftFieldUtil.getShiftEndTime(result, ShiftFieldUtil.resolveLastPlannedShiftIndex(result));
            if (arrangement.constrainsSourceResult(result) && Objects.nonNull(lastEnd)
                    && lastEnd.after(timeline.getMaintenanceStartTime())) {
                throw new IllegalStateException("原生产结果在设备安排接受后穿透维护截止：" + result.getLhMachineCode());
            }
            SkuScheduleDTO sku = context.getScheduleResultSourceSkuMap().get(result);
            FirstInspectionAllocationPlan inspection = context.getFirstInspectionResultPlanMap().get(result);
            if (Objects.nonNull(inspection) && Objects.equals(timeline.getPrecisionPlanId(), inspection.getEquipmentPlanId())
                    && (!Objects.equals(timeline.getInspectionStartTime(), inspection.getInspectionStartTime())
                    || !Objects.equals(timeline.getProductionResumeTime(), inspection.getInspectionEndTime()))) {
                throw new IllegalStateException("精度公共首检与最终有效安排时间不一致：" + result.getLhMachineCode());
            }
            Map<Integer, Integer> inspectionQuantities = Objects.nonNull(inspection)
                    && Objects.equals(timeline.getPrecisionPlanId(), inspection.getEquipmentPlanId())
                    ? FirstInspectionAllocationUtil.toShiftQtyMap(inspection) : java.util.Collections.emptyMap();
            List<MachineMaintenanceWindowDTO> windows = ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                    context, context.getDevicePlanShutList(), machine.getMachineCode(), machine.getMaintenanceWindowList());
            for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                Date start = ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex());
                Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
                if (Objects.isNull(quantity) || quantity <= 0 || !this.overlaps(start, end,
                        timeline.getMaintenanceStartTime(), timeline.getProductionResumeTime())) {
                    continue;
                }
                if (Objects.isNull(sku)) {
                    throw new IllegalStateException("精度最终容量校验缺少结果来源SKU：" + result.getLhMachineCode());
                }
                int baseCapacity = ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                        ShiftCapacityResolverUtil.resolveRuntimeShiftCapacity(context, machine, sku.getShiftCapacity()),
                        shift, ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context), result.getScheduleType());
                int normalCapacity = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(
                        context.getDevicePlanShutList(), machine.getCleaningWindowList(), windows,
                        machine.getMachineCode(), start, end, baseCapacity, sku.getLhTimeSeconds(),
                        ShiftCapacityResolverUtil.resolveMachineMouldQty(result.getMouldQty()),
                        ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                        context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY),
                        context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS),
                        context.getParamIntValue(LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY));
                int inspectionQty = inspectionQuantities.getOrDefault(shift.getShiftIndex(), 0);
                if (quantity > normalCapacity + inspectionQty) {
                    String reason = "精度最终班次容量不一致，物料=" + result.getMaterialCode()
                            + "，机台=" + machine.getMachineCode() + "，班次=" + shift.getShiftIndex()
                            + "，实际=" + quantity + "，正常产能=" + normalCapacity + "，已归属首检=" + inspectionQty;
                    this.recordUnsupported(context, timeline.getPhysicalMachineCode(), reason);
                    throw new IllegalStateException(reason);
                }
            }
        }
    }

    /** @param context 上下文 @param machineCode 任一侧或物理机台 @return 是否已有有效安排 */
    private boolean hasEffectiveArrangement(LhScheduleContext context, String machineCode) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getEquipmentPlanRuntimeState().getArrangementMap().values().stream()
                .anyMatch(arrangement -> arrangement.isEffective()
                        && StringUtils.equals(physical, arrangement.getTimeline().getPhysicalMachineCode()));
    }

    /** @param context 上下文 @param machineCode 任一侧或物理机台 @return 全部物理侧 */
    private List<MachineScheduleDTO> physicalMachines(LhScheduleContext context, String machineCode) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return context.getMachineScheduleMap().values().stream().filter(Objects::nonNull)
                .filter(side -> StringUtils.equals(physical,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(side.getMachineCode())))
                .collect(Collectors.toList());
    }

    /** @param context 上下文 @param timeline 单场景候选 @return 尚未覆盖的组合原因，空表示未发现重叠 */
    private String resolveUnsupportedCombination(LhScheduleContext context, EquipmentPlanTimeline timeline) {
        Date start = timeline.getMaintenanceStartTime();
        Date end = timeline.getProductionResumeTime();
        for (MdmDevicePlanShut stop : context.getDevicePlanShutList()) {
            if (Objects.nonNull(stop) && StringUtils.equals(timeline.getPhysicalMachineCode(),
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(stop.getMachineCode()))
                    && this.overlaps(start, end, stop.getBeginDate(), stop.getEndDate())) {
                return "精度与设备停机/维修重叠尚未接入，停机类型=" + stop.getMachineStopType();
            }
        }
        for (MachineScheduleDTO side : this.physicalMachines(context, timeline.getPhysicalMachineCode())) {
            for (MachineCleaningWindowDTO cleaning : side.getCleaningWindowList()) {
                if (Objects.nonNull(cleaning) && this.overlaps(start, end, cleaning.getCleanStartTime(), cleaning.getReadyTime())) {
                    return "精度与清洗重叠尚未接入";
                }
            }
            for (MachineMaintenanceWindowDTO window : side.getMaintenanceWindowList()) {
                if (Objects.nonNull(window) && this.overlaps(start, end, window.getMaintenanceStartTime(),
                        Objects.nonNull(window.getProductionResumeTime())
                                ? window.getProductionResumeTime() : window.getMaintenanceEndTime())) {
                    return "维护之间的重叠尚未接入";
                }
            }
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            Date changeoverEnd = Objects.isNull(result.getMouldChangeStartTime()) ? null
                    : LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(), "1".equals(result.getIsTypeBlock())
                            ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                            : LhScheduleTimeUtil.getMouldChangeTotalHours(context));
            if (StringUtils.equals(timeline.getPhysicalMachineCode(),
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode()))
                    && this.overlaps(start, end, result.getMouldChangeStartTime(), changeoverEnd)) {
                return "精度与已提交换模/换活字块重叠尚未接入";
            }
        }
        return null;
    }

    /** @param context 上下文 @param physicalMachine 物理机台 @param reason 组合拒绝原因 */
    private void recordUnsupported(LhScheduleContext context, String physicalMachine, String reason) {
        if (context.getEquipmentPlanRuntimeState().getDiagnosticKeys().add(physicalMachine + "|" + reason)) {
            PriorityTraceLogHelper.appendProcessLog(context, "设备计划未覆盖组合",
                    "批次=" + context.getBatchNo() + "，工厂=" + context.getFactoryCode()
                            + "，物理机台=" + physicalMachine + "，原因=" + reason);
        }
    }

    /** @param context 上下文 @return 包含兼容计数的稳定物理额度指纹 */
    private String quotaFingerprint(LhScheduleContext context) {
        return context.getDailyMaintenancePhysicalMachineSetMap().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue().stream().sorted().collect(Collectors.joining(",")))
                .collect(Collectors.joining(";")) + "|" + context.getDailyMaintenanceCountMap().entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(Object::toString).collect(Collectors.joining(";"));
    }

    /** 构造与当前判定事实关联的指纹，额度或收尾事实变化时0结论允许复评。 */
    private String inputFingerprint(LhScheduleContext context, MachineScheduleDTO machine,
                                    Date prediction, Date confirmed, boolean beforeCommit, String quotaVersion) {
        LhPrecisionPlan plan = maintenanceScheduleService.resolveMaintenancePlan(context, machine.getMachineCode());
        return beforeCommit + "|" + machine.getCurrentMaterialCode() + "|" + machine.isEnding()
                + "|" + (Objects.isNull(prediction) ? null : prediction.getTime())
                + "|" + (Objects.isNull(confirmed) ? null : confirmed.getTime())
                + "|" + (Objects.isNull(plan) ? null : plan.getId() + ":" + plan.getDaysToDue() + ":" + plan.getPlanDate())
                + "|" + quotaVersion + "|" + this.occupationFingerprint(context, machine.getMachineCode());
    }

    /**
     * 清洗处置、维修实际时间或配对侧状态变化也须触发复评，不能永久缓存曾被组合冲突拒绝的0结果。
     * @param context 当前上下文
     * @param machineCode 任一物理侧
     * @return 只含相关机台实际占用的稳定指纹
     */
    private String occupationFingerprint(LhScheduleContext context, String machineCode) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        List<String> occupations = new ArrayList<>();
        for (MachineScheduleDTO side : this.physicalMachines(context, machineCode)) {
            occupations.add(side.getMachineCode() + ":" + side.getCurrentMaterialCode() + ":" + side.isEnding()
                    + ":" + side.getEstimatedEndTime());
            side.getCleaningWindowList().stream().filter(Objects::nonNull).forEach(window ->
                    occupations.add("清洗:" + window.getCleanStartTime() + ":" + window.getReadyTime()));
            side.getMaintenanceWindowList().stream().filter(Objects::nonNull).forEach(window ->
                    occupations.add("维护:" + window.getPrecisionPlanId() + ":" + window.getMaintenanceStartTime()
                            + ":" + window.getProductionResumeTime()));
        }
        context.getDevicePlanShutList().stream().filter(Objects::nonNull)
                .filter(stop -> StringUtils.equals(physical, LhSingleControlMachineUtil.resolvePhysicalMachineCode(stop.getMachineCode())))
                .forEach(stop -> occupations.add("停机:" + stop.getMachineStopType() + ":" + stop.getBeginDate() + ":" + stop.getEndDate()));
        return occupations.stream().sorted().collect(Collectors.joining(";"));
    }

    /** @param first 当前时间 @param second 候选时间 @return 非空较晚值 */
    private Date later(Date first, Date second) {
        return Objects.isNull(first) || (Objects.nonNull(second) && second.after(first)) ? second : first;
    }

    /** 按半开区间判断已知占用重叠；未定义重叠策略时仅报告冲突，不计算串并行结果。 */
    private boolean overlaps(Date firstStart, Date firstEnd, Date secondStart, Date secondEnd) {
        return Objects.nonNull(firstStart) && Objects.nonNull(firstEnd)
                && Objects.nonNull(secondStart) && Objects.nonNull(secondEnd)
                && firstStart.before(secondEnd) && secondStart.before(firstEnd);
    }
}
