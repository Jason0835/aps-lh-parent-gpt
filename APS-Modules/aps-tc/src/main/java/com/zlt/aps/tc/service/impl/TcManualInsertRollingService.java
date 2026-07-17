package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.domain.vo.TcManualRollingTask;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工插单、调量和普通转机后的横表局部滚动服务。
 *
 * <p>滚动范围限定在同工厂、同排程日、同批次、同机台，从操作班次和顺位开始至第六班。
 * 班产不足时顺延下一班，第六班后的剩余量写入未排表。</p>
 */
@Service
public class TcManualInsertRollingService {

    /** 人工插单数据来源。 */
    private static final String INSERT_DATA_SOURCE = "INSERT";

    /** 未发布状态。 */
    private static final String RELEASE_STATUS_UNPUBLISHED = "0";

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcScheduleUnplannedMapper scheduleUnplannedMapper;

    private final TcScheduleResultExplainMapper scheduleResultExplainMapper;

    private final TcManualMachineRuleValidator machineRuleValidator;

    /**
     * 构造胎侧人工横表滚动服务。
     *
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     * @param scheduleResultExplainMapper 结果解释 Mapper
     * @param machineRuleValidator 机台班次和有效产能校验器
     */
    public TcManualInsertRollingService(TcScheduleResultMapper scheduleResultMapper,
                                        TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                        TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                        TcManualMachineRuleValidator machineRuleValidator) {
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.machineRuleValidator = machineRuleValidator;
    }

    /**
     * 写入人工插单并滚动同机台后续任务。
     *
     * @param insertResult 人工插单结果快照
     * @return 新增结果行数
     * @throws ServiceException 班次顺序、班产或排程状态不满足规则时抛出
     */
    int insertAndRoll(TcScheduleResult insertResult) {
        List<Integer> insertShiftOrderList = this.resolveInsertShiftOrderList(insertResult);
        int shiftOrder = insertShiftOrderList.get(0);
        int insertSequence = this.requireSequence(insertResult, shiftOrder);
        List<TcScheduleResult> existResultList = this.loadSameMachineResults(insertResult);
        this.validateEditableResults(existResultList);

        insertResult.setDataSource(INSERT_DATA_SOURCE);
        insertResult.setReleaseStatus(RELEASE_STATUS_UNPUBLISHED);
        insertResult.setTaskVersion(0L);
        TcManualRollingTask[] insertTaskArray = insertShiftOrderList.stream()
                .map(currentShiftOrder -> this.buildInsertTask(insertResult, currentShiftOrder))
                .toArray(TcManualRollingTask[]::new);
        Map<String, TcScheduleResult> writeMap = this.rollMachineWindow(insertResult, existResultList, shiftOrder,
                insertSequence, null, insertTaskArray);
        return writeMap.values().stream().anyMatch(result -> result == insertResult) ? 1 : 0;
    }

    /**
     * 修改选中班次计划量并滚动同机台后续任务。
     *
     * @param changeResult 调量请求转换后的结果
     * @return 受影响结果行数
     * @throws ServiceException 结果不存在、计划量小于完成量或状态不可编辑时抛出
     */
    int changeQtyAndRoll(TcScheduleResult changeResult) {
        TcScheduleResult current = this.requireResult(changeResult.getId());
        this.validateEditableResults(Collections.singletonList(current));
        int shiftOrder = this.resolveOperationShift(changeResult);
        BigDecimal newPlanQty = this.getPlanQty(changeResult, shiftOrder);
        BigDecimal finishQty = this.getFinishQty(current, shiftOrder);
        if (newPlanQty.compareTo(finishQty) < 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeQty.lessThanFinish"));
        }
        int sequence = this.defaultSequence(this.getSequence(current, shiftOrder));
        List<TcScheduleResult> existResultList = this.loadSameMachineResults(current);
        TcManualRollingTask changeTask = this.buildTask(current, shiftOrder, sequence, newPlanQty);
        Map<String, TcScheduleResult> writeMap = this.rollMachineWindow(current, existResultList, shiftOrder,
                sequence, current.getId(), changeTask);
        return writeMap.size();
    }

    /**
     * 将选中班次转移到目标机台，并分别滚动源、目标机台任务链。
     *
     * @param transferResult 转机请求转换后的结果，机台编码为目标机台
     * @return 受影响结果行数
     * @throws ServiceException 结果不存在、源目标机台相同或状态不可编辑时抛出
     */
    int changeMachineAndRoll(TcScheduleResult transferResult) {
        TcScheduleResult source = this.requireResult(transferResult.getId());
        this.validateEditableResults(Collections.singletonList(source));
        if (Objects.equals(StringUtils.trim(source.getMachineCode()), StringUtils.trim(transferResult.getMachineCode()))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.sameMachine"));
        }
        int shiftOrder = this.resolveOperationShift(transferResult);
        int sourceSequence = this.defaultSequence(this.getSequence(source, shiftOrder));
        BigDecimal transferPlanQty = this.getPlanQty(source, shiftOrder);
        BigDecimal transferFinishQty = this.getFinishQty(source, shiftOrder);

        List<TcScheduleResult> sourceResultList = this.loadSameMachineResults(source);
        Map<String, TcScheduleResult> sourceWriteMap = this.rollMachineWindow(source, sourceResultList, shiftOrder,
                sourceSequence, source.getId());

        TcScheduleResult targetTemplate = this.copyBaseResult(source);
        targetTemplate.setMachineCode(StringUtils.trim(transferResult.getMachineCode()));
        int targetSequence = this.getSequence(transferResult, shiftOrder) == null
                ? sourceSequence : this.getSequence(transferResult, shiftOrder);
        this.setPlanQty(targetTemplate, shiftOrder, transferPlanQty);
        this.setFinishQty(targetTemplate, shiftOrder, transferFinishQty);
        this.setSequence(targetTemplate, shiftOrder, targetSequence);
        List<TcScheduleResult> targetResultList = this.loadSameMachineResults(targetTemplate);

        TcScheduleResult targetSameGrain = targetResultList.stream()
                .filter(item -> this.isSameMergeGrain(item, source))
                .findFirst().orElse(null);
        TcManualRollingTask targetTask;
        Long replaceTargetId = null;
        if (targetSameGrain == null) {
            targetTemplate.setTaskVersion(0L);
            targetTask = this.buildTask(targetTemplate, shiftOrder, targetSequence, transferPlanQty);
            targetTask.setFinishQty(transferFinishQty);
        } else {
            replaceTargetId = targetSameGrain.getId();
            BigDecimal mergedQty = this.getPlanQty(targetSameGrain, shiftOrder).add(transferPlanQty);
            int mergedSequence = this.getSequence(targetSameGrain, shiftOrder) == null
                    ? targetSequence : this.getSequence(targetSameGrain, shiftOrder);
            targetTask = this.buildTask(targetSameGrain, shiftOrder, mergedSequence, mergedQty);
            targetTask.setFinishQty(this.getFinishQty(targetSameGrain, shiftOrder).add(transferFinishQty));
        }
        Map<String, TcScheduleResult> targetWriteMap = this.rollMachineWindow(targetTemplate, targetResultList,
                shiftOrder, targetTask.getOriginalSequence(), replaceTargetId, targetTask);
        Map<String, TcScheduleResult> affectedResultMap = new LinkedHashMap<>(sourceWriteMap);
        affectedResultMap.putAll(targetWriteMap);
        return affectedResultMap.size();
    }

    /**
     * 删除指定结果后，从给定班次开始重排同机台任务链。
     *
     * @param removedResult 待删除结果
     * @param startShiftOrder 起始班次
     * @return 受影响结果行数
     */
    int rollAfterRemove(TcScheduleResult removedResult, int startShiftOrder) {
        List<TcScheduleResult> existResultList = this.loadSameMachineResults(removedResult).stream()
                .filter(item -> !Objects.equals(item.getId(), removedResult.getId()))
                .collect(Collectors.toList());
        return this.rollMachineWindow(removedResult, existResultList, startShiftOrder, 1, null).size();
    }

    /**
     * 从操作位置开始重排单机台窗口。
     *
     * @param baseResult 操作范围参考
     * @param existResultList 当前机台排程结果
     * @param startShiftOrder 起始班次
     * @param startSequence 起始顺序
     * @param replaceResultId 起始任务中需要替换的结果 ID
     * @param extraTaskList 起始位置新增或替换的任务
     * @return 已写入结果集合，Boolean 为新增标记
     */
    private Map<String, TcScheduleResult> rollMachineWindow(TcScheduleResult baseResult,
                                                             List<TcScheduleResult> existResultList,
                                                             int startShiftOrder, int startSequence,
                                                             Long replaceResultId,
                                                             TcManualRollingTask... extraTaskList) {
        this.validateEditableResults(existResultList);
        List<TcManualRollingTask> prefixTaskList = this.buildPrefixTaskList(existResultList, startShiftOrder,
                startSequence, replaceResultId);
        LinkedList<TcManualRollingTask> rollingQueue = this.buildRollingQueue(existResultList, startShiftOrder,
                startSequence, replaceResultId, extraTaskList);
        Map<String, TcScheduleResult> writeMap = new LinkedHashMap<>();

        this.clearNewTaskFields(extraTaskList, startShiftOrder);
        this.clearAffectedFields(existResultList, startShiftOrder, writeMap);
        for (int shiftOrder = startShiftOrder; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal remainCapacity = this.machineRuleValidator.resolveRollingCapacity(baseResult,
                    baseResult.getMachineCode(), shiftOrder);
            BigDecimal shiftCapacity = remainCapacity;
            List<Date> shiftWindow = this.machineRuleValidator.resolveRollingShiftWindow(baseResult, shiftOrder);
            Date shiftStartTime = shiftWindow.size() == 2 ? shiftWindow.get(0) : null;
            Date shiftCursor = shiftStartTime;
            Date shiftEndTime = shiftWindow.size() == 2 ? shiftWindow.get(1) : null;
            int sequence = 1;
            if (shiftOrder == startShiftOrder) {
                for (TcManualRollingTask prefixTask : prefixTaskList) {
                    shiftCursor = this.applyTask(prefixTask, shiftOrder, sequence, prefixTask.getPlanQty(),
                            shiftCapacity, shiftStartTime, shiftCursor, shiftEndTime, writeMap);
                    remainCapacity = remainCapacity.subtract(prefixTask.getPlanQty());
                    sequence++;
                }
            }
            while (!rollingQueue.isEmpty() && remainCapacity.compareTo(BigDecimal.ZERO) > 0) {
                TcManualRollingTask task = rollingQueue.getFirst();
                if (task.getMinimumShiftOrder() != null && shiftOrder < task.getMinimumShiftOrder()) {
                    // 多班插单的后续班任务形成时间边界，禁止被前一班剩余产能提前吸收。
                    break;
                }
                BigDecimal assignedQty = task.getPlanQty().min(remainCapacity);
                shiftCursor = this.applyTask(task, shiftOrder, sequence, assignedQty,
                        shiftCapacity, shiftStartTime, shiftCursor, shiftEndTime, writeMap);
                task.setPlanQty(task.getPlanQty().subtract(assignedQty));
                remainCapacity = remainCapacity.subtract(assignedQty);
                sequence++;
                if (task.getPlanQty().compareTo(BigDecimal.ZERO) <= 0) {
                    rollingQueue.removeFirst();
                }
            }
        }
        this.persistResults(writeMap);
        this.persistUnplanned(baseResult, rollingQueue, writeMap.values());
        return writeMap;
    }

    /**
     * 构造操作位置之前的前缀任务。
     *
     * @param resultList 当前排程结果
     * @param shiftOrder 操作班次
     * @param startSequence 起始顺序
     * @param replaceResultId 替换结果 ID
     * @return 前缀任务
     */
    private List<TcManualRollingTask> buildPrefixTaskList(List<TcScheduleResult> resultList, int shiftOrder,
                                                           int startSequence, Long replaceResultId) {
        return this.buildShiftTasks(resultList, shiftOrder).stream()
                .filter(task -> task.getOriginalSequence() < startSequence)
                .filter(task -> !Objects.equals(task.getResult().getId(), replaceResultId))
                .collect(Collectors.toList());
    }

    /**
     * 构造操作位置后的滚动任务队列。
     *
     * @param resultList 当前排程结果
     * @param startShiftOrder 起始班次
     * @param startSequence 起始顺序
     * @param replaceResultId 需要被替换的结果 ID
     * @param extraTaskList 插入到窗口起点的任务
     * @return 滚动任务队列
     */
    private LinkedList<TcManualRollingTask> buildRollingQueue(List<TcScheduleResult> resultList,
                                                               int startShiftOrder, int startSequence,
                                                               Long replaceResultId,
                                                               TcManualRollingTask... extraTaskList) {
        List<TcManualRollingTask> rollingTaskList = new ArrayList<>();
        List<TcManualRollingTask> extraTaskSourceList = new ArrayList<>();
        if (extraTaskList != null) {
            for (TcManualRollingTask extraTask : extraTaskList) {
                if (extraTask != null && this.isPositive(extraTask.getPlanQty())) {
                    rollingTaskList.add(extraTask);
                    extraTaskSourceList.add(extraTask);
                }
            }
        }
        for (int shiftOrder = startShiftOrder; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            List<TcManualRollingTask> shiftTaskList = this.buildShiftTasks(resultList, shiftOrder);
            for (TcManualRollingTask task : shiftTaskList) {
                if (Objects.equals(task.getResult().getId(), replaceResultId)
                        && shiftOrder == startShiftOrder) {
                    continue;
                }
                if (shiftOrder == startShiftOrder && task.getOriginalSequence() < startSequence) {
                    continue;
                }
                rollingTaskList.add(task);
            }
        }
        rollingTaskList.sort(Comparator.comparing(TcManualRollingTask::getOriginalShiftOrder)
                .thenComparing(TcManualRollingTask::getOriginalSequence)
                .thenComparing(task -> extraTaskSourceList.stream().anyMatch(extraTask -> extraTask == task) ? 0 : 1)
                .thenComparing(task -> task.getResult().getId(), Comparator.nullsFirst(Long::compareTo)));
        return new LinkedList<>(rollingTaskList);
    }

    /**
     * 构造单班次任务列表。
     *
     * @param resultList 排程结果
     * @param shiftOrder 班次顺序
     * @return 按班内顺序排列的任务
     */
    private List<TcManualRollingTask> buildShiftTasks(List<TcScheduleResult> resultList, int shiftOrder) {
        return resultList.stream().filter(item -> this.isPositive(this.getPlanQty(item, shiftOrder)))
                .map(item -> this.buildTask(item, shiftOrder,
                        this.defaultSequence(this.getSequence(item, shiftOrder)), this.getPlanQty(item, shiftOrder)))
                .sorted(Comparator.comparing(TcManualRollingTask::getOriginalSequence)
                        .thenComparing(task -> task.getResult().getId(), Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 清理受影响窗口字段，后续由滚动队列重新装填。
     *
     * @param resultList 当前排程结果
     * @param startShiftOrder 起始班次
     * @param writeMap 待写结果集合
     */
    private void clearAffectedFields(List<TcScheduleResult> resultList, int startShiftOrder,
                                     Map<String, TcScheduleResult> writeMap) {
        for (TcScheduleResult result : resultList) {
            boolean changed = false;
            for (int shiftOrder = startShiftOrder; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                if (this.getPlanQty(result, shiftOrder).compareTo(BigDecimal.ZERO) > 0
                        || this.getSequence(result, shiftOrder) != null) {
                    changed = true;
                }
                this.clearShift(result, shiftOrder);
            }
            if (changed) {
                writeMap.put(this.resultMapKey(result), result);
            }
        }
    }

    /**
     * 将一段计划量写入横表结果。
     *
     * @param task 滚动任务
     * @param shiftOrder 目标班次
     * @param sequence 目标顺序
     * @param assignedQty 本班分配量
     * @param writeMap 待写结果集合
     */
    private Date applyTask(TcManualRollingTask task, int shiftOrder, int sequence, BigDecimal assignedQty,
                           BigDecimal shiftCapacity, Date shiftStartTime, Date startTime, Date shiftEndTime,
                           Map<String, TcScheduleResult> writeMap) {
        TcScheduleResult result = task.getResult();
        BigDecimal existingQty = this.getPlanQty(result, shiftOrder);
        this.setPlanQty(result, shiftOrder, existingQty.add(assignedQty));
        if (this.getSequence(result, shiftOrder) == null) {
            this.setSequence(result, shiftOrder, sequence);
        }
        if (!task.isFinishQtyWritten()) {
            this.setFinishQty(result, shiftOrder, task.getFinishQty());
            task.setFinishQtyWritten(true);
        }
        Date taskEndTime = this.calculateTaskEndTime(shiftStartTime, startTime, shiftEndTime,
                assignedQty, shiftCapacity);
        if (startTime != null && taskEndTime != null) {
            String startTimeField = String.format(TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder);
            if (result.getFieldValueByFieldName(startTimeField) == null) {
                result.setFieldValueByFieldName(startTimeField, startTime);
            }
            result.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), taskEndTime);
            result.setFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder),
                    "{\"schemaVersion\":1,\"source\":\"MANUAL_ROLLING\"}");
        }
        writeMap.put(this.resultMapKey(result), result);
        return taskEndTime == null ? startTime : taskEndTime;
    }

    /**
     * 按本班有效产能占比计算任务预计结束时间。
     *
     * @param shiftStartTime 班次计划开始时间
     * @param startTime 任务预计开始时间
     * @param shiftEndTime 班次计划结束时间
     * @param assignedQty 本任务分配量
     * @param shiftCapacity 本班有效产能
     * @return 任务预计结束时间，资料不足时返回 null
     */
    private Date calculateTaskEndTime(Date shiftStartTime, Date startTime, Date shiftEndTime,
                                      BigDecimal assignedQty, BigDecimal shiftCapacity) {
        if (shiftStartTime == null || startTime == null || shiftEndTime == null
                || !shiftEndTime.after(shiftStartTime)
                || !this.isPositive(assignedQty) || !this.isPositive(shiftCapacity)) {
            return null;
        }
        long shiftDurationMillis = shiftEndTime.getTime() - shiftStartTime.getTime();
        long taskDurationMillis = BigDecimal.valueOf(shiftDurationMillis).multiply(assignedQty)
                .divide(shiftCapacity, 0, RoundingMode.HALF_UP).longValue();
        long endMillis = Math.min(shiftEndTime.getTime(), startTime.getTime() + taskDurationMillis);
        return new Date(endMillis);
    }

    /**
     * 持久化滚动后的横表结果，并对全部受影响旧行递增任务版本。
     *
     * @param writeMap 待写结果集合
     * @throws ServiceException 任一写入失败时抛出
     */
    private void persistResults(Map<String, TcScheduleResult> writeMap) {
        for (TcScheduleResult result : writeMap.values()) {
            if (!this.hasPositivePlanQty(result)) {
                if (result.getId() != null && this.scheduleResultMapper.deleteById(result.getId()) != 1) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                }
                continue;
            }
            if (result.getId() == null) {
                result.setTaskVersion(0L);
                if (this.scheduleResultMapper.insert(result) != 1) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
                }
            } else {
                result.setTaskVersion(result.getTaskVersion() == null ? 1L : result.getTaskVersion() + 1L);
                this.rollbackPublishedStatus(result);
                if (this.scheduleResultMapper.updateById(result) != 1) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                }
            }
        }
    }

    /**
     * 判断横向结果是否至少保留一个正计划量班次。
     *
     * @param result 排程结果
     * @return true 表示结果仍需在看板展示
     */
    private boolean hasPositivePlanQty(TcScheduleResult result) {
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isPositive(this.getPlanQty(result, shiftOrder))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空新增任务模板的受影响班次，避免请求计划量在滚动分配时被重复累加。
     *
     * @param extraTaskList 新增或替换任务
     * @param startShiftOrder 起始班次
     */
    private void clearNewTaskFields(TcManualRollingTask[] extraTaskList, int startShiftOrder) {
        if (extraTaskList == null) {
            return;
        }
        for (TcManualRollingTask task : extraTaskList) {
            if (task == null || task.getResult() == null || task.getResult().getId() != null) {
                continue;
            }
            for (int shiftOrder = startShiftOrder; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                this.clearShift(task.getResult(), shiftOrder);
            }
        }
    }

    /**
     * 构造不受实体可变字段影响的写集合键。
     *
     * @param result 排程结果
     * @return 稳定写集合键
     */
    private String resultMapKey(TcScheduleResult result) {
        return result.getId() == null ? "NEW:" + System.identityHashCode(result) : "ID:" + result.getId();
    }

    /**
     * 将第六班后的剩余任务写入未排表和解释表。
     *
     * @param baseResult 操作范围参考
     * @param rollingQueue 未分配任务
     * @param affectedResultList 本次滚动受影响结果
     */
    private void persistUnplanned(TcScheduleResult baseResult, LinkedList<TcManualRollingTask> rollingQueue,
                                  Collection<TcScheduleResult> affectedResultList) {
        Map<String, TcManualRollingTask> desiredTaskMap = new LinkedHashMap<>();
        for (TcManualRollingTask task : rollingQueue) {
            if (!this.isPositive(task.getPlanQty())) {
                continue;
            }
            String taskBusinessKey = this.buildManualTaskBusinessKey(task.getResult(), task.getOriginalShiftOrder());
            TcManualRollingTask existingTask = desiredTaskMap.get(taskBusinessKey);
            if (existingTask == null) {
                desiredTaskMap.put(taskBusinessKey, task);
            } else {
                existingTask.setPlanQty(existingTask.getPlanQty().add(task.getPlanQty()));
            }
        }

        Set<String> affectedTaskKeySet = this.buildAffectedTaskKeySet(baseResult, affectedResultList);
        LambdaQueryWrapper<TcScheduleUnplanned> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleUnplanned::getFactoryCode, baseResult.getFactoryCode());
        wrapper.eq(TcScheduleUnplanned::getScheduleDate, baseResult.getScheduleDate());
        wrapper.eq(TcScheduleUnplanned::getBatchNo, baseResult.getBatchNo());
        wrapper.eq(TcScheduleUnplanned::getUnplannedReasonCode, "CAPACITY_NOT_ENOUGH");
        List<TcScheduleUnplanned> existingUnplannedList = this.scheduleUnplannedMapper.selectList(wrapper);
        Map<String, TcScheduleUnplanned> existingUnplannedMap = existingUnplannedList == null
                ? new LinkedHashMap<>() : existingUnplannedList.stream()
                .filter(item -> affectedTaskKeySet.contains(item.getTaskBusinessKey()))
                .collect(Collectors.toMap(TcScheduleUnplanned::getTaskBusinessKey, item -> item,
                        (left, right) -> left, LinkedHashMap::new));

        for (String taskBusinessKey : affectedTaskKeySet) {
            TcManualRollingTask desiredTask = desiredTaskMap.remove(taskBusinessKey);
            TcScheduleUnplanned existingUnplanned = existingUnplannedMap.get(taskBusinessKey);
            if (desiredTask == null && existingUnplanned != null) {
                existingUnplanned.setPlanQty(BigDecimal.ZERO);
                existingUnplanned.setUnplannedEvidenceJson(
                        "{\"schemaVersion\":1,\"status\":\"RESOLVED_BY_MANUAL_ROLLING\"}");
                this.updateUnplanned(existingUnplanned);
                this.upsertUnplannedExplain(baseResult, null, taskBusinessKey, true);
            } else if (desiredTask != null) {
                TcScheduleUnplanned unplanned = existingUnplanned == null
                        ? new TcScheduleUnplanned() : existingUnplanned;
                this.fillUnplanned(baseResult, desiredTask, taskBusinessKey, unplanned);
                if (existingUnplanned == null) {
                    if (this.scheduleUnplannedMapper.insert(unplanned) != 1) {
                        throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
                    }
                } else {
                    this.updateUnplanned(unplanned);
                }
                this.upsertUnplannedExplain(baseResult, desiredTask, taskBusinessKey, false);
            }
        }
        for (Map.Entry<String, TcManualRollingTask> entry : desiredTaskMap.entrySet()) {
            TcScheduleUnplanned unplanned = new TcScheduleUnplanned();
            this.fillUnplanned(baseResult, entry.getValue(), entry.getKey(), unplanned);
            if (this.scheduleUnplannedMapper.insert(unplanned) != 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
            }
            this.upsertUnplannedExplain(baseResult, entry.getValue(), entry.getKey(), false);
        }
    }

    /**
     * 回填人工滚动未排任务字段。
     *
     * @param baseResult 操作范围参考
     * @param task 未排任务
     * @param taskBusinessKey 任务业务键
     * @param unplanned 未排实体
     */
    private void fillUnplanned(TcScheduleResult baseResult, TcManualRollingTask task,
                               String taskBusinessKey, TcScheduleUnplanned unplanned) {
        unplanned.setFactoryCode(baseResult.getFactoryCode());
        unplanned.setBatchNo(baseResult.getBatchNo());
        unplanned.setTaskBusinessKey(taskBusinessKey);
        unplanned.setScheduleDate(baseResult.getScheduleDate());
        unplanned.setSidewallCode(task.getResult().getSidewallCode());
        unplanned.setGlueCode(task.getResult().getGlueCode());
        unplanned.setMouthPlateCode(task.getResult().getMouthPlateCode());
        unplanned.setShiftOrder(task.getOriginalShiftOrder());
        unplanned.setPlanQty(task.getPlanQty());
        unplanned.setUnplannedReasonCode("CAPACITY_NOT_ENOUGH");
        unplanned.setUnplannedReasonDesc(I18nUtil.getMessage("ui.tc.schedule.unplanned.capacityNotEnough"));
        unplanned.setUnplannedEvidenceJson(
                "{\"schemaVersion\":1,\"rule\":\"TC_SHIFT_MAX_CAPACITY\",\"source\":\"MANUAL_ROLLING\"}");
    }

    /**
     * 更新已有未排任务。
     *
     * @param unplanned 未排任务
     */
    private void updateUnplanned(TcScheduleUnplanned unplanned) {
        if (this.scheduleUnplannedMapper.updateById(unplanned) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
        }
    }

    /**
     * 新增或更新人工滚动未排解释。
     *
     * @param baseResult 操作范围参考
     * @param task 未排任务，已收敛时允许为空
     * @param taskBusinessKey 任务业务键
     * @param resolved 是否已收敛
     */
    private void upsertUnplannedExplain(TcScheduleResult baseResult, TcManualRollingTask task,
                                        String taskBusinessKey, boolean resolved) {
        LambdaQueryWrapper<TcScheduleResultExplain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResultExplain::getBatchNo, baseResult.getBatchNo());
        wrapper.eq(TcScheduleResultExplain::getTaskBusinessKey, taskBusinessKey);
        List<TcScheduleResultExplain> explainList = this.scheduleResultExplainMapper.selectList(wrapper);
        TcScheduleResultExplain explain = explainList == null || explainList.isEmpty()
                ? new TcScheduleResultExplain() : explainList.get(0);
        explain.setFactoryCode(baseResult.getFactoryCode());
        explain.setBatchNo(baseResult.getBatchNo());
        explain.setScheduleDate(baseResult.getScheduleDate());
        explain.setTaskBusinessKey(taskBusinessKey);
        if (task != null) {
            explain.setSidewallCode(task.getResult().getSidewallCode());
            explain.setShiftOrder(task.getOriginalShiftOrder());
        }
        explain.setFinalPlanQty(resolved || task == null ? BigDecimal.ZERO : task.getPlanQty());
        explain.setAssignStatus(resolved ? "RESOLVED" : "UNPLANNED");
        explain.setTaskStatus(resolved ? "RESOLVED" : "UNPLANNED");
        explain.setUnplannedReasonCode(resolved ? null : "CAPACITY_NOT_ENOUGH");
        explain.setUnplannedEvidenceJson(resolved
                ? "{\"schemaVersion\":1,\"status\":\"RESOLVED_BY_MANUAL_ROLLING\"}"
                : "{\"schemaVersion\":1,\"reasonCode\":\"CAPACITY_NOT_ENOUGH\",\"source\":\"MANUAL_ROLLING\"}");
        int affectedCount = explain.getId() == null ? this.scheduleResultExplainMapper.insert(explain)
                : this.scheduleResultExplainMapper.updateById(explain);
        if (affectedCount != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
        }
    }

    /**
     * 构造本次滚动受影响任务业务键集合。
     *
     * @param baseResult 操作范围参考
     * @param affectedResultList 受影响结果
     * @return 六班稳定任务业务键
     */
    private Set<String> buildAffectedTaskKeySet(TcScheduleResult baseResult,
                                                Collection<TcScheduleResult> affectedResultList) {
        Set<TcScheduleResult> affectedResultSet = new LinkedHashSet<>();
        affectedResultSet.add(baseResult);
        if (affectedResultList != null) {
            affectedResultSet.addAll(affectedResultList);
        }
        Set<String> taskKeySet = new LinkedHashSet<>();
        for (TcScheduleResult result : affectedResultSet) {
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                taskKeySet.add(this.buildManualTaskBusinessKey(result, shiftOrder));
            }
        }
        return taskKeySet;
    }

    /**
     * 按详设来源维度构造人工滚动稳定任务业务键。
     *
     * @param result 来源结果
     * @param shiftOrder 来源班次
     * @return 稳定任务业务键
     */
    private String buildManualTaskBusinessKey(TcScheduleResult result, int shiftOrder) {
        String sourceOrderNo = StringUtils.isBlank(result.getOrderNo())
                ? "RESULT" + Objects.toString(result.getId(), "NEW") : result.getOrderNo();
        String sourceSuffix = INSERT_DATA_SOURCE.equals(result.getDataSource()) ? "INSERT" : "MANUAL";
        return String.join("-", this.safeKeyPart(result.getFactoryCode()),
                this.safeKeyPart(result.getSidewallCode()), String.valueOf(shiftOrder),
                this.safeKeyPart(sourceOrderNo), sourceSuffix, Objects.toString(result.getId(), "NEW"));
    }

    /**
     * 将空业务键片段标准化为 UNKNOWN。
     *
     * @param value 原始值
     * @return 非空业务键片段
     */
    private String safeKeyPart(String value) {
        return StringUtils.isBlank(value) ? TcScheduleConstants.UNKNOWN_CODE : value.trim();
    }

    /**
     * 查询同工厂、日期、批次和机台的排程结果。
     *
     * @param reference 查询参考
     * @return 同机台结果
     */
    private List<TcScheduleResult> loadSameMachineResults(TcScheduleResult reference) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(reference.getBatchNo()), TcScheduleResult::getBatchNo,
                reference.getBatchNo());
        wrapper.eq(TcScheduleResult::getMachineCode, reference.getMachineCode());
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(wrapper);
        return resultList == null ? new ArrayList<>() : resultList;
    }

    /**
     * 校验排程结果均允许人工编辑。
     *
     * @param resultList 排程结果
     * @throws ServiceException 存在发布中或已发布结果时抛出
     */
    private void validateEditableResults(List<TcScheduleResult> resultList) {
        boolean blocked = resultList.stream().map(TcScheduleResult::getReleaseStatus)
                .anyMatch(status -> "3".equals(status) || "4".equals(status));
        if (blocked) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.releaseBlocked"));
        }
    }

    /**
     * 读取请求中唯一的操作班次。
     *
     * @param result 请求结果
     * @return 班次顺序
     * @throws ServiceException 没有计划量或出现多班次计划量时抛出
     */
    private int resolveOperationShift(TcScheduleResult result) {
        List<Integer> shiftOrderList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.getRawPlanQty(result, shiftOrder) != null) {
                shiftOrderList.add(shiftOrder);
            }
        }
        if (shiftOrderList.size() != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.singleShiftRequired"));
        }
        return shiftOrderList.get(0);
    }

    /**
     * 解析人工插单包含的全部班次，并校验每个计划量都同时提供顺序。
     *
     * @param result 插单结果
     * @return 从早到晚的插单班次
     * @throws ServiceException 没有计划量或计划量与顺序未成对时抛出
     */
    private List<Integer> resolveInsertShiftOrderList(TcScheduleResult result) {
        List<Integer> shiftOrderList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = this.getPlanQty(result, shiftOrder);
            Integer sequence = this.getSequence(result, shiftOrder);
            if (this.isPositive(planQty) && sequence == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftPairRequired"));
            }
            if (!this.isPositive(planQty) && sequence != null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftPairRequired"));
            }
            if (this.isPositive(planQty)) {
                shiftOrderList.add(shiftOrder);
            }
        }
        if (shiftOrderList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftRequired"));
        }
        return shiftOrderList;
    }

    /**
     * 读取并校验操作顺序。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 班内顺序
     */
    private int requireSequence(TcScheduleResult result, int shiftOrder) {
        Integer sequence = this.getSequence(result, shiftOrder);
        if (sequence == null || sequence < 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.invalidSequence"));
        }
        return sequence;
    }

    /**
     * 按主键读取排程结果。
     *
     * @param resultId 结果 ID
     * @return 排程结果
     * @throws ServiceException 结果不存在时抛出
     */
    private TcScheduleResult requireResult(Long resultId) {
        TcScheduleResult result = resultId == null ? null : this.scheduleResultMapper.selectById(resultId);
        if (result == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return result;
    }

    /**
     * 构造滚动任务。
     *
     * @param result 任务结果
     * @param shiftOrder 原班次
     * @param sequence 原顺序
     * @param planQty 计划量
     * @return 滚动任务
     */
    private TcManualRollingTask buildTask(TcScheduleResult result, int shiftOrder, int sequence,
                                          BigDecimal planQty) {
        TcManualRollingTask task = new TcManualRollingTask();
        task.setResult(result);
        task.setOriginalShiftOrder(shiftOrder);
        task.setOriginalSequence(sequence);
        task.setPlanQty(planQty);
        task.setFinishQty(this.getFinishQty(result, shiftOrder));
        return task;
    }

    /**
     * 构造保持用户指定最早班次的人工插单任务。
     *
     * @param insertResult 插单结果
     * @param shiftOrder 用户指定班次
     * @return 插单滚动任务
     */
    private TcManualRollingTask buildInsertTask(TcScheduleResult insertResult, int shiftOrder) {
        TcManualRollingTask task = this.buildTask(insertResult, shiftOrder,
                this.requireSequence(insertResult, shiftOrder), this.getPlanQty(insertResult, shiftOrder));
        task.setMinimumShiftOrder(shiftOrder);
        return task;
    }

    /**
     * 复制结果归并粒度和工艺快照，清空六班动态字段。
     *
     * @param source 源结果
     * @return 目标新结果模板
     */
    private TcScheduleResult copyBaseResult(TcScheduleResult source) {
        TcScheduleResult target = new TcScheduleResult();
        BeanUtils.copyProperties(source, target, "id", "createBy", "createTime", "updateBy", "updateTime");
        target.setId(null);
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            this.clearShift(target, shiftOrder);
        }
        return target;
    }

    /**
     * 判断两个结果是否属于可合并粒度。
     *
     * @param target 目标结果
     * @param source 源结果
     * @return true 表示允许合并
     */
    private boolean isSameMergeGrain(TcScheduleResult target, TcScheduleResult source) {
        return Objects.equals(target.getBatchNo(), source.getBatchNo())
                && Objects.equals(target.getSidewallCode(), source.getSidewallCode())
                && Objects.equals(target.getConstructionVersion(), source.getConstructionVersion())
                && Objects.equals(target.getSidewallCraft(), source.getSidewallCraft())
                && Objects.equals(target.getGlueCode(), source.getGlueCode())
                && Objects.equals(target.getBaseGlueCode(), source.getBaseGlueCode())
                && Objects.equals(target.getWholeGlueCode(), source.getWholeGlueCode())
                && Objects.equals(target.getMouthPlateCode(), source.getMouthPlateCode())
                && Objects.equals(target.getTailFlag(), source.getTailFlag());
    }

    /**
     * 已发布成功的人工调整结果回退为待发布状态。
     *
     * @param result 受影响结果
     */
    private void rollbackPublishedStatus(TcScheduleResult result) {
        if ("1".equals(result.getReleaseStatus())) {
            result.setReleaseStatus("5");
        }
    }

    /**
     * 清空指定班次的排程字段。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     */
    private void clearShift(TcScheduleResult result, int shiftOrder) {
        this.setSequence(result, shiftOrder, null);
        this.setPlanQty(result, shiftOrder, null);
        this.setFinishQty(result, shiftOrder, null);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), null);
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder), null);
    }

    /**
     * 动态读取班次计划量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 非空计划量
     */
    private BigDecimal getPlanQty(TcScheduleResult result, int shiftOrder) {
        Object value = this.getRawPlanQty(result, shiftOrder);
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }

    /**
     * 动态读取班次原始计划量，用于区分未提交字段与显式调为 0。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 原始计划量字段值
     */
    private Object getRawPlanQty(TcScheduleResult result, int shiftOrder) {
        return result.getFieldValueByFieldName(
                String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
    }

    /**
     * 动态设置班次计划量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @param planQty 计划量
     */
    private void setPlanQty(TcScheduleResult result, int shiftOrder, BigDecimal planQty) {
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
    }

    /**
     * 动态读取班次完成量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 非空完成量
     */
    private BigDecimal getFinishQty(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(
                String.format(TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder));
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }

    /**
     * 动态设置班次完成量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @param finishQty 完成量
     */
    private void setFinishQty(TcScheduleResult result, int shiftOrder, BigDecimal finishQty) {
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder),
                finishQty == null || finishQty.compareTo(BigDecimal.ZERO) == 0 ? null : finishQty);
    }

    /**
     * 动态读取班次顺序。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 班内顺序
     */
    private Integer getSequence(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(
                String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Integer ? (Integer) value : null;
    }

    /**
     * 动态设置班次顺序。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @param sequence 班内顺序
     */
    private void setSequence(TcScheduleResult result, int shiftOrder, Integer sequence) {
        result.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder),
                sequence);
    }

    /**
     * 判断数值是否为正数。
     *
     * @param value 数值
     * @return true 表示正数
     */
    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 将空或非正顺序标准化为 1。
     *
     * @param sequence 原顺序
     * @return 有效顺序
     */
    private int defaultSequence(Integer sequence) {
        return sequence == null || sequence < 1 ? 1 : sequence;
    }
}
