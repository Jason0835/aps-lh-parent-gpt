package com.zlt.aps.tc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleExplainTargetRel;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.api.domain.vo.TcAutoScheduleResponseVo;
import com.zlt.aps.tc.api.enums.TcAutoScheduleTaskStatusEnum;
import com.zlt.aps.tc.api.enums.TcMachineAssignStatusEnum;
import com.zlt.aps.tc.api.enums.TcScheduleRuleCodeEnum;
import com.zlt.aps.tc.domain.TcAutoScheduleTask;
import com.zlt.aps.tc.engine.domain.*;
import com.zlt.aps.tc.engine.service.ITcSnapshotAndPersistService;
import com.zlt.aps.tc.engine.service.impl.TcPersistService;
import com.zlt.aps.tc.engine.service.impl.TcScheduleQualitySummaryService;
import com.zlt.aps.tc.engine.service.impl.TcSnapshotBuildService;
import com.zlt.aps.tc.mapper.*;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;

/**
 * 胎侧自动排程业务快照和落库步骤服务。
 *
 * <p>负责在模板快照阶段生成解释快照，将任务链转换为排程结果并写入排程结果与解释表。</p>
 */
@Slf4j
@Primary
@Service
public class TcBizSnapshotAndPersistService implements ITcSnapshotAndPersistService {

    private final TcSnapshotBuildService snapshotBuildService;

    private final TcPersistService persistService;

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcScheduleResultExplainMapper scheduleResultExplainMapper;

    private final TcScheduleUnplannedMapper scheduleUnplannedMapper;

    private final TcScheduleExplainTargetRelMapper scheduleExplainTargetRelMapper;

    private final TcAutoScheduleTaskMapper autoScheduleTaskMapper;

    /** 排程质量指标统一汇总服务。 */
    private final TcScheduleQualitySummaryService qualitySummaryService;

    /** 通用批量写入服务。 */
    private final BaseDao baseDao;

    /** 最终持久化短事务模板。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建胎侧自动排程业务快照和落库步骤服务。
     *
     * @param snapshotBuildService        解释快照构建服务
     * @param persistService              落库实体转换服务
     * @param scheduleResultMapper        胎侧排程结果 Mapper
     * @param scheduleResultExplainMapper 胎侧排程解释 Mapper
     * @param scheduleUnplannedMapper     胎侧未排任务 Mapper
     * @param scheduleExplainTargetRelMapper 胎侧来源解释目标关联 Mapper
     * @param baseDao                     通用批量写入服务
     * @param transactionManager          事务管理器
     */
    @Autowired
    public TcBizSnapshotAndPersistService(TcSnapshotBuildService snapshotBuildService,
                                          TcPersistService persistService,
                                          TcScheduleResultMapper scheduleResultMapper,
                                          TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                          TcScheduleExplainTargetRelMapper scheduleExplainTargetRelMapper,
                                          TcAutoScheduleTaskMapper autoScheduleTaskMapper,
                                          TcScheduleQualitySummaryService qualitySummaryService,
                                          BaseDao baseDao,
                                          PlatformTransactionManager transactionManager) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.scheduleExplainTargetRelMapper = scheduleExplainTargetRelMapper;
        this.autoScheduleTaskMapper = autoScheduleTaskMapper;
        this.qualitySummaryService = qualitySummaryService;
        this.baseDao = baseDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 创建兼容旧测试装配的胎侧快照和落库服务。
     *
     * @param snapshotBuildService 解释快照构建服务
     * @param persistService 落库实体转换服务
     * @param scheduleResultMapper 胎侧排程结果 Mapper
     * @param scheduleResultExplainMapper 胎侧排程解释 Mapper
     * @param scheduleUnplannedMapper 胎侧未排任务 Mapper
     * @param autoScheduleTaskMapper 异步任务 Mapper
     * @param qualitySummaryService 质量摘要服务
     * @param baseDao 通用批量写入服务
     * @param transactionManager 事务管理器
     */
    public TcBizSnapshotAndPersistService(TcSnapshotBuildService snapshotBuildService,
                                          TcPersistService persistService,
                                          TcScheduleResultMapper scheduleResultMapper,
                                          TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                          TcAutoScheduleTaskMapper autoScheduleTaskMapper,
                                          TcScheduleQualitySummaryService qualitySummaryService,
                                          BaseDao baseDao,
                                          PlatformTransactionManager transactionManager) {
        this(snapshotBuildService, persistService, scheduleResultMapper, scheduleResultExplainMapper,
                scheduleUnplannedMapper, null, autoScheduleTaskMapper, qualitySummaryService,
                baseDao, transactionManager);
    }

    /**
     * 执行解释快照构建和实际落库。
     *
     * @param context 胎侧排程上下文，需包含已完成机台分配的任务链
     */
    @Override
    public void snapshotAndPersist(TcScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException(I18nUtil.getMessage("ui.tc.schedule.contextEmpty"));
        }
        this.prepareSourceTaskAllocations(context);
        // 解释快照在事务外完成；最终事务只承担旧批次删除与新批次写入。
        this.buildSnapshot(context);
        TcPersistResult persistResult = transactionTemplate.execute(transactionStatus -> {
            this.logicDeleteOldSchedule(context);
            TcPersistResult result = this.persistScheduleContext(context, transactionStatus);
            this.markCoreTaskSuccess(context, result);
            return result;
        });
        if (persistResult == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.persistFailed"));
        }
        context.setPersistResult(persistResult);
    }

    /**
     * 在结果、未排和解释落库的同一事务内标记核心任务成功并写入质量摘要。
     *
     * @param context 排程上下文
     * @param persistResult 落库汇总
     * @throws ServiceException 任务状态更新失败时抛出并回滚整批结果
     */
    private void markCoreTaskSuccess(TcScheduleContext context, TcPersistResult persistResult) {
        if (StrUtil.isBlank(context.getTaskId())) {
            return;
        }
        Map<String, Object> summary = qualitySummaryService.build(context, persistResult);
        context.setQualitySummary(summary);
        TcAutoScheduleResponseVo taskResponse = this.buildCoreTaskResponse(context, persistResult, summary);
        int affectedRows = autoScheduleTaskMapper.update(null, new LambdaUpdateWrapper<TcAutoScheduleTask>()
                .eq(TcAutoScheduleTask::getTaskId, context.getTaskId())
                .eq(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.RUNNING.getCode())
                .set(TcAutoScheduleTask::getTaskStatus, TcAutoScheduleTaskStatusEnum.SUCCESS.getCode())
                .set(TcAutoScheduleTask::getProgress, 100)
                .set(TcAutoScheduleTask::getCurrentStage, TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE)
                .set(TcAutoScheduleTask::getCurrentStageName, I18nUtil.getMessage("ui.tc.schedule.taskCompleted"))
                .set(TcAutoScheduleTask::getBatchNo, context.getBatchNo())
                .set(TcAutoScheduleTask::getTraceId, context.getTraceId())
                .set(TcAutoScheduleTask::getResultJson, JSON.toJSONString(taskResponse))
                .set(TcAutoScheduleTask::getIssueJson, JSON.toJSONString(context.getIssueCollector().getIssues()))
                .set(TcAutoScheduleTask::getSummaryJson, JSON.toJSONString(summary))
                .set(TcAutoScheduleTask::getEndTime, new Date())
                .set(TcAutoScheduleTask::getLastHeartbeatTime, new Date()));
        if (affectedRows != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.taskStatusUpdateFailed"));
        }
    }

    /**
     * 构建与核心结果同事务保存的完整任务响应。
     *
     * @param context 排程上下文
     * @param persistResult 落库汇总
     * @param summary 质量指标摘要
     * @return 完整任务响应
     */
    private TcAutoScheduleResponseVo buildCoreTaskResponse(TcScheduleContext context, TcPersistResult persistResult,
                                                            Map<String, Object> summary) {
        TcAutoScheduleResponseVo response = new TcAutoScheduleResponseVo();
        response.setTaskId(context.getTaskId());
        response.setTaskStatus(TcAutoScheduleTaskStatusEnum.SUCCESS.getCode());
        response.setProgress(100);
        response.setCurrentStage(TcScheduleConstants.AUTO_SCHEDULE_STAGE_COMPLETE);
        response.setCurrentStageName(I18nUtil.getMessage("ui.tc.schedule.taskCompleted"));
        response.setBatchNo(context.getBatchNo());
        response.setTraceId(context.getTraceId());
        response.setSuccess(Boolean.TRUE);
        response.setResultCount(persistResult.getResultCount());
        response.setUnplannedCount(persistResult.getUnplannedCount());
        response.setIssues(context.getIssueCollector().getIssues());
        response.setIssueCount(response.getIssues().size());
        response.setSummary(summary);
        response.setMessage(I18nUtil.getMessage("ui.tc.schedule.executeFinished"));
        return response;
    }

    /**
     * 构建所有待排任务的解释快照。
     *
     * @param context 胎侧排程上下文
     */
    private void buildSnapshot(TcScheduleContext context) {
        List<TcTaskDraft> explainTaskList = this.resolveExplainTaskList(context);
        if (CollUtil.isEmpty(explainTaskList)) {
            return;
        }
        for (TcTaskDraft task : explainTaskList) {
            TcSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
            context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
        }
    }

    /**
     * 将自动排程上下文中的任务结果和解释写入数据库。
     *
     * @param context 自动排程上下文
     * @return 落库汇总
     */
    private TcPersistResult persistScheduleContext(TcScheduleContext context, TransactionStatus transactionStatus) {
        TcPersistResult persistResult = new TcPersistResult();
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return persistResult;
        }
        List<TcScheduleResult> resultList = new ArrayList<>();
        List<TcTaskDraft> unplannedTaskList = new ArrayList<>();
        Map<TcScheduleResult, List<String>> resultBusinessKeyMap = new IdentityHashMap<>();
        Map<String, Long> resultIdMap = new HashMap<>();
        Map<String, Long> unplannedIdMap = new HashMap<>();
        for (TcTaskDraft taskDraft : context.getTaskDraftList()) {
            if (isUnplannedTask(taskDraft)) {
                unplannedTaskList.add(taskDraft);
            }
        }
        for (ScheduleTaskLinkedList<TcTaskDraft> chain : context.getTaskChainGroup().values()) {
            List<ScheduleTaskNode<TcTaskDraft>> nodeList = chain.toList();
            List<TcScheduleResult> chainResultList = persistService.convertChainToResult(chain, context);
            registerChainResultBusinessKey(nodeList, chainResultList, resultBusinessKeyMap);
            resultList.addAll(chainResultList);
        }
        Map<TcScheduleResult, List<String>> mergedResultBusinessKeyMap = new IdentityHashMap<>();
        List<TcScheduleResult> mergedResultList = mergeScheduleResults(resultList, resultBusinessKeyMap, mergedResultBusinessKeyMap);
        for (TcScheduleResult result : mergedResultList) {
            normalizeResultShiftFields(result);
        }
        List<TcScheduleResult> visibleResultList = filterVisibleScheduleResults(mergedResultList, mergedResultBusinessKeyMap);
        visibleResultList.sort(Comparator
                .comparing((TcScheduleResult result) -> StrUtil.blankToDefault(result.getMachineCode(), ""))
                .thenComparing(result -> StrUtil.blankToDefault(result.getSidewallCode(), ""))
                .thenComparing(result -> StrUtil.blankToDefault(result.getConstructionVersion(), "")));
        assignTcOrderNo(visibleResultList, context.getBatchNo());
        resequenceVisibleShiftSequences(visibleResultList);
        this.batchSaveWithFallback(visibleResultList, transactionStatus, "RESULT", this::buildResultErrorMsg);
        for (TcScheduleResult result : visibleResultList) {
            this.registerInsertedResultId(result, mergedResultBusinessKeyMap, resultIdMap);
        }
        Map<Long, TcScheduleResult> finalResultMap = visibleResultList.stream()
                .filter(Objects::nonNull)
                .filter(result -> result.getId() != null)
                .collect(java.util.stream.Collectors.toMap(TcScheduleResult::getId, result -> result,
                        (first, second) -> first, LinkedHashMap::new));
        persistResult.setResultCount(visibleResultList.size());
        // 未排表落库统一批量写入，批量失败时回滚保存点并逐行定位失败对象。
        Map<String, TcScheduleUnplanned> unplannedMap = this.buildMergedUnplannedMap(unplannedTaskList, context);
        if (CollUtil.isNotEmpty(unplannedMap)) {
            List<TcScheduleUnplanned> unplannedList = new ArrayList<>(unplannedMap.values());
            this.batchSaveWithFallback(unplannedList, transactionStatus,
                    TcMachineAssignStatusEnum.UNPLANNED.getCode(), this::buildUnplannedErrorMsg);
            for (TcTaskDraft unplannedTask : unplannedTaskList) {
                TcScheduleUnplanned unplanned = unplannedMap.get(this.buildUnplannedMergeKey(
                        this.buildScheduleUnplanned(unplannedTask,
                                context.getSnapshotMap().get(unplannedTask.getBusinessKey()), context)));
                if (unplanned != null && unplanned.getId() != null) {
                    unplannedIdMap.put(unplannedTask.getBusinessKey(), unplanned.getId());
                }
            }
            persistResult.setUnplannedCount(unplannedList.size());
        }
        // 解释表落库统一批量写入，任何单行失败都会回滚整个最终事务。
        List<TcScheduleResultExplain> explainList = new ArrayList<>();
        for (TcTaskDraft taskDraft : this.resolveExplainTaskList(context)) {
            TcSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TcScheduleResultExplain explain = persistService.convertExplain(taskDraft, snapshot, context);
            Long resultId = this.resolveSourceSingleResultId(taskDraft, context, resultIdMap);
            if (resultId != null) {
                explain.setResultId(resultId);
            }
            explain.setFinalAssignmentJson(this.buildFinalAssignmentJson(
                    taskDraft, context, resultIdMap, unplannedIdMap, finalResultMap));
            explainList.add(explain);
        }
        if (CollUtil.isNotEmpty(explainList)) {
            this.batchSaveWithFallback(explainList, transactionStatus, "EXPLAIN",
                    (explain, exception) -> this.buildExplainErrorMsg(null, exception));
            persistResult.setExplainCount(explainList.size());
        }
        List<TcScheduleExplainTargetRel> targetRelList = this.buildExplainTargetRelList(
                context, explainList, resultIdMap, unplannedIdMap);
        if (CollUtil.isNotEmpty(targetRelList) && scheduleExplainTargetRelMapper != null) {
            this.batchSaveWithFallback(targetRelList, transactionStatus, "EXPLAIN_TARGET_REL",
                    (relation, exception) -> MessageFormat.format(
                            I18nUtil.getMessage("ui.tc.schedule.explainTargetPersistFailed"),
                            relation.getSourceTaskBusinessKey(), exception.getMessage()));
        }
        return persistResult;
    }

    /**
     * 解析解释落库任务列表。
     *
     * @param context 排程上下文
     * @return 已生成汇总组时返回原始来源任务，否则兼容返回生产任务
     */
    private List<TcTaskDraft> resolveExplainTaskList(TcScheduleContext context) {
        return CollUtil.isNotEmpty(context.getSourceTaskDraftList())
                ? context.getSourceTaskDraftList() : context.getTaskDraftList();
    }

    /**
     * 按机台分配后的实际结果片段重新汇总并分摊来源最终计划量。
     *
     * @param context 排程上下文
     */
    private void prepareSourceTaskAllocations(TcScheduleContext context) {
        if (context == null || CollUtil.isEmpty(context.getPlanTaskGroupMap())) {
            return;
        }
        Map<String, List<TcTaskDraft>> fragmentMap = context.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(task -> StrUtil.isNotBlank(task.getPlanGroupKey()))
                .collect(java.util.stream.Collectors.groupingBy(TcTaskDraft::getPlanGroupKey,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (TcPlanTaskGroup taskGroup : context.getPlanTaskGroupMap().values()) {
            List<TcTaskDraft> fragmentList = fragmentMap.getOrDefault(
                    taskGroup.getPlanGroupKey(), Collections.emptyList());
            Map<String, BigDecimal> sourceFinalQtyMap = taskGroup.getSourceTaskList().stream()
                    .collect(java.util.stream.Collectors.toMap(TcTaskDraft::getBusinessKey,
                            task -> BigDecimal.ZERO, BigDecimal::add, LinkedHashMap::new));
            for (TcTaskDraft fragment : fragmentList) {
                Map<String, BigDecimal> fragmentAllocationMap = this.allocateByWeight(
                        fragment.getPlanQty(), taskGroup.getSourceWeightMap());
                fragmentAllocationMap.forEach((sourceBusinessKey, allocatedQty) ->
                        sourceFinalQtyMap.merge(sourceBusinessKey, allocatedQty, BigDecimal::add));
            }
            BigDecimal groupFinalPlanQty = fragmentList.stream().map(TcTaskDraft::getPlanQty)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            TcTaskDraft aggregateTask = taskGroup.getAggregateTask();
            Map<String, BigDecimal> sourcePreLossPlanQtyMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getPreLossPlanQty(),
                    taskGroup.getSourceWeightMap());
            Map<String, BigDecimal> sourceLossAddQtyMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getLossAddQty(),
                    taskGroup.getSourceWeightMap());
            Map<String, BigDecimal> sourceMinStartAdjustQtyMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getMinStartAdjustQty(),
                    taskGroup.getSourceWeightMap());
            Map<String, BigDecimal> sourceRoundAdjustQtyMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getTailRoundAdjustQty(),
                    taskGroup.getSourceWeightMap());
            Map<String, BigDecimal> sourcePlanQtyBeforeToolLimitMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getPlanQtyBeforeToolLimit(),
                    taskGroup.getSourceWeightMap());
            Map<String, BigDecimal> sourceToolLimitAdjustQtyMap = this.allocateByWeight(
                    aggregateTask == null ? BigDecimal.ZERO : aggregateTask.getToolLimitAdjustQty(),
                    taskGroup.getSourceWeightMap());
            taskGroup.setGroupFinalPlanQty(groupFinalPlanQty);
            if (aggregateTask != null) {
                taskGroup.setGroupMinStartAdjustQty(aggregateTask.getMinStartAdjustQty());
                taskGroup.setGroupRoundAdjustQty(aggregateTask.getTailRoundAdjustQty());
                aggregateTask.setGroupMinStartAdjustQty(aggregateTask.getMinStartAdjustQty());
                aggregateTask.setGroupRoundAdjustQty(aggregateTask.getTailRoundAdjustQty());
                aggregateTask.setGroupFinalPlanQty(groupFinalPlanQty);
                // PLAN_CALC 记录的是派机前估算证据，快照阶段必须覆盖为机台确认后的最终结算分量。
                if (context.getRuleTraceMap().get(aggregateTask.getBusinessKey()) != null) {
                    context.getRuleTraceMap().get(aggregateTask.getBusinessKey()).getRuleHits().stream()
                            .filter(item -> TcScheduleRuleCodeEnum.PLAN_QTY_AGGREGATE.getCode()
                                    .equals(item.getRuleCode()))
                            .filter(item -> item.getEvidence() instanceof Map)
                            .forEach(item -> {
                                Map<String, Object> evidence = (Map<String, Object>) item.getEvidence();
                                evidence.put("preLossPlanQty", aggregateTask.getPreLossPlanQty());
                                evidence.put("lossAddQty", aggregateTask.getLossAddQty());
                                evidence.put("groupMinStartAdjustQty", taskGroup.getGroupMinStartAdjustQty());
                                evidence.put("groupRoundAdjustQty", taskGroup.getGroupRoundAdjustQty());
                                evidence.put("planQtyBeforeToolLimit", aggregateTask.getPlanQtyBeforeToolLimit());
                                evidence.put("groupFinalPlanQty", groupFinalPlanQty);
                                evidence.put("calcFormulaDesc", aggregateTask.getCalcFormulaDesc());
                                evidence.put("allocationStage", "SNAPSHOT");
                            });
                }
            }
            List<TcTaskDraft> assignedFragmentList = fragmentList.stream()
                    .filter(task -> !this.isUnplannedTask(task) && this.isPositiveQty(task.getPlanQty()))
                    .collect(java.util.stream.Collectors.toList());
            TcTaskDraft finalLedgerFragment = fragmentList.stream()
                    .filter(task -> task.getToolLedgerOrder() != null)
                    .max(Comparator.comparing(TcTaskDraft::getToolLedgerOrder)).orElse(null);
            String ledgerOwnerBusinessKey = taskGroup.getSourceTaskList().stream()
                    .map(TcTaskDraft::getBusinessKey).filter(Objects::nonNull)
                    .max(String::compareTo).orElse(null);
            boolean allUnplanned = CollUtil.isNotEmpty(fragmentList)
                    && assignedFragmentList.isEmpty() && groupFinalPlanQty.compareTo(BigDecimal.ZERO) > 0;
            for (TcTaskDraft sourceTask : taskGroup.getSourceTaskList()) {
                String sourceBusinessKey = sourceTask.getBusinessKey();
                sourceTask.setPlanQty(sourceFinalQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setPreLossPlanQty(sourcePreLossPlanQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setLossAddQty(sourceLossAddQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setMinStartAdjustQty(sourceMinStartAdjustQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setTailRoundAdjustQty(sourceRoundAdjustQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setPlanQtyBeforeToolLimit(sourcePlanQtyBeforeToolLimitMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setToolLimitAdjustQty(sourceToolLimitAdjustQtyMap.getOrDefault(sourceBusinessKey, BigDecimal.ZERO));
                sourceTask.setGroupMinStartAdjustQty(taskGroup.getGroupMinStartAdjustQty());
                sourceTask.setGroupRoundAdjustQty(taskGroup.getGroupRoundAdjustQty());
                sourceTask.setGroupFinalPlanQty(groupFinalPlanQty);
                if (aggregateTask != null) {
                    sourceTask.setResolvedLossRate(aggregateTask.getResolvedLossRate());
                    sourceTask.setLossMatchLevel(aggregateTask.getLossMatchLevel());
                    sourceTask.setLossMatchSource(aggregateTask.getLossMatchSource());
                    sourceTask.setCalcFormulaDesc(aggregateTask.getCalcFormulaDesc());
                }
                if (context.getRuleTraceMap().get(sourceTask.getBusinessKey()) != null) {
                    // 来源分摊证据与来源解释字段使用同一份最终分量，避免继续展示派机前估算值。
                    context.getRuleTraceMap().get(sourceTask.getBusinessKey()).getRuleHits().stream()
                            .filter(item -> TcScheduleRuleCodeEnum.PLAN_QTY_SOURCE_ALLOCATE.getCode()
                                    .equals(item.getRuleCode()))
                            .filter(item -> item.getEvidence() instanceof Map)
                            .forEach(item -> {
                                Map<String, Object> evidence = (Map<String, Object>) item.getEvidence();
                                evidence.put("allocatedPlanQty", sourceTask.getPlanQty());
                                evidence.put("allocatedPreLossPlanQty", sourceTask.getPreLossPlanQty());
                                evidence.put("allocatedLossAddQty", sourceTask.getLossAddQty());
                                evidence.put("allocatedMinStartAdjustQty", sourceTask.getMinStartAdjustQty());
                                evidence.put("allocatedRoundAdjustQty", sourceTask.getTailRoundAdjustQty());
                                evidence.put("allocatedPlanQtyBeforeToolLimit", sourceTask.getPlanQtyBeforeToolLimit());
                                evidence.put("calcFormulaDesc", sourceTask.getCalcFormulaDesc());
                                evidence.put("allocationStage", "SNAPSHOT");
                            });
                }
                sourceTask.setMachineCode(assignedFragmentList.isEmpty()
                        ? null : assignedFragmentList.get(0).getMachineCode());
                if (finalLedgerFragment != null
                        && Objects.equals(ledgerOwnerBusinessKey, sourceTask.getBusinessKey())) {
                    sourceTask.setToolLedgerOrder(finalLedgerFragment.getToolLedgerOrder());
                    sourceTask.setAvailableToolQty(finalLedgerFragment.getAvailableToolQty());
                    sourceTask.setToolUsedQty(finalLedgerFragment.getToolUsedQty());
                    sourceTask.setRemainingToolQty(finalLedgerFragment.getRemainingToolQty());
                }
                if (allUnplanned) {
                    TcTaskDraft unplannedFragment = fragmentList.get(0);
                    sourceTask.setUnplannedReasonCode(unplannedFragment.getUnplannedReasonCode());
                    sourceTask.setUnplannedReasonDesc(unplannedFragment.getUnplannedReasonDesc());
                } else {
                    sourceTask.setUnplannedReasonCode(null);
                    sourceTask.setUnplannedReasonDesc(null);
                }
            }
        }
    }

    /**
     * 单一已排结果兼容回填旧 RESULT_ID。
     *
     * @param sourceTask 来源解释任务
     * @param context 排程上下文
     * @param resultIdMap 任务业务键与结果主键映射
     * @return 仅关联一个结果且没有未排片段时返回结果主键，否则返回空
     */
    private Long resolveSourceSingleResultId(TcTaskDraft sourceTask, TcScheduleContext context,
                                             Map<String, Long> resultIdMap) {
        List<TcTaskDraft> groupFragmentList = context.getTaskDraftList().stream()
                .filter(task -> Objects.equals(sourceTask.getPlanGroupKey(), task.getPlanGroupKey()))
                .filter(task -> this.isPositiveQty(task.getPlanQty()))
                .collect(java.util.stream.Collectors.toList());
        if (groupFragmentList.stream().anyMatch(this::isUnplannedTask)) {
            return null;
        }
        Set<Long> resultIdSet = groupFragmentList.stream()
                .map(task -> resultIdMap.get(task.getBusinessKey()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return resultIdSet.size() == 1 ? resultIdSet.iterator().next() : null;
    }

    /**
     * 构建来源解释与结果或未排片段关联记录。
     *
     * @param context 排程上下文
     * @param explainList 已保存解释列表
     * @param resultIdMap 任务业务键与结果主键映射
     * @param unplannedIdMap 任务业务键与未排主键映射
     * @return 关联记录列表
     */
    private List<TcScheduleExplainTargetRel> buildExplainTargetRelList(
            TcScheduleContext context, List<TcScheduleResultExplain> explainList,
            Map<String, Long> resultIdMap, Map<String, Long> unplannedIdMap) {
        Map<String, TcScheduleResultExplain> explainMap = explainList.stream()
                .collect(java.util.stream.Collectors.toMap(TcScheduleResultExplain::getTaskBusinessKey,
                        explain -> explain, (first, second) -> first, LinkedHashMap::new));
        List<TcScheduleExplainTargetRel> relationList = new ArrayList<>();
        for (TcPlanTaskGroup taskGroup : context.getPlanTaskGroupMap().values()) {
            List<TcTaskDraft> fragmentList = context.getTaskDraftList().stream()
                    .filter(task -> Objects.equals(taskGroup.getPlanGroupKey(), task.getPlanGroupKey()))
                    .filter(task -> this.isPositiveQty(task.getPlanQty()))
                    .collect(java.util.stream.Collectors.toList());
            for (TcTaskDraft fragment : fragmentList) {
                Map<String, BigDecimal> allocationMap = this.allocateByWeight(
                        fragment.getPlanQty(), taskGroup.getSourceWeightMap());
                for (Map.Entry<String, BigDecimal> allocationEntry : allocationMap.entrySet()) {
                    if (!this.isPositiveQty(allocationEntry.getValue())) {
                        continue;
                    }
                    TcScheduleResultExplain explain = explainMap.get(allocationEntry.getKey());
                    if (explain == null) {
                        continue;
                    }
                    boolean unplanned = this.isUnplannedTask(fragment);
                    TcScheduleExplainTargetRel relation = new TcScheduleExplainTargetRel();
                    relation.setFactoryCode(context.getFactoryCode());
                    relation.setBatchNo(context.getBatchNo());
                    relation.setScheduleDate(context.getScheduleDate());
                    relation.setExplainId(explain.getId());
                    relation.setPlanGroupKey(taskGroup.getPlanGroupKey());
                    relation.setSourceTaskBusinessKey(allocationEntry.getKey());
                    relation.setTargetType(unplanned ? "UNPLANNED" : "RESULT");
                    Long targetId = unplanned
                            ? unplannedIdMap.get(fragment.getBusinessKey())
                            : resultIdMap.get(fragment.getBusinessKey());
                    if (targetId == null) {
                        throw new ServiceException(MessageFormat.format(
                                I18nUtil.getMessage("ui.tc.schedule.explainTargetMissing"),
                                fragment.getBusinessKey()));
                    }
                    relation.setTargetId(targetId);
                    relation.setTargetBusinessKey(fragment.getBusinessKey());
                    relation.setShiftOrder(fragment.getShiftOrder());
                    relation.setMachineCode(fragment.getMachineCode());
                    relation.setAllocatedQty(allocationEntry.getValue());
                    relationList.add(relation);
                }
            }
        }
        return relationList;
    }

    /**
     * 构建来源解释对应的最终分配证据。
     *
     * <p>该证据在结果归并、零计划过滤、最终班内顺序重排和主键回填后生成，
     * 因此其中的机台与顺序代表最终可见结果，而不是派机前基础排序。</p>
     *
     * @param sourceTask 来源解释任务
     * @param context 排程上下文
     * @param resultIdMap 实际片段业务键与结果主键映射
     * @param unplannedIdMap 实际片段业务键与未排主键映射
     * @param finalResultMap 结果主键与最终可见结果映射
     * @return 带版本号的最终分配JSON
     * @throws ServiceException 正计划量片段无法定位最终目标时抛出
     */
    private String buildFinalAssignmentJson(TcTaskDraft sourceTask, TcScheduleContext context,
                                            Map<String, Long> resultIdMap,
                                            Map<String, Long> unplannedIdMap,
                                            Map<Long, TcScheduleResult> finalResultMap) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", "1");
        List<Map<String, Object>> assignmentList = new ArrayList<>();
        if (sourceTask == null || context == null || StrUtil.isBlank(sourceTask.getBusinessKey())) {
            root.put("assignments", assignmentList);
            return JSON.toJSONString(root);
        }
        TcPlanTaskGroup taskGroup = context.getPlanTaskGroupMap() == null
                ? null : context.getPlanTaskGroupMap().get(sourceTask.getPlanGroupKey());
        List<TcTaskDraft> fragmentList = taskGroup == null
                ? Collections.singletonList(sourceTask)
                : context.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(fragment -> Objects.equals(taskGroup.getPlanGroupKey(), fragment.getPlanGroupKey()))
                .filter(fragment -> this.isPositiveQty(fragment.getPlanQty()))
                .collect(java.util.stream.Collectors.toList());
        Map<String, BigDecimal> sourceWeightMap = taskGroup == null
                ? Collections.singletonMap(sourceTask.getBusinessKey(), BigDecimal.ONE)
                : taskGroup.getSourceWeightMap();
        for (TcTaskDraft fragment : fragmentList) {
            if (!this.isPositiveQty(fragment.getPlanQty())) {
                continue;
            }
            BigDecimal allocatedQty = this.allocateByWeight(fragment.getPlanQty(), sourceWeightMap)
                    .get(sourceTask.getBusinessKey());
            if (!this.isPositiveQty(allocatedQty)) {
                continue;
            }
            boolean unplanned = this.isUnplannedTask(fragment);
            Long targetId = unplanned
                    ? unplannedIdMap.get(fragment.getBusinessKey())
                    : resultIdMap.get(fragment.getBusinessKey());
            if (targetId == null) {
                throw new ServiceException(MessageFormat.format(
                        I18nUtil.getMessage("ui.tc.schedule.explainTargetMissing"),
                        fragment.getBusinessKey()));
            }
            TcScheduleResult finalResult = unplanned ? null : finalResultMap.get(targetId);
            Map<String, Object> assignment = new LinkedHashMap<>();
            assignment.put("targetType", unplanned ? "UNPLANNED" : "RESULT");
            assignment.put("targetId", targetId);
            assignment.put("targetBusinessKey", fragment.getBusinessKey());
            assignment.put("machineCode", unplanned ? null : fragment.getMachineCode());
            assignment.put("shiftOrder", fragment.getShiftOrder());
            assignment.put("sequence", unplanned ? null
                    : this.resolveFinalSequence(finalResult, fragment.getShiftOrder()));
            assignment.put("allocatedQty", allocatedQty);
            assignment.put("selectedMachineScore", unplanned ? null
                    : this.resolveSelectedMachineScore(fragment, context));
            assignmentList.add(assignment);
        }
        BigDecimal assignmentTotal = assignmentList.stream()
                .map(assignment -> (BigDecimal) assignment.get("allocatedQty"))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sourceFinalPlanQty = BigDecimalUtils.valueOf(sourceTask.getPlanQty());
        if (CollUtil.isNotEmpty(assignmentList) && assignmentTotal.compareTo(sourceFinalPlanQty) != 0) {
            Map<String, Object> lastAssignment = assignmentList.get(assignmentList.size() - 1);
            BigDecimal lastAllocatedQty = BigDecimalUtils.valueOf(lastAssignment.get("allocatedQty"));
            lastAssignment.put("allocatedQty", lastAllocatedQty.add(sourceFinalPlanQty.subtract(assignmentTotal)));
        }
        this.sortFinalAssignmentList(assignmentList);
        root.put("assignments", assignmentList);
        return JSON.toJSONString(root);
    }

    /**
     * 读取实际片段选中机台的最终评分。
     *
     * @param fragment 实际排程片段
     * @param context 排程上下文
     * @return 选中机台评分；没有候选证据时返回null
     */
    private BigDecimal resolveSelectedMachineScore(TcTaskDraft fragment, TcScheduleContext context) {
        if (fragment == null || context == null || context.getCandidateTraceMap() == null) {
            return null;
        }
        List<TcMachineCandidate> candidateList = context.getCandidateTraceMap().get(fragment.getBusinessKey());
        if (CollUtil.isEmpty(candidateList)) {
            return null;
        }
        return candidateList.stream()
                .filter(Objects::nonNull)
                .filter(candidate -> Objects.equals(fragment.getMachineCode(), candidate.getMachineCode()))
                .map(TcMachineCandidate::getScore)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /**
     * 读取最终结果指定班次的可见顺序。
     *
     * @param result 最终可见结果
     * @param shiftOrder 班次顺序
     * @return 最终班内顺序；无结果或班次无效时返回null
     */
    private Integer resolveFinalSequence(TcScheduleResult result, Integer shiftOrder) {
        if (result == null || shiftOrder == null || shiftOrder < 1
                || shiftOrder > TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
            return null;
        }
        Object sequence = result.getFieldValueByFieldName(
                String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return sequence instanceof Number ? ((Number) sequence).intValue() : null;
    }

    /**
     * 按目标类型、班次、机台、顺序和目标业务键稳定排列最终分配证据。
     *
     * @param assignmentList 最终分配证据列表
     */
    private void sortFinalAssignmentList(List<Map<String, Object>> assignmentList) {
        assignmentList.sort(Comparator
                .comparing((Map<String, Object> assignment) -> String.valueOf(assignment.get("targetType")))
                .thenComparing(assignment -> (Integer) assignment.get("shiftOrder"),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(assignment -> Objects.toString(assignment.get("machineCode"), ""))
                .thenComparing(assignment -> (Integer) assignment.get("sequence"),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(assignment -> Objects.toString(
                        assignment.get("targetBusinessKey"), "")));
    }

    /**
     * 按来源权重分摊指定数量。
     *
     * @param totalQty 汇总数量
     * @param sourceWeightMap 来源权重
     * @return 来源业务键与分摊数量
     */
    private Map<String, BigDecimal> allocateByWeight(BigDecimal totalQty,
                                                      Map<String, BigDecimal> sourceWeightMap) {
        List<PlanQuantityAllocationItem> allocationItemList = sourceWeightMap.entrySet().stream()
                .map(entry -> new PlanQuantityAllocationItem(
                        entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .collect(java.util.stream.Collectors.toList());
        return PlanQuantityAllocationUtils.allocate(
                        totalQty, allocationItemList, TcScheduleConstants.DECIMAL_CALCULATION_SCALE).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PlanQuantityAllocationItem::getSourceBusinessKey,
                        PlanQuantityAllocationItem::getAllocatedQty,
                        BigDecimal::add, LinkedHashMap::new));
    }

    /**
     * 删除同工厂、同排程日期的旧解释、未排和结果。
     *
     * <p>该方法只在最终短事务内调用，删除和新批次写入任一失败都会整体回滚。</p>
     *
     * @param context 自动排程上下文
     */
    private void logicDeleteOldSchedule(TcScheduleContext context) {
        LambdaQueryWrapper<TcScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.eq(TcScheduleResult::getFactoryCode, context.getFactoryCode());
        resultWrapper.eq(TcScheduleResult::getScheduleDate, context.getScheduleDate());
        List<TcScheduleResult> oldResultList = scheduleResultMapper.selectList(resultWrapper);

        LambdaQueryWrapper<TcScheduleUnplanned> unplannedWrapper = new LambdaQueryWrapper<>();
        unplannedWrapper.eq(TcScheduleUnplanned::getFactoryCode, context.getFactoryCode());
        unplannedWrapper.eq(TcScheduleUnplanned::getScheduleDate, context.getScheduleDate());
        List<TcScheduleUnplanned> oldUnplannedList = scheduleUnplannedMapper.selectList(unplannedWrapper);

        Set<String> oldBatchNoSet = new LinkedHashSet<>();
        oldResultList.stream().map(TcScheduleResult::getBatchNo).filter(StrUtil::isNotBlank).forEach(oldBatchNoSet::add);
        oldUnplannedList.stream().map(TcScheduleUnplanned::getBatchNo).filter(StrUtil::isNotBlank).forEach(oldBatchNoSet::add);
        if (CollUtil.isNotEmpty(oldBatchNoSet)) {
            scheduleResultExplainMapper.logicDeleteByFactoryCodeAndBatchNos(context.getFactoryCode(),
                    new ArrayList<>(oldBatchNoSet));
        }
        LambdaQueryWrapper<TcScheduleExplainTargetRel> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(TcScheduleExplainTargetRel::getFactoryCode, context.getFactoryCode());
        relationWrapper.eq(TcScheduleExplainTargetRel::getScheduleDate, context.getScheduleDate());
        if (scheduleExplainTargetRelMapper != null) {
            scheduleExplainTargetRelMapper.delete(relationWrapper);
        }
        scheduleResultExplainMapper.logicDeleteByFactoryCodeAndScheduleDate(context.getFactoryCode(), context.getScheduleDate());
        scheduleUnplannedMapper.logicDeleteByFactoryCodeAndScheduleDate(context.getFactoryCode(), context.getScheduleDate());
        scheduleResultMapper.logicDeleteByFactoryCodeAndScheduleDate(context.getFactoryCode(), context.getScheduleDate());
    }

    /**
     * 分批保存实体；批量失败时回滚当前保存点并逐行重试定位失败对象。
     *
     * @param entityList       待保存实体
     * @param transactionStatus 当前事务状态
     * @param dataType         数据类型
     * @param errorMessageBuilder 单行失败消息构建器
     * @param <T>              实体类型
     */
    private <T extends BaseEntity> void batchSaveWithFallback(List<T> entityList, TransactionStatus transactionStatus,
                                                               String dataType,
                                                               BiFunction<T, RuntimeException, String> errorMessageBuilder) {
        if (CollUtil.isEmpty(entityList)) {
            return;
        }
        int batchSize = 1000;
        for (int startIndex = 0; startIndex < entityList.size(); startIndex += batchSize) {
            int endIndex = Math.min(startIndex + batchSize, entityList.size());
            List<T> batchList = entityList.subList(startIndex, endIndex);
            Object savepoint = transactionStatus.createSavepoint();
            try {
                Integer affectedRows = baseDao.saveBatch(batchList);
                if (!Objects.equals(affectedRows, batchList.size())) {
                    throw new ServiceException(MessageFormat.format(
                            I18nUtil.getMessage("ui.tc.schedule.batchAffectedRowsInvalid"),
                            batchList.size(), affectedRows));
                }
                transactionStatus.releaseSavepoint(savepoint);
            } catch (RuntimeException batchException) {
                transactionStatus.rollbackToSavepoint(savepoint);
                log.warn("[TC_SCHEDULE_PERSIST_BATCH_RETRY] dataType={}, batchStart={}, batchSize={}",
                        dataType, startIndex, batchList.size(), batchException);
                for (T entity : batchList) {
                    try {
                        int affectedRows = baseDao.save(entity);
                        if (affectedRows != 1) {
                            throw new ServiceException(MessageFormat.format(
                                    I18nUtil.getMessage("ui.tc.schedule.rowAffectedRowsInvalid"), affectedRows));
                        }
                    } catch (RuntimeException rowException) {
                        String errorMessage = errorMessageBuilder.apply(entity, rowException);
                        log.error("[TC_SCHEDULE_PERSIST_ROW_FAIL] dataType={}, errorMessage={}",
                                dataType, errorMessage, rowException);
                        throw new ServiceException(errorMessage);
                    }
                }
                transactionStatus.releaseSavepoint(savepoint);
            }
        }
    }

    /**
     * 判断任务是否属于未排任务。
     *
     * @param taskDraft 待排任务草稿
     * @return true 表示任务未分配到机台，需要写入未排表
     */
    private boolean isUnplannedTask(TcTaskDraft taskDraft) {
        if (taskDraft == null) {
            return false;
        }
        if (StrUtil.isNotBlank(taskDraft.getUnplannedReasonCode())) {
            return true;
        }
        // 零计划任务未进入任务链属于“无需生产”，只写解释，不应误写未排表。
        return taskDraft.isUnassigned() && taskDraft.getPlanQty() != null
                && taskDraft.getPlanQty().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 过滤需要写入结果表的可见排程结果。
     *
     * @param mergedResultList           已归并的结果行
     * @param mergedResultBusinessKeyMap 结果行与任务业务键映射
     * @return 至少存在一个正计划量班次的结果行
     */
    private List<TcScheduleResult> filterVisibleScheduleResults(List<TcScheduleResult> mergedResultList,
                                                                Map<TcScheduleResult, List<String>> mergedResultBusinessKeyMap) {
        List<TcScheduleResult> visibleResultList = new ArrayList<>();
        if (CollUtil.isEmpty(mergedResultList)) {
            return visibleResultList;
        }
        for (TcScheduleResult result : mergedResultList) {
            if (hasAnyPositivePlanQty(result)) {
                visibleResultList.add(result);
                continue;
            }
            if (mergedResultBusinessKeyMap != null) {
                mergedResultBusinessKeyMap.remove(result);
            }
        }
        return visibleResultList;
    }

    /**
     * 判断结果行是否存在任一正计划量班次。
     *
     * @param result 排程结果
     * @return true 表示结果行需要写入结果表
     */
    private boolean hasAnyPositivePlanQty(TcScheduleResult result) {
        if (result == null) {
            return false;
        }
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (isPositiveQty(getShiftPlanQty(result, shiftOrder))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按未排列表表结构归并未排任务。
     *
     * <p>按工厂、批次、日期、胎侧、胶料、口型、班次和原因归并，
     * 需求量和期望计划量直接写入未排列表，完整规则证据继续写入解释表。</p>
     *
     * @param unplannedTaskList 未排任务列表
     * @param context           胎侧排程上下文
     * @return 未排列表归并结果
     */
    private Map<String, TcScheduleUnplanned> buildMergedUnplannedMap(List<TcTaskDraft> unplannedTaskList,
                                                                     TcScheduleContext context) {
        Map<String, TcScheduleUnplanned> unplannedMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(unplannedTaskList)) {
            return unplannedMap;
        }
        for (TcTaskDraft taskDraft : unplannedTaskList) {
            TcSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TcScheduleUnplanned unplanned = buildScheduleUnplanned(taskDraft, snapshot, context);
            unplannedMap.putIfAbsent(buildUnplannedMergeKey(unplanned), unplanned);
        }
        return unplannedMap;
    }

    /**
     * 构建未排列表归并键。
     *
     * @param unplanned 未排列表实体
     * @return 归并键
     */
    private String buildUnplannedMergeKey(TcScheduleUnplanned unplanned) {
        Date scheduleDate = unplanned == null ? null : unplanned.getScheduleDate();
        return StrUtil.blankToDefault(unplanned == null ? null : unplanned.getFactoryCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getBatchNo(), "")
                + "|" + (scheduleDate == null ? "" : String.valueOf(scheduleDate.getTime()))
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getSidewallCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getGlueCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getMouthPlateCode(), "")
                + "|" + (unplanned == null || unplanned.getShiftOrder() == null ? "" : unplanned.getShiftOrder())
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getUnplannedReasonCode(), "");
    }

    /**
     * 构建胎侧未排任务实体。
     *
     * @param taskDraft 未排任务草稿
     * @param snapshot  解释快照，可为空
     * @param context   胎侧排程上下文
     * @return 胎侧未排任务实体
     */
    private TcScheduleUnplanned buildScheduleUnplanned(TcTaskDraft taskDraft, TcSnapshotBuildResult snapshot,
                                                       TcScheduleContext context) {
        TcScheduleUnplanned unplanned = new TcScheduleUnplanned();
        unplanned.setFactoryCode(context == null ? null : context.getFactoryCode());
        unplanned.setBatchNo(context == null ? null : context.getBatchNo());
        unplanned.setScheduleDate(context == null ? null : context.getScheduleDate());
        if (taskDraft != null) {
            // 保留未排任务稳定业务键，确保未排表可以与解释表关联追踪。
            unplanned.setTaskBusinessKey(taskDraft.getBusinessKey());
            unplanned.setSidewallCode(taskDraft.getSidewallCode());
            unplanned.setGlueCode(taskDraft.getGlueCode());
            unplanned.setMouthPlateCode(taskDraft.getMouthPlateCode());
            unplanned.setShiftOrder(taskDraft.getShiftOrder());
            unplanned.setDemandQty(taskDraft.getDemandQty());
            unplanned.setPlanQty(taskDraft.getPlanQty());
            unplanned.setUnplannedReasonCode(taskDraft.getUnplannedReasonCode());
            unplanned.setUnplannedReasonDesc(taskDraft.getUnplannedReasonDesc());
        }
        if (snapshot != null) {
            unplanned.setUnplannedEvidenceJson(snapshot.getUnplannedEvidenceJson());
        }
        return unplanned;
    }

    /**
     * 归一化结果表班次字段。
     *
     * <p>结果表只表示已经安排到机台产能的任务。机台为空或班次计划量为空/小于等于 0 时，
     * 对应班次顺序和起止时间必须清空，避免看板展示无产能承接的顺序。</p>
     *
     * @param result 排程结果
     */
    private void normalizeResultShiftFields(TcScheduleResult result) {
        if (result == null) {
            return;
        }
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            BigDecimal planQty = this.getShiftPlanQty(result, shiftOrder);
            if (StrUtil.isBlank(result.getMachineCode())) {
                result.setFieldValueByFieldName(planQtyField, BigDecimal.ZERO);
            }
            if (StrUtil.isBlank(result.getMachineCode()) || !this.isPositiveQty(planQty)) {
                this.clearShiftFields(result, shiftOrder);
            }
        }
    }

    /**
     * 按机台和班次重排结果表可见行顺序。
     *
     * <p>结果表展示顺序只面向已经落到机台且班次计划量大于 0 的可见结果行；原始任务链顺序仍保留在解释表和日志中。</p>
     *
     * @param resultList 已归并并归一化的排程结果列表
     */
    private void resequenceVisibleShiftSequences(List<TcScheduleResult> resultList) {
        if (CollUtil.isEmpty(resultList)) {
            return;
        }
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            Map<String, List<TcScheduleResult>> machineShiftResultMap = new LinkedHashMap<>();
            for (TcScheduleResult result : resultList) {
                if (!isVisibleShiftResult(result, shiftOrder)) {
                    continue;
                }
                String groupKey = StrUtil.blankToDefault(result.getMachineCode(), "") + "|" + shiftOrder;
                machineShiftResultMap.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(result);
            }
            for (List<TcScheduleResult> visibleResultList : machineShiftResultMap.values()) {
                resequenceSingleVisibleShift(visibleResultList, shiftOrder);
            }
        }
    }

    /**
     * 判断结果行在指定班次是否属于可见行。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return true 表示该班次应参与展示顺序重排
     */
    private boolean isVisibleShiftResult(TcScheduleResult result, int shiftOrder) {
        if (result == null || StrUtil.isBlank(result.getMachineCode())) {
            return false;
        }
        return isPositiveQty(getShiftPlanQty(result, shiftOrder));
    }

    /**
     * 重排单个机台班次的可见结果行。
     *
     * @param visibleResultList 可见结果行
     * @param shiftOrder        班次顺序
     */
    private void resequenceSingleVisibleShift(List<TcScheduleResult> visibleResultList, int shiftOrder) {
        visibleResultList.sort(Comparator
                .comparing((TcScheduleResult result) -> resolveSortSequence(result, shiftOrder))
                .thenComparing(result -> StrUtil.blankToDefault(result.getOrderNo(), ""))
                .thenComparing(result -> StrUtil.blankToDefault(result.getSidewallCode(), "")));
        for (int index = 0; index < visibleResultList.size(); index++) {
            setShiftSequence(visibleResultList.get(index), shiftOrder, index + 1);
        }
    }

    /**
     * 读取班次计划量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 班次计划量
     */
    private BigDecimal getShiftPlanQty(TcScheduleResult result, int shiftOrder) {
        Object planQty = result.getFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
        return planQty instanceof BigDecimal ? (BigDecimal) planQty : null;
    }

    /**
     * 设置班次展示顺序。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @param sequence   展示顺序
     */
    private void setShiftSequence(TcScheduleResult result, int shiftOrder, Integer sequence) {
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), sequence);
    }

    /**
     * 读取重排前的班次顺序，空顺序排在最后。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 排序用顺序值
     */
    private Integer resolveSortSequence(TcScheduleResult result, int shiftOrder) {
        Object sequence = result.getFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        if (sequence instanceof Integer) {
            return (Integer) sequence;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 判断计划量是否大于 0。
     *
     * @param planQty 班次计划量
     * @return true 表示计划量大于 0
     */
    private boolean isPositiveQty(BigDecimal planQty) {
        return planQty != null && planQty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 清空指定班次的顺序和时间字段。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     */
    private void clearShiftFields(TcScheduleResult result, int shiftOrder) {
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), null);
    }
    /**
     * 将任务链转换后的结果实体与原任务业务键建立关联。
     *
     * @param nodeList             任务链节点列表
     * @param chainResultList      任务链转换后的结果实体列表
     * @param resultBusinessKeyMap 结果实体与任务业务键映射
     */
    private void registerChainResultBusinessKey(List<ScheduleTaskNode<TcTaskDraft>> nodeList,
                                                List<TcScheduleResult> chainResultList,
                                                Map<TcScheduleResult, List<String>> resultBusinessKeyMap) {
        if (CollUtil.isEmpty(chainResultList)) {
            return;
        }
        if (nodeList == null || nodeList.size() != chainResultList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.resultRelationFailed"));
        }
        for (int index = 0; index < chainResultList.size(); index++) {
            ScheduleTaskNode<TcTaskDraft> node = nodeList.get(index);
            TcTaskDraft task = node == null ? null : node.getTask();
            addResultBusinessKey(resultBusinessKeyMap, chainResultList.get(index), task == null ? null : task.getBusinessKey());
        }
    }

    /**
     * 记录结果实体和任务业务键的关联。
     *
     * @param resultBusinessKeyMap 结果实体与任务业务键映射
     * @param result               排程结果
     * @param businessKey          任务业务键
     */
    private void addResultBusinessKey(Map<TcScheduleResult, List<String>> resultBusinessKeyMap,
                                      TcScheduleResult result, String businessKey) {
        if (result == null || StrUtil.isBlank(businessKey)) {
            return;
        }
        resultBusinessKeyMap.computeIfAbsent(result, key -> new ArrayList<>()).add(businessKey);
    }

    /**
     * 归并胎侧排程结果行。
     *
     * <p>自动排程结果按工厂、批次、日期、机台和胎侧规格落一行；同一行的 1 到 6 班计划量横向写入。
     * 未排任务机台为空时也按同一胎侧归并，避免同一胎侧被拆成多条结果。</p>
     *
     * @param resultList              引擎转换出的原始结果
     * @param resultBusinessKeyMap    原始结果与任务业务键映射
     * @param mergedBusinessKeyMap    归并结果与任务业务键映射
     * @return 归并后的结果列表
     */
    private List<TcScheduleResult> mergeScheduleResults(List<TcScheduleResult> resultList,
                                                        Map<TcScheduleResult, List<String>> resultBusinessKeyMap,
                                                        Map<TcScheduleResult, List<String>> mergedBusinessKeyMap) {
        Map<String, TcScheduleResult> mergedResultMap = new LinkedHashMap<>();
        for (TcScheduleResult result : resultList) {
            String mergeKey = buildResultMergeKey(result);
            TcScheduleResult merged = mergedResultMap.get(mergeKey);
            if (merged == null) {
                mergedResultMap.put(mergeKey, result);
                mergedBusinessKeyMap.put(result, new ArrayList<>());
                appendBusinessKeys(mergedBusinessKeyMap.get(result), resultBusinessKeyMap.get(result));
                continue;
            }
            mergeResultShiftFields(merged, result);
            appendBusinessKeys(mergedBusinessKeyMap.get(merged), resultBusinessKeyMap.get(result));
        }
        return new ArrayList<>(mergedResultMap.values());
    }

    /**
     * 构建结果行归并键。
     *
     * @param result 排程结果
     * @return 归并键
     */
    private String buildResultMergeKey(TcScheduleResult result) {
        Date scheduleDate = result == null ? null : result.getScheduleDate();
        return StrUtil.blankToDefault(result == null ? null : result.getFactoryCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getBatchNo(), "")
                + "|" + (scheduleDate == null ? "" : String.valueOf(scheduleDate.getTime()))
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getMachineCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getSidewallCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getConstructionVersion(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getSidewallCraft(), "")
                + "|" + String.valueOf(result == null ? null : result.getSidewallLength())
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getGlueCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getBaseGlueCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getWholeGlueCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getGlueSeq(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getMouthPlateCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getTailFlag(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getDataSource(), "");
    }

    /**
     * 追加任务业务键。
     *
     * @param target 目标业务键列表
     * @param source 来源业务键列表
     */
    private void appendBusinessKeys(List<String> target, List<String> source) {
        if (target == null || CollUtil.isEmpty(source)) {
            return;
        }
        for (String businessKey : source) {
            if (StrUtil.isNotBlank(businessKey) && !target.contains(businessKey)) {
                target.add(businessKey);
            }
        }
    }

    /**
     * 生成胎侧自动排程结果工单号。
     *
     * @param resultList 归并后的结果列表
     * @param batchNo    胎侧自动排程批次号
     */
    private void assignTcOrderNo(List<TcScheduleResult> resultList, String batchNo) {
        if (CollUtil.isEmpty(resultList)) {
            return;
        }
        for (int index = 0; index < resultList.size(); index++) {
            resultList.get(index).setOrderNo(StrUtil.blankToDefault(batchNo, "") + "-" + String.format("%04d", index + 1));
        }
    }

    /**
     * 合并排程结果的横向班次字段。
     *
     * @param target 目标结果
     * @param source 来源结果
     */
    private void mergeResultShiftFields(TcScheduleResult target, TcScheduleResult source) {
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            String sequenceField = String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            String startTimeField = String.format(TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder);
            String endTimeField = String.format(TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder);
            BigDecimal targetPlanQty = (BigDecimal) target.getFieldValueByFieldName(planQtyField);
            BigDecimal sourcePlanQty = (BigDecimal) source.getFieldValueByFieldName(planQtyField);
            Integer targetSequence = (Integer) target.getFieldValueByFieldName(sequenceField);
            Integer sourceSequence = (Integer) source.getFieldValueByFieldName(sequenceField);
            Date targetStartTime = (Date) target.getFieldValueByFieldName(startTimeField);
            Date sourceStartTime = (Date) source.getFieldValueByFieldName(startTimeField);
            Date targetEndTime = (Date) target.getFieldValueByFieldName(endTimeField);
            Date sourceEndTime = (Date) source.getFieldValueByFieldName(endTimeField);
            target.setFieldValueByFieldName(planQtyField, this.addQty(targetPlanQty, sourcePlanQty));
            target.setFieldValueByFieldName(sequenceField, this.minSequence(targetSequence, sourceSequence));
            target.setFieldValueByFieldName(startTimeField, this.minDate(targetStartTime, sourceStartTime));
            target.setFieldValueByFieldName(endTimeField, this.maxDate(targetEndTime, sourceEndTime));
        }
    }

    /**
     * 累加班次计划量，空来源不覆盖已有值。
     *
     * @param target 目标计划量
     * @param source 来源计划量
     * @return 累加后的计划量
     */
    private BigDecimal addQty(BigDecimal target, BigDecimal source) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            return source;
        }
        return target.add(source);
    }

    /**
     * 选择较小班内顺序。
     *
     * @param target 目标顺序
     * @param source 来源顺序
     * @return 较小顺序
     */
    private Integer minSequence(Integer target, Integer source) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            return source;
        }
        return Math.min(target, source);
    }

    /**
     * 选择较早时间。
     *
     * @param target 目标时间
     * @param source 来源时间
     * @return 较早时间
     */
    private Date minDate(Date target, Date source) {
        if (source == null) {
            return target;
        }
        if (target == null || source.before(target)) {
            return source;
        }
        return target;
    }

    /**
     * 选择较晚时间。
     *
     * @param target 目标时间
     * @param source 来源时间
     * @return 较晚时间
     */
    private Date maxDate(Date target, Date source) {
        if (source == null) {
            return target;
        }
        if (target == null || source.after(target)) {
            return source;
        }
        return target;
    }

    /**
     * 记录结果表插入后回填的主键。
     *
     * @param result               已插入的排程结果
     * @param resultBusinessKeyMap 结果实体与任务业务键映射
     * @param resultIdMap          任务业务键与结果表主键映射
     */
    private void registerInsertedResultId(TcScheduleResult result, Map<TcScheduleResult, List<String>> resultBusinessKeyMap,
                                          Map<String, Long> resultIdMap) {
        List<String> businessKeyList = resultBusinessKeyMap.get(result);
        if (CollUtil.isEmpty(businessKeyList) || result == null || result.getId() == null) {
            return;
        }
        for (String businessKey : businessKeyList) {
            if (StrUtil.isNotBlank(businessKey)) {
                resultIdMap.put(businessKey, result.getId());
            }
        }
    }

    /**
     * 根据任务业务键读取解释表可关联的结果表主键。
     *
     * <p>零计划量任务不写入结果表，解释行允许不关联 resultId；正计划任务仍要求存在结果行。</p>
     *
     * @param taskDraft   待排任务
     * @param resultIdMap 任务业务键与结果表主键映射
     * @return 结果表主键；零计划或未排任务返回 null
     */
    private Long resolveOptionalResultId(TcTaskDraft taskDraft, Map<String, Long> resultIdMap) {
        if (isUnplannedTask(taskDraft) || taskDraft == null || !isPositiveQty(taskDraft.getPlanQty())) {
            return null;
        }
        String businessKey = taskDraft.getBusinessKey();
        Long resultId = StrUtil.isBlank(businessKey) ? null : resultIdMap.get(businessKey);
        if (resultId == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.explainResultMissing"));
        }
        return resultId;
    }

    /**
     * 构建结果表落库失败信息。
     *
     * @param result 排程结果
     * @param ex     异常
     * @return 错误信息
     */
    private String buildResultErrorMsg(TcScheduleResult result, RuntimeException ex) {
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.resultRowPersistFailed"),
                result == null ? null : result.getOrderNo(),
                result == null ? null : result.getSidewallCode(),
                result == null ? null : result.getMachineCode(), ex == null ? null : ex.getMessage());
    }

    /**
     * 构建未排表落库失败信息。
     *
     * @param unplanned 未排任务
     * @param ex        异常
     * @return 错误信息
     */
    private String buildUnplannedErrorMsg(TcScheduleUnplanned unplanned, RuntimeException ex) {
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.unplannedRowPersistFailed"),
                unplanned == null ? null : unplanned.getBatchNo(),
                unplanned == null ? null : unplanned.getSidewallCode(), ex == null ? null : ex.getMessage());
    }

    /**
     * 构建解释表落库失败信息。
     *
     * @param taskDraft 待排任务
     * @param ex        异常
     * @return 错误信息
     */
    private String buildExplainErrorMsg(TcTaskDraft taskDraft, RuntimeException ex) {
        return MessageFormat.format(I18nUtil.getMessage("ui.tc.schedule.explainRowPersistFailed"),
                taskDraft == null ? null : taskDraft.getOrderNo(),
                taskDraft == null ? null : taskDraft.getSidewallCode(), ex == null ? null : ex.getMessage());
    }
}
