package com.zlt.aps.tm.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleRuleResult;
import com.zlt.aps.common.engine.schedule.ScheduleScoreResult;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.enums.TmScheduleErrorCodeEnum;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.ITmMachineAssignService;
import com.zlt.aps.tm.engine.strategy.ITmMachineFilterRule;
import com.zlt.aps.tm.engine.strategy.ITmMachineScoreStrategy;
import com.zlt.aps.tm.engine.strategy.TmStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面机台分配默认步骤服务。
 *
 * <p>对已预置机台编码的任务直接追加到对应机台任务链；
 * 对未预置机台的任务执行候选机台过滤规则链和评分策略，选择最高分机台分配任务。
 * 全部候选机台被过滤时按过滤原因归类未排原因（产能不足→CAPACITY_NOT_ENOUGH 等）。</p>
 */
@Slf4j
@Service
public class TmMachineAssignService implements ITmMachineAssignService {

    /** 默认机台过滤规则编码 */
    private static final String DEFAULT_FILTER_RULE_CODE = "DEFAULT";

    /** 默认机台评分策略编码 */
    private static final String DEFAULT_SCORE_STRATEGY_CODE = "DEFAULT";

    private final TmTaskChainScheduleService taskChainScheduleService;

    private final TmStrategyRegistry strategyRegistry;

    /**
     * 创建默认机台分配步骤服务。
     *
     * @param taskChainScheduleService 任务链排程服务
     * @param strategyRegistry         胎面策略注册表
     */
    public TmMachineAssignService(TmTaskChainScheduleService taskChainScheduleService,
                                  TmStrategyRegistry strategyRegistry) {
        this.taskChainScheduleService = taskChainScheduleService;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void assign(TmScheduleContext context) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        for (TmTaskDraft task : new ArrayList<>(context.getTaskDraftList())) {
            if (task.isUnassigned()) {
                // 未预置机台的任务走完整过滤评分流程
                assignByFilterAndScore(task, context);
                continue;
            }
            // 已预置机台的任务直接追加到对应机台任务链
            TmMachineCandidate candidate = new TmMachineCandidate();
            candidate.setMachineCode(task.getMachineCode());
            context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(candidate));
            addAssignTrace(context, task, "PASS", task.getMachineCode(), null, null);
            this.taskChainScheduleService.appendAutoTask(task, candidate, context);
        }
    }

    /**
     * 对未预置机台的任务执行候选机台过滤和评分，选择最高分机台分配任务。
     *
     * @param task    待排任务草稿
     * @param context 胎面排程上下文
     */
    private void assignByFilterAndScore(TmTaskDraft task, TmScheduleContext context) {
        List<TmMachineCandidate> candidateList = context.getMachineCandidateList();
        if (CollUtil.isEmpty(candidateList)) {
            log.warn("[TM_MACHINE_ASSIGN] 工厂无可用机台候选列表，任务[{}]标记无可用机台", task.getBusinessKey());
            context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.emptyList());
            addAssignTrace(context, task, "REJECT", null, TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode(),
                    TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getDesc());
            markNoAvailableMachine(task);
            return;
        }

        // 构建机台规则上下文
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);

        // 获取过滤规则和评分策略
        ITmMachineFilterRule filterRule = strategyRegistry.getMachineFilterRule(DEFAULT_FILTER_RULE_CODE);
        ITmMachineScoreStrategy scoreStrategy = strategyRegistry.getMachineScoreStrategy(DEFAULT_SCORE_STRATEGY_CODE);

        // 复制候选机台列表，并按当前任务动态补齐口型、胶料、定点/禁排和剩余产能判断。
        List<TmMachineCandidate> candidates = copyCandidates(candidateList);
        prepareCandidatesForTask(task, context, candidates);

        // 执行过滤规则链，记录被过滤的候选机台
        List<TmMachineCandidate> passedCandidates = new ArrayList<>();
        for (TmMachineCandidate candidate : candidates) {
            ScheduleRuleResult ruleResult = filterRule.evaluate(candidate, ruleContext);
            if (ruleResult.isPassed()) {
                passedCandidates.add(candidate);
                addFilterTrace(context, task, candidate, "PASS", null, null);
            } else {
                addFilterTrace(context, task, candidate, "REJECT", ruleResult.getReasonCode(), ruleResult.getReasonDesc());
                log.debug("[TM_MACHINE_ASSIGN] 任务[{}]机台[{}]被过滤，原因={}{}",
                        task.getBusinessKey(), candidate.getMachineCode(),
                        ruleResult.getReasonCode(), ruleResult.getReasonDesc());
            }
        }

        // 全部候选机台被过滤，按过滤原因归类未排原因
        if (passedCandidates.isEmpty()) {
            TmUnplannedReasonEnum unplannedReason = this.resolveUnplannedReasonFromCandidates(candidates);
            log.info("[TM_MACHINE_ASSIGN] 任务[{}]所有候选机台均被过滤，未排原因={}",
                    task.getBusinessKey(), unplannedReason.getCode());
            context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);
            this.addAssignTrace(context, task, "REJECT", null, unplannedReason.getCode(), unplannedReason.getDesc());
            this.markUnplanned(task, unplannedReason);
            return;
        }

        // 对通过过滤的候选机台执行评分
        for (TmMachineCandidate candidate : passedCandidates) {
            ScheduleScoreResult scoreResult = scoreStrategy.score(candidate, ruleContext);
            addScoreTrace(context, task, candidate, scoreResult);
            log.debug("[TM_MACHINE_ASSIGN] 任务[{}]机台[{}]评分={}",
                    task.getBusinessKey(), candidate.getMachineCode(), scoreResult.getTotalScore());
        }
        context.getCandidateTraceMap().put(task.getBusinessKey(), candidates);

        // 按评分降序选择最高分机台，同分按机台编码升序排序保证稳定
        passedCandidates.sort(Comparator
                .comparing(TmMachineCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TmMachineCandidate::getMachineCode, Comparator.nullsLast(Comparator.naturalOrder())));
        TmMachineCandidate bestCandidate = passedCandidates.get(0);
        log.info("[TM_MACHINE_ASSIGN] 任务[{}]选中机台[{}]，评分={}",
                task.getBusinessKey(), bestCandidate.getMachineCode(), bestCandidate.getScore());
        addAssignTrace(context, task, "PASS", bestCandidate.getMachineCode(), null, null);

        // 按同班次匹配机台优先拆分任务，当前班全部匹配机台不足时再滚动到后续班次。
        this.appendTaskWithCapacityOverflow(task, bestCandidate, passedCandidates, filterRule, scoreStrategy, context);
    }

    /**
     * 按同班次匹配机台优先拆分并追加任务。
     *
     * <p>首段仍使用本轮评分选中的机台；当该机台当前班剩余产能不足时，
     * 剩余量先在当前班其他匹配机台中继续分配。当前班所有匹配机台均无剩余产能后，
     * 再滚动到下一班并重复同样逻辑，最多滚动到六班。</p>
     *
     * @param task 当前待排任务
     * @param selectedCandidate 已选中候选机台
     * @param firstShiftCandidates 当前班已通过过滤和评分的候选机台
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @param context 胎面排程上下文
     */
    private void appendTaskWithCapacityOverflow(TmTaskDraft task, TmMachineCandidate selectedCandidate,
                                                List<TmMachineCandidate> firstShiftCandidates,
                                                ITmMachineFilterRule filterRule,
                                                ITmMachineScoreStrategy scoreStrategy,
                                                TmScheduleContext context) {
        BigDecimal remainingQty = nvl(task.getPlanQty());
        Integer startShiftOrder = task.getShiftOrder() == null ? 1 : task.getShiftOrder();
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            this.appendZeroPlanTask(task, selectedCandidate, context, startShiftOrder);
            return;
        }
        boolean plannedAny = false;
        int overflowIndex = 1;
        for (int shiftOrder = startShiftOrder; shiftOrder <= 6 && remainingQty.compareTo(BigDecimal.ZERO) > 0; shiftOrder++) {
            List<TmMachineCandidate> shiftCandidates = this.resolveShiftAssignableCandidates(task, context, filterRule,
                    scoreStrategy, firstShiftCandidates, selectedCandidate, shiftOrder, remainingQty, startShiftOrder,
                    plannedAny, overflowIndex);
            for (TmMachineCandidate candidate : shiftCandidates) {
                if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                TmTaskDraft currentTask = plannedAny
                        ? this.copyOverflowTask(task, shiftOrder, remainingQty, startShiftOrder, overflowIndex,
                        candidate.getMachineCode()) : task;
                currentTask.setShiftOrder(shiftOrder);
                BigDecimal machineSpeed = this.resolveMachineSpeed(currentTask, candidate);
                TmMachineCandidate runtimeCandidate = this.copyCandidate(candidate);
                runtimeCandidate.setMachineSpeed(machineSpeed);
                runtimeCandidate.setRemainCapacity(this.resolveRemainCapacity(currentTask, context, runtimeCandidate,
                        machineSpeed));
                BigDecimal remainCapacity = nvl(runtimeCandidate.getRemainCapacity());
                if (remainCapacity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal beforeAssignQty = remainingQty;
                BigDecimal assignedQty = remainingQty.min(remainCapacity);
                BigDecimal overflowQty = remainingQty.subtract(assignedQty);
                this.applyCapacitySplitResult(currentTask, beforeAssignQty, assignedQty, remainCapacity, machineSpeed,
                        Objects.equals(shiftOrder, startShiftOrder) ? "同班次匹配机台承接" : "产能不足顺延到后续班次承接");
                this.addContextTask(context, currentTask);
                context.getCandidateTraceMap().put(currentTask.getBusinessKey(), Collections.singletonList(runtimeCandidate));
                this.addCapacitySplitTrace(context, currentTask, beforeAssignQty, assignedQty, overflowQty,
                        remainCapacity, runtimeCandidate.getMachineCode(), Objects.equals(shiftOrder, startShiftOrder));
                this.addAssignTrace(context, currentTask, "PASS", runtimeCandidate.getMachineCode(), null, null);
                this.taskChainScheduleService.appendAutoTask(currentTask, runtimeCandidate, context);
                plannedAny = true;
                remainingQty = overflowQty;
                overflowIndex++;
            }
        }
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            TmTaskDraft unplannedTask = plannedAny
                    ? this.copyOverflowTask(task, 6, remainingQty, startShiftOrder, overflowIndex, "UNPLANNED") : task;
            unplannedTask.setPlanQty(remainingQty);
            unplannedTask.setShiftOrder(6);
            unplannedTask.setMachineCode(null);
            unplannedTask.setUnplannedReasonCode(TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode());
            unplannedTask.setUnplannedReasonDesc(TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
            this.addContextTask(context, unplannedTask);
            this.addCapacityUnplannedTrace(context, unplannedTask, remainingQty);
            this.addAssignTrace(context, unplannedTask, "REJECT", null,
                    TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode(),
                    TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
        }
    }

    /**
     * 获取指定班次可承接当前剩余计划量的候选机台列表。
     *
     * @param sourceTask 来源任务
     * @param context 排程上下文
     * @param filterRule 机台过滤规则
     * @param scoreStrategy 机台评分策略
     * @param firstShiftCandidates 当前班已评分候选机台
     * @param selectedCandidate 首选机台
     * @param shiftOrder 当前处理班次
     * @param remainingQty 当前剩余待排量
     * @param startShiftOrder 来源班次
     * @param plannedAny 是否已经生成过分配段
     * @param overflowIndex 当前拆分序号
     * @return 已排序候选机台列表
     */
    private List<TmMachineCandidate> resolveShiftAssignableCandidates(TmTaskDraft sourceTask, TmScheduleContext context,
                                                                      ITmMachineFilterRule filterRule,
                                                                      ITmMachineScoreStrategy scoreStrategy,
                                                                      List<TmMachineCandidate> firstShiftCandidates,
                                                                      TmMachineCandidate selectedCandidate,
                                                                      Integer shiftOrder,
                                                                      BigDecimal remainingQty,
                                                                      Integer startShiftOrder,
                                                                      boolean plannedAny,
                                                                      int overflowIndex) {
        if (Objects.equals(shiftOrder, startShiftOrder) && CollUtil.isNotEmpty(firstShiftCandidates)) {
            return this.sortCandidatesWithSelectedFirst(firstShiftCandidates, selectedCandidate);
        }
        TmTaskDraft probeTask = plannedAny
                ? this.copyOverflowTask(sourceTask, shiftOrder, remainingQty, startShiftOrder, overflowIndex, "PROBE")
                : sourceTask;
        probeTask.setShiftOrder(shiftOrder);
        probeTask.setPlanQty(remainingQty);
        return this.buildPassedAndScoredCandidates(probeTask, context, filterRule, scoreStrategy);
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
    private List<TmMachineCandidate> buildPassedAndScoredCandidates(TmTaskDraft task, TmScheduleContext context,
                                                                    ITmMachineFilterRule filterRule,
                                                                    ITmMachineScoreStrategy scoreStrategy) {
        List<TmMachineCandidate> candidates = copyCandidates(context.getMachineCandidateList());
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        prepareCandidatesForTask(task, context, candidates);
        TmMachineRuleContext ruleContext = new TmMachineRuleContext();
        ruleContext.setTaskDraft(task);
        ruleContext.setScheduleContext(context);
        List<TmMachineCandidate> passedCandidates = new ArrayList<>();
        for (TmMachineCandidate candidate : candidates) {
            ScheduleRuleResult ruleResult = filterRule.evaluate(candidate, ruleContext);
            if (ruleResult.isPassed()) {
                passedCandidates.add(candidate);
                addFilterTrace(context, task, candidate, "PASS", null, null);
            } else {
                addFilterTrace(context, task, candidate, "REJECT", ruleResult.getReasonCode(), ruleResult.getReasonDesc());
            }
        }
        for (TmMachineCandidate candidate : passedCandidates) {
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
    private List<TmMachineCandidate> sortCandidates(List<TmMachineCandidate> candidates) {
        candidates.sort(Comparator
                .comparing(TmMachineCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TmMachineCandidate::getMachineCode, Comparator.nullsLast(Comparator.naturalOrder())));
        return candidates;
    }

    /**
     * 当前班首段固定使用已经选中的机台，其余机台按评分顺序继续承接溢出量。
     *
     * @param candidates 当前班已通过过滤和评分的候选机台
     * @param selectedCandidate 已选中候选机台
     * @return 首选机台排在第一位的候选机台列表
     */
    private List<TmMachineCandidate> sortCandidatesWithSelectedFirst(List<TmMachineCandidate> candidates,
                                                                     TmMachineCandidate selectedCandidate) {
        List<TmMachineCandidate> sortedCandidates = this.sortCandidates(new ArrayList<>(candidates));
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
     * 追加零计划量任务到选中机台，避免库存充足场景被误判为未排。
     *
     * @param task              当前零计划量任务
     * @param selectedCandidate 已选中候选机台
     * @param context           胎面排程上下文
     * @param shiftOrder        当前任务班次
     */
    private void appendZeroPlanTask(TmTaskDraft task, TmMachineCandidate selectedCandidate,
                                    TmScheduleContext context, Integer shiftOrder) {
        task.setShiftOrder(shiftOrder);
        BigDecimal machineSpeed = this.resolveMachineSpeed(task, selectedCandidate);
        TmMachineCandidate runtimeCandidate = this.copyCandidate(selectedCandidate);
        runtimeCandidate.setMachineSpeed(machineSpeed);
        runtimeCandidate.setRemainCapacity(this.resolveRemainCapacity(task, context, runtimeCandidate, machineSpeed));
        task.setMachineRemainCapacity(runtimeCandidate.getRemainCapacity());
        task.setMachineSpeed(machineSpeed);
        context.getCandidateTraceMap().put(task.getBusinessKey(), Collections.singletonList(runtimeCandidate));
        this.addCapacitySplitTrace(context, task, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                nvl(runtimeCandidate.getRemainCapacity()), runtimeCandidate.getMachineCode(), true);
        this.addAssignTrace(context, task, "PASS", runtimeCandidate.getMachineCode(), null, null);
        this.taskChainScheduleService.appendAutoTask(task, runtimeCandidate, context);
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
    private TmTaskDraft copyOverflowTask(TmTaskDraft source, Integer shiftOrder, BigDecimal planQty,
                                         Integer sourceShift, int overflowIndex, String machineCode) {
        TmTaskDraft target = new TmTaskDraft();
        target.setOrderNo(source.getOrderNo());
        target.setSourceOrderNos(source.getSourceOrderNos());
        target.setTreadCode(source.getTreadCode());
        target.setGlueCode(source.getGlueCode());
        target.setBaseGlueCode(source.getBaseGlueCode());
        target.setMouthPlateCode(source.getMouthPlateCode());
        target.setShiftOrder(shiftOrder);
        target.setCurrentShiftDemandQty(source.getCurrentShiftDemandQty());
        target.setGuardDemandQty(source.getGuardDemandQty());
        target.setRollingStockQty(source.getRollingStockQty());
        target.setSixClockStockQty(source.getSixClockStockQty());
        target.setGuardShiftCount(source.getGuardShiftCount());
        target.setGuardRangeHours(source.getGuardRangeHours());
        target.setSupplyHours(source.getSupplyHours());
        target.setCurrentShiftStockGapQty(source.getCurrentShiftStockGapQty());
        target.setStockGapQty(source.getStockGapQty());
        target.setPlanQty(planQty);
        target.setTreadShoulderLength(source.getTreadShoulderLength());
        target.setTailFlag(source.getTailFlag());
        target.setTailBalanceQty(source.getTailBalanceQty());
        target.setLossRate(source.getLossRate());
        target.setBaseDemandQty(source.getBaseDemandQty());
        target.setLossAddQty(source.getLossAddQty());
        target.setToolLimitAdjustQty(source.getToolLimitAdjustQty());
        target.setMinStartAdjustQty(source.getMinStartAdjustQty());
        target.setTailRoundAdjustQty(source.getTailRoundAdjustQty());
        target.setCalcFormulaDesc(source.getCalcFormulaDesc());
        target.setTotalToolQty(source.getTotalToolQty());
        target.setCurlRollLength(source.getCurlRollLength());
        target.setDefaultCurlRollLength(source.getDefaultCurlRollLength());
        target.setMinStartQty(source.getMinStartQty());
        target.setPreviousSpecSwitchHours(source.getPreviousSpecSwitchHours());
        target.setPreviousGlueSwitchHours(source.getPreviousGlueSwitchHours());
        target.setFixedMachineMatched(source.getFixedMachineMatched());
        target.setDemandQty(source.getDemandQty());
        target.setNewSpecInfo(source.getNewSpecInfo());
        target.setExperimentSpecInfo(source.getExperimentSpecInfo());
        target.setBusinessKeySuffix("OVERFLOW_FROM_CLASS" + sourceShift + "_TO_CLASS" + shiftOrder + "_" + machineCode + "_" + overflowIndex);
        return target;
    }

    /**
     * 回填产能拆分后的任务计划量和解释字段。
     *
     * @param task            当前任务
     * @param beforeAssignQty 拆分前待排量
     * @param assignedQty     本班实际分配量
     * @param remainCapacity  本班分配前剩余产能
     * @param machineSpeed    机台速度
     */
    private void applyCapacitySplitResult(TmTaskDraft task, BigDecimal beforeAssignQty, BigDecimal assignedQty,
                                          BigDecimal remainCapacity, BigDecimal machineSpeed, String splitDesc) {
        task.setPlanQty(assignedQty);
        task.setMachineRemainCapacity(remainCapacity);
        task.setMachineSpeed(machineSpeed);
        task.setCapacityAdjustQty(nvl(task.getCapacityAdjustQty()).add(assignedQty.subtract(nvl(beforeAssignQty))));
        task.setCalcFormulaDesc(this.appendFormulaDesc(task.getCalcFormulaDesc(), splitDesc));
    }

    /**
     * 将任务加入上下文任务列表，确保顺延任务能生成解释和落库记录。
     *
     * @param context 排程上下文
     * @param task    任务
     */
    private void addContextTask(TmScheduleContext context, TmTaskDraft task) {
        if (context.getTaskDraftList().contains(task)) {
            return;
        }
        try {
            context.getTaskDraftList().add(task);
        } catch (UnsupportedOperationException ex) {
            List<TmTaskDraft> mutableList = new ArrayList<>(context.getTaskDraftList());
            mutableList.add(task);
            context.setTaskDraftList(mutableList);
        }
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
    private void addCapacitySplitTrace(TmScheduleContext context, TmTaskDraft task, BigDecimal beforeAssignQty,
                                       BigDecimal assignedQty, BigDecimal overflowQty, BigDecimal remainCapacity,
                                       String machineCode, boolean sameShift) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("shiftOrder", task.getShiftOrder());
        evidence.put("machineCode", machineCode);
        evidence.put("splitType", sameShift ? "SAME_SHIFT_MATCHED_MACHINE" : "NEXT_SHIFT_MATCHED_MACHINE");
        evidence.put("beforeAssignQty", beforeAssignQty);
        evidence.put("assignedQty", assignedQty);
        evidence.put("overflowQty", overflowQty);
        evidence.put("remainCapacity", remainCapacity);
        traceOf(context, task).addRuleHit("CAPACITY_OVERFLOW_SPLIT",
                overflowQty.compareTo(BigDecimal.ZERO) > 0 ? "SPLIT" : "PASS", evidence);
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
    private void addNewSpecAdvanceResultTrace(TmScheduleContext context, TmTaskDraft task, BigDecimal assignedQty,
                                              BigDecimal overflowQty, String machineCode) {
        TmNewSpecInfo newSpecInfo = task.getNewSpecInfo();
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
        traceOf(context, task).addRuleHit("NEW_SPEC_ADVANCE_RESULT",
                advancedWindowHit ? "PASS" : "ROLLING", evidence);
    }

    /**
     * 写入超过六班仍无法排完的未排证据。
     *
     * @param context      排程上下文
     * @param task         未排任务
     * @param remainingQty 六班后剩余未排量
     */
    private void addCapacityUnplannedTrace(TmScheduleContext context, TmTaskDraft task, BigDecimal remainingQty) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("remainingQty", remainingQty);
        evidence.put("reasonCode", TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getCode());
        evidence.put("reasonDesc", TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH.getDesc());
        traceOf(context, task).addRuleHit("CAPACITY_OVERFLOW_UNPLANNED", "REJECT", evidence);
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
     * 写入机台过滤证据。
     *
     * @param context    排程上下文
     * @param task       任务草稿
     * @param candidate  候选机台
     * @param result     过滤结果
     * @param reasonCode 原因编码
     * @param reasonDesc 原因描述
     */
    private void addFilterTrace(TmScheduleContext context, TmTaskDraft task, TmMachineCandidate candidate,
                                String result, String reasonCode, String reasonDesc) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("machineCode", candidate.getMachineCode());
        evidence.put("remainCapacity", candidate.getRemainCapacity());
        evidence.put("reasonCode", reasonCode);
        evidence.put("reasonDesc", reasonDesc);
        traceOf(context, task).addRuleHit("MACHINE_FILTER", result, evidence);
    }

    /**
     * 写入机台评分证据。
     *
     * @param context     排程上下文
     * @param task        任务草稿
     * @param candidate   候选机台
     * @param scoreResult 评分结果
     */
    private void addScoreTrace(TmScheduleContext context, TmTaskDraft task, TmMachineCandidate candidate,
                               ScheduleScoreResult scoreResult) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("machineCode", candidate.getMachineCode());
        evidence.put("strategyCode", scoreResult == null ? null : scoreResult.getStrategyCode());
        evidence.put("score", scoreResult == null ? null : scoreResult.getTotalScore());
        evidence.put("scoreItems", scoreResult == null ? null : scoreResult.getScoreItems());
        evidence.put("description", scoreResult == null ? null : scoreResult.getDescription());
        evidence.put("remainCapacity", candidate.getRemainCapacity());
        evidence.put("machineSpeed", candidate.getMachineSpeed());
        traceOf(context, task).addRuleHit("MACHINE_SCORE", "PASS", evidence);
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
    private void addAssignTrace(TmScheduleContext context, TmTaskDraft task, String result, String selectedMachineCode,
                                String reasonCode, String reasonDesc) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("selectedMachineCode", selectedMachineCode);
        evidence.put("reasonCode", reasonCode);
        evidence.put("reasonDesc", reasonDesc);
        traceOf(context, task).addRuleHit("MACHINE_ASSIGN", result, evidence);
    }

    /**
     * 获取任务规则证据对象，不存在时创建。
     *
     * @param context 排程上下文
     * @param task    任务草稿
     * @return 规则证据对象
     */
    private TmRuleTrace traceOf(TmScheduleContext context, TmTaskDraft task) {
        return context.getRuleTraceMap().computeIfAbsent(task.getBusinessKey(), key -> new TmRuleTrace());
    }

    /**
     * 标记任务无可用机台。
     *
     * @param task 待排任务草稿
     */
    private void markNoAvailableMachine(TmTaskDraft task) {
        this.markUnplanned(task, TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE);
    }

    /**
     * 按指定未排原因枚举标记任务。
     *
     * @param task   待排任务草稿
     * @param reason 未排原因枚举
     */
    private void markUnplanned(TmTaskDraft task, TmUnplannedReasonEnum reason) {
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
    private TmUnplannedReasonEnum resolveUnplannedReasonFromCandidates(List<TmMachineCandidate> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE;
        }
        Set<String> reasonCodes = candidates.stream()
                .filter(TmMachineCandidate::isFiltered)
                .map(TmMachineCandidate::getFilterReasonCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (reasonCodes.size() == 1) {
            String reasonCode = reasonCodes.iterator().next();
            if ("NO_REMAIN_CAPACITY".equals(reasonCode)) {
                return TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
            }
            if ("MOUTH_PLATE_NOT_MATCH".equals(reasonCode)) {
                return TmUnplannedReasonEnum.MOUTH_PLATE_NOT_MATCH;
            }
            if ("GLUE_MACHINE_NOT_MATCH".equals(reasonCode)) {
                return TmUnplannedReasonEnum.GLUE_MACHINE_NOT_ALLOWED;
            }
        }
        // MACHINE_DISABLED / FIXED_MACHINE_* / 混合原因，兜底为无可用机台
        return TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE;
    }

    /**
     * 复制候选机台列表，避免过滤评分过程污染上下文中的原始候选列表。
     *
     * @param source 原始候选机台列表
     * @return 复制后的候选机台列表
     */
    private List<TmMachineCandidate> copyCandidates(List<TmMachineCandidate> source) {
        return source.stream().map(this::copyCandidate).collect(Collectors.toList());
    }

    /**
     * 复制单个候选机台对象，重置过滤状态和评分。
     *
     * @param source 原始候选机台
     * @return 复制后的候选机台
     */
    private TmMachineCandidate copyCandidate(TmMachineCandidate source) {
        TmMachineCandidate copy = new TmMachineCandidate();
        copy.setMachineCode(source.getMachineCode());
        copy.setEnabled(source.getEnabled());
        copy.setMaxCapacity(source.getMaxCapacity());
        copy.setRemainCapacity(source.getRemainCapacity());
        copy.setMaintenanceHours(source.getMaintenanceHours());
        copy.setMachineSpeed(source.getMachineSpeed());
        copy.getTreadSpeedMap().putAll(source.getTreadSpeedMap());
        copy.setMouthPlateCodes(source.getMouthPlateCodes());
        copy.setForbiddenGlueCodes(source.getForbiddenGlueCodes());
        copy.setFixedAllowTreadCodes(source.getFixedAllowTreadCodes());
        copy.setFixedForbidTreadCodes(source.getFixedForbidTreadCodes());
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
     * @param context    胎面排程上下文
     * @param candidates 候选机台列表
     */
    private void prepareCandidatesForTask(TmTaskDraft task, TmScheduleContext context,
                                          List<TmMachineCandidate> candidates) {
        boolean hasFixedAllowRule = candidates.stream()
                .anyMatch(candidate -> contains(candidate.getFixedAllowTreadCodes(), task.getTreadCode()));
        for (TmMachineCandidate candidate : candidates) {
            BigDecimal machineSpeed = resolveMachineSpeed(task, candidate);
            BigDecimal remainCapacity = resolveRemainCapacity(task, context, candidate, machineSpeed);
            candidate.setMachineSpeed(machineSpeed);
            candidate.setRemainCapacity(remainCapacity);
            candidate.setMouthPlateMatched(isMouthPlateMatched(task, candidate));
            candidate.setGlueMachineMatched(!contains(candidate.getForbiddenGlueCodes(), task.getGlueCode()));
            boolean fixedAllowMatched = contains(candidate.getFixedAllowTreadCodes(), task.getTreadCode());
            candidate.setFixedMachineSelected(!hasFixedAllowRule || fixedAllowMatched);
            candidate.setFixedMachineMatched(fixedAllowMatched);
            candidate.setFixedMachineExcluded(contains(candidate.getFixedForbidTreadCodes(), task.getTreadCode()));
            // 打印机台速度选择来源
            log.info("[TM_MACHINE_SPEED] treadCode={}, machineCode={}, 机台速度【{}】，来源={}",
                    task.getTreadCode(), candidate.getMachineCode(), machineSpeed,
                    candidate.getTreadSpeedMap().containsKey(task.getTreadCode()) ? "胎面规格速度" : "机台默认速度");
            // 打印机台产能计算公式
            log.info("[TM_MACHINE_CAPACITY] treadCode={}, machineCode={}, shiftOrder={}, 最大产能【maxCapacity】-检修时长*机台速度【maintenanceDeduct】-已排计划量【assignedPlanQty】=剩余产能【remainCapacity】",
                    task.getTreadCode(), candidate.getMachineCode(), task.getShiftOrder());
            log.info("[TM_MACHINE_CAPACITY_DETAIL] treadCode={}, machineCode={}, shiftOrder={}, maxCapacity={}, maintenanceHours={}, machineSpeed={}, assignedPlanQty={}, remainCapacity={}",
                    task.getTreadCode(), candidate.getMachineCode(), task.getShiftOrder(),
                    candidate.getMaxCapacity(), candidate.getMaintenanceHours(), machineSpeed,
                    resolveAssignedPlanQty(context, candidate.getMachineCode(), task.getShiftOrder()),
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
    private boolean isMouthPlateMatched(TmTaskDraft task, TmMachineCandidate candidate) {
        if (StrUtil.isBlank(task.getMouthPlateCode())) {
            return true;
        }
        if (candidate.getMouthPlateCodes() == null) {
            return !Boolean.FALSE.equals(candidate.getMouthPlateMatched());
        }
        return contains(candidate.getMouthPlateCodes(), task.getMouthPlateCode());
    }

    /**
     * 解析机台生产速度，优先机台+胎面规格，其次机台默认速度。
     *
     * @param task      待排任务草稿
     * @param candidate 候选机台
     * @return 生产速度，缺失时返回0
     */
    private BigDecimal resolveMachineSpeed(TmTaskDraft task, TmMachineCandidate candidate) {
        BigDecimal speed = candidate.getTreadSpeedMap().get(task.getTreadCode());
        if (speed != null) {
            return speed;
        }
        return nvl(candidate.getMachineSpeed());
    }

    /**
     * 计算候选机台当前任务所在班次的剩余产能。
     *
     * @param task         待排任务草稿
     * @param context      胎面排程上下文
     * @param candidate    候选机台
     * @param machineSpeed 生产速度
     * @return 剩余产能
     */
    private BigDecimal resolveRemainCapacity(TmTaskDraft task, TmScheduleContext context,
                                             TmMachineCandidate candidate, BigDecimal machineSpeed) {
        BigDecimal maxCapacity = candidate.getMaxCapacity() == null ? candidate.getRemainCapacity() : candidate.getMaxCapacity();
        BigDecimal maintenanceDeduct = nvl(candidate.getMaintenanceHours()).multiply(nvl(machineSpeed));
        BigDecimal assignedPlanQty = resolveAssignedPlanQty(context, candidate.getMachineCode(), task.getShiftOrder());
        return nvl(maxCapacity).subtract(maintenanceDeduct).subtract(assignedPlanQty).max(BigDecimal.ZERO);
    }

    /**
     * 汇总目标机台班次已排计划量。
     *
     * @param context     胎面排程上下文
     * @param machineCode 机台编码
     * @param shiftOrder  班次顺序
     * @return 已排计划量
     */
    private BigDecimal resolveAssignedPlanQty(TmScheduleContext context, String machineCode, Integer shiftOrder) {
        ScheduleTaskLinkedList<TmTaskDraft> chain = context.getTaskChain(machineCode, shiftOrder);
        if (chain == null || CollUtil.isEmpty(chain.toList())) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ScheduleTaskNode<TmTaskDraft> node : chain.toList()) {
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
     * @param context  胎面排程上下文
     * @param taskList 已准备好的任务草稿列表
     * @throws ServiceException 上下文为空时抛出
     */
    public void assignPrepared(TmScheduleContext context, List<TmTaskDraft> taskList) {
        if (context == null) {
            throw new ServiceException(TmScheduleErrorCodeEnum.TM_CONTEXT_EMPTY.getDefaultMessage());
        }
        context.setTaskDraftList(taskList);
        assign(context);
    }
}
