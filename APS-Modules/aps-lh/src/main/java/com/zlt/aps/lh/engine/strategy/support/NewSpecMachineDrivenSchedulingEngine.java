package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.component.LhMachineSupplyStructureRule;
import com.zlt.aps.lh.component.DedicatedType;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * S4.5 新增排产机台驱动资源竞争引擎。
 *
 * <p>先按业务日合法班次查找专供承接机会，再按“班次→机台→日期池→候选 SKU”普通竞争。本引擎只生成一轮
 * Machine→SKU 轻量分配计划，不直接修改结果、机台、模具、首检或数量账本；
 * 正式时间轴和一次性提交继续交给现有新增排产内核。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class NewSpecMachineDrivenSchedulingEngine {

    /** 专供关系只读取本批内存索引，预演不消费资源。 */
    @Resource
    private LhMachineSupplyStructureRule machineSupplyStructureRule = new LhMachineSupplyStructureRule();

    /** 专供优先作用域。 */
    private static final String COMPETITION_SCOPE_SUPPLY = "专供机台优先承接";
    /** 固定指令指向其他物理机台。 */
    private static final int FIXED_INSTRUCTION_OTHER_MACHINE = 2;

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
        boolean dynamicBestMatchCompetitionEnabled =
                this.isDynamicBestMatchCompetitionEnabled(context);
        List<MachineResource> machineResources = this.buildMachineResources(
                context, dayContext, candidatePoolMap, dynamicBestMatchCompetitionEnabled);
        if (CollectionUtils.isEmpty(orderedPoolDates) || CollectionUtils.isEmpty(machineResources)) {
            return null;
        }
        /*
         * 专供优先后，尺寸建立同班次机台处理层级；组内完整适配同分后，55组先比余量再比名次，非55组直接比名次。
         * 组内继续保持现有固定指令、最新可用时间和机台编码顺序。
         */
        List<List<MachineResource>> dimensionMachineGroups =
                dynamicBestMatchCompetitionEnabled
                        ? this.groupMachineResourcesByDimension(context, machineResources)
                        : Collections.<List<MachineResource>>emptyList();
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
        // 先扫描本业务日全部合法班次的专供机会，再允许普通机台消费目标名额。
        // 提案正式提交后外层会刷新运行态；失败组合仍由现有失败缓存排除，不登记虚拟预留。
        NewSpecScheduleProposal supplyProposal = this.findSupplyPriorityProposal(
                context, dayContext, machineResources, orderedPoolDates, candidatePoolMap,
                machineMatch, normalizedFailureSet, availabilityResolver, actualAvailableTimeMode,
                pendingSkuIdentitySet, roundCache, dynamicBestMatchCompetitionEnabled);
        if (Objects.nonNull(supplyProposal)) {
            return supplyProposal;
        }
        NewSpecScheduleProposal deferredCrossDayProposal = null;
        LhShiftConfigVO deferredCrossDayShift = null;
        for (LhShiftConfigVO shift : dayContext.getDayShifts()) {
            if (Objects.isNull(shift) || Objects.isNull(shift.getShiftEndDateTime())) {
                continue;
            }
            // 匹配专供结构已在上方完整试排；其余候选不新增硬限制，仍保护固定指令。
            NewSpecScheduleProposal dedicatedMachineWinner = this.selectFirstDimensionGroupWinner(
                    context, dayContext, shift,
                    dynamicBestMatchCompetitionEnabled ? dimensionMachineGroups : Collections.singletonList(machineResources),
                    orderedPoolDates, candidatePoolMap, machineMatch, normalizedFailureSet,
                    availabilityResolver, actualAvailableTimeMode, pendingSkuIdentitySet,
                    resource -> machineSupplyStructureRule.getMachinePriority(context, resource.getPhysicalMachineCode()) == 0,
                    resource -> candidate -> this.canSelectUnderFixedInstructions(
                            context, dayContext, resource, candidate, fixedPhysicalMachineCodeSet),
                    roundCache, dynamicBestMatchCompetitionEnabled);
            if (Objects.nonNull(dedicatedMachineWinner)
                    && !dedicatedMachineWinner.getAvailabilityPlan().isSourceDayCrossDayPreparation()) {
                this.traceWinningProposal(context, dayContext, shift, dedicatedMachineWinner,
                        actualAvailableTimeMode, COMPETITION_SCOPE_DYNAMIC);
                return dedicatedMachineWinner;
            }
            if (!dynamicBestMatchCompetitionEnabled) {
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
             * 固定机台和续作原机台属于独立指令作用域。只有固定组合完整不可排时，
             * 才允许进入普通动态竞争，避免把固定指令作为普通匹配分值污染 bestSku 比较。
             */
            if (!CollectionUtils.isEmpty(fixedPhysicalMachineCodeSet)) {
                NewSpecScheduleProposal fixedWinner =
                        this.selectFirstDimensionGroupWinner(
                                context, dayContext, shift, dimensionMachineGroups,
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
            NewSpecScheduleProposal singleControlTrialWinner =
                    this.selectFirstDimensionGroupWinner(
                            context, dayContext, shift, dimensionMachineGroups,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            normalizedFailureSet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            machineResource -> LhSingleControlMachineUtil
                                    .isConfiguredSingleControlMachine(
                                            context,
                                            machineResource.getMachine().getMachineCode()),
                            machineResource -> this::isTrialOrMassTrialCandidate,
                            roundCache, true);
            if (Objects.nonNull(singleControlTrialWinner)) {
                this.traceWinningProposal(
                        context, dayContext, shift, singleControlTrialWinner,
                        actualAvailableTimeMode, COMPETITION_SCOPE_SINGLE_CONTROL_TRIAL);
                return singleControlTrialWinner;
            }
            NewSpecMachineProposalBuckets dimensionGroupWinnerBuckets =
                    this.selectFirstDimensionGroupProposalBuckets(
                            context, dayContext, shift, dimensionMachineGroups,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            normalizedFailureSet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet, roundCache);
            NewSpecScheduleProposal ordinaryWinner =
                    dimensionGroupWinnerBuckets.getOrdinaryProposal();
            if (Objects.nonNull(ordinaryWinner)) {
                this.traceWinningProposal(
                        context, dayContext, shift, ordinaryWinner,
                        actualAvailableTimeMode, COMPETITION_SCOPE_DYNAMIC);
                return ordinaryWinner;
            }
            NewSpecScheduleProposal crossDayWinner =
                    dimensionGroupWinnerBuckets.getCrossDayProposal();
            if (Objects.nonNull(crossDayWinner)) {
                if (Objects.isNull(deferredCrossDayProposal)) {
                    deferredCrossDayProposal = crossDayWinner;
                    deferredCrossDayShift = shift;
                    this.logSourceDayCrossDayProposalAction(
                            "暂存并继续查找当前业务日普通提案",
                            context, dayContext, shift, crossDayWinner);
                }
                continue;
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
     * 在既有业务日可排范围内先提交唯一结构的专供提案，允许等待后续班次释放。
     * @param context 批次上下文
     * @param dayContext 当前业务日及合法班次
     * @param resources 已通过资源作用域筛选的机台
     * @param poolDates 合法日期池
     * @param pools 本阶段候选
     * @param machineMatch 公共硬匹配
     * @param failedAssignments 当前运行态提交失败组合
     * @param resolver 完整时间轴预演入口
     * @param actualTimeMode 实际归班模式
     * @param pendingSkus 待排身份集合
     * @param cache 本轮缓存
     * @param dynamicCompetition 是否使用标准动态竞争
     * @return 可提交的专供提案，无可行机会时交回普通竞争
     */
    private NewSpecScheduleProposal findSupplyPriorityProposal(
            LhScheduleContext context, DayScheduleContext dayContext,
            List<MachineResource> resources, List<LocalDate> poolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> pools, IMachineMatchStrategy machineMatch,
            Set<String> failedAssignments, NewSpecMachineAvailabilityResolver resolver,
            boolean actualTimeMode, Set<SkuScheduleDTO> pendingSkus,
            NewSpecProposalRoundCache cache, boolean dynamicCompetition) {
        if (CollectionUtils.isEmpty(context.getFormingMachineSupplyLhMachineMap())) {
            return null;
        }
        List<List<MachineResource>> groups = dynamicCompetition
                ? this.groupMachineResourcesByDimension(context, resources) : Collections.singletonList(resources);
        Set<String> fixedMachines = this.resolveFixedPhysicalMachineCodes(context, dayContext, pools);
        // 强专供先完成本业务日合法机会扫描，再尝试弱专供；层内沿用完整最优匹配。
        for (DedicatedType dedicatedType : java.util.Arrays.asList(DedicatedType.EXCLUSIVE, DedicatedType.PREFERRED)) {
            for (LhShiftConfigVO shift : dayContext.getDayShifts()) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftEndDateTime())) {
                    continue;
                }
                // 单独划定专供组合范围，随后复用完整准入、供胚、结构名额和真实时间轴。
                NewSpecScheduleProposal proposal = this.selectFirstDimensionGroupWinner(
                        context, dayContext, shift, groups, poolDates, pools,
                        machineMatch, failedAssignments, resolver, actualTimeMode, pendingSkus,
                        resource -> !CollectionUtils.isEmpty(machineSupplyStructureRule
                                .getSuppliedFormingMachines(context, resource.getPhysicalMachineCode())),
                        resource -> candidate -> machineSupplyStructureRule.getMatchPriority(
                                context, resource.getPhysicalMachineCode(), candidate.getSku()) == dedicatedType
                                && this.canSelectUnderFixedInstructions(
                                context, dayContext, resource, candidate, fixedMachines),
                        cache, dynamicCompetition);
                if (Objects.nonNull(proposal)) {
                    this.traceWinningProposal(context, dayContext, shift, proposal,
                            actualTimeMode, COMPETITION_SCOPE_SUPPLY);
                    return proposal;
                }
            }
        }
        return null;
    }

    /**
     * 专供优选沿用固定指令保护，不能抢占其它指定SKU的机台。
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param resource 候选机台
     * @param candidate 候选SKU
     * @param fixedMachines 已有固定指令机台集合
     * @return 当前组合是否处于合法指令范围
     */
    private boolean canSelectUnderFixedInstructions(LhScheduleContext context, DayScheduleContext dayContext,
            MachineResource resource, DailyNewSpecCandidate candidate, Set<String> fixedMachines) {
        int fixedScore = this.resolveFixedInstructionScore(context, dayContext, candidate.getSku(), resource.getMachine());
        return fixedScore != FIXED_INSTRUCTION_OTHER_MACHINE
                && (!fixedMachines.contains(resource.getPhysicalMachineCode()) || fixedScore == 0);
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
     * @param standardDynamicCompetition 是否启用标准尺寸组完整适配竞争
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
            boolean standardDynamicCompetition) {
        List<NewSpecScheduleProposal> proposalList =
                new ArrayList<NewSpecScheduleProposal>(machineResources.size());
        for (MachineResource machineResource : machineResources) {
            if (!machineScope.test(machineResource)
                    || !this.isResourceReadyBeforeShiftEnd(machineResource, shift)) {
                continue;
            }
            NewSpecScheduleProposal proposal = machineSkuCompetitionService.findBestSkuForMachine(
                    context, dayContext, shift, machineResource, orderedPoolDates, candidatePoolMap,
                    machineMatch, failedAssignmentKeySet, availabilityResolver, actualAvailableTimeMode,
                    pendingSkuIdentitySet, candidateScopeResolver.apply(machineResource), roundCache,
                    standardDynamicCompetition);
            if (Objects.nonNull(proposal)) {
                proposalList.add(proposal);
            }
        }
        return proposalList;
    }

    /**
     * 按尺寸升序查找第一个能够形成胜出提案的机台组。
     *
     * <p>每个尺寸组内部完整复用现有单机最佳SKU和跨机台唯一胜者逻辑；小尺寸组仍有
     * 合法提案时不会扫描更大尺寸组。固定指令、单控试制量试等作用域由调用方保持原顺序。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前竞争班次
     * @param dimensionMachineGroups 尺寸升序机台组
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 已失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排SKU
     * @param machineScope 当前机台作用域
     * @param candidateScopeResolver 每台机台候选作用域
     * @param roundCache 当前阶段轻量缓存
     * @param standardDynamicCompetition 是否启用标准尺寸组完整适配竞争
     * @return 最小可排尺寸组的胜出提案；全部不可排时返回null
     */
    private NewSpecScheduleProposal selectFirstDimensionGroupWinner(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            List<List<MachineResource>> dimensionMachineGroups,
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
            boolean standardDynamicCompetition) {
        if (CollectionUtils.isEmpty(dimensionMachineGroups)) {
            return null;
        }
        for (List<MachineResource> dimensionMachineGroup : dimensionMachineGroups) {
            List<NewSpecScheduleProposal> proposalList = this.buildMachineBestProposalList(
                    context, dayContext, shift, dimensionMachineGroup,
                    orderedPoolDates, candidatePoolMap, machineMatch,
                    failedAssignmentKeySet, availabilityResolver,
                    actualAvailableTimeMode, pendingSkuIdentitySet,
                    machineScope, candidateScopeResolver, roundCache,
                    standardDynamicCompetition);
            NewSpecScheduleProposal winner = machineSkuCompetitionService.selectRoundWinner(
                    context, proposalList, roundCache, standardDynamicCompetition);
            if (Objects.nonNull(winner)) {
                return winner;
            }
        }
        return null;
    }

    /**
     * 按尺寸升序选择普通提案，并保留现有跨日准备兜底。
     *
     * <p>普通提案在第一个可排尺寸组产生后立即返回；跨日准备提案只暂存最小尺寸组胜者，
     * 继续扫描后续尺寸组的普通提案，确保“普通优先、跨日兜底”的既有语义不变。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前竞争班次
     * @param dimensionMachineGroups 尺寸升序机台组
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 已失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排SKU
     * @param roundCache 当前阶段轻量缓存
     * @return 普通胜出提案及最小尺寸组跨日兜底提案
     */
    private NewSpecMachineProposalBuckets selectFirstDimensionGroupProposalBuckets(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            List<List<MachineResource>> dimensionMachineGroups,
            List<LocalDate> orderedPoolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode,
            Set<SkuScheduleDTO> pendingSkuIdentitySet,
            NewSpecProposalRoundCache roundCache) {
        NewSpecScheduleProposal firstCrossDayWinner = null;
        for (List<MachineResource> dimensionMachineGroup : dimensionMachineGroups) {
            List<NewSpecMachineProposalBuckets> proposalBucketList =
                    this.buildMachineProposalBuckets(
                            context, dayContext, shift, dimensionMachineGroup,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            failedAssignmentKeySet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            machineResource -> true,
                            machineResource -> candidate -> true,
                            roundCache, true);
            List<NewSpecScheduleProposal> ordinaryProposalList =
                    new ArrayList<NewSpecScheduleProposal>(proposalBucketList.size());
            List<NewSpecScheduleProposal> crossDayProposalList =
                    new ArrayList<NewSpecScheduleProposal>(proposalBucketList.size());
            for (NewSpecMachineProposalBuckets proposalBucket : proposalBucketList) {
                if (proposalBucket.hasOrdinaryProposal()) {
                    ordinaryProposalList.add(proposalBucket.getOrdinaryProposal());
                }
                if (proposalBucket.hasCrossDayProposal()) {
                    crossDayProposalList.add(proposalBucket.getCrossDayProposal());
                }
            }
            NewSpecScheduleProposal ordinaryWinner = machineSkuCompetitionService
                    .selectRoundWinner(context, ordinaryProposalList, roundCache);
            if (Objects.nonNull(ordinaryWinner)) {
                return NewSpecMachineProposalBuckets.of(
                        ordinaryWinner, firstCrossDayWinner);
            }
            if (Objects.isNull(firstCrossDayWinner)) {
                firstCrossDayWinner = machineSkuCompetitionService
                        .selectRoundWinner(context, crossDayProposalList, roundCache);
            }
        }
        return NewSpecMachineProposalBuckets.of(null, firstCrossDayWinner);
    }

    /**
     * 为当前班次构建每台机台的普通/跨日准备双桶提案。
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
     * @param standardDynamicCompetition 是否启用标准尺寸组完整适配竞争
     * @return 每台机台最多一个普通桶和最多一个跨日准备桶
     */
    private List<NewSpecMachineProposalBuckets> buildMachineProposalBuckets(
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
            boolean standardDynamicCompetition) {
        List<NewSpecMachineProposalBuckets> proposalBucketList =
                new ArrayList<NewSpecMachineProposalBuckets>(machineResources.size());
        for (MachineResource machineResource : machineResources) {
            if (!machineScope.test(machineResource)
                    || !this.isResourceReadyBeforeShiftEnd(machineResource, shift)) {
                continue;
            }
            NewSpecMachineProposalBuckets proposalBucket =
                    machineSkuCompetitionService.findProposalBucketsForMachine(
                            context, dayContext, shift, machineResource,
                            orderedPoolDates, candidatePoolMap, machineMatch,
                            failedAssignmentKeySet, availabilityResolver,
                            actualAvailableTimeMode, pendingSkuIdentitySet,
                            candidateScopeResolver.apply(machineResource), roundCache,
                            standardDynamicCompetition);
            if (proposalBucket.hasOrdinaryProposal()
                    || proposalBucket.hasCrossDayProposal()) {
                proposalBucketList.add(proposalBucket);
            }
        }
        return proposalBucketList;
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
        NewSpecScheduleProposal firstCrossDayProposal = null;
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
            NewSpecMachineProposalBuckets proposalBucket =
                    machineSkuCompetitionService.findProposalBucketsForMachine(
                    context, dayContext, shift, machineResource, orderedPoolDates,
                    candidatePoolMap, machineMatch, failedAssignmentKeySet,
                    availabilityResolver, actualAvailableTimeMode,
                    pendingSkuIdentitySet, candidate -> true, roundCache, false);
            if (proposalBucket.hasOrdinaryProposal()) {
                return proposalBucket.getOrdinaryProposal();
            }
            if (proposalBucket.hasCrossDayProposal()
                    && Objects.isNull(firstCrossDayProposal)) {
                firstCrossDayProposal = proposalBucket.getCrossDayProposal();
            }
        }
        return firstCrossDayProposal;
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
        if ((StringUtils.equals(COMPETITION_SCOPE_DYNAMIC, competitionScope)
                || StringUtils.equals(COMPETITION_SCOPE_SHARED_ORDER, competitionScope))
                && !CollectionUtils.isEmpty(machineSupplyStructureRule.getPreferredLhMachines(
                context, proposal.getCandidate().getSku()))) {
            decisionEvidence = new StringBuilder(decisionEvidence)
                    .append(", 专供优先结果=本业务日合法范围内无剩余可提交匹配专供提案，进入普通竞争")
                    .toString();
        }
        String machineCode = proposal.getMatchResult().getMachine().getMachineCode();
        decisionEvidence = new StringBuilder(decisionEvidence)
                .append(", 专供结构类型=").append(machineSupplyStructureRule.getDedicatedType(context, proposal.getCandidate().getSku()))
                .append(", 专供匹配层级=").append(machineSupplyStructureRule.getMatchPriority(context, machineCode, proposal.getCandidate().getSku()))
                .toString();
        String dimensionSize = this.isDynamicBestMatchCompetitionEnabled(context)
                ? this.resolveMachineDimensionSize(context, machineCode).toPlainString()
                : "-";
        boolean fiftyFiveTieRuleEnabled = proposal.isFiftyFiveDimension()
                && (StringUtils.equals(COMPETITION_SCOPE_DYNAMIC, competitionScope)
                || StringUtils.equals(COMPETITION_SCOPE_SINGLE_CONTROL_TRIAL, competitionScope)
                || StringUtils.equals(COMPETITION_SCOPE_SUPPLY, competitionScope));
        // 仅首检结果没有窗口内正式生产班次，日志保留为空。
        Integer formalShiftIndex = Objects.isNull(plan.getFormalTargetShift())
                ? null : plan.getFormalTargetShift().getShiftIndex();
        log.info("新增排产机台驱动提案胜出, batchNo: {}, businessDate: {}, phase: {}, "
                        + "competitionScope: {}, resourceShiftIndex: {}, productionOccupationShiftIndex: {}, "
                        + "formalShiftIndex: {}, mode: {}, "
                        + "dimensionSize: {}, machineCode: {}, poolDate: {}, materialCode: {}, productStatus: {}, "
                        + "remainingMachineGap: {}, matchLevel: {}, fiftyFiveTieRuleEnabled: {}, "
                        + "competitionSurplusQty: {}, competitionSortRank: {}, productionOccupationStartTime: {}, "
                        + "formalAvailableTime: {}, {}",
                context.getBatchNo(), dayContext.getScheduleDate(), dayContext.getCurrentPhase(),
                competitionScope, resourceShift.getShiftIndex(),
                plan.getProductionOccupationShift().getShiftIndex(),
                formalShiftIndex,
                actualAvailableTimeMode ? "实际可开产时间" : "机台收尾时间",
                dimensionSize, machineCode, proposal.getPoolDate(),
                proposal.getCandidate().getSku().getMaterialCode(),
                proposal.getCandidate().getSku().getProductStatus(),
                proposal.getCandidate().getRemainingMachineCount(),
                proposal.getMatchResult().getMatchLevel().getDescription(),
                fiftyFiveTieRuleEnabled, proposal.getCompetitionSurplusQty(),
                proposal.getCompetitionSortRank(), plan.getProductionOccupationStartTime(),
                plan.getFormalAvailableProductionTime(), decisionEvidence);
        String detail = new StringBuilder(896)
                .append("batchNo=").append(context.getBatchNo())
                .append(", businessDate=").append(dayContext.getScheduleDate())
                .append(", phase=").append(dayContext.getCurrentPhase())
                .append(", competitionScope=").append(competitionScope)
                .append(", resourceShift=class").append(resourceShift.getShiftIndex())
                .append(", formalShift=class").append(
                        formalShiftIndex)
                .append(", productionOccupationShift=class").append(
                        plan.getProductionOccupationShift().getShiftIndex())
                .append(", shiftMode=").append(actualAvailableTimeMode
                        ? "实际可开产时间" : "机台收尾时间")
                .append(", dimensionSize=").append(dimensionSize)
                .append(", machineCode=").append(
                        machineCode)
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
                .append(", fiftyFiveTieRuleEnabled=").append(fiftyFiveTieRuleEnabled)
                .append(", competitionSurplusQty=").append(proposal.getCompetitionSurplusQty())
                .append(", competitionSortRank=").append(proposal.getCompetitionSortRank())
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
                .append(", completeSoftMatch=").append(
                        Objects.isNull(metricSnapshot) ? "-" : metricSnapshot.describeSoftMatch())
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

    /**
     * 构建当前轮次的轻量机台资源。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param candidatePoolMap 日期候选池
     * @param resolveDimensionSize 是否为标准S4.5资源解析尺寸
     * @return 按现有固定指令、可用时间和机台编码排序的机台资源
     */
    private List<MachineResource> buildMachineResources(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            boolean resolveDimensionSize) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return Collections.emptyList();
        }
        List<MachineResource> resources = new ArrayList<MachineResource>(
                context.getMachineScheduleMap().size());
        Date defaultAvailableTime = this.resolveDefaultMachineAvailableTime(
                context, dayContext);
        Set<String> machineResourceScopeCodeSet =
                context.getNewSpecMachineResourceScopeCodeSet();
        for (MachineScheduleDTO machine : context.getMachineScheduleMap().values()) {
            if (Objects.isNull(machine) || StringUtils.isEmpty(machine.getMachineCode())) {
                continue;
            }
            if (!CollectionUtils.isEmpty(machineResourceScopeCodeSet)
                    && !machineResourceScopeCodeSet.contains(machine.getMachineCode())) {
                continue;
            }
            resources.add(new MachineResource(
                    machine, Collections.singletonList(machine.getMachineCode()),
                    Objects.nonNull(machine.getEstimatedEndTime())
                            ? machine.getEstimatedEndTime() : defaultAvailableTime,
                    resolveDimensionSize
                            ? this.resolveMachineDimensionSize(context, machine.getMachineCode())
                            : null));
        }
        Set<String> fixedPhysicalMachineCodeSet = this.resolveFixedPhysicalMachineCodes(
                context, dayContext, candidatePoolMap);
        resources.sort(Comparator
                .comparingInt((MachineResource resource) -> machineSupplyStructureRule
                        .getMachinePriority(context, resource.getPhysicalMachineCode()))
                .thenComparingInt(resource ->
                        fixedPhysicalMachineCodeSet.contains(resource.getPhysicalMachineCode()) ? 0 : 1)
                .thenComparing(MachineResource.RESOURCE_ORDER));
        return resources;
    }

    /**
     * 将机台资源先按专供优先、再按数值尺寸升序分组。
     *
     * <p>TreeMap按BigDecimal数值比较，因此55与55.0归入同一组；组内按资源进入顺序
     * 保留固定指令、最新可用时间和机台编码等现有顺序。</p>
     *
     * @param context 专供关系上下文
     * @param machineResources 已按现有规则排序的机台资源
     * @return 尺寸升序的机台资源组
     */
    private List<List<MachineResource>> groupMachineResourcesByDimension(
            LhScheduleContext context, List<MachineResource> machineResources) {
        if (CollectionUtils.isEmpty(machineResources)) {
            return Collections.emptyList();
        }
        Map<Integer, Map<BigDecimal, List<MachineResource>>> priorityGroups = new TreeMap<>();
        for (MachineResource machineResource : machineResources) {
            BigDecimal dimensionSize = Objects.requireNonNull(
                    machineResource.getDimensionSize(), "机台尺寸不能为空");
            int priority = machineSupplyStructureRule.getMachinePriority(
                    context, machineResource.getPhysicalMachineCode());
            // 先分专供/普通，再分尺寸，避免TreeMap按尺寸重新覆盖专供优先。
            priorityGroups.computeIfAbsent(priority, key -> new TreeMap<>())
                    .computeIfAbsent(dimensionSize, key -> new ArrayList<MachineResource>(8))
                    .add(machineResource);
        }
        List<List<MachineResource>> groups = new ArrayList<>(machineResources.size());
        priorityGroups.values().forEach(dimensions -> groups.addAll(dimensions.values()));
        return groups;
    }

    /**
     * 从硫化机台基础资料解析数值尺寸。
     *
     * <p>单控L/R运行态优先读取本侧配置，缺失时再按物理机台编码读取。尺寸属于本次分组
     * 的必填业务输入，不设置默认值，避免基础数据异常时静默改变机台处理顺序。</p>
     *
     * @param context 排程上下文
     * @param machineCode 运行态机台编码
     * @return 数值尺寸
     */
    private BigDecimal resolveMachineDimensionSize(
            LhScheduleContext context,
            String machineCode) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getMachineInfoMap())
                || StringUtils.isEmpty(machineCode)) {
            throw new IllegalArgumentException("新增排产机台尺寸基础数据不能为空, machineCode="
                    + machineCode);
        }
        LhMachineInfo machineInfo = context.getMachineInfoMap().get(machineCode);
        if (Objects.isNull(machineInfo)) {
            machineInfo = context.getMachineInfoMap().get(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
        }
        if (Objects.isNull(machineInfo) || StringUtils.isEmpty(machineInfo.getDimensionSize())) {
            throw new IllegalArgumentException("新增排产机台尺寸未配置, machineCode=" + machineCode);
        }
        try {
            return new BigDecimal(StringUtils.trim(machineInfo.getDimensionSize()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "新增排产机台尺寸不是有效数字, machineCode=" + machineCode
                            + ", dimensionSize=" + machineInfo.getDimensionSize(), exception);
        }
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
