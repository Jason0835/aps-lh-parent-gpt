package com.zlt.aps.gsq.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommand;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommandBatch;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingContext;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingOperationEnum;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingResult;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingWriteResult;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualTaskDraft;
import com.zlt.aps.gsq.engine.service.facade.GsqScheduleOperationFacade;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.loader.GsqManualConstraintDataLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 钢丝圈人工滚动应用服务。
 *
 * <p>负责数据库快照与运行态任务之间的映射、结果装配和一次性持久化。
 * 所有滚动计算统一交给 aps-engine-gsq-core，计算期间不持有或修改数据库实体。</p>
 */
@Slf4j
@Service
public class GsqManualInsertRollingService {

    /** 插单数据来源标识 */
    private static final String INSERT_DATA_SOURCE = "1";

    /** 班次计划量字段模板 */
    private static final String CLASS_PLAN_QTY_FIELD_TEMPLATE = "class%dPlanQty";

    /** 班次完成量字段模板 */
    private static final String CLASS_FINISH_QTY_FIELD_TEMPLATE = "class%dFinishQty";

    /** 班次顺序字段模板 */
    private static final String CLASS_SEQUENCE_FIELD_TEMPLATE = "class%dSequence";

    /** 班次原因分析字段模板 */
    private static final String CLASS_ANALYSIS_FIELD_TEMPLATE = "class%dAnalysis";

    /** 班次开始时间字段模板 */
    private static final String CLASS_START_TIME_FIELD_TEMPLATE = "class%dStartTime";

    /** 班次结束时间字段模板 */
    private static final String CLASS_END_TIME_FIELD_TEMPLATE = "class%dEndTime";

    /** 钢丝圈最大班次数 */
    private static final int GSQ_MAX_SHIFT_ORDER = 6;

    private final GsqScheduleResultMapper gsqScheduleResultMapper;
    private final GsqScheduleOperationFacade gsqScheduleOperationFacade;
    private final GsqManualConstraintDataLoadService gsqManualConstraintDataLoadService;

    @Autowired
    public GsqManualInsertRollingService(GsqScheduleResultMapper gsqScheduleResultMapper,
                                        GsqScheduleOperationFacade gsqScheduleOperationFacade,
                                        GsqManualConstraintDataLoadService gsqManualConstraintDataLoadService) {
        this.gsqScheduleResultMapper = gsqScheduleResultMapper;
        this.gsqScheduleOperationFacade = gsqScheduleOperationFacade;
        this.gsqManualConstraintDataLoadService = gsqManualConstraintDataLoadService;
    }

    /**
     * 插单入口。
     *
     * @param insertResult 插单模板（含机台、钢丝圈、6班次计划量/顺序/分析、锚点）
     * @return 受影响行数
     */
    public int insertAndRoll(GsqScheduleResult insertResult) {
        List<Integer> shiftOrderList = this.resolveInsertShiftOrderList(insertResult);
        if (CollUtil.isEmpty(shiftOrderList)) {
            throw new ServiceException("插单班次和计划量不能为空");
        }
        String resultGroupKey = "MANUAL:" + IdUtil.fastSimpleUUID();
        GsqManualRollingCommandBatch commandBatch = new GsqManualRollingCommandBatch();
        for (int index = 0; index < shiftOrderList.size(); index++) {
            int shiftOrder = shiftOrderList.get(index);
            GsqManualTaskDraft task = this.mapInsertTask(insertResult, resultGroupKey, shiftOrder, index);
            GsqManualRollingCommand command = new GsqManualRollingCommand();
            command.setOperationType(GsqManualRollingOperationEnum.INSERT);
            command.setTargetMachineCode(insertResult.getMachineCode());
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getShiftSequence(insertResult, shiftOrder));
            command.setInsertTask(task);
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        Map<String, GsqScheduleResult> newTemplateMap = new LinkedHashMap<>();
        newTemplateMap.put(resultGroupKey, insertResult);
        GsqManualRollingWriteResult writeResult = this.executeAndPersist(insertResult,
                Collections.singletonList(insertResult.getMachineCode()), commandBatch, newTemplateMap);
        return writeResult.getInsertCount() + writeResult.getUnplannedCount();
    }

    /**
     * 转机台入口。
     *
     * @param transferResult 转机台请求（id + 新机台 + 目标班次 + 锚点）
     * @return 受影响行数
     */
    public int changeMachineAndRoll(GsqScheduleResult transferResult) {
        return this.changeMachineAndRollBatch(Collections.singletonList(transferResult));
    }

    /**
     * 批量转机台入口。
     *
     * @param transferResultList 转机台请求列表
     * @return 受影响行数
     */
    public int changeMachineAndRollBatch(List<GsqScheduleResult> transferResultList) {
        List<GsqScheduleResult> requestList = this.requireRequests(transferResultList);
        List<GsqScheduleResult> currentList = requestList.stream()
                .map(req -> this.loadResultById(req.getId(), "转机台排程结果不存在或已失效"))
                .collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        GsqManualRollingCommandBatch commandBatch = new GsqManualRollingCommandBatch();
        Set<String> machineCodeSet = currentList.stream().map(GsqScheduleResult::getMachineCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (int index = 0; index < requestList.size(); index++) {
            GsqScheduleResult request = requestList.get(index);
            GsqScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request, current);
            String targetMachineCode = StrUtil.trim(request.getMachineCode());
            machineCodeSet.add(targetMachineCode);
            GsqManualRollingCommand command = new GsqManualRollingCommand();
            command.setOperationType(GsqManualRollingOperationEnum.CHANGE_MACHINE);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setTargetMachineCode(targetMachineCode);
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getShiftSequence(request, shiftOrder));
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        GsqManualRollingWriteResult writeResult = this.executeAndPersist(currentList.get(0),
                new ArrayList<>(machineCodeSet), commandBatch, Collections.emptyMap());
        return writeResult.getUpdateCount() + writeResult.getInsertCount();
    }

    /**
     * 删除入口。
     *
     * @param deleteResult 删除请求（id）
     * @return 受影响行数
     */
    public int deleteAndRoll(GsqScheduleResult deleteResult) {
        return this.deleteAndRollBatch(Collections.singletonList(deleteResult));
    }

    /**
     * 批量删除入口。
     *
     * @param deleteResultList 删除请求列表
     * @return 受影响行数
     */
    public int deleteAndRollBatch(List<GsqScheduleResult> deleteResultList) {
        List<GsqScheduleResult> targetList = this.requireRequests(deleteResultList);
        this.validateSameScheduleScope(targetList);
        GsqManualRollingCommandBatch commandBatch = new GsqManualRollingCommandBatch();
        for (int index = 0; index < targetList.size(); index++) {
            GsqScheduleResult target = targetList.get(index);
            GsqManualRollingCommand command = new GsqManualRollingCommand();
            command.setOperationType(GsqManualRollingOperationEnum.DELETE);
            command.setResultGroupKey(String.valueOf(target.getId()));
            command.setSourceMachineCode(target.getMachineCode());
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        List<String> machineCodeList = targetList.stream().map(GsqScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        GsqManualRollingWriteResult writeResult = this.executeAndPersist(targetList.get(0),
                machineCodeList, commandBatch, Collections.emptyMap());
        return writeResult.getDeleteCount();
    }

    /**
     * 调量入口。
     *
     * @param changeResult 调量请求（id + 新计划量 + 原因分析）
     * @return 受影响行数
     */
    public int changeQtyAndRoll(GsqScheduleResult changeResult) {
        return this.changeQtyAndRollBatch(Collections.singletonList(changeResult));
    }

    /**
     * 批量调量入口。
     *
     * @param changeResultList 调量请求列表
     * @return 受影响行数
     */
    public int changeQtyAndRollBatch(List<GsqScheduleResult> changeResultList) {
        List<GsqScheduleResult> requestList = this.requireRequests(changeResultList);
        List<GsqScheduleResult> currentList = requestList.stream()
                .map(req -> this.loadResultById(req.getId(), "调量排程结果不存在或已失效"))
                .collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        GsqManualRollingCommandBatch commandBatch = new GsqManualRollingCommandBatch();
        for (int index = 0; index < requestList.size(); index++) {
            GsqScheduleResult request = requestList.get(index);
            GsqScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request, current);
            GsqManualRollingCommand command = new GsqManualRollingCommand();
            command.setOperationType(GsqManualRollingOperationEnum.CHANGE_QTY);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setPlanQty(BigDecimal.valueOf(this.getShiftPlanQty(request, shiftOrder)));
            command.setAnalysis(this.getShiftText(request, CLASS_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        GsqScheduleResult reference = currentList.get(0);
        List<String> machineCodeList = currentList.stream().map(GsqScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        GsqManualRollingWriteResult writeResult = this.executeAndPersist(reference,
                machineCodeList, commandBatch, Collections.emptyMap());
        return writeResult.getUpdateCount() + writeResult.getInsertCount();
    }

    // ==================== 核心装配 ====================

    /**
     * 加载快照→构建上下文→执行引擎→装配持久化。
     *
     * @param reference      参考记录（提供工厂、排程日期、批次号）
     * @param machineCodes   锁定机台集合
     * @param commandBatch   命令批次
     * @param newTemplateMap 新插单模板（key=resultGroupKey）
     * @return 写入统计
     */
    private GsqManualRollingWriteResult executeAndPersist(GsqScheduleResult reference,
                                                          List<String> machineCodes,
                                                          GsqManualRollingCommandBatch commandBatch,
                                                          Map<String, GsqScheduleResult> newTemplateMap) {
        List<GsqScheduleResult> snapshotList = this.loadScheduleResults(reference, machineCodes);
        this.validateEditableResults(snapshotList);
        GsqManualRollingContext context = this.buildContext(reference, machineCodes, snapshotList, commandBatch);
        GsqManualRollingResult rollingResult;
        try {
            rollingResult = gsqScheduleOperationFacade.execute(commandBatch, context);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("[GSQ_MANUAL_ROLL_VALIDATE] factoryCode={}, scheduleDate={}, batchNo={}, reason={}",
                    reference.getFactoryCode(), reference.getScheduleDate(), reference.getBatchNo(),
                    exception.getMessage());
            throw new ServiceException("钢丝圈人工排程操作失败：" + exception.getMessage());
        }
        return this.assembleAndPersist(snapshotList, rollingResult, newTemplateMap, reference);
    }

    /**
     * 构建运行态上下文。
     */
    private GsqManualRollingContext buildContext(GsqScheduleResult reference, List<String> machineCodes,
                                                List<GsqScheduleResult> snapshotList,
                                                GsqManualRollingCommandBatch commandBatch) {
        GsqManualRollingContext context = new GsqManualRollingContext();
        context.setFactoryCode(reference.getFactoryCode());
        context.setBatchNo(reference.getBatchNo());
        context.setScheduleDate(reference.getScheduleDate());
        context.setTraceId(IdUtil.fastSimpleUUID());
        context.setOperator("GSQ_MANUAL_OPERATION");
        // 加载机台定额等约束
        gsqManualConstraintDataLoadService.enrich(context, machineCodes, commandBatch);
        // 将数据库快照按6班次拆分为任务草稿
        List<GsqManualTaskDraft> taskList = new ArrayList<>();
        for (GsqScheduleResult result : snapshotList) {
            for (int shiftOrder = 1; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
                if (this.isPositive(this.getShiftPlanQty(result, shiftOrder))
                        || this.isPositive(this.getShiftFinishQty(result, shiftOrder))) {
                    taskList.add(this.mapExistingTask(result, shiftOrder));
                }
            }
        }
        context.setTaskList(taskList);
        return context;
    }

    /**
     * 装配横向结果并一次性持久化。
     */
    private GsqManualRollingWriteResult assembleAndPersist(List<GsqScheduleResult> snapshotList,
                                                           GsqManualRollingResult rollingResult,
                                                           Map<String, GsqScheduleResult> newTemplateMap,
                                                           GsqScheduleResult reference) {
        Map<Long, GsqScheduleResult> sourceMap = snapshotList.stream()
                .filter(result -> result.getId() != null)
                .collect(Collectors.toMap(GsqScheduleResult::getId, Function.identity(), (l, r) -> l,
                        LinkedHashMap::new));
        Map<String, List<GsqManualTaskDraft>> groupTaskMap = rollingResult.getScheduledTaskList().stream()
                .collect(Collectors.groupingBy(GsqManualTaskDraft::getResultGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, GsqScheduleResult> updateMap = new LinkedHashMap<>();
        List<GsqScheduleResult> insertList = new ArrayList<>();
        Set<Long> retainedSourceIdSet = new LinkedHashSet<>();

        for (Map.Entry<String, List<GsqManualTaskDraft>> entry : groupTaskMap.entrySet()) {
            List<GsqManualTaskDraft> groupTaskList = entry.getValue();
            GsqManualTaskDraft firstTask = groupTaskList.get(0);
            GsqScheduleResult source = firstTask.getSourceResultId() == null
                    ? null : sourceMap.get(firstTask.getSourceResultId());
            boolean reuseSource = source != null && entry.getKey().equals(String.valueOf(source.getId()));
            GsqScheduleResult target;
            if (reuseSource) {
                target = this.copyFullResult(source);
                retainedSourceIdSet.add(source.getId());
                updateMap.put(source.getId(), target);
            } else {
                GsqScheduleResult template = newTemplateMap.get(entry.getKey());
                if (template == null) {
                    template = source;
                }
                if (template == null) {
                    throw new ServiceException("钢丝圈人工排程操作失败：模板缺失");
                }
                target = this.copyBaseResult(template);
                target.setOrderNo(reference.getBatchNo() + "-MANUAL-" + IdUtil.fastSimpleUUID().substring(0, 8));
                target.setDataSource(INSERT_DATA_SOURCE);
                target.setIsRelease(ApsConstant.NO_RELEASE);
                insertList.add(target);
            }
            this.clearScheduleFields(target);
            target.setMachineCode(firstTask.getMachineCode());
            for (GsqManualTaskDraft task : groupTaskList) {
                this.writeTask(target, task);
            }
            if (reuseSource) {
                this.markEditedReleaseStatus(target);
            }
        }

        GsqManualRollingWriteResult writeResult = new GsqManualRollingWriteResult();
        Set<Long> allowedDeleteResultIdSet = new LinkedHashSet<>(rollingResult.getExplicitDeleteResultIdSet());
        allowedDeleteResultIdSet.addAll(rollingResult.getMoveToUnplannedResultIdSet());
        Set<Long> protectedSourceIdSet = snapshotList.stream().map(GsqScheduleResult::getId)
                .filter(Objects::nonNull)
                .filter(resultId -> !rollingResult.getExplicitDeleteResultIdSet().contains(resultId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean retainedProtectedResult = retainedSourceIdSet.stream().anyMatch(protectedSourceIdSet::contains);
        if (rollingResult.isContainsNonDeleteOperation() && !protectedSourceIdSet.isEmpty()
                && !retainedProtectedResult) {
            throw new ServiceException("非删除操作不允许清空机台全部排程结果");
        }
        for (GsqScheduleResult source : snapshotList) {
            if (!retainedSourceIdSet.contains(source.getId())) {
                if (!allowedDeleteResultIdSet.contains(source.getId())) {
                    throw new ServiceException("人工滚动变更计划不完整，已阻止隐式删除");
                }
                int deletedRows = gsqScheduleResultMapper.deleteById(source.getId());
                if (deletedRows != 1) {
                    throw new ServiceException("排程状态已变化，请刷新后重试");
                }
                writeResult.setDeleteCount(writeResult.getDeleteCount() + deletedRows);
            }
        }
        for (GsqScheduleResult insertResult : insertList) {
            writeResult.setInsertCount(writeResult.getInsertCount() + gsqScheduleResultMapper.insert(insertResult));
        }
        for (GsqScheduleResult updateResult : updateMap.values()) {
            int updatedRows = gsqScheduleResultMapper.updateById(updateResult);
            if (updatedRows != 1) {
                throw new ServiceException("排程状态已变化，请刷新后重试");
            }
            writeResult.setUpdateCount(writeResult.getUpdateCount() + updatedRows);
        }
        return writeResult;
    }

    // ==================== 映射辅助方法 ====================

    /**
     * 既有排程结果→任务草稿。
     */
    private GsqManualTaskDraft mapExistingTask(GsqScheduleResult result, int shiftOrder) {
        GsqManualTaskDraft task = new GsqManualTaskDraft();
        task.setTaskId(result.getId() + ":" + shiftOrder + ":0");
        task.setResultGroupKey(String.valueOf(result.getId()));
        task.setSourceResultId(result.getId());
        task.setSourceShiftOrder(shiftOrder);
        task.setSourceSequence(this.getShiftSequence(result, shiftOrder));
        task.setMachineCode(result.getMachineCode());
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.getShiftSequence(result, shiftOrder));
        task.setPlanQty(BigDecimal.valueOf(this.nvlInt(this.getShiftPlanQty(result, shiftOrder))));
        task.setFinishQty(BigDecimal.valueOf(this.nvlInt(this.getShiftFinishQty(result, shiftOrder))));
        task.setSteelRingCode(result.getSteelRingCode());
        task.setProSize(result.getProSize());
        task.setDataSource(result.getDataSource());
        task.setAnalysis(this.getShiftText(result, CLASS_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
        return task;
    }

    /**
     * 插单模板→任务草稿。
     */
    private GsqManualTaskDraft mapInsertTask(GsqScheduleResult insertResult, String resultGroupKey,
                                            int shiftOrder, int fragmentIndex) {
        GsqManualTaskDraft task = new GsqManualTaskDraft();
        task.setTaskId(resultGroupKey + ":" + shiftOrder + ":" + fragmentIndex);
        task.setResultGroupKey(resultGroupKey);
        task.setSourceShiftOrder(shiftOrder);
        task.setSourceSequence(this.getShiftSequence(insertResult, shiftOrder));
        task.setMachineCode(insertResult.getMachineCode());
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.getShiftSequence(insertResult, shiftOrder));
        task.setPlanQty(BigDecimal.valueOf(this.nvlInt(this.getShiftPlanQty(insertResult, shiftOrder))));
        task.setFinishQty(BigDecimal.ZERO);
        task.setSteelRingCode(insertResult.getSteelRingCode());
        task.setProSize(insertResult.getProSize());
        task.setDataSource(INSERT_DATA_SOURCE);
        task.setAnalysis(this.getShiftText(insertResult, CLASS_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
        task.setInsertTask(true);
        return task;
    }

    /**
     * 任务草稿→横向6班次字段（关键：BigDecimal 转 Integer）。
     */
    private void writeTask(GsqScheduleResult target, GsqManualTaskDraft task) {
        int shiftOrder = task.getShiftOrder();
        this.setShiftValue(target, CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftOrder,
                task.getPlanQty() == null ? null : task.getPlanQty().intValue());
        this.setShiftValue(target, CLASS_FINISH_QTY_FIELD_TEMPLATE, shiftOrder,
                task.getFinishQty() == null ? null : task.getFinishQty().intValue());
        this.setShiftValue(target, CLASS_SEQUENCE_FIELD_TEMPLATE, shiftOrder, task.getSequence());
        this.setShiftValue(target, CLASS_ANALYSIS_FIELD_TEMPLATE, shiftOrder, task.getAnalysis());
    }

    /**
     * 清空6班次可变字段。
     */
    private void clearScheduleFields(GsqScheduleResult result) {
        for (int shiftOrder = 1; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            this.setShiftValue(result, CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftOrder, null);
            this.setShiftValue(result, CLASS_FINISH_QTY_FIELD_TEMPLATE, shiftOrder, null);
            this.setShiftValue(result, CLASS_SEQUENCE_FIELD_TEMPLATE, shiftOrder, null);
            this.setShiftValue(result, CLASS_START_TIME_FIELD_TEMPLATE, shiftOrder, null);
            this.setShiftValue(result, CLASS_END_TIME_FIELD_TEMPLATE, shiftOrder, null);
            this.setShiftValue(result, CLASS_ANALYSIS_FIELD_TEMPLATE, shiftOrder, null);
        }
    }

    // ==================== 反射辅助方法（动态字段访问） ====================

    private Integer getShiftPlanQty(GsqScheduleResult result, int shiftOrder) {
        String fieldName = String.format(CLASS_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
        return (Integer) result.getFieldValueByFieldName(fieldName);
    }

    private Integer getShiftFinishQty(GsqScheduleResult result, int shiftOrder) {
        String fieldName = String.format(CLASS_FINISH_QTY_FIELD_TEMPLATE, shiftOrder);
        return (Integer) result.getFieldValueByFieldName(fieldName);
    }

    private Integer getShiftSequence(GsqScheduleResult result, int shiftOrder) {
        String fieldName = String.format(CLASS_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
        return (Integer) result.getFieldValueByFieldName(fieldName);
    }

    private String getShiftText(GsqScheduleResult result, String template, int shiftOrder) {
        String fieldName = String.format(template, shiftOrder);
        return (String) result.getFieldValueByFieldName(fieldName);
    }

    private void setShiftValue(GsqScheduleResult result, String template, int shiftOrder, Object value) {
        String fieldName = String.format(template, shiftOrder);
        result.setFieldValueByFieldName(fieldName, value);
    }

    // ==================== 其他辅助方法 ====================

    /**
     * 加载同工厂同排程日期同机台的快照。
     */
    private List<GsqScheduleResult> loadScheduleResults(GsqScheduleResult reference, List<String> machineCodes) {
        if (CollUtil.isEmpty(machineCodes)) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GsqScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(GsqScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.in(GsqScheduleResult::getMachineCode, machineCodes);
        wrapper.eq(GsqScheduleResult::getIsDelete, 0);
        return gsqScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 校验发布状态等。
     */
    private void validateEditableResults(List<GsqScheduleResult> snapshotList) {
        if (CollUtil.isEmpty(snapshotList)) {
            return;
        }
        for (GsqScheduleResult result : snapshotList) {
            if (ApsConstant.IS_RELEASE.equals(result.getIsRelease())) {
                throw new ServiceException("已发布成功的排程结果不允许人工操作:" + result.getId());
            }
        }
    }

    /**
     * 校验同工厂同排程日期同批次。
     */
    private void validateSameScheduleScope(List<GsqScheduleResult> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        GsqScheduleResult reference = list.get(0);
        for (GsqScheduleResult result : list) {
            if (!Objects.equals(reference.getFactoryCode(), result.getFactoryCode())
                    || !Objects.equals(reference.getScheduleDate(), result.getScheduleDate())) {
                throw new ServiceException("批量操作必须属于同一工厂同一排程日期");
            }
        }
    }

    /**
     * 解析插单涉及的班次列表。
     */
    private List<Integer> resolveInsertShiftOrderList(GsqScheduleResult insertResult) {
        List<Integer> shiftOrderList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isPositive(this.getShiftPlanQty(insertResult, shiftOrder))) {
                shiftOrderList.add(shiftOrder);
            }
        }
        return shiftOrderList;
    }

    /**
     * 解析操作班次（请求中指定，否则取首个有计划量的班次）。
     */
    private int resolveOperationShift(GsqScheduleResult request, GsqScheduleResult current) {
        for (int shiftOrder = 1; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isPositive(this.getShiftPlanQty(request, shiftOrder))) {
                return shiftOrder;
            }
        }
        for (int shiftOrder = 1; shiftOrder <= GSQ_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isPositive(this.getShiftPlanQty(current, shiftOrder))) {
                return shiftOrder;
            }
        }
        return 1;
    }

    private GsqScheduleResult loadResultById(Long id, String errorMsg) {
        GsqScheduleResult result = gsqScheduleResultMapper.selectById(id);
        if (result == null || Objects.equals(result.getIsDelete(), 1)) {
            throw new ServiceException(errorMsg);
        }
        return result;
    }

    private List<GsqScheduleResult> requireRequests(List<GsqScheduleResult> list) {
        if (CollUtil.isEmpty(list)) {
            throw new ServiceException("请求不能为空");
        }
        return list;
    }

    private GsqScheduleResult copyFullResult(GsqScheduleResult source) {
        GsqScheduleResult target = new GsqScheduleResult();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private GsqScheduleResult copyBaseResult(GsqScheduleResult source) {
        return this.copyFullResult(source);
    }

    /**
     * 人工编辑后发布状态回退为待发布。
     */
    private void markEditedReleaseStatus(GsqScheduleResult target) {
        if (ApsConstant.IS_RELEASE.equals(target.getIsRelease())) {
            target.setIsRelease(ApsConstant.WAIT_RELEASING);
        }
    }

    private boolean isPositive(Integer value) {
        return value != null && value > 0;
    }

    private int nvlInt(Integer value) {
        return value == null ? 0 : value;
    }
}
