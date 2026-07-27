package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationItem;
import com.zlt.aps.common.engine.quantity.PlanQuantityAllocationUtils;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmScheduleExplainTargetRel;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.enums.TmMachineAssignStatusEnum;
import com.zlt.aps.tm.api.enums.TmScheduleRuleCodeEnum;
import com.zlt.aps.tm.engine.domain.*;
import com.zlt.aps.tm.engine.service.ITmSnapshotAndPersistService;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import com.zlt.aps.tm.engine.service.impl.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleExplainTargetRelMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
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
 * 胎面自动排程业务快照和落库步骤服务。
 *
 * <p>负责在模板快照阶段生成解释快照，将任务链转换为排程结果并写入排程结果与解释表。</p>
 */
@Slf4j
@Primary
@Service
public class TmBizSnapshotAndPersistService implements ITmSnapshotAndPersistService {

    private final TmSnapshotBuildService snapshotBuildService;

    private final TmPersistService persistService;

    private final TmScheduleResultMapper scheduleResultMapper;

    private final TmScheduleResultExplainMapper scheduleResultExplainMapper;

    private final TmScheduleUnplannedMapper scheduleUnplannedMapper;

    private final TmScheduleExplainTargetRelMapper scheduleExplainTargetRelMapper;

    /** 通用批量写入服务。 */
    private final BaseDao baseDao;

    /** 最终持久化短事务模板。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建胎面自动排程业务快照和落库步骤服务。
     *
     * @param snapshotBuildService        解释快照构建服务
     * @param persistService              落库实体转换服务
     * @param scheduleResultMapper        胎面排程结果 Mapper
     * @param scheduleResultExplainMapper 胎面排程解释 Mapper
     * @param scheduleUnplannedMapper     胎面未排任务 Mapper
     * @param scheduleExplainTargetRelMapper 胎面来源解释目标关联 Mapper
     * @param baseDao                     通用批量写入服务
     * @param transactionManager          事务管理器
     */
    @Autowired
    public TmBizSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService,
                                          TmPersistService persistService,
                                          TmScheduleResultMapper scheduleResultMapper,
                                          TmScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TmScheduleUnplannedMapper scheduleUnplannedMapper,
                                          TmScheduleExplainTargetRelMapper scheduleExplainTargetRelMapper,
                                          BaseDao baseDao,
                                          PlatformTransactionManager transactionManager) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.scheduleExplainTargetRelMapper = scheduleExplainTargetRelMapper;
        this.baseDao = baseDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 创建兼容旧测试装配的胎面快照和落库服务。
     *
     * @param snapshotBuildService 解释快照构建服务
     * @param persistService 落库实体转换服务
     * @param scheduleResultMapper 胎面排程结果 Mapper
     * @param scheduleResultExplainMapper 胎面排程解释 Mapper
     * @param scheduleUnplannedMapper 胎面未排任务 Mapper
     * @param baseDao 通用批量写入服务
     * @param transactionManager 事务管理器
     */
    public TmBizSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService,
                                          TmPersistService persistService,
                                          TmScheduleResultMapper scheduleResultMapper,
                                          TmScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TmScheduleUnplannedMapper scheduleUnplannedMapper,
                                          BaseDao baseDao,
                                          PlatformTransactionManager transactionManager) {
        this(snapshotBuildService, persistService, scheduleResultMapper, scheduleResultExplainMapper,
                scheduleUnplannedMapper, null, baseDao, transactionManager);
    }

    /**
     * 执行解释快照构建和实际落库。
     *
     * @param context 胎面排程上下文，需包含已完成机台分配的任务链
     */
    @Override
    public void snapshotAndPersist(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("胎面排程上下文不能为空");
        }
        this.prepareSourceTaskAllocations(context);
        // 解释快照在事务外完成；最终事务只承担旧批次删除与新批次写入。
        this.buildSnapshot(context);
        TmPersistResult persistResult = transactionTemplate.execute(transactionStatus -> {
            this.logicDeleteOldSchedule(context);
            return this.persistScheduleContext(context, transactionStatus);
        });
        if (persistResult == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.persistFailed"));
        }
        context.setPersistResult(persistResult);
    }

    /**
     * 构建所有待排任务的解释快照。
     *
     * @param context 胎面排程上下文
     */
    private void buildSnapshot(TmScheduleContext context) {
        List<TmTaskDraft> explainTaskList = this.resolveExplainTaskList(context);
        if (CollUtil.isEmpty(explainTaskList) && CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        // 先为实际排程片段构建快照，保证未排表可以拿到片段级未排证据。
        for (TmTaskDraft task : context.getTaskDraftList()) {
            TmSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
            context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
        }
        // 再构建来源解释快照；服务内部会按同一汇总组聚合片段候选证据。
        for (TmTaskDraft task : explainTaskList) {
            TmSnapshotBuildResult snapshot = snapshotBuildService.buildTaskExplain(task, context);
            context.getSnapshotMap().put(task.getBusinessKey(), snapshot);
        }
    }

    /**
     * 将自动排程上下文中的任务结果和解释写入数据库。
     *
     * @param context 自动排程上下文
     * @return 落库汇总
     */
    private TmPersistResult persistScheduleContext(TmScheduleContext context, TransactionStatus transactionStatus) {
        TmPersistResult persistResult = new TmPersistResult();
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return persistResult;
        }
        List<TmScheduleResult> resultList = new ArrayList<>();
        List<TmTaskDraft> unplannedTaskList = new ArrayList<>();
        Map<TmScheduleResult, List<String>> resultBusinessKeyMap = new IdentityHashMap<>();
        Map<String, TmTaskDraft> taskBusinessKeyMap = new LinkedHashMap<>();
        Map<String, Long> resultIdMap = new HashMap<>();
        Map<String, Long> unplannedIdMap = new HashMap<>();
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            if (isUnplannedTask(taskDraft)) {
                unplannedTaskList.add(taskDraft);
            }
        }
        for (ScheduleTaskLinkedList<TmTaskDraft> chain : context.getTaskChainGroup().values()) {
            List<ScheduleTaskNode<TmTaskDraft>> nodeList = chain.toList();
            nodeList.stream()
                    .filter(Objects::nonNull)
                    .map(ScheduleTaskNode::getTask)
                    .filter(Objects::nonNull)
                    .forEach(task -> taskBusinessKeyMap.putIfAbsent(task.getBusinessKey(), task));
            List<TmScheduleResult> chainResultList = persistService.convertChainToResult(chain, context);
            registerChainResultBusinessKey(nodeList, chainResultList, resultBusinessKeyMap);
            resultList.addAll(chainResultList);
        }
        Map<TmScheduleResult, List<String>> mergedResultBusinessKeyMap = new IdentityHashMap<>();
        List<TmScheduleResult> mergedResultList = mergeScheduleResults(resultList, resultBusinessKeyMap, mergedResultBusinessKeyMap);
        for (TmScheduleResult result : mergedResultList) {
            this.aggregateResultSnapshot(result, mergedResultBusinessKeyMap, taskBusinessKeyMap);
            normalizeResultShiftFields(result);
        }
        List<TmScheduleResult> visibleResultList = filterVisibleScheduleResults(mergedResultList, mergedResultBusinessKeyMap);
        assignTmOrderNo(visibleResultList, context.getBatchNo());
        resequenceVisibleShiftSequences(visibleResultList);
        this.batchSaveWithFallback(visibleResultList, transactionStatus, "RESULT", this::buildResultErrorMsg);
        for (TmScheduleResult result : visibleResultList) {
            this.registerInsertedResultId(result, mergedResultBusinessKeyMap, resultIdMap);
        }
        persistResult.setResultCount(visibleResultList.size());
        // 未排表落库统一批量写入，批量失败时回滚保存点并逐行定位失败对象。
        Map<String, TmScheduleUnplanned> unplannedMap = this.buildMergedUnplannedMap(unplannedTaskList, context);
        if (CollUtil.isNotEmpty(unplannedMap)) {
            List<TmScheduleUnplanned> unplannedList = new ArrayList<>(unplannedMap.values());
            this.batchSaveWithFallback(unplannedList, transactionStatus,
                    TmMachineAssignStatusEnum.UNPLANNED.getCode(), this::buildUnplannedErrorMsg);
            for (TmTaskDraft unplannedTask : unplannedTaskList) {
                TmScheduleUnplanned unplanned = unplannedMap.get(this.buildUnplannedMergeKey(
                        this.buildScheduleUnplanned(unplannedTask,
                                context.getSnapshotMap().get(unplannedTask.getBusinessKey()), context)));
                if (unplanned != null && unplanned.getId() != null) {
                    unplannedIdMap.put(unplannedTask.getBusinessKey(), unplanned.getId());
                }
            }
            persistResult.setUnplannedCount(unplannedList.size());
        }
        // 解释表落库统一批量写入，任何单行失败都会回滚整个最终事务。
        List<TmScheduleResultExplain> explainList = new ArrayList<>();
        for (TmTaskDraft taskDraft : this.resolveExplainTaskList(context)) {
            TmSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TmScheduleResultExplain explain = persistService.convertExplain(taskDraft, snapshot, context);
            Long resultId = this.resolveSourceSingleResultId(taskDraft, context, resultIdMap);
            if (resultId != null) {
                explain.setResultId(resultId);
            }
            explainList.add(explain);
        }
        if (CollUtil.isNotEmpty(explainList)) {
            this.batchSaveWithFallback(explainList, transactionStatus, "EXPLAIN",
                    (explain, exception) -> this.buildExplainErrorMsg(null, exception));
            persistResult.setExplainCount(explainList.size());
        }
        List<TmScheduleExplainTargetRel> targetRelList = this.buildExplainTargetRelList(
                context, explainList, resultIdMap, unplannedIdMap);
        if (CollUtil.isNotEmpty(targetRelList) && scheduleExplainTargetRelMapper != null) {
            this.batchSaveWithFallback(targetRelList, transactionStatus, "EXPLAIN_TARGET_REL",
                    (relation, exception) -> MessageFormat.format(
                            I18nUtil.getMessage("ui.tm.schedule.explainTargetPersistFailed"),
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
    private List<TmTaskDraft> resolveExplainTaskList(TmScheduleContext context) {
        return CollUtil.isNotEmpty(context.getSourceTaskDraftList())
                ? context.getSourceTaskDraftList() : context.getTaskDraftList();
    }

    /**
     * 按机台分配后的实际结果片段重新汇总并分摊来源最终计划量。
     *
     * @param context 排程上下文
     */
    private void prepareSourceTaskAllocations(TmScheduleContext context) {
        if (context == null || CollUtil.isEmpty(context.getPlanTaskGroupMap())) {
            return;
        }
        Map<String, List<TmTaskDraft>> fragmentMap = context.getTaskDraftList().stream()
                .filter(Objects::nonNull)
                .filter(task -> StrUtil.isNotBlank(task.getPlanGroupKey()))
                .collect(java.util.stream.Collectors.groupingBy(TmTaskDraft::getPlanGroupKey,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        for (TmPlanTaskGroup taskGroup : context.getPlanTaskGroupMap().values()) {
            List<TmTaskDraft> fragmentList = fragmentMap.getOrDefault(
                    taskGroup.getPlanGroupKey(), Collections.emptyList());
            Map<String, BigDecimal> sourceFinalQtyMap = taskGroup.getSourceTaskList().stream()
                    .collect(java.util.stream.Collectors.toMap(TmTaskDraft::getBusinessKey,
                            task -> BigDecimal.ZERO, BigDecimal::add, LinkedHashMap::new));
            for (TmTaskDraft fragment : fragmentList) {
                Map<String, BigDecimal> fragmentAllocationMap = this.allocateByWeight(
                        fragment.getPlanQty(), taskGroup.getSourceWeightMap());
                fragmentAllocationMap.forEach((sourceBusinessKey, allocatedQty) ->
                        sourceFinalQtyMap.merge(sourceBusinessKey, allocatedQty, BigDecimal::add));
            }
            BigDecimal groupFinalPlanQty = fragmentList.stream().map(TmTaskDraft::getPlanQty)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            taskGroup.setGroupFinalPlanQty(groupFinalPlanQty);
            List<TmTaskDraft> assignedFragmentList = fragmentList.stream()
                    .filter(task -> !this.isUnplannedTask(task) && this.isPositiveQty(task.getPlanQty()))
                    .collect(java.util.stream.Collectors.toList());
            boolean allUnplanned = CollUtil.isNotEmpty(fragmentList)
                    && assignedFragmentList.isEmpty() && groupFinalPlanQty.compareTo(BigDecimal.ZERO) > 0;
            for (TmTaskDraft sourceTask : taskGroup.getSourceTaskList()) {
                sourceTask.setPlanQty(sourceFinalQtyMap.getOrDefault(sourceTask.getBusinessKey(), BigDecimal.ZERO));
                sourceTask.setGroupFinalPlanQty(groupFinalPlanQty);
                if (context.getRuleTraceMap().get(sourceTask.getBusinessKey()) != null) {
                    context.getRuleTraceMap().get(sourceTask.getBusinessKey()).getRuleHits().stream()
                            .filter(item -> TmScheduleRuleCodeEnum.PLAN_QTY_SOURCE_ALLOCATE.getCode()
                                    .equals(item.getRuleCode()))
                            .filter(item -> item.getEvidence() instanceof Map)
                            .forEach(item -> {
                                Map<String, Object> evidence = (Map<String, Object>) item.getEvidence();
                                evidence.put("allocatedPlanQty", sourceTask.getPlanQty());
                                evidence.put("allocationStage", "SNAPSHOT");
                            });
                }
                sourceTask.setMachineCode(assignedFragmentList.isEmpty()
                        ? null : assignedFragmentList.get(0).getMachineCode());
                if (allUnplanned) {
                    TmTaskDraft unplannedFragment = fragmentList.get(0);
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
    private Long resolveSourceSingleResultId(TmTaskDraft sourceTask, TmScheduleContext context,
                                             Map<String, Long> resultIdMap) {
        List<TmTaskDraft> groupFragmentList = context.getTaskDraftList().stream()
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
     * 构建来源解释与结果/未排片段关联记录。
     *
     * @param context 排程上下文
     * @param explainList 已保存解释列表
     * @param resultIdMap 任务业务键与结果主键映射
     * @param unplannedIdMap 任务业务键与未排主键映射
     * @return 关联记录列表
     */
    private List<TmScheduleExplainTargetRel> buildExplainTargetRelList(TmScheduleContext context,
                                                                       List<TmScheduleResultExplain> explainList,
                                                                       Map<String, Long> resultIdMap,
                                                                       Map<String, Long> unplannedIdMap) {
        Map<String, TmScheduleResultExplain> explainMap = explainList.stream()
                .collect(java.util.stream.Collectors.toMap(TmScheduleResultExplain::getTaskBusinessKey,
                        explain -> explain, (first, second) -> first, LinkedHashMap::new));
        List<TmScheduleExplainTargetRel> relationList = new ArrayList<>();
        for (TmPlanTaskGroup taskGroup : context.getPlanTaskGroupMap().values()) {
            List<TmTaskDraft> fragmentList = context.getTaskDraftList().stream()
                    .filter(task -> Objects.equals(taskGroup.getPlanGroupKey(), task.getPlanGroupKey()))
                    .filter(task -> this.isPositiveQty(task.getPlanQty()))
                    .collect(java.util.stream.Collectors.toList());
            for (TmTaskDraft fragment : fragmentList) {
                Map<String, BigDecimal> allocationMap = this.allocateByWeight(
                        fragment.getPlanQty(), taskGroup.getSourceWeightMap());
                for (Map.Entry<String, BigDecimal> allocationEntry : allocationMap.entrySet()) {
                    if (!this.isPositiveQty(allocationEntry.getValue())) {
                        continue;
                    }
                    TmScheduleResultExplain explain = explainMap.get(allocationEntry.getKey());
                    if (explain == null) {
                        continue;
                    }
                    TmScheduleExplainTargetRel relation = new TmScheduleExplainTargetRel();
                    relation.setFactoryCode(context.getFactoryCode());
                    relation.setBatchNo(context.getBatchNo());
                    relation.setScheduleDate(context.getScheduleDate());
                    relation.setExplainId(explain.getId());
                    relation.setPlanGroupKey(taskGroup.getPlanGroupKey());
                    relation.setSourceTaskBusinessKey(allocationEntry.getKey());
                    boolean unplanned = this.isUnplannedTask(fragment);
                    relation.setTargetType(unplanned ? "UNPLANNED" : "RESULT");
                    Long targetId = unplanned
                            ? unplannedIdMap.get(fragment.getBusinessKey())
                            : resultIdMap.get(fragment.getBusinessKey());
                    if (targetId == null) {
                        throw new ServiceException(MessageFormat.format(
                                I18nUtil.getMessage("ui.tm.schedule.explainTargetMissing"),
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
     * 按来源权重分摊指定数量。
     *
     * @param totalQty 汇总数量
     * @param sourceWeightMap 来源权重
     * @return 来源业务键与分摊数量映射
     */
    private Map<String, BigDecimal> allocateByWeight(BigDecimal totalQty,
                                                     Map<String, BigDecimal> sourceWeightMap) {
        List<PlanQuantityAllocationItem> allocationItemList = sourceWeightMap.entrySet().stream()
                .map(entry -> new PlanQuantityAllocationItem(entry.getKey(), entry.getValue(), BigDecimal.ZERO))
                .collect(java.util.stream.Collectors.toList());
        return PlanQuantityAllocationUtils.allocate(totalQty, allocationItemList,
                        TmScheduleConstants.DECIMAL_CALCULATION_SCALE).stream()
                .collect(java.util.stream.Collectors.toMap(PlanQuantityAllocationItem::getSourceBusinessKey,
                        PlanQuantityAllocationItem::getAllocatedQty,
                        BigDecimal::add, LinkedHashMap::new));
    }

    /**
     * 按归并结果关联的任务业务键汇总来源快照。
     *
     * @param result                     已归并的胎面排程结果
     * @param mergedResultBusinessKeyMap 归并结果与任务业务键映射
     * @param taskBusinessKeyMap         任务业务键与任务映射
     */
    private void aggregateResultSnapshot(TmScheduleResult result,
                                         Map<TmScheduleResult, List<String>> mergedResultBusinessKeyMap,
                                         Map<String, TmTaskDraft> taskBusinessKeyMap) {
        List<String> businessKeyList = mergedResultBusinessKeyMap.get(result);
        if (CollUtil.isEmpty(businessKeyList) || CollUtil.isEmpty(taskBusinessKeyMap)) {
            return;
        }
        List<TmTaskDraft> sourceTaskList = businessKeyList.stream()
                .map(taskBusinessKeyMap::get)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        TmScheduleResultSnapshotAssembler.assemble(result, sourceTaskList);
    }

    /**
     * 删除同工厂、同排程日期的旧解释、未排和结果。
     *
     * <p>该方法只在最终短事务内调用，删除和新批次写入任一失败都会整体回滚。</p>
     *
     * @param context 自动排程上下文
     */
    private void logicDeleteOldSchedule(TmScheduleContext context) {
        LambdaQueryWrapper<TmScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.eq(TmScheduleResult::getFactoryCode, context.getFactoryCode());
        resultWrapper.eq(TmScheduleResult::getScheduleDate, context.getScheduleDate());
        List<TmScheduleResult> oldResultList = scheduleResultMapper.selectList(resultWrapper);

        LambdaQueryWrapper<TmScheduleUnplanned> unplannedWrapper = new LambdaQueryWrapper<>();
        unplannedWrapper.eq(TmScheduleUnplanned::getFactoryCode, context.getFactoryCode());
        unplannedWrapper.eq(TmScheduleUnplanned::getScheduleDate, context.getScheduleDate());
        List<TmScheduleUnplanned> oldUnplannedList = scheduleUnplannedMapper.selectList(unplannedWrapper);

        Set<String> oldBatchNoSet = new LinkedHashSet<>();
        oldResultList.stream().map(TmScheduleResult::getBatchNo).filter(StrUtil::isNotBlank).forEach(oldBatchNoSet::add);
        oldUnplannedList.stream().map(TmScheduleUnplanned::getBatchNo).filter(StrUtil::isNotBlank).forEach(oldBatchNoSet::add);
        if (CollUtil.isNotEmpty(oldBatchNoSet)) {
            scheduleResultExplainMapper.logicDeleteByFactoryCodeAndBatchNos(context.getFactoryCode(),
                    new ArrayList<>(oldBatchNoSet));
        }
        LambdaQueryWrapper<TmScheduleExplainTargetRel> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(TmScheduleExplainTargetRel::getFactoryCode, context.getFactoryCode());
        relationWrapper.eq(TmScheduleExplainTargetRel::getScheduleDate, context.getScheduleDate());
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
                    throw new ServiceException("批量写入影响行数异常，expected=" + batchList.size()
                            + "，actual=" + affectedRows);
                }
                transactionStatus.releaseSavepoint(savepoint);
            } catch (RuntimeException batchException) {
                transactionStatus.rollbackToSavepoint(savepoint);
                log.warn("[TM_SCHEDULE_PERSIST_BATCH_RETRY] dataType={}, batchStart={}, batchSize={}",
                        dataType, startIndex, batchList.size(), batchException);
                for (T entity : batchList) {
                    try {
                        int affectedRows = baseDao.save(entity);
                        if (affectedRows != 1) {
                            throw new ServiceException(MessageFormat.format(
                                    I18nUtil.getMessage("ui.data.alert.tm.schedule.rowAffectedRowsInvalid"), affectedRows));
                        }
                    } catch (RuntimeException rowException) {
                        String errorMessage = errorMessageBuilder.apply(entity, rowException);
                        log.error("[TM_SCHEDULE_PERSIST_ROW_FAIL] dataType={}, errorMessage={}",
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
    private boolean isUnplannedTask(TmTaskDraft taskDraft) {
        if (taskDraft == null) {
            return false;
        }
        if (StrUtil.isNotBlank(taskDraft.getUnplannedReasonCode())) {
            return true;
        }
        return taskDraft.isUnassigned() && isPositiveQty(taskDraft.getPlanQty());
    }

    /**
     * 过滤需要写入结果表的可见排程结果。
     *
     * @param mergedResultList           已归并的结果行
     * @param mergedResultBusinessKeyMap 结果行与任务业务键映射
     * @return 至少存在一个正计划量班次的结果行
     */
    private List<TmScheduleResult> filterVisibleScheduleResults(List<TmScheduleResult> mergedResultList,
                                                                Map<TmScheduleResult, List<String>> mergedResultBusinessKeyMap) {
        List<TmScheduleResult> visibleResultList = new ArrayList<>();
        if (CollUtil.isEmpty(mergedResultList)) {
            return visibleResultList;
        }
        for (TmScheduleResult result : mergedResultList) {
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
    private boolean hasAnyPositivePlanQty(TmScheduleResult result) {
        if (result == null) {
            return false;
        }
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (isPositiveQty(getShiftPlanQty(result, shiftOrder))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按未排列表表结构归并未排任务。
     *
     * <p>未排表没有班次和来源工单字段，因此同一工厂、批次、日期、胎面、胶料、口型和原因只写一行；
     * 任务级班次和计划量追溯继续写入解释表。</p>
     *
     * @param unplannedTaskList 未排任务列表
     * @param context           胎面排程上下文
     * @return 未排列表归并结果
     */
    private Map<String, TmScheduleUnplanned> buildMergedUnplannedMap(List<TmTaskDraft> unplannedTaskList,
                                                                     TmScheduleContext context) {
        Map<String, TmScheduleUnplanned> unplannedMap = new LinkedHashMap<>();
        if (CollUtil.isEmpty(unplannedTaskList)) {
            return unplannedMap;
        }
        for (TmTaskDraft taskDraft : unplannedTaskList) {
            TmSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TmScheduleUnplanned unplanned = buildScheduleUnplanned(taskDraft, snapshot, context);
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
    private String buildUnplannedMergeKey(TmScheduleUnplanned unplanned) {
        Date scheduleDate = unplanned == null ? null : unplanned.getScheduleDate();
        return StrUtil.blankToDefault(unplanned == null ? null : unplanned.getFactoryCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getBatchNo(), "")
                + "|" + (scheduleDate == null ? "" : String.valueOf(scheduleDate.getTime()))
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getTreadCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getGlueCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getMouthPlateCode(), "")
                + "|" + StrUtil.blankToDefault(unplanned == null ? null : unplanned.getUnplannedReasonCode(), "");
    }

    /**
     * 构建胎面未排任务实体。
     *
     * @param taskDraft 未排任务草稿
     * @param snapshot  解释快照，可为空
     * @param context   胎面排程上下文
     * @return 胎面未排任务实体
     */
    private TmScheduleUnplanned buildScheduleUnplanned(TmTaskDraft taskDraft, TmSnapshotBuildResult snapshot,
                                                       TmScheduleContext context) {
        TmScheduleUnplanned unplanned = new TmScheduleUnplanned();
        unplanned.setFactoryCode(context == null ? null : context.getFactoryCode());
        unplanned.setBatchNo(context == null ? null : context.getBatchNo());
        unplanned.setScheduleDate(context == null ? null : context.getScheduleDate());
        if (taskDraft != null) {
            unplanned.setTreadCode(taskDraft.getTreadCode());
            unplanned.setGlueCode(taskDraft.getGlueCode());
            unplanned.setMouthPlateCode(taskDraft.getMouthPlateCode());
            unplanned.setUnplannedReasonCode(taskDraft.getUnplannedReasonCode());
            unplanned.setUnplannedReasonDesc(taskDraft.getUnplannedReasonDesc());
        }
        if (snapshot != null) {
            unplanned.setUnplannedEvidenceJson(snapshot.getUnplannedEvidenceJson());
        }
        if (StrUtil.isBlank(unplanned.getUnplannedEvidenceJson()) && this.isUnplannedTask(taskDraft)) {
            unplanned.setUnplannedEvidenceJson(this.buildFallbackUnplannedEvidence(taskDraft, context));
        }
        return unplanned;
    }

    /**
     * 构建未排证据兜底摘要，避免规则快照缺失时未排表只有原因码而无法审计。
     *
     * @param taskDraft 未排任务草稿
     * @param context 胎面排程上下文
     * @return JSON 格式的未排证据摘要
     */
    private String buildFallbackUnplannedEvidence(TmTaskDraft taskDraft, TmScheduleContext context) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("reasonCode", taskDraft == null ? null : taskDraft.getUnplannedReasonCode());
        evidence.put("reasonDesc", taskDraft == null ? null : taskDraft.getUnplannedReasonDesc());
        evidence.put("planQty", taskDraft == null ? null : taskDraft.getPlanQty());
        evidence.put("toolOverflowQty", taskDraft == null ? null : taskDraft.getToolOverflowQty());
        evidence.put("availableToolQty", taskDraft == null ? null : taskDraft.getAvailableToolQty());
        evidence.put("planQtyBeforeToolLimit", taskDraft == null ? null : taskDraft.getPlanQtyBeforeToolLimit());
        evidence.put("batchNo", context == null ? null : context.getBatchNo());
        evidence.put("traceId", context == null ? null : context.getTraceId());
        return JSONUtil.toJsonStr(evidence);
    }

    /**
     * 归一化结果表班次字段。
     *
     * <p>结果表只表示已经安排到机台产能的任务。机台为空或班次计划量为空/小于等于 0 时，
     * 对应班次顺序和起止时间必须清空，避免看板展示无产能承接的顺序。</p>
     *
     * @param result 排程结果
     */
    private void normalizeResultShiftFields(TmScheduleResult result) {
        if (result == null) {
            return;
        }
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
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
    private void resequenceVisibleShiftSequences(List<TmScheduleResult> resultList) {
        if (CollUtil.isEmpty(resultList)) {
            return;
        }
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            Map<String, List<TmScheduleResult>> machineShiftResultMap = new LinkedHashMap<>();
            for (TmScheduleResult result : resultList) {
                if (!isVisibleShiftResult(result, shiftOrder)) {
                    continue;
                }
                String groupKey = StrUtil.blankToDefault(result.getMachineCode(), "") + "|" + shiftOrder;
                machineShiftResultMap.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(result);
            }
            for (List<TmScheduleResult> visibleResultList : machineShiftResultMap.values()) {
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
    private boolean isVisibleShiftResult(TmScheduleResult result, int shiftOrder) {
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
    private void resequenceSingleVisibleShift(List<TmScheduleResult> visibleResultList, int shiftOrder) {
        visibleResultList.sort(Comparator
                .comparing((TmScheduleResult result) -> resolveSortSequence(result, shiftOrder))
                .thenComparing(result -> StrUtil.blankToDefault(result.getOrderNo(), ""))
                .thenComparing(result -> StrUtil.blankToDefault(result.getTreadCode(), "")));
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
    private BigDecimal getShiftPlanQty(TmScheduleResult result, int shiftOrder) {
        Object planQty = result.getFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
        return planQty instanceof BigDecimal ? (BigDecimal) planQty : null;
    }

    /**
     * 设置班次展示顺序。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @param sequence   展示顺序
     */
    private void setShiftSequence(TmScheduleResult result, int shiftOrder, Integer sequence) {
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), sequence);
    }

    /**
     * 读取重排前的班次顺序，空顺序排在最后。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 排序用顺序值
     */
    private Integer resolveSortSequence(TmScheduleResult result, int shiftOrder) {
        Object sequence = result.getFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
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
    private void clearShiftFields(TmScheduleResult result, int shiftOrder) {
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), null);
    }
    /**
     * 将任务链转换后的结果实体与原任务业务键建立关联。
     *
     * @param nodeList             任务链节点列表
     * @param chainResultList      任务链转换后的结果实体列表
     * @param resultBusinessKeyMap 结果实体与任务业务键映射
     */
    private void registerChainResultBusinessKey(List<ScheduleTaskNode<TmTaskDraft>> nodeList,
                                                List<TmScheduleResult> chainResultList,
                                                Map<TmScheduleResult, List<String>> resultBusinessKeyMap) {
        if (CollUtil.isEmpty(chainResultList)) {
            return;
        }
        if (nodeList == null || nodeList.size() != chainResultList.size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.resultRelationFailed"));
        }
        for (int index = 0; index < chainResultList.size(); index++) {
            ScheduleTaskNode<TmTaskDraft> node = nodeList.get(index);
            TmTaskDraft task = node == null ? null : node.getTask();
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
    private void addResultBusinessKey(Map<TmScheduleResult, List<String>> resultBusinessKeyMap,
                                      TmScheduleResult result, String businessKey) {
        if (result == null || StrUtil.isBlank(businessKey)) {
            return;
        }
        resultBusinessKeyMap.computeIfAbsent(result, key -> new ArrayList<>()).add(businessKey);
    }

    /**
     * 归并胎面排程结果行。
     *
     * <p>自动排程结果按工厂、批次、日期、机台和胎面规格落一行；同一行的 1 到 6 班计划量横向写入。
     * 未排任务机台为空时也按同一胎面归并，避免同一胎面被拆成多条结果。</p>
     *
     * @param resultList              引擎转换出的原始结果
     * @param resultBusinessKeyMap    原始结果与任务业务键映射
     * @param mergedBusinessKeyMap    归并结果与任务业务键映射
     * @return 归并后的结果列表
     */
    private List<TmScheduleResult> mergeScheduleResults(List<TmScheduleResult> resultList,
                                                        Map<TmScheduleResult, List<String>> resultBusinessKeyMap,
                                                        Map<TmScheduleResult, List<String>> mergedBusinessKeyMap) {
        Map<String, TmScheduleResult> mergedResultMap = new LinkedHashMap<>();
        for (TmScheduleResult result : resultList) {
            String mergeKey = buildResultMergeKey(result);
            TmScheduleResult merged = mergedResultMap.get(mergeKey);
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
    private String buildResultMergeKey(TmScheduleResult result) {
        Date scheduleDate = result == null ? null : result.getScheduleDate();
        return StrUtil.blankToDefault(result == null ? null : result.getFactoryCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getBatchNo(), "")
                + "|" + (scheduleDate == null ? "" : String.valueOf(scheduleDate.getTime()))
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getMachineCode(), "")
                + "|" + StrUtil.blankToDefault(result == null ? null : result.getTreadCode(), "");
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
     * 生成胎面自动排程结果工单号。
     *
     * @param resultList 归并后的结果列表
     * @param batchNo    胎面自动排程批次号
     */
    private void assignTmOrderNo(List<TmScheduleResult> resultList, String batchNo) {
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
    private void mergeResultShiftFields(TmScheduleResult target, TmScheduleResult source) {
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            String planQtyField = String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            String sequenceField = String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            String startTimeField = String.format(TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder);
            String endTimeField = String.format(TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder);
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
    private void registerInsertedResultId(TmScheduleResult result, Map<TmScheduleResult, List<String>> resultBusinessKeyMap,
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
    private Long resolveOptionalResultId(TmTaskDraft taskDraft, Map<String, Long> resultIdMap) {
        if (isUnplannedTask(taskDraft) || taskDraft == null || !isPositiveQty(taskDraft.getPlanQty())) {
            return null;
        }
        String businessKey = taskDraft.getBusinessKey();
        Long resultId = StrUtil.isBlank(businessKey) ? null : resultIdMap.get(businessKey);
        if (resultId == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.explainResultMissing"));
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
    private String buildResultErrorMsg(TmScheduleResult result, RuntimeException ex) {
        return "结果表写入失败，orderNo=" + (result == null ? null : result.getOrderNo())
                + "，treadCode=" + (result == null ? null : result.getTreadCode())
                + "，machineCode=" + (result == null ? null : result.getMachineCode())
                + "，原因=" + ex.getMessage();
    }

    /**
     * 构建未排表落库失败信息。
     *
     * @param taskDraft 未排任务
     * @param ex        异常
     * @return 错误信息
     */
    private String buildUnplannedErrorMsg(TmScheduleUnplanned unplanned, RuntimeException ex) {
        return "未排表写入失败，batchNo=" + (unplanned == null ? null : unplanned.getBatchNo())
                + "，treadCode=" + (unplanned == null ? null : unplanned.getTreadCode())
                + "，原因=" + ex.getMessage();
    }

    /**
     * 构建解释表落库失败信息。
     *
     * @param taskDraft 待排任务
     * @param ex        异常
     * @return 错误信息
     */
    private String buildExplainErrorMsg(TmTaskDraft taskDraft, RuntimeException ex) {
        return "解释表写入失败，orderNo=" + (taskDraft == null ? null : taskDraft.getOrderNo())
                + "，treadCode=" + (taskDraft == null ? null : taskDraft.getTreadCode())
                + "，原因=" + ex.getMessage();
    }
}
