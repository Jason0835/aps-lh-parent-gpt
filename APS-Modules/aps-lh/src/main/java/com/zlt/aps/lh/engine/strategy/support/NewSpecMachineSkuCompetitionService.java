package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * S4.5 新增排产 Machine-SKU 动态竞争选择器。
 *
 * <p>本选择器只读取当前日期候选池和排程运行态。每台机台扫描时只保留一个最佳可排提案，
 * 不缓存完整 Machine×SKU 矩阵；跨机台阶段再按单控试制/量试优先、匹配等级、收尾时间和
 * 机台编码选出本轮唯一提案。正式资源扣减和结果写入仍由现有提交链完成。</p>
 *
 * @author APS
 */
@Component
@Slf4j
public class NewSpecMachineSkuCompetitionService {

    /** 单个 Machine×SKU 的完整只读准入和真实时间轴预演入口。 */
    @Resource
    private NewSpecCandidateAttemptService candidateAttemptService;

    /**
     * 按原 SKU 业务顺序为当前机台查找最佳可排 SKU。
     *
     * <p>每个候选必须先通过反向硬匹配、结构准入和真实时间轴，之后先比较统一Map剩余
     * 目标物理机台缺口，缺口相同再比较匹配等级；缺口和等级均相同不替换，保证原业务
     * 排序靠前的 SKU 获胜。标准S4.5必须扫描完整日期池才能确认最大缺口；关闭缺口优先的
     * 固定指令和辅助入口仍可在形成可提交的“同胎胚”提案后提前结束。</p>
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前机台资源竞争班次
     * @param machineResource 当前机台资源
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 当前运行态下已提交失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排 SKU 对象身份集合
     * @param candidateScope 当前扫描作用域
     * @param roundCache 当前阶段轻量统计缓存
     * @return 当前机台最佳可提交提案；无可排 SKU 时返回 null
     */
    public NewSpecScheduleProposal findBestSkuForMachine(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            List<LocalDate> orderedPoolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode,
            Set<SkuScheduleDTO> pendingSkuIdentitySet,
            Predicate<DailyNewSpecCandidate> candidateScope,
            NewSpecProposalRoundCache roundCache) {
        return this.findBestSkuForMachine(
                context, dayContext, shift, machineResource, orderedPoolDates,
                candidatePoolMap, machineMatch, failedAssignmentKeySet,
                availabilityResolver, actualAvailableTimeMode, pendingSkuIdentitySet,
                candidateScope, roundCache, true);
    }

    /**
     * 按指定竞争口径为当前机台查找最佳可排 SKU。
     *
     * @param context 排程上下文
     * @param dayContext 当前业务日
     * @param shift 当前机台资源竞争班次
     * @param machineResource 当前机台资源
     * @param orderedPoolDates 有序日期池
     * @param candidatePoolMap 日期候选池
     * @param machineMatch 反向硬匹配策略
     * @param failedAssignmentKeySet 当前运行态下已提交失败组合
     * @param availabilityResolver 真实时间轴解析器
     * @param actualAvailableTimeMode 是否按真实可开产时间归班
     * @param pendingSkuIdentitySet 当前仍待排 SKU 对象身份集合
     * @param candidateScope 当前扫描作用域
     * @param roundCache 当前阶段轻量统计缓存
     * @param prioritizeTargetMachineGap true-标准S4.5先补目标机台缺口；false-辅助入口保持原竞争口径
     * @return 当前机台最佳可提交提案；无可排 SKU 时返回 null
     */
    public NewSpecScheduleProposal findBestSkuForMachine(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            List<LocalDate> orderedPoolDates,
            Map<LocalDate, List<DailyNewSpecCandidate>> candidatePoolMap,
            IMachineMatchStrategy machineMatch,
            Set<String> failedAssignmentKeySet,
            NewSpecMachineAvailabilityResolver availabilityResolver,
            boolean actualAvailableTimeMode,
            Set<SkuScheduleDTO> pendingSkuIdentitySet,
            Predicate<DailyNewSpecCandidate> candidateScope,
            NewSpecProposalRoundCache roundCache,
            boolean prioritizeTargetMachineGap) {
        if (Objects.isNull(context) || Objects.isNull(dayContext) || Objects.isNull(shift)
                || Objects.isNull(machineResource) || CollectionUtils.isEmpty(orderedPoolDates)
                || CollectionUtils.isEmpty(candidatePoolMap) || Objects.isNull(machineMatch)
                || Objects.isNull(availabilityResolver) || Objects.isNull(roundCache)) {
            return null;
        }
        Predicate<DailyNewSpecCandidate> effectiveScope = Objects.isNull(candidateScope)
                ? candidate -> true : candidateScope;
        Set<String> normalizedFailureSet = Objects.isNull(failedAssignmentKeySet)
                ? Collections.<String>emptySet() : failedAssignmentKeySet;
        for (LocalDate poolDate : orderedPoolDates) {
            List<DailyNewSpecCandidate> poolCandidates = candidatePoolMap.get(poolDate);
            if (CollectionUtils.isEmpty(poolCandidates)) {
                continue;
            }
            NewSpecScheduleProposal bestProposal = null;
            for (DailyNewSpecCandidate candidate : poolCandidates) {
                if (!effectiveScope.test(candidate)) {
                    continue;
                }
                String executableFailure = this.resolveExecutableFailure(
                        candidate, pendingSkuIdentitySet);
                if (StringUtils.isNotEmpty(executableFailure)) {
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "CANDIDATE_STATE", executableFailure);
                    continue;
                }
                roundCache.recordEvaluatedPair();
                MachineSkuMatchResult matchResult = machineMatch.matchSkuOnMachine(
                        context, machineResource.getMachine(), candidate.getSku());
                if (!matchResult.isMatched()) {
                    String failureReason = StringUtils.defaultIfEmpty(
                            matchResult.getFailureReason(), "机台与SKU硬匹配失败");
                    this.recordCandidateFailureIfAbsent(candidate, failureReason);
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "HARD_MATCH", failureReason);
                    continue;
                }
                String assignmentKey = NewSpecMachineAssignmentPlan.buildAssignmentKey(
                        matchResult, candidate.getSku(), shift.getShiftIndex());
                if (normalizedFailureSet.contains(assignmentKey)) {
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "FAILED_ASSIGNMENT_CACHE",
                            "当前运行态下该机台、SKU和班次已提交失败");
                    continue;
                }
                String eligibilityFailureReason = candidateAttemptService
                        .resolveReadOnlyEligibilityFailure(context, candidate, matchResult);
                if (StringUtils.isNotEmpty(eligibilityFailureReason)) {
                    roundCache.recordEligibilityRejected();
                    this.recordCandidateFailureIfAbsent(candidate, eligibilityFailureReason);
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "READ_ONLY_ELIGIBILITY",
                            eligibilityFailureReason);
                    continue;
                }
                NewSpecMachineAvailabilityPlan availabilityPlan = availabilityResolver.resolve(
                        context, dayContext, candidate, matchResult.getMachine());
                Integer formalTargetShiftIndex = Objects.isNull(availabilityPlan)
                        || Objects.isNull(availabilityPlan.getFormalTargetShift())
                        ? null : availabilityPlan.getFormalTargetShift().getShiftIndex();
                String formalAssignmentKey = NewSpecMachineAssignmentPlan
                        .buildStructureLimitAssignmentKey(
                                matchResult, candidate.getSku(), formalTargetShiftIndex);
                if (Objects.nonNull(formalTargetShiftIndex)
                        && normalizedFailureSet.contains(formalAssignmentKey)) {
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "STRUCTURE_ASSIGNMENT_CACHE",
                            "当前运行态下该机台、SKU和正式班次已被结构准入拒绝");
                    continue;
                }
                StructureMachineLimitDecision structureLimitDecision = candidateAttemptService
                        .resolveStructureMachineLimitDecision(
                                context, dayContext, candidate, matchResult,
                                availabilityPlan, poolDate, roundCache);
                if (Objects.nonNull(structureLimitDecision)
                        && structureLimitDecision.isApplicable()
                        && !structureLimitDecision.isAllowed()) {
                    roundCache.recordEligibilityRejected();
                    this.recordCandidateFailureIfAbsent(
                            candidate, structureLimitDecision.getReason());
                    if (Objects.nonNull(failedAssignmentKeySet)) {
                        failedAssignmentKeySet.add(formalAssignmentKey);
                    }
                    candidateAttemptService.logStructureMachineLimitDecision(
                            "PREVIEW_REJECT", structureLimitDecision, candidate);
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "STRUCTURE_ADMISSION",
                            structureLimitDecision.getReason());
                    continue;
                }
                NewSpecScheduleProposal proposal = candidateAttemptService.previewWithPlan(
                        context, dayContext, shift, machineResource, poolDate, candidate,
                        matchResult, availabilityPlan, actualAvailableTimeMode,
                        structureLimitDecision);
                if (Objects.isNull(proposal)) {
                    roundCache.recordTimelineRejected();
                    String timelineFailure = this.resolveTimelineFailureReason(
                            shift, candidate, availabilityPlan);
                    this.traceMachineSkuDecision(
                            context, dayContext, shift, machineResource,
                            poolDate, candidate, "TIMELINE", timelineFailure);
                    continue;
                }
                this.traceMachineSkuDecision(
                        context, dayContext, shift, machineResource,
                        poolDate, candidate, "PROPOSAL_GENERATED", "已形成完整可执行提案");
                if (Objects.isNull(bestProposal)
                        || this.compareCandidateProposal(
                        proposal, bestProposal, prioritizeTargetMachineGap) < 0) {
                    bestProposal = proposal;
                }
                if (MachineSkuMatchLevel.SAME_EMBRYO
                        == proposal.getMatchResult().getMatchLevel()
                        && !prioritizeTargetMachineGap) {
                    return proposal;
                }
            }
            if (Objects.nonNull(bestProposal)) {
                // 只有当前更早日期池不存在可排提案时，才允许进入后续日期池。
                return bestProposal;
            }
        }
        return null;
    }

    /**
     * 从每台机台的最佳提案中选出当前轮唯一胜出组合。
     *
     * <p>先按声明范围合并重复物理机台，再在存在合法单控试制/量试提案时收窄到单控作用域；
     * 作用域内先补统一Map目标物理机台缺口，再按匹配等级、收尾时间、机台编码决胜。</p>
     *
     * @param context 排程上下文
     * @param machineBestProposalList 每台机台当前最佳提案
     * @param roundCache 当前阶段轻量统计缓存
     * @return 当前轮唯一胜出提案；无提案时返回 null
     */
    public NewSpecScheduleProposal selectRoundWinner(
            LhScheduleContext context,
            List<NewSpecScheduleProposal> machineBestProposalList,
            NewSpecProposalRoundCache roundCache) {
        return this.selectRoundWinner(
                context, machineBestProposalList, roundCache, true);
    }

    /**
     * 从每台机台最佳提案中按指定竞争口径选择当前轮唯一组合。
     *
     * @param context 排程上下文
     * @param machineBestProposalList 每台机台当前最佳提案
     * @param roundCache 当前阶段轻量缓存
     * @param prioritizeTargetMachineGap true-标准S4.5先补目标机台缺口；false-固定指令保持原口径
     * @return 当前轮唯一胜出提案；无提案时返回 null
     */
    public NewSpecScheduleProposal selectRoundWinner(
            LhScheduleContext context,
            List<NewSpecScheduleProposal> machineBestProposalList,
            NewSpecProposalRoundCache roundCache,
            boolean prioritizeTargetMachineGap) {
        if (CollectionUtils.isEmpty(machineBestProposalList)) {
            return null;
        }
        List<NewSpecScheduleProposal> resourceProposalList =
                this.retainBestProposalPerMachineResource(
                        context, machineBestProposalList, prioritizeTargetMachineGap);
        if (Objects.nonNull(roundCache)) {
            roundCache.recordRetainedBestProposalCount(resourceProposalList.size());
        }
        boolean hasSingleControlTrialProposal = resourceProposalList.stream()
                .anyMatch(proposal -> this.isSingleControlTrialOrMassTrialProposal(
                        context, proposal));
        List<NewSpecScheduleProposal> competitionProposalList = hasSingleControlTrialProposal
                ? new ArrayList<NewSpecScheduleProposal>(resourceProposalList.size())
                : resourceProposalList;
        if (hasSingleControlTrialProposal) {
            for (NewSpecScheduleProposal proposal : resourceProposalList) {
                if (this.isSingleControlTrialOrMassTrialProposal(context, proposal)) {
                    competitionProposalList.add(proposal);
                }
            }
        }
        Map<SkuScheduleDTO, NewSpecScheduleProposal> skuWinnerMap =
                new IdentityHashMap<SkuScheduleDTO, NewSpecScheduleProposal>(
                        Math.max(4, competitionProposalList.size() * 2));
        for (NewSpecScheduleProposal proposal : competitionProposalList) {
            SkuScheduleDTO sku = proposal.getCandidate().getSku();
            NewSpecScheduleProposal currentWinner = skuWinnerMap.get(sku);
            if (Objects.isNull(currentWinner)
                    || this.compareProposal(
                    context, proposal, currentWinner, prioritizeTargetMachineGap) < 0) {
                skuWinnerMap.put(sku, proposal);
            }
        }
        return skuWinnerMap.values().stream()
                .min((left, right) -> this.compareProposal(
                        context, left, right, prioritizeTargetMachineGap))
                .orElse(null);
    }

    /**
     * 每个物理机台资源只保留一个最佳提案，避免正规单控 L/R 重复形成整机提案。
     *
     * @param context 排程上下文
     * @param proposalList 原始机台最佳提案
     * @param prioritizeTargetMachineGap 是否优先补统一Map目标物理机台缺口
     * @return 物理资源去重后的提案
     */
    private List<NewSpecScheduleProposal> retainBestProposalPerMachineResource(
            LhScheduleContext context,
            List<NewSpecScheduleProposal> proposalList,
            boolean prioritizeTargetMachineGap) {
        Map<String, NewSpecScheduleProposal> resourceProposalMap =
                new LinkedHashMap<String, NewSpecScheduleProposal>(
                        Math.max(4, proposalList.size() * 2));
        for (NewSpecScheduleProposal proposal : proposalList) {
            if (Objects.isNull(proposal) || Objects.isNull(proposal.getMatchResult())
                    || Objects.isNull(proposal.getMatchResult().getMachine())) {
                continue;
            }
            String resourceKey = this.resolveMachineResourceKey(proposal.getMatchResult());
            NewSpecScheduleProposal currentProposal = resourceProposalMap.get(resourceKey);
            if (Objects.isNull(currentProposal)
                    || this.compareProposal(
                    context, proposal, currentProposal,
                    prioritizeTargetMachineGap) < 0) {
                resourceProposalMap.put(resourceKey, proposal);
            }
        }
        return new ArrayList<NewSpecScheduleProposal>(resourceProposalMap.values());
    }

    /**
     * 解析提案占用的机台资源键。
     *
     * @param matchResult 反向匹配结果
     * @return 单边运行态编码或 L/R 整机物理编码
     */
    private String resolveMachineResourceKey(MachineSkuMatchResult matchResult) {
        MachineScheduleDTO machine = matchResult.getMachine();
        if (matchResult.getDeclaredMachineCodes().size() > 1) {
            return StringUtils.defaultString(
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                            machine.getMachineCode()));
        }
        return StringUtils.defaultString(machine.getMachineCode());
    }

    /**
     * 判断提案是否命中“单控机台 + 试制/量试 SKU”优先作用域。
     *
     * @param context 排程上下文
     * @param proposal 当前机台最佳提案
     * @return true-命中单控试制/量试优先；false-未命中
     */
    private boolean isSingleControlTrialOrMassTrialProposal(
            LhScheduleContext context,
            NewSpecScheduleProposal proposal) {
        if (Objects.isNull(proposal) || Objects.isNull(proposal.getCandidate())
                || Objects.isNull(proposal.getCandidate().getSku())
                || Objects.isNull(proposal.getMatchResult())
                || Objects.isNull(proposal.getMatchResult().getMachine())) {
            return false;
        }
        SkuScheduleDTO sku = proposal.getCandidate().getSku();
        boolean trialOrMassTrial = StringUtils.equals(
                ConstructionStageEnum.TRIAL.getCode(), sku.getConstructionStage())
                || StringUtils.equals(
                ConstructionStageEnum.MASS_TRIAL.getCode(), sku.getConstructionStage());
        return trialOrMassTrial && LhSingleControlMachineUtil.isConfiguredSingleControlMachine(
                context, proposal.getMatchResult().getMachine().getMachineCode());
    }

    /**
     * 比较两个可提交提案的跨机台优先级。
     *
     * @param context 排程上下文
     * @param left 左提案
     * @param right 右提案
     * @return 负数表示左提案优先，正数表示右提案优先
     */
    private int compareProposal(LhScheduleContext context,
                                NewSpecScheduleProposal left,
                                NewSpecScheduleProposal right,
                                boolean prioritizeTargetMachineGap) {
        if (prioritizeTargetMachineGap) {
            int gapCompareResult = this.compareRemainingMachineGap(left, right);
            if (gapCompareResult != 0) {
                return gapCompareResult;
            }
        }
        int compareResult = this.compareMatchLevel(
                left.getMatchResult(), right.getMatchResult());
        if (compareResult != 0) {
            return compareResult;
        }
        Date leftEndingTime = this.resolveCompetitionEndingTime(
                context, left.getMatchResult());
        Date rightEndingTime = this.resolveCompetitionEndingTime(
                context, right.getMatchResult());
        compareResult = Comparator.nullsLast(Date::compareTo)
                .compare(leftEndingTime, rightEndingTime);
        if (compareResult != 0) {
            return compareResult;
        }
        return Comparator.nullsLast(String::compareTo).compare(
                left.getMatchResult().getMachine().getMachineCode(),
                right.getMatchResult().getMachine().getMachineCode());
    }

    /**
     * 比较同一机台上的两个可排 SKU 提案。
     *
     * <p>先补统一Map目标物理机台缺口，再比较Machine-SKU匹配等级。剩余缺口相同时，
     * 继续沿用原匹配等级和业务顺序，避免目标机台数只成为上限而在资源竞争中静默缺台。</p>
     *
     * @param left 左提案
     * @param right 右提案
     * @return 负数表示左提案优先，正数表示右提案优先
     */
    private int compareCandidateProposal(NewSpecScheduleProposal left,
                                         NewSpecScheduleProposal right,
                                         boolean prioritizeTargetMachineGap) {
        if (prioritizeTargetMachineGap) {
            int compareResult = this.compareRemainingMachineGap(left, right);
            if (compareResult != 0) {
                return compareResult;
            }
        }
        return this.compareMatchLevel(left.getMatchResult(), right.getMatchResult());
    }

    /**
     * 按统一Map尚缺物理机台数降序比较提案。
     *
     * @param left 左提案
     * @param right 右提案
     * @return 负数表示左侧缺口更大，正数表示右侧缺口更大
     */
    private int compareRemainingMachineGap(NewSpecScheduleProposal left,
                                           NewSpecScheduleProposal right) {
        int leftGap = Objects.isNull(left) || Objects.isNull(left.getCandidate())
                ? 0 : Math.max(0, left.getCandidate().getRemainingMachineCount());
        int rightGap = Objects.isNull(right) || Objects.isNull(right.getCandidate())
                ? 0 : Math.max(0, right.getCandidate().getRemainingMachineCount());
        return Integer.compare(rightGap, leftGap);
    }

    /**
     * 获取机台竞争使用的真实收尾时间。
     *
     * <p>普通机台和单控单边读取代表机台收尾时间；正规单控整机声明 L/R 两侧时，
     * 必须取两侧较晚时间，避免较早释放侧代表整机提前抢占 SKU。</p>
     *
     * @param context 排程上下文
     * @param matchResult 反向匹配结果
     * @return 当前物理资源竞争收尾时间
     */
    public Date resolveCompetitionEndingTime(LhScheduleContext context,
                                             MachineSkuMatchResult matchResult) {
        if (Objects.isNull(matchResult) || Objects.isNull(matchResult.getMachine())) {
            return null;
        }
        if (matchResult.getDeclaredMachineCodes().size() <= 1
                || Objects.isNull(context)
                || CollectionUtils.isEmpty(context.getMachineScheduleMap())) {
            return matchResult.getMachine().getEstimatedEndTime();
        }
        Date latestEndingTime = null;
        for (String machineCode : matchResult.getDeclaredMachineCodes()) {
            MachineScheduleDTO declaredMachine = context.getMachineScheduleMap().get(machineCode);
            Date endingTime = Objects.isNull(declaredMachine)
                    ? null : declaredMachine.getEstimatedEndTime();
            if (Objects.nonNull(endingTime)
                    && (Objects.isNull(latestEndingTime)
                    || endingTime.after(latestEndingTime))) {
                latestEndingTime = endingTime;
            }
        }
        return Objects.nonNull(latestEndingTime)
                ? latestEndingTime : matchResult.getMachine().getEstimatedEndTime();
    }

    /**
     * 比较两个 Machine-SKU 组合的匹配等级。
     *
     * @param left 左匹配结果
     * @param right 右匹配结果
     * @return 负数表示左侧匹配等级更高，正数表示右侧更高
     */
    private int compareMatchLevel(MachineSkuMatchResult left,
                                  MachineSkuMatchResult right) {
        int leftPriority = Objects.isNull(left.getMatchLevel())
                ? Integer.MAX_VALUE : left.getMatchLevel().getPriority();
        int rightPriority = Objects.isNull(right.getMatchLevel())
                ? Integer.MAX_VALUE : right.getMatchLevel().getPriority();
        return Integer.compare(leftPriority, rightPriority);
    }

    /**
     * 判断候选是否仍有资格进入当前机台扫描。
     *
     * @param candidate 当前日期池候选
     * @param pendingSkuIdentitySet 当前仍待排 SKU 对象身份集合
     * @return true-可进入扫描；false-已阻断、无剩余机会或已出队
     */
    private String resolveExecutableFailure(
            DailyNewSpecCandidate candidate,
            Set<SkuScheduleDTO> pendingSkuIdentitySet) {
        if (Objects.isNull(candidate) || Objects.isNull(candidate.getSku())) {
            return "候选或SKU为空";
        }
        if (candidate.isMachineCompetitionBlocked()) {
            return StringUtils.defaultIfEmpty(
                    candidate.getLastFailure(), "候选被机台无关业务门禁阻断");
        }
        if (candidate.getRemainingMachineCount() <= 0) {
            return candidate.getScheduledMachineCount() >= candidate.getTargetMachineCount()
                    ? "正常需求已满足，剩余机台数为0"
                    : "候选剩余机台数为0";
        }
        if (CollectionUtils.isEmpty(pendingSkuIdentitySet)
                || !pendingSkuIdentitySet.contains(candidate.getSku())) {
            return "SKU已不在当前待排队列";
        }
        return null;
    }

    /**
     * 解析真实时间轴未形成提案的准确原因。
     *
     * @param shift 当前资源班次
     * @param candidate 当前候选
     * @param availabilityPlan 真实时间轴计划
     * @return 时间轴失败原因
     */
    private String resolveTimelineFailureReason(
            LhShiftConfigVO shift,
            DailyNewSpecCandidate candidate,
            NewSpecMachineAvailabilityPlan availabilityPlan) {
        if (Objects.isNull(availabilityPlan)) {
            return "未形成真实可开产时间计划";
        }
        if (StringUtils.isNotEmpty(availabilityPlan.getUnavailableReason())) {
            return availabilityPlan.getUnavailableReason();
        }
        LhShiftConfigVO competitionTargetShift =
                availabilityPlan.getCompetitionTargetShift();
        if (Objects.nonNull(shift) && Objects.nonNull(shift.getShiftIndex())
                && Objects.nonNull(competitionTargetShift)
                && Objects.nonNull(competitionTargetShift.getShiftIndex())
                && !Objects.equals(
                shift.getShiftIndex(), competitionTargetShift.getShiftIndex())) {
            return new StringBuilder(160)
                    .append("真实可开产班次与当前资源班次不一致，currentShift=")
                    .append(shift.getShiftIndex())
                    .append(", competitionTargetShift=")
                    .append(competitionTargetShift.getShiftIndex())
                    .append(", formalTargetShift=")
                    .append(Objects.isNull(availabilityPlan.getFormalTargetShift())
                            ? null : availabilityPlan.getFormalTargetShift().getShiftIndex())
                    .append(", formalAvailableTime=")
                    .append(availabilityPlan.getFormalAvailableProductionTime())
                    .toString();
        }
        if (Objects.nonNull(candidate)
                && StringUtils.isNotEmpty(candidate.getLastFailure())) {
            return candidate.getLastFailure();
        }
        return "真实时间轴未形成当前资源班次可执行提案";
    }

    /**
     * 只登记候选当前运行态的首个失败原因，后续机台试算不得覆盖。
     *
     * @param candidate 当前候选
     * @param failureReason 准确失败原因
     */
    private void recordCandidateFailureIfAbsent(
            DailyNewSpecCandidate candidate,
            String failureReason) {
        if (Objects.nonNull(candidate)
                && StringUtils.isEmpty(candidate.getLastFailure())
                && StringUtils.isNotEmpty(failureReason)) {
            candidate.setLastFailure(failureReason);
        }
    }

    /**
     * 记录Machine×SKU×阶段×日期池×班次的首次决策轨迹。
     *
     * <p>同一组合可能被多个班次和多轮重复试算，运行态只接受首次原因，避免后续机台判断
     * 覆盖首个淘汰点。日志仅读取已计算状态，不重新匹配、预分配模具或查询数据库。</p>
     */
    private void traceMachineSkuDecision(
            LhScheduleContext context,
            DayScheduleContext dayContext,
            LhShiftConfigVO shift,
            MachineResource machineResource,
            LocalDate poolDate,
            DailyNewSpecCandidate candidate,
            String decisionStage,
            String reason) {
        if (Objects.isNull(candidate) || Objects.isNull(candidate.getSku())
                || Objects.isNull(machineResource)
                || Objects.isNull(machineResource.getMachine())) {
            return;
        }
        MachineScheduleDTO machine = machineResource.getMachine();
        String traceKey = new StringBuilder(128)
                .append(machine.getMachineCode()).append('|')
                .append(Objects.isNull(dayContext) ? null : dayContext.getCurrentPhase()).append('|')
                .append(poolDate).append('|')
                .append(Objects.isNull(shift) ? null : shift.getShiftIndex())
                .toString();
        String traceValue = new StringBuilder(96)
                .append(decisionStage).append(": ").append(reason)
                .toString();
        if (!candidate.recordFirstDecisionTrace(traceKey, traceValue)) {
            return;
        }
        String logTemplate = "新增排产Machine-SKU决策轨迹, batchNo: {}, phase: {}, businessDate: {}, "
                        + "workDate: {}, shift: {}, machineCode: {}, machineEndTime: {}, "
                        + "materialCode: {}, productStatus: {}, targetPlanDate: {}, poolDate: {}, "
                        + "runtimeOriginalPoolDate: {}, originalDayPlanQty: {}, dailyQuotaRemaining: {}, "
                        + "futureOnlyEarlyProductionCandidate: {}, alreadyScheduled: {}, bound: {}, "
                        + "targetMachineCount: {}, scheduledMachineCount: {}, remainingMachineCount: {}, "
                        + "decisionStage: {}, reason: {}";
        Object[] logArguments = new Object[]{
                Objects.isNull(context) ? null : context.getBatchNo(),
                Objects.isNull(dayContext) ? null : dayContext.getCurrentPhase(),
                Objects.isNull(dayContext) ? null : dayContext.getScheduleDate(),
                Objects.isNull(shift) ? null : shift.getWorkDate(),
                Objects.isNull(shift) ? null : shift.getShiftIndex(),
                machine.getMachineCode(), machine.getEstimatedEndTime(),
                candidate.getSku().getMaterialCode(), candidate.getSku().getProductStatus(),
                candidate.getTargetPlanDate(), poolDate, candidate.getPoolDate(),
                candidate.getOriginalDayPlanQty(), candidate.getRealtimeDayPlanRemainingQty(),
                candidate.isFutureOnlyEarlyProductionCandidate(),
                candidate.getScheduledMachineCount() > 0, candidate.isBoundOnMachine(),
                candidate.getTargetMachineCount(), candidate.getScheduledMachineCount(),
                candidate.getRemainingMachineCount(), decisionStage, reason};
        if (StringUtils.equals("PROPOSAL_GENERATED", decisionStage)) {
            // 可执行提案数量远多于最终淘汰点，默认仅调试输出，避免正常排程产生海量INFO日志。
            log.debug(logTemplate, logArguments);
        } else {
            log.info(logTemplate, logArguments);
        }
    }
}
