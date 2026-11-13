package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.UnscheduledReasonEnum;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.ActiveMachineBinding;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DailyUnscheduledSkuSubstitutionPolicy;
import com.zlt.aps.lh.engine.strategy.support.DayDrivenScheduleState;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.MachinePriorityMetricSnapshot;
import com.zlt.aps.lh.engine.strategy.support.HistoricalResidualCapacityInfo;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAssignmentPlan;
import com.zlt.aps.lh.engine.strategy.support.NewSpecProposalRoundCache;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchCandidateScope;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日末结构切换置换，接管形成合法正量后保留结果，原物料迁移失败恢复待排。
 * 历史指定按物理机保护，迁移允许减量，接管失败恢复完整资源与日驱动状态。
 *
 * @author Codex
 * @date 2026/11/12
 */
@Slf4j
@Service
public class StructureSwitchResultSubstitutionCoordinator {
    /**
     * 结构切换置换结果首个正量班次的固定中文原因说明。
     */
    private static final String STRUCTURE_SWITCH_SUBSTITUTION_ANALYSIS = "结构切换置换";

    /**
     * 普通新增既有的资源撤销与机台前态恢复能力。
     */
    @Resource
    private NewSpecResultSubstitutionCoordinator resultSubstitution;
    /**
     * 原结构切换资格、选机和提交能力。
     */
    @Resource
    @Lazy
    private StructureSwitchSchedulingService structureScheduling;
    /**
     * 正常新增单组合预演与提交内核。
     */
    @Resource
    @Lazy
    private NewSpecProductionStrategy productionStrategy;
    /**
     * 本批共用策略工厂。
     */
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    /**
     * 结构占用重建能力。
     */
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    /**
     * 胶囊消费重建能力。
     */
    @Resource
    private CapsuleReplacementRuleService capsuleReplacementRuleService;

    /**
     * 在日末候选构建之前检查是否存在可置换的结构切换绑定。
     *
     * @param context 当前结果
     * @param day 当前业务日
     * @param state 外层绑定
     * @return 存在可置换来源时返回 true
     */
    public boolean hasEligibleBinding(LhScheduleContext context, DayScheduleContext day, DayDrivenScheduleState state) {
        return state.getActiveBindings().stream().anyMatch(binding -> this.isEligibleVictim(context, day, state, binding));
    }

    /**
     * 按业务顺序对日末零正量候选执行一次有限置换。
     *
     * @param context 共享资源和结果
     * @param day 当前业务日
     * @param state 外层日驱动状态
     * @param candidates 当日仍具备正常准入的候选
     */
    public void substitute(LhScheduleContext context, DayScheduleContext day, DayDrivenScheduleState state,
            List<DailyNewSpecCandidate> candidates) {
        long started = System.nanoTime();
        int snapshotCount = 0;
        int attemptCount = 0;
        int successCount = 0;
        List<ActiveMachineBinding> bindings = state.getActiveBindings().stream()
                .filter(binding -> this.isEligibleVictim(context, day, state, binding))
                .collect(Collectors.toList());
        if (bindings.isEmpty()) {
            return;
        }
        List<DailyNewSpecCandidate> pending = DailyUnscheduledSkuSubstitutionPolicy.collectCandidates(
                context, day, candidates);
        Set<String> consumedMachines = new LinkedHashSet<>();
        for (DailyNewSpecCandidate candidate : pending) {
            if (candidate.getRemainingMachineCount() <= 0) {
                continue;
            }
            List<ActiveMachineBinding> available = bindings.stream()
                    .filter(binding -> !consumedMachines.contains(
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode())))
                    .filter(binding -> this.isEligibleVictim(context, day, state, binding))
                    .collect(Collectors.toList());
            if (available.isEmpty()) {
                break;
            }
            List<SkuScheduleDTO> affectedSkus = available.stream().map(ActiveMachineBinding::getSku)
                    .collect(Collectors.toList());
            affectedSkus.add(candidate.getSku());
            // 同一目标的失败尝试均回到同一资源版本，只捕获一次完整快照和一次外层进度。
            ScheduleSubstitutionAttemptSnapshot snapshot = ScheduleSubstitutionAttemptSnapshot.capture(context, affectedSkus);
            snapshotCount++;
            Runnable restoreState = state.captureEndingDeferredAttemptRollback();
            for (ActiveMachineBinding victim : this.rankVictims(context, day, state, candidate,
                    available, snapshot, restoreState)) {
                attemptCount++;
                if (this.trySubstitute(context, day, state, candidate, victim, consumedMachines,
                        snapshot, restoreState)) {
                    consumedMachines.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(victim.getMachineCode()));
                    successCount++;
                    break;
                }
            }
        }
        log.info("结构切换日末置换收口, batchNo={}, businessDate={}, pending={}, bindings={}, snapshots={}, "
                        + "attempts={}, successes={}, elapsedMs={}", context.getBatchNo(), day.getScheduleDate(),
                pending.size(), bindings.size(), snapshotCount, attemptCount, successCount,
                (System.nanoTime() - started) / 1_000_000L);
    }

    /**
     * 对当前资源版本的可撤销绑定排序，失败候选只预演一次。
     * 完整提案用后释放，仅缓存绑定、历史来源、软指标和前料结束时间。
     *
     * @param context 资源上下文
     * @param day 当前日
     * @param state 外层绑定
     * @param candidate 待接管候选
     * @param bindings 本目标可尝试的绑定
     * @param snapshot 当前目标共用资源快照
     * @param restoreState 外层状态恢复动作
     * @return 按历史来源、完整适配、收尾时间和机台编码排序的合法绑定
     */
    private List<ActiveMachineBinding> rankVictims(LhScheduleContext context, DayScheduleContext day,
            DayDrivenScheduleState state, DailyNewSpecCandidate candidate, List<ActiveMachineBinding> bindings,
            ScheduleSubstitutionAttemptSnapshot snapshot, Runnable restoreState) {
        List<RankedVictim> ranked = new ArrayList<>(bindings.size());
        DailySchedulePhase originalPhase = day.getCurrentPhase();
        for (ActiveMachineBinding binding : bindings) {
            try {
                if (!this.releaseVictim(context, day, state, candidate.getSku(), binding)) {
                    continue;
                }
                NewSpecMachineAssignmentPlan plan = this.previewTakeover(context, day, candidate, binding);
                if (Objects.nonNull(plan)) {
                    ranked.add(new RankedVictim(binding, plan.getMatchResult().getPriorityMetricSnapshot(),
                            plan.getAvailabilityPlan().getOccupationEndTime(),
                            plan.getAvailabilityPlan().getHistoricalResidualCapacityInfo()));
                } else {
                    log.info("结构切换置换接管预演拒绝, batchNo={}, businessDate={}, target={}, victim={}, machine={}, reason={}",
                            context.getBatchNo(), day.getScheduleDate(), candidate.getSku().getMaterialCode(),
                            binding.getSku().getMaterialCode(), binding.getMachineCode(), candidate.getLastFailure());
                }
            } catch (RuntimeException exception) {
                log.error("结构切换置换候选预演失败, batchNo={}, machine={}",
                        context.getBatchNo(), binding.getMachineCode(), exception);
            } finally {
                snapshot.restore(context);
                restoreState.run();
                day.setCurrentPhase(originalPhase);
            }
        }
        ranked.sort(Comparator.comparing((RankedVictim entry) -> entry.history,
                HistoricalResidualCapacityInfo::compareSourcePriority)
                .thenComparing(entry -> entry.metric, MachinePriorityMetricSnapshot::compareSoftMatch)
                .thenComparing(entry -> entry.endingTime, Comparator.nullsLast(Date::compareTo))
                .thenComparing(entry -> entry.binding.getMachineCode()));
        if (log.isInfoEnabled()) {
            log.info("结构切换置换历史资源排序, batchNo={}, businessDate={}, materialCode={}, candidates={}",
                    context.getBatchNo(), day.getScheduleDate(), candidate.getSku().getMaterialCode(),
                    ranked.stream().map(RankedVictim::describe).collect(Collectors.joining(";")));
        }
        return ranked.stream().map(entry -> entry.binding).collect(Collectors.toList());
    }

    /**
     * 当前目标的轻量排序依据，避免保留完整时间轴及多份资源快照。
     */
    private static final class RankedVictim {
        /**
         * 原结构切换绑定。
         */
        private final ActiveMachineBinding binding;
        /**
         * 撤销占用后冻结的完整适配指标。
         */
        private final MachinePriorityMetricSnapshot metric;
        /**
         * 前料实际结束时间。
         */
        private final Date endingTime;
        /**
         * 提案冻结的历史剩余资源来源，空表示当前资源。
         */
        private final HistoricalResidualCapacityInfo history;

        /**
         * 保存单候选的轻量排序信息。
         *
         * @param binding 原绑定
         * @param metric 完整适配指标
         * @param endingTime 前料结束时间
         * @param history 冻结历史来源
         */
        private RankedVictim(ActiveMachineBinding binding, MachinePriorityMetricSnapshot metric, Date endingTime,
                HistoricalResidualCapacityInfo history) {
            this.binding = binding;
            this.metric = metric;
            this.endingTime = endingTime;
            this.history = history;
        }

        /**
         * 输出本次冻结的来源业务日、班次及完整匹配指标，便于核对跨日资源顺序。
         *
         * @return 单台机台的排序依据
         */
        private String describe() {
            LhShiftConfigVO shift = Objects.isNull(history) ? null : history.getSourceShift();
            return binding.getMachineCode() + "[sourceWorkDate=" + (Objects.isNull(shift) ? null : shift.getWorkDate())
                    + ",sourceShift=" + (Objects.isNull(shift) ? null : shift.getShiftIndex())
                    + ",sourceStart=" + (Objects.isNull(history) ? null : history.getSourceStartTime())
                    + "," + metric.describeSoftMatch() + "]";
        }
    }

    /**
     * 检查当日新占用的结构切换绑定以及整台机历史、恢复和后继计划边界。
     *
     * @param context 当前结果
     * @param day 当前日
     * @param state 绑定状态
     * @param binding 待撤销绑定
     * @return 可完整撤销时返回 true
     */
    private boolean isEligibleVictim(LhScheduleContext context, DayScheduleContext day,
            DayDrivenScheduleState state, ActiveMachineBinding binding) {
        LhScheduleResult result = binding.getScheduleResult();
        if (!binding.isStructureSwitchBinding() || !context.getScheduleResultList().contains(result)
                || DailyUnscheduledSkuSubstitutionPolicy.isHistoricalMachineProtected(context, binding.getMachineCode())
                || !"结构切换结果机台".equals(DailyUnscheduledSkuSubstitutionPolicy
                        .resolveProtectionReason(context, state, binding))
                || !day.contains(result.getMouldChangeStartTime())
                || !DailyUnscheduledSkuSubstitutionPolicy.hasPositiveDayQty(day, result)) {
            return false;
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode());
        // 已存在后继占用时，撤销前料会改变后继准备依据，交由原结果保留。
        return context.getScheduleResultList().stream()
                .filter(other -> other != result && other != binding.getPairScheduleResult())
                .filter(other -> physical.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        other.getLhMachineCode())))
                .noneMatch(other -> Objects.nonNull(other.getSpecEndTime())
                        && other.getSpecEndTime().after(result.getMouldChangeStartTime())
                        && ShiftFieldUtil.resolveScheduledQty(other) > 0);
    }

    /**
     * 撤销结果贡献和前置绑定后重建结构、模具及胶囊运行态。
     *
     * @param context 资源上下文
     * @param day 当前日
     * @param state 绑定状态
     * @param target 接管物料
     * @param victim 被置换绑定
     * @return 撤销成功时返回 true
     */
    private boolean releaseVictim(LhScheduleContext context, DayScheduleContext day,
            DayDrivenScheduleState state, SkuScheduleDTO target, ActiveMachineBinding victim) {
        Date ready = resultSubstitution.removeVictimBindingResult(context, day, victim);
        if (Objects.isNull(ready)) {
            return false;
        }
        state.removeBinding(victim);
        context.getPreScheduledMachineBindingList().removeIf(binding -> binding == victim
                || binding.getScheduleResult() == victim.getScheduleResult());
        for (LhScheduleResult result : Arrays.asList(victim.getScheduleResult(), victim.getPairScheduleResult())) {
            context.getStructureSwitchResultPlanMap().remove(result);
            context.getFirstInspectionResultPlanMap().remove(result);
            context.getCommittedWholeSingleControlPairMap().entrySet()
                    .removeIf(entry -> entry.getKey() == result || entry.getValue() == result);
        }
        this.restorePredecessorMouldRelease(context, victim, ready);
        resultSubstitution.restoreMachineStateBeforeVictim(context, victim, target, ready);
        context.setMouldResourceContext(MouldResourceContext.from(context));
        context.rebuildScheduledMachineCountMaps(context.getScheduleWindowShifts());
        context.getStructureSwitchRuntimeMap().remove(victim.getSku().getStructureName());
        StructureSwitchSchedulingPolicy.rebuildCommittedState(context);
        structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
        capsuleReplacementRuleService.rebuildRuntimeState(context, null);
        return true;
    }

    /**
     * 撤销原切换对前料模具发布的释放时点，由新接管提交重新登记真实释放时间。
     *
     * @param context 当前结果及模具释放表
     * @param victim 已撤销的切换绑定
     * @param originalRelease 原切换开始时间
     */
    private void restorePredecessorMouldRelease(LhScheduleContext context, ActiveMachineBinding victim,
            Date originalRelease) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(victim.getMachineCode());
        context.getScheduleResultList().stream()
                .filter(result -> physical.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        result.getLhMachineCode())))
                .filter(result -> Objects.nonNull(result.getSpecEndTime())
                        && !result.getSpecEndTime().after(originalRelease))
                .flatMap(result -> LhMouldCodeUtil.splitMouldCode(result.getMouldCode()).stream())
                .forEach(code -> context.getPreScheduledMouldReleaseTimeMap().remove(code, originalRelease));
    }

    /**
     * 用普通选料准入预演接管组合，显式保留后处理跨日绑定。
     *
     * @param context 资源上下文
     * @param day 当前日
     * @param candidate 正常候选
     * @param victim 已撤销绑定
     * @return 当前日合法完整提案，失败为空
     */
    private NewSpecMachineAssignmentPlan previewTakeover(LhScheduleContext context, DayScheduleContext day,
            DailyNewSpecCandidate candidate, ActiveMachineBinding victim) {
        NewSpecMachineAssignmentPlan plan = productionStrategy.previewMachineAssignment(context, day, candidate,
                context.getMachineScheduleMap().get(victim.getMachineCode()), strategyFactory.getMachineMatchStrategy(),
                strategyFactory.getMouldChangeBalanceStrategy(), strategyFactory.getFirstInspectionBalanceStrategy(),
                strategyFactory.getCapacityCalculateStrategy(), day.getDayStartTime(), false,
                new NewSpecProposalRoundCache(1));
        return Objects.isNull(plan) ? null : plan.forPostProcessing();
    }

    /**
     * 复核释放资源后是否存在更优的直接可用机台，保证接管原机仍符合最优匹配条件。
     * 先过滤当日不可用及来源确定较晚的机台，再复用冻结提案比较历史来源和软指标。
     *
     * @param context 撤销原占用后的状态
     * @param day 当前业务日
     * @param candidate 接管候选
     * @param selected 已选中的接管提案
     * @return 存在更优且当天合法开产的空闲机台时返回 true
     */
    private boolean hasBetterAvailableMachine(LhScheduleContext context, DayScheduleContext day,
            DailyNewSpecCandidate candidate, NewSpecMachineAssignmentPlan selected) {
        String selectedCode = selected.getMatchResult().getMachine().getMachineCode();
        HistoricalResidualCapacityInfo selectedHistory = selected.getAvailabilityPlan().getHistoricalResidualCapacityInfo();
        for (MachineScheduleDTO machine : new ArrayList<>(context.getMachineScheduleMap().values())) {
            String code = machine.getMachineCode();
            // 当日结束后才释放的机台无法承接当日正量，先裁掉这类资源再执行硬匹配。
            if (Objects.nonNull(machine.getEstimatedEndTime())
                    && !machine.getEstimatedEndTime().before(day.getDayEndTime())) {
                continue;
            }
            if (LhSingleControlMachineUtil.resolvePhysicalMachineCode(code).equals(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(selectedCode))
                    || DailyUnscheduledSkuSubstitutionPolicy.isHistoricalMachineProtected(context, code)) {
                continue;
            }
            // 更晚才释放的资源无法拥有更早来源，先裁掉确定落后于已选历史班次的机台。
            if (Objects.nonNull(selectedHistory) && Objects.nonNull(selectedHistory.getSourceShift())
                    && Objects.nonNull(selectedHistory.getSourceShift().getShiftEndDateTime())
                    && Objects.nonNull(machine.getEstimatedEndTime())
                    && !machine.getEstimatedEndTime().before(selectedHistory.getSourceShift().getShiftEndDateTime())) {
                continue;
            }
            NewSpecMachineAssignmentPlan alternative = productionStrategy.previewMachineAssignment(context, day,
                    candidate, machine, strategyFactory.getMachineMatchStrategy(),
                    strategyFactory.getMouldChangeBalanceStrategy(), strategyFactory.getFirstInspectionBalanceStrategy(),
                    strategyFactory.getCapacityCalculateStrategy(), day.getDayStartTime(), false,
                    new NewSpecProposalRoundCache(1));
            if (Objects.isNull(alternative)) {
                continue;
            }
            int historyComparison = HistoricalResidualCapacityInfo.compareSourcePriority(
                    alternative.getAvailabilityPlan().getHistoricalResidualCapacityInfo(), selectedHistory);
            if (historyComparison < 0) {
                return true;
            }
            if (historyComparison > 0) {
                continue;
            }
            int comparison = MachinePriorityMetricSnapshot.compareSoftMatch(
                    alternative.getMatchResult().getPriorityMetricSnapshot(), selected.getMatchResult().getPriorityMetricSnapshot());
            if (comparison > 0) {
                continue;
            }
            int timeComparison = Comparator.nullsLast(Date::compareTo).compare(
                    alternative.getAvailabilityPlan().getOccupationEndTime(),
                    selected.getAvailabilityPlan().getOccupationEndTime());
            if (comparison < 0 || timeComparison < 0 || (timeComparison == 0 && code.compareTo(selectedCode) < 0)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 给置换成功生成的主侧和配对侧首个正量班追加原因，复用公共去重并保留首检等已有说明。
     *
     * @param context 完整排程班次窗口
     * @param binding 本次成功的接管或迁移绑定
     * @param analysis 固定中文的置换说明
     */
    private void appendFirstShiftAnalysis(LhScheduleContext context, ActiveMachineBinding binding, String analysis) {
        for (LhScheduleResult result : Arrays.asList(binding.getScheduleResult(), binding.getPairScheduleResult())) {
            if (Objects.isNull(result)) {
                continue;
            }
            context.getScheduleWindowShifts().stream().filter(Objects::nonNull)
                    .filter(shift -> Objects.nonNull(shift.getShiftStartDateTime()))
                    .filter(shift -> {
                        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                        return Objects.nonNull(quantity) && quantity > 0;
                    })
                    .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                    .ifPresent(shift -> ShiftFieldUtil.appendShiftAnalysis(result, shift.getShiftIndex(), analysis));
        }
    }

    /**
     * 在接管成功后的资源检查点上尝试迁移，失败时只恢复本次迁移的变化。
     *
     * @param context 已提交接管结果的上下文
     * @param day 当前业务日
     * @param sku 被置换的原物料
     * @param takeoverSku 已成功接管的物料
     * @param excludedPhysicalMachines 本次禁止迁入的物理机台
     * @return 迁移成功的绑定，无合法迁移时为空
     */
    private ActiveMachineBinding tryRelocateAfterTakeover(LhScheduleContext context, DayScheduleContext day,
            SkuScheduleDTO sku, SkuScheduleDTO takeoverSku, Set<String> excludedPhysicalMachines) {
        ScheduleSubstitutionAttemptSnapshot takeoverSnapshot = ScheduleSubstitutionAttemptSnapshot.capture(
                context, Arrays.asList(sku, takeoverSku));
        boolean migrated = false;
        String failureReason = UnscheduledReasonEnum.NO_AVAILABLE_MACHINE.getDescription();
        try {
            ActiveMachineBinding binding = structureScheduling.relocateCurrentDay(context, day, sku,
                    excludedPhysicalMachines);
            if (Objects.isNull(binding)) {
                failureReason = context.getStructureSwitchFailureReasonMap().getOrDefault(
                        StructureSwitchCandidateScope.skuKey(sku), failureReason);
                return null;
            }
            if (!DailyUnscheduledSkuSubstitutionPolicy.hasPositiveDayQty(day, binding.getScheduleResult())
                    || DailyUnscheduledSkuSubstitutionPolicy.isHistoricalMachineProtected(context, binding.getMachineCode())) {
                return null;
            }
            String validationFailure = StructureSwitchSchedulingPolicy.validateCommittedResults(context);
            if (StringUtils.isNotEmpty(validationFailure)) {
                failureReason = validationFailure;
                return null;
            }
            migrated = true;
            return binding;
        } catch (RuntimeException exception) {
            log.error("结构切换置换迁移异常，保留接管, batchNo={}, materialCode={}",
                    context.getBatchNo(), sku.getMaterialCode(), exception);
            return null;
        } finally {
            if (!migrated) {
                takeoverSnapshot.restore(context);
                String detail = "结构切换置换后原物料待排，" + failureReason;
                context.getStructureSwitchFailureReasonMap().put(StructureSwitchCandidateScope.skuKey(sku), failureReason);
                log.info("结构切换置换迁移失败保留接管, batchNo={}, businessDate={}, materialCode={}, reason={}",
                        context.getBatchNo(), day.getScheduleDate(), sku.getMaterialCode(), detail);
            }
        }
    }

    /**
     * 接管形成合法正量后独立尝试原物料迁移，迁移失败保留接管并恢复原物料待排。
     *
     * @param context 共享上下文
     * @param day 当前日
     * @param state 外层状态
     * @param candidate 待接管候选
     * @param victim 原结构切换绑定
     * @param consumedMachines 本轮已提交机台，成功后登记新机台
     * @param snapshot 当前目标共用的资源快照
     * @param restoreState 当前目标共用的外层恢复动作
     * @return 接管结果合法保留时返回 true
     */
    private boolean trySubstitute(LhScheduleContext context, DayScheduleContext day, DayDrivenScheduleState state,
            DailyNewSpecCandidate candidate, ActiveMachineBinding victim, Set<String> consumedMachines,
            ScheduleSubstitutionAttemptSnapshot snapshot, Runnable restoreState) {
        if (!this.isEligibleVictim(context, day, state, victim)) {
            return false;
        }
        DailySchedulePhase originalPhase = day.getCurrentPhase();
        boolean committed = false;
        try {
            if (!this.releaseVictim(context, day, state, candidate.getSku(), victim)) {
                return false;
            }
            NewSpecMachineAssignmentPlan plan = this.previewTakeover(context, day, candidate, victim);
            if (Objects.isNull(plan)) {
                log.info("结构切换置换接管重检失败, batchNo={}, machine={}, reason={}",
                        context.getBatchNo(), victim.getMachineCode(), candidate.getLastFailure());
                return false;
            }
            if (this.hasBetterAvailableMachine(context, day, candidate, plan)) {
                log.info("结构切换置换存在更优空闲机台, batchNo={}, target={}, machine={}",
                        context.getBatchNo(), candidate.getSku().getMaterialCode(), victim.getMachineCode());
                return false;
            }
            if (!productionStrategy.executeIsolatedMachineAssignment(context, day, plan,
                    strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                    strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory.getCapacityCalculateStrategy())) {
                return false;
            }
            ActiveMachineBinding takeover = context.getPreScheduledMachineBindingList().stream()
                    .filter(binding -> binding.getSku() == candidate.getSku())
                    .filter(binding -> binding.getMachineCode().equals(victim.getMachineCode())
                            && DailyUnscheduledSkuSubstitutionPolicy.hasPositiveDayQty(day, binding.getScheduleResult()))
                    .findFirst().orElse(null);
            if (Objects.isNull(takeover)) {
                return false;
            }
            Set<String> excluded = new LinkedHashSet<>(consumedMachines);
            excluded.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(victim.getMachineCode()));
            ActiveMachineBinding migrated = this.tryRelocateAfterTakeover(context, day, victim.getSku(), candidate.getSku(), excluded);
            if (DailyUnscheduledSkuSubstitutionPolicy.isHistoricalMachineProtected(context, victim.getMachineCode())
                    || StringUtils.isNotEmpty(StructureSwitchSchedulingPolicy.validateCommittedResults(context))) {
                return false;
            }
            context.getPreScheduledMachineBindingList().stream()
                    .filter(binding -> context.getScheduleResultList().contains(binding.getScheduleResult()))
                    .forEach(state::registerBinding);
            state.markScheduledAndCarryOver(candidate.getSku());
            state.markMachinePriorityTraceHit(candidate.getSku());
            state.reopenPendingSku(victim.getSku());
            if (state.hasActiveBinding(victim.getSku())) {
                state.markScheduledAndCarryOver(victim.getSku());
            }
            this.appendFirstShiftAnalysis(context, takeover, STRUCTURE_SWITCH_SUBSTITUTION_ANALYSIS);
            if (Objects.nonNull(migrated)) {
                this.appendFirstShiftAnalysis(context, migrated, STRUCTURE_SWITCH_SUBSTITUTION_ANALYSIS);
            }
            HistoricalResidualCapacityInfo history = plan.getAvailabilityPlan().getHistoricalResidualCapacityInfo();
            log.info("结构切换置换来源确认, batchNo={}, materialCode={}, machine={}, poolDate={}, sourceStart={}",
                    context.getBatchNo(), candidate.getSku().getMaterialCode(), victim.getMachineCode(),
                    candidate.getPoolDate(), Objects.isNull(history) ? null : history.getSourceStartTime());
            String detail = String.format("批次=%s，业务日=%s，接管物料=%s，原机=%s，原物料=%s，新机=%s，迁移成功=%s，迁移失败保留接管=true",
                    context.getBatchNo(), day.getScheduleDate(), candidate.getSku().getMaterialCode(),
                    victim.getMachineCode(), victim.getSku().getMaterialCode(),
                    Objects.isNull(migrated) ? "待排" : migrated.getMachineCode(), Objects.nonNull(migrated));
            PriorityTraceLogHelper.appendProcessLog(context, "结构切换日末置换", detail);
            log.info("结构切换日末置换成功, {}", detail);
            if (Objects.nonNull(migrated)) {
                consumedMachines.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(migrated.getMachineCode()));
            }
            committed = true;
            return true;
        } catch (RuntimeException exception) {
            log.error("结构切换日末置换异常, batchNo={}, machine={}",
                    context.getBatchNo(), victim.getMachineCode(), exception);
            return false;
        } finally {
            day.setCurrentPhase(originalPhase);
            if (!committed) {
                snapshot.restore(context);
                restoreState.run();
                log.info("结构切换日末置换恢复, batchNo={}, target={}, victim={}, machine={}", context.getBatchNo(),
                        candidate.getSku().getMaterialCode(), victim.getSku().getMaterialCode(), victim.getMachineCode());
            }
        }
    }
}
