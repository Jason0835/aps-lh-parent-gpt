package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.enums.TmMachineAssignStatusEnum;
import com.zlt.aps.tm.engine.domain.TmPersistResult;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmSnapshotBuildResult;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.ITmSnapshotAndPersistService;
import com.zlt.aps.tm.engine.service.impl.TmPersistService;
import com.zlt.aps.tm.engine.service.impl.TmSnapshotBuildService;
import com.zlt.aps.tm.mapper.TmScheduleResultExplainMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.extern.slf4j.Slf4j;
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
     * @param baseDao                     通用批量写入服务
     * @param transactionManager          事务管理器
     */
    public TmBizSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService,
                                          TmPersistService persistService,
                                          TmScheduleResultMapper scheduleResultMapper,
                                          TmScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TmScheduleUnplannedMapper scheduleUnplannedMapper,
                                          BaseDao baseDao,
                                          PlatformTransactionManager transactionManager) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.baseDao = baseDao;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return;
        }
        for (TmTaskDraft task : context.getTaskDraftList()) {
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
            persistResult.setUnplannedCount(unplannedList.size());
        }
        // 解释表落库统一批量写入，任何单行失败都会回滚整个最终事务。
        List<TmScheduleResultExplain> explainList = new ArrayList<>();
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            TmSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TmScheduleResultExplain explain = persistService.convertExplain(taskDraft, snapshot, context);
            Long resultId = resolveOptionalResultId(taskDraft, resultIdMap);
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
        return persistResult;
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
        return taskDraft != null && (taskDraft.isUnassigned() || StrUtil.isNotBlank(taskDraft.getUnplannedReasonCode()));
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
