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
 * 全部候选机台被过滤时标记未排原因 {@code NO_AVAILABLE_MACHINE}。</p>
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
        for (TmTaskDraft task : context.getTaskDraftList()) {
            if (task.isUnassigned()) {
                // 未预置机台的任务走完整过滤评分流程
                assignByFilterAndScore(task, context);
                continue;
            }
            // 已预置机台的任务直接追加到对应机台任务链
            TmMachineCandidate candidate = new TmMachineCandidate();
            candidate.setMachineCode(task.getMachineCode());
            addAssignTrace(context, task, "PASS", task.getMachineCode(), null, null);
            taskChainScheduleService.appendAutoTask(task, candidate, context);
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

        // 全部候选机台被过滤，标记无可用机台
        if (passedCandidates.isEmpty()) {
            log.info("[TM_MACHINE_ASSIGN] 任务[{}]所有候选机台均被过滤，标记无可用机台", task.getBusinessKey());
            addAssignTrace(context, task, "REJECT", null, TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode(),
                    TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getDesc());
            markNoAvailableMachine(task);
            return;
        }

        // 对通过过滤的候选机台执行评分
        for (TmMachineCandidate candidate : passedCandidates) {
            ScheduleScoreResult scoreResult = scoreStrategy.score(candidate, ruleContext);
            addScoreTrace(context, task, candidate, scoreResult);
            log.debug("[TM_MACHINE_ASSIGN] 任务[{}]机台[{}]评分={}",
                    task.getBusinessKey(), candidate.getMachineCode(), scoreResult.getTotalScore());
        }

        // 按评分降序选择最高分机台，同分按机台编码升序排序保证稳定
        passedCandidates.sort(Comparator
                .comparing(TmMachineCandidate::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TmMachineCandidate::getMachineCode, Comparator.nullsLast(Comparator.naturalOrder())));
        TmMachineCandidate bestCandidate = passedCandidates.get(0);
        log.info("[TM_MACHINE_ASSIGN] 任务[{}]选中机台[{}]，评分={}",
                task.getBusinessKey(), bestCandidate.getMachineCode(), bestCandidate.getScore());
        addAssignTrace(context, task, "PASS", bestCandidate.getMachineCode(), null, null);

        // 追加任务到选中机台的任务链
        taskChainScheduleService.appendAutoTask(task, bestCandidate, context);
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
        task.setUnplannedReasonCode(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getCode());
        task.setUnplannedReasonDesc(TmUnplannedReasonEnum.NO_AVAILABLE_MACHINE.getDesc());
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
