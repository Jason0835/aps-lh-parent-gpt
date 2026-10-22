package com.zlt.aps.lh.service.impl;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.LhMachineSupplyStructureRule;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.ITypeBlockProductionStrategy;
import com.zlt.aps.lh.engine.strategy.impl.NewSpecProductionStrategy;
import com.zlt.aps.lh.engine.strategy.support.DailyCandidateReason;
import com.zlt.aps.lh.engine.strategy.support.DailyNewSpecCandidate;
import com.zlt.aps.lh.engine.strategy.support.DailySchedulePhase;
import com.zlt.aps.lh.engine.strategy.support.DayScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.NewSpecCandidatePoolBuilder;
import com.zlt.aps.lh.engine.strategy.support.NewSpecMachineAssignmentPlan;
import com.zlt.aps.lh.engine.strategy.support.NewSpecProposalRoundCache;
import com.zlt.aps.lh.engine.strategy.support.MouldResourceContext;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternatePlanReleaseEvent;
import com.zlt.aps.lh.engine.strategy.support.PreviousAlternateStructureLimitExemptionPolicy;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchCandidateScope;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchPriorityPolicy;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchShiftAudit;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 结构切换独立编排：真实班次、供胚有序结构、四级组合、SKU业务顺序及合法机台。
 * 每轮只提交一个物理组；预演不创建全量匹配矩阵，失败缓存限定当前资源版本。
 * 数量、时间轴、历史资格和原子提交复用原服务，不建立第二套资源或数量账本。
 * @author APS
 * @since 2026/10/09
 */
@Slf4j
@Service
public class StructureSwitchSchedulingService {
    /**
     * 共用的时间轴、分量和原机续排执行内核。
     */
    @Resource
    private NewSpecProductionStrategy productionStrategy;
    /**
     * 本批排程使用的公共策略工厂。
     */
    @Resource
    private ScheduleStrategyFactory strategyFactory;
    /**
     * 正常需求与提前生产的统一物料资格入口。
     */
    @Resource
    private NewSpecMaterialEligibilityService materialEligibilityService;
    /**
     * 统一目标机台数和剩余机台机会计算器。
     */
    @Resource
    private NewSpecCandidatePoolBuilder candidatePoolBuilder;
    /**
     * 换活字块关系及硬约束的公共判断入口。
     */
    @Resource
    private ITypeBlockProductionStrategy typeBlockProductionStrategy;
    /**
     * 按本批年月版本解析结构专供关系。
     */
    @Resource
    private LhMachineSupplyStructureRule supplyStructureRule;
    /**
     * 真实未完成数量和共享消费账本读取入口。
     */
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;
    /**
     * 结构班次在机索引与收尾对齐入口。
     */
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    /**
     * 正式收尾变化后的设备计划复评入口。
     */
    @Resource
    private LhEquipmentPlanScheduleService equipmentPlanScheduleService;
    /**
     * 有效历史指定组合的物料及机台需求资格入口。
     */
    @Resource
    private PreviousAlternatePlanEligibilityService historicalEligibilityService;
    /**
     * 成功承接结果的历史来源绑定入口。
     */
    @Resource
    private PreviousAlternatePlanReuseService historicalReuseService;

    /**
     * 判断是否仍有待排的结构切换需求。
     *
     * @param context 已冻结的身份及真实剩余量
     * @return 是否仍有需要进入独立步骤的需求
     */
    public boolean hasRemainingCandidates(LhScheduleContext context) {
        return context.getStructureSwitchCandidateSkuMap().values().stream()
                .anyMatch(sku -> targetScheduleQtyResolver.previewUnfulfilledProductionRemainingQty(context, sku) > 0);
    }

    /**
     * 补回前置历史入口清理的独立候选身份，数量仍读取原共享账本。
     *
     * @param context 续作和换活字块后的上下文
     */
    public void prepareCandidates(LhScheduleContext context) {
        Set<String> pendingKeys = context.getNewSpecSkuList().stream().filter(Objects::nonNull)
                .map(StructureSwitchCandidateScope::skuKey).collect(Collectors.toSet());
        context.getStructureSwitchCandidateSkuMap().forEach((key, sku) -> {
            if (pendingKeys.add(key)) {
                context.getNewSpecSkuList().add(sku);
            }
        });
        context.rebuildStructureSkuMapFromPending(context.getNewSpecSkuList());
    }

    /**
     * 按真实班次执行结构切换，退出时恢复日期和执行作用域，保留已消费视图供后处理读取。
     *
     * @param context 本批次唯一资源及需求账本
     */
    public void schedule(LhScheduleContext context) {
        Date originalDate = context.getCurrentScheduleDate();
        boolean originalScope = context.isStructureSwitchScheduling();
        context.setStructureSwitchScheduling(true);
        List<LhShiftConfigVO> shifts = StructureSwitchSchedulingPolicy.orderedShifts(context);
        Map<LocalDate, List<LhShiftConfigVO>> days = LhScheduleTimeUtil.groupByWorkDate(shifts);
        Map<String, List<LhMouldChangePlan>> history = context.getEligiblePreviousAlternatePlans().stream()
                .collect(Collectors.groupingBy(plan -> MonthPlanDateResolver.buildMaterialStatusKey(
                        plan.getAfterMaterialCode(), plan.getProductStatus()), LinkedHashMap::new, Collectors
                            .toList()));
        // 只冻结编码，回滚会替换机台DTO；每轮必须按编码重新读取实时对象。
        List<String> machineCodes = new ArrayList<>(context.getMachineScheduleMap().keySet());
        int dayIndex = 0;
        try {
            this.appendCandidateSnapshot(context);
            if (Objects.isNull(context.getMouldResourceContext())) {
                context.setMouldResourceContext(MouldResourceContext.from(context));
            }
            context.rebuildScheduledMachineCountMaps(shifts);
            structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);
            for (Map.Entry<LocalDate, List<LhShiftConfigVO>> entry : days.entrySet()) {
                dayIndex++;
                DayScheduleContext day = new DayScheduleContext(entry.getKey(), entry.getValue(),
                        dayIndex == 1, dayIndex == days.size());
                context.setCurrentScheduleDate(LhScheduleTimeUtil.clearTime(entry.getValue().get(0).getWorkDate()));
                if (Objects.nonNull(context.getMouldResourceContext())) {
                    context.getMouldResourceContext().refreshAvailability(context);
                }
                List<SkuScheduleDTO> candidates = new ArrayList<>(context.getStructureSwitchCandidateSkuMap().values());
                candidates.forEach(sku -> materialEligibilityService.prepareNormalPlanOnDueDate(context, sku, entry
                    .getKey()));
                // 业务排序一天一次；首班和第二班均在组合等级内沿用，不再按同胎胚覆盖。
                strategyFactory.getSkuPriorityStrategy().sortNewSpecPoolByPriority(context, candidates);
                Map<String, List<SkuScheduleDTO>> groups = this.groupBySupplyTime(context, candidates);
                for (LhShiftConfigVO shift : entry.getValue()) {
                    // 原因只代表当前班次；后续班次必须重新观察，不能保留先前已变化的名额拒绝。
                    context.getStructureSwitchFailureReasonMap().clear();
                    productionStrategy.continueStructureSwitchShift(context, day, shift);
                    this.scheduleShift(context, day, shift, groups, machineCodes, history);
                }
                // 整个业务日完成后才登记日期上界，避免只完成首班就跳过同日后续班次。
                context.getPreScheduledMachineBindingList().stream()
                        .filter(binding -> binding.isStructureSwitchBinding())
                        .forEach(binding -> binding.setPreparedThroughDate(day.getScheduleDate()));
                StructureSwitchShiftAudit.appendDaySummary(context, day);
            }
            context.getStructureSwitchCandidateSkuMap().forEach((key, sku) -> {
                String reason = context.getStructureSwitchFailureReasonMap().get(key);
                if (context.hasPreviousAlternateTrialPlan(sku) && StringUtils.isNotEmpty(reason)) {
                    context.getPreviousAlternateTrialFailureMap().put(key, reason);
                }
            });
        } finally {
            context.setCurrentScheduleDate(originalDate);
            context.setStructureSwitchScheduling(originalScope);
            // 日级资格是阶段快照；普通提前生产必须在其原时点读取最新结构占用。
            Set<String> structures = context.getStructureSwitchCandidateSourceMap().values().stream()
                    .map(CxEmbryoLhTime::getNextStructureName).collect(Collectors.toSet());
            context.getStructureEarlyProductionAdmissionMap().values()
                    .forEach(admissions -> admissions.keySet().removeAll(structures));
            context.rebuildStructureSkuMapFromPending(context.getNewSpecSkuList());
        }
    }

    /**
     * 按最早供胚时间组织结构候选，保留同结构SKU原业务顺序。
     *
     * @param context 来源快照
     * @param candidates 已按原业务顺序排序的SKU
     * @return 按供胚时间有序的结构分组
     */
    private Map<String, List<SkuScheduleDTO>> groupBySupplyTime(LhScheduleContext context,
        List<SkuScheduleDTO> candidates) {
        candidates.sort(Comparator.comparing(sku -> context.getStructureSwitchCandidateSourceMap()
                .get(StructureSwitchCandidateScope.skuKey(sku)).getEarliestLhTime()));
        return candidates.stream().collect(Collectors.groupingBy(SkuScheduleDTO::getStructureName,
                LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 班内每轮只提交一个组合；全部失败后才进入下一班，当前资源未变化的失败组合不重复预演。
     *
     * @param context 实时上下文
     * @param day 当前业务日
     * @param shift 当前竞争班次
     * @param groups 稳定有序的结构分组
     * @param machineCodes 本批机台编码索引
     * @param history SKU对应的有效历史关系
     */
    private void scheduleShift(LhScheduleContext context, DayScheduleContext day, LhShiftConfigVO shift,
            Map<String, List<SkuScheduleDTO>> groups, List<String> machineCodes,
            Map<String, List<LhMouldChangePlan>> history) {
        long started = System.nanoTime();
        NewSpecProposalRoundCache cache = new NewSpecProposalRoundCache(context.getStructureSwitchCandidateSkuMap()
            .size());
        Set<String> failed = new LinkedHashSet<>();
        int committed = 0;
        while (!context.isInterrupted()) {
            equipmentPlanScheduleService.reviewConfirmedEndings(context);
            if (cache.invalidateEquipmentPlanVersion(context.getEquipmentPlanRuntimeState().getConstraintVersion())) {
                failed.clear();
            }
            Selection selected = this.selectNext(context, day, shift, groups, machineCodes, history, failed, cache);
            if (Objects.isNull(selected)) {
                break;
            }
            SkuScheduleDTO sku = selected.assignment.getCandidate().getSku();
            day.setCurrentPhase(selected.assignment.getCandidate().hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                    ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
            Set<LhScheduleResult> before = Collections.newSetFromMap(new IdentityHashMap<>());
            before.addAll(context.getScheduleResultList());
            boolean success = this.withHistoricalScope(context, sku, selected.machineCode, selected.history,
                    () -> productionStrategy.executeMachineAssignment(context, day, selected.assignment,
                            strategyFactory.getMachineMatchStrategy(), strategyFactory.getMouldChangeBalanceStrategy(),
                            strategyFactory.getFirstInspectionBalanceStrategy(), strategyFactory
                                .getCapacityCalculateStrategy()));
            StructureSwitchShiftAudit.recordSubmission(context, day, selected.assignment,
                    success ? "结构切换独立步骤已提交正量" : "结构切换独立步骤提交回滚");
            if (!success) {
                failed.add(selected.failureKey);
                this.recordFailure(context, sku, selected.assignment.getCandidate().getLastFailure());
                this.traceHistoricalFailure(context, shift, sku, selected, "正式提交",
                        selected.assignment.getCandidate().getLastFailure());
                continue;
            }
            if (Objects.nonNull(selected.history)) {
                historicalReuseService.bindPreviousAlternateResults(context, selected.history, before);
            }
            committed++;
            context.getStructureSwitchFailureReasonMap().remove(StructureSwitchCandidateScope.skuKey(sku));
            cache.clearAfterStateChanged();
            failed.clear();
            String detail = String.format("批次=%s，班次=%s，结构=%s，物料=%s，状态=%s，机台=%s，优先级=%s，供胚时间=%s",
                    context.getBatchNo(), shift.getShiftIndex(), sku.getStructureName(), sku.getMaterialCode(),
                    sku.getProductStatus(), selected.machineCode, selected.rank + 1,
                    context.getStructureSwitchCandidateSourceMap().get(StructureSwitchCandidateScope.skuKey(sku))
                        .getEarliestLhTime());
            PriorityTraceLogHelper.appendProcessLog(context, "结构切换逐轮提交", detail);
        }
        log.info("结构切换班次收敛, batchNo={}, shift={}, committed={}, evaluatedPairs={}, failedPairs={}, elapsedMs={}",
                context.getBatchNo(), shift.getShiftIndex(), committed, cache.getEvaluatedPairCount(), failed.size(),
                (System.nanoTime() - started) / 1_000_000L);
    }

    /**
     * 按结构供胚时间和同结构四级组合顺序选择下一条合法提案。
     *
     * @param context 上下文
     * @param day 业务日
     * @param shift 竞争班次
     * @param groups 有序结构
     * @param machineCodes 机台索引
     * @param history 历史索引
     * @param failed 本版本失败键
     * @param cache 本版本预演缓存
     * @return 第一可行结构中最高组合等级、最先SKU的合法机台
     */
    private Selection selectNext(LhScheduleContext context, DayScheduleContext day, LhShiftConfigVO shift,
            Map<String, List<SkuScheduleDTO>> groups, List<String> machineCodes,
            Map<String, List<LhMouldChangePlan>> history, Set<String> failed, NewSpecProposalRoundCache cache) {
        for (List<SkuScheduleDTO> original : groups.values()) {
            CxEmbryoLhTime source = context.getStructureSwitchCandidateSourceMap()
                    .get(StructureSwitchCandidateScope.skuKey(original.get(0)));
            if (!source.getEarliestLhTime().before(shift.getShiftEndDateTime())) {
                // 尚未供胚的整个结构跳过机台扫描，不为注定落后续班次的组合重复计算时间轴。
                original.forEach(sku -> this.recordFailure(context, sku,
                        day.isLastScheduleDay() ? "胎胚最早可供硫化时间超出当前剩余排程窗口"
                                : "胎胚最早可供硫化时间尚未进入当前班次"));
                continue;
            }
            // 先完成同结构全部SKU的高等级组合，再进入低等级，避免前序普通SKU先占名额。
            // S1与首班共用本顺序；同胎胚关系不得越过历史指定、活字块或原SKU延期排序。
            Map<String, DailyNewSpecCandidate> candidates = new LinkedHashMap<>();
            for (int combinationRank = StructureSwitchPriorityPolicy.HISTORICAL;
                    combinationRank <= StructureSwitchPriorityPolicy.ORDINARY; combinationRank++) {
                for (SkuScheduleDTO sku : original) {
                    if (targetScheduleQtyResolver.previewUnfulfilledProductionRemainingQty(context, sku) <= 0) {
                        continue;
                    }
                    String skuKey = StructureSwitchCandidateScope.skuKey(sku);
                    // 同一选择轮资源不变，只解析一次普通物料资格；成功提交后下一轮重新读取。
                    DailyNewSpecCandidate ordinary = candidates.computeIfAbsent(skuKey, ignored -> {
                        DailyNewSpecCandidate candidate = materialEligibilityService.resolveCandidate(
                                context, sku, day.getScheduleDate());
                        candidatePoolBuilder.refreshRemainingMachineCount(context, day.getScheduleDate(), candidate);
                        return candidate;
                    });
                    Selection selection = this.selectMachine(context, day, shift, sku, machineCodes,
                            history.getOrDefault(skuKey, Collections.emptyList()), ordinary, combinationRank, failed, cache);
                    if (Objects.nonNull(selection)) {
                        return selection;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 为当前SKU在指定组合等级中选择满足本班硬约束的最早机台。
     *
     * @param context 上下文
     * @param day 业务日
     * @param shift 竞争班次
     * @param sku 当前SKU
     * @param machineCodes 机台索引
     * @param history 当前SKU历史关系
     * @param ordinary 本选择轮已解析的普通物料资格及机台需求
     * @param combinationRank 当前结构正在竞争的组合等级
     * @param failed 失败键
     * @param cache 资源版本缓存
     * @return 当前等级下实际占用最早的合法组合
     */
    private Selection selectMachine(LhScheduleContext context, DayScheduleContext day, LhShiftConfigVO shift,
            SkuScheduleDTO sku, List<String> machineCodes, List<LhMouldChangePlan> history,
            DailyNewSpecCandidate ordinary, int combinationRank, Set<String> failed, NewSpecProposalRoundCache cache) {
        boolean pendingHistory = history.stream()
                .anyMatch(plan -> !context.getPreviousAlternateResultPlanMap().containsValue(plan));
        if (!pendingHistory && (ordinary.getReasons().isEmpty()
                || ordinary.getTargetMachineCount() <= ordinary.getScheduledMachineCount())) {
            String reason = ordinary.getReasons().isEmpty()
                    ? ordinary.getLastFailure() : "本班无新增物理机台缺口";
            this.recordFailure(context, sku, reason);
            if (combinationRank == StructureSwitchPriorityPolicy.HISTORICAL) {
                String detail = String.format("批次=%s，班次=%s，结构=%s，物料=%s，状态=%s，目标台数=%s，已排台数=%s，原因=%s",
                        context.getBatchNo(), shift.getShiftIndex(), sku.getStructureName(), sku.getMaterialCode(),
                        sku.getProductStatus(), ordinary.getTargetMachineCount(), ordinary.getScheduledMachineCount(),
                        reason);
                PriorityTraceLogHelper.appendProcessLog(context, "结构切换候选需求跳过", detail);
            }
            return null;
        }
        Set<String> boundMachines = context.getPreScheduledMachineBindingList().stream()
                .filter(binding -> StringUtils.equals(binding.getSkuKey(), StructureSwitchCandidateScope.skuKey(sku)))
                .filter(binding -> {
                    MachineScheduleDTO current = context.getMachineScheduleMap().get(binding.getMachineCode());
                    return Objects.nonNull(current) && StringUtils.equals(current.getCurrentMaterialCode(), sku
                        .getMaterialCode());
                })
                .map(binding -> LhSingleControlMachineUtil.resolvePhysicalMachineCode(binding.getMachineCode()))
                .collect(Collectors.toSet());
        List<Selection> available = machineCodes.stream().map(context.getMachineScheduleMap()::get)
                .filter(machine -> this.hasRemainingCapacity(context, machine, shift))
                .filter(machine -> !boundMachines.contains(LhSingleControlMachineUtil
                    .resolvePhysicalMachineCode(machine.getMachineCode())))
                .flatMap(machine -> {
                    LhMouldChangePlan plan = this.findHistoricalPlan(context, sku, machine, history);
                    Selection ordinarySelection = new Selection(machine.getMachineCode(), null);
                    // 历史动作失败后仍可按普通关系竞争原机，沿用迁移前历史阶段到新增阶段的回落语义。
                    return Objects.isNull(plan) ? Stream.of(ordinarySelection)
                            : Stream.of(new Selection(machine.getMachineCode(), plan), ordinarySelection);
                })
                .filter(selection -> !ordinary.getReasons().isEmpty() || Objects.nonNull(selection.history))
                .peek(selection -> selection.rank = StructureSwitchPriorityPolicy.resolveRank(context, sku,
                        context.getMachineScheduleMap().get(selection.machineCode), selection.history,
                        typeBlockProductionStrategy, supplyStructureRule))
                .collect(Collectors.toList());
        if (available.isEmpty()) {
            // 某个高等级没有关系不代表全部机台不可用，只在最终普通层记录无候选。
            if (combinationRank == StructureSwitchPriorityPolicy.ORDINARY) {
                this.recordFailure(context, sku, "本班无剩余可用机台，已绑定原机不重复开机");
            }
            return null;
        }
        List<Selection> machines = available.stream()
                .filter(selection -> selection.rank == combinationRank)
                .sorted(Comparator.comparing(selection -> selection.machineCode))
                .collect(Collectors.toList());
        Selection best = null;
        Set<String> physicalGroups = new LinkedHashSet<>();
        for (Selection selection : machines) {
            MachineScheduleDTO machine = context.getMachineScheduleMap().get(selection.machineCode);
            String resourceCode = LhSingleControlMachineUtil.isWholeMachineGranularitySku(context, sku)
                    ? LhSingleControlMachineUtil.resolvePhysicalMachineCode(selection.machineCode)
                    : selection.machineCode;
            String actionKey = Objects.isNull(selection.history) ? "NORMAL" : "HISTORY:" + selection.history.getId();
            if (!physicalGroups.add(resourceCode + "|" + actionKey)) {
                continue;
            }
            String key = NewSpecMachineAssignmentPlan.buildAssignmentKey(resourceCode, sku, shift.getShiftIndex())
                    + "|" + actionKey;
            selection.failureKey = key;
            if (failed.contains(key)) {
                continue;
            }
            cache.recordEvaluatedPair();
            selection.assignment = this.withHistoricalScope(context, sku, selection.machineCode,
                    selection.history, () -> {
                        DailyNewSpecCandidate candidate = ordinary;
                        if (Objects.nonNull(selection.history)) {
                            candidate = materialEligibilityService.resolvePreviousAlternateCandidate(context, sku,
                                    day.getScheduleDate(), selection.history, selection.machineCode);
                            historicalEligibilityService.fillMachineDemand(context, day.getScheduleDate(), candidate);
                        }
                        if (candidate.getReasons().isEmpty()
                                || candidate.getTargetMachineCount() <= candidate.getScheduledMachineCount()) {
                            String reason = candidate.getReasons().isEmpty()
                                    ? candidate.getLastFailure() : "本班无新增物理机台缺口";
                            this.recordFailure(context, sku, reason);
                            this.traceHistoricalFailure(context, shift, sku, selection, "物料与需求资格", reason);
                            return null;
                        }
                        day.setCurrentPhase(candidate.hasReason(DailyCandidateReason.EARLY_PRODUCTION)
                                ? DailySchedulePhase.EARLY_PRODUCTION : DailySchedulePhase.NORMAL_RESOURCE_COMPETITION);
                        NewSpecMachineAssignmentPlan assignment = productionStrategy.previewMachineAssignment(context, day,
                                candidate, machine, strategyFactory.getMachineMatchStrategy(),
                                strategyFactory.getMouldChangeBalanceStrategy(),
                                strategyFactory.getFirstInspectionBalanceStrategy(),
                                strategyFactory.getCapacityCalculateStrategy(),
                                shift.getShiftStartDateTime(), false, cache);
                        if (Objects.isNull(assignment)) {
                            this.recordFailure(context, sku, candidate.getLastFailure());
                            this.traceHistoricalFailure(context, shift, sku, selection, "完整提案预演",
                                    candidate.getLastFailure());
                            return null;
                        }
                        Date occupation = assignment.getAvailabilityPlan().getProductionOccupationStartTime();
                        if (Objects.isNull(occupation) || occupation.before(shift.getShiftStartDateTime())
                                || !occupation.before(shift.getShiftEndDateTime())) {
                            this.recordFailure(context, sku, "完整时间轴需在后续合法班次重试");
                            this.traceHistoricalFailure(context, shift, sku, selection, "竞争班次",
                                    "完整时间轴需在后续合法班次重试");
                            return null;
                        }
                        return assignment.forStructureSwitch(shift);
                    });
            if (Objects.isNull(selection.assignment)) {
                failed.add(key);
            } else if (Objects.isNull(best) || selection.assignment.getAvailabilityPlan()
                .getProductionOccupationStartTime()
                    .before(best.assignment.getAvailabilityPlan().getProductionOccupationStartTime())) {
                if (Objects.nonNull(best)) {
                    best.assignment = null;
                }
                best = selection;
            } else {
                selection.assignment = null;
            }
        }
        return best;
    }

    /**
     * 判断机台是否在当前班次具有可使用的剩余时间。
     *
     * @param context 上下文
     * @param machine 实时机台
     * @param shift 本班
     * @return 窗口内已释放且启用的机台
     */
    private boolean hasRemainingCapacity(LhScheduleContext context, MachineScheduleDTO machine, LhShiftConfigVO shift) {
        return Objects.nonNull(machine) && MachineStatusUtil.isEnabled(machine.getStatus())
                && !context.isContinuousStopHoldMachine(machine.getMachineCode())
                && (Objects.isNull(machine.getEstimatedEndTime()) || machine.getEstimatedEndTime().before(shift
                    .getShiftEndDateTime()))
                && (context.getNewSpecMachineResourceScopeCodeSet().isEmpty()
                || context.getNewSpecMachineResourceScopeCodeSet().contains(machine.getMachineCode()));
    }

    /**
     * 查找当前机台和SKU精确匹配且尚未承接的有效历史计划。
     *
     * @param context 本批资格
     * @param sku 后物料
     * @param machine 当前机台
     * @param plans 历史索引
     * @return 尚未成功承接且精确匹配的历史关系
     */
    private LhMouldChangePlan findHistoricalPlan(LhScheduleContext context, SkuScheduleDTO sku,
            MachineScheduleDTO machine, List<LhMouldChangePlan> plans) {
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machine
            .getMachineCode());
        return plans.stream().filter(plan -> context.isEligiblePreviousAlternateCombination(sku, plan, machine
            .getMachineCode()))
                .filter(plan -> !context.getPreviousAlternateResultPlanMap().containsValue(plan))
                .filter(plan -> StringUtils.equals(machine.getCurrentMaterialCode(), plan.getBeforeMaterialCode())
                        || (Objects.nonNull(event) && event.getPlan() == plan
                        && (StringUtils.isEmpty(machine.getCurrentMaterialCode())
                        || (event.isBeforeMaterialNotScheduled()
                        && StringUtils.equals(machine.getCurrentMaterialCode(), plan.getAfterMaterialCode())))))
                .findFirst().orElse(null);
    }

    /**
     * 保存精确历史组合的淘汰依据，避免SKU汇总原因掩盖指定机台落选原因。
     *
     * @param context 上下文
     * @param shift 本次竞争班次
     * @param sku 指定后物料
     * @param selection 被拒绝的组合
     * @param stage 资格、预演或提交阶段
     * @param reason 原判断返回的原因
     */
    private void traceHistoricalFailure(LhScheduleContext context, LhShiftConfigVO shift, SkuScheduleDTO sku,
            Selection selection, String stage, String reason) {
        if (Objects.isNull(selection.history)) {
            return;
        }
        String detail = String.format("批次=%s，班次=%s，结构=%s，物料=%s，状态=%s，机台=%s，计划ID=%s，阶段=%s，原因=%s",
                context.getBatchNo(), shift.getShiftIndex(), sku.getStructureName(), sku.getMaterialCode(),
                sku.getProductStatus(), selection.machineCode, selection.history.getId(), stage, reason);
        PriorityTraceLogHelper.appendProcessLog(context, "结构切换指定组合未选中", detail);
    }

    /**
     * 在精确历史交替作用域内执行预演或提交，并恢复调用前状态。
     *
     * @param context 上下文
     * @param sku 当前SKU
     * @param machineCode 精确机台
     * @param plan 当前历史关系
     * @param action 预演或提交
     * @param <T> 动作结果类型
     * @return 动作结果
     */
    private <T> T withHistoricalScope(LhScheduleContext context, SkuScheduleDTO sku, String machineCode,
            LhMouldChangePlan plan, Supplier<T> action) {
        PreviousAlternatePlanReleaseEvent previous = context.getActivePreviousAlternateEvent();
        Date previousReady = context.getPreviousAlternateCandidateAvailableTimeMap().get(sku);
        PreviousAlternatePlanReleaseEvent event = context.getPreviousAlternateReleaseEventMap().get(machineCode);
        boolean matches = Objects.nonNull(plan) && Objects.nonNull(event) && event.getPlan() == plan;
        try (PreviousAlternateStructureLimitExemptionPolicy.Scope scope =
                     PreviousAlternateStructureLimitExemptionPolicy.openScope(context, sku, plan, machineCode)) {
            context.setActivePreviousAlternateEvent(matches ? event : null);
            if (matches) {
                context.getPreviousAlternateCandidateAvailableTimeMap().put(sku, event.getOfflineTime());
            }
            return action.get();
        } finally {
            context.setActivePreviousAlternateEvent(previous);
            if (Objects.isNull(previousReady)) {
                context.getPreviousAlternateCandidateAvailableTimeMap().remove(sku);
            } else {
                context.getPreviousAlternateCandidateAvailableTimeMap().put(sku, previousReady);
            }
        }
    }

    /**
     * 保存结构切换候选的最近有效失败原因。
     *
     * @param context 上下文
     * @param sku 当前SKU
     * @param reason 原失败原因，空原因不覆盖已有证据
     */
    private void recordFailure(LhScheduleContext context, SkuScheduleDTO sku, String reason) {
        if (StringUtils.isNotEmpty(reason)) {
            String key = StructureSwitchCandidateScope.skuKey(sku);
            String previous = context.getStructureSwitchFailureReasonMap().get(key);
            // 已有合法机台被共享名额拒绝时，后续不匹配机台不能掩盖真正限制；不改变候选选择。
            if (StringUtils.startsWith(previous, StructureSwitchSchedulingPolicy.SHIFT_LIMIT_REJECTION_PREFIX)
                    && !StringUtils.startsWith(reason, StructureSwitchSchedulingPolicy.SHIFT_LIMIT_REJECTION_PREFIX)) {
                return;
            }
            context.getStructureSwitchFailureReasonMap().put(key, reason);
        }
    }

    /**
     * 冻结候选身份的审计日志，便于对照全部候选与最终结果，不额外运行资格或资源分配。
     *
     * @param context 已完成前置阶段的来源和需求快照
     */
    private void appendCandidateSnapshot(LhScheduleContext context) {
        String detail = context.getStructureSwitchCandidateSkuMap().values().stream().map(sku -> {
            CxEmbryoLhTime source = context.getStructureSwitchCandidateSourceMap()
                .get(StructureSwitchCandidateScope.skuKey(sku));
            return String.format("批次=%s，结构=%s，物料=%s，状态=%s，sourceId=%s，供胚时间=%s，步骤前未完成需求=%s",
                    context.getBatchNo(), sku.getStructureName(), sku.getMaterialCode(), sku.getProductStatus(),
                    source.getId(), LhScheduleTimeUtil.formatDateTime(source.getEarliestLhTime()),
                    targetScheduleQtyResolver.previewUnfulfilledProductionRemainingQty(context, sku));
        }).collect(Collectors.joining("\n"));
        PriorityTraceLogHelper.appendProcessLog(context, "结构切换候选快照", detail);
    }

    /**
     * 单轮最多保留一个最佳完整提案，其余机台只保存编码、关系和等级。
     * @author APS
     * @since 2026/10/09
     */
    private static final class Selection {
        /**
         * 当前运行侧机台编码。
         */
        private final String machineCode;
        /**
         * 精确匹配的历史关系，没有时为空。
         */
        private final LhMouldChangePlan history;
        /**
         * 四级机台优先序。
         */
        private int rank;
        /**
         * 只读预演成功后冻结的单组合计划。
         */
        private NewSpecMachineAssignmentPlan assignment;
        /**
         * 当前班次、物理资源和历史/普通动作共同隔离的失败键。
         */
        private String failureKey;

        /**
         * 创建不预占资源的轻量机台候选。
         *
         * @param machineCode 实际机台
         * @param history 本批有效历史计划
         */
        private Selection(String machineCode, LhMouldChangePlan history) {
            this.machineCode = machineCode;
            this.history = history;
        }
    }
}
