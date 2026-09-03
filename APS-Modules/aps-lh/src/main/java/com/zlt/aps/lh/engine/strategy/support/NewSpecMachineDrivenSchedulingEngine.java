package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * S4.5 新增排产机台驱动资源竞争引擎。
 *
 * <p>唯一编排顺序为“班次→机台→日期池→候选 SKU”。本引擎只生成一轮
 * Machine→SKU 轻量分配计划，不直接修改结果、机台、模具、首检或数量账本；
 * 正式时间轴和一次性提交继续交给现有新增排产内核。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class NewSpecMachineDrivenSchedulingEngine {

    /** 普通动态最佳匹配作用域。 */
    private static final String COMPETITION_SCOPE_DYNAMIC = "动态最佳匹配";
    /** 固定指令作用域。 */
    private static final String COMPETITION_SCOPE_FIXED = "固定指令";
    /** 单控试制、量试优先作用域。 */
    private static final String COMPETITION_SCOPE_SINGLE_CONTROL_TRIAL = "单控试制量试优先";
    /** 共享辅助入口原机台顺序作用域。 */
    private static final String COMPETITION_SCOPE_SHARED_ORDER = "共享入口原机台顺序";

    /** 日期池构建及 remainingMachineCount 刷新入口 */
    @Resource
    private NewSpecCandidatePoolBuilder candidatePoolBuilder;
    /** 单机最佳 SKU 与跨机台唯一胜出组合选择入口 */
    @Resource
    private NewSpecMachineSkuCompetitionService machineSkuCompetitionService;

    /**
     * 为当前阶段一次性构建并排序日期候选池。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param phase 当前阶段
     * @param candidateList 当前阶段候选
     * @return 日期升序候选池
     */
    public Map<LocalDate, List<DailyNewSpecCandidate>> buildCandidatePools(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DailySchedulePhase phase,
            List<DailyNewSpecCandidate> candidateList) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || CollectionUtils.isEmpty(candidateList)) {
            return Collections.emptyMap();
        }
        return candidatePoolBuilder.buildOrderedPools(
                context, this.resolveWindowStartDate(context, dayContext),
                dayContext.getScheduleDate(), phase, candidateList);
    }

    /**
     * 为当前阶段生成下一条完成真实时间轴预演的Machine→SKU提案。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param phase 当前阶段
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 已失败组合
     * @param availabilityResolver 正式时间轴无副作用解析器
     * @param roundCache 当前运行态版本只读缓存
     * @return 下一条可提交提案；全部组合不可行时返回null
     */
    public NewSpecScheduleProposal buildNextProposal(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            DailySchedulePhase phase,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            NewSpecProposalRoundCache roundCache) {
        if (Objects.isNull(context) || Objects.isNull(dayContext)
                || Objects.isNull(machineMatch) || Objects.isNull(availabilityResolver)
                || Objects.isNull(roundCache) || CollectionUtils.isEmpty(candidatePoolMap)) {
            return null;
        }
        List<LocalDate> orderedPoolDates = this.resolveOrderedPoolDates(
                candidatePoolMap, dayContext.getScheduleDate(), phase);
        List<MachineResource> machineResources = this.buildMachineResources(
                context, dayContext, candidatePoolMap);
        if (CollectionUtils.isEmpty(orderedPoolDates) || CollectionUtils.isEmpty(machineResources)) {
            return null;
        }
        Set<String> normalizedFailureSet = Objects.isNull(failedAssignmentKeySet)
                ? Collections.<String>emptySet() : failedAssignmentKeySet;
        boolean actualAvailableTimeMode = Objects.isNull(context.getScheduleConfig())
                || context.getScheduleConfig().isNewSpecMachineResourceUseActualAvailableTime();
        Set<SkuScheduleDTO> pendingSkuIdentitySet = Collections.newSetFromMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>(
                        Math.max(16, context.getNewSpecSkuList().size() * 2)));
        pendingSkuIdentitySet.addAll(context.getNewSpecSkuList());
        this.refreshCandidateRuntimeState(
                context, dayContext, candidatePoolMap);
        Set<String> fixedPhysicalMachineCodeSet = this.resolveFixedPhysicalMachineCodes(
                context, dayContext, candidatePoolMap);
        NewSpecScheduleProposal deferredCrossDayProposal = null;
        LhShiftConfigVO deferredCrossDayShift = null;
        for (LhShiftConfigVO shift : dayContext.getDayShifts()) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            if (!this.isDynamicBestMatchCompetitionEnabled(context)) {
                NewSpecScheduleProposal existingOrderProposal =
                        this.findFirstProposalByExistingMachineOrder(
                                context, dayContext, shift, machineResources,
                                orderedPoolDates, candidatePoolMap, machineMatch,
                                normalizedFailureSet, availabilityResolver,
                                actualAvailableTimeMode, pendingSkuIdentitySet,
                                fixedPhysicalMachineCodeSet, roundCache);
                if (Objects.nonNull(existingOrderProposal)) {
                    this.traceWinningProposal(
                            context, dayContext, shift, existingOrderProposal,
                            actualAvailableTimeMode, COMPETITION_SCOPE_SHARED_ORDER);
                    return existingOrderProposal;
                }
                continue;
            }
            /*
             * 固定机台、历史反选和续作原机台属于独立指令作用域。只有固定组合完整不可排时，
             * 才允许进入普通动态竞争，避免把固定指令作为普通匹配分值污染 bestSku 比较。
             */
            if (!CollectionUtils.isEmpty(fixedPhysicalMachineCodeSet)) {
                List<NewSpecScheduleProposal> fixedProposalList =
                        this.buildMachineBestProposalList(
                                context, dayContext, shift, machineResources,
                                orderedPoolDates, candidatePoolMap, machineMatch,
                                normalizedFailureSet, availabilityResolver,
                                actualAvailableTimeMode, pendingSkuIdentitySet,
                                machineResource -> fixedPhysicalMachineCodeSet.contains(
                                        machineResource.getPhysicalMachineCode()),
                                machineResource -> candidate ->
                                        this.resolveFixedInstructionScore(
                                                context, dayContext, candidate.getSku(),
                                                machineResource.getMachine()) == 0,
                                roundCache, false);
                NewSpecScheduleProposal fixedWinner = machineSkuCompetitionService
                        .selectRoundWinner(context, fixedProposalList, roundCache, false);
                if (Objects.nonNull(fixedWinner)) {
                    this.traceWinningProposal(
                            context, dayContext, shift, fixedWinner,
                            actualAvailableTimeMode, COMPETITION_SCOPE_FIXED);
                    return fixedWinner;
                }
            }
            /*
             * 单控试制/量试优先必须在“每台机台普通 bestSku”计算前形成独立作用域。
             * 否则单控机台可能因正规 SKU 匹配等级更高而把自身 bestSku 选成正规 SKU，
             * 进而漏掉池内已经完整可排的试制/量试组合，违背单控资源优先目的。
             */
            List<NewSpecScheduleProposal> singleControlTrialProposalList =
                    this.buildMachineBestProposalList(
                            context, dayContext, shift, machineResources,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            normalizedFailureSet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            machineResource -> LhSingleControlMachineUtil
                                    .isConfiguredSingleControlMachine(
                                            context,
                                            machineResource.getMachine().getMachineCode()),
                            machineResource -> this::isTrialOrMassTrialCandidate,
                            roundCache, true);
            NewSpecScheduleProposal singleControlTrialWinner = machineSkuCompetitionService
                    .selectRoundWinner(context, singleControlTrialProposalList, roundCache);
            if (Objects.nonNull(singleControlTrialWinner)) {
                this.traceWinningProposal(
                        context, dayContext, shift, singleControlTrialWinner,
                        actualAvailableTimeMode, COMPETITION_SCOPE_SINGLE_CONTROL_TRIAL);
                return singleControlTrialWinner;
            }
            List<NewSpecScheduleProposal> machineBestProposalList =
                    this.buildMachineBestProposalList(
                            context, dayContext, shift, machineResources,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            normalizedFailureSet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            machineResource -> true,
                            machineResource -> candidate -> true,
                            roundCache, true);
            NewSpecScheduleProposal winner = machineSkuCompetitionService
                    .selectRoundWinner(context, machineBestProposalList, roundCache);
            if (Objects.nonNull(winner)) {
                /*
                 * 跨日准备是空闲资源优化路径：同一业务日仍有普通提案时，普通提案优先提交；
                 * 只有全日无普通提案时才返回最早暂存的跨日准备提案。
                 */
                if (winner.getAvailabilityPlan().isSourceDayCrossDayPreparation()) {
                    if (Objects.isNull(deferredCrossDayProposal)) {
                        deferredCrossDayProposal = winner;
                        deferredCrossDayShift = shift;
                        this.logSourceDayCrossDayProposalAction(
                                "暂存并继续查找当前业务日普通提案",
                                context, dayContext, shift, winner);
                    }
                    continue;
                }
                this.traceWinningProposal(
                        context, dayContext, shift, winner,
                        actualAvailableTimeMode, COMPETITION_SCOPE_DYNAMIC);
                return winner;
            }
        }
        if (Objects.nonNull(deferredCrossDayProposal)) {
            this.logSourceDayCrossDayProposalAction(
                    "启用目标日跨日准备兜底", context, dayContext,
                    deferredCrossDayShift, deferredCrossDayProposal);
            this.traceWinningProposal(
                    context, dayContext, deferredCrossDayShift, deferredCrossDayProposal,
                    actualAvailableTimeMode, COMPETITION_SCOPE_DYNAMIC);
            return deferredCrossDayProposal;
        }
        return null;
    }

    /**
     * 输出目标日跨日准备提案的关键对账日志。
     *
     * @param action 当前决策动作
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 跨日准备占用的资源班次
     * @param proposal 跨日准备提案
     */
    private void logSourceDayCrossDayProposalAction(String action,
                                                    LhScheduleContext context,
                                                    DayScheduleContext dayContext,
                                                    LhShiftConfigVO shift,
                                                    NewSpecScheduleProposal proposal) {
        log.info("目标日跨日准备提案{}, batchNo: {}, businessDate: {}, resourceShift: class{}, "
                        + "formalShift: class{}, machineCode: {}, materialCode: {}, productStatus: {}",
                action, context.getBatchNo(), dayContext.getScheduleDate(),
                shift.getShiftIndex(),
                proposal.getAvailabilityPlan().getFormalTargetShift().getShiftIndex(),
                proposal.getMatchResult().getMachine().getMachineCode(),
                proposal.getCandidate().getSku().getMaterialCode(),
                proposal.getCandidate().getSku().getProductStatus());
    }

    /**
     * 为当前班次构建“每台机台一个最佳 SKU”的轻量提案列表。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前竞争班次
     * @param machineResources 当前机台资源
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 已失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排 SKU
     * @param machineScope 当前机台作用域
     * @param candidateScopeResolver 每台机台的候选作用域
     * @param roundCache 当前阶段轻量缓存
     * @param prioritizeTargetMachineGap 是否优先补统一Map目标物理机台缺口
     * @return 每台机台最多一个最佳提案
     */
    private List<NewSpecScheduleProposal> buildMachineBestProposalList(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            List<MachineResource> machineResources,
            List<LocalDate> orderedPoolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode,
            Set<SkuScheduleDTO> pendingSkuIdentitySet,
            Predicate<MachineResource> machineScope,
            java.util.function.Function<MachineResource, Predicate<DailyNewSpecCandidate>>
                    candidateScopeResolver,
            NewSpecProposalRoundCache roundCache,
            boolean prioritizeTargetMachineGap) {
        List<NewSpecScheduleProposal> proposalList =
                new ArrayList<NewSpecScheduleProposal>(machineResources.size());
        for (MachineResource machineResource : machineResources) {
            if (!machineScope.test(machineResource)
                    || !this.isResourceReadyBeforeShiftEnd(machineResource, shift)) {
                continue;
            }
            NewSpecScheduleProposal proposal = machineSkuCompetitionService
                    .findBestSkuForMachine(
                            context, dayContext, shift, machineResource,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            failedAssignmentKeySet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            candidateScopeResolver.apply(machineResource), roundCache,
                            prioritizeTargetMachineGap);
            if (Objects.nonNull(proposal)) {
                proposalList.add(proposal);
            }
        }
        return proposalList;
    }

    /**
     * 共享新增内核的辅助入口保持现有机台顺序，避免 S4.5.1/S4.5.2 继承跨 SKU 动态竞争。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前竞争班次
     * @param machineResources 当前机台资源
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 已失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排 SKU
     * @param fixedPhysicalMachineCodeSet 固定指令物理机台集合
     * @param roundCache 当前阶段轻量缓存
     * @return 按原机台顺序找到的首个提案；无可排组合时返回 null
     */
    private NewSpecScheduleProposal findFirstProposalByExistingMachineOrder(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            List<MachineResource> machineResources,
            List<LocalDate> orderedPoolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode,
            Set<SkuScheduleDTO> pendingSkuIdentitySet,
            Set<String> fixedPhysicalMachineCodeSet,
            NewSpecProposalRoundCache roundCache) {
        for (MachineResource machineResource : machineResources) {
            if (!this.isResourceReadyBeforeShiftEnd(machineResource, shift)) {
                continue;
            }
            if (fixedPhysicalMachineCodeSet.contains(machineResource.getPhysicalMachineCode())) {
                NewSpecScheduleProposal fixedProposal = machineSkuCompetitionService
                        .findBestSkuForMachine(
                                context, dayContext, shift, machineResource,
                                orderedPoolDates, candidatePoolMap, machineMatch,
                                failedAssignmentKeySet, availabilityResolver,
                                actualAvailableTimeMode, pendingSkuIdentitySet,
                                candidate -> this.resolveFixedInstructionScore(
                                        context, dayContext, candidate.getSku(),
                                        machineResource.getMachine()) == 0,
                                roundCache, false);
                if (Objects.nonNull(fixedProposal)) {
                    return fixedProposal;
                }
            }
            NewSpecScheduleProposal proposal = machineSkuCompetitionService.findBestSkuForMachine(
                    context, dayContext, shift, machineResource, orderedPoolDates,
                    candidatePoolMap, machineMatch, failedAssignmentKeySet,
                    availabilityResolver, actualAvailableTimeMode,
                    pendingSkuIdentitySet, candidate -> true, roundCache, false);
            if (Objects.nonNull(proposal)) {
                return proposal;
            }
        }
        return null;
    }

    /**
     * 记录当前轮唯一胜出提案及其统一竞争口径。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param resourceShift 机台资源归属班次
     * @param proposal 当前轮胜出提案
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param competitionScope 胜出提案所属竞争作用域
     */
    private void traceWinningProposal(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO resourceShift,
            NewSpecScheduleProposal proposal,
            boolean actualAvailableTimeMode,
            String competitionScope) {
        NewSpecMachineAvailabilityPlan plan = proposal.getAvailabilityPlan();
        // 胜出后只读取固定指令和冻结快照补齐对账证据，不重新执行候选匹配或资源计算。
        String decisionEvidence = this.buildWinningDecisionEvidence(
                context, dayContext.getScheduleDate(), proposal.getCandidate().getSku(),
                proposal.getMatchResult(), competitionScope);
        log.info("新增排产机台驱动提案胜出, batchNo: {}, businessDate: {}, phase: {}, "
                        + "competitionScope: {}, resourceShiftIndex: {}, productionOccupationShiftIndex: {}, "
                        + "formalShiftIndex: {}, mode: {}, "
                        + "machineCode: {}, poolDate: {}, materialCode: {}, productStatus: {}, "
                        + "remainingMachineGap: {}, matchLevel: {}, productionOccupationStartTime: {}, "
                        + "formalAvailableTime: {}, {}",
                context.getBatchNo(), dayContext.getScheduleDate(), dayContext.getCurrentPhase(),
                competitionScope, resourceShift.getShiftIndex(),
                plan.getProductionOccupationShift().getShiftIndex(),
                plan.getFormalTargetShift().getShiftIndex(),
                actualAvailableTimeMode ? "实际可开产时间" : "机台收尾时间",
                proposal.getMatchResult().getMachine().getMachineCode(), proposal.getPoolDate(),
                proposal.getCandidate().getSku().getMaterialCode(),
                proposal.getCandidate().getSku().getProductStatus(),
                proposal.getCandidate().getRemainingMachineCount(),
                proposal.getMatchResult().getMatchLevel().getDescription(),
                plan.getProductionOccupationStartTime(),
                plan.getFormalAvailableProductionTime(), decisionEvidence);
        String detail = new StringBuilder(896)
                .append("batchNo=").append(context.getBatchNo())
                .append(", businessDate=").append(dayContext.getScheduleDate())
                .append(", phase=").append(dayContext.getCurrentPhase())
                .append(", competitionScope=").append(competitionScope)
                .append(", resourceShift=class").append(resourceShift.getShiftIndex())
                .append(", formalShift=class").append(
                        plan.getFormalTargetShift().getShiftIndex())
                .append(", productionOccupationShift=class").append(
                        plan.getProductionOccupationShift().getShiftIndex())
                .append(", shiftMode=").append(actualAvailableTimeMode
                        ? "实际可开产时间" : "机台收尾时间")
                .append(", machineCode=").append(
                        proposal.getMatchResult().getMachine().getMachineCode())
                .append(", physicalMachineCode=").append(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                                proposal.getMatchResult().getMachine().getMachineCode()))
                .append(", poolDate=").append(proposal.getPoolDate())
                .append(", materialCode=").append(
                        proposal.getCandidate().getSku().getMaterialCode())
                .append(", productStatus=").append(
                        proposal.getCandidate().getSku().getProductStatus())
                .append(", remainingMachineGap=").append(
                        proposal.getCandidate().getRemainingMachineCount())
                .append(", matchLevel=").append(
                        proposal.getMatchResult().getMatchLevel().getDescription())
                .append(", machineEndingTime=").append(
                        LhScheduleTimeUtil.formatDateTime(
                                machineSkuCompetitionService.resolveCompetitionEndingTime(
                                        context, proposal.getMatchResult())))
                .append(", formalAvailableTime=").append(
                        LhScheduleTimeUtil.formatDateTime(
                                plan.getFormalAvailableProductionTime()))
                .append(", productionOccupationStartTime=").append(
                        LhScheduleTimeUtil.formatDateTime(
                                plan.getProductionOccupationStartTime()))
                .append(", ").append(decisionEvidence)
                .toString();
        PriorityTraceLogHelper.appendProcessLog(
                context, "新增排产机台选择SKU", detail);
    }

    /**
     * 构建胜出组合的固定指令、前物料和冻结匹配证据。
     *
     * <p>本方法只读取胜出提案已经生成的软指标快照；不重新分配模具、不重新执行匹配，
     * 固定来源也仅在固定指令作用域内解析，避免给普通动态竞争增加无意义扫描。</p>
     *
     * @param context 排程上下文
     * @param businessDate 当前业务日
     * @param sku 胜出SKU
     * @param matchResult 已冻结匹配结果
     * @param competitionScope 竞争作用域
     * @return 可直接追加到应用日志和过程日志的决策证据
     */
    String buildWinningDecisionEvidence(LhScheduleContext context,
                                        LocalDate businessDate,
                                        SkuScheduleDTO sku,
                                        MachineSkuMatchResult matchResult,
                                        String competitionScope) {
        MachineScheduleDTO machine = Objects.isNull(matchResult)
                ? null : matchResult.getMachine();
        MachinePriorityMetricSnapshot metricSnapshot = Objects.isNull(matchResult)
                ? null : matchResult.getPriorityMetricSnapshot();
        boolean fixedInstructionScope = StringUtils.equals(
                COMPETITION_SCOPE_FIXED, competitionScope);
        DayTypeBlockReverseSelectionDirective dayTypeBlockDirective = fixedInstructionScope
                ? this.findDayTypeBlockFixedDirective(
                context, businessDate, sku, machine) : null;
        String fixedInstructionSource = fixedInstructionScope
                ? this.resolveFixedInstructionSource(
                context, sku, machine, dayTypeBlockDirective) : "-";
        String fixedInstructionMatchedLayer = Objects.isNull(dayTypeBlockDirective)
                ? "-" : MachinePriorityMetricSnapshot.resolveTraceText(
                dayTypeBlockDirective.getMatchedLayer());
        String previousMaterialCode = this.resolveWinningPreviousMaterialCode(
                context, machine, dayTypeBlockDirective);
        String ordinaryCompetitionStatus = this.resolveOrdinaryCompetitionStatus(competitionScope);
        String ordinaryCompetitionReason = this.resolveOrdinaryCompetitionReason(competitionScope);
        return new StringBuilder(512)
                .append("fixedInstructionSource=").append(fixedInstructionSource)
                .append(", fixedInstructionMatchedLayer=").append(fixedInstructionMatchedLayer)
                .append(", previousMaterialCode=").append(
                        MachinePriorityMetricSnapshot.resolveTraceText(previousMaterialCode))
                .append(", embryoMatchedValue=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null : metricSnapshot.getEmbryoMatchedValue()))
                .append(", mouldShellMatchedValue=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null : metricSnapshot.getMouldShellMatchedValue()))
                .append(", specMatchedValue=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null : metricSnapshot.getSpecMatchedValue()))
                .append(", proSizeMatchedValue=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null : metricSnapshot.getProSizeMatchedValue()))
                .append(", targetMouldCodes=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null : metricSnapshot.getTargetMouldCodes()))
                .append(", targetMouldShellStandards=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null
                                : metricSnapshot.getTargetMouldShellStandards()))
                .append(", machineBoundMouldCodes=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null
                                : metricSnapshot.getMachineBoundMouldCodes()))
                .append(", machineBoundMouldShellStandards=").append(MachinePriorityMetricSnapshot.resolveTraceText(
                        Objects.isNull(metricSnapshot) ? null
                                : metricSnapshot.getMachineBoundMouldShellStandards()))
                .append(", ordinaryCompetitionStatus=").append(ordinaryCompetitionStatus)
                .append(", ordinaryCompetitionReason=").append(ordinaryCompetitionReason)
                .toString();
    }

    /**
     * 判断当前调用是否为标准 S4.5 新增排产。
     *
     * @param context 排程上下文
     * @return true-启用跨 SKU 动态竞争；false-共享辅助入口保持原机台顺序
     */
    private boolean isDynamicBestMatchCompetitionEnabled(LhScheduleContext context) {
        return Objects.nonNull(context) && StringUtils.equals(
                ScheduleStepEnum.S4_5_NEW_PRODUCTION.getCode(), context.getCurrentStep());
    }

    /**
     * 判断候选是否属于单控资源优先保护的试制或量试 SKU。
     *
     * @param candidate 当前日期池候选
     * @return true-试制或量试；false-其它类型
     */
    private boolean isTrialOrMassTrialCandidate(DailyNewSpecCandidate candidate) {
        if (Objects.isNull(candidate) || Objects.isNull(candidate.getSku())) {
            return false;
        }
        String constructionStage = candidate.getSku().getConstructionStage();
        return StringUtils.equals(ConstructionStageEnum.TRIAL.getCode(), constructionStage)
                || StringUtils.equals(
                ConstructionStageEnum.MASS_TRIAL.getCode(), constructionStage);
    }

    /**
     * 每次提案扫描前按当前正式结果刷新候选剩余机台机会。
     *
     * <p>同一候选不再在每台机台、每个班次内重复读取中心目标机台数和已排机台数。</p>
     */
    private void refreshCandidateRuntimeState(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap) {
        Set<DailyNewSpecCandidate> refreshedCandidateSet = Collections.newSetFromMap(
                new IdentityHashMap<DailyNewSpecCandidate, Boolean>(16));
        for (List<DailyNewSpecCandidate> poolCandidates : candidatePoolMap.values()) {
            if (CollectionUtils.isEmpty(poolCandidates)) {
                continue;
            }
            for (DailyNewSpecCandidate candidate : poolCandidates) {
                if (Objects.nonNull(candidate) && refreshedCandidateSet.add(candidate)) {
                    candidatePoolBuilder.refreshRemainingMachineCount(
                            context, dayContext.getScheduleDate(), candidate);
                }
            }
        }
    }

    private List<LocalDate> resolveOrderedPoolDates(
            Map<LocalDate, List<DailyNewSpecCandidate>> poolMap,
            LocalDate currentDate,
            DailySchedulePhase phase) {
        if (CollectionUtils.isEmpty(poolMap) || Objects.isNull(currentDate)) {
            return Collections.emptyList();
        }
        List<LocalDate> orderedDates = new ArrayList<LocalDate>(poolMap.size());
        for (LocalDate poolDate : poolMap.keySet()) {
            if (Objects.isNull(poolDate)) {
                continue;
            }
            if (phase == DailySchedulePhase.EARLY_PRODUCTION) {
                if (poolDate.isAfter(currentDate)) {
                    orderedDates.add(poolDate);
                }
            } else if (!poolDate.isAfter(currentDate)) {
                orderedDates.add(poolDate);
            }
        }
        Collections.sort(orderedDates);
        return orderedDates;
    }

    private List<MachineResource> buildMachineResources(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return Collections.emptyList();
        }
        List<MachineResource> resources = new ArrayList<MachineResource>(
                context.getMachineScheduleMap().size());
        Date defaultAvailableTime = this.resolveDefaultMachineAvailableTime(
                context, dayContext);
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
                continue;
            }
            resources.add(new MachineResource(
                    machine, Collections.singletonList(machine.getMachineCode()),
                    Objects.nonNull(machine.getEstimatedEndTime())
                            ? machine.getEstimatedEndTime() : defaultAvailableTime));
        }
        Set<String> fixedPhysicalMachineCodeSet = this.resolveFixedPhysicalMachineCodes(
                context, dayContext, candidatePoolMap);
        resources.sort(Comparator
                .comparingInt((MachineResource resource) ->
                        fixedPhysicalMachineCodeSet.contains(resource.getPhysicalMachineCode()) ? 0 : 1)
                .thenComparing(MachineResource.RESOURCE_ORDER));
        return resources;
    }

    /**
     * 解析候选在当前机台的固定指令优先档。
     *
     * @return 0-命中固定机台，1-无固定指令，2-固定指令指向其它机台
     */
    private int resolveFixedInstructionScore(LhScheduleContext context,
                                             DayScheduleContext dayContext,
                                             SkuScheduleDTO sku,
                                             MachineScheduleDTO machine) {
        Set<String> fixedMachineCodes = this.resolveFixedMachineCodes(
                context, dayContext, sku);
        if (CollectionUtils.isEmpty(fixedMachineCodes)) {
            return 1;
        }
        String physicalMachineCode = Objects.isNull(machine) ? null
                : LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                        machine.getMachineCode());
        return fixedMachineCodes.contains(physicalMachineCode) ? 0 : 2;
    }

    /**
     * 解析胜出固定组合的真实指令来源。
     *
     * <p>判断顺序与 {@link #resolveFixedMachineCodes(LhScheduleContext, DayScheduleContext, SkuScheduleDTO)}
     * 保持一致，只在胜出日志阶段读取已有上下文，不改变固定机台集合和竞争顺序。</p>
     *
     * @param context 排程上下文
     * @param sku 胜出SKU
     * @param machine 胜出机台
     * @param dayTypeBlockDirective 已匹配的按天换活字块反选指令
     * @return 固定指令来源；无法解析时返回“-”
     */
    private String resolveFixedInstructionSource(LhScheduleContext context,
                                                 SkuScheduleDTO sku,
                                                 MachineScheduleDTO machine,
                                                 DayTypeBlockReverseSelectionDirective
                                                         dayTypeBlockDirective) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)) {
            return "-";
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                machine.getMachineCode());
        if (this.isSamePhysicalMachine(
                physicalMachineCode, context.resolveSubstitutionSpecifiedMachineCode(sku))) {
            return "共用模具置换指定机台";
        }
        if (this.isSamePhysicalMachine(
                physicalMachineCode, sku.getPreferredContinuousMachineCode())) {
            return "续作原机台";
        }
        if (!CollectionUtils.isEmpty(context.getHistoricalReverseSelectionDirectiveList())) {
            for (HistoricalReverseSelectionDirective directive
                    : context.getHistoricalReverseSelectionDirectiveList()) {
                if (Objects.nonNull(directive) && !directive.isAttempted()
                        && !directive.isAlreadySatisfied()
                        && this.isSameSku(directive.getMaterialCode(),
                        directive.getProductStatus(), sku)
                        && this.isSamePhysicalMachine(
                        physicalMachineCode,
                        StringUtils.isNotEmpty(directive.getEffectiveMachineCode())
                                ? directive.getEffectiveMachineCode() : directive.getMachineCode())) {
                    return "历史交替计划反选";
                }
            }
        }
        if (Objects.nonNull(dayTypeBlockDirective)) {
            return "按天换活字块反选";
        }
        return "-";
    }

    /**
     * 查找胜出组合对应的按天换活字块固定指令。
     *
     * <p>固定来源和固定匹配层级共同复用本次查询结果，避免为胜出日志重复扫描指令列表。</p>
     *
     * @param context 排程上下文
     * @param businessDate 当前业务日
     * @param sku 胜出SKU
     * @param machine 胜出机台
     * @return 匹配指令；未命中返回null
     */
    private DayTypeBlockReverseSelectionDirective findDayTypeBlockFixedDirective(
            LhScheduleContext context,
            LocalDate businessDate,
            SkuScheduleDTO sku,
            MachineScheduleDTO machine) {
        if (Objects.isNull(context) || Objects.isNull(sku) || Objects.isNull(machine)
                || CollectionUtils.isEmpty(
                context.getDayTypeBlockReverseSelectionDirectiveList())) {
            return null;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                machine.getMachineCode());
        for (DayTypeBlockReverseSelectionDirective directive
                : context.getDayTypeBlockReverseSelectionDirectiveList()) {
            if (Objects.nonNull(directive) && !directive.isSatisfied()
                    && Objects.equals(businessDate, directive.getScheduleDate())
                    && this.isSameSku(directive.getMaterialCode(),
                    directive.getProductStatus(), sku)
                    && this.isSamePhysicalMachine(
                    physicalMachineCode, directive.getMachineCode())) {
                return directive;
            }
        }
        return null;
    }

    /**
     * 解析胜出组合对应的选机前物料。
     *
     * <p>按天换活字块指令优先使用检测时冻结的前物料；普通动态竞争优先读取机台对象的
     * 前物料，若机台已在续作释放阶段清空，则读取初始化机台快照的当前物料。该方法只
     * 读取已有运行态快照，不参与任何排程计算。</p>
     *
     * @param context 排程上下文
     * @param machine 胜出机台
     * @param dayTypeBlockDirective 按天换活字块反选指令
     * @return 选机前物料编码；无法解析时返回null
     */
    private String resolveWinningPreviousMaterialCode(
            LhScheduleContext context,
            MachineScheduleDTO machine,
            DayTypeBlockReverseSelectionDirective dayTypeBlockDirective) {
        if (Objects.nonNull(dayTypeBlockDirective)
                && StringUtils.isNotEmpty(dayTypeBlockDirective.getPreviousMaterialCode())) {
            return dayTypeBlockDirective.getPreviousMaterialCode();
        }
        if (Objects.isNull(machine)) {
            return null;
        }
        if (StringUtils.isNotEmpty(machine.getPreviousMaterialCode())) {
            return machine.getPreviousMaterialCode();
        }
        if (Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getInitialMachineScheduleMap())
                || StringUtils.isEmpty(machine.getMachineCode())) {
            return null;
        }
        MachineScheduleDTO initialMachine = context.getInitialMachineScheduleMap().get(
                machine.getMachineCode());
        return Objects.isNull(initialMachine) ? null : initialMachine.getCurrentMaterialCode();
    }

    /**
     * 判断两个机台编码是否指向同一物理机台。
     *
     * @param expectedPhysicalMachineCode 目标物理机台编码
     * @param candidateMachineCode 待比较机台编码
     * @return true-同一物理机台；false-不同或编码为空
     */
    private boolean isSamePhysicalMachine(String expectedPhysicalMachineCode,
                                          String candidateMachineCode) {
        if (StringUtils.isEmpty(expectedPhysicalMachineCode)
                || StringUtils.isEmpty(candidateMachineCode)) {
            return false;
        }
        return StringUtils.equals(
                expectedPhysicalMachineCode,
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(candidateMachineCode));
    }

    /**
     * 解析胜出组合是否参加普通动态竞争。
     *
     * @param competitionScope 竞争作用域
     * @return 已参与、未参与或不适用
     */
    private String resolveOrdinaryCompetitionStatus(String competitionScope) {
        if (StringUtils.equals(COMPETITION_SCOPE_DYNAMIC, competitionScope)) {
            return "已参与";
        }
        if (StringUtils.equals(COMPETITION_SCOPE_SHARED_ORDER, competitionScope)) {
            return "不适用";
        }
        return "未参与";
    }

    /**
     * 解析胜出组合未参加普通动态竞争的原因。
     *
     * @param competitionScope 竞争作用域
     * @return 普通竞争状态原因
     */
    private String resolveOrdinaryCompetitionReason(String competitionScope) {
        if (StringUtils.equals(COMPETITION_SCOPE_FIXED, competitionScope)) {
            return "固定组合完整可排，按独立指令作用域优先提交";
        }
        if (StringUtils.equals(COMPETITION_SCOPE_SINGLE_CONTROL_TRIAL, competitionScope)) {
            return "单控试制量试独立作用域已有完整可排组合";
        }
        if (StringUtils.equals(COMPETITION_SCOPE_SHARED_ORDER, competitionScope)) {
            return "共享辅助入口保持原机台顺序";
        }
        return "-";
    }

    private Set<String> resolveFixedPhysicalMachineCodes(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap) {
        Set<String> fixedMachineCodes = new java.util.LinkedHashSet<String>(8);
        for (List<DailyNewSpecCandidate> candidates : candidatePoolMap.values()) {
            if (CollectionUtils.isEmpty(candidates)) {
                continue;
            }
            for (DailyNewSpecCandidate candidate : candidates) {
                if (Objects.nonNull(candidate) && Objects.nonNull(candidate.getSku())) {
                    fixedMachineCodes.addAll(this.resolveFixedMachineCodes(
                            context, dayContext, candidate.getSku()));
                }
            }
        }
        return fixedMachineCodes;
    }

    private Set<String> resolveFixedMachineCodes(LhScheduleContext context,
                                                 DayScheduleContext dayContext,
                                                 SkuScheduleDTO sku) {
        Set<String> fixedPhysicalMachineCodes = new java.util.LinkedHashSet<String>(4);
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return fixedPhysicalMachineCodes;
        }
        this.addFixedMachineCode(
                fixedPhysicalMachineCodes,
                context.resolveSubstitutionSpecifiedMachineCode(sku));
        this.addFixedMachineCode(
                fixedPhysicalMachineCodes, sku.getPreferredContinuousMachineCode());
        if (!CollectionUtils.isEmpty(context.getHistoricalReverseSelectionDirectiveList())) {
            for (HistoricalReverseSelectionDirective directive
                    : context.getHistoricalReverseSelectionDirectiveList()) {
                if (Objects.isNull(directive) || directive.isAttempted()
                        || directive.isAlreadySatisfied()
                        || !this.isSameSku(directive.getMaterialCode(),
                        directive.getProductStatus(), sku)) {
                    continue;
                }
                this.addFixedMachineCode(fixedPhysicalMachineCodes,
                        StringUtils.isNotEmpty(directive.getEffectiveMachineCode())
                                ? directive.getEffectiveMachineCode() : directive.getMachineCode());
            }
        }
        if (!CollectionUtils.isEmpty(context.getDayTypeBlockReverseSelectionDirectiveList())) {
            for (DayTypeBlockReverseSelectionDirective directive
                    : context.getDayTypeBlockReverseSelectionDirectiveList()) {
                if (Objects.isNull(directive) || directive.isSatisfied()
                        || Objects.isNull(dayContext)
                        || !Objects.equals(dayContext.getScheduleDate(), directive.getScheduleDate())
                        || !this.isSameSku(directive.getMaterialCode(),
                        directive.getProductStatus(), sku)) {
                    continue;
                }
                this.addFixedMachineCode(
                        fixedPhysicalMachineCodes, directive.getMachineCode());
            }
        }
        return fixedPhysicalMachineCodes;
    }

    private void addFixedMachineCode(Set<String> fixedPhysicalMachineCodes,
                                     String machineCode) {
        if (StringUtils.isEmpty(machineCode)) {
            return;
        }
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        if (StringUtils.isNotEmpty(physicalMachineCode)) {
            fixedPhysicalMachineCodes.add(physicalMachineCode);
        }
    }

    private boolean isSameSku(String materialCode,
                              String productStatus,
                              SkuScheduleDTO sku) {
        String normalizedDirectiveStatus = StringUtils.defaultIfEmpty(productStatus, "S");
        String normalizedSkuStatus = StringUtils.defaultIfEmpty(sku.getProductStatus(), "S");
        return StringUtils.equals(materialCode, sku.getMaterialCode())
                && StringUtils.equals(normalizedDirectiveStatus, normalizedSkuStatus);
    }

    private boolean isResourceReadyBeforeShiftEnd(MachineResource machineResource,
                                                   LhShiftConfigVO shift) {
        Date latestAvailableTime = machineResource.getEndingTime();
        return Objects.nonNull(latestAvailableTime)
                && Objects.nonNull(shift)
                && Objects.nonNull(shift.getShiftEndDateTime())
                && latestAvailableTime.before(shift.getShiftEndDateTime());
    }

    /**
     * 解析从未占用机台进入本轮竞争时的默认可用时间。
     *
     * <p>排产提交成功后机台预计结束时间会立即更新，下一轮重新构建资源时自然读取最新值。
     * 从未占用的空闲机台以本次排程窗口开始时间作为可用时间，保证后续准入统一执行严格
     * {@code machineAvailableTime < shiftEndTime}，不通过null分支绕过边界。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @return 从未占用机台的默认可用时间
     */
    private Date resolveDefaultMachineAvailableTime(LhScheduleContext context,
                                                    DayScheduleContext dayContext) {
        if (Objects.nonNull(context)
                && !CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            Date windowStartTime = context.getScheduleWindowShifts().stream()
                    .filter(Objects::nonNull)
                    .map(LhShiftConfigVO::getShiftStartDateTime)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);
            if (Objects.nonNull(windowStartTime)) {
                return windowStartTime;
            }
        }
        return Objects.isNull(dayContext) ? null : dayContext.getDayStartTime();
    }

    private LocalDate resolveWindowStartDate(LhScheduleContext context,
                                             DayScheduleContext dayContext) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getScheduleWindowShifts())) {
            return dayContext.getScheduleDate();
        }
        for (LhShiftConfigVO shift : context.getScheduleWindowShifts()) {
            if (Objects.nonNull(shift) && Objects.nonNull(shift.getWorkDate())) {
                return shift.getWorkDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            }
        }
        return dayContext.getScheduleDate();
    }
}
