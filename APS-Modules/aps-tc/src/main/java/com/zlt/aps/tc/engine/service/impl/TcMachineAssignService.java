package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.enums.*;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.ITcMachineAssignService;
import com.zlt.aps.tc.engine.strategy.*;
import com.zlt.aps.tc.engine.util.TcGlueSimilarityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧机台分配默认步骤服务。
 *
 * <p>对已预置机台编码的任务直接追加到对应机台任务链；
 * 对未预置机台的任务执行候选机台过滤规则链和评分策略，选择最高分机台分配任务。
 * 全部候选机台被过滤时按过滤原因归类未排原因（产能不足→CAPACITY_NOT_ENOUGH 等）。</p>
 */
@Slf4j
@Service
public class TcMachineAssignService implements ITcMachineAssignService {

    private final TcTaskChainScheduleService taskChainScheduleService;

    private final TcStrategyRegistry strategyRegistry;

    /** 损耗率四层匹配解析器 */
    private final TcLossRateResolver lossRateResolver = new TcLossRateResolver();

    /**
     * 创建默认机台分配步骤服务。
     *
     * @param taskChainScheduleService 任务链排程服务
     * @param strategyRegistry         胎侧策略注册表
     */
    public TcMachineAssignService(TcTaskChainScheduleService taskChainScheduleService,
                                  TcStrategyRegistry strategyRegistry) {
        this.taskChainScheduleService = taskChainScheduleService;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void assign(TcScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        if (context.getCurrentAvailableToolQty() == null) {
            context.setCurrentAvailableToolQty(context.getInitialAvailableToolQty());
        }
        // 零计划和已明确未排的任务保留在上下文中生成解释，但不进入机台过滤、评分和任务链。
        Map<Integer, List<TcTaskDraft>> shiftTaskMap = new ArrayList<>(context.getTaskDraftList()).stream()
                .filter(this::isMachineAssignmentRequired)
                .collect(Collectors.groupingBy(task -> this.normalizeShiftOrder(task.getShiftOrder()),
                        TreeMap::new, Collectors.toList()));
        for (Map.Entry<Integer, List<TcTaskDraft>> entry : shiftTaskMap.entrySet()) {
            List<TcTaskDraft> remainingTaskList = new ArrayList<>(entry.getValue());
            while (CollUtil.isNotEmpty(remainingTaskList)) {
                TcTaskDraft task = this.selectNextTaskByMachinePredecessor(remainingTaskList, context);
                remainingTaskList.remove(task);
                this.assignSingleTask(task, context);
            }
            this.fillCurrentShiftIdleCapacity(entry.getKey(), shiftTaskMap, context);
        }
    }

    /**
     * 执行单个任务的机台分配。
     *
     * @param task 当前待排任务
     * @param context 胎侧排程上下文
     */
    private void assignSingleTask(TcTaskDraft task, TcScheduleContext context) {
        if (task.isUnassigned()) {
            // 未预置机台的任务走完整过滤评分流程
            this.assignByFilterAndScore(task, context);
            return;
        }
        // 已预置机台的任务补齐速度、剩余产能等运行态信息后直接追加到对应机台任务链。
        TcMachineCandidate candidate = this.resolvePresetMachineCandidate(task, context);
        context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(candidate));
        this.addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS, task.getMachineCode(), null, null);
        this.bindSmallGlueMachine(context, task, task.getMachineCode(), null,
                TcScheduleConstants.PRESET_MACHINE_BIND_SOURCE);
        this.taskChainScheduleService.appendAutoTask(task, candidate, context);
    }

    /**
     * 判断任务是否需要进入机台分配。
     *
     * @param task 待排任务草稿
     * @return true 表示任务计划量大于零且尚未明确标记未排
     */
    private boolean isMachineAssignmentRequired(TcTaskDraft task) {
        return task != null && task.getPlanQty() != null
                && task.getPlanQty().compareTo(BigDecimal.ZERO) > 0
                && StrUtil.isBlank(task.getUnplannedReasonCode());
    }

    /**
     * 解析预置机台任务的运行态候选机台信息。
     *
     * <p>预置机台任务不经过完整过滤评分流程，但仍需要沿用候选机台中的生产速度、最大产能和检修时长，
     * 以便后续产能判断、任务链时间计算和解释快照使用同一份机台能力数据。</p>
     *
     * @param task 当前任务
     * @param context 胎侧排程上下文
     * @return 补齐速度和剩余产能后的候选机台
     */
    private TcMachineCandidate resolvePresetMachineCandidate(TcTaskDraft task, TcScheduleContext context) {
        TcMachineCandidate sourceCandidate = this.findCandidateByMachineCode(context.getMachineCandidateList(),
                task.getMachineCode());
        TcMachineCandidate candidate = sourceCandidate == null ? new TcMachineCandidate() : this.copyCandidate(sourceCandidate);
        candidate.setMachineCode(task.getMachineCode());
        BigDecimal machineSpeed = this.resolveMachineSpeed(task, candidate, context);
        BigDecimal remainCapacity = this.resolveRemainCapacity(task, context, candidate, machineSpeed);
        candidate.setMachineSpeed(machineSpeed);
        candidate.setRemainCapacity(remainCapacity);
        task.setMachineSpeed(machineSpeed);
        task.setMachineRemainCapacity(remainCapacity);
        task.setMaintenanceHours(candidate.getMaintenanceHours());
        return candidate;
    }

    /**
     * 根据同机台前置任务连续性选择下一个待排任务。
     *
     * <p>同一班次内每排入一个任务后都会重新计算剩余任务的连续性得分；一班没有当前链尾时使用
     * 前一天同机台链尾，二到六班没有当前链尾时使用上一已排班次链尾。</p>
     *
     * @param remainingTaskList 当前班次尚未分配的任务
     * @param context 胎侧排程上下文
     * @return 本轮应优先分配的任务
     */
    private TcTaskDraft selectNextTaskByMachinePredecessor(List<TcTaskDraft> remainingTaskList,
                                                           TcScheduleContext context) {
        Map<String, TcChainSortScore> scoreMap = new LinkedHashMap<>();
        for (TcTaskDraft task : remainingTaskList) {
            scoreMap.put(task.getBusinessKey(), this.calculateBestChainSortScore(task, context));
        }
        ITcChainTaskPriorityStrategy priorityStrategy = this.resolveChainTaskPriorityStrategy(context);
        TcTaskDraft selectedTask = priorityStrategy.select(remainingTaskList, context, scoreMap);
        log.info("[TC_CHAIN_TASK_ORDER] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, shiftOrder={}, predecessorSnapshot={}, selectedBusinessKey={}, selectedSidewallCode={}, selectedGlueCode={}, strategyCode={}, chainSortScores={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), this.formatScheduleDate(context),
                this.normalizeShiftOrder(selectedTask.getShiftOrder()), this.summarizeMachinePredecessors(context,
                        this.normalizeShiftOrder(selectedTask.getShiftOrder())), selectedTask.getBusinessKey(),
                selectedTask.getSidewallCode(), selectedTask.getGlueCode(), priorityStrategy.getStrategyCode(), scoreMap);
        return selectedTask;
    }

    /**
     * 计算任务在所有候选机台上的最佳链式连续性排序分。
     *
     * @param task 当前任务
     * @param context 胎侧排程上下文
     * @return 最佳排序分
     */
    private ITcChainTaskPriorityStrategy resolveChainTaskPriorityStrategy(TcScheduleContext context) {
        String strategyCode = this.resolveParamValue(context,
                TcScheduleConstants.PARAM_CHAIN_TASK_PRIORITY_STRATEGY,
                TcScheduleStrategyEnum.CONTINUITY_FIRST.getCode());
        if (TcScheduleStrategyEnum.CONTINUITY_FIRST.getCode().equals(strategyCode)) {
            return new TcContinuityFirstChainTaskPriorityStrategy();
        }
        if (TcScheduleStrategyEnum.EMERGENCY_FIRST.getCode().equals(strategyCode)) {
            return new TcEmergencyFirstChainTaskPriorityStrategy();
        }
        throw new ServiceException(TcScheduleErrorCodeEnum.TC_STRATEGY_NOT_REGISTERED.getDefaultMessage()
                + ":" + strategyCode);
    }

    private String resolveParamValue(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        return paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())
                ? defaultValue : paramValue.getEffectiveValue().trim();
    }
    private TcChainSortScore calculateBestChainSortScore(TcTaskDraft task, TcScheduleContext context) {
        if (CollUtil.isEmpty(context.getMachineCandidateList())) {
            return TcChainSortScore.ZERO;
        }
        TcChainSortScore bestScore = TcChainSortScore.ZERO;
        for (TcMachineCandidate candidate : context.getMachineCandidateList()) {
            TcChainSortScore currentScore = this.calculateChainSortScore(task, context, candidate);
            if (currentScore.compareTo(bestScore) > 0) {
                bestScore = currentScore;
            }
        }
        return bestScore;
    }

    /**
     * 计算任务相对单台机台当前链尾的连续性排序分。
     *
     * @param task 当前任务
     * @param context 胎侧排程上下文
     * @param candidate 候选机台
     * @return 排序分
     */
    private TcChainSortScore calculateChainSortScore(TcTaskDraft task, TcScheduleContext context,
                                                     TcMachineCandidate candidate) {
        if (Boolean.FALSE.equals(candidate.getEnabled())) {
            return TcChainSortScore.ZERO;
        }
        TcTaskPredecessor predecessor = this.resolveEffectivePredecessor(context, candidate.getMachineCode(),
                task.getShiftOrder());
        String tailMainGlueCode = predecessor == null ? candidate.getTailMainGlueCode() : predecessor.getGlueCode();
        String tailBaseGlueCode = predecessor == null ? candidate.getTailBaseGlueCode() : predecessor.getBaseGlueCode();
        String tailMouthPlateCode = predecessor == null ? candidate.getTailMouthPlateCode() : predecessor.getMouthPlateCode();
        BigDecimal machineSpeed = this.resolveMachineSpeed(task, candidate, context);
        BigDecimal remainCapacity = this.resolveRemainCapacity(task, context, candidate, machineSpeed);
        BigDecimal capacityScore = this.capacityFitScore(task, remainCapacity);
        boolean mainGlueMatched = TcGlueSimilarityUtils.isSameNonBlank(task.getGlueCode(), tailMainGlueCode);
        int baseGlueMatchedCount = mainGlueMatched ? 0
                : TcGlueSimilarityUtils.calculateIntersectionCount(
                        TcGlueSimilarityUtils.parseCodeSet(task.getBaseGlueCode()),
                        TcGlueSimilarityUtils.parseCodeSet(tailBaseGlueCode));
        boolean mouthPlateMatched = TcGlueSimilarityUtils.isSameNonBlank(
                task.getMouthPlateCode(), tailMouthPlateCode);
        BigDecimal mainGlueScore = mainGlueMatched ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal baseGlueScore = mainGlueMatched ? BigDecimal.ZERO
                : TcGlueSimilarityUtils.calculateSimilarityScore(
                        task.getBaseGlueCode(), tailBaseGlueCode, BigDecimal.valueOf(8));
        BigDecimal mouthPlateScore = mouthPlateMatched ? BigDecimal.TEN : BigDecimal.ZERO;
        BigDecimal switchCostScore = BigDecimal.TEN.subtract(this.nvl(candidate.getSwitchCostHours())).max(BigDecimal.ZERO);
        BigDecimal fixedScore = Boolean.TRUE.equals(candidate.getFixedMachineMatched()) ? BigDecimal.TEN : BigDecimal.ZERO;
        return new TcChainSortScore(mainGlueMatched ? 1 : 0, baseGlueMatchedCount,
                mouthPlateMatched ? 1 : 0, capacityScore, mainGlueScore, baseGlueScore, mouthPlateScore,
                switchCostScore, fixedScore);
    }

    /**
     * 将当前有效前置任务写入候选机台链尾字段，供既有评分策略复用。
     *
     * @param task 当前任务
     * @param context 胎侧排程上下文
     * @param candidate 候选机台
     */
    private void applyEffectivePredecessor(TcTaskDraft task, TcScheduleContext context, TcMachineCandidate candidate) {
        TcTaskPredecessor predecessor = this.resolveEffectivePredecessor(context, candidate.getMachineCode(),
                task.getShiftOrder());
        if (predecessor == null) {
            return;
        }
        candidate.setTailMainGlueCode(predecessor.getGlueCode());
        candidate.setTailBaseGlueCode(predecessor.getBaseGlueCode());
        candidate.setTailMouthPlateCode(predecessor.getMouthPlateCode());
        candidate.getEvidence().put("predecessorSidewallCode", predecessor.getSidewallCode());
        candidate.getEvidence().put("predecessorGlueCode", predecessor.getGlueCode());
        candidate.getEvidence().put("predecessorBaseGlueCode", predecessor.getBaseGlueCode());
        candidate.getEvidence().put("predecessorMouthPlateCode", predecessor.getMouthPlateCode());
        candidate.getEvidence().put("predecessorShiftOrder", predecessor.getShiftOrder());
        candidate.getEvidence().put("predecessorSequence", predecessor.getSequence());
    }

    /**
     * 解析当前任务相对指定机台的有效前置任务。
     *
     * @param context 胎侧排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder 当前任务班次
     * @return 有效前置任务；没有链尾时返回 null
     */
    private TcTaskPredecessor resolveEffectivePredecessor(TcScheduleContext context, String machineCode,
                                                          Integer shiftOrder) {
        Integer normalizedShiftOrder = this.normalizeShiftOrder(shiftOrder);
        TcTaskDraft currentShiftTail = this.findChainTailTask(context, machineCode, normalizedShiftOrder);
        if (currentShiftTail != null) {
            return this.buildPredecessorFromTask(machineCode, normalizedShiftOrder, currentShiftTail);
        }
        for (int previousShiftOrder = normalizedShiftOrder - 1; previousShiftOrder >= 1; previousShiftOrder--) {
            TcTaskDraft previousShiftTail = this.findChainTailTask(context, machineCode, previousShiftOrder);
            if (previousShiftTail != null) {
                return this.buildPredecessorFromTask(machineCode, previousShiftOrder, previousShiftTail);
            }
        }
        return context.getMachinePredecessorMap().get(machineCode);
    }

    /**
     * 查找指定机台班次的任务链尾。
     *
     * @param context 胎侧排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder 班次序号
     * @return 链尾任务；任务链为空时返回 null
     */
    private TcTaskDraft findChainTailTask(TcScheduleContext context, String machineCode, Integer shiftOrder) {
        if (StrUtil.isBlank(machineCode)) {
            return null;
        }
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return null;
        }
        List<ScheduleTaskNode<TcTaskDraft>> nodeList = chain.toList();
        ScheduleTaskNode<TcTaskDraft> tailNode = nodeList.get(nodeList.size() - 1);
        return tailNode == null ? null : tailNode.getTask();
    }

    /**
     * 将任务草稿转换为前置任务快照。
     *
     * @param machineCode 机台编码
     * @param shiftOrder 班次序号
     * @param task 任务草稿
     * @return 前置任务快照
     */
    private TcTaskPredecessor buildPredecessorFromTask(String machineCode, Integer shiftOrder, TcTaskDraft task) {
        TcTaskPredecessor predecessor = new TcTaskPredecessor();
        predecessor.setMachineCode(machineCode);
        predecessor.setSidewallCode(task.getSidewallCode());
        predecessor.setGlueCode(task.getGlueCode());
        predecessor.setBaseGlueCode(task.getBaseGlueCode());
        predecessor.setMouthPlateCode(task.getMouthPlateCode());
        predecessor.setShiftOrder(shiftOrder);
        predecessor.setBusinessKey(task.getBusinessKey());
        return predecessor;
    }

    /**
     * 汇总当前班次各机台有效前置任务，供日志解释排序依据。
     *
     * @param context 胎侧排程上下文
     * @param shiftOrder 班次序号
     * @return 机台前置任务摘要
     */
    private Map<String, String> summarizeMachinePredecessors(TcScheduleContext context, Integer shiftOrder) {
        if (CollUtil.isEmpty(context.getMachineCandidateList())) {
            return Collections.emptyMap();
        }
        Map<String, String> summary = new LinkedHashMap<>();
        for (TcMachineCandidate candidate : context.getMachineCandidateList()) {
            TcTaskPredecessor predecessor = this.resolveEffectivePredecessor(context, candidate.getMachineCode(), shiftOrder);
            if (predecessor == null) {
                summary.put(candidate.getMachineCode(), null);
                continue;
            }
            summary.put(candidate.getMachineCode(), predecessor.getSidewallCode() + "/" + predecessor.getGlueCode()
                    + "/" + predecessor.getMouthPlateCode());
        }
        return summary;
    }

    /**
     * 计算产能适配排序分。
     *
     * @param task 当前任务
     * @param remainCapacity 剩余产能
     * @return 产能适配分
     */
    private BigDecimal capacityFitScore(TcTaskDraft task, BigDecimal remainCapacity) {
        BigDecimal planQty = nvl(task.getPlanQty());
        BigDecimal normalizedRemainCapacity = nvl(remainCapacity);
        if (normalizedRemainCapacity.compareTo(BigDecimal.ZERO) <= 0 || planQty.compareTo(BigDecimal.ZERO) <= 0
                || normalizedRemainCapacity.compareTo(planQty) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal wasteRatio = normalizedRemainCapacity.subtract(planQty)
                .divide(normalizedRemainCapacity, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        java.math.RoundingMode.HALF_UP);
        return BigDecimal.TEN.multiply(BigDecimal.ONE.subtract(wasteRatio))
                .max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 规范化班次序号。
     *
     * @param shiftOrder 原始班次序号
     * @return 1到6之间的班次序号
     */
    private Integer normalizeShiftOrder(Integer shiftOrder) {
        if (shiftOrder == null || shiftOrder < 1) {
            return 1;
        }
        return Math.min(shiftOrder, TcScheduleConstants.TC_MAX_SHIFT_ORDER);
    }

    /**
     * 对未预置机台的任务执行候选机台过滤和评分，选择最高分机台分配任务。
     *
     * @param task    待排任务草稿
     * @param context 胎侧排程上下文
     */
    private void assignByFilterAndScore(TcTaskDraft task, TcScheduleContext context) {
        List<TcMachineCandidate> candidateList = context.getMachineCandidateList();
        if (CollUtil.isEmpty(candidateList)) {
            log.warn("[TC_MACHINE_ASSIGN] 工厂无可用机台候选列表，任务[{}]标记无可用机台", task.getBusinessKey());
            context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.emptyList());
            addAssignTrace(context, task, TcScheduleRuleResultEnum.REJECT, null,
                    TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode(),
                    TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getDesc());
            this.logMachineAssignSummary(context, task, Collections.emptyList(), Collections.emptyList(), null,
                    TcScheduleRuleResultEnum.REJECT, TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE);
            markNoAvailableMachine(task);
            return;
        }

        // 构建机台规则上下文
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);

        // 获取过滤规则和评分策略
        ITcMachineFilterRule filterRule = strategyRegistry.getMachineFilterRule(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        ITcMachineScoreStrategy scoreStrategy = strategyRegistry.getMachineScoreStrategy(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));

        // 复制候选机台列表，并按当前任务动态补齐口型、胶料、定点/禁排和剩余产能判断。
        List<TcMachineCandidate> candidates = copyCandidates(candidateList);
        prepareCandidatesForTask(task, context, candidates);

        // 执行过滤规则链，记录被过滤的候选机台
        List<TcMachineCandidate> passedCandidates = new ArrayList<>();
        for (TcMachineCandidate candidate : candidates) {
            ScheduleRuleResult ruleResult = filterRule.evaluate(candidate, ruleContext);
            if (ruleResult.isPassed()) {
                passedCandidates.add(candidate);
                addFilterTrace(context, task, candidate, TcScheduleRuleResultEnum.PASS, null, null);
            } else {
                addFilterTrace(context, task, candidate, TcScheduleRuleResultEnum.REJECT,
                        ruleResult.getReasonCode(), ruleResult.getReasonDesc());
                log.debug("[TC_MACHINE_ASSIGN] 任务[{}]机台[{}]被过滤，原因={}{}",
                        task.getBusinessKey(), candidate.getMachineCode(),
                        ruleResult.getReasonCode(), ruleResult.getReasonDesc());
            }
        }

        // 全部候选机台被过滤，按过滤原因归类未排原因
        if (passedCandidates.isEmpty()) {
            TcUnplannedReasonEnum unplannedReason = this.resolveUnplannedReasonFromCandidates(candidates);
            log.info("[TC_MACHINE_ASSIGN] 任务[{}]所有候选机台均被过滤，未排原因={}",
                    task.getBusinessKey(), unplannedReason.getCode());
            this.logMachineAssignSummary(context, task, candidates, passedCandidates, null,
                    TcScheduleRuleResultEnum.REJECT, unplannedReason);
            context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);
            this.addSmallGlueContinuousTrace(context, task, this.resolveSmallGlueBoundMachine(task, context), null,
                    false, this.resolveSmallGlueSwitchReason(this.resolveSmallGlueBoundMachine(task, context), candidates));
            this.addAssignTrace(context, task, TcScheduleRuleResultEnum.REJECT, null,
                    unplannedReason.getCode(), unplannedReason.getDesc());
            this.markUnplanned(task, unplannedReason);
            return;
        }

        // 对通过过滤的候选机台执行评分
        for (TcMachineCandidate candidate : passedCandidates) {
            ScheduleScoreResult scoreResult = scoreStrategy.score(candidate, ruleContext);
            addScoreTrace(context, task, candidate, scoreResult);
            log.debug("[TC_MACHINE_ASSIGN] 任务[{}]机台[{}]评分={}",
                    task.getBusinessKey(), candidate.getMachineCode(), scoreResult.getTotalScore());
        }
        context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);

        // 小胶种优先使用已绑定机台；没有绑定或绑定机台不可用时按评分稳定选择。
        String originalSmallGlueMachine = this.resolveSmallGlueBoundMachine(task, context);
        passedCandidates = this.sortCandidatesForSmallGlue(task, context, passedCandidates, null);
        TcMachineCandidate bestCandidate = passedCandidates.get(0);
        this.bindSmallGlueMachine(context, task, bestCandidate.getMachineCode(), originalSmallGlueMachine,
                this.resolveSmallGlueSwitchReason(originalSmallGlueMachine, candidates));
        if (task.getShiftOrder() == null) {
            task.setShiftOrder(1);
        }
        log.info("[TC_MACHINE_ASSIGN] 任务[{}]选中机台[{}]，评分={}",
                task.getBusinessKey(), bestCandidate.getMachineCode(), bestCandidate.getScore());
        this.logMachineAssignSummary(context, task, candidates, passedCandidates, bestCandidate,
                TcScheduleRuleResultEnum.PASS, null);
        addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS,
                bestCandidate.getMachineCode(), null, null);

        // 当前班先由选中机台承接；产能溢出先尝试同班其他机台，工装溢出及同班剩余量再顺延。
        this.appendTaskWithCapacityOverflow(task, bestCandidate, passedCandidates, filterRule, scoreStrategy, context);
    }

    /**
     * 按当前班真实产能分配，并按“同班其他机台优先、其后跨班”承接溢出量。
     *
     * <p>首段先使用本轮评分选中的机台；产能不足量再尝试同班其他候选机台，
     * 同班仍不足时与工装限制溢出量合并顺延，后续班次优先合并同机台同胎侧任务。</p>
     *
     * @param task 当前待排任务
     * @param selectedCandidate 已选中候选机台
     * @param firstShiftCandidates 当前班已通过过滤和评分的候选机台
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @param context 胎侧排程上下文
     */
    private void appendTaskWithCapacityOverflow(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                                List<TcMachineCandidate> firstShiftCandidates,
                                                ITcMachineFilterRule filterRule,
                                                ITcMachineScoreStrategy scoreStrategy,
                                                TcScheduleContext context) {
        BigDecimal originalToolOverflowQty = nvl(task.getToolOverflowQty());
        this.finalizeSelectedTaskPlan(task, selectedCandidate, context);
        BigDecimal currentShiftPlanQty = nvl(task.getPlanQty());
        BigDecimal toolOverflowQty = nvl(task.getToolOverflowQty());
        Integer startShiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        // 工装结算后补打最终态，避免 [TC_MACHINE_ASSIGN_SUMMARY] 在结算前打印 result=PASS 误导排查
        log.info("[TC_MACHINE_ASSIGN_FINAL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, selectedMachineCode={}, planQtyBeforeToolLimit={}, finalPlanQty={}, toolLimitAdjustQty={}, toolOverflowQty={}, availableToolQty={}, assignStatus={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), this.formatScheduleDate(context),
                task.getBusinessKey(), task.getSidewallCode(), task.getShiftOrder(),
                selectedCandidate.getMachineCode(), task.getPlanQtyBeforeToolLimit(), task.getPlanQty(),
                task.getToolLimitAdjustQty(), task.getToolOverflowQty(), task.getAvailableToolQty(),
                StrUtil.isNotBlank(task.getUnplannedReasonCode())
                        ? TcMachineAssignStatusEnum.UNPLANNED.getCode()
                        : TcMachineAssignStatusEnum.ASSIGNED.getCode());
        if (currentShiftPlanQty.compareTo(BigDecimal.ZERO) <= 0 && toolOverflowQty.compareTo(BigDecimal.ZERO) <= 0) {
            this.appendZeroPlanTask(task, selectedCandidate, context, startShiftOrder);
            return;
        }
        if (currentShiftPlanQty.compareTo(BigDecimal.ZERO) <= 0
                && toolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
            // 工装限制将计划量压为零且存在工装溢出：原任务标记产能不足未排并记证据，溢出量继续顺延承接，避免 machineCode 留空形成 null 原因孤儿任务
            this.markUnplanned(task, TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH);
            this.addToolLimitUnplannedTrace(context, task, toolOverflowQty, startShiftOrder);
            this.appendCarryoverQty(task, selectedCandidate, firstShiftCandidates, filterRule, scoreStrategy,
                    context, toolOverflowQty, BigDecimal.ZERO, toolOverflowQty, startShiftOrder);
            return;
        }

        BigDecimal capacityOverflowQty = BigDecimal.ZERO;
        task.setShiftOrder(startShiftOrder);
        if (currentShiftPlanQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal machineSpeed = this.resolveMachineSpeed(task, selectedCandidate, context);
            TcMachineCandidate runtimeCandidate = this.copyCandidate(selectedCandidate);
            runtimeCandidate.setMachineSpeed(machineSpeed);
            runtimeCandidate.setRemainCapacity(this.resolveRemainCapacity(task, context, runtimeCandidate, machineSpeed));
            BigDecimal remainCapacity = nvl(runtimeCandidate.getRemainCapacity());
            BigDecimal assignedQty = currentShiftPlanQty.min(remainCapacity);
            capacityOverflowQty = currentShiftPlanQty.subtract(assignedQty);
            if (assignedQty.compareTo(BigDecimal.ZERO) > 0) {
                this.applyCapacitySplitResult(task, currentShiftPlanQty, assignedQty, remainCapacity, machineSpeed,
                        capacityOverflowQty.compareTo(BigDecimal.ZERO) > 0 ? "当前班产能受限" : "当前班选中机台承接");
                if (originalToolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
                    task.setPlanQty(assignedQty.setScale(0, java.math.RoundingMode.CEILING));
                }
                this.settleAssignedTaskToolState(task, context);
                this.addContextTask(context, task);
                context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                this.addCapacitySplitTrace(context, task, currentShiftPlanQty, assignedQty, capacityOverflowQty,
                        remainCapacity, runtimeCandidate.getMachineCode(), true);
                this.bindSmallGlueMachine(context, task, runtimeCandidate.getMachineCode(),
                        this.resolveSmallGlueBoundMachine(task, context), null);
                this.addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS,
                        runtimeCandidate.getMachineCode(), null, null);
                this.taskChainScheduleService.appendAutoTask(task, runtimeCandidate, context);
            }
        }

        BigDecimal carryoverQty = capacityOverflowQty.add(toolOverflowQty);
        if (carryoverQty.compareTo(BigDecimal.ZERO) > 0) {
            this.appendCarryoverQty(task, selectedCandidate, firstShiftCandidates, filterRule, scoreStrategy, context,
                    carryoverQty, capacityOverflowQty, toolOverflowQty, startShiftOrder);
        }
    }

    /**
     * 当前班原需求排完后，使用后续班次待排需求补足当前班未满产机台。
     *
     * @param currentShiftOrder 当前班次
     * @param shiftTaskMap      按班次分组的待排任务
     * @param context           胎侧排程上下文
     */
    private void fillCurrentShiftIdleCapacity(Integer currentShiftOrder, Map<Integer, List<TcTaskDraft>> shiftTaskMap,
                                             TcScheduleContext context) {
        Integer targetShiftOrder = this.normalizeShiftOrder(currentShiftOrder);
        if (targetShiftOrder >= 6 || CollUtil.isEmpty(context.getMachineCandidateList())) {
            return;
        }
        ITcMachineFilterRule filterRule = strategyRegistry.getMachineFilterRule(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        ITcMachineScoreStrategy scoreStrategy = strategyRegistry.getMachineScoreStrategy(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        int earlyFillIndex = 1;
        boolean hasAssigned = true;
        while (hasAssigned) {
            hasAssigned = false;
            for (TcMachineCandidate machineCandidate : context.getMachineCandidateList()) {
                if (machineCandidate == null || StrUtil.isBlank(machineCandidate.getMachineCode())) {
                    continue;
                }
                TcTaskDraft sourceTask = this.selectFutureEarlyFillTask(targetShiftOrder, shiftTaskMap, context,
                        filterRule, scoreStrategy, machineCandidate.getMachineCode(), earlyFillIndex);
                if (sourceTask == null) {
                    continue;
                }
                Integer sourceShiftOrder = this.normalizeShiftOrder(sourceTask.getShiftOrder());
                TcMachineCandidate runtimeCandidate = this.resolveEarlyFillRuntimeCandidate(sourceTask,
                        targetShiftOrder, sourceShiftOrder, context, filterRule, scoreStrategy,
                        machineCandidate.getMachineCode(), earlyFillIndex);
                if (runtimeCandidate == null || nvl(runtimeCandidate.getRemainCapacity()).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal sourcePlanQty = nvl(sourceTask.getPlanQty());
                BigDecimal assignedQty = sourcePlanQty.min(nvl(runtimeCandidate.getRemainCapacity()));
                if (assignedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TcTaskDraft earlyFillTask = this.copyFutureEarlyFillTask(sourceTask, targetShiftOrder, assignedQty,
                        sourceShiftOrder, earlyFillIndex, runtimeCandidate.getMachineCode());
                this.applyCapacitySplitResult(earlyFillTask, sourcePlanQty, assignedQty,
                        nvl(runtimeCandidate.getRemainCapacity()), nvl(runtimeCandidate.getMachineSpeed()), "后续班次提前补产");
                this.settleAssignedTaskToolState(earlyFillTask, context);
                this.addContextTask(context, earlyFillTask);
                context.getCandidateTraceMap().put(earlyFillTask.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                this.addFutureEarlyFillTrace(context, earlyFillTask, sourceTask, sourceShiftOrder, targetShiftOrder,
                        runtimeCandidate.getMachineCode(), assignedQty, sourcePlanQty.subtract(assignedQty),
                        nvl(runtimeCandidate.getRemainCapacity()), runtimeCandidate.getScore());
            this.addAssignTrace(context, earlyFillTask, TcScheduleRuleResultEnum.PASS,
                    runtimeCandidate.getMachineCode(), null, null);
                this.taskChainScheduleService.appendAutoTask(earlyFillTask, runtimeCandidate, context);
                this.deductFutureTaskPlan(sourceTask, assignedQty, shiftTaskMap, context);
                log.info("[TC_FUTURE_SHIFT_EARLY_FILL] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, sourceBusinessKey={}, targetBusinessKey={}, sourceShiftOrder={}, targetShiftOrder={}, machineCode={}, assignedQty={}, sourceRemainQty={}",
                        context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), this.formatScheduleDate(context),
                        sourceTask.getBusinessKey(), earlyFillTask.getBusinessKey(), sourceShiftOrder, targetShiftOrder,
                        runtimeCandidate.getMachineCode(), assignedQty, nvl(sourceTask.getPlanQty()));
                hasAssigned = true;
                earlyFillIndex++;
            }
        }
    }

    /**
     * 从后续班次中选择最适合提前到当前机台生产的任务。
     *
     * @param targetShiftOrder 目标提前班次
     * @param shiftTaskMap     按班次分组的待排任务
     * @param context          胎侧排程上下文
     * @param filterRule       机台过滤规则
     * @param scoreStrategy    机台评分策略
     * @param machineCode      当前未满产机台
     * @param earlyFillIndex   提前补产序号
     * @return 可提前任务；没有可提前任务时返回 null
     */
    private TcTaskDraft selectFutureEarlyFillTask(Integer targetShiftOrder, Map<Integer, List<TcTaskDraft>> shiftTaskMap,
                                                  TcScheduleContext context, ITcMachineFilterRule filterRule,
                                                  ITcMachineScoreStrategy scoreStrategy, String machineCode,
                                                  int earlyFillIndex) {
        TcTaskDraft bestTask = null;
        BigDecimal bestScore = BigDecimal.ZERO;
        Integer bestSourceShiftOrder = null;
        int bestTaskIndex = Integer.MAX_VALUE;
        for (Map.Entry<Integer, List<TcTaskDraft>> entry : shiftTaskMap.entrySet()) {
            Integer sourceShiftOrder = this.normalizeShiftOrder(entry.getKey());
            if (sourceShiftOrder <= targetShiftOrder || CollUtil.isEmpty(entry.getValue())) {
                continue;
            }
            for (int taskIndex = 0; taskIndex < entry.getValue().size(); taskIndex++) {
                TcTaskDraft sourceTask = entry.getValue().get(taskIndex);
                if (sourceTask == null || nvl(sourceTask.getPlanQty()).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TcMachineCandidate runtimeCandidate = this.resolveEarlyFillRuntimeCandidate(sourceTask,
                        targetShiftOrder, sourceShiftOrder, context, filterRule, scoreStrategy, machineCode,
                        earlyFillIndex);
                if (runtimeCandidate == null || nvl(runtimeCandidate.getRemainCapacity()).compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal score = nvl(runtimeCandidate.getScore());
                if (bestTask == null || score.compareTo(bestScore) > 0
                        || (score.compareTo(bestScore) == 0 && sourceShiftOrder.compareTo(bestSourceShiftOrder) < 0)
                        || (score.compareTo(bestScore) == 0 && sourceShiftOrder.equals(bestSourceShiftOrder)
                        && taskIndex < bestTaskIndex)) {
                    bestTask = sourceTask;
                    bestScore = score;
                    bestSourceShiftOrder = sourceShiftOrder;
                    bestTaskIndex = taskIndex;
                }
            }
        }
        return bestTask;
    }

    /**
     * 按目标班次和机台重新过滤评分后，解析提前补产可用的运行态候选机台。
     *
     * @param sourceTask       后续班次来源任务
     * @param targetShiftOrder 目标提前班次
     * @param sourceShiftOrder 来源班次
     * @param context          胎侧排程上下文
     * @param filterRule       机台过滤规则
     * @param scoreStrategy    机台评分策略
     * @param machineCode      目标机台
     * @param earlyFillIndex   提前补产序号
     * @return 运行态候选机台；不满足过滤或产能时返回 null
     */
    private TcMachineCandidate resolveEarlyFillRuntimeCandidate(TcTaskDraft sourceTask, Integer targetShiftOrder,
                                                               Integer sourceShiftOrder, TcScheduleContext context,
                                                               ITcMachineFilterRule filterRule,
                                                               ITcMachineScoreStrategy scoreStrategy,
                                                               String machineCode, int earlyFillIndex) {
        TcTaskDraft probeTask = this.copyFutureEarlyFillTask(sourceTask, targetShiftOrder, nvl(sourceTask.getPlanQty()),
                sourceShiftOrder, earlyFillIndex, machineCode + "_PROBE");
        List<TcMachineCandidate> candidates = this.buildPassedAndScoredCandidates(probeTask, context,
                filterRule, scoreStrategy);
        TcMachineCandidate candidate = this.findCandidateByMachineCode(candidates, machineCode);
        if (candidate == null) {
            return null;
        }
        BigDecimal machineSpeed = this.resolveMachineSpeed(probeTask, candidate, context);
        TcMachineCandidate runtimeCandidate = this.copyCandidate(candidate);
        runtimeCandidate.setMachineSpeed(machineSpeed);
        runtimeCandidate.setRemainCapacity(this.resolveRemainCapacity(probeTask, context, runtimeCandidate, machineSpeed));
        return runtimeCandidate;
    }

    /**
     * 扣减被提前生产的后续班次原任务，防止重复排产。
     *
     * @param sourceTask  来源任务
     * @param deductedQty 本次提前生产量
     * @param shiftTaskMap 按班次分组的待排任务
     * @param context     胎侧排程上下文
     */
    private void deductFutureTaskPlan(TcTaskDraft sourceTask, BigDecimal deductedQty,
                                      Map<Integer, List<TcTaskDraft>> shiftTaskMap, TcScheduleContext context) {
        BigDecimal remainQty = nvl(sourceTask.getPlanQty()).subtract(nvl(deductedQty)).max(BigDecimal.ZERO);
        sourceTask.setPlanQty(remainQty);
        sourceTask.setPreLossPlanQty(this.deductQty(sourceTask.getPreLossPlanQty(), deductedQty));
        sourceTask.setPlanQtyBeforeToolLimit(this.deductQty(sourceTask.getPlanQtyBeforeToolLimit(), deductedQty));
        sourceTask.setBaseDemandQty(this.deductQty(sourceTask.getBaseDemandQty(), deductedQty));
        sourceTask.setDemandQty(this.deductQty(sourceTask.getDemandQty(), deductedQty));
        sourceTask.setCurrentShiftDemandQty(this.deductQty(sourceTask.getCurrentShiftDemandQty(), deductedQty));
        sourceTask.setGuardDemandQty(this.deductQty(sourceTask.getGuardDemandQty(), deductedQty));
        sourceTask.setCurrentShiftStockGapQty(this.deductQty(sourceTask.getCurrentShiftStockGapQty(), deductedQty));
        sourceTask.setStockGapQty(this.deductQty(sourceTask.getStockGapQty(), deductedQty));
        sourceTask.setPlanStockQty(nvl(sourceTask.getRollingStockQty()).add(nvl(sourceTask.getPlanQty()))
                .subtract(nvl(sourceTask.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        if (remainQty.compareTo(BigDecimal.ZERO) > 0) {
            return;
        }
        for (List<TcTaskDraft> taskList : shiftTaskMap.values()) {
            if (taskList != null) {
                taskList.remove(sourceTask);
            }
        }
        if (context.getTaskDraftList() != null) {
            context.getTaskDraftList().remove(sourceTask);
        }
    }

    /**
     * 扣减数量字段，字段为空时保持为空，避免改变原始缺省语义。
     *
     * @param value       原数量
     * @param deductedQty 扣减量
     * @return 扣减后的非负数量
     */
    private BigDecimal deductQty(BigDecimal value, BigDecimal deductedQty) {
        if (value == null) {
            return null;
        }
        return value.subtract(nvl(deductedQty)).max(BigDecimal.ZERO);
    }

    /**
     * 复制后续班次提前补产任务，保留来源业务属性并切换到目标班次。
     *
     * @param sourceTask       来源任务
     * @param targetShiftOrder 目标提前班次
     * @param planQty          本次提前生产量
     * @param sourceShiftOrder 来源班次
     * @param earlyFillIndex   提前补产序号
     * @param machineCode      目标机台
     * @return 提前补产任务副本
     */
    private TcTaskDraft copyFutureEarlyFillTask(TcTaskDraft sourceTask, Integer targetShiftOrder, BigDecimal planQty,
                                                Integer sourceShiftOrder, int earlyFillIndex, String machineCode) {
        TcTaskDraft target = this.copyOverflowTask(sourceTask, targetShiftOrder, planQty, sourceShiftOrder,
                earlyFillIndex, machineCode);
        target.setBusinessKeySuffix(this.buildFutureEarlyFillBusinessKeySuffix(sourceTask, sourceShiftOrder,
                targetShiftOrder, machineCode, earlyFillIndex));
        target.setCalcFormulaDesc(this.appendFormulaDesc(target.getCalcFormulaDesc(), "后续班次提前补产"));
        return target;
    }

    /**
     * 构建提前补产任务业务键后缀。
     *
     * @param sourceTask       来源任务
     * @param sourceShiftOrder 来源班次
     * @param targetShiftOrder 目标提前班次
     * @param machineCode      目标机台
     * @param earlyFillIndex   提前补产序号
     * @return 业务键后缀
     */
    private String buildFutureEarlyFillBusinessKeySuffix(TcTaskDraft sourceTask, Integer sourceShiftOrder,
                                                         Integer targetShiftOrder, String machineCode,
                                                         int earlyFillIndex) {
        String sourceOrderToken = this.normalizeBusinessKeyToken(sourceTask == null ? null : sourceTask.getOrderNo());
        if (StrUtil.isBlank(sourceOrderToken) && sourceTask != null) {
            sourceOrderToken = this.normalizeBusinessKeyToken(sourceTask.getSourceOrderNos());
        }
        if (StrUtil.isBlank(sourceOrderToken)) {
            sourceOrderToken = TcScheduleConstants.UNKNOWN_CODE;
        }
        return "EARLY_FILL_SRC_" + sourceOrderToken + "_FROM_CLASS" + sourceShiftOrder
                + "_TO_CLASS" + targetShiftOrder + "_" + machineCode + "_" + earlyFillIndex;
    }

    /**
     * 写入后续班次提前补产证据。
     *
     * @param context          排程上下文
     * @param targetTask       提前补产任务
     * @param sourceTask       来源任务
     * @param sourceShiftOrder 来源班次
     * @param targetShiftOrder 目标提前班次
     * @param machineCode      目标机台
     * @param assignedQty      本次提前生产量
     * @param sourceRemainQty  来源任务扣减后剩余量
     * @param remainCapacity   当前机台剩余产能
     * @param selectedScore    选中评分
     */
    private void addFutureEarlyFillTrace(TcScheduleContext context, TcTaskDraft targetTask, TcTaskDraft sourceTask,
                                         Integer sourceShiftOrder, Integer targetShiftOrder, String machineCode,
                                         BigDecimal assignedQty, BigDecimal sourceRemainQty,
                                         BigDecimal remainCapacity, BigDecimal selectedScore) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceShiftOrder", sourceShiftOrder);
        evidence.put("targetShiftOrder", targetShiftOrder);
        evidence.put("sourceTask", sourceTask == null ? null : sourceTask.getBusinessKey());
        evidence.put("targetMachineCode", machineCode);
        evidence.put("assignedQty", assignedQty);
        evidence.put("sourceRemainQty", sourceRemainQty.max(BigDecimal.ZERO));
        evidence.put("remainCapacity", remainCapacity);
        evidence.put("selectedScore", selectedScore);
        evidence.put("priority", "硬约束过滤后按固定/定点、主胶连续、基部胶相似、口型连续、切换成本、产能适配评分排序");
        traceOf(context, targetTask).addRuleHit(TcScheduleRuleCodeEnum.FUTURE_SHIFT_EARLY_FILL,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 先用来源班次其他机台承接产能溢出，再从下一班承接剩余量并优先合并同机台同胎侧任务。
     *
     * @param sourceTask 来源任务
     * @param selectedCandidate 来源任务选中机台
     * @param firstShiftCandidates 来源班次已通过过滤候选机台
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @param context 排程上下文
     * @param carryoverQty 顺延总量
     * @param capacityOverflowQty 产能溢出量
     * @param toolOverflowQty 工装溢出量
     * @param sourceShiftOrder 来源班次
     */
    private void appendCarryoverQty(TcTaskDraft sourceTask, TcMachineCandidate selectedCandidate,
                                    List<TcMachineCandidate> firstShiftCandidates,
                                    ITcMachineFilterRule filterRule, ITcMachineScoreStrategy scoreStrategy,
                                    TcScheduleContext context, BigDecimal carryoverQty,
                                    BigDecimal capacityOverflowQty, BigDecimal toolOverflowQty,
                                    Integer sourceShiftOrder) {
        BigDecimal remainingQty = nvl(carryoverQty);
        BigDecimal sameShiftCapacityQty = nvl(capacityOverflowQty);
        String sourceType = this.resolveCarryoverSourceType(capacityOverflowQty, toolOverflowQty);
        int overflowIndex = 1;
        int firstTargetShiftOrder = sameShiftCapacityQty.compareTo(BigDecimal.ZERO) > 0
                ? sourceShiftOrder : sourceShiftOrder + 1;
        for (int shiftOrder = firstTargetShiftOrder;
             shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER
                     && remainingQty.compareTo(BigDecimal.ZERO) > 0;
             shiftOrder++) {
            BigDecimal shiftProbeQty = shiftOrder == sourceShiftOrder
                    ? remainingQty.min(sameShiftCapacityQty) : remainingQty;
            if (shiftProbeQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            List<TcMachineCandidate> shiftCandidates = this.resolveShiftAssignableCandidates(sourceTask, context,
                    filterRule, scoreStrategy, firstShiftCandidates, selectedCandidate, shiftOrder, shiftProbeQty,
                    sourceShiftOrder, overflowIndex);
            for (TcMachineCandidate candidate : shiftCandidates) {
                if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal candidateAssignableQty = shiftOrder == sourceShiftOrder
                        ? remainingQty.min(sameShiftCapacityQty) : remainingQty;
                if (candidateAssignableQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal machineSpeed = this.resolveMachineSpeed(sourceTask, candidate, context);
                TcMachineCandidate runtimeCandidate = this.copyCandidate(candidate);
                runtimeCandidate.setMachineSpeed(machineSpeed);
                TcTaskDraft capacityProbeTask = this.copyOverflowTask(sourceTask, shiftOrder, candidateAssignableQty,
                        sourceShiftOrder, overflowIndex, candidate.getMachineCode());
                TcTaskDraft mergeTarget = this.findMergeTarget(context, candidate.getMachineCode(), shiftOrder,
                        sourceTask);
                runtimeCandidate.setRemainCapacity(mergeTarget == null
                        ? this.resolvePrependRemainCapacity(capacityProbeTask, context, runtimeCandidate, machineSpeed)
                        : this.resolveRemainCapacityWithoutNewSwitch(capacityProbeTask, context, runtimeCandidate,
                                machineSpeed));
                BigDecimal remainCapacity = nvl(runtimeCandidate.getRemainCapacity());
                if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal beforeAssignQty = remainingQty;
                BigDecimal assignedQty = candidateAssignableQty.min(remainCapacity);
                BigDecimal overflowQty = remainingQty.subtract(assignedQty);
                if (mergeTarget != null) {
                    BigDecimal beforeMergeQty = nvl(mergeTarget.getPlanQty());
                    BigDecimal afterMergeQty = beforeMergeQty.add(assignedQty)
                            .setScale(0, java.math.RoundingMode.CEILING);
                    this.taskChainScheduleService.changeQty(mergeTarget.getBusinessKey(), afterMergeQty, shiftOrder, context);
                    mergeTarget.setPlanQty(afterMergeQty);
                    this.applyCarryoverMergeToolState(mergeTarget, assignedQty, context);
                    context.getCandidateTraceMap().put(mergeTarget.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                    this.addCarryoverTrace(context, mergeTarget, sourceTask, sourceType, assignedQty, sourceShiftOrder,
                            shiftOrder, candidate.getMachineCode(), beforeMergeQty, afterMergeQty, true);
                    this.bindSmallGlueMachine(context, mergeTarget, candidate.getMachineCode(),
                            this.resolveSmallGlueBoundMachine(mergeTarget, context), null);
                    this.logCarryoverSummary(context, mergeTarget, sourceTask, sourceType, assignedQty,
                            capacityOverflowQty, toolOverflowQty, sourceShiftOrder, shiftOrder,
                            candidate.getMachineCode(), beforeMergeQty, afterMergeQty, beforeAssignQty, true);
                this.addAssignTrace(context, mergeTarget, TcScheduleRuleResultEnum.PASS,
                        candidate.getMachineCode(), null, null);
                } else {
                    TcTaskDraft overflowTask = this.copyOverflowTask(sourceTask, shiftOrder, assignedQty,
                            sourceShiftOrder, overflowIndex, candidate.getMachineCode());
                    this.applyCapacitySplitResult(overflowTask, beforeAssignQty, assignedQty, remainCapacity,
                            machineSpeed, "顺延量承接");
                    this.settleAssignedTaskToolState(overflowTask, context);
                    this.addContextTask(context, overflowTask);
                    context.getCandidateTraceMap().put(overflowTask.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                    this.addCapacitySplitTrace(context, overflowTask, beforeAssignQty, assignedQty, overflowQty,
                            remainCapacity, runtimeCandidate.getMachineCode(), false);
                    this.addCarryoverTrace(context, overflowTask, sourceTask, sourceType, assignedQty, sourceShiftOrder,
                            shiftOrder, candidate.getMachineCode(), BigDecimal.ZERO, assignedQty, false);
                    this.logCarryoverSummary(context, overflowTask, sourceTask, sourceType, assignedQty,
                            capacityOverflowQty, toolOverflowQty, sourceShiftOrder, shiftOrder,
                            candidate.getMachineCode(), BigDecimal.ZERO, assignedQty, beforeAssignQty, false);
                    this.bindSmallGlueMachine(context, overflowTask, runtimeCandidate.getMachineCode(),
                            this.resolveSmallGlueBoundMachine(overflowTask, context), null);
            this.addAssignTrace(context, overflowTask, TcScheduleRuleResultEnum.PASS,
                    runtimeCandidate.getMachineCode(), null, null);
                    this.taskChainScheduleService.prependAutoTask(overflowTask, runtimeCandidate, context);
                }
                remainingQty = overflowQty;
                if (shiftOrder == sourceShiftOrder) {
                    sameShiftCapacityQty = sameShiftCapacityQty.subtract(assignedQty).max(BigDecimal.ZERO);
                }
                overflowIndex++;
            }
        }
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            TcTaskDraft unplannedTask = this.copyOverflowTask(sourceTask,
                    TcScheduleConstants.TC_MAX_SHIFT_ORDER, remainingQty, sourceShiftOrder,
                    overflowIndex, TcMachineAssignStatusEnum.UNPLANNED.getCode());
            unplannedTask.setPlanQty(remainingQty.setScale(0, java.math.RoundingMode.CEILING));
            unplannedTask.setShiftOrder(TcScheduleConstants.TC_MAX_SHIFT_ORDER);
            unplannedTask.setMachineCode(null);
            unplannedTask.setUnplannedReasonCode(TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode());
            unplannedTask.setUnplannedReasonDesc(TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
            this.addContextTask(context, unplannedTask);
            this.addCapacityUnplannedTrace(context, unplannedTask, remainingQty);
            this.addCarryoverTrace(context, unplannedTask, sourceTask, sourceType, remainingQty, sourceShiftOrder,
                    TcScheduleConstants.TC_MAX_SHIFT_ORDER, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, false);
            this.logCarryoverSummary(context, unplannedTask, sourceTask, sourceType, remainingQty,
                    capacityOverflowQty, toolOverflowQty, sourceShiftOrder,
                    TcScheduleConstants.TC_MAX_SHIFT_ORDER, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, remainingQty, false);
        this.addAssignTrace(context, unplannedTask, TcScheduleRuleResultEnum.REJECT, null,
                    TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode(),
                    TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
        }
    }


    /**
     * 打印单任务机台分配摘要，info层只保留候选统计、选中结果和产能承接结论。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param candidates 全量候选机台
     * @param passedCandidates 过滤通过候选机台
     * @param selectedCandidate 选中候选机台
     * @param result 分配结果
     * @param unplannedReason 未排原因
     */
    private void logMachineAssignSummary(TcScheduleContext context, TcTaskDraft task,
                                         List<TcMachineCandidate> candidates,
                                         List<TcMachineCandidate> passedCandidates,
                                         TcMachineCandidate selectedCandidate,
                                         TcScheduleRuleResultEnum result,
                                         TcUnplannedReasonEnum unplannedReason) {
        BigDecimal planQty = task == null ? BigDecimal.ZERO : nvl(task.getPlanQty());
        BigDecimal remainCapacity = selectedCandidate == null ? null : nvl(selectedCandidate.getRemainCapacity());
        BigDecimal assignedQty = remainCapacity == null ? null : planQty.min(remainCapacity);
        BigDecimal overflowQty = assignedQty == null ? null : planQty.subtract(assignedQty).max(BigDecimal.ZERO);
        Object scoreItems = selectedCandidate == null || selectedCandidate.getScoreResult() == null
                ? null : selectedCandidate.getScoreResult().getScoreItems();
        log.info("[TC_MACHINE_ASSIGN_SUMMARY] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, businessKey={}, sidewallCode={}, shiftOrder={}, result={}, candidateCount={}, passedCount={}, rejectedCount={}, rejectReasonSummary={}, selectedMachineCode={}, selectedScore={}, scoreItems={}, planQty={}, remainCapacity={}, assignedQty={}, overflowQty={}, reasonCode={}, reasonDesc={}",
                context == null ? null : context.getBatchNo(), context == null ? null : context.getTraceId(),
                context == null ? null : context.getFactoryCode(), formatScheduleDate(context),
                task == null ? null : task.getBusinessKey(), task == null ? null : task.getSidewallCode(),
                task == null ? null : task.getShiftOrder(), result.getCode(),
                candidates == null ? 0 : candidates.size(), passedCandidates == null ? 0 : passedCandidates.size(),
                candidates == null || passedCandidates == null ? 0 : candidates.size() - passedCandidates.size(),
                summarizeRejectReasons(candidates), selectedCandidate == null ? null : selectedCandidate.getMachineCode(),
                selectedCandidate == null ? null : selectedCandidate.getScore(), scoreItems, planQty, remainCapacity,
                assignedQty, overflowQty, unplannedReason == null ? null : unplannedReason.getCode(),
                unplannedReason == null ? null : unplannedReason.getDesc());
    }

    /**
     * 打印顺延、合并和六班后未排摘要，便于还原计划量如何跨班承接。
     *
     * @param context 排程上下文
     * @param targetTask 目标任务
     * @param sourceTask 来源任务
     * @param sourceType 顺延来源类型
     * @param carryoverQty 本次承接或未排数量
     * @param capacityOverflowQty 产能溢出量
     * @param toolOverflowQty 工装溢出量
     * @param sourceShiftOrder 来源班次
     * @param targetShiftOrder 目标班次
     * @param targetMachineCode 目标机台
     * @param beforeMergeQty 合并前计划量
     * @param afterMergeQty 合并后计划量
     * @param remainingQty 本轮处理前剩余量
     * @param merged 是否合并到既有任务
     */
    private void logCarryoverSummary(TcScheduleContext context, TcTaskDraft targetTask, TcTaskDraft sourceTask,
                                     String sourceType, BigDecimal carryoverQty, BigDecimal capacityOverflowQty,
                                     BigDecimal toolOverflowQty, Integer sourceShiftOrder, Integer targetShiftOrder,
                                     String targetMachineCode, BigDecimal beforeMergeQty, BigDecimal afterMergeQty,
                                     BigDecimal remainingQty, boolean merged) {
        log.info("[TC_MACHINE_CARRYOVER] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, sourceBusinessKey={}, targetBusinessKey={}, sourceType={}, sourceShiftOrder={}, targetShiftOrder={}, targetMachineCode={}, carryoverQty={}, capacityOverflowQty={}, toolOverflowQty={}, beforeMergePlanQty={}, afterMergePlanQty={}, remainingQty={}, merged={}, reasonCode={}",
                context == null ? null : context.getBatchNo(), context == null ? null : context.getTraceId(),
                context == null ? null : context.getFactoryCode(), formatScheduleDate(context),
                sourceTask == null ? null : sourceTask.getBusinessKey(), targetTask == null ? null : targetTask.getBusinessKey(),
                sourceType, sourceShiftOrder, targetShiftOrder, targetMachineCode, carryoverQty, capacityOverflowQty,
                toolOverflowQty, beforeMergeQty, afterMergeQty, remainingQty, merged,
                targetTask == null ? null : targetTask.getUnplannedReasonCode());
    }

    /**
     * 汇总候选机台过滤原因。
     *
     * @param candidates 候选机台列表
     * @return 原因编码到数量的映射
     */
    private Map<String, Long> summarizeRejectReasons(List<TcMachineCandidate> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyMap();
        }
        return candidates.stream()
                .filter(TcMachineCandidate::isFiltered)
                .collect(Collectors.groupingBy(candidate -> StrUtil.blankToDefault(candidate.getFilterReasonCode(),
                                TcMachineFilterReasonEnum.UNKNOWN.getCode()),
                        LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * 格式化排程日期，避免日志中直接打印Date对象造成排查口径不统一。
     *
     * @param context 排程上下文
     * @return yyyy-MM-dd格式日期；日期为空时返回null
     */
    private String formatScheduleDate(TcScheduleContext context) {
        return context == null || context.getScheduleDate() == null ? null : DateUtil.formatDate(context.getScheduleDate());
    }

    /**
     * 获取指定班次可承接顺延量的候选机台列表。
     *
     * @param sourceTask 来源任务
     * @param context 排程上下文
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @param firstShiftCandidates 来源班次已评分候选机台
     * @param selectedCandidate 首选机台
     * @param shiftOrder 当前处理班次
     * @param remainingQty 当前剩余待排量
     * @param sourceShiftOrder 来源班次
     * @param overflowIndex 当前拆分序号
     * @return 已排序候选机台列表
     */
    private List<TcMachineCandidate> resolveShiftAssignableCandidates(TcTaskDraft sourceTask, TcScheduleContext context,
                                                                      ITcMachineFilterRule filterRule,
                                                                      ITcMachineScoreStrategy scoreStrategy,
                                                                      List<TcMachineCandidate> firstShiftCandidates,
                                                                      TcMachineCandidate selectedCandidate,
                                                                      Integer shiftOrder,
                                                                      BigDecimal remainingQty,
                                                                      Integer sourceShiftOrder,
                                                                      int overflowIndex) {
        TcTaskDraft probeTask = this.copyOverflowTask(sourceTask, shiftOrder, remainingQty, sourceShiftOrder,
                overflowIndex, TcScheduleConstants.OVERFLOW_PROBE_SUFFIX);
        probeTask.setShiftOrder(shiftOrder);
        probeTask.setPlanQty(remainingQty);
        List<TcMachineCandidate> candidates = this.buildPassedAndScoredCandidates(probeTask, context, filterRule, scoreStrategy);
        if (CollUtil.isEmpty(candidates) && CollUtil.isNotEmpty(firstShiftCandidates)) {
            candidates = this.buildPassedAndScoredCandidates(probeTask, context, filterRule, scoreStrategy);
        }
        return this.sortCandidatesForSmallGlue(probeTask, context, candidates, selectedCandidate);
    }

    /**
     * 重新执行候选机台过滤和评分，用于溢出量进入后续班次后的跨机台承接判断。
     *
     * @param task 当前拆分任务
     * @param context 排程上下文
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @return 通过过滤且已按评分排序的候选机台
     */
    private List<TcMachineCandidate> buildPassedAndScoredCandidates(TcTaskDraft task, TcScheduleContext context,
                                                                    ITcMachineFilterRule filterRule,
                                                                    ITcMachineScoreStrategy scoreStrategy) {
        List<TcMachineCandidate> candidates = copyCandidates(context.getMachineCandidateList());
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        prepareCandidatesForTask(task, context, candidates);
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        List<TcMachineCandidate> passedCandidates = new ArrayList<>();
        for (TcMachineCandidate candidate : candidates) {
            ScheduleRuleResult ruleResult = filterRule.evaluate(candidate, ruleContext);
            if (ruleResult.isPassed()) {
                passedCandidates.add(candidate);
                addFilterTrace(context, task, candidate, TcScheduleRuleResultEnum.PASS, null, null);
            } else {
                addFilterTrace(context, task, candidate, TcScheduleRuleResultEnum.REJECT,
                        ruleResult.getReasonCode(), ruleResult.getReasonDesc());
            }
        }
        for (TcMachineCandidate candidate : passedCandidates) {
            ScheduleScoreResult scoreResult = scoreStrategy.score(candidate, ruleContext);
            addScoreTrace(context, task, candidate, scoreResult);
        }
        context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);
        return this.sortCandidates(passedCandidates);
    }

    /**
     * 将候选机台按评分降序、机台编码升序排序。
     *
     * @param candidates 候选机台
     * @return 排序后的候选机台
     */
    private List<TcMachineCandidate> sortCandidates(List<TcMachineCandidate> candidates) {
        candidates.sort(Comparator
                .comparing(TcMachineCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TcMachineCandidate::getMachineCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return candidates;
    }

    /**
     * 当前班首段固定使用已经选中的机台，其余机台按评分顺序继续承接溢出量。
     *
     * @param candidates 当前班已通过过滤和评分的候选机台
     * @param selectedCandidate 已选中候选机台
     * @return 首选机台排在第一位的候选机台列表
     */
    private List<TcMachineCandidate> sortCandidatesWithSelectedFirst(List<TcMachineCandidate> candidates,
                                                                     TcMachineCandidate selectedCandidate) {
        List<TcMachineCandidate> sortedCandidates = this.sortCandidates(new ArrayList<>(Optional.ofNullable(candidates).orElse(Collections.emptyList())));
        if (selectedCandidate == null || StrUtil.isBlank(selectedCandidate.getMachineCode())) {
            return sortedCandidates;
        }
        sortedCandidates.sort((left, right) -> {
            boolean leftSelected = selectedCandidate.getMachineCode().equals(left.getMachineCode());
            boolean rightSelected = selectedCandidate.getMachineCode().equals(right.getMachineCode());
            if (leftSelected == rightSelected) {
                return 0;
            }
            return leftSelected ? -1 : 1;
        });
        return sortedCandidates;
    }

    /**
     * 在机台确认后结算最终损耗率、工装限制和最终计划量。
     *
     * @param task 当前任务
     * @param selectedCandidate 已选中机台
     * @param context 排程上下文
     */
    private void finalizeSelectedTaskPlan(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                          TcScheduleContext context) {
        BigDecimal preLossPlanQty = task.getPreLossPlanQty() == null ? nvl(task.getPlanQty()) : nvl(task.getPreLossPlanQty());
        preLossPlanQty = preLossPlanQty.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                java.math.RoundingMode.HALF_UP);
        TcLossRuleMatchResult matchResult = this.lossRateResolver.resolve(context.getLossRuleList(), task.getSidewallCode(), selectedCandidate == null ? task.getMachineCode() : selectedCandidate.getMachineCode());
        BigDecimal resolvedLossRate = matchResult == null || matchResult.getLossRate() == null
                ? nvl(task.getLossRate()) : nvl(matchResult.getLossRate());
        BigDecimal lossAddQty = this.calculateLossAddQty(preLossPlanQty, resolvedLossRate);
        BigDecimal planQtyBeforeToolLimit = preLossPlanQty.add(lossAddQty);
        BigDecimal currentAvailableToolQty = this.resolveCurrentAvailableToolQty(context, task);
        BigDecimal originalToolLimitAdjustQty = nvl(task.getToolLimitAdjustQty());
        BigDecimal originalToolOverflowQty = nvl(task.getToolOverflowQty());
        BigDecimal finalPlanQty = planQtyBeforeToolLimit;
        BigDecimal toolLimitAdjustQty = BigDecimal.ZERO;
        BigDecimal toolOverflowQty = BigDecimal.ZERO;
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (currentAvailableToolQty != null && curlLength.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxPlanQty = currentAvailableToolQty.multiply(curlLength);
            if (finalPlanQty.compareTo(maxPlanQty) > 0) {
                finalPlanQty = maxPlanQty.max(BigDecimal.ZERO);
        toolLimitAdjustQty = finalPlanQty.subtract(planQtyBeforeToolLimit)
                .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        java.math.RoundingMode.HALF_UP);
                toolOverflowQty = planQtyBeforeToolLimit.subtract(finalPlanQty).max(BigDecimal.ZERO)
                .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        java.math.RoundingMode.HALF_UP);
            }
        } else if (originalToolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
            toolLimitAdjustQty = originalToolLimitAdjustQty;
            toolOverflowQty = originalToolOverflowQty;
        }
        task.setAvailableToolQty(currentAvailableToolQty);
        task.setResolvedLossRate(resolvedLossRate);
        task.setLossMatchLevel(matchResult == null
                ? (task.getLossRate() == null
                ? TcLossMatchLevelEnum.NONE.getCode() : TcLossMatchLevelEnum.LEGACY_TASK.getCode())
                : matchResult.getMatchLevel());
        task.setLossMatchSource(this.buildLossMatchSource(task, selectedCandidate, matchResult));
        task.setPreLossPlanQty(preLossPlanQty);
        task.setLossAddQty(lossAddQty);
        task.setPlanQtyBeforeToolLimit(planQtyBeforeToolLimit);
        task.setToolLimitAdjustQty(toolLimitAdjustQty);
        task.setToolOverflowQty(toolOverflowQty);
        task.setPlanQty(finalPlanQty);
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(finalPlanQty).subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), "机台确认损耗"));
        if (toolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
            task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), "工装限制"));
        }
    }

    /**
     * 读取当前任务结算前的全局可用工装数量。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @return 全局可用工装数量；未启用工装限制时返回 null
     */
    private BigDecimal resolveCurrentAvailableToolQty(TcScheduleContext context, TcTaskDraft task) {
        if (context != null && context.getCurrentAvailableToolQty() != null) {
            return context.getCurrentAvailableToolQty().setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    java.math.RoundingMode.HALF_UP);
        }
        if (task != null && task.getAvailableToolQty() != null) {
            return task.getAvailableToolQty().setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    java.math.RoundingMode.HALF_UP);
        }
        return null;
    }


    /**
     * 计算损耗补偿量。
     *
     * @param preLossPlanQty 损耗前计划量
     * @param resolvedLossRate 最终损耗率
     * @return 损耗补偿量
     */
    private BigDecimal calculateLossAddQty(BigDecimal preLossPlanQty, BigDecimal resolvedLossRate) {
        if (nvl(preLossPlanQty).compareTo(BigDecimal.ZERO) <= 0 || nvl(resolvedLossRate).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return nvl(preLossPlanQty).multiply(nvl(resolvedLossRate))
                .divide(BigDecimal.valueOf(100), TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        java.math.RoundingMode.HALF_UP);
    }

    /**
     * 在任务实际落到班次机台后结算工装占用和剩余工装。
     *
     * @param task 当前任务
     * @param context 排程上下文
     */
    private void settleAssignedTaskToolState(TcTaskDraft task, TcScheduleContext context) {
        BigDecimal currentAvailableToolQty = this.resolveCurrentAvailableToolQty(context, task);
        if (currentAvailableToolQty == null) {
            return;
        }
        task.setAvailableToolQty(currentAvailableToolQty);
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            task.setToolUsedQty(BigDecimal.ZERO.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    java.math.RoundingMode.HALF_UP));
            task.setRemainingToolQty(currentAvailableToolQty);
            context.setCurrentAvailableToolQty(currentAvailableToolQty);
            return;
        }
        BigDecimal toolUsedQty = nvl(task.getPlanQty()).subtract(nvl(task.getCurrentShiftDemandQty()))
                .divide(curlLength, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                        java.math.RoundingMode.HALF_UP);
        BigDecimal remainingToolQty = currentAvailableToolQty.subtract(toolUsedQty).max(BigDecimal.ZERO);
        if (task.getTotalToolQty() != null) {
            remainingToolQty = remainingToolQty.min(task.getTotalToolQty());
        }
        remainingToolQty = remainingToolQty.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                java.math.RoundingMode.HALF_UP);
        task.setToolUsedQty(toolUsedQty);
        task.setRemainingToolQty(remainingToolQty);
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(nvl(task.getPlanQty()))
                .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        context.setCurrentAvailableToolQty(remainingToolQty);
    }

    /**
     * 顺延量合并到已存在任务后，按增量回写工装状态。
     *
     * @param mergeTarget 被合并任务
     * @param carryoverQty 本次合并顺延量
     * @param context 排程上下文
     */
    private void applyCarryoverMergeToolState(TcTaskDraft mergeTarget, BigDecimal carryoverQty, TcScheduleContext context) {
        BigDecimal currentAvailableToolQty = this.resolveCurrentAvailableToolQty(context, mergeTarget);
        if (currentAvailableToolQty == null) {
            return;
        }
        BigDecimal curlLength = this.resolveCurlLength(mergeTarget);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            mergeTarget.setRemainingToolQty(currentAvailableToolQty);
            context.setCurrentAvailableToolQty(currentAvailableToolQty);
            return;
        }
        BigDecimal incrementalToolQty = nvl(carryoverQty).divide(curlLength,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, java.math.RoundingMode.HALF_UP);
        BigDecimal remainingToolQty = currentAvailableToolQty.subtract(incrementalToolQty).max(BigDecimal.ZERO);
        if (mergeTarget.getTotalToolQty() != null) {
            remainingToolQty = remainingToolQty.min(mergeTarget.getTotalToolQty());
        }
        remainingToolQty = remainingToolQty.setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                java.math.RoundingMode.HALF_UP);
        mergeTarget.setAvailableToolQty(currentAvailableToolQty);
        mergeTarget.setToolUsedQty(nvl(mergeTarget.getToolUsedQty()).add(incrementalToolQty));
        mergeTarget.setRemainingToolQty(remainingToolQty);
        mergeTarget.setPlanStockQty(nvl(mergeTarget.getPlanStockQty()).add(nvl(carryoverQty)));
        context.setCurrentAvailableToolQty(remainingToolQty);
    }

    /**
     * 解析卷曲长度。
     *
     * @param task 当前任务
     * @return 卷曲长度，无法取得时返回 0
     */
    private BigDecimal resolveCurlLength(TcTaskDraft task) {
        if (task.getCurlRollLength() != null && task.getCurlRollLength().compareTo(BigDecimal.ZERO) > 0) {
            return task.getCurlRollLength();
        }
        return nvl(task.getDefaultCurlRollLength());
    }

    /**
     * 构建损耗率命中来源说明。
     *
     * @param task 当前任务
     * @param selectedCandidate 已选中机台
     * @param matchResult 匹配结果
     * @return 命中来源说明
     */
    private String buildLossMatchSource(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                        TcLossRuleMatchResult matchResult) {
        String machineCode = selectedCandidate == null ? task.getMachineCode() : selectedCandidate.getMachineCode();
        if (matchResult == null || matchResult.getMatchedRule() == null) {
            return "machineCode=" + machineCode + ",sidewallCode=" + task.getSidewallCode();
        }
        TcLossRule matchedRule = matchResult.getMatchedRule();
        return "machineCode=" + machineCode + ",sidewallCode=" + task.getSidewallCode()
                + ",ruleMachineCode=" + matchedRule.getMachineCode()
                + ",ruleSidewallCode=" + matchedRule.getSidewallCode()
                + ",priority=" + matchedRule.getPriority();
    }

    /**
     * 记录零计划量任务的评估结果。
     *
     * <p>零计划量表示当前任务无需占用机台产能，只保留候选机台、评分和容量证据，
     * 不追加到任务链，避免生成全 0 的排程结果行。</p>
     *
     * @param task              当前零计划量任务
     * @param selectedCandidate 已选中候选机台
     * @param context           胎侧排程上下文
     * @param shiftOrder        当前任务班次
     */
    private void appendZeroPlanTask(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                    TcScheduleContext context, Integer shiftOrder) {
        task.setShiftOrder(shiftOrder);
        BigDecimal machineSpeed = this.resolveMachineSpeed(task, selectedCandidate, context);
        TcMachineCandidate runtimeCandidate = this.copyCandidate(selectedCandidate);
        runtimeCandidate.setMachineSpeed(machineSpeed);
        runtimeCandidate.setRemainCapacity(this.resolveRemainCapacity(task, context, runtimeCandidate, machineSpeed));
        task.setMachineCode(runtimeCandidate.getMachineCode());
        task.setMachineRemainCapacity(runtimeCandidate.getRemainCapacity());
        task.setMachineSpeed(machineSpeed);
        context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(runtimeCandidate));
        this.addCapacitySplitTrace(context, task, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                nvl(runtimeCandidate.getRemainCapacity()), runtimeCandidate.getMachineCode(), true);
        this.addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS,
                runtimeCandidate.getMachineCode(), null, null);
    }

    /**
     * 复制产能溢出顺延任务，保留原任务业务属性并追加业务键后缀。
     *
     * @param source        来源任务
     * @param shiftOrder    顺延后的班次
     * @param planQty       顺延计划量
     * @param sourceShift   来源班次
     * @param overflowIndex 顺延序号
     * @return 顺延任务副本
     */
    private TcTaskDraft copyOverflowTask(TcTaskDraft source, Integer shiftOrder, BigDecimal planQty,
                                         Integer sourceShift, int overflowIndex, String machineCode) {
        TcTaskDraft target = new TcTaskDraft();
        target.setOrderNo(source.getOrderNo());
        target.setSourceOrderNos(source.getSourceOrderNos());
        target.setSidewallCode(source.getSidewallCode());
        target.setConstructionVersion(source.getConstructionVersion());
        target.setSidewallCraft(source.getSidewallCraft());
        target.setSidewallWeight(source.getSidewallWeight());
        target.setSidewallWearpRubberWeight(source.getSidewallWearpRubberWeight());
        target.setMonthSurplusQty(source.getMonthSurplusQty());
        target.setGlueCode(source.getGlueCode());
        target.setBaseGlueCode(source.getBaseGlueCode());
        target.setMouthPlateCode(source.getMouthPlateCode());
        target.setShiftOrder(shiftOrder);
        target.setCurrentShiftDemandQty(BigDecimal.ZERO);
        target.setGuardDemandQty(BigDecimal.ZERO);
        target.setRollingStockQty(nvl(source.getPlanStockQty()));
        target.setSixClockStockQty(source.getSixClockStockQty());
        target.setGuardShiftCount(source.getGuardShiftCount());
        target.setGuardRangeHours(source.getGuardRangeHours());
        target.setSupplyHours(source.getSupplyHours());
        target.setCurrentShiftStockGapQty(BigDecimal.ZERO);
        target.setStockGapQty(BigDecimal.ZERO);
        target.setStockDeductQty(BigDecimal.ZERO);
        target.setPlanStockQty(nvl(source.getPlanStockQty()).add(nvl(planQty)));
        target.setPlanQty(planQty);
        target.setSidewallLength(source.getSidewallLength());
        target.setTailFlag(source.getTailFlag());
        target.setTailBalanceQty(source.getTailBalanceQty());
        target.setLossRate(source.getLossRate());
        target.setResolvedLossRate(source.getResolvedLossRate());
        target.setLossMatchLevel(source.getLossMatchLevel());
        target.setLossMatchSource(source.getLossMatchSource());
        target.setPreLossPlanQty(nvl(planQty));
        target.setPlanQtyBeforeToolLimit(nvl(planQty));
        target.setBaseDemandQty(nvl(planQty));
        target.setLossAddQty(BigDecimal.ZERO);
        target.setToolLimitAdjustQty(BigDecimal.ZERO);
        target.setToolOverflowQty(BigDecimal.ZERO);
        target.setMinStartAdjustQty(source.getMinStartAdjustQty());
        target.setTailRoundAdjustQty(source.getTailRoundAdjustQty());
        target.setCalcFormulaDesc(source.getCalcFormulaDesc());
        target.setTotalToolQty(source.getTotalToolQty());
        target.setCurlRollLength(source.getCurlRollLength());
        target.setDefaultCurlRollLength(source.getDefaultCurlRollLength());
        target.setMinStartQty(source.getMinStartQty());
        target.setPreviousSpecSwitchHours(source.getPreviousSpecSwitchHours());
        target.setPreviousGlueSwitchHours(source.getPreviousGlueSwitchHours());
        target.setPreviousGlueSwitchCapacityDeduct(source.getPreviousGlueSwitchCapacityDeduct());
        target.setFixedMachineMatched(source.getFixedMachineMatched());
        target.setDemandQty(source.getDemandQty());
        target.setNewSpecInfo(source.getNewSpecInfo());
        target.setExperimentSpecInfo(source.getExperimentSpecInfo());
        target.setSmallGlueFlag(source.getSmallGlueFlag());
        target.setBusinessKeySuffix(this.buildOverflowBusinessKeySuffix(source, sourceShift, shiftOrder, machineCode, overflowIndex));
        return target;
    }

    /**
     * 构建顺延任务业务键后缀，来源工单参与唯一性，避免同规格同班次多个未排副本冲突。
     *
     * @param source        来源任务
     * @param sourceShift   来源班次
     * @param shiftOrder    顺延后的班次
     * @param machineCode   承接机台编码或未排标识
     * @param overflowIndex 顺延序号
     * @return 顺延任务业务键后缀
     */
    private String buildOverflowBusinessKeySuffix(TcTaskDraft source, Integer sourceShift, Integer shiftOrder,
                                                  String machineCode, int overflowIndex) {
        String sourceOrderToken = this.normalizeBusinessKeyToken(source == null ? null : source.getOrderNo());
        if (StrUtil.isBlank(sourceOrderToken) && source != null) {
            sourceOrderToken = this.normalizeBusinessKeyToken(source.getSourceOrderNos());
        }
        if (StrUtil.isBlank(sourceOrderToken)) {
            sourceOrderToken = TcScheduleConstants.UNKNOWN_CODE;
        }
        return TcScheduleConstants.CAPACITY_OVERFLOW_BUSINESS_KEY_PREFIX + sourceOrderToken + "_FROM_CLASS" + sourceShift
                + "_TO_CLASS" + shiftOrder + "_" + machineCode + "_" + overflowIndex;
    }

    /**
     * 标准化业务键片段，保留可读前缀并追加哈希，控制长度且降低不同来源工单冲突概率。
     *
     * @param value 原始业务值
     * @return 可用于业务键后缀的片段
     */
    private String normalizeBusinessKeyToken(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalizedValue = value.trim().replaceAll("[^A-Za-z0-9]", "_");
        String readablePrefix = normalizedValue.length() > 24 ? normalizedValue.substring(0, 24) : normalizedValue;
        return readablePrefix + "_" + Integer.toHexString(value.hashCode());
    }

    /**
     * 回填产能拆分后的任务计划量和解释字段。
     *
     * @param task            当前任务
     * @param beforeAssignQty 拆分前待排量
     * @param assignedQty     本班实际分配量
     * @param remainCapacity  本班分配前剩余产能
     * @param machineSpeed    机台速度
     * @param splitDesc       拆分说明
     */
    private void applyCapacitySplitResult(TcTaskDraft task, BigDecimal beforeAssignQty, BigDecimal assignedQty,
                                          BigDecimal remainCapacity, BigDecimal machineSpeed, String splitDesc) {
        BigDecimal normalizedAssignedQty = StrUtil.contains(splitDesc, "顺延") ? assignedQty.setScale(0, java.math.RoundingMode.CEILING) : assignedQty;
        task.setPlanQty(normalizedAssignedQty);
        task.setMachineRemainCapacity(remainCapacity);
        task.setMachineSpeed(machineSpeed);
        task.setCapacityAdjustQty(nvl(task.getCapacityAdjustQty()).add(normalizedAssignedQty.subtract(nvl(beforeAssignQty))));
        task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), splitDesc));
    }

    /**
     * 将任务加入上下文任务列表，确保顺延任务能生成解释和落库记录。
     *
     * @param context 排程上下文
     * @param task    任务
     */
    private void addContextTask(TcScheduleContext context, TcTaskDraft task) {
        if (context.getTaskDraftList().contains(task)) {
            return;
        }
        try {
            context.getTaskDraftList().add(task);
        } catch (UnsupportedOperationException ex) {
            List<TcTaskDraft> mutableList = new ArrayList<>(context.getTaskDraftList());
            mutableList.add(task);
            context.setTaskDraftList(mutableList);
        }
    }

    /**
     * 查找目标机台班次内可合并的同胎侧任务。
     *
     * @param context 排程上下文
     * @param machineCode 目标机台编码
     * @param shiftOrder 目标班次
     * @param sourceTask 来源任务
     * @return 可合并任务；不存在时返回 null
     */
    private TcTaskDraft findMergeTarget(TcScheduleContext context, String machineCode, Integer shiftOrder,
                                        TcTaskDraft sourceTask) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return null;
        }
        for (ScheduleTaskNode<TcTaskDraft> node : chain.toList()) {
            TcTaskDraft targetTask = node.getTask();
            if (targetTask != null
                    && Objects.equals(targetTask.getSidewallCode(), sourceTask.getSidewallCode())
                    && Objects.equals(targetTask.getGlueCode(), sourceTask.getGlueCode())
                    && Objects.equals(targetTask.getMouthPlateCode(), sourceTask.getMouthPlateCode())) {
                return targetTask;
            }
        }
        return null;
    }

    /**
     * 解析顺延来源类型。
     *
     * @param capacityOverflowQty 产能溢出量
     * @param toolOverflowQty 工装溢出量
     * @return 来源类型编码
     */
    private String resolveCarryoverSourceType(BigDecimal capacityOverflowQty, BigDecimal toolOverflowQty) {
        boolean capacityOverflow = nvl(capacityOverflowQty).compareTo(BigDecimal.ZERO) > 0;
        boolean toolOverflow = nvl(toolOverflowQty).compareTo(BigDecimal.ZERO) > 0;
        if (capacityOverflow && toolOverflow) {
            return TcScheduleConstants.CARRYOVER_SOURCE_CAPACITY_LIMIT + ","
                    + TcScheduleConstants.CARRYOVER_SOURCE_TOOL_LIMIT;
        }
        if (toolOverflow) {
            return TcScheduleConstants.CARRYOVER_SOURCE_TOOL_LIMIT;
        }
        return TcScheduleConstants.CARRYOVER_SOURCE_CAPACITY_LIMIT;
    }

    /**
     * 写入统一顺延承接证据。
     *
     * @param context 排程上下文
     * @param targetTask 目标任务
     * @param sourceTask 来源任务
     * @param sourceType 来源类型
     * @param carryoverQty 本次承接顺延量
     * @param sourceShiftOrder 来源班次
     * @param targetShiftOrder 目标班次
     * @param targetMachineCode 目标机台
     * @param beforeMergeQty 合并前计划量
     * @param afterMergeQty 合并后计划量
     * @param merged 是否合并既有任务
     */
    private void addCarryoverTrace(TcScheduleContext context, TcTaskDraft targetTask, TcTaskDraft sourceTask,
                                   String sourceType, BigDecimal carryoverQty, Integer sourceShiftOrder,
                                   Integer targetShiftOrder, String targetMachineCode, BigDecimal beforeMergeQty,
                                   BigDecimal afterMergeQty, boolean merged) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("sourceType", sourceType);
        evidence.put("sourceShiftOrder", sourceShiftOrder);
        evidence.put("sourceTask", sourceTask == null ? null : sourceTask.getBusinessKey());
        evidence.put("carryoverQty", carryoverQty);
        evidence.put("targetShiftOrder", targetShiftOrder);
        evidence.put("targetMachineCode", targetMachineCode);
        evidence.put("merged", merged);
        evidence.put("beforeMergePlanQty", beforeMergeQty);
        evidence.put("afterMergePlanQty", afterMergeQty);
        traceOf(context, targetTask).addRuleHit(TcScheduleRuleCodeEnum.PLAN_QTY_CARRYOVER,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 写入产能拆分证据。
     *
     * @param context         排程上下文
     * @param task            当前任务
     * @param beforeAssignQty 拆分前待排量
     * @param assignedQty     本班实际分配量
     * @param overflowQty     顺延量
     * @param remainCapacity  本班分配前剩余产能
     */
    private void addCapacitySplitTrace(TcScheduleContext context, TcTaskDraft task, BigDecimal beforeAssignQty,
                                       BigDecimal assignedQty, BigDecimal overflowQty, BigDecimal remainCapacity,
                                       String machineCode, boolean sameShift) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("machineCode", machineCode);
        evidence.put("splitType", sameShift ? TcScheduleConstants.SPLIT_TYPE_SAME_SHIFT_MATCHED_MACHINE
                : TcScheduleConstants.SPLIT_TYPE_NEXT_SHIFT_MATCHED_MACHINE);
        evidence.put("beforeAssignQty", beforeAssignQty);
        evidence.put("assignedQty", assignedQty);
        evidence.put("overflowQty", overflowQty);
        evidence.put("remainCapacity", remainCapacity);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.CAPACITY_OVERFLOW_SPLIT,
                overflowQty.compareTo(BigDecimal.ZERO) > 0
                        ? TcScheduleRuleResultEnum.SPLIT : TcScheduleRuleResultEnum.PASS,
                evidence);
        this.addNewSpecAdvanceResultTrace(context, task, assignedQty, overflowQty, machineCode);
    }

    /**
     * 写入新规格提前排产后的机台分配结果证据。
     *
     * @param context     排程上下文
     * @param task        当前任务
     * @param assignedQty 本班实际分配量
     * @param overflowQty 顺延量
     * @param machineCode 选中机台编码
     */
    private void addNewSpecAdvanceResultTrace(TcScheduleContext context, TcTaskDraft task, BigDecimal assignedQty,
                                              BigDecimal overflowQty, String machineCode) {
        TcNewSpecInfo newSpecInfo = task.getNewSpecInfo();
        if (newSpecInfo == null || !newSpecInfo.isNewSpecHit()) {
            return;
        }
        List<Integer> adjustedTargetWindow = newSpecInfo.getAdjustedTargetWindow();
        boolean advancedWindowHit = adjustedTargetWindow != null && adjustedTargetWindow.contains(task.getShiftOrder());
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("machineCode", machineCode);
        evidence.put("normalTargetShift", newSpecInfo.getNormalTargetShift());
        evidence.put("adjustedTargetShift", newSpecInfo.getAdjustedTargetShift());
        evidence.put("adjustedTargetWindow", adjustedTargetWindow);
        evidence.put("advancedWindowHit", advancedWindowHit);
        evidence.put("assignedQty", assignedQty);
        evidence.put("overflowQty", overflowQty);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.NEW_SPEC_ADVANCE_RESULT,
                advancedWindowHit ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.ROLLING,
                evidence);
    }

    /**
     * 写入超过六班仍无法排完的未排证据。
     *
     * @param context      排程上下文
     * @param task         未排任务
     * @param remainingQty 六班后剩余未排量
     */
    private void addCapacityUnplannedTrace(TcScheduleContext context, TcTaskDraft task, BigDecimal remainingQty) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("remainingQty", remainingQty);
        evidence.put("reasonCode", TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode());
        evidence.put("reasonDesc", TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.CAPACITY_OVERFLOW_UNPLANNED,
                TcScheduleRuleResultEnum.REJECT, evidence);
    }

    /**
     * 写入工装限制压零未排证据。
     *
     * <p>当全局工装池耗尽导致 {@code finalizeSelectedTaskPlan} 将计划量压为零、且存在工装溢出顺延时，
     * 原任务无法在当前班次占用机台产能，标记产能不足未排并记录工装证据，便于后续追溯工装耗尽与顺延目标。</p>
     *
     * @param context           排程上下文
     * @param task              当前未排任务
     * @param toolOverflowQty   工装溢出顺延量
     * @param sourceShiftOrder  来源班次
     */
    private void addToolLimitUnplannedTrace(TcScheduleContext context, TcTaskDraft task,
                                            BigDecimal toolOverflowQty, Integer sourceShiftOrder) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("reasonCode", TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode());
        evidence.put("reasonDesc", TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
        evidence.put("sourceType", TcScheduleConstants.CARRYOVER_SOURCE_TOOL_LIMIT);
        evidence.put("sourceShiftOrder", sourceShiftOrder);
        evidence.put("toolOverflowQty", toolOverflowQty);
        evidence.put("availableToolQty", task.getAvailableToolQty());
        evidence.put("planQtyBeforeToolLimit", task.getPlanQtyBeforeToolLimit());
        evidence.put("carryoverTargetShiftFrom", sourceShiftOrder == null ? null : sourceShiftOrder + 1);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.TOOL_LIMIT_UNPLANNED,
                TcScheduleRuleResultEnum.REJECT, evidence);
    }

    /**
     * 追加计划量公式说明。
     *
     * @param current  当前说明
     * @param addition 新增说明
     * @return 合并后的说明
     */
    private String appendFormulaDesc(String current, String addition) {
        if (StrUtil.isBlank(current)) {
            return addition;
        }
        if (current.contains(addition)) {
            return current;
        }
        return current + "->" + addition;
    }

    /**
     * 判断任务是否启用小胶种连续生产规则。
     *
     * @param task 任务草稿
     * @return true 表示当前任务是小胶种任务
     */
    private boolean isSmallGlueTask(TcTaskDraft task) {
        return task != null && Boolean.TRUE.equals(task.getSmallGlueFlag()) && StrUtil.isNotBlank(task.getGlueCode());
    }

    /**
     * 读取小胶种当前绑定机台。
     *
     * @param task 任务草稿
     * @param context 排程上下文
     * @return 已绑定机台；未命中小胶种或尚未绑定时返回 null
     */
    private String resolveSmallGlueBoundMachine(TcTaskDraft task, TcScheduleContext context) {
        if (!this.isSmallGlueTask(task) || context == null || context.getSmallGlueMachineMap() == null) {
            return null;
        }
        return context.getSmallGlueMachineMap().get(task.getGlueCode());
    }

    /**
     * 按小胶种绑定规则排序候选机台。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @param candidates 已通过过滤和评分的候选机台
     * @param selectedCandidate 原首选机台，非小胶种顺延时保持当前班选中机台优先
     * @return 排序后的候选机台
     */
    private List<TcMachineCandidate> sortCandidatesForSmallGlue(TcTaskDraft task, TcScheduleContext context,
                                                                List<TcMachineCandidate> candidates,
                                                                TcMachineCandidate selectedCandidate) {
        List<TcMachineCandidate> sortedCandidates = this.sortCandidates(new ArrayList<>(Optional.ofNullable(candidates).orElse(Collections.emptyList())));
        String boundMachineCode = this.resolveSmallGlueBoundMachine(task, context);
        if (StrUtil.isNotBlank(boundMachineCode)) {
            TcMachineCandidate boundCandidate = this.findCandidateByMachineCode(sortedCandidates, boundMachineCode);
            if (boundCandidate != null) {
                sortedCandidates.remove(boundCandidate);
                sortedCandidates.add(0, boundCandidate);
                return sortedCandidates;
            }
        }
        if (!this.isSmallGlueTask(task) && selectedCandidate != null) {
            return this.sortCandidatesWithSelectedFirst(sortedCandidates, selectedCandidate);
        }
        return sortedCandidates;
    }

    /**
     * 绑定或刷新小胶种生产机台。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param selectedMachineCode 实际选中机台
     * @param originalMachineCode 原绑定机台
     * @param switchReason 切换原因
     */
    private void bindSmallGlueMachine(TcScheduleContext context, TcTaskDraft task, String selectedMachineCode,
                                      String originalMachineCode, String switchReason) {
        if (!this.isSmallGlueTask(task) || context == null || StrUtil.isBlank(selectedMachineCode)) {
            return;
        }
        String previousMachineCode = StrUtil.blankToDefault(originalMachineCode,
                context.getSmallGlueMachineMap().get(task.getGlueCode()));
        context.getSmallGlueMachineMap().put(task.getGlueCode(), selectedMachineCode);
        this.addSmallGlueContinuousTrace(context, task, previousMachineCode, selectedMachineCode,
                StrUtil.isNotBlank(previousMachineCode) && !previousMachineCode.equals(selectedMachineCode), switchReason);
    }

    /**
     * 查找候选机台。
     *
     * @param candidates 候选机台列表
     * @param machineCode 机台编码
     * @return 候选机台；不存在时返回 null
     */
    private TcMachineCandidate findCandidateByMachineCode(List<TcMachineCandidate> candidates, String machineCode) {
        if (CollUtil.isEmpty(candidates) || StrUtil.isBlank(machineCode)) {
            return null;
        }
        for (TcMachineCandidate candidate : candidates) {
            if (machineCode.equals(candidate.getMachineCode())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 解析小胶种绑定机台切换原因。
     *
     * @param boundMachineCode 原绑定机台
     * @param candidates 当前任务全部候选机台
     * @return 切换原因；没有绑定或绑定机台仍可用时返回 null
     */
    private String resolveSmallGlueSwitchReason(String boundMachineCode, List<TcMachineCandidate> candidates) {
        if (StrUtil.isBlank(boundMachineCode)) {
            return null;
        }
        TcMachineCandidate boundCandidate = this.findCandidateByMachineCode(candidates, boundMachineCode);
        if (boundCandidate == null) {
            return TcScheduleConstants.SMALL_GLUE_BOUND_MACHINE_NOT_FOUND;
        }
        if (boundCandidate.isFiltered()) {
            return boundCandidate.getFilterReasonCode();
        }
        if (nvl(boundCandidate.getRemainCapacity()).compareTo(BigDecimal.ZERO) <= 0) {
            return TcMachineFilterReasonEnum.NO_REMAIN_CAPACITY.getCode();
        }
        return null;
    }

    /**
     * 写入小胶种连续生产证据。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @param originalMachineCode 原绑定机台
     * @param selectedMachineCode 当前选中机台
     * @param switched 是否发生切换
     * @param switchReason 切换原因
     */
    private void addSmallGlueContinuousTrace(TcScheduleContext context, TcTaskDraft task, String originalMachineCode,
                                             String selectedMachineCode, boolean switched, String switchReason) {
        if (!this.isSmallGlueTask(task)) {
            return;
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("paramCode", TcScheduleConstants.PARAM_SMALL_GLUE_CODES);
        evidence.put("glueCode", task.getGlueCode());
        evidence.put("originalMachineCode", originalMachineCode);
        evidence.put("selectedMachineCode", selectedMachineCode);
        evidence.put("firstBind", StrUtil.isBlank(originalMachineCode) && StrUtil.isNotBlank(selectedMachineCode));
        evidence.put("switched", switched);
        evidence.put("switchReason", switchReason);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.SMALL_GLUE_CONTINUOUS,
                selectedMachineCode == null
                        ? TcScheduleRuleResultEnum.REJECT : TcScheduleRuleResultEnum.PASS,
                evidence);
    }

    /**
     * 写入机台过滤证据。
     *
     * @param context    排程上下文
     * @param task       任务草稿
     * @param candidate  候选机台
     * @param result     过滤结果
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     */
    private void addFilterTrace(TcScheduleContext context, TcTaskDraft task, TcMachineCandidate candidate,
                                TcScheduleRuleResultEnum result, String reasonCode, String reasonDesc) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("machineCode", candidate.getMachineCode());
        evidence.put("remainCapacity", candidate.getRemainCapacity());
        evidence.put("reasonCode", reasonCode);
        evidence.put("reasonDesc", reasonDesc);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.MACHINE_FILTER, result, evidence);
    }

    /**
     * 写入机台评分证据。
     *
     * @param context     排程上下文
     * @param task        任务草稿
     * @param candidate   候选机台
     * @param scoreResult 评分结果
     */
    private void addScoreTrace(TcScheduleContext context, TcTaskDraft task, TcMachineCandidate candidate,
                               ScheduleScoreResult scoreResult) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("machineCode", candidate.getMachineCode());
        evidence.put("strategyCode", scoreResult == null ? null : scoreResult.getStrategyCode());
        evidence.put("score", scoreResult == null ? null : scoreResult.getTotalScore());
        evidence.put("scoreItems", scoreResult == null ? null : scoreResult.getScoreItems());
        evidence.put("description", scoreResult == null ? null : scoreResult.getDescription());
        evidence.put("remainCapacity", candidate.getRemainCapacity());
        evidence.put("machineSpeed", candidate.getMachineSpeed());
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.MACHINE_SCORE,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 写入机台分配最终结果证据。
     *
     * @param context             排程上下文
     * @param task                任务草稿
     * @param result              分配结果
     * @param selectedMachineCode 选中机台编码
     * @param reasonCode          原因编码
     * @param reasonDesc          原因描述
     */
    private void addAssignTrace(TcScheduleContext context, TcTaskDraft task, TcScheduleRuleResultEnum result,
                                String selectedMachineCode, String reasonCode, String reasonDesc) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("selectedMachineCode", selectedMachineCode);
        evidence.put("reasonCode", reasonCode);
        evidence.put("reasonDesc", reasonDesc);
        traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.MACHINE_ASSIGN, result, evidence);
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TcRuleTrace traceOf(TcScheduleContext context, TcTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TcRuleTrace());
    }

    /**
     * 标记任务无可用机台。
     *
     * @param task 待排任务草稿
     */
    private void markNoAvailableMachine(TcTaskDraft task) {
        this.markUnplanned(task, TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE);
    }

    /**
     * 按指定未排原因枚举标记任务。
     *
     * @param task   待排任务草稿
     * @param reason 未排原因枚举
     */
    private void markUnplanned(TcTaskDraft task, TcUnplannedReasonEnum reason) {
        task.setUnplannedReasonCode(reason.getCode());
        task.setUnplannedReasonDesc(reason.getDesc());
    }

    /**
     * 根据被过滤候选机台的过滤原因，归类顶层未排原因。
     *
     * <p>全部候选机台被过滤时，不再一律标记 NO_AVAILABLE_MACHINE，而是按过滤原因码映射：
     * 全部 NO_REMAIN_CAPACITY→CAPACITY_NOT_ENOUGH；全部 MOUTH_PLATE_NOT_MATCH→MOUTH_PLATE_NOT_MATCH；
     * 全部 GLUE_MACHINE_NOT_MATCH→GLUE_MACHINE_NOT_ALLOWED；MACHINE_DISABLED/定点规则/混合原因→NO_AVAILABLE_MACHINE 兜底。</p>
     *
     * @param candidates 被过滤的候选机台列表
     * @return 归类后的未排原因枚举
     */
    private TcUnplannedReasonEnum resolveUnplannedReasonFromCandidates(List<TcMachineCandidate> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE;
        }
        Set<String> reasonCodes = candidates.stream()
                .filter(TcMachineCandidate::isFiltered)
                .map(TcMachineCandidate::getFilterReasonCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (reasonCodes.size() == 1) {
            String reasonCode = reasonCodes.iterator().next();
            if (TcMachineFilterReasonEnum.NO_REMAIN_CAPACITY.getCode().equals(reasonCode)) {
                return TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
            }
            if (TcMachineFilterReasonEnum.MOUTH_PLATE_NOT_MATCH.getCode().equals(reasonCode)) {
                return TcUnplannedReasonEnum.MOUTH_PLATE_NOT_MATCH;
            }
            if (TcMachineFilterReasonEnum.GLUE_MACHINE_NOT_MATCH.getCode().equals(reasonCode)) {
                return TcUnplannedReasonEnum.GLUE_MACHINE_NOT_ALLOWED;
            }
        }
        // MACHINE_DISABLED / FIXED_MACHINE_* / 混合原因，兜底为无可用机台
        return TcUnplannedReasonEnum.NO_AVAILABLE_MACHINE;
    }

    /**
     * 复制候选机台列表，避免过滤评分过程污染上下文中的原始候选列表。
     *
     * @param source 原始候选机台列表
     * @return 复制后的候选机台列表
     */
    private List<TcMachineCandidate> copyCandidates(List<TcMachineCandidate> source) {
        return source.stream().map(this::copyCandidate).collect(Collectors.toList());
    }

    /**
     * 复制单个候选机台对象，重置过滤状态和评分。
     *
     * @param source 原始候选机台
     * @return 复制后的候选机台
     */
    private TcMachineCandidate copyCandidate(TcMachineCandidate source) {
        TcMachineCandidate copy = new TcMachineCandidate();
        copy.setMachineCode(source.getMachineCode());
        copy.setEnabled(source.getEnabled());
        copy.setMaxCapacity(source.getMaxCapacity());
        copy.setRemainCapacity(source.getRemainCapacity());
        copy.setMaintenanceHours(source.getMaintenanceHours());
        copy.getMaintenanceHoursByShift().putAll(source.getMaintenanceHoursByShift());
        copy.setMachineSpeed(source.getMachineSpeed());
        copy.getSidewallSpeedMap().putAll(source.getSidewallSpeedMap());
        copy.setConfiguredMouthPlateCodes(source.getConfiguredMouthPlateCodes());
        copy.setMouthPlateCodes(source.getMouthPlateCodes());
        copy.setConfiguredGlueCodes(source.getConfiguredGlueCodes());
        copy.setAllowedGlueCodes(source.getAllowedGlueCodes());
        copy.setForbiddenGlueCodes(source.getForbiddenGlueCodes());
        copy.setConfiguredFixedAllowSidewallCodes(source.getConfiguredFixedAllowSidewallCodes());
        copy.setFixedAllowSidewallCodes(source.getFixedAllowSidewallCodes());
        copy.setFixedForbidSidewallCodes(source.getFixedForbidSidewallCodes());
        copy.setTcDjSharedMachine(source.getTcDjSharedMachine());
        copy.setAllowedTcShiftCodes(source.getAllowedTcShiftCodes());
        copy.setSharedDjShiftCodes(source.getSharedDjShiftCodes());
        copy.setMouthPlateMatched(source.getMouthPlateMatched());
        copy.setGlueMachineMatched(source.getGlueMachineMatched());
        copy.setFixedMachineSelected(source.getFixedMachineSelected());
        copy.setFixedMachineExcluded(source.getFixedMachineExcluded());
        copy.setTailMainGlueCode(source.getTailMainGlueCode());
        copy.setTailBaseGlueCode(source.getTailBaseGlueCode());
        copy.setTailMouthPlateCode(source.getTailMouthPlateCode());
        copy.setSwitchCostHours(source.getSwitchCostHours());
        copy.setFixedMachineMatched(source.getFixedMachineMatched());
        copy.getEvidence().putAll(source.getEvidence());
        return copy;
    }

    /**
     * 按当前任务补齐候选机台的动态过滤和评分输入。
     *
     * @param task       待排任务草稿
     * @param context    胎侧排程上下文
     * @param candidates 候选机台列表
     */
    private void prepareCandidatesForTask(TcTaskDraft task, TcScheduleContext context,
                                          List<TcMachineCandidate> candidates) {
        boolean hasFixedAllowRule = candidates.stream()
                .anyMatch(candidate -> contains(candidate.getConfiguredFixedAllowSidewallCodes(), task.getSidewallCode()));
        for (TcMachineCandidate candidate : candidates) {
            this.applyEffectivePredecessor(task, context, candidate);
            BigDecimal machineSpeed = resolveMachineSpeed(task, candidate, context);
            BigDecimal remainCapacity = resolveRemainCapacity(task, context, candidate, machineSpeed);
            candidate.setMachineSpeed(machineSpeed);
            candidate.setRemainCapacity(remainCapacity);
            candidate.setMouthPlateMatched(isMouthPlateMatched(task, candidate));
            candidate.setGlueMachineMatched(isGlueMachineMatched(task, candidate));
            boolean fixedAllowMatched = contains(candidate.getFixedAllowSidewallCodes(), task.getSidewallCode());
            candidate.setFixedMachineSelected(!hasFixedAllowRule || fixedAllowMatched);
            candidate.setFixedMachineMatched(fixedAllowMatched);
            candidate.setFixedMachineExcluded(contains(candidate.getFixedForbidSidewallCodes(), task.getSidewallCode()));
            // 打印机台速度选择来源
            log.info("[TC_MACHINE_SPEED] sidewallCode={}, machineCode={}, 机台速度【{}】，来源={}",
                    task.getSidewallCode(), candidate.getMachineCode(), machineSpeed,
                    candidate.getSidewallSpeedMap().containsKey(task.getSidewallCode()) ? "胎侧规格速度" : "最大产能/班次小时数");
            // 打印机台产能计算公式
            log.info("[TC_MACHINE_CAPACITY] sidewallCode={}, machineCode={}, shiftOrder={}, 最大产能【maxCapacity】-检修折算量【maintenanceDeduct】-已排计划量【assignedPlanQty】-已发生切换折算量【existingSwitchDeduct】-当前切换折算量【currentSwitchDeduct】=剩余产能【remainCapacity】",
                    task.getSidewallCode(), candidate.getMachineCode(), task.getShiftOrder());
            BigDecimal shiftMaintenanceHours = this.resolveMaintenanceHours(task, candidate);
            log.info("[TC_MACHINE_CAPACITY_DETAIL] sidewallCode={}, machineCode={}, shiftOrder={}, maxCapacity={}, maintenanceHours={}, totalMaintenanceHours={}, machineSpeed={}, assignedPlanQty={}, previousGlueCode={}, currentGlueCode={}, glueChangeCapacityDeductParam={}, currentGlueSwitchCapacityDeduct={}, existingSwitchCapacityDeduct={}, currentSwitchCapacityDeduct={}, remainCapacity={}",
                    task.getSidewallCode(), candidate.getMachineCode(), task.getShiftOrder(),
                    candidate.getMaxCapacity(), shiftMaintenanceHours, candidate.getMaintenanceHours(), machineSpeed,
                    resolveAssignedPlanQty(context, candidate.getMachineCode(), task.getShiftOrder()),
                    candidate.getTailMainGlueCode(), task.getGlueCode(),
                    this.resolveParamValue(context, TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT,
                            TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT),
                    candidate.getEvidence().get("glueSwitchCapacityDeduct"),
                    candidate.getEvidence().get("existingSwitchCapacityDeduct"),
                    candidate.getEvidence().get("currentSwitchCapacityDeduct"),
                    remainCapacity);
        }
    }

    /**
     * 判断候选机台是否匹配任务口型板。
     *
     * @param task      待排任务草稿
     * @param candidate 候选机台
     * @return true 表示匹配
     */
    private boolean isMouthPlateMatched(TcTaskDraft task, TcMachineCandidate candidate) {
        if (StrUtil.isBlank(task.getMouthPlateCode())) {
            return true;
        }
        if (CollUtil.isEmpty(candidate.getConfiguredMouthPlateCodes())) {
            return true;
        }
        return contains(candidate.getMouthPlateCodes(), task.getMouthPlateCode());
    }

    /**
     * 判断当前任务胶料是否符合候选机台胶料关系。
     *
     * <p>禁用关系始终优先排除；当前机台没有配置任务主胶料时不限制，存在配置时按本机关系判断。</p>
     *
     * @param task             待排任务草稿
     * @param candidate        候选机台
     * @return true 表示匹配
     */
    private boolean isGlueMachineMatched(TcTaskDraft task, TcMachineCandidate candidate) {
        if (StrUtil.isBlank(task.getGlueCode())) {
            return true;
        }
        if (contains(candidate.getForbiddenGlueCodes(), task.getGlueCode())) {
            return false;
        }
        if (!contains(candidate.getConfiguredGlueCodes(), task.getGlueCode())) {
            return true;
        }
        return contains(candidate.getAllowedGlueCodes(), task.getGlueCode());
    }

    /**
     * 解析机台生产速度，优先机台+胎侧规格，其次最大产能/班次小时数，最后机台默认速度。
     *
     * @param task      待排任务草稿
     * @param candidate 候选机台
     * @param context   胎侧排程上下文
     * @return 生产速度，缺失时返回0
     */
    private BigDecimal resolveMachineSpeed(TcTaskDraft task, TcMachineCandidate candidate, TcScheduleContext context) {
        BigDecimal speed = candidate.getSidewallSpeedMap().get(task.getSidewallCode());
        if (speed != null) {
            return speed;
        }
        // 无胎侧规格速度时，使用最大产能 / 班次小时数作为机台生产速度
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal shiftHours = nvl(context.getShiftHoursMap().get(task.getShiftOrder()));
        if (maxCapacity.compareTo(BigDecimal.ZERO) > 0 && shiftHours.compareTo(BigDecimal.ZERO) > 0) {
        return maxCapacity.divide(shiftHours, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                java.math.RoundingMode.HALF_UP);
        }
        return nvl(candidate.getMachineSpeed());
    }

    /**
     * 计算合并到既有同胎侧任务时的剩余产能，合并数量不会新增切换损失。
     *
     * @param task         待合并任务草稿
     * @param context      排程上下文
     * @param candidate    候选机台
     * @param machineSpeed 生产速度
     * @return 扣除检修、已排量和既有切换损失后的剩余产能
     */
    private BigDecimal resolveRemainCapacityWithoutNewSwitch(TcTaskDraft task, TcScheduleContext context,
                                                              TcMachineCandidate candidate,
                                                              BigDecimal machineSpeed) {
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal maintenanceDeduct = this.resolveMaintenanceHours(task, candidate).multiply(this.nvl(machineSpeed));
        BigDecimal assignedPlanQty = this.resolveAssignedPlanQty(context, candidate.getMachineCode(),
                task.getShiftOrder());
        BigDecimal existingSwitchDeduct = this.resolveExistingSwitchDeduct(context, candidate.getMachineCode(),
                task.getShiftOrder());
        candidate.setSwitchCostHours(BigDecimal.ZERO);
        task.setPreviousSpecSwitchHours(BigDecimal.ZERO);
        task.setPreviousGlueSwitchHours(BigDecimal.ZERO);
        task.setPreviousGlueSwitchCapacityDeduct(BigDecimal.ZERO);
        candidate.getEvidence().put("specSwitchHours", BigDecimal.ZERO);
        candidate.getEvidence().put("glueSwitchHours", BigDecimal.ZERO);
        candidate.getEvidence().put("glueSwitchCapacityDeduct", BigDecimal.ZERO);
        candidate.getEvidence().put("accumulatedSwitchHours",
                this.resolveExistingSwitchHours(context, candidate.getMachineCode(), task.getShiftOrder()));
        candidate.getEvidence().put("existingSwitchCapacityDeduct", existingSwitchDeduct);
        candidate.getEvidence().put("currentSwitchCapacityDeduct", BigDecimal.ZERO);
        return this.nvl(maxCapacity).subtract(maintenanceDeduct).subtract(assignedPlanQty)
                .subtract(existingSwitchDeduct).max(BigDecimal.ZERO);
    }

    /**
     * 计算顺延任务前插后的剩余产能，按前插后的完整链顺序重新折算切换损失。
     *
     * @param task         待前插任务草稿
     * @param context      排程上下文
     * @param candidate    候选机台
     * @param machineSpeed 待前插任务生产速度
     * @return 按前插后链顺序扣减切换损失的剩余产能
     */
    private BigDecimal resolvePrependRemainCapacity(TcTaskDraft task, TcScheduleContext context,
                                                    TcMachineCandidate candidate, BigDecimal machineSpeed) {
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal maintenanceDeduct = this.resolveMaintenanceHours(task, candidate).multiply(this.nvl(machineSpeed));
        BigDecimal assignedPlanQty = this.resolveAssignedPlanQty(context, candidate.getMachineCode(),
                task.getShiftOrder());
        List<TcTaskDraft> orderedTaskList = new ArrayList<>();
        orderedTaskList.add(task);
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(candidate.getMachineCode(),
                task.getShiftOrder());
        if (chain != null && CollUtil.isNotEmpty(chain.toList())) {
            orderedTaskList.addAll(chain.toList().stream()
                    .map(ScheduleTaskNode::getTask)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        TcTaskPredecessor predecessor = this.resolvePreviousShiftPredecessor(context, candidate.getMachineCode(),
                task.getShiftOrder());
        BigDecimal totalSwitchHours = BigDecimal.ZERO;
        BigDecimal totalSwitchDeduct = BigDecimal.ZERO;
        for (TcTaskDraft currentTask : orderedTaskList) {
            BigDecimal currentSpeed = currentTask == task
                    ? this.nvl(machineSpeed) : this.nvl(currentTask.getMachineSpeed());
            if (this.nvl(currentTask.getPlanQty()).compareTo(BigDecimal.ZERO) <= 0
                    || currentSpeed.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal specSwitchHours = predecessor != null
                    && !Objects.equals(predecessor.getSidewallCode(), currentTask.getSidewallCode())
                ? this.resolveSwitchParamHours(context, TcScheduleConstants.PARAM_SPEC_CHANGE_MINUTES)
                : BigDecimal.ZERO;
            BigDecimal glueSwitchCapacityDeduct = predecessor != null
                    && this.isGlueSwitch(predecessor.getGlueCode(), currentTask.getGlueCode())
                ? this.resolveGlueSwitchCapacityDeduct(context) : BigDecimal.ZERO;
            BigDecimal glueSwitchHours = this.convertCapacityDeductToHours(glueSwitchCapacityDeduct, currentSpeed);
            BigDecimal switchHours = specSwitchHours.add(glueSwitchHours);
            if (currentTask == task) {
                currentTask.setPreviousSpecSwitchHours(specSwitchHours);
                currentTask.setPreviousGlueSwitchHours(glueSwitchHours);
                currentTask.setPreviousGlueSwitchCapacityDeduct(glueSwitchCapacityDeduct);
                candidate.setSwitchCostHours(switchHours);
                candidate.getEvidence().put("specSwitchHours", specSwitchHours);
                candidate.getEvidence().put("glueSwitchHours", glueSwitchHours);
                candidate.getEvidence().put("glueSwitchCapacityDeduct", glueSwitchCapacityDeduct);
            }
            totalSwitchHours = totalSwitchHours.add(switchHours);
            totalSwitchDeduct = totalSwitchDeduct.add(specSwitchHours.multiply(currentSpeed))
                    .add(glueSwitchCapacityDeduct);
            predecessor = this.buildPredecessorFromTask(candidate.getMachineCode(), task.getShiftOrder(), currentTask);
        }
        candidate.getEvidence().put("accumulatedSwitchHours", totalSwitchHours);
        candidate.getEvidence().put("existingSwitchCapacityDeduct",
                this.resolveExistingSwitchDeduct(context, candidate.getMachineCode(), task.getShiftOrder()));
        candidate.getEvidence().put("currentSwitchCapacityDeduct",
                this.nvl(task.getPreviousSpecSwitchHours()).multiply(this.nvl(machineSpeed))
                        .add(this.nvl(task.getPreviousGlueSwitchCapacityDeduct())));
        candidate.getEvidence().put("reorderedTotalSwitchCapacityDeduct", totalSwitchDeduct);
        return this.nvl(maxCapacity).subtract(maintenanceDeduct).subtract(assignedPlanQty)
                .subtract(totalSwitchDeduct).max(BigDecimal.ZERO);
    }

    /**
     * 解析当前班前插任务对应的上一班链尾或排程日前置任务。
     *
     * @param context     排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  当前班次
     * @return 上一班链尾或排程日前置任务；不存在时返回null
     */
    private TcTaskPredecessor resolvePreviousShiftPredecessor(TcScheduleContext context, String machineCode,
                                                              Integer shiftOrder) {
        int normalizedShiftOrder = this.normalizeShiftOrder(shiftOrder);
        for (int previousShiftOrder = normalizedShiftOrder - 1; previousShiftOrder >= 1; previousShiftOrder--) {
            TcTaskDraft previousShiftTail = this.findChainTailTask(context, machineCode, previousShiftOrder);
            if (previousShiftTail != null) {
                return this.buildPredecessorFromTask(machineCode, previousShiftOrder, previousShiftTail);
            }
        }
        return context.getMachinePredecessorMap().get(machineCode);
    }
    /**
     * 计算候选机台当前任务所在班次的剩余产能。
     *
     * @param task         待排任务草稿
     * @param context      胎侧排程上下文
     * @param candidate    候选机台
     * @param machineSpeed 生产速度
     * @return 剩余产能
     */
    private BigDecimal resolveRemainCapacity(TcTaskDraft task, TcScheduleContext context,
                                             TcMachineCandidate candidate, BigDecimal machineSpeed) {
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal maintenanceDeduct = this.resolveMaintenanceHours(task, candidate).multiply(nvl(machineSpeed));
        BigDecimal assignedPlanQty = resolveAssignedPlanQty(context, candidate.getMachineCode(), task.getShiftOrder());
        BigDecimal existingSwitchDeduct = this.resolveExistingSwitchDeduct(context, candidate.getMachineCode(),
                task.getShiftOrder());
        BigDecimal currentSwitchHours = this.resolveCurrentSwitchHours(task, context, candidate);
        BigDecimal currentSwitchDeduct = this.nvl(task.getPreviousSpecSwitchHours()).multiply(this.nvl(machineSpeed))
                .add(this.nvl(task.getPreviousGlueSwitchCapacityDeduct()));
        candidate.setSwitchCostHours(currentSwitchHours);
        candidate.getEvidence().put("specSwitchHours", this.nvl(task.getPreviousSpecSwitchHours()));
        candidate.getEvidence().put("glueSwitchHours", this.nvl(task.getPreviousGlueSwitchHours()));
        candidate.getEvidence().put("glueSwitchCapacityDeduct",
                this.nvl(task.getPreviousGlueSwitchCapacityDeduct()));
        candidate.getEvidence().put("accumulatedSwitchHours",
                this.resolveExistingSwitchHours(context, candidate.getMachineCode(), task.getShiftOrder()));
        candidate.getEvidence().put("existingSwitchCapacityDeduct", existingSwitchDeduct);
        candidate.getEvidence().put("currentSwitchCapacityDeduct", currentSwitchDeduct);
        return nvl(maxCapacity).subtract(maintenanceDeduct).subtract(assignedPlanQty)
                .subtract(existingSwitchDeduct).subtract(currentSwitchDeduct).max(BigDecimal.ZERO);
    }

    /**
     * 解析候选机台最大班产，基础数据无效时使用固定兼容值。
     *
     * @param candidate 候选机台
     * @return 正数最大班产
     */
    private BigDecimal resolveMachineMaxCapacity(TcMachineCandidate candidate) {
        BigDecimal maxCapacity = candidate == null ? null : candidate.getMaxCapacity();
        if (maxCapacity == null || maxCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal(TcScheduleConstants.DEFAULT_MACHINE_MAX_CAPACITY);
        }
        return maxCapacity;
    }


    /**
     * 计算当前候选机台相对有效前置任务的规格和胶料切换时长。
     *
     * @param task      当前任务
     * @param context   排程上下文
     * @param candidate 候选机台
     * @return 当前候选切换总小时数
     */
    private BigDecimal resolveCurrentSwitchHours(TcTaskDraft task, TcScheduleContext context,
                                                 TcMachineCandidate candidate) {
        TcTaskPredecessor predecessor = this.resolveEffectivePredecessor(context, candidate.getMachineCode(),
                task.getShiftOrder());
        BigDecimal specSwitchHours = BigDecimal.ZERO;
        BigDecimal glueSwitchHours = BigDecimal.ZERO;
        task.setPreviousGlueSwitchCapacityDeduct(BigDecimal.ZERO);
        if (predecessor != null) {
            if (!Objects.equals(predecessor.getSidewallCode(), task.getSidewallCode())) {
            specSwitchHours = this.resolveSwitchParamHours(context, TcScheduleConstants.PARAM_SPEC_CHANGE_MINUTES);
            }
            if (this.isGlueSwitch(predecessor.getGlueCode(), task.getGlueCode())) {
                BigDecimal glueSwitchCapacityDeduct = this.resolveGlueSwitchCapacityDeduct(context);
                task.setPreviousGlueSwitchCapacityDeduct(glueSwitchCapacityDeduct);
                glueSwitchHours = this.convertCapacityDeductToHours(glueSwitchCapacityDeduct,
                        this.resolveMachineSpeed(task, candidate, context));
            }
        }
        task.setPreviousSpecSwitchHours(specSwitchHours);
        task.setPreviousGlueSwitchHours(glueSwitchHours);
        return specSwitchHours.add(glueSwitchHours);
    }

    /**
     * 汇总目标机台班次内已发生切换的折算产能。
     *
     * @param context     排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 已发生切换折算量
     */
    private BigDecimal resolveExistingSwitchDeduct(TcScheduleContext context, String machineCode, Integer shiftOrder) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return BigDecimal.ZERO;
        }
        return chain.toList().stream()
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .map(chainTask -> this.nvl(chainTask.getPreviousSpecSwitchHours())
                        .multiply(this.nvl(chainTask.getMachineSpeed()))
                        .add(this.nvl(chainTask.getPreviousGlueSwitchCapacityDeduct())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 解析主胶料切换固定产能扣减量。
     *
     * @param context 排程上下文
     * @return 非负固定扣减量；参数缺失或非法时返回默认值
     */
    private BigDecimal resolveGlueSwitchCapacityDeduct(TcScheduleContext context) {
        String value = this.resolveParamValue(context, TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT,
                TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        try {
            BigDecimal deduct = new BigDecimal(value);
            if (deduct.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=NEGATIVE_VALUE",
                        context.getBatchNo(), context.getTraceId(),
                        TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT, value);
                return BigDecimal.ZERO;
            }
            return deduct;
        } catch (NumberFormatException exception) {
            log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, defaultValue={}, reason=INVALID_NUMBER",
                    context.getBatchNo(), context.getTraceId(),
                    TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT, value,
                    TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT, exception);
            return new BigDecimal(TcScheduleConstants.DEFAULT_GLUE_CHANGE_CAPACITY_DEDUCT);
        }
    }

    /**
     * 将固定产能扣减量按当前任务速度折算为切换小时数。
     *
     * @param capacityDeduct 固定产能扣减量
     * @param machineSpeed 当前任务机台速度
     * @return 切换小时数；速度无效时返回0
     */
    private BigDecimal convertCapacityDeductToHours(BigDecimal capacityDeduct, BigDecimal machineSpeed) {
        if (this.nvl(capacityDeduct).compareTo(BigDecimal.ZERO) <= 0
                || this.nvl(machineSpeed).compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return capacityDeduct.divide(machineSpeed, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                java.math.RoundingMode.HALF_UP);
    }

    /**
     * 判断前后任务是否发生可确认的主胶料切换。
     *
     * @param previousGlueCode 前置任务主胶料编码
     * @param currentGlueCode 当前任务主胶料编码
     * @return 两个编码均非空且不相同时返回true
     */
    private boolean isGlueSwitch(String previousGlueCode, String currentGlueCode) {
        return StrUtil.isNotBlank(previousGlueCode) && StrUtil.isNotBlank(currentGlueCode)
                && !Objects.equals(previousGlueCode.trim(), currentGlueCode.trim());
    }

    /**
     * 汇总目标机台班次内已发生切换小时数。
     *
     * @param context     排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 累计切换小时数
     */
    private BigDecimal resolveExistingSwitchHours(TcScheduleContext context, String machineCode, Integer shiftOrder) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return BigDecimal.ZERO;
        }
        return chain.toList().stream()
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .map(chainTask -> this.nvl(chainTask.getPreviousSpecSwitchHours())
                        .add(this.nvl(chainTask.getPreviousGlueSwitchHours())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 将切换分钟数参数转换为小时数。
     *
     * @param context   排程上下文
     * @param paramCode 参数编码
     * @return 非负切换小时数，参数缺失或非法时返回0
     */
    private BigDecimal resolveSwitchParamHours(TcScheduleContext context, String paramCode) {
        String value = this.resolveParamValue(context, paramCode, "0");
        try {
            BigDecimal switchMinutes = new BigDecimal(value).max(BigDecimal.ZERO);
            if (switchMinutes.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
        return switchMinutes.divide(BigDecimal.valueOf(TcScheduleConstants.MINUTES_PER_HOUR),
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            log.warn("[TC_SWITCH_PARAM] batchNo={}, traceId={}, paramCode={}, paramValue={}, reason=INVALID_NUMBER",
                    context.getBatchNo(), context.getTraceId(), paramCode, value, exception);
            return BigDecimal.ZERO;
        }
    }
    /**
     * 解析当前任务班次实际需要扣减的检修小时数。
     *
     * @param task      待排任务草稿
     * @param candidate 候选机台
     * @return 当前班次检修小时数，未加载分班数据时回退到当日总检修小时数
     */
    private BigDecimal resolveMaintenanceHours(TcTaskDraft task, TcMachineCandidate candidate) {
        Map<Integer, BigDecimal> maintenanceHoursByShift = candidate.getMaintenanceHoursByShift();
        if (CollUtil.isNotEmpty(maintenanceHoursByShift)) {
            return nvl(maintenanceHoursByShift.get(this.normalizeShiftOrder(task.getShiftOrder())));
        }
        return nvl(candidate.getMaintenanceHours());
    }
    /**
     * 汇总目标机台班次已排计划量。
     *
     * @param context     胎侧排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 已排计划量
     */
    private BigDecimal resolveAssignedPlanQty(TcScheduleContext context, String machineCode, Integer shiftOrder) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ScheduleTaskNode<TcTaskDraft> node : chain.toList()) {
            if (node.getTask() != null && node.getTask().getPlanQty() != null) {
                total = total.add(node.getTask().getPlanQty());
            }
        }
        return total;
    }

    /**
     * 判断集合是否包含非空值。
     *
     * @param values 集合
     * @param value  值
     * @return true 表示包含
     */
    private boolean contains(java.util.Set<String> values, String value) {
        return values != null && StrUtil.isNotBlank(value) && values.contains(value);
    }

    /**
     * 空值转0。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 使用调用方准备好的任务列表执行机台分配。
     *
     * <p>该方法用于场景测试或上层已完成数据加载的入口，先把任务列表放入上下文，
     * 再复用默认分配逻辑建立任务链。方法会修改上下文中的任务列表和任务链。</p>
     *
     * @param context  胎侧排程上下文
     * @param taskList 已准备好的任务草稿列表
     * @throws ServiceException 上下文为空时抛出
     */
    public void assignPrepared(TcScheduleContext context, List<TcTaskDraft> taskList) {
        if (context == null) {
            throw new ServiceException(TcScheduleErrorCodeEnum.TC_CONTEXT_EMPTY.getDefaultMessage());
        }
        context.setTaskDraftList(taskList);
        assign(context);
    }
}
