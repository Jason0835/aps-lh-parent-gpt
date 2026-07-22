package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.enums.TmReleaseStatusTransition;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.domain.vo.TmManualRollingWriteResult;
import com.zlt.aps.tm.engine.domain.manual.*;
import com.zlt.aps.tm.engine.service.facade.TmScheduleOperationFacade;
import com.zlt.aps.tm.engine.validator.TmInsertPositionValidator;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎面人工滚动应用服务。
 *
 * <p>本服务只负责数据库快照与独立运行态任务之间的映射、结果装配和一次性持久化。
 * 所有滚动计算统一交给 aps-engine-tm，计算期间不持有或修改数据库实体。</p>
 */
@Slf4j
@Service
public class TmManualInsertRollingService {

    private static final String INSERT_DATA_SOURCE = "INSERT";

    private final TmScheduleResultMapper tmScheduleResultMapper;

    private final TmMachineInfoMapper tmMachineInfoMapper;

    private final TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    private final TmScheduleOperationFacade tmScheduleOperationFacade;

    /**
     * 构造人工滚动应用服务。
     *
     * @param tmScheduleResultMapper    排程结果 Mapper
     * @param tmMachineInfoMapper       机台信息 Mapper
     * @param tmScheduleUnplannedMapper 未排结果 Mapper
     * @param tmScheduleOperationFacade 排程纯计算门面
     */
    @Autowired
    public TmManualInsertRollingService(TmScheduleResultMapper tmScheduleResultMapper,
                                        TmMachineInfoMapper tmMachineInfoMapper,
                                        TmScheduleUnplannedMapper tmScheduleUnplannedMapper,
                                        TmScheduleOperationFacade tmScheduleOperationFacade) {
        this.tmScheduleResultMapper = tmScheduleResultMapper;
        this.tmMachineInfoMapper = tmMachineInfoMapper;
        this.tmScheduleUnplannedMapper = tmScheduleUnplannedMapper;
        this.tmScheduleOperationFacade = tmScheduleOperationFacade;
    }

    /**
     * 为不启动 Spring 的既有单元测试创建应用服务。
     *
     * @param tmScheduleResultMapper    排程结果 Mapper
     * @param tmMachineInfoMapper       机台信息 Mapper
     * @param tmScheduleUnplannedMapper 未排结果 Mapper
     */
    public TmManualInsertRollingService(TmScheduleResultMapper tmScheduleResultMapper,
                                        TmMachineInfoMapper tmMachineInfoMapper,
                                        TmScheduleUnplannedMapper tmScheduleUnplannedMapper) {
        this(tmScheduleResultMapper, tmMachineInfoMapper, tmScheduleUnplannedMapper,
                new TmScheduleOperationFacade(new com.zlt.aps.tm.engine.service.impl.TmTaskChainScheduleService(),
                        null, null, new com.zlt.aps.tm.engine.service.impl.TmManualRollingEngineService()));
    }

    /**
     * 插入一个或多个班次的人工任务并一次滚动落库。
     *
     * @param insertResult 插单模板
     * @return 新增结果行数
     * @throws ServiceException 插单位置、机台产能或任务链校验失败时抛出
     */
    int insertAndRoll(TmScheduleResult insertResult) {
        List<Integer> shiftOrderList = this.resolveInsertShiftOrderList(insertResult);
        if (CollUtil.isEmpty(shiftOrderList)) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.insertShiftEmpty", "插单班次和顺序不能为空"));
        }
        String resultGroupKey = "MANUAL:" + IdUtil.fastSimpleUUID();
        TmManualRollingCommandBatch commandBatch = new TmManualRollingCommandBatch();
        for (int index = 0; index < shiftOrderList.size(); index++) {
            int shiftOrder = shiftOrderList.get(index);
            TmManualTaskDraft task = this.mapInsertTask(insertResult, resultGroupKey, shiftOrder, index);
            TmManualRollingCommand command = new TmManualRollingCommand();
            command.setOperationType(TmManualRollingOperationEnum.INSERT);
            command.setTargetMachineCode(insertResult.getMachineCode());
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getShiftSequence(insertResult, shiftOrder));
            command.setInsertTask(task);
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        Map<String, TmScheduleResult> newTemplateMap = new LinkedHashMap<>();
        newTemplateMap.put(resultGroupKey, insertResult);
        TmManualRollingWriteResult writeResult = this.executeAndPersist(insertResult,
                Collections.singletonList(insertResult.getMachineCode()), commandBatch, newTemplateMap);
        log.info("[TM_MANUAL_ROLL] operation=INSERT, factoryCode={}, scheduleDate={}, machineCode={}, insertCount={}, updateCount={}, unplannedQty={}",
                insertResult.getFactoryCode(), insertResult.getScheduleDate(), insertResult.getMachineCode(),
                writeResult.getInsertCount(), writeResult.getUpdateCount(), writeResult.getUnplannedQty());
        return writeResult.getInsertCount();
    }

    /**
     * 单条调量并滚动。
     *
     * @param changeResult 调量请求
     * @return 更新及新增行数
     * @throws ServiceException 目标不存在或校验失败时抛出
     */
    int changeQtyAndRoll(TmScheduleResult changeResult) {
        return this.changeQtyAndRollBatch(Collections.singletonList(changeResult));
    }

    /**
     * 在同一运行态上下文中批量调量并只持久化一次。
     *
     * @param changeResultList 调量请求集合
     * @return 更新及新增行数
     * @throws ServiceException 请求跨越不同工厂日期批次或校验失败时抛出
     */
    int changeQtyAndRollBatch(List<TmScheduleResult> changeResultList) {
        List<TmScheduleResult> requestList = this.requireRequests(changeResultList);
        List<TmScheduleResult> currentList = requestList.stream()
                .map(request -> this.loadResultById(request.getId(),
                        "ui.data.alert.tm.schedule.changeQtyResultNotFound", "调量排程结果不存在或已失效"))
                .collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        TmManualRollingCommandBatch commandBatch = new TmManualRollingCommandBatch();
        for (int index = 0; index < requestList.size(); index++) {
            TmScheduleResult request = requestList.get(index);
            TmScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request, current);
            TmManualRollingCommand command = new TmManualRollingCommand();
            String analysis = this.getShiftText(request,
                    TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder);
            command.setOperationType("ROLLING_RECALC".equals(analysis)
                    ? TmManualRollingOperationEnum.AUTO_ROLLING : TmManualRollingOperationEnum.CHANGE_QTY);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setPlanQty(this.getShiftPlanQty(request, shiftOrder));
            command.setAnalysis(analysis);
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        TmScheduleResult reference = currentList.get(0);
        List<String> machineCodeList = currentList.stream().map(TmScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        TmManualRollingWriteResult writeResult = this.executeAndPersist(reference,
                machineCodeList, commandBatch, Collections.emptyMap());
        return writeResult.getUpdateCount() + writeResult.getInsertCount();
    }

    /**
     * 单条转机台并滚动。
     *
     * @param transferResult 转机台请求
     * @return 更新及新增行数
     * @throws ServiceException 目标不存在或校验失败时抛出
     */
    int changeMachineAndRoll(TmScheduleResult transferResult) {
        return this.changeMachineAndRollBatch(Collections.singletonList(transferResult));
    }

    /**
     * 在同一上下文中批量转机台并只持久化一次。
     *
     * @param transferResultList 转机台请求集合
     * @return 更新及新增行数
     * @throws ServiceException 请求跨越不同工厂日期批次或校验失败时抛出
     */
    int changeMachineAndRollBatch(List<TmScheduleResult> transferResultList) {
        List<TmScheduleResult> requestList = this.requireRequests(transferResultList);
        List<TmScheduleResult> currentList = requestList.stream()
                .map(request -> this.loadResultById(request.getId(),
                        "ui.data.alert.tm.schedule.changeMachineResultNotFound", "转机台排程结果不存在或已失效"))
                .collect(Collectors.toList());
        this.validateSameScheduleScope(currentList);
        TmManualRollingCommandBatch commandBatch = new TmManualRollingCommandBatch();
        Set<String> machineCodeSet = currentList.stream().map(TmScheduleResult::getMachineCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (int index = 0; index < requestList.size(); index++) {
            TmScheduleResult request = requestList.get(index);
            TmScheduleResult current = currentList.get(index);
            int shiftOrder = this.resolveOperationShift(request, current);
            String targetMachineCode = StrUtil.trim(request.getMachineCode());
            machineCodeSet.add(targetMachineCode);
            TmManualRollingCommand command = new TmManualRollingCommand();
            command.setOperationType(TmManualRollingOperationEnum.CHANGE_MACHINE);
            command.setResultGroupKey(String.valueOf(current.getId()));
            command.setSourceMachineCode(current.getMachineCode());
            command.setSourceShiftOrder(shiftOrder);
            command.setTargetMachineCode(targetMachineCode);
            command.setTargetShiftOrder(shiftOrder);
            command.setTargetSequence(this.getShiftSequence(request, shiftOrder));
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        TmManualRollingWriteResult writeResult = this.executeAndPersist(currentList.get(0),
                new ArrayList<>(machineCodeSet), commandBatch, Collections.emptyMap());
        return writeResult.getUpdateCount() + writeResult.getInsertCount();
    }

    /**
     * 删除单条结果并滚动。
     *
     * @param deleteResult 待删除结果
     * @return 逻辑删除行数
     * @throws ServiceException 删除目标非法时抛出
     */
    int deleteAndRoll(TmScheduleResult deleteResult) {
        return this.deleteAndRollBatch(Collections.singletonList(deleteResult));
    }

    /**
     * 在一个上下文中批量删除并只持久化一次。
     *
     * @param deleteResultList 待删除结果集合
     * @return 逻辑删除行数
     * @throws ServiceException 请求跨越不同工厂日期批次或校验失败时抛出
     */
    int deleteAndRollBatch(List<TmScheduleResult> deleteResultList) {
        List<TmScheduleResult> targetList = this.requireRequests(deleteResultList);
        this.validateSameScheduleScope(targetList);
        TmManualRollingCommandBatch commandBatch = new TmManualRollingCommandBatch();
        for (int index = 0; index < targetList.size(); index++) {
            TmScheduleResult target = targetList.get(index);
            TmManualRollingCommand command = new TmManualRollingCommand();
            command.setOperationType(TmManualRollingOperationEnum.DELETE);
            command.setResultGroupKey(String.valueOf(target.getId()));
            command.setSourceMachineCode(target.getMachineCode());
            command.setCommandOrder(index);
            commandBatch.addCommand(command);
        }
        List<String> machineCodeList = targetList.stream().map(TmScheduleResult::getMachineCode)
                .distinct().collect(Collectors.toList());
        TmManualRollingWriteResult writeResult = this.executeAndPersist(targetList.get(0),
                machineCodeList, commandBatch, Collections.emptyMap());
        return writeResult.getDeleteCount();
    }

    /**
     * 将锁定范围快照映射为运行态、执行引擎并一次性装配持久化。
     *
     * @param reference      排程范围参考
     * @param machineCodes   锁定机台集合
     * @param commandBatch   批量命令
     * @param newTemplateMap 新结果分组模板
     * @return 写入统计
     * @throws ServiceException 计算或落库校验失败时抛出并由外层事务回滚
     */
    private TmManualRollingWriteResult executeAndPersist(TmScheduleResult reference,
                                                          List<String> machineCodes,
                                                          TmManualRollingCommandBatch commandBatch,
                                                          Map<String, TmScheduleResult> newTemplateMap) {
        List<TmScheduleResult> snapshotList = this.loadScheduleResults(reference, machineCodes);
        this.validateEditableResults(snapshotList);
        TmManualRollingContext context = this.buildContext(reference, machineCodes, snapshotList);
        TmManualRollingResult rollingResult;
        try {
            rollingResult = tmScheduleOperationFacade.execute(commandBatch, context);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("[TM_MANUAL_ROLL_VALIDATE] factoryCode={}, scheduleDate={}, batchNo={}, reason={}",
                    reference.getFactoryCode(), reference.getScheduleDate(), reference.getBatchNo(), exception.getMessage());
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
        }
        return this.assembleAndPersist(snapshotList, rollingResult, newTemplateMap, reference);
    }

    /**
     * 构建与数据库实体解耦的运行态上下文。
     *
     * @param reference    排程范围参考
     * @param machineCodes 机台集合
     * @param snapshotList 数据库快照
     * @return 运行态上下文
     */
    private TmManualRollingContext buildContext(TmScheduleResult reference, List<String> machineCodes,
                                                 List<TmScheduleResult> snapshotList) {
        TmManualRollingContext context = new TmManualRollingContext();
        context.setFactoryCode(reference.getFactoryCode());
        context.setBatchNo(reference.getBatchNo());
        context.setScheduleDate(reference.getScheduleDate());
        context.setTraceId(IdUtil.fastSimpleUUID());
        context.setOperator("TM_MANUAL_OPERATION");
        context.setMachineCapacityMap(this.loadMachineCapacityMap(reference.getFactoryCode(), machineCodes));
        List<TmManualTaskDraft> taskList = new ArrayList<>();
        for (TmScheduleResult result : snapshotList) {
            for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
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
     * 装配横向结果并在当前事务内一次提交全部新增、更新、删除和未排记录。
     *
     * @param snapshotList  原始快照
     * @param rollingResult 引擎结果
     * @param newTemplateMap 新分组模板
     * @param reference     排程范围参考
     * @return 写入统计
     * @throws ServiceException 更新冲突或装配校验失败时抛出
     */
    private TmManualRollingWriteResult assembleAndPersist(List<TmScheduleResult> snapshotList,
                                                           TmManualRollingResult rollingResult,
                                                           Map<String, TmScheduleResult> newTemplateMap,
                                                           TmScheduleResult reference) {
        Map<Long, TmScheduleResult> sourceMap = snapshotList.stream()
                .filter(result -> result.getId() != null)
                .collect(Collectors.toMap(TmScheduleResult::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, List<TmManualTaskDraft>> groupTaskMap = rollingResult.getScheduledTaskList().stream()
                .collect(Collectors.groupingBy(TmManualTaskDraft::getResultGroupKey,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, TmScheduleResult> updateMap = new LinkedHashMap<>();
        List<TmScheduleResult> insertList = new ArrayList<>();
        Set<Long> retainedSourceIdSet = new LinkedHashSet<>();

        for (Map.Entry<String, List<TmManualTaskDraft>> entry : groupTaskMap.entrySet()) {
            List<TmManualTaskDraft> groupTaskList = entry.getValue();
            TmManualTaskDraft firstTask = groupTaskList.get(0);
            TmScheduleResult source = firstTask.getSourceResultId() == null
                    ? null : sourceMap.get(firstTask.getSourceResultId());
            boolean reuseSource = source != null && entry.getKey().equals(String.valueOf(source.getId()));
            TmScheduleResult target;
            if (reuseSource) {
                target = this.copyFullResult(source);
                retainedSourceIdSet.add(source.getId());
                updateMap.put(source.getId(), target);
            } else {
                TmScheduleResult template = newTemplateMap.get(entry.getKey());
                if (template == null) {
                    template = source;
                }
                if (template == null) {
                    throw new ServiceException(this.resolveTmMessage(
                            "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
                }
                target = this.copyBaseResult(template);
                target.setOrderNo(reference.getBatchNo() + "-MANUAL-" + IdUtil.fastSimpleUUID().substring(0, 8));
                target.setDataSource(INSERT_DATA_SOURCE);
                target.setReleaseStatus(ApsConstant.NO_RELEASE);
                insertList.add(target);
            }
            this.clearScheduleFields(target);
            target.setMachineCode(firstTask.getMachineCode());
            for (TmManualTaskDraft task : groupTaskList) {
                this.writeTask(target, task);
            }
            this.validateAssembledResult(target);
            if (reuseSource) {
                this.markEditedReleaseStatus(target);
            }
        }

        TmManualRollingWriteResult writeResult = new TmManualRollingWriteResult();
        for (TmScheduleResult source : snapshotList) {
            if (!retainedSourceIdSet.contains(source.getId())) {
                int deletedRows = tmScheduleResultMapper.deleteById(source.getId());
                if (deletedRows != 1) {
                    throw new ServiceException(this.resolveTmMessage(
                            "ui.data.alert.tm.schedule.operationConcurrentChanged", "排程状态已变化，请刷新后重试"));
                }
                writeResult.setDeleteCount(writeResult.getDeleteCount() + deletedRows);
            }
        }
        for (TmScheduleResult insertResult : insertList) {
            writeResult.setInsertCount(writeResult.getInsertCount() + tmScheduleResultMapper.insert(insertResult));
        }
        for (TmScheduleResult updateResult : updateMap.values()) {
            int updatedRows = tmScheduleResultMapper.updateById(updateResult);
            if (updatedRows != 1) {
                throw new ServiceException(this.resolveTmMessage(
                        "ui.data.alert.tm.schedule.operationConcurrentChanged", "排程状态已变化，请刷新后重试"));
            }
            writeResult.setUpdateCount(writeResult.getUpdateCount() + updatedRows);
        }
        for (TmManualTaskDraft unplannedTask : rollingResult.getUnplannedTaskList()) {
            this.insertUnplanned(reference, unplannedTask);
            writeResult.setUnplannedCount(writeResult.getUnplannedCount() + 1);
            writeResult.setUnplannedQty(BigDecimalUtils.add(writeResult.getUnplannedQty(), unplannedTask.getPlanQty()));
        }
        return writeResult;
    }

    /**
     * 将既有横向班次映射为独立任务片段。
     *
     * @param result     排程结果
     * @param shiftOrder 班次
     * @return 独立任务片段
     */
    private TmManualTaskDraft mapExistingTask(TmScheduleResult result, int shiftOrder) {
        TmManualTaskDraft task = new TmManualTaskDraft();
        task.setTaskId(result.getId() + ":" + shiftOrder + ":0");
        task.setResultGroupKey(String.valueOf(result.getId()));
        task.setSourceResultId(result.getId());
        task.setSourceShiftOrder(shiftOrder);
        task.setSourceSequence(this.getShiftSequence(result, shiftOrder));
        task.setMachineCode(result.getMachineCode());
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.getShiftSequence(result, shiftOrder));
        task.setPlanQty(this.getShiftPlanQty(result, shiftOrder));
        task.setFinishQty(this.getShiftFinishQty(result, shiftOrder));
        task.setTreadCode(result.getTreadCode());
        task.setGlueCode(result.getGlueCode());
        task.setBaseGlueCode(result.getBaseGlueCode());
        task.setMouthPlateCode(result.getMouthPlateCode());
        task.setDataSource(result.getDataSource());
        task.setAnalysis(this.getShiftText(result, TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
        task.setSourceStartTime(this.getShiftDate(result, TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder));
        task.setSourceEndTime(this.getShiftDate(result, TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder));
        return task;
    }

    /**
     * 将插单班次映射为稳定临时任务。
     *
     * @param resultGroupKey 新结果分组键
     * @param insertResult   插单模板
     * @param shiftOrder     班次
     * @param fragmentIndex  片段号
     * @return 插单任务
     */
    private TmManualTaskDraft mapInsertTask(TmScheduleResult insertResult, String resultGroupKey,
                                             int shiftOrder, int fragmentIndex) {
        TmManualTaskDraft task = new TmManualTaskDraft();
        task.setTaskId(resultGroupKey + ":" + shiftOrder + ":" + fragmentIndex);
        task.setResultGroupKey(resultGroupKey);
        task.setSourceShiftOrder(shiftOrder);
        task.setSourceSequence(this.getShiftSequence(insertResult, shiftOrder));
        task.setMachineCode(insertResult.getMachineCode());
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.getShiftSequence(insertResult, shiftOrder));
        task.setPlanQty(this.getShiftPlanQty(insertResult, shiftOrder));
        task.setFinishQty(BigDecimal.ZERO);
        task.setTreadCode(insertResult.getTreadCode());
        task.setGlueCode(insertResult.getGlueCode());
        task.setBaseGlueCode(insertResult.getBaseGlueCode());
        task.setMouthPlateCode(insertResult.getMouthPlateCode());
        task.setDataSource(INSERT_DATA_SOURCE);
        task.setAnalysis(this.getShiftText(insertResult, TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder));
        task.setInsertTask(true);
        return task;
    }

    /**
     * 将任务片段写入横向结果的动态班次字段。
     *
     * @param target 目标结果
     * @param task   任务片段
     */
    private void writeTask(TmScheduleResult target, TmManualTaskDraft task) {
        int shiftOrder = task.getShiftOrder();
        this.setShiftValue(target, TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder, task.getPlanQty());
        this.setShiftValue(target, TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder, task.getFinishQty());
        this.setShiftValue(target, TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder, task.getSequence());
        this.setShiftValue(target, TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder, task.getAnalysis());
        if (Objects.equals(task.getSourceShiftOrder(), task.getShiftOrder())) {
            this.setShiftValue(target, TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE,
                    shiftOrder, task.getSourceStartTime());
            this.setShiftValue(target, TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                    shiftOrder, task.getSourceEndTime());
        }
    }

    /**
     * 清空横向结果的六班可变字段，随后完全按最终任务链重建。
     *
     * @param result 横向结果
     */
    private void clearScheduleFields(TmScheduleResult result) {
        List<String> fieldTemplateList = java.util.Arrays.asList(
                TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE);
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            for (String fieldTemplate : fieldTemplateList) {
                this.setShiftValue(result, fieldTemplate, shiftOrder, null);
            }
        }
    }

    /**
     * 校验单行装配后不存在同班覆盖且计划量不小于完成量。
     *
     * @param result 装配结果
     * @throws ServiceException 数量非法时抛出
     */
    private void validateAssembledResult(TmScheduleResult result) {
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = this.getShiftPlanQty(result, shiftOrder);
            BigDecimal finishQty = this.getShiftFinishQty(result, shiftOrder);
            if (planQty.compareTo(finishQty) < 0 || finishQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException(this.resolveTmMessage(
                        "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
            }
        }
    }

    /**
     * 查询锁定范围内排程结果。
     *
     * @param reference    范围参考
     * @param machineCodes 机台集合
     * @return 当前结果快照
     */
    private List<TmScheduleResult> loadScheduleResults(TmScheduleResult reference, List<String> machineCodes) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, reference.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, reference.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(reference.getBatchNo()), TmScheduleResult::getBatchNo, reference.getBatchNo());
        wrapper.in(TmScheduleResult::getMachineCode, machineCodes);
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 查询锁定机台班产。
     *
     * @param factoryCode 工厂编码
     * @param machineCodes 机台集合
     * @return 机台班产映射
     * @throws ServiceException 任一机台未维护有效班产时抛出
     */
    private Map<String, BigDecimal> loadMachineCapacityMap(String factoryCode, List<String> machineCodes) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, factoryCode);
        wrapper.in(TmMachineInfo::getMachineCode, machineCodes);
        Map<String, BigDecimal> capacityMap = tmMachineInfoMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(TmMachineInfo::getMachineCode, TmMachineInfo::getMaxCapacity,
                        (left, right) -> left, LinkedHashMap::new));
        boolean missingCapacity = machineCodes.stream()
                .anyMatch(machineCode -> !this.isPositive(capacityMap.get(machineCode)));
        if (missingCapacity) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.insertMachineCapacityEmpty", "机台最大班产未维护"));
        }
        return capacityMap;
    }

    /**
     * 写入第六班后仍无法容纳的未排任务。
     *
     * @param reference 排程范围参考
     * @param task      未排任务
     */
    private void insertUnplanned(TmScheduleResult reference, TmManualTaskDraft task) {
        TmUnplannedReasonEnum reason = TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
        TmScheduleUnplanned unplanned = new TmScheduleUnplanned();
        unplanned.setFactoryCode(reference.getFactoryCode());
        unplanned.setBatchNo(reference.getBatchNo());
        unplanned.setScheduleDate(reference.getScheduleDate());
        unplanned.setTreadCode(task.getTreadCode());
        unplanned.setGlueCode(task.getGlueCode());
        unplanned.setMouthPlateCode(task.getMouthPlateCode());
        unplanned.setUnplannedReasonCode(reason.getCode());
        unplanned.setUnplannedReasonDesc(reason.getDesc());
        Map<String, Object> evidenceMap = new LinkedHashMap<>();
        evidenceMap.put("source", "MANUAL_ROLL");
        evidenceMap.put("machineCode", task.getMachineCode());
        evidenceMap.put("taskId", task.getTaskId());
        evidenceMap.put("unplannedQty", task.getPlanQty());
        unplanned.setUnplannedEvidenceJson(JSON.toJSONString(evidenceMap));
        if (tmScheduleUnplannedMapper.insert(unplanned) != 1) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
        }
    }

    /**
     * 复制完整排程结果，确保引擎计算完成前不修改数据库快照。
     *
     * @param source 来源结果
     * @return 完整副本
     */
    private TmScheduleResult copyFullResult(TmScheduleResult source) {
        TmScheduleResult target = this.copyBaseResult(source);
        target.setId(source.getId());
        target.setReleaseStatus(source.getReleaseStatus());
        target.setDataSource(source.getDataSource());
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            for (String fieldTemplate : this.scheduleFieldTemplates()) {
                String fieldName = String.format(fieldTemplate, shiftOrder);
                target.setFieldValueByFieldName(fieldName, source.getFieldValueByFieldName(fieldName));
            }
        }
        return target;
    }

    /**
     * 复制新增结果需要的业务字段。
     *
     * @param source 来源模板
     * @return 新结果模板
     */
    private TmScheduleResult copyBaseResult(TmScheduleResult source) {
        TmScheduleResult target = new TmScheduleResult();
        target.setFactoryCode(source.getFactoryCode());
        target.setBatchNo(source.getBatchNo());
        target.setOrderNo(source.getOrderNo());
        target.setScheduleDate(source.getScheduleDate());
        target.setMachineCode(source.getMachineCode());
        target.setTreadCode(source.getTreadCode());
        target.setGlueCode(source.getGlueCode());
        target.setBaseGlueCode(source.getBaseGlueCode());
        target.setWholeGlueCode(source.getWholeGlueCode());
        target.setGlueSeq(source.getGlueSeq());
        target.setMouthPlateCode(source.getMouthPlateCode());
        target.setTreadShoulderLength(source.getTreadShoulderLength());
        target.setCxRemainQty(source.getCxRemainQty());
        target.setMaterialCode(source.getMaterialCode());
        target.setMaterialDesc(source.getMaterialDesc());
        target.setEmbryoCode(source.getEmbryoCode());
        target.setMainMaterialDesc(source.getMainMaterialDesc());
        target.setCxMachineCode(source.getCxMachineCode());
        target.setSixClockStockQty(source.getSixClockStockQty());
        target.setCurlRollLength(source.getCurlRollLength());
        target.setTailFlag(source.getTailFlag());
        target.setRemark(source.getRemark());
        return target;
    }

    /**
     * 人工编辑后按既有状态迁移矩阵回退发布状态。
     *
     * @param result 待更新结果
     * @throws ServiceException 状态不可编辑时抛出
     */
    private void markEditedReleaseStatus(TmScheduleResult result) {
        if (!TmReleaseStatusTransition.isEditable(result.getReleaseStatus())) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.illegalReleaseTransition", "排程发布状态迁移不合法"));
        }
        if (ApsConstant.IS_RELEASE.equals(result.getReleaseStatus())) {
            result.setReleaseStatus(ApsConstant.WAIT_RELEASING);
        }
    }

    /**
     * 校验锁定范围结果均可编辑。
     *
     * @param resultList 结果集合
     * @throws ServiceException 存在不可编辑状态时抛出
     */
    private void validateEditableResults(List<TmScheduleResult> resultList) {
        if (resultList.stream().anyMatch(result -> !TmReleaseStatusTransition.isEditable(result.getReleaseStatus()))) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
    }

    /**
     * 校验批量请求处于同一工厂、日期和批次。
     *
     * @param resultList 结果集合
     * @throws ServiceException 范围不一致时抛出
     */
    private void validateSameScheduleScope(List<TmScheduleResult> resultList) {
        TmScheduleResult first = resultList.get(0);
        boolean invalid = resultList.stream().anyMatch(result ->
                !Objects.equals(first.getFactoryCode(), result.getFactoryCode())
                        || !Objects.equals(first.getScheduleDate(), result.getScheduleDate())
                        || !Objects.equals(first.getBatchNo(), result.getBatchNo()));
        if (invalid) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
        }
    }

    /**
     * 规范化非空请求集合。
     *
     * @param requestList 请求集合
     * @return 非空请求集合
     * @throws ServiceException 请求为空时抛出
     */
    private List<TmScheduleResult> requireRequests(List<TmScheduleResult> requestList) {
        if (CollUtil.isEmpty(requestList) || requestList.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException(this.resolveTmMessage(
                    "ui.data.alert.tm.schedule.operationFailed", "人工排程操作失败"));
        }
        return requestList;
    }

    /**
     * 根据ID读取当前排程结果。
     *
     * @param id             结果ID
     * @param messageKey     国际化键
     * @param defaultMessage 默认文案
     * @return 当前结果
     * @throws ServiceException 结果不存在时抛出
     */
    private TmScheduleResult loadResultById(Long id, String messageKey, String defaultMessage) {
        TmScheduleResult result = id == null ? null : tmScheduleResultMapper.selectById(id);
        if (result == null) {
            throw new ServiceException(this.resolveTmMessage(messageKey, defaultMessage));
        }
        return result;
    }

    /**
     * 解析操作班次。
     *
     * @param operationResult 操作请求
     * @param oldResult       当前结果
     * @return 操作班次
     */
    private int resolveOperationShift(TmScheduleResult operationResult, TmScheduleResult oldResult) {
        Integer shiftOrder = TmInsertPositionValidator.resolveShiftOrder(operationResult);
        if (shiftOrder == null) {
            shiftOrder = TmInsertPositionValidator.resolveShiftOrder(oldResult);
        }
        return shiftOrder == null ? 1 : shiftOrder;
    }

    /**
     * 解析插单有效班次。
     *
     * @param insertResult 插单请求
     * @return 有效班次列表
     */
    private List<Integer> resolveInsertShiftOrderList(TmScheduleResult insertResult) {
        if (insertResult == null) {
            return Collections.emptyList();
        }
        List<Integer> shiftOrderList = new ArrayList<>();
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.isPositive(this.getShiftPlanQty(insertResult, shiftOrder))
                    && this.getShiftSequence(insertResult, shiftOrder) != null) {
                shiftOrderList.add(shiftOrder);
            }
        }
        return shiftOrderList;
    }

    private List<String> scheduleFieldTemplates() {
        return java.util.Arrays.asList(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE,
                TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE);
    }

    private BigDecimal getShiftPlanQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder)));
    }

    private BigDecimal getShiftFinishQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                String.format(TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder)));
    }

    private Integer getShiftSequence(TmScheduleResult result, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(
                String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return value instanceof Integer ? (Integer) value : null;
    }

    private String getShiftText(TmScheduleResult result, String fieldTemplate, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(fieldTemplate, shiftOrder));
        return value == null ? null : value.toString();
    }

    private Date getShiftDate(TmScheduleResult result, String fieldTemplate, int shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(fieldTemplate, shiftOrder));
        return value instanceof Date ? (Date) value : null;
    }

    private void setShiftValue(TmScheduleResult result, String fieldTemplate, int shiftOrder, Object value) {
        result.setFieldValueByFieldName(String.format(fieldTemplate, shiftOrder), value);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private String resolveTmMessage(String messageKey, String defaultMessage) {
        if (RequestContextHolder.getRequestAttributes() == null) {
            return defaultMessage;
        }
        try {
            String message = I18nUtil.getMessage(messageKey);
            return StringUtils.isBlank(message) || messageKey.equals(message) ? defaultMessage : message;
        } catch (Exception exception) {
            return defaultMessage;
        }
    }
}
