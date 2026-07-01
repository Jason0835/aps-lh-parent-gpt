package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResultExplain;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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

    /**
     * 创建胎面自动排程业务快照和落库步骤服务。
     *
     * @param snapshotBuildService       解释快照构建服务
     * @param persistService             落库实体转换服务
     * @param scheduleResultMapper       胎面排程结果 Mapper
     * @param scheduleResultExplainMapper 胎面排程解释 Mapper
     * @param scheduleUnplannedMapper     胎面未排任务 Mapper
     */
    public TmBizSnapshotAndPersistService(TmSnapshotBuildService snapshotBuildService,
                                          TmPersistService persistService,
                                          TmScheduleResultMapper scheduleResultMapper,
                                          TmScheduleResultExplainMapper scheduleResultExplainMapper,
                                          TmScheduleUnplannedMapper scheduleUnplannedMapper) {
        this.snapshotBuildService = snapshotBuildService;
        this.persistService = persistService;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
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
        buildSnapshot(context);
        context.setPersistResult(persistScheduleContext(context));
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
    private TmPersistResult persistScheduleContext(TmScheduleContext context) {
        TmPersistResult persistResult = new TmPersistResult();
        if (CollUtil.isEmpty(context.getTaskDraftList())) {
            return persistResult;
        }
        List<TmScheduleResult> resultList = new ArrayList<>();
        List<TmTaskDraft> unplannedTaskList = new ArrayList<>();
        Map<TmScheduleResult, List<String>> resultBusinessKeyMap = new IdentityHashMap<>();
        Map<String, Long> resultIdMap = new HashMap<>();
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            if (isUnplannedTask(taskDraft)) {
                unplannedTaskList.add(taskDraft);
            }
        }
        for (ScheduleTaskLinkedList<TmTaskDraft> chain : context.getTaskChainGroup().values()) {
            List<ScheduleTaskNode<TmTaskDraft>> nodeList = chain.toList();
            List<TmScheduleResult> chainResultList = persistService.convertChainToResult(chain, context);
            registerChainResultBusinessKey(nodeList, chainResultList, resultBusinessKeyMap);
            resultList.addAll(chainResultList);
        }
        Map<TmScheduleResult, List<String>> mergedResultBusinessKeyMap = new IdentityHashMap<>();
        List<TmScheduleResult> mergedResultList = mergeScheduleResults(resultList, resultBusinessKeyMap, mergedResultBusinessKeyMap);
        assignTmOrderNo(mergedResultList, context.getBatchNo());
        for (TmScheduleResult result : mergedResultList) {
            normalizeResultShiftFields(result);
            try {
                scheduleResultMapper.insert(result);
                registerInsertedResultId(result, mergedResultBusinessKeyMap, resultIdMap);
                persistResult.setResultCount(persistResult.getResultCount() + 1);
            } catch (RuntimeException ex) {
                String errorMsg = buildResultErrorMsg(result, ex);
                log.error("[TM_SCHEDULE_PERSIST_RESULT_FAIL] {}", errorMsg, ex);
                throw new ServiceException(errorMsg);
            }
        }
        Map<String, TmScheduleUnplanned> unplannedMap = buildMergedUnplannedMap(unplannedTaskList, context);
        for (TmScheduleUnplanned unplanned : unplannedMap.values()) {
            try {
                scheduleUnplannedMapper.insert(unplanned);
                persistResult.setUnplannedCount(persistResult.getUnplannedCount() + 1);
            } catch (RuntimeException ex) {
                String errorMsg = buildUnplannedErrorMsg(unplanned, ex);
                log.error("[TM_SCHEDULE_PERSIST_UNPLANNED_FAIL] {}", errorMsg, ex);
                throw new ServiceException(errorMsg);
            }
        }
        for (TmTaskDraft taskDraft : context.getTaskDraftList()) {
            TmSnapshotBuildResult snapshot = context.getSnapshotMap().get(taskDraft.getBusinessKey());
            TmScheduleResultExplain explain = persistService.convertExplain(taskDraft, snapshot, context);
            try {
                if (!isUnplannedTask(taskDraft)) {
                    explain.setResultId(resolveResultId(taskDraft, resultIdMap));
                }
                scheduleResultExplainMapper.insert(explain);
                persistResult.setExplainCount(persistResult.getExplainCount() + 1);
            } catch (RuntimeException ex) {
                String errorMsg = buildExplainErrorMsg(taskDraft, ex);
                log.error("[TM_SCHEDULE_PERSIST_EXPLAIN_FAIL] {}", errorMsg, ex);
                throw new ServiceException(errorMsg);
            }
        }
        return persistResult;
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
        if (StrUtil.isBlank(result.getMachineCode())) {
            clearAllShiftFields(result);
            result.setClass1PlanQty(BigDecimal.ZERO);
            result.setClass2PlanQty(BigDecimal.ZERO);
            result.setClass3PlanQty(BigDecimal.ZERO);
            result.setClass4PlanQty(BigDecimal.ZERO);
            result.setClass5PlanQty(BigDecimal.ZERO);
            result.setClass6PlanQty(BigDecimal.ZERO);
            return;
        }
        if (!isPositiveQty(result.getClass1PlanQty())) {
            clearClass1ShiftFields(result);
        }
        if (!isPositiveQty(result.getClass2PlanQty())) {
            clearClass2ShiftFields(result);
        }
        if (!isPositiveQty(result.getClass3PlanQty())) {
            clearClass3ShiftFields(result);
        }
        if (!isPositiveQty(result.getClass4PlanQty())) {
            clearClass4ShiftFields(result);
        }
        if (!isPositiveQty(result.getClass5PlanQty())) {
            clearClass5ShiftFields(result);
        }
        if (!isPositiveQty(result.getClass6PlanQty())) {
            clearClass6ShiftFields(result);
        }
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
     * 清空全部班次顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearAllShiftFields(TmScheduleResult result) {
        clearClass1ShiftFields(result);
        clearClass2ShiftFields(result);
        clearClass3ShiftFields(result);
        clearClass4ShiftFields(result);
        clearClass5ShiftFields(result);
        clearClass6ShiftFields(result);
    }

    /**
     * 清空 1 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass1ShiftFields(TmScheduleResult result) {
        result.setClass1Sequence(null);
        result.setClass1StartTime(null);
        result.setClass1EndTime(null);
    }

    /**
     * 清空 2 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass2ShiftFields(TmScheduleResult result) {
        result.setClass2Sequence(null);
        result.setClass2StartTime(null);
        result.setClass2EndTime(null);
    }

    /**
     * 清空 3 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass3ShiftFields(TmScheduleResult result) {
        result.setClass3Sequence(null);
        result.setClass3StartTime(null);
        result.setClass3EndTime(null);
    }

    /**
     * 清空 4 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass4ShiftFields(TmScheduleResult result) {
        result.setClass4Sequence(null);
        result.setClass4StartTime(null);
        result.setClass4EndTime(null);
    }

    /**
     * 清空 5 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass5ShiftFields(TmScheduleResult result) {
        result.setClass5Sequence(null);
        result.setClass5StartTime(null);
        result.setClass5EndTime(null);
    }

    /**
     * 清空 6 班顺序和时间字段。
     *
     * @param result 排程结果
     */
    private void clearClass6ShiftFields(TmScheduleResult result) {
        result.setClass6Sequence(null);
        result.setClass6StartTime(null);
        result.setClass6EndTime(null);
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
            throw new ServiceException("任务链结果数量与任务节点数量不一致，无法关联解释表结果ID");
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
        target.setClass1PlanQty(addQty(target.getClass1PlanQty(), source.getClass1PlanQty()));
        target.setClass1Sequence(minSequence(target.getClass1Sequence(), source.getClass1Sequence()));
        target.setClass1StartTime(minDate(target.getClass1StartTime(), source.getClass1StartTime()));
        target.setClass1EndTime(maxDate(target.getClass1EndTime(), source.getClass1EndTime()));
        target.setClass2PlanQty(addQty(target.getClass2PlanQty(), source.getClass2PlanQty()));
        target.setClass2Sequence(minSequence(target.getClass2Sequence(), source.getClass2Sequence()));
        target.setClass2StartTime(minDate(target.getClass2StartTime(), source.getClass2StartTime()));
        target.setClass2EndTime(maxDate(target.getClass2EndTime(), source.getClass2EndTime()));
        target.setClass3PlanQty(addQty(target.getClass3PlanQty(), source.getClass3PlanQty()));
        target.setClass3Sequence(minSequence(target.getClass3Sequence(), source.getClass3Sequence()));
        target.setClass3StartTime(minDate(target.getClass3StartTime(), source.getClass3StartTime()));
        target.setClass3EndTime(maxDate(target.getClass3EndTime(), source.getClass3EndTime()));
        target.setClass4PlanQty(addQty(target.getClass4PlanQty(), source.getClass4PlanQty()));
        target.setClass4Sequence(minSequence(target.getClass4Sequence(), source.getClass4Sequence()));
        target.setClass4StartTime(minDate(target.getClass4StartTime(), source.getClass4StartTime()));
        target.setClass4EndTime(maxDate(target.getClass4EndTime(), source.getClass4EndTime()));
        target.setClass5PlanQty(addQty(target.getClass5PlanQty(), source.getClass5PlanQty()));
        target.setClass5Sequence(minSequence(target.getClass5Sequence(), source.getClass5Sequence()));
        target.setClass5StartTime(minDate(target.getClass5StartTime(), source.getClass5StartTime()));
        target.setClass5EndTime(maxDate(target.getClass5EndTime(), source.getClass5EndTime()));
        target.setClass6PlanQty(addQty(target.getClass6PlanQty(), source.getClass6PlanQty()));
        target.setClass6Sequence(minSequence(target.getClass6Sequence(), source.getClass6Sequence()));
        target.setClass6StartTime(minDate(target.getClass6StartTime(), source.getClass6StartTime()));
        target.setClass6EndTime(maxDate(target.getClass6EndTime(), source.getClass6EndTime()));
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
     * 根据任务业务键读取解释表需要关联的结果表主键。
     *
     * @param taskDraft   待排任务
     * @param resultIdMap 任务业务键与结果表主键映射
     * @return 结果表主键
     */
    private Long resolveResultId(TmTaskDraft taskDraft, Map<String, Long> resultIdMap) {
        String businessKey = taskDraft == null ? null : taskDraft.getBusinessKey();
        Long resultId = StrUtil.isBlank(businessKey) ? null : resultIdMap.get(businessKey);
        if (resultId == null) {
            throw new ServiceException("解释表写入失败，未找到结果表ID，orderNo="
                    + (taskDraft == null ? null : taskDraft.getOrderNo())
                    + "，treadCode=" + (taskDraft == null ? null : taskDraft.getTreadCode()));
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
