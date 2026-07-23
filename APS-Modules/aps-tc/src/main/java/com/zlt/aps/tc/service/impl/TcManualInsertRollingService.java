package com.zlt.aps.tc.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResultExplain;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.engine.domain.manual.*;
import com.zlt.aps.tc.engine.service.facade.TcScheduleOperationFacade;
import com.zlt.aps.tc.engine.service.impl.TcManualRollingEngineService;
import com.zlt.aps.tc.engine.service.impl.TcTaskChainScheduleService;
import com.zlt.aps.tc.mapper.TcScheduleResultExplainMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import com.zlt.aps.tc.service.loader.TcManualConstraintDataLoadService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工滚动应用服务。
 *
 * <p>本服务只负责数据库快照与独立任务片段之间的映射、结果装配和一次性持久化；
 * 插单、删除、调量、转机台和自动滚动的任务链计算统一交给纯滚动引擎。</p>
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

    private final TcScheduleOperationFacade scheduleOperationFacade;

    private final TcManualConstraintDataLoadService manualConstraintDataLoadService;

    /**
     * 构造胎侧人工滚动应用服务。
     *
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     * @param scheduleResultExplainMapper 结果解释 Mapper
     * @param machineRuleValidator 机台规则校验器
     * @param scheduleOperationFacade 纯计算门面
     * @param manualConstraintDataLoadService 人工约束数据装载服务
     */
    @Autowired
    public TcManualInsertRollingService(TcScheduleResultMapper scheduleResultMapper,
                                        TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                        TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                        TcManualMachineRuleValidator machineRuleValidator,
                                        TcScheduleOperationFacade scheduleOperationFacade,
                                        TcManualConstraintDataLoadService manualConstraintDataLoadService) {
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.scheduleResultExplainMapper = scheduleResultExplainMapper;
        this.machineRuleValidator = machineRuleValidator;
        this.scheduleOperationFacade = scheduleOperationFacade;
        this.manualConstraintDataLoadService = manualConstraintDataLoadService;
    }

    /**
     * 为不启动 Spring 的单元测试创建应用服务。
     *
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     * @param scheduleResultExplainMapper 结果解释 Mapper
     * @param machineRuleValidator 机台规则校验器
     */
    public TcManualInsertRollingService(TcScheduleResultMapper scheduleResultMapper,
                                        TcScheduleUnplannedMapper scheduleUnplannedMapper,
                                        TcScheduleResultExplainMapper scheduleResultExplainMapper,
                                        TcManualMachineRuleValidator machineRuleValidator) {
        this(scheduleResultMapper, scheduleUnplannedMapper, scheduleResultExplainMapper, machineRuleValidator,
                new TcScheduleOperationFacade(new TcTaskChainScheduleService(), null, null,
                        new TcManualRollingEngineService()), null);
    }

    /**
     * 插入一个或多个班次任务并统一滚动落库。
     *
     * @param insertResult 插单结果模板
     * @return 新增结果行数
     */
    int insertAndRoll(TcScheduleResult insertResult) {
        insertResult.setDataSource(INSERT_DATA_SOURCE);
        insertResult.setReleaseStatus(RELEASE_STATUS_UNPUBLISHED);
        insertResult.setTaskVersion(0L);
        List<Integer> shiftOrderList = this.resolveInsertShiftOrderList(insertResult);
        String resultGroupKey = "MANUAL:" + IdUtil.fastSimpleUUID();
        TcManualRollingCommandBatch commandBatch = new TcManualRollingCommandBatch();
        for (int index = 0; index < shiftOrderList.size(); index++) {
            int shiftOrder = shiftOrderList.get(index);
            TcManualTaskDraft task = this.mapTask(insertResult, resultGroupKey, shiftOrder, index);
            task.setSourceResultId(null);
            task.setInsertTask(true);
            task.setDataSource(INSERT_DATA_SOURCE);
            TcManualRollingCommand command = new TcManualRollingCommand();
            command.setOperationType(TcManualRollingOperationEnum.INSERT);
            command.setTargetMachineCode(insertResult.getMachineCode());
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getSequence(insertResult, shiftOrder));
            command.setInsertTask(task);
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        Map<String, TcScheduleResult> templateMap = new LinkedHashMap<>();
        templateMap.put(resultGroupKey, insertResult);
        return this.executeAndPersist(insertResult, Collections.singletonList(insertResult.getMachineCode()),
                commandBatch, templateMap).getInsertCount();
    }

    /**
     * 单条调量并滚动。
     *
     * @param changeResult 调量请求
     * @return 受影响结果行数
     */
    int changeQtyAndRoll(TcScheduleResult changeResult) {
        return this.changeQtyAndRollBatch(Collections.singletonList(changeResult));
    }

    /**
     * 批量调量并只持久化一次。
     *
     * @param changeResultList 调量请求集合
     * @return 受影响结果行数
     */
    int changeQtyAndRollBatch(List<TcScheduleResult> changeResultList) {
        List<TcScheduleResult> requestList = this.requireRequests(changeResultList);
        List<TcScheduleResult> currentList = requestList.stream()
                .map(request -> this.requireResult(request.getId())).collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        TcManualRollingCommandBatch commandBatch = new TcManualRollingCommandBatch();
        for (int index = 0; index < requestList.size(); index++) {
            TcScheduleResult request = requestList.get(index);
            TcScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request);
            TcManualRollingCommand command = new TcManualRollingCommand();
            command.setOperationType("ROLLING_RECALC".equals(this.getAnalysis(request, shiftOrder))
                    ? TcManualRollingOperationEnum.AUTO_ROLLING : TcManualRollingOperationEnum.CHANGE_QTY);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setPlanQty(this.getPlanQty(request, shiftOrder));
            command.setAnalysis(this.getAnalysis(request, shiftOrder));
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        List<String> machineCodeList = currentList.stream().map(TcScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        TcManualRollingWriteResult writeResult = this.executeAndPersist(currentList.get(0), machineCodeList,
                commandBatch, Collections.emptyMap());
        return writeResult.getInsertCount() + writeResult.getUpdateCount();
    }

    /**
     * 单条转机台并滚动。
     *
     * @param transferResult 转机台请求
     * @return 受影响结果行数
     */
    int changeMachineAndRoll(TcScheduleResult transferResult) {
        return this.changeMachineAndRollBatch(Collections.singletonList(transferResult));
    }

    /**
     * 批量转机台并只持久化一次。
     *
     * @param transferResultList 转机台请求集合
     * @return 受影响结果行数
     */
    int changeMachineAndRollBatch(List<TcScheduleResult> transferResultList) {
        List<TcScheduleResult> requestList = this.requireRequests(transferResultList);
        List<TcScheduleResult> currentList = requestList.stream()
                .map(request -> this.requireResult(request.getId())).collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        Set<String> machineCodeSet = currentList.stream().map(TcScheduleResult::getMachineCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        TcManualRollingCommandBatch commandBatch = new TcManualRollingCommandBatch();
        for (int index = 0; index < requestList.size(); index++) {
            TcScheduleResult request = requestList.get(index);
            TcScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request);
            if (Objects.equals(StringUtils.trim(current.getMachineCode()), StringUtils.trim(request.getMachineCode()))) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.sameMachine"));
            }
            machineCodeSet.add(request.getMachineCode());
            TcManualRollingCommand command = new TcManualRollingCommand();
            command.setOperationType(TcManualRollingOperationEnum.CHANGE_MACHINE);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setTargetMachineCode(request.getMachineCode());
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getSequence(request, shiftOrder));
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        TcManualRollingWriteResult writeResult = this.executeAndPersist(currentList.get(0),
                new ArrayList<>(machineCodeSet), commandBatch, Collections.emptyMap());
        return writeResult.getInsertCount() + writeResult.getUpdateCount();
    }

    /**
     * 批量删除结果并统一滚动。
     *
     * @param deleteResultList 删除目标快照
     * @return 逻辑删除结果行数
     */
    int deleteAndRollBatch(List<TcScheduleResult> deleteResultList) {
        List<TcScheduleResult> targetList = this.requireRequests(deleteResultList);
        this.validateSameScheduleScope(targetList);
        TcManualRollingCommandBatch commandBatch = new TcManualRollingCommandBatch();
        for (int index = 0; index < targetList.size(); index++) {
            TcScheduleResult target = targetList.get(index);
            TcManualRollingCommand command = new TcManualRollingCommand();
            command.setOperationType(TcManualRollingOperationEnum.DELETE);
            command.setResultGroupKey(String.valueOf(target.getId()));
            command.setSourceMachineCode(target.getMachineCode());
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        List<String> machineCodeList = targetList.stream().map(TcScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        return this.executeAndPersist(targetList.get(0), machineCodeList, commandBatch,
                Collections.emptyMap()).getDeleteCount();
    }

    /**
     * 兼容既有调用，将单条删除转为批量命令。
     *
     * @param removedResult 待删除结果
     * @param startShiftOrder 兼容参数，实际由任务来源班次确定最早范围
     * @return 受影响结果行数
     */
    int rollAfterRemove(TcScheduleResult removedResult, int startShiftOrder) {
        return this.deleteAndRollBatch(Collections.singletonList(removedResult));
    }

    /** 构建运行态、执行纯引擎并统一落库。 */
    private TcManualRollingWriteResult executeAndPersist(TcScheduleResult reference, List<String> machineCodeList,
                                                          TcManualRollingCommandBatch commandBatch,
                                                          Map<String, TcScheduleResult> newTemplateMap) {
        List<TcScheduleResult> snapshotList = this.loadScheduleResults(reference, machineCodeList);
        this.validateEditableResults(snapshotList);
        TcManualRollingContext context = this.buildContext(
                reference, machineCodeList, snapshotList, commandBatch);
        TcManualRollingResult rollingResult;
        try {
            rollingResult = this.scheduleOperationFacade.execute(commandBatch, context);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
        }
        return this.assembleAndPersist(reference, snapshotList, rollingResult, newTemplateMap);
    }

    /** 将数据库横表快照拆成独立班次任务。 */
    private TcManualRollingContext buildContext(TcScheduleResult reference, List<String> machineCodeList,
                                                 List<TcScheduleResult> snapshotList,
                                                 TcManualRollingCommandBatch commandBatch) {
        TcManualRollingContext context = new TcManualRollingContext();
        context.setFactoryCode(reference.getFactoryCode());
        context.setScheduleDate(reference.getScheduleDate());
        context.setBatchNo(reference.getBatchNo());
        List<TcManualTaskDraft> taskList = new ArrayList<>();
        for (TcScheduleResult result : snapshotList) {
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                if (this.getPlanQty(result, shiftOrder).signum() > 0) {
                    taskList.add(this.mapTask(result, String.valueOf(result.getId()), shiftOrder, 0));
                }
            }
        }
        context.setTaskList(taskList);
        for (String machineCode : machineCodeList) {
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                context.getShiftCapacityMap().put(machineCode + "|" + shiftOrder,
                        this.machineRuleValidator.resolveRollingCapacity(reference, machineCode, shiftOrder));
            }
        }
        if (manualConstraintDataLoadService != null) {
            manualConstraintDataLoadService.enrich(context, machineCodeList, commandBatch);
        }
        return context;
    }

    /** 将一条横向结果班次映射为独立任务片段。 */
    private TcManualTaskDraft mapTask(TcScheduleResult result, String resultGroupKey,
                                      int shiftOrder, int fragmentIndex) {
        TcManualTaskDraft task = new TcManualTaskDraft();
        task.setTaskId(Objects.toString(result.getId(), "NEW") + ":" + shiftOrder + ":" + fragmentIndex);
        task.setResultGroupKey(resultGroupKey);
        task.setMergeGrainKey(this.buildMergeGrainKey(result));
        task.setSourceResultId(result.getId());
        task.setSourceShiftOrder(shiftOrder);
        task.setSourceSequence(this.defaultSequence(this.getSequence(result, shiftOrder)));
        task.setMachineCode(result.getMachineCode());
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.defaultSequence(this.getSequence(result, shiftOrder)));
        task.setPlanQty(this.getPlanQty(result, shiftOrder));
        task.setFinishQty(this.getFinishQty(result, shiftOrder));
        task.setSidewallCode(result.getSidewallCode());
        task.setGlueCode(result.getGlueCode());
        task.setBaseGlueCode(result.getBaseGlueCode());
        task.setMouthPlateCode(result.getMouthPlateCode());
        task.setDataSource(result.getDataSource());
        task.setAnalysis(this.getAnalysis(result, shiftOrder));
        task.setSourceStartTime(this.getDate(result, TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder));
        task.setSourceEndTime(this.getDate(result, TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder));
        return task;
    }

    /** 装配完整横表并一次性持久化。 */
    private TcManualRollingWriteResult assembleAndPersist(TcScheduleResult reference,
                                                           List<TcScheduleResult> snapshotList,
                                                           TcManualRollingResult rollingResult,
                                                           Map<String, TcScheduleResult> newTemplateMap) {
        Map<String, TcScheduleResult> existingMap = snapshotList.stream().collect(Collectors.toMap(
                result -> String.valueOf(result.getId()), result -> result, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<TcManualTaskDraft>> groupTaskMap = rollingResult.getScheduledTaskList().stream()
                .collect(Collectors.groupingBy(TcManualTaskDraft::getResultGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, TcScheduleResult> assembledMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<TcManualTaskDraft>> entry : groupTaskMap.entrySet()) {
            TcScheduleResult target = existingMap.get(entry.getKey());
            if (target == null) {
                TcScheduleResult template = newTemplateMap.get(entry.getKey());
                if (template == null) {
                    TcManualTaskDraft firstTask = entry.getValue().get(0);
                    template = firstTask.getSourceResultId() == null ? reference
                            : existingMap.get(String.valueOf(firstTask.getSourceResultId()));
                }
                target = this.copyBaseResult(template == null ? reference : template);
                target.setOrderNo(reference.getBatchNo() + "-MANUAL-" + IdUtil.fastSimpleUUID().substring(0, 8));
            }
            this.clearAllShifts(target);
            List<TcManualTaskDraft> orderedTaskList = entry.getValue().stream()
                    .sorted(Comparator.comparing(TcManualTaskDraft::getShiftOrder)
                            .thenComparing(TcManualTaskDraft::getSequence)).collect(Collectors.toList());
            target.setMachineCode(orderedTaskList.get(0).getMachineCode());
            for (TcManualTaskDraft task : orderedTaskList) {
                int shiftOrder = task.getShiftOrder();
                if (this.getRawPlanQty(target, shiftOrder) != null) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
                }
                this.setPlanQty(target, shiftOrder, task.getPlanQty());
                this.setFinishQty(target, shiftOrder, task.getFinishQty());
                this.setSequence(target, shiftOrder, task.getSequence());
                this.setAnalysis(target, shiftOrder, StringUtils.isBlank(task.getAnalysis())
                        ? "{\"schemaVersion\":1,\"source\":\"MANUAL_ROLLING\"}" : task.getAnalysis());
                if (Objects.equals(task.getSourceShiftOrder(), task.getShiftOrder())) {
                    this.setDate(target, TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE,
                            shiftOrder, task.getSourceStartTime());
                    this.setDate(target, TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                            shiftOrder, task.getSourceEndTime());
                }
            }
            assembledMap.put(entry.getKey(), target);
        }
        TcManualRollingWriteResult writeResult = new TcManualRollingWriteResult();
        for (Map.Entry<String, TcScheduleResult> entry : existingMap.entrySet()) {
            TcScheduleResult target = assembledMap.remove(entry.getKey());
            if (target == null) {
                if (this.scheduleResultMapper.deleteById(entry.getValue().getId()) != 1) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                }
                writeResult.setDeleteCount(writeResult.getDeleteCount() + 1);
            } else {
                target.setTaskVersion(target.getTaskVersion() == null ? 1L : target.getTaskVersion() + 1L);
                this.rollbackPublishedStatus(target);
                if (this.scheduleResultMapper.updateById(target) != 1) {
                    throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
                }
                writeResult.setUpdateCount(writeResult.getUpdateCount() + 1);
            }
        }
        for (TcScheduleResult target : assembledMap.values()) {
            target.setId(null);
            target.setTaskVersion(0L);
            target.setReleaseStatus(RELEASE_STATUS_UNPUBLISHED);
            if (StringUtils.isBlank(target.getDataSource())) {
                target.setDataSource(INSERT_DATA_SOURCE);
            }
            if (this.scheduleResultMapper.insert(target) != 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
            }
            writeResult.setInsertCount(writeResult.getInsertCount() + 1);
        }
        this.persistUnplanned(reference, rollingResult.getUnplannedTaskList(), snapshotList, newTemplateMap);
        return writeResult;
    }

    /** 将未排片段同步到未排表和解释表。 */
    private void persistUnplanned(TcScheduleResult reference, List<TcManualTaskDraft> unplannedTaskList,
                                  Collection<TcScheduleResult> snapshotList,
                                  Map<String, TcScheduleResult> newTemplateMap) {
        Map<String, TcManualTaskDraft> desiredMap = new LinkedHashMap<>();
        for (TcManualTaskDraft task : unplannedTaskList) {
            TcScheduleResult source = task.getSourceResultId() == null ? newTemplateMap.get(task.getResultGroupKey())
                    : snapshotList.stream().filter(item -> Objects.equals(item.getId(), task.getSourceResultId()))
                    .findFirst().orElse(reference);
            if (source == null) {
                source = reference;
            }
            String taskBusinessKey = this.buildManualTaskBusinessKey(source, task.getSourceShiftOrder());
            TcManualTaskDraft existing = desiredMap.get(taskBusinessKey);
            if (existing == null) {
                desiredMap.put(taskBusinessKey, task);
            } else {
                existing.setPlanQty(existing.getPlanQty().add(task.getPlanQty()));
            }
        }
        Set<String> affectedTaskKeySet = new LinkedHashSet<>();
        List<TcScheduleResult> affectedSourceList = new ArrayList<>(snapshotList);
        affectedSourceList.addAll(newTemplateMap.values());
        for (TcScheduleResult source : affectedSourceList) {
            for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
                affectedTaskKeySet.add(this.buildManualTaskBusinessKey(source, shiftOrder));
            }
        }
        LambdaQueryWrapper<TcScheduleUnplanned> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleUnplanned::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TcScheduleUnplanned::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(TcScheduleUnplanned::getBatchNo, reference.getBatchNo());
        wrapper.eq(TcScheduleUnplanned::getUnplannedReasonCode, "CAPACITY_NOT_ENOUGH");
        List<TcScheduleUnplanned> existingList = this.scheduleUnplannedMapper.selectList(wrapper);
        Map<String, TcScheduleUnplanned> existingMap = existingList == null ? new LinkedHashMap<>()
                : existingList.stream().filter(item -> affectedTaskKeySet.contains(item.getTaskBusinessKey()))
                .collect(Collectors.toMap(TcScheduleUnplanned::getTaskBusinessKey, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        for (String taskBusinessKey : affectedTaskKeySet) {
            TcManualTaskDraft desiredTask = desiredMap.remove(taskBusinessKey);
            TcScheduleUnplanned existing = existingMap.get(taskBusinessKey);
            if (desiredTask == null && existing != null) {
                existing.setPlanQty(BigDecimal.ZERO);
                existing.setUnplannedEvidenceJson(
                        "{\"schemaVersion\":1,\"status\":\"RESOLVED_BY_MANUAL_ROLLING\"}");
                this.updateUnplanned(existing);
                this.upsertUnplannedExplain(reference, null, taskBusinessKey, true);
            } else if (desiredTask != null) {
                TcScheduleUnplanned unplanned = existing == null ? new TcScheduleUnplanned() : existing;
                this.fillUnplanned(reference, desiredTask, taskBusinessKey, unplanned);
                if (existing == null) {
                    if (this.scheduleUnplannedMapper.insert(unplanned) != 1) {
                        throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
                    }
                } else {
                    this.updateUnplanned(unplanned);
                }
                this.upsertUnplannedExplain(reference, desiredTask, taskBusinessKey, false);
            }
        }
        for (Map.Entry<String, TcManualTaskDraft> entry : desiredMap.entrySet()) {
            TcScheduleUnplanned unplanned = new TcScheduleUnplanned();
            this.fillUnplanned(reference, entry.getValue(), entry.getKey(), unplanned);
            if (this.scheduleUnplannedMapper.insert(unplanned) != 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
            }
            this.upsertUnplannedExplain(reference, entry.getValue(), entry.getKey(), false);
        }
    }

    /** 回填未排任务字段。 */
    private void fillUnplanned(TcScheduleResult reference, TcManualTaskDraft task,
                               String taskBusinessKey, TcScheduleUnplanned unplanned) {
            unplanned.setFactoryCode(reference.getFactoryCode());
            unplanned.setBatchNo(reference.getBatchNo());
            unplanned.setScheduleDate(reference.getScheduleDate());
            unplanned.setTaskBusinessKey(taskBusinessKey);
            unplanned.setSidewallCode(task.getSidewallCode());
            unplanned.setGlueCode(task.getGlueCode());
            unplanned.setMouthPlateCode(task.getMouthPlateCode());
            unplanned.setShiftOrder(task.getSourceShiftOrder());
            unplanned.setPlanQty(task.getPlanQty());
            unplanned.setUnplannedReasonCode("CAPACITY_NOT_ENOUGH");
            unplanned.setUnplannedReasonDesc(I18nUtil.getMessage("ui.tc.schedule.unplanned.capacityNotEnough"));
            unplanned.setUnplannedEvidenceJson(
                    "{\"schemaVersion\":1,\"rule\":\"TC_SHIFT_MAX_CAPACITY\",\"source\":\"MANUAL_ROLLING\"}");
    }

    /** 更新已有未排任务。 */
    private void updateUnplanned(TcScheduleUnplanned unplanned) {
        if (this.scheduleUnplannedMapper.updateById(unplanned) != 1) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.persistFailed"));
        }
    }

    /** 新增或更新未排解释。 */
    private void upsertUnplannedExplain(TcScheduleResult reference, TcManualTaskDraft task,
                                        String taskBusinessKey, boolean resolved) {
        LambdaQueryWrapper<TcScheduleResultExplain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResultExplain::getBatchNo, reference.getBatchNo());
        wrapper.eq(TcScheduleResultExplain::getTaskBusinessKey, taskBusinessKey);
        List<TcScheduleResultExplain> explainList = this.scheduleResultExplainMapper.selectList(wrapper);
        TcScheduleResultExplain explain = explainList == null || explainList.isEmpty()
                ? new TcScheduleResultExplain() : explainList.get(0);
        explain.setFactoryCode(reference.getFactoryCode());
        explain.setBatchNo(reference.getBatchNo());
        explain.setScheduleDate(reference.getScheduleDate());
        explain.setTaskBusinessKey(taskBusinessKey);
        if (task != null) {
            explain.setSidewallCode(task.getSidewallCode());
            explain.setShiftOrder(task.getSourceShiftOrder());
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

    /** 查询锁定机台范围内的完整横表快照。 */
    private List<TcScheduleResult> loadScheduleResults(TcScheduleResult reference, List<String> machineCodeList) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(reference.getBatchNo()), TcScheduleResult::getBatchNo,
                reference.getBatchNo());
        wrapper.in(TcScheduleResult::getMachineCode, machineCodeList);
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(wrapper);
        return resultList == null ? new ArrayList<>() : resultList;
    }

    /** 校验结果均可人工编辑。 */
    private void validateEditableResults(List<TcScheduleResult> resultList) {
        if (resultList.stream().map(TcScheduleResult::getReleaseStatus)
                .anyMatch(status -> "3".equals(status) || "4".equals(status))) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.releaseBlocked"));
        }
    }

    /** 校验批量请求处于同一工厂、日期和批次。 */
    private void validateSameScheduleScope(List<TcScheduleResult> resultList) {
        TcScheduleResult reference = resultList.get(0);
        boolean invalid = resultList.stream().anyMatch(result -> !Objects.equals(reference.getFactoryCode(),
                result.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(), result.getScheduleDate())
                || !Objects.equals(reference.getBatchNo(), result.getBatchNo()));
        if (invalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.concurrentChanged"));
        }
    }

    /** 校验并返回非空请求。 */
    private List<TcScheduleResult> requireRequests(List<TcScheduleResult> requestList) {
        if (requestList == null || requestList.isEmpty() || requestList.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return requestList;
    }

    /** 按主键读取有效结果。 */
    private TcScheduleResult requireResult(Long resultId) {
        TcScheduleResult result = resultId == null ? null : this.scheduleResultMapper.selectById(resultId);
        if (result == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        return result;
    }

    /** 解析请求中唯一操作班次。 */
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

    /** 解析多班插单班次并校验计划量和顺序成对。 */
    private List<Integer> resolveInsertShiftOrderList(TcScheduleResult result) {
        List<Integer> shiftOrderList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = this.getPlanQty(result, shiftOrder);
            Integer sequence = this.getSequence(result, shiftOrder);
            if ((planQty.signum() > 0) != (sequence != null)) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftPairRequired"));
            }
            if (planQty.signum() > 0) {
                shiftOrderList.add(shiftOrder);
            }
        }
        if (shiftOrderList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftRequired"));
        }
        return shiftOrderList;
    }

    /** 复制归并粒度和工艺快照并清空动态字段。 */
    private TcScheduleResult copyBaseResult(TcScheduleResult source) {
        TcScheduleResult target = new TcScheduleResult();
        BeanUtils.copyProperties(source, target, "id", "createBy", "createTime", "updateBy", "updateTime");
        target.setId(null);
        this.clearAllShifts(target);
        return target;
    }

    /** 清空六班动态字段。 */
    private void clearAllShifts(TcScheduleResult result) {
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            this.setPlanQty(result, shiftOrder, null);
            this.setFinishQty(result, shiftOrder, null);
            this.setSequence(result, shiftOrder, null);
            this.setAnalysis(result, shiftOrder, null);
            this.setDate(result, TcScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder, null);
            this.setDate(result, TcScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder, null);
        }
    }

    /** 构造胎侧完整兼容归并粒度键。 */
    private String buildMergeGrainKey(TcScheduleResult result) {
        return String.join("|", this.safe(result.getBatchNo()), this.safe(result.getSidewallCode()),
                this.safe(result.getConstructionVersion()), this.safe(result.getSidewallCraft()),
                this.safe(result.getGlueCode()), this.safe(result.getBaseGlueCode()),
                this.safe(result.getWholeGlueCode()), this.safe(result.getMouthPlateCode()),
                this.safe(result.getTailFlag()));
    }

    /** 构造稳定未排任务业务键。 */
    private String buildManualTaskBusinessKey(TcScheduleResult result, Integer shiftOrder) {
        String sourceOrderNo = StringUtils.isBlank(result.getOrderNo())
                ? "RESULT" + Objects.toString(result.getId(), "NEW") : result.getOrderNo();
        String sourceSuffix = INSERT_DATA_SOURCE.equals(result.getDataSource()) ? "INSERT" : "MANUAL";
        return String.join("-", this.safeKeyPart(result.getFactoryCode()),
                this.safeKeyPart(result.getSidewallCode()), Objects.toString(shiftOrder, "1"),
                this.safeKeyPart(sourceOrderNo), sourceSuffix, Objects.toString(result.getId(), "NEW"));
    }

    /** 已发布结果回退待发布状态。 */
    private void rollbackPublishedStatus(TcScheduleResult result) {
        if ("1".equals(result.getReleaseStatus())) {
            result.setReleaseStatus("5");
        }
    }

    /** 读取计划量。 */
    private BigDecimal getPlanQty(TcScheduleResult result, int shiftOrder) {
        Object value = this.getRawPlanQty(result, shiftOrder);
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }

    /** 读取原始计划量。 */
    private Object getRawPlanQty(TcScheduleResult result, int shiftOrder) {
        return result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
    }

    /** 写入计划量。 */
    private void setPlanQty(TcScheduleResult result, int shiftOrder, BigDecimal value) {
        result.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), value);
    }

    /** 读取完成量。 */
    private BigDecimal getFinishQty(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder));
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }

    /** 写入完成量。 */
    private void setFinishQty(TcScheduleResult result, int shiftOrder, BigDecimal value) {
        result.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder), value);
    }

    /** 读取顺序。 */
    private Integer getSequence(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    /** 写入顺序。 */
    private void setSequence(TcScheduleResult result, int shiftOrder, Integer value) {
        result.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), value);
    }

    /** 读取原因分析。 */
    private String getAnalysis(TcScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
        return value == null ? null : value.toString();
    }

    /** 写入原因分析。 */
    private void setAnalysis(TcScheduleResult result, int shiftOrder, String value) {
        result.setFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder), value);
    }

    /** 读取日期动态字段。 */
    private java.util.Date getDate(TcScheduleResult result, String fieldTemplate, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(fieldTemplate, shiftOrder));
        return value instanceof java.util.Date ? (java.util.Date) value : null;
    }

    /** 写入日期动态字段。 */
    private void setDate(TcScheduleResult result, String fieldTemplate, int shiftOrder, java.util.Date value) {
        result.setFieldValueByFieldName(String.format(fieldTemplate, shiftOrder), value);
    }

    /** 空顺序按 1 处理。 */
    private int defaultSequence(Integer sequence) {
        return sequence == null || sequence < 1 ? 1 : sequence;
    }

    /** 空归并字段标准化。 */
    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** 空业务键字段标准化。 */
    private String safeKeyPart(String value) {
        return StringUtils.isBlank(value) ? TcScheduleConstants.UNKNOWN_CODE : value.trim();
    }
}
