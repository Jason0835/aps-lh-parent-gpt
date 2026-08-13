package com.zlt.aps.tc.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.*;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintCalculator;
import com.zlt.aps.common.engine.schedule.constraint.SchedulePlanQtyAdjustmentResult;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerResult;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleToolLedgerSnapshot;
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
 * <p>对已预置机台编码的任务固定候选机台后复用统一工装与产能校验；
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

    /** 胎面、胎侧共用排程约束纯计算器 */
    private final ScheduleConstraintCalculator constraintCalculator = new ScheduleConstraintCalculator();

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
        Map<String, BigDecimal> runtimeStockMap = new HashMap<>(context.getInitialStockMap());
        boolean inventoryClosedLoopEnabled = CollUtil.isNotEmpty(context.getPlanTaskGroupMap());
        // 先保留全部生产任务，逐班按实际库存重新计算后再决定是否进入机台分配。
        Map<Integer, List<TcTaskDraft>> shiftTaskMap = new ArrayList<>(context.getTaskDraftList()).stream()
                .filter(Objects::nonNull)
                .filter(task -> !Boolean.TRUE.equals(task.getSourceExplainTask()))
                .collect(Collectors.groupingBy(task -> this.normalizeShiftOrder(task.getShiftOrder()),
                        TreeMap::new, Collectors.toList()));
        for (Map.Entry<Integer, List<TcTaskDraft>> entry : shiftTaskMap.entrySet()) {
            if (inventoryClosedLoopEnabled) {
                this.recalculateShiftPlans(context, entry.getValue(), runtimeStockMap);
            }
            List<TcTaskDraft> remainingTaskList = entry.getValue().stream()
                    .filter(this::isMachineAssignmentRequired)
                    .collect(Collectors.toList());
            while (CollUtil.isNotEmpty(remainingTaskList)) {
                TcTaskDraft task = this.selectNextTaskByMachinePredecessor(remainingTaskList, context);
                remainingTaskList.remove(task);
                this.assignSingleTask(task, context);
            }
            this.fillCurrentShiftIdleCapacity(entry.getKey(), shiftTaskMap, context);
            if (inventoryClosedLoopEnabled) {
                this.closeShiftInventory(context, entry.getKey(), entry.getValue(), runtimeStockMap);
            }
        }
    }

    /**
     * 按当前实际班初库存重新计算本班计划量，避免后续班次继续使用机台损耗和工装限制确定前的旧库存。
     *
     * @param context         胎侧排程上下文
     * @param shiftTaskList   当前班原始生产任务
     * @param runtimeStockMap 已完成班次形成的实际可用库存
     */
    private void recalculateShiftPlans(TcScheduleContext context, List<TcTaskDraft> shiftTaskList,
                                       Map<String, BigDecimal> runtimeStockMap) {
        if (CollUtil.isEmpty(shiftTaskList)) {
            return;
        }
        ITcPlanQtyStrategy planQtyStrategy = strategyRegistry.getPlanQtyStrategy(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_PLAN_QTY_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        Map<String, BigDecimal> planningStockMap = new HashMap<>(runtimeStockMap);
        Set<String> recalculatedSidewallCodeSet = new HashSet<>();
        shiftTaskList.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TcTaskDraft::getSidewallCode,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TcTaskDraft::getBaseSortIndex,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TcTaskDraft::getBusinessKey,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(task -> {
                    if (StrUtil.isBlank(task.getSidewallCode())
                            || StrUtil.isNotBlank(task.getUnplannedReasonCode())) {
                        return;
                    }
                    BigDecimal openingStock = planningStockMap.getOrDefault(
                            task.getSidewallCode(), this.nvl(task.getRollingStockQty()));
                    runtimeStockMap.putIfAbsent(task.getSidewallCode(), openingStock);
                    task.setRollingStockQty(openingStock);
                    if (StrUtil.contains(task.getPlanGroupKey(), "|PRESET|")) {
                        planningStockMap.put(task.getSidewallCode(), openingStock.add(this.nvl(task.getPlanQty()))
                                .subtract(this.nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
                        return;
                    }
                    if (!recalculatedSidewallCodeSet.add(task.getSidewallCode())) {
                        this.clearDuplicateProductShiftPlan(task, openingStock);
                        return;
                    }
                    task.setPlanQty(null);
                    TcPlanQtyResult planQtyResult = planQtyStrategy.calculate(task, context);
                    this.applyRuntimePlanQtyResult(task, planQtyResult);
                    planningStockMap.put(task.getSidewallCode(), this.nvl(task.getPlanStockQty()));
                });
    }

    /**
     * 清空同产品同班重复任务的运行计划量，原始需求只由稳定排序后的首个任务消费一次。
     *
     * @param task 重复任务
     * @param openingStock 当前班可用库存
     */
    private void clearDuplicateProductShiftPlan(TcTaskDraft task, BigDecimal openingStock) {
        task.setPreLossPlanQty(BigDecimal.ZERO);
        task.setLossAddQty(BigDecimal.ZERO);
        task.setPlanQtyBeforeToolLimit(BigDecimal.ZERO);
        task.setPlanQty(BigDecimal.ZERO);
        task.setPlanStockQty(openingStock);
    }

    /**
     * 使用本班实际已排量关账并生成可用库存与短缺台账。
     *
     * @param context         胎侧排程上下文
     * @param shiftOrder      当前班次
     * @param shiftTaskList   当前班原始生产任务
     * @param runtimeStockMap 实际库存运行态
     */
    private void closeShiftInventory(TcScheduleContext context, Integer shiftOrder,
                                     List<TcTaskDraft> shiftTaskList,
                                     Map<String, BigDecimal> runtimeStockMap) {
        Map<String, BigDecimal> demandQtyMap = shiftTaskList.stream()
                .filter(Objects::nonNull)
                .filter(task -> StrUtil.isNotBlank(task.getSidewallCode()))
                .collect(Collectors.groupingBy(TcTaskDraft::getSidewallCode, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getCurrentShiftDemandQty()), BigDecimal::max)));
        Map<String, BigDecimal> assignedQtyMap = context.getTaskChainGroup().values().stream()
                .filter(Objects::nonNull)
                .flatMap(chain -> chain.toList().stream())
                .map(ScheduleTaskNode::getTask)
                .filter(Objects::nonNull)
                .filter(task -> Objects.equals(this.normalizeShiftOrder(task.getShiftOrder()), shiftOrder))
                .filter(task -> StrUtil.isNotBlank(task.getSidewallCode()))
                .collect(Collectors.groupingBy(TcTaskDraft::getSidewallCode, LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                task -> this.nvl(task.getPlanQty()), BigDecimal::add)));
        Set<String> sidewallCodeSet = new LinkedHashSet<>(demandQtyMap.keySet());
        sidewallCodeSet.addAll(assignedQtyMap.keySet());
        for (String sidewallCode : sidewallCodeSet) {
            BigDecimal rawBalance = runtimeStockMap.getOrDefault(sidewallCode, BigDecimal.ZERO)
                    .add(assignedQtyMap.getOrDefault(sidewallCode, BigDecimal.ZERO))
                    .subtract(demandQtyMap.getOrDefault(sidewallCode, BigDecimal.ZERO));
            BigDecimal shortageQty = rawBalance.min(BigDecimal.ZERO).abs();
            BigDecimal availableStockQty = rawBalance.max(BigDecimal.ZERO);
            runtimeStockMap.put(sidewallCode, availableStockQty);
            context.getProductShiftShortageMap().put(sidewallCode + "|" + shiftOrder, shortageQty);
            shiftTaskList.stream()
                    .filter(task -> Objects.equals(sidewallCode, task.getSidewallCode()))
                    .forEach(task -> task.setPlanStockQty(availableStockQty));
        }
        context.setRemainingStockMap(new HashMap<>(runtimeStockMap));
    }

    /**
     * 将逐班重算结果回填任务草稿。
     *
     * @param task   当前任务
     * @param result 计划量策略结果
     */
    private void applyRuntimePlanQtyResult(TcTaskDraft task, TcPlanQtyResult result) {
        if (result == null) {
            return;
        }
        task.setBaseDemandQty(result.getBaseDemandQty());
        task.setLossAddQty(result.getLossAddQty());
        task.setToolLimitAdjustQty(result.getToolLimitAdjustQty());
        task.setToolOverflowQty(result.getToolOverflowQty());
        task.setMinStartAdjustQty(result.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(result.getTailRoundAdjustQty());
        task.setCapacityAdjustQty(result.getCapacityAdjustQty());
        task.setPreLossPlanQty(result.getPreLossPlanQty());
        task.setPlanQtyBeforeToolLimit(result.getPlanQtyBeforeToolLimit());
        task.setPlanQty(result.getFinalPlanQty());
        task.setCalcFormulaDesc(result.getCalcFormulaDesc());
    }

    /**
     * 执行单个任务的机台分配。
     *
     * @param task 当前待排任务
     * @param context 胎侧排程上下文
     */
    private void assignSingleTask(TcTaskDraft task, TcScheduleContext context) {
        if (this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) <= 0
                && this.nvl(task.getToolOverflowQty()).compareTo(BigDecimal.ZERO) <= 0) {
            context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.emptyList());
            this.addAssignTrace(context, task, TcScheduleRuleResultEnum.SKIP, null,
                    TcScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getCode(),
                    TcScheduleTaskStatusEnum.NO_PRODUCTION_NEEDED.getDesc());
            return;
        }
        if (task.isUnassigned()) {
            // 未预置机台的任务走完整过滤评分流程
            this.assignByFilterAndScore(task, context);
            return;
        }
        TcMachineCandidate presetCandidate = this.findCandidateByMachineCode(
                context.getMachineCandidateList(), task.getMachineCode());
        if (!this.isMachineShiftOpen(task, context, presetCandidate)) {
            // 预置机台同样不能绕过机台开机班次；清除预置值后复用完整候选与顺延规则。
            task.setMachineCode(null);
            this.assignByFilterAndScore(task, context);
            return;
        }
        // 预置机台仅固定候选机台，工装、产能、结算和顺延仍复用普通选中机台流程。
        TcMachineCandidate candidate = this.resolvePresetMachineCandidate(task, context);
        context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(candidate));
        this.addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS, task.getMachineCode(), null, null);
        this.bindSmallGlueMachine(context, task, task.getMachineCode(), null,
                TcScheduleConstants.PRESET_MACHINE_BIND_SOURCE);
        ITcMachineFilterRule filterRule = strategyRegistry.getMachineFilterRule(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        ITcMachineScoreStrategy scoreStrategy = strategyRegistry.getMachineScoreStrategy(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_SCORE_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        this.appendTaskWithCapacityOverflow(task, candidate, Collections.singletonList(candidate), filterRule,
                scoreStrategy, context, "预置机台");
    }

    /**
     * 判断预置机台是否开放任务所在班次。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @param candidate 预置机台候选
     * @return true 表示机台开放当前班次
     */
    private boolean isMachineShiftOpen(TcTaskDraft task, TcScheduleContext context,
                                       TcMachineCandidate candidate) {
        if (task == null || context == null || candidate == null || candidate.getOpenShiftCodes() == null) {
            return false;
        }
        TcShiftTimeWindow shiftTimeWindow = context.getShiftTimeWindowMap().get(task.getShiftOrder());
        return shiftTimeWindow != null && StrUtil.isNotBlank(shiftTimeWindow.getShiftCode())
                && candidate.getOpenShiftCodes().contains(shiftTimeWindow.getShiftCode().trim());
    }

    /**
     * 判断任务是否需要进入机台分配。
     *
     * @param task 待排任务草稿
     * @return true 表示任务计划量大于零且尚未明确标记未排
     */
    private boolean isMachineAssignmentRequired(TcTaskDraft task) {
        return task != null && (this.nvl(task.getPlanQty()).compareTo(BigDecimal.ZERO) > 0
                || this.nvl(task.getToolOverflowQty()).compareTo(BigDecimal.ZERO) > 0)
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
        boolean startupShift = this.isStartupShift(context, remainingTaskList.get(0));
        TcTaskDraft selectedTask = startupShift
                ? this.selectStartupSupplyFirstTask(remainingTaskList, scoreMap)
                : priorityStrategy.select(remainingTaskList, context, scoreMap);
        String appliedStrategyCode = startupShift ? "STARTUP_SUPPLY_FIRST" : priorityStrategy.getStrategyCode();
        if (startupShift) {
            Map<String, Object> sortEvidence = new LinkedHashMap<>();
            sortEvidence.put("phase", "MACHINE_ASSIGN");
            sortEvidence.put("strategyCode", appliedStrategyCode);
            sortEvidence.put("shiftOrder", selectedTask.getShiftOrder());
            sortEvidence.put("supplyHours", selectedTask.getSupplyHours());
            sortEvidence.put("latestStartTime", selectedTask.getLatestStartTime());
            sortEvidence.put("presetMachine", !selectedTask.isUnassigned());
            sortEvidence.put("chainSortScore", scoreMap.get(selectedTask.getBusinessKey()));
            traceOf(context, selectedTask).addRuleHit(TcScheduleRuleCodeEnum.TASK_SORT,
                    TcScheduleRuleResultEnum.PASS, sortEvidence);
        }
        log.info("[TC_CHAIN_TASK_ORDER] batchNo={}, traceId={}, factoryCode={}, scheduleDate={}, shiftOrder={}, predecessorSnapshot={}, selectedBusinessKey={}, selectedSidewallCode={}, selectedGlueCode={}, strategyCode={}, chainSortScores={}",
                context.getBatchNo(), context.getTraceId(), context.getFactoryCode(), this.formatScheduleDate(context),
                this.normalizeShiftOrder(selectedTask.getShiftOrder()), this.summarizeMachinePredecessors(context,
                        this.normalizeShiftOrder(selectedTask.getShiftOrder())), selectedTask.getBusinessKey(),
                selectedTask.getSidewallCode(), selectedTask.getGlueCode(), appliedStrategyCode, scoreMap);
        return selectedTask;
    }

    /**
     * 在开产班次内按库存供应成型时长严格选择下一个规格。
     *
     * <p>预置机台任务仍作为硬约束优先；其余任务先按供应时长升序，再按最晚开始时间、
     * 连续性得分、基础顺序和业务键稳定兜底。</p>
     *
     * @param remainingTaskList 当前班次剩余任务
     * @param chainScoreMap     任务连续性得分
     * @return 本轮开产班次优先任务
     */
    TcTaskDraft selectStartupSupplyFirstTask(List<TcTaskDraft> remainingTaskList,
                                             Map<String, TcChainSortScore> chainScoreMap) {
        List<TcTaskDraft> presetTaskList = remainingTaskList.stream()
                .filter(task -> !task.isUnassigned())
                .collect(Collectors.toList());
        List<TcTaskDraft> candidateTaskList = CollUtil.isEmpty(presetTaskList)
                ? new ArrayList<>(remainingTaskList) : presetTaskList;
        candidateTaskList.sort(Comparator
                .comparing((TcTaskDraft task) -> task.getSupplyHours() == null)
                .thenComparing(task -> task.getSupplyHours() == null ? BigDecimal.ZERO : task.getSupplyHours())
                .thenComparing(TcTaskDraft::getLatestStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing((TcTaskDraft task) -> chainScoreMap.getOrDefault(
                                task.getBusinessKey(), TcChainSortScore.ZERO),
                        Comparator.reverseOrder())
                .thenComparing(TcTaskDraft::getBaseSortIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(task -> StrUtil.blankToDefault(task.getBusinessKey(), "")));
        return candidateTaskList.get(0);
    }

    /**
     * 判断任务是否属于整日停产后的首个开放班次。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 属于开产班次返回true
     */
    private boolean isStartupShift(TcScheduleContext context, TcTaskDraft task) {
        return context != null && task != null && context.getStartupShiftOrderSet() != null
                && context.getStartupShiftOrderSet().contains(task.getShiftOrder());
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
        return strategyRegistry.getChainTaskPriorityStrategy(strategyCode);
    }

    private String resolveParamValue(TcScheduleContext context, String paramCode, String defaultValue) {
        TcParamValue paramValue = context.getParamMap().get(paramCode);
        return paramValue == null || StrUtil.isBlank(paramValue.getEffectiveValue())
                ? defaultValue : paramValue.getEffectiveValue().trim();
    }

    /**
     * 从本次排程参数快照读取评分权重(BigDecimal)，非法值回退为详设默认值。
     *
     * @param context      排程上下文
     * @param paramCode    参数编码
     * @param defaultValue 详设默认值(字符串)
     * @return 非负评分权重
     */
    private BigDecimal resolveScoreWeight(TcScheduleContext context, String paramCode, String defaultValue) {
        String effectiveValue = defaultValue;
        TcParamValue paramValue = context == null ? null : context.getParamMap().get(paramCode);
        if (paramValue != null && paramValue.getEffectiveValue() != null
                && !paramValue.getEffectiveValue().trim().isEmpty()) {
            effectiveValue = paramValue.getEffectiveValue().trim();
        }
        try {
            return new BigDecimal(effectiveValue).max(BigDecimal.ZERO);
        } catch (NumberFormatException exception) {
            return new BigDecimal(defaultValue);
        }
    }
    private TcChainSortScore calculateBestChainSortScore(TcTaskDraft task, TcScheduleContext context) {
        if (CollUtil.isEmpty(context.getMachineCandidateList())) {
            return TcChainSortScore.ZERO;
        }
        List<TcMachineCandidate> candidates = this.copyCandidates(context.getMachineCandidateList());
        this.prepareCandidatesForTask(task, context, candidates);
        ITcMachineFilterRule filterRule = strategyRegistry.getMachineFilterRule(this.resolveParamValue(context,
                TcScheduleConstants.PARAM_MACHINE_FILTER_STRATEGY, TcScheduleStrategyEnum.DEFAULT.getCode()));
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        TcChainSortScore bestScore = TcChainSortScore.ZERO;
        for (TcMachineCandidate candidate : candidates) {
            if (!filterRule.evaluate(candidate, ruleContext).isPassed()) {
                continue;
            }
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
        String tailMainGlueCode = candidate.getTailMainGlueCode();
        String tailBaseGlueCode = candidate.getTailBaseGlueCode();
        String tailMouthPlateCode = candidate.getTailMouthPlateCode();
        BigDecimal remainCapacity = candidate.getRemainCapacity();
        // 评分权重统一从 TC_SCORE_WEIGHT_* 参数读取，与机台评分 TcDefaultMachineScoreStrategy 同口径，
        // 使调参同时影响机台选择与班内任务链排序（businessScore = capacityScore + switchCostScore + fixedScore）。
        BigDecimal remainCapWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_REMAIN_CAP,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_REMAIN_CAP);
        BigDecimal mainGlueWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_GLUE_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_GLUE_CONT);
        BigDecimal baseGlueWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_BASE_GLUE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_BASE_GLUE);
        BigDecimal mouthPlateWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_MOUTH_CONT,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_MOUTH_CONT);
        BigDecimal switchCostWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_SWITCH_COST,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_SWITCH_COST);
        BigDecimal fixedWeight = this.resolveScoreWeight(context,
                TcScheduleConstants.PARAM_SCORE_WEIGHT_FIXED_MACHINE,
                TcScheduleConstants.DEFAULT_SCORE_WEIGHT_FIXED_MACHINE);
        BigDecimal capacityScore = this.capacityFitScore(task, remainCapacity, remainCapWeight);
        boolean mainGlueMatched = TcGlueSimilarityUtils.isSameNonBlank(task.getGlueCode(), tailMainGlueCode);
        int baseGlueMatchedCount = mainGlueMatched ? 0
                : TcGlueSimilarityUtils.calculateIntersectionCount(
                        TcGlueSimilarityUtils.parseCodeSet(task.getBaseGlueCode()),
                        TcGlueSimilarityUtils.parseCodeSet(tailBaseGlueCode));
        boolean mouthPlateMatched = TcGlueSimilarityUtils.isSameNonBlank(
                task.getMouthPlateCode(), tailMouthPlateCode);
        BigDecimal mainGlueScore = mainGlueMatched ? mainGlueWeight : BigDecimal.ZERO;
        BigDecimal baseGlueScore = mainGlueMatched ? BigDecimal.ZERO
                : TcGlueSimilarityUtils.calculateSimilarityScore(
                        task.getBaseGlueCode(), tailBaseGlueCode, baseGlueWeight);
        BigDecimal mouthPlateScore = mouthPlateMatched ? mouthPlateWeight : BigDecimal.ZERO;
        BigDecimal switchCostScore = switchCostWeight.subtract(this.nvl(candidate.getSwitchCostHours())).max(BigDecimal.ZERO);
        BigDecimal fixedScore = Boolean.TRUE.equals(candidate.getFixedMachineMatched()) ? fixedWeight : BigDecimal.ZERO;
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
    private BigDecimal capacityFitScore(TcTaskDraft task, BigDecimal remainCapacity, BigDecimal weight) {
        BigDecimal planQty = nvl(task.getPlanQty());
        BigDecimal normalizedRemainCapacity = nvl(remainCapacity);
        if (normalizedRemainCapacity.compareTo(BigDecimal.ZERO) <= 0 || planQty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal assignedQty = planQty.min(normalizedRemainCapacity);
        BigDecimal fillRatio = assignedQty.divide(normalizedRemainCapacity,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, java.math.RoundingMode.HALF_UP);
        return weight.multiply(fillRatio)
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
        if (task.getShiftOrder() == null) {
            // 未指定班次的新增任务从第一班开始尝试，保证开机班次硬约束有明确匹配目标。
            task.setShiftOrder(1);
        }
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
            if (this.isCapacityOrShiftBlockedCarryover(task, context, filterRule)) {
                context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);
                log.info("[TC_MACHINE_ASSIGN] 任务[{}]当前班无可用承接，进入后续班次继续尝试",
                        task.getBusinessKey());
                this.removeContextTask(context, task);
                boolean machineShiftBlocked = candidates.stream().anyMatch(candidate ->
                        TcMachineFilterReasonEnum.MACHINE_SHIFT_NOT_OPEN.getCode()
                                .equals(candidate.getFilterReasonCode()));
                BigDecimal capacityOverflowQty = machineShiftBlocked
                        ? BigDecimal.ZERO : nvl(task.getPlanQty());
                this.appendCarryoverQty(task, null, Collections.emptyList(), filterRule, scoreStrategy, context,
                        nvl(task.getPlanQty()), capacityOverflowQty, BigDecimal.ZERO,
                        this.normalizeShiftOrder(task.getShiftOrder()), true);
                return;
            }
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
        this.appendTaskWithCapacityOverflow(task, bestCandidate, passedCandidates, filterRule, scoreStrategy, context,
                "普通排产");
    }

    /**
     * 判断任务是否仅因当前班次无剩余产能或机台未开班而可进入后续班次承接。
     *
     * @param task 当前待排任务
     * @param context 排程上下文
     * @param filterRule 机台过滤规则
     * @return true 表示后续班次仍存在可尝试的静态可行机台
     */
    private boolean isCapacityOrShiftBlockedCarryover(TcTaskDraft task, TcScheduleContext context,
                                                      ITcMachineFilterRule filterRule) {
        List<TcMachineCandidate> staticCandidates = this.copyCandidates(context.getMachineCandidateList());
        this.prepareCandidatesForTask(task, context, staticCandidates);
        TcMachineRuleContext ruleContext = new TcMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        List<TcMachineCandidate> staticPassedCandidates = staticCandidates.stream()
                .filter(candidate -> filterRule.evaluateStatic(candidate, ruleContext).isPassed())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(staticPassedCandidates)
                && staticPassedCandidates.stream().allMatch(candidate -> nvl(candidate.getRemainCapacity())
                .compareTo(BigDecimal.ZERO) <= 0)) {
            return true;
        }

        Integer currentShiftOrder = this.normalizeShiftOrder(task.getShiftOrder());
        return this.hasFutureStaticCandidate(task, context, filterRule, currentShiftOrder);
    }

    /**
     * 按后续班次逐班执行完整静态规则，判断是否仍有可承接机台。
     *
     * <p>胎侧共用机台错班等约束会随班次变化，因此必须以目标班次重新校验全部静态规则。</p>
     *
     * @param task 当前待排任务
     * @param context 排程上下文
     * @param filterRule 机台过滤规则
     * @param currentShiftOrder 当前班次顺序
     * @return true 表示至少一个后续班次存在静态可行机台
     */
    private boolean hasFutureStaticCandidate(TcTaskDraft task, TcScheduleContext context,
                                             ITcMachineFilterRule filterRule, Integer currentShiftOrder) {
        Integer originalShiftOrder = task.getShiftOrder();
        try {
            for (int futureShiftOrder = currentShiftOrder + 1;
                 futureShiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; futureShiftOrder++) {
                TcShiftTimeWindow shiftTimeWindow = context.getShiftTimeWindowMap().get(futureShiftOrder);
                if (shiftTimeWindow == null || StrUtil.isBlank(shiftTimeWindow.getShiftCode())) {
                    continue;
                }
                task.setShiftOrder(futureShiftOrder);
                List<TcMachineCandidate> futureCandidates = this.copyCandidates(context.getMachineCandidateList());
                this.prepareCandidatesForTask(task, context, futureCandidates);
                TcMachineRuleContext futureRuleContext = new TcMachineRuleContext();
                futureRuleContext.setTaskDraft(task);
                futureRuleContext.setScheduleContext(context);
                if (futureCandidates.stream()
                        .anyMatch(candidate -> filterRule.evaluateStatic(candidate, futureRuleContext).isPassed())) {
                    return true;
                }
            }
            return false;
        } finally {
            task.setShiftOrder(originalShiftOrder);
        }
    }

    /**
     * 从待持久化任务集合移除已转换为顺延任务的来源任务。
     *
     * @param context 排程上下文
     * @param task 已转换为顺延任务的来源任务
     */
    private void removeContextTask(TcScheduleContext context, TcTaskDraft task) {
        List<TcTaskDraft> taskList = context.getTaskDraftList();
        if (taskList == null || taskList.isEmpty()) {
            return;
        }
        try {
            taskList.remove(task);
        } catch (UnsupportedOperationException exception) {
            List<TcTaskDraft> mutableTaskList = new ArrayList<>(taskList);
            mutableTaskList.remove(task);
            context.setTaskDraftList(mutableTaskList);
        }
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
     * @param taskSource 任务来源
     */
    private void appendTaskWithCapacityOverflow(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                                List<TcMachineCandidate> firstShiftCandidates,
                                                ITcMachineFilterRule filterRule,
                                                ITcMachineScoreStrategy scoreStrategy,
                                                TcScheduleContext context, String taskSource) {
        BigDecimal originalToolOverflowQty = nvl(task.getToolOverflowQty());
        this.finalizeSelectedTaskPlan(task, selectedCandidate, context, taskSource);
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
            // 本任务先以0计划量结算当班成型消耗，释放结果只能供后续任务或顺延任务使用。
            this.settleAssignedTaskToolState(task, context, taskSource);
            this.appendCarryoverQty(task, selectedCandidate, firstShiftCandidates, filterRule, scoreStrategy,
                    context, toolOverflowQty, BigDecimal.ZERO, toolOverflowQty, startShiftOrder, false);
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
                    task.setPlanQty(assignedQty);
                }
                this.settleAssignedTaskToolState(task, context, taskSource);
                this.addContextTask(context, task);
                context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                this.addCapacitySplitTrace(context, task, currentShiftPlanQty, assignedQty, capacityOverflowQty,
                        remainCapacity, runtimeCandidate.getMachineCode(), true);
                this.bindSmallGlueMachine(context, task, runtimeCandidate.getMachineCode(),
                        this.resolveSmallGlueBoundMachine(task, context), null);
                this.addAssignTrace(context, task, TcScheduleRuleResultEnum.PASS,
                        runtimeCandidate.getMachineCode(), null, null);
                this.taskChainScheduleService.appendAutoTask(task, runtimeCandidate, context);
                this.appendCapacityDeductProcessLog(context, task, runtimeCandidate, currentShiftPlanQty, assignedQty,
                        capacityOverflowQty, capacityOverflowQty.compareTo(BigDecimal.ZERO) > 0
                                ? "当前班产能受限拆分" : "当前班选中机台承接");
            }
        }

        BigDecimal carryoverQty = capacityOverflowQty.add(toolOverflowQty);
        if (carryoverQty.compareTo(BigDecimal.ZERO) > 0) {
            this.appendCarryoverQty(task, selectedCandidate, firstShiftCandidates, filterRule, scoreStrategy, context,
                    carryoverQty, capacityOverflowQty, toolOverflowQty, startShiftOrder, false);
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
                BigDecimal capacityAllowedQty = sourcePlanQty.min(nvl(runtimeCandidate.getRemainCapacity()));
                BigDecimal assignedQty = this.limitPlanQtyByCurrentTool(
                        sourceTask, context, capacityAllowedQty, "提前补产");
                if (assignedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TcTaskDraft earlyFillTask = this.copyFutureEarlyFillTask(sourceTask, targetShiftOrder, assignedQty,
                        sourceShiftOrder, earlyFillIndex, runtimeCandidate.getMachineCode());
                this.applyCapacitySplitResult(earlyFillTask, sourcePlanQty, assignedQty,
                        nvl(runtimeCandidate.getRemainCapacity()), nvl(runtimeCandidate.getMachineSpeed()), "后续班次提前补产");
                this.settleAssignedTaskToolState(earlyFillTask, context, "提前补产");
                this.addContextTask(context, earlyFillTask);
                context.getCandidateTraceMap().put(earlyFillTask.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                this.addFutureEarlyFillTrace(context, earlyFillTask, sourceTask, sourceShiftOrder, targetShiftOrder,
                        runtimeCandidate.getMachineCode(), assignedQty, sourcePlanQty.subtract(assignedQty),
                        nvl(runtimeCandidate.getRemainCapacity()), runtimeCandidate.getScore());
            this.addAssignTrace(context, earlyFillTask, TcScheduleRuleResultEnum.PASS,
                    runtimeCandidate.getMachineCode(), null, null);
                this.taskChainScheduleService.appendAutoTask(earlyFillTask, runtimeCandidate, context);
                this.appendCapacityDeductProcessLog(context, earlyFillTask, runtimeCandidate, sourcePlanQty, assignedQty,
                        sourcePlanQty.subtract(assignedQty), "后续班次提前补产");
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
                                    Integer sourceShiftOrder, boolean capacityBlockedCarryover) {
        BigDecimal remainingQty = nvl(carryoverQty);
        BigDecimal sameShiftCapacityQty = nvl(capacityOverflowQty);
        String sourceType = capacityBlockedCarryover
                ? (nvl(capacityOverflowQty).compareTo(BigDecimal.ZERO) > 0
                ? "CAPACITY_BLOCKED_CARRYOVER" : "MACHINE_SHIFT_BLOCKED_CARRYOVER")
                : this.resolveCarryoverSourceType(capacityOverflowQty, toolOverflowQty);
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
                        ? this.resolveRemainCapacity(capacityProbeTask, context, runtimeCandidate, machineSpeed)
                        : this.resolveRemainCapacityWithoutNewSwitch(capacityProbeTask, context, runtimeCandidate,
                                machineSpeed));
                BigDecimal remainCapacity = nvl(runtimeCandidate.getRemainCapacity());
                if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal beforeAssignQty = remainingQty;
                BigDecimal capacityAllowedQty = candidateAssignableQty.min(remainCapacity);
                BigDecimal assignedQty = this.limitPlanQtyByCurrentTool(
                        sourceTask, context, capacityAllowedQty,
                        mergeTarget == null ? "顺延新建" : "顺延合并");
                BigDecimal overflowQty = remainingQty.subtract(assignedQty);
                if (assignedQty.compareTo(capacityAllowedQty) < 0) {
                    toolOverflowQty = remainingQty.max(nvl(toolOverflowQty));
                    sourceType = this.resolveCarryoverSourceType(capacityOverflowQty, toolOverflowQty);
                }
                if (assignedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (mergeTarget != null) {
                    BigDecimal beforeMergeQty = nvl(mergeTarget.getPlanQty());
                    BigDecimal afterMergeQty = beforeMergeQty.add(assignedQty);
                    this.taskChainScheduleService.changeQty(mergeTarget.getBusinessKey(), afterMergeQty, shiftOrder, context);
                    mergeTarget.setPlanQty(afterMergeQty);
                    this.applyCarryoverMergeToolState(mergeTarget, assignedQty, context);
                    if (capacityBlockedCarryover) {
                        mergeTarget.setCalcFormulaDesc(this.appendFormulaDesc(mergeTarget.getCalcFormulaDesc(),
                                "当前班无可用承接，后续班承接"));
                    }
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
                    this.appendCapacityDeductProcessLog(context, mergeTarget, runtimeCandidate, beforeAssignQty, assignedQty,
                            overflowQty, "顺延量合并承接");
                } else {
                    TcTaskDraft overflowTask = this.copyOverflowTask(sourceTask, shiftOrder, assignedQty,
                            sourceShiftOrder, overflowIndex, candidate.getMachineCode());
                    this.applyCapacitySplitResult(overflowTask, beforeAssignQty, assignedQty, remainCapacity,
                            machineSpeed, "顺延量承接");
                    if (capacityBlockedCarryover) {
                        overflowTask.setCalcFormulaDesc(this.appendFormulaDesc(overflowTask.getCalcFormulaDesc(),
                                "当前班无可用承接，后续班承接"));
                    }
                    this.settleAssignedTaskToolState(overflowTask, context, "顺延新建");
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
                    // 顺延任务按来源任务的生成顺序追加，避免后生成任务反向插到目标班次链首。
                    this.taskChainScheduleService.appendAutoTask(overflowTask, runtimeCandidate, context);
                    this.appendCapacityDeductProcessLog(context, overflowTask, runtimeCandidate, beforeAssignQty, assignedQty,
                            overflowQty, "顺延量承接");
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
            unplannedTask.setPlanQty(remainingQty);
            unplannedTask.setShiftOrder(TcScheduleConstants.TC_MAX_SHIFT_ORDER);
            unplannedTask.setMachineCode(null);
            TcUnplannedReasonEnum unplannedReason = nvl(toolOverflowQty).compareTo(BigDecimal.ZERO) > 0
                    ? TcUnplannedReasonEnum.TOOL_NOT_ENOUGH : TcUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
            unplannedTask.setUnplannedReasonCode(unplannedReason.getCode());
            unplannedTask.setUnplannedReasonDesc(unplannedReason.getDesc());
            if (capacityBlockedCarryover) {
                unplannedTask.setCalcFormulaDesc(this.appendFormulaDesc(unplannedTask.getCalcFormulaDesc(),
                        "6班承接结束仍有剩余"));
            }
            this.addContextTask(context, unplannedTask);
            if (TcUnplannedReasonEnum.TOOL_NOT_ENOUGH.equals(unplannedReason)) {
                this.addToolLimitUnplannedTrace(context, unplannedTask, remainingQty, sourceShiftOrder);
            } else {
                this.addCapacityUnplannedTrace(context, unplannedTask, remainingQty);
            }
            this.addCarryoverTrace(context, unplannedTask, sourceTask, sourceType, remainingQty, sourceShiftOrder,
                    TcScheduleConstants.TC_MAX_SHIFT_ORDER, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, false);
            this.logCarryoverSummary(context, unplannedTask, sourceTask, sourceType, remainingQty,
                    capacityOverflowQty, toolOverflowQty, sourceShiftOrder,
                    TcScheduleConstants.TC_MAX_SHIFT_ORDER, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, remainingQty, false);
            this.addAssignTrace(context, unplannedTask, TcScheduleRuleResultEnum.REJECT, null,
                    unplannedReason.getCode(), unplannedReason.getDesc());
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
     * 按胎侧“优先排满一台”口径稳定排序候选机台。
     *
     * @param task       当前胎侧任务
     * @param candidates 已通过硬约束并完成评分的候选机台
     * @return 排序后的候选机台
     */
    private List<TcMachineCandidate> sortCandidates(TcTaskDraft task, List<TcMachineCandidate> candidates) {
        BigDecimal planQty = this.nvl(task == null ? null : task.getPlanQty());
        candidates.sort(Comparator
                .comparing((TcMachineCandidate candidate) -> this.canFillMachine(planQty, candidate),
                        Comparator.reverseOrder())
                .thenComparing(candidate -> this.calculateCapacityFillRatio(planQty, candidate),
                        Comparator.reverseOrder())
                .thenComparing(candidate -> this.calculateRemainingTaskQty(planQty, candidate))
                .thenComparing(candidate -> this.calculateUnusedMachineCapacity(planQty, candidate))
                .thenComparing(TcMachineCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TcMachineCandidate::getMachineCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return candidates;
    }

    /**
     * 判断任务能否排满候选机台当前剩余产能。
     *
     * @param planQty   当前任务剩余计划量
     * @param candidate 候选机台
     * @return true 表示当前任务量不小于机台剩余产能
     */
    private boolean canFillMachine(BigDecimal planQty, TcMachineCandidate candidate) {
        BigDecimal remainCapacity = this.nvl(candidate == null ? null : candidate.getRemainCapacity());
        return remainCapacity.compareTo(BigDecimal.ZERO) > 0 && planQty.compareTo(remainCapacity) >= 0;
    }

    /**
     * 计算候选机台容量填充率。
     *
     * @param planQty   当前任务剩余计划量
     * @param candidate 候选机台
     * @return 0到1之间的容量填充率
     */
    private BigDecimal calculateCapacityFillRatio(BigDecimal planQty, TcMachineCandidate candidate) {
        BigDecimal remainCapacity = this.nvl(candidate == null ? null : candidate.getRemainCapacity());
        if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return planQty.min(remainCapacity).divide(remainCapacity,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 计算当前候选机台承接后的任务剩余量。
     *
     * @param planQty   当前任务剩余计划量
     * @param candidate 候选机台
     * @return 任务剩余量
     */
    private BigDecimal calculateRemainingTaskQty(BigDecimal planQty, TcMachineCandidate candidate) {
        return planQty.subtract(this.nvl(candidate == null ? null : candidate.getRemainCapacity()))
                .max(BigDecimal.ZERO);
    }

    /**
     * 计算当前候选机台承接后的空闲产能。
     *
     * @param planQty   当前任务剩余计划量
     * @param candidate 候选机台
     * @return 空闲产能
     */
    private BigDecimal calculateUnusedMachineCapacity(BigDecimal planQty, TcMachineCandidate candidate) {
        return this.nvl(candidate == null ? null : candidate.getRemainCapacity()).subtract(planQty)
                .max(BigDecimal.ZERO);
    }

    /**
     * 当前班首段固定使用已经选中的机台，其余机台按评分顺序继续承接溢出量。
     *
     * @param candidates 当前班已通过过滤和评分的候选机台
     * @param selectedCandidate 已选中候选机台
     * @return 首选机台排在第一位的候选机台列表
     */
    private List<TcMachineCandidate> sortCandidatesWithSelectedFirst(TcTaskDraft task,
                                                                     List<TcMachineCandidate> candidates,
                                                                     TcMachineCandidate selectedCandidate) {
        List<TcMachineCandidate> sortedCandidates = this.sortCandidates(task,
                new ArrayList<>(Optional.ofNullable(candidates).orElse(Collections.emptyList())));
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
     * 在机台确认后按损耗率、最小起排、卷曲取整和工装限制结算最终计划量。
     *
     * @param task 当前任务
     * @param selectedCandidate 已选中机台
     * @param context 排程上下文
     */
    private void finalizeSelectedTaskPlan(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                          TcScheduleContext context, String taskSource) {
        BigDecimal preLossPlanQty = task.getPreLossPlanQty() == null ? nvl(task.getPlanQty()) : nvl(task.getPreLossPlanQty());
        TcLossRuleMatchResult matchResult = this.lossRateResolver.resolve(context.getLossRuleList(), task.getSidewallCode(), selectedCandidate == null ? task.getMachineCode() : selectedCandidate.getMachineCode());
        BigDecimal resolvedLossRate = matchResult == null || matchResult.getLossRate() == null
                ? this.resolveFallbackLossRate(context, task) : nvl(matchResult.getLossRate());
        boolean tailTask = this.isTailTask(task);
        BigDecimal originalTailAdjustQty = nvl(task.getTailRoundAdjustQty());
        BigDecimal curlLength = this.resolveCurlLength(task);
        SchedulePlanQtyAdjustmentResult planQtyAdjustmentResult = this.constraintCalculator.calculatePlanQtyAfterLoss(
                preLossPlanQty, resolvedLossRate, task.getMinStartQty(), curlLength, tailTask,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE);
        BigDecimal planQtyAfterStandardRules = planQtyAdjustmentResult.getFinalPlanQty();
        BigDecimal startupPlanQtyLimit = this.resolveStartupPlanQtyLimit(context, task);
        BigDecimal planQtyBeforeToolLimit = this.applyStartupFinalPlanQtyLimit(
                context, task, planQtyAfterStandardRules);
        boolean startupThresholdAdjusted = planQtyBeforeToolLimit.compareTo(planQtyAfterStandardRules) < 0;
        BigDecimal currentAvailableToolQty = this.resolveCurrentAvailableToolQty(context, task);
        BigDecimal originalToolLimitAdjustQty = nvl(task.getToolLimitAdjustQty());
        BigDecimal originalToolOverflowQty = nvl(task.getToolOverflowQty());
        BigDecimal finalPlanQty = planQtyBeforeToolLimit;
        BigDecimal toolLimitAdjustQty = BigDecimal.ZERO;
        BigDecimal toolOverflowQty = BigDecimal.ZERO;
        if (currentAvailableToolQty != null && curlLength.compareTo(BigDecimal.ZERO) > 0) {
            finalPlanQty = this.limitPlanQtyByCurrentTool(task, context, planQtyBeforeToolLimit, taskSource);
            toolLimitAdjustQty = finalPlanQty.subtract(planQtyBeforeToolLimit)
                    .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                            java.math.RoundingMode.HALF_UP);
            toolOverflowQty = planQtyBeforeToolLimit.subtract(finalPlanQty).max(BigDecimal.ZERO)
                    .setScale(TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                            java.math.RoundingMode.HALF_UP);
        } else {
            this.limitPlanQtyByCurrentTool(task, context, planQtyBeforeToolLimit, taskSource);
            if (originalToolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
                toolLimitAdjustQty = originalToolLimitAdjustQty;
                toolOverflowQty = originalToolOverflowQty;
            }
        }
        task.setAvailableToolQty(currentAvailableToolQty);
        task.setResolvedLossRate(resolvedLossRate);
        task.setLossMatchLevel(matchResult == null
                ? (task.getLossRate() == null
                ? (this.hasConfiguredLossRate(context)
                ? TcLossMatchLevelEnum.PARAM_DEFAULT.getCode() : TcLossMatchLevelEnum.NONE.getCode())
                : TcLossMatchLevelEnum.LEGACY_TASK.getCode())
                : matchResult.getMatchLevel());
        task.setLossMatchSource(this.buildLossMatchSource(task, selectedCandidate, matchResult,
                task.getLossMatchLevel()));
        task.setPreLossPlanQty(planQtyAdjustmentResult.getPreLossPlanQty());
        task.setLossAddQty(planQtyAdjustmentResult.getLossAddQty());
        task.setMinStartAdjustQty(startupThresholdAdjusted
                ? BigDecimal.ZERO : planQtyAdjustmentResult.getMinStartAdjustQty());
        task.setTailRoundAdjustQty(startupThresholdAdjusted
                ? planQtyBeforeToolLimit.subtract(nvl(task.getBaseDemandQty()))
                .subtract(planQtyAdjustmentResult.getLossAddQty())
                : (tailTask ? originalTailAdjustQty : planQtyAdjustmentResult.getRoundAdjustQty()));
        task.setPlanQtyBeforeToolLimit(planQtyBeforeToolLimit);
        task.setToolLimitAdjustQty(toolLimitAdjustQty);
        task.setToolOverflowQty(toolOverflowQty);
        task.setPlanQty(finalPlanQty);
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(finalPlanQty).subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        task.setCalcFormulaDesc(this.buildFinalPlanCalcFormulaDesc(task.getCalcFormulaDesc(), tailTask));
        if (startupPlanQtyLimit != null) {
            task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), "开产阈值封顶"));
            Map<String, Object> startupEvidence = new LinkedHashMap<>();
            startupEvidence.put("phase", "MACHINE_FINALIZE");
            startupEvidence.put("shiftOrder", task.getShiftOrder());
            startupEvidence.put("currentShiftDemandQty", task.getCurrentShiftDemandQty());
            startupEvidence.put("rollingStockQty", task.getRollingStockQty());
            startupEvidence.put("supplyHours", task.getSupplyHours());
            startupEvidence.put("threshold", this.resolveStartupThreshold(context));
            startupEvidence.put("planQtyAfterStandardRules", planQtyAfterStandardRules);
            startupEvidence.put("planQtyLimit", startupPlanQtyLimit);
            startupEvidence.put("finalPlanQtyBeforeToolLimit", planQtyBeforeToolLimit);
            traceOf(context, task).addRuleHit(TcScheduleRuleCodeEnum.STARTUP_THRESHOLD_ADJUST,
                    startupThresholdAdjusted ? TcScheduleRuleResultEnum.PASS : TcScheduleRuleResultEnum.SKIP,
                    startupEvidence);
        }
        if (toolOverflowQty.compareTo(BigDecimal.ZERO) > 0) {
            task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), "工装限制"));
        }
    }

    /**
     * 计算开产班次最终计划量上限。
     *
     * @param context 排程上下文
     * @param task    当前任务
     * @return 开产上限；非开产班次或当班需求量非正数时返回null
     */
    private BigDecimal resolveStartupPlanQtyLimit(TcScheduleContext context, TcTaskDraft task) {
        if (!this.isStartupShift(context, task)
                || nvl(task.getCurrentShiftDemandQty()).compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return nvl(task.getCurrentShiftDemandQty()).multiply(this.resolveStartupThreshold(context))
                .subtract(nvl(task.getRollingStockQty())).max(BigDecimal.ZERO);
    }

    /**
     * 对机台确认后的标准计划量应用开产硬上限。
     *
     * @param context                   排程上下文
     * @param task                      当前任务
     * @param planQtyAfterStandardRules 损耗、最小起排和卷曲取整后的数量
     * @return 开产封顶后的数量；不满足开产条件时返回原数量
     */
    BigDecimal applyStartupFinalPlanQtyLimit(TcScheduleContext context, TcTaskDraft task,
                                             BigDecimal planQtyAfterStandardRules) {
        BigDecimal startupPlanQtyLimit = this.resolveStartupPlanQtyLimit(context, task);
        return startupPlanQtyLimit == null ? nvl(planQtyAfterStandardRules)
                : nvl(planQtyAfterStandardRules).min(startupPlanQtyLimit);
    }

    /**
     * 读取有效开产阈值，缺失、非法或非正数时回退默认值1。
     *
     * @param context 排程上下文
     * @return 有效开产阈值
     */
    private BigDecimal resolveStartupThreshold(TcScheduleContext context) {
        String value = this.resolveParamValue(context, TcScheduleConstants.PARAM_OPEN_SHIFT_THRESHOLD,
                TcScheduleConstants.DEFAULT_OPEN_SHIFT_THRESHOLD);
        try {
            BigDecimal threshold = new BigDecimal(value);
            return threshold.compareTo(BigDecimal.ZERO) > 0 ? threshold : BigDecimal.ONE;
        } catch (NumberFormatException exception) {
            return BigDecimal.ONE;
        }
    }

    /**
     * 判断当前任务是否按收尾计划量口径结算。
     *
     * @param task 当前任务
     * @return 收尾标识、收尾余量和标准长度均有效时返回true
     */
    private boolean isTailTask(TcTaskDraft task) {
        return task != null
                && TcYesNoEnum.YES.getCode().equals(task.getTailFlag())
                && this.nvl(task.getTailBalanceQty()).compareTo(BigDecimal.ZERO) > 0
                && this.nvl(task.getSidewallLength()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 构建机台确认后的最终计划量公式说明。
     *
     * @param currentFormulaDesc 派机前公式说明
     * @param tailTask 是否收尾任务
     * @return 最终计划量公式说明
     */
    private String buildFinalPlanCalcFormulaDesc(String currentFormulaDesc, boolean tailTask) {
        if (tailTask) {
            return this.appendFormulaDesc(currentFormulaDesc, "机台确认损耗");
        }
        if ("基础需求->库存抵扣->派机前最小起排与卷数取整估算".equals(currentFormulaDesc)) {
            return "基础需求->库存抵扣->机台确认损耗->最小起排->卷数取整";
        }
        return this.appendFormulaDesc(currentFormulaDesc, "机台确认损耗->最小起排->卷数取整");
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
     * 使用生产前工装余额限制当前入口的实际承接量，并记录完整代入式。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @param requestedQty 本入口请求承接量
     * @param taskSource 任务来源
     * @return 工装允许的实际承接量；未启用约束或卷曲长度无效时返回请求量
     */
    private BigDecimal limitPlanQtyByCurrentTool(TcTaskDraft task, TcScheduleContext context,
                                                 BigDecimal requestedQty, String taskSource) {
        BigDecimal normalizedRequestedQty = nvl(requestedQty).max(BigDecimal.ZERO);
        BigDecimal currentAvailableToolQty = this.resolveCurrentAvailableToolQty(context, task);
        if (currentAvailableToolQty == null) {
            context.appendDeferredShiftProcessLog(task.getShiftOrder(),
                    "工装生产前校验：来源={0}，胎侧代码={1}，请求量={2}米；总工装未配置或非正数，未启用工装约束，实际承接量={2}米。",
                    taskSource, task.getSidewallCode(), normalizedRequestedQty);
            return normalizedRequestedQty;
        }
        BigDecimal curlLength = this.resolveCurlLength(task);
        if (curlLength.compareTo(BigDecimal.ZERO) <= 0) {
            context.appendDeferredShiftProcessLog(task.getShiftOrder(),
                    "工装生产前校验：来源={0}，胎侧代码={1}，请求量={2}米；规格卷曲长度及默认卷曲长度均无效，无法计算工装允许量，沿用请求量。",
                    taskSource, task.getSidewallCode(), normalizedRequestedQty);
            return normalizedRequestedQty;
        }
        ScheduleToolLedgerResult limitResult = this.constraintCalculator
                .settleProductionBeforeReleaseToolLedger(normalizedRequestedQty, BigDecimal.ZERO,
                        currentAvailableToolQty, task.getTotalToolQty(), curlLength);
        context.appendDeferredShiftProcessLog(task.getShiftOrder(),
                "工装生产前校验：来源={0}，胎侧代码={1}，校验前可用工装={2}套，请求量={3}米，有效卷曲长度={4}米/套；允许量=min({3}米,max({2}套,0)×{4}米/套)={5}米；工装溢出量=max({3}米-{5}米,0)={6}米；实际承接量={5}米。",
                taskSource, task.getSidewallCode(), currentAvailableToolQty, normalizedRequestedQty, curlLength,
                limitResult.getAllowedPlanQty(), limitResult.getOverflowPlanQty());
        return limitResult.getAllowedPlanQty();
    }

    /**
     * 在任务实际落到班次机台后结算工装占用和剩余工装。
     *
     * @param task 当前任务
     * @param context 排程上下文
     * @param taskSource 任务来源
     */
    private void settleAssignedTaskToolState(TcTaskDraft task, TcScheduleContext context, String taskSource) {
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
            task.setToolLedgerOrder(context.nextToolLedgerOrder());
            context.setCurrentAvailableToolQty(currentAvailableToolQty);
            this.recordToolLedgerSnapshot(context, task, currentAvailableToolQty, task.getToolUsedQty(), currentAvailableToolQty);
            return;
        }
        ScheduleToolLedgerResult ledgerResult = this.constraintCalculator.settleProductionBeforeReleaseToolLedger(
                task.getPlanQty(), task.getCurrentShiftDemandQty(), currentAvailableToolQty,
                task.getTotalToolQty(), curlLength);
        task.setToolUsedQty(ledgerResult.getToolUsedQty());
        task.setRemainingToolQty(ledgerResult.getRemainingToolQty());
        task.setToolLedgerOrder(context.nextToolLedgerOrder());
        task.setPlanStockQty(nvl(task.getRollingStockQty()).add(nvl(task.getPlanQty()))
                .subtract(nvl(task.getCurrentShiftDemandQty())).max(BigDecimal.ZERO));
        context.setCurrentAvailableToolQty(ledgerResult.getRemainingToolQty());
        this.recordToolLedgerSnapshot(context, task, ledgerResult.getAvailableToolQty(), ledgerResult.getToolUsedQty(),
                ledgerResult.getRemainingToolQty());
        context.appendDeferredShiftProcessLog(task.getShiftOrder(),
                "工装任务后结算：来源={0}，胎侧代码={1}，净占用工装数量=({2}米-{3}米)÷{4}米/套={5}套；下一任务可用工装数量=min(max({6}套-{5}套,0),{7}套)={8}套。本任务释放量仅供下一任务使用。",
                taskSource, task.getSidewallCode(), nvl(task.getPlanQty()), nvl(task.getCurrentShiftDemandQty()),
                curlLength, ledgerResult.getToolUsedQty(), currentAvailableToolQty, task.getTotalToolQty(),
                ledgerResult.getRemainingToolQty());
    }

    /**
     * 保存任务本次工装结算快照，供过程日志按实际分配结果展示。
     *
     * @param context           胎侧排程上下文
     * @param task              已完成工装结算的任务
     * @param availableToolQty  结算前可用工装数量
     * @param toolUsedQty       本次净占用工装数量
     * @param remainingToolQty  结算后剩余工装数量
     */
    private void recordToolLedgerSnapshot(TcScheduleContext context, TcTaskDraft task,
                                          BigDecimal availableToolQty, BigDecimal toolUsedQty,
                                          BigDecimal remainingToolQty) {
        if (context == null || task == null || StrUtil.isBlank(task.getBusinessKey())) {
            return;
        }
        ScheduleToolLedgerSnapshot snapshot = new ScheduleToolLedgerSnapshot();
        snapshot.setAvailableToolQty(availableToolQty);
        snapshot.setToolUsedQty(toolUsedQty);
        snapshot.setRemainingToolQty(remainingToolQty);
        context.getToolLedgerSnapshotMap().put(task.getBusinessKey(), snapshot);
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
            mergeTarget.setToolLedgerOrder(context.nextToolLedgerOrder());
            context.setCurrentAvailableToolQty(currentAvailableToolQty);
            this.recordToolLedgerSnapshot(context, mergeTarget, currentAvailableToolQty,
                    mergeTarget.getToolUsedQty(), currentAvailableToolQty);
            return;
        }
        ScheduleToolLedgerResult ledgerResult = this.constraintCalculator.settleCommittedToolLedger(
                carryoverQty, BigDecimal.ZERO, currentAvailableToolQty,
                mergeTarget.getTotalToolQty(), curlLength);
        mergeTarget.setAvailableToolQty(currentAvailableToolQty);
        mergeTarget.setToolUsedQty(nvl(mergeTarget.getToolUsedQty()).add(ledgerResult.getToolUsedQty()));
        mergeTarget.setRemainingToolQty(ledgerResult.getRemainingToolQty());
        mergeTarget.setToolLedgerOrder(context.nextToolLedgerOrder());
        mergeTarget.setPlanStockQty(nvl(mergeTarget.getPlanStockQty()).add(nvl(carryoverQty)));
        context.setCurrentAvailableToolQty(ledgerResult.getRemainingToolQty());
        this.recordToolLedgerSnapshot(context, mergeTarget, ledgerResult.getAvailableToolQty(),
                mergeTarget.getToolUsedQty(), ledgerResult.getRemainingToolQty());
        context.appendDeferredShiftProcessLog(mergeTarget.getShiftOrder(),
                "工装任务后结算：来源=顺延合并，胎侧代码={0}，净占用工装数量=({1}米-0米)÷{2}米/套={3}套；下一任务可用工装数量=min(max({4}套-{3}套,0),{5}套)={6}套。",
                mergeTarget.getSidewallCode(), nvl(carryoverQty), curlLength, ledgerResult.getToolUsedQty(),
                currentAvailableToolQty, mergeTarget.getTotalToolQty(), ledgerResult.getRemainingToolQty());
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
     * @param lossMatchLevel 最终损耗匹配层级
     * @return 命中来源说明
     */
    private String buildLossMatchSource(TcTaskDraft task, TcMachineCandidate selectedCandidate,
                                        TcLossRuleMatchResult matchResult, String lossMatchLevel) {
        String machineCode = selectedCandidate == null ? task.getMachineCode() : selectedCandidate.getMachineCode();
        if (matchResult == null || matchResult.getMatchedRule() == null) {
            return "machineCode=" + machineCode + ",sidewallCode=" + task.getSidewallCode()
                    + ",lossMatchLevel=" + lossMatchLevel;
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
        target.setEmbryoCode(source.getEmbryoCode());
        target.setCxMachineCode(source.getCxMachineCode());
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
        target.setFormingGuardWindowQtyMap(source.getFormingGuardWindowQtyMap());
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
        // 汇总任务拆分、顺延和提前补产必须保留同一计划量汇总组及来源关系。
        target.setPlanGroupKey(source.getPlanGroupKey());
        target.setSourceTaskBusinessKeyList(source.getSourceTaskBusinessKeyList() == null
                ? null : new ArrayList<>(source.getSourceTaskBusinessKeyList()));
        target.setSourceExplainTask(Boolean.FALSE);
        target.setGroupSourceCount(source.getGroupSourceCount());
        target.setGroupRequiredQty(source.getGroupRequiredQty());
        target.setGroupBaseDemandQty(source.getGroupBaseDemandQty());
        target.setGroupMinStartAdjustQty(source.getGroupMinStartAdjustQty());
        target.setGroupRoundAdjustQty(source.getGroupRoundAdjustQty());
        target.setGroupFinalPlanQty(source.getGroupFinalPlanQty());
        // 顺延任务沿用来源任务的待排名次，保证过程日志与原任务处理顺序一致。
        target.setBaseSortIndex(source.getBaseSortIndex());
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
        BigDecimal normalizedAssignedQty = assignedQty;
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
        TcScheduleRuleCodeEnum ruleCode = "CAPACITY_BLOCKED_CARRYOVER".equals(sourceType)
                ? TcScheduleRuleCodeEnum.CAPACITY_BLOCKED_CARRYOVER : TcScheduleRuleCodeEnum.PLAN_QTY_CARRYOVER;
        traceOf(context, targetTask).addRuleHit(ruleCode,
                TcScheduleRuleResultEnum.PASS, evidence);
    }

    /**
     * 在任务进入机台任务链后记录实际产能扣减，并延后到机台评分日志之后渲染。
     *
     * @param context         胎侧排程上下文
     * @param task            已承接任务
     * @param candidate       承接机台运行态
     * @param beforeAssignQty 分配前待承接量
     * @param assignedQty     本次实际承接量
     * @param overflowQty     本次未承接的溢出量
     * @param splitDesc       承接或拆分说明
     */
    private void appendCapacityDeductProcessLog(TcScheduleContext context, TcTaskDraft task,
                                                TcMachineCandidate candidate, BigDecimal beforeAssignQty,
                                                BigDecimal assignedQty, BigDecimal overflowQty, String splitDesc) {
        Map<String, Object> evidence = candidate == null || candidate.getEvidence() == null
                ? Collections.emptyMap() : candidate.getEvidence();
        BigDecimal beforeRemainCapacity = candidate == null ? BigDecimal.ZERO : this.nvl(candidate.getRemainCapacity());
        BigDecimal afterRemainCapacity = beforeRemainCapacity.subtract(this.nvl(assignedQty)).max(BigDecimal.ZERO);
        BigDecimal currentSpecSwitchDeduct = this.nvl(task.getPreviousSpecSwitchHours())
                .multiply(this.nvl(task.getMachineSpeed()));
        BigDecimal currentGlueSwitchDeduct = this.nvl(task.getPreviousGlueSwitchCapacityDeduct());
        context.appendDeferredShiftProcessLog(task.getShiftOrder(), "产能扣减：胎侧代码={0}，机台={1}，班次={2}，最大产能={3}，检修扣减={4}，已排计划量扣减={5}，已发生切换扣减={6}，本次规格切换扣减={7}，本次胶料切换扣减={8}，分配前待承接量={9}，分配前剩余产能={10}，本次分配量={11}，分配后剩余产能={12}，溢出量={13}，拆分原因={14}",
                task.getSidewallCode(), candidate == null ? "未提供" : candidate.getMachineCode(), task.getShiftOrder(),
                this.getCandidateEvidenceDecimal(evidence, "maxCapacity"),
                this.getCandidateEvidenceDecimal(evidence, "maintenanceCapacityDeduct"),
                this.getCandidateEvidenceDecimal(evidence, "assignedPlanQty"),
                this.getCandidateEvidenceDecimal(evidence, "existingSwitchCapacityDeduct"),
                currentSpecSwitchDeduct, currentGlueSwitchDeduct, this.nvl(beforeAssignQty), beforeRemainCapacity,
                this.nvl(assignedQty), afterRemainCapacity, this.nvl(overflowQty), splitDesc);
        context.appendDeferredShiftFullProcessTrace(task.getShiftOrder(), new ScheduleProcessTraceEvent(
                "机台分配", task.getBusinessKey(), "机台产能即时扣减与拆分",
                "选中机台的班次容量账本、检修计划、已排任务、胎侧/垫胶共用机台约束和切换扣减。",
                "机台=" + (candidate == null ? "未提供" : candidate.getMachineCode()) + "，班次="
                        + task.getShiftOrder() + "，分配前待承接量=" + this.nvl(beforeAssignQty)
                        + "米（本轮之前该任务尚未被机台承接的余量），分配前剩余产能=" + beforeRemainCapacity + "米。",
                "分配前待承接量不是机台产能，而是本轮分配前该任务尚未被任何机台承接的计划量；首次承接取当前任务计划量，拆分、合并或顺延后取上一次分配后的剩余量。"
                        + "本次分配量取任务待承接量与满足胎侧/垫胶共用机台约束后的可用机台产能中的较小值；机台容量无效时按5500米回退。",
                "本轮待承接量=" + this.nvl(beforeAssignQty) + "米；本次分配量=min("
                        + this.nvl(beforeAssignQty) + ",本轮可用机台产能)=" + this.nvl(assignedQty)
                        + "米；溢出量=max(" + this.nvl(beforeAssignQty) + "-" + this.nvl(assignedQty)
                        + ",0)=" + this.nvl(overflowQty) + "米；规格切换扣减=" + currentSpecSwitchDeduct
                        + "米，胶料切换扣减=" + currentGlueSwitchDeduct + "米；" + splitDesc + "。",
                "本次分配=" + this.nvl(assignedQty) + "米，分配后剩余产能=" + afterRemainCapacity
                        + "米，溢出=" + this.nvl(overflowQty) + "米。",
                this.nvl(overflowQty).compareTo(BigDecimal.ZERO) > 0
                        ? "已分配片段进入机台任务链，溢出量进入同班其他机台或后续班次。"
                        : "已分配数量进入机台任务链并参与最终落库。"
        ));
    }

    /**
     * 获取候选机台证据中的数值，缺失时按零展示。
     *
     * @param evidence 机台证据
     * @param key      证据键
     * @return 非空数值
     */
    private BigDecimal getCandidateEvidenceDecimal(Map<String, Object> evidence, String key) {
        Object value = evidence.get(key);
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
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
        evidence.put("reasonCode", TcUnplannedReasonEnum.TOOL_NOT_ENOUGH.getCode());
        evidence.put("reasonDesc", TcUnplannedReasonEnum.TOOL_NOT_ENOUGH.getDesc());
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
            return this.sortCandidatesWithSelectedFirst(task, sortedCandidates, selectedCandidate);
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
        copy.setOpenShiftCodes(source.getOpenShiftCodes() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(source.getOpenShiftCodes()));
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
            // 候选级速度与容量证据仅在 DEBUG 输出，避免 INFO 随任务与机台笛卡尔积膨胀。
            log.debug("[TC_MACHINE_SPEED] sidewallCode={}, machineCode={}, 机台速度【{}】，来源={}",
                    task.getSidewallCode(), candidate.getMachineCode(), machineSpeed,
                    candidate.getSidewallSpeedMap().containsKey(task.getSidewallCode()) ? "胎侧规格速度" : "最大产能/班次小时数");
            log.debug("[TC_MACHINE_CAPACITY] sidewallCode={}, machineCode={}, shiftOrder={}, 最大产能【maxCapacity】-检修折算量【maintenanceDeduct】-已排计划量【assignedPlanQty】-已发生切换折算量【existingSwitchDeduct】-当前切换折算量【currentSwitchDeduct】=剩余产能【remainCapacity】",
                    task.getSidewallCode(), candidate.getMachineCode(), task.getShiftOrder());
            BigDecimal shiftMaintenanceHours = this.resolveMaintenanceHours(task, candidate);
            log.debug("[TC_MACHINE_CAPACITY_DETAIL] sidewallCode={}, machineCode={}, shiftOrder={}, maxCapacity={}, maintenanceHours={}, totalMaintenanceHours={}, machineSpeed={}, assignedPlanQty={}, previousGlueCode={}, currentGlueCode={}, glueChangeCapacityDeductParam={}, currentGlueSwitchCapacityDeduct={}, existingSwitchCapacityDeduct={}, currentSwitchCapacityDeduct={}, remainCapacity={}",
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
        if (speed != null && speed.compareTo(BigDecimal.ZERO) > 0) {
            return speed;
        }
        // 规格速度未命中时优先使用机台级速度，最后才按最大产能和班次时长推导。
        if (candidate.getMachineSpeed() != null
                && candidate.getMachineSpeed().compareTo(BigDecimal.ZERO) > 0) {
            return candidate.getMachineSpeed();
        }
        BigDecimal maxCapacity = this.resolveMachineMaxCapacity(candidate);
        BigDecimal shiftHours = nvl(context.getShiftHoursMap().get(task.getShiftOrder()));
        if (maxCapacity.compareTo(BigDecimal.ZERO) > 0 && shiftHours.compareTo(BigDecimal.ZERO) > 0) {
            return maxCapacity.divide(shiftHours, TcScheduleConstants.DECIMAL_CALCULATION_SCALE,
                    java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
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
        candidate.getEvidence().put("maxCapacity", this.nvl(maxCapacity));
        candidate.getEvidence().put("maintenanceCapacityDeduct", maintenanceDeduct);
        candidate.getEvidence().put("assignedPlanQty", assignedPlanQty);
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
        candidate.getEvidence().put("maxCapacity", this.nvl(maxCapacity));
        candidate.getEvidence().put("maintenanceCapacityDeduct", maintenanceDeduct);
        candidate.getEvidence().put("assignedPlanQty", assignedPlanQty);
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
        candidate.getEvidence().put("maxCapacity", this.nvl(maxCapacity));
        candidate.getEvidence().put("maintenanceCapacityDeduct", maintenanceDeduct);
        candidate.getEvidence().put("assignedPlanQty", assignedPlanQty);
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
     * 按任务旧值、工厂参数顺序解析无基础规则命中时的损耗率。
     *
     * @param context 排程上下文
     * @param task 当前任务
     * @return 百分比口径的非负损耗率
     */
    BigDecimal resolveFallbackLossRate(TcScheduleContext context, TcTaskDraft task) {
        if (task != null && task.getLossRate() != null) {
            return this.nvl(task.getLossRate()).max(BigDecimal.ZERO);
        }
        String value = this.resolveParamValue(context, TcScheduleConstants.PARAM_DEFAULT_LOSS_RATE,
                TcScheduleConstants.DEFAULT_LOSS_RATE);
        try {
            return new BigDecimal(value).max(BigDecimal.ZERO);
        } catch (NumberFormatException exception) {
            return new BigDecimal(TcScheduleConstants.DEFAULT_LOSS_RATE);
        }
    }

    /**
     * 判断本次排程是否存在有效的默认损耗率参数值。
     *
     * @param context 排程上下文
     * @return true表示参数快照中存在非空值
     */
    private boolean hasConfiguredLossRate(TcScheduleContext context) {
        if (context == null || context.getParamMap() == null) {
            return false;
        }
        TcParamValue paramValue = context.getParamMap().get(TcScheduleConstants.PARAM_DEFAULT_LOSS_RATE);
        return paramValue != null && TcParamValueSourceEnum.TABLE.getCode().equals(paramValue.getSource())
                && StrUtil.isNotBlank(paramValue.getEffectiveValue());
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
