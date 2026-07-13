package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
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
import com.zlt.aps.tm.domain.vo.TmManualRollingTask;
import com.zlt.aps.tm.domain.vo.TmManualRollingWriteResult;
import com.zlt.aps.tm.engine.validator.TmInsertPositionValidator;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工操作局部滚动服务。
 *
 * <p>该服务处理插单、调量和转机台后的同排程日期局部滚动。滚动范围只覆盖指定机台
 * 从操作班次、操作顺位开始到第 6 班，不跨排程日、不自动选择其他机台。</p>
 */
@Slf4j
@Service
public class TmManualInsertRollingService {


    private static final String INSERT_DATA_SOURCE = "INSERT";

    private final TmScheduleResultMapper tmScheduleResultMapper;

    private final TmMachineInfoMapper tmMachineInfoMapper;

    private final TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    /**
     * 构造人工操作局部滚动服务。
     *
     * @param tmScheduleResultMapper    排程结果 Mapper
     * @param tmMachineInfoMapper       胎面机台 Mapper
     * @param tmScheduleUnplannedMapper 未排结果 Mapper
     */
    public TmManualInsertRollingService(TmScheduleResultMapper tmScheduleResultMapper,
                                        TmMachineInfoMapper tmMachineInfoMapper,
                                        TmScheduleUnplannedMapper tmScheduleUnplannedMapper) {
        this.tmScheduleResultMapper = tmScheduleResultMapper;
        this.tmMachineInfoMapper = tmMachineInfoMapper;
        this.tmScheduleUnplannedMapper = tmScheduleUnplannedMapper;
    }

    /**
     * 插入人工插单并滚动更新同机台后续班次。
     *
     * @param insertResult 人工插单排程结果
     * @return 新插单结果写入行数
     * @throws ServiceException 插单班次、顺序或机台产能缺失时抛出
     */
    int insertAndRoll(TmScheduleResult insertResult) {
        Integer startShiftOrder = TmInsertPositionValidator.resolveShiftOrder(insertResult);
        Integer insertSequence = TmInsertPositionValidator.resolveSequence(insertResult, startShiftOrder);
        if (startShiftOrder == null || insertSequence == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertShiftEmpty", "插单班次和顺序不能为空"));
        }
        TmManualRollingTask insertTask = this.buildInsertTask(insertResult, startShiftOrder, insertSequence);
        TmManualRollingWriteResult writeResult = this.rollMachineWindow(insertResult, startShiftOrder, insertSequence,
                this.loadSameMachineResults(insertResult), this.loadMachineCapacity(insertResult), null, false, insertTask);
        log.info("[TM_MANUAL_ROLL] operation=INSERT, factoryCode={}, scheduleDate={}, machineCode={}, startShiftOrder={}, insertCount={}, updateCount={}, unplannedQty={}",
                insertResult.getFactoryCode(), insertResult.getScheduleDate(), insertResult.getMachineCode(), startShiftOrder,
                writeResult.getInsertCount(), writeResult.getUpdateCount(), writeResult.getUnplannedQty());
        return writeResult.getInsertCount();
    }

    /**
     * 调整计划量并滚动更新同机台后续班次。
     *
     * @param changeResult 调量后的排程结果
     * @return 更新行数
     * @throws ServiceException 调量结果不存在或计划量小于完成量时抛出
     */
    int changeQtyAndRoll(TmScheduleResult changeResult) {
        TmScheduleResult oldResult = this.loadResultById(changeResult.getId(), "ui.data.alert.tm.schedule.changeQtyResultNotFound",
                "调量排程结果不存在或已失效");
        Integer shiftOrder = this.resolveOperationShift(changeResult, oldResult);
        Integer sequence = this.getShiftSequence(oldResult, shiftOrder);
        if (sequence == null) {
            sequence = 1;
        }
        BigDecimal changeQty = this.getShiftPlanQty(changeResult, shiftOrder);
        BigDecimal finishQty = this.getShiftFinishQty(oldResult, shiftOrder);
        if (changeQty.compareTo(finishQty) < 0) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.changeQtyLessThanFinish", "调量计划量不能小于已完成量"));
        }
        TmManualRollingTask changeTask = this.buildExistingTask(oldResult, shiftOrder);
        changeTask.setPlanQty(changeQty);

        TmManualRollingWriteResult writeResult = this.rollMachineWindow(oldResult, shiftOrder, sequence,
                this.loadSameMachineResults(oldResult), this.loadMachineCapacity(oldResult), oldResult.getId(), false, changeTask);
        log.info("[TM_MANUAL_ROLL] operation=CHANGE_QTY, factoryCode={}, scheduleDate={}, machineCode={}, startShiftOrder={}, updateCount={}, unplannedQty={}",
                oldResult.getFactoryCode(), oldResult.getScheduleDate(), oldResult.getMachineCode(), shiftOrder,
                writeResult.getUpdateCount(), writeResult.getUnplannedQty());
        return writeResult.getUpdateCount();
    }

    /**
     * 调整机台并滚动更新原机台和目标机台。
     *
     * @param transferResult 转机台后的排程结果
     * @return 更新行数
     * @throws ServiceException 转机台结果不存在时抛出
     */
    int changeMachineAndRoll(TmScheduleResult transferResult) {
        TmScheduleResult oldResult = this.loadResultById(transferResult.getId(), "ui.data.alert.tm.schedule.changeMachineResultNotFound",
                "转机台排程结果不存在或已失效");
        String sourceMachineCode = oldResult.getMachineCode();
        Integer shiftOrder = this.resolveOperationShift(transferResult, oldResult);
        Integer sourceSequence = this.getShiftSequence(oldResult, shiftOrder);
        if (sourceSequence == null) {
            sourceSequence = 1;
        }
        BigDecimal transferPlanQty = this.getShiftPlanQty(oldResult, shiftOrder);

        // 源机台：加载同机台结果，将 oldResult 副本的转出班次计划量置 0，使该班次任务在滚动中不占产能、不被重新装箱，其它班次任务保留并重排
        List<TmScheduleResult> sourceExistList = this.loadSameMachineResults(oldResult);
        TmScheduleResult sourceOldResultCopy = null;
        for (TmScheduleResult item : sourceExistList) {
            if (item.getId() != null && item.getId().equals(oldResult.getId())) {
                sourceOldResultCopy = item;
                break;
            }
        }
        if (sourceOldResultCopy != null) {
            this.setShiftPlanQty(sourceOldResultCopy, shiftOrder, BigDecimal.ZERO);
        }

        TmManualRollingWriteResult totalResult = new TmManualRollingWriteResult();
        // 源机台滚动：不排除 oldResult（replaceResultId=null），其转出班次任务量为 0 不占产能，其它班次任务保留重排
        totalResult.add(this.rollMachineWindow(oldResult, shiftOrder, sourceSequence,
                sourceExistList, this.loadMachineCapacity(oldResult), null, false));
        // 单班次场景下 oldResult 副本无其它班次任务，不会被滚动 updateResultMap 收录，需显式落库以清空转出班次
        if (sourceOldResultCopy != null) {
            this.markEditedReleaseStatus(sourceOldResultCopy);
            if (tmScheduleResultMapper.updateById(sourceOldResultCopy) != 1) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationConcurrentChanged",
                        "排程状态已变化，请刷新后重试"));
            }
        }

        // 目标机台按当前结果粒度查找同胎面，存在时合并到目标记录，避免产生重复结果行。
        TmScheduleResult moveTemplate = this.copyBaseResult(oldResult);
        moveTemplate.setMachineCode(transferResult.getMachineCode());
        List<TmScheduleResult> targetExistList = this.loadSameMachineResults(moveTemplate);
        TmScheduleResult targetSameTreadResult = targetExistList.stream()
                .filter(item -> Objects.equals(item.getBatchNo(), oldResult.getBatchNo()))
                .filter(item -> Objects.equals(item.getTreadCode(), oldResult.getTreadCode()))
                .findFirst().orElse(null);
        if (targetSameTreadResult != null) {
            Integer targetSequence = this.getShiftSequence(targetSameTreadResult, shiftOrder);
            if (targetSequence == null) {
                targetSequence = this.resolveNextTargetSequence(targetExistList, shiftOrder);
            }
            // 同胎面合并沿用目标机台既有序号，避免改变目标机台当前顺序；后续业务口径变化时在此处调整。
            BigDecimal mergedPlanQty = BigDecimalUtils.add(this.getShiftPlanQty(targetSameTreadResult, shiftOrder),
                    transferPlanQty);
            this.setShiftSequence(targetSameTreadResult, shiftOrder, targetSequence);
            this.setShiftPlanQty(targetSameTreadResult, shiftOrder, mergedPlanQty);
            totalResult.add(this.rollMachineWindow(moveTemplate, shiftOrder, targetSequence,
                    targetExistList, this.loadMachineCapacity(moveTemplate), null, true));
        } else {
            // 目标不存在同胎面时保留原插入和滚动口径，新行只承载转出班次，避免跨班次污染。
            Integer targetSequence = TmInsertPositionValidator.resolveSequence(transferResult, shiftOrder);
            if (targetSequence == null) {
                targetSequence = sourceSequence;
            }
            this.setShiftPlanQty(moveTemplate, shiftOrder, transferPlanQty);
            this.setShiftSequence(moveTemplate, shiftOrder, targetSequence);
            TmManualRollingTask transferTask = this.buildInsertTask(moveTemplate, shiftOrder, targetSequence);
            totalResult.add(this.rollMachineWindow(moveTemplate, shiftOrder, targetSequence,
                    targetExistList, this.loadMachineCapacity(moveTemplate), null, false, transferTask));
        }
        log.info("[TM_MANUAL_ROLL] operation=CHANGE_MACHINE, factoryCode={}, scheduleDate={}, sourceMachineCode={}, targetMachineCode={}, startShiftOrder={}, updateCount={}, unplannedQty={}",
                oldResult.getFactoryCode(), oldResult.getScheduleDate(), sourceMachineCode, transferResult.getMachineCode(),
                shiftOrder, totalResult.getUpdateCount(), totalResult.getUnplannedQty());
        return totalResult.getUpdateCount();
    }

    /**
     * 查询同工厂、同排程日期、同机台排程结果。
     *
     * @param queryResult 查询条件
     * @return 同机台排程结果
     */
    private List<TmScheduleResult> loadSameMachineResults(TmScheduleResult queryResult) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, queryResult.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, queryResult.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(queryResult.getBatchNo()), TmScheduleResult::getBatchNo, queryResult.getBatchNo());
        wrapper.eq(TmScheduleResult::getMachineCode, queryResult.getMachineCode());
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 计算目标机台操作班次的下一个可用序号。
     *
     * <p>目标同胎面在操作班次尚无序号时，只根据目标机台当前窗口计算，禁止使用源机台序号。</p>
     *
     * @param targetResultList 目标机台当前结果
     * @param shiftOrder       操作班次
     * @return 目标机台下一可用序号
     */
    private Integer resolveNextTargetSequence(List<TmScheduleResult> targetResultList, int shiftOrder) {
        return targetResultList.stream()
                .map(result -> this.getShiftSequence(result, shiftOrder))
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .map(sequence -> sequence + 1)
                .orElse(1);
    }

    /**
     * 根据 ID 查询排程结果。
     *
     * @param id             排程结果 ID
     * @param messageKey     国际化 key
     * @param defaultMessage 默认提示
     * @return 排程结果
     * @throws ServiceException 结果不存在时抛出
     */
    private TmScheduleResult loadResultById(Long id, String messageKey, String defaultMessage) {
        TmScheduleResult oldResult = tmScheduleResultMapper.selectById(id);
        if (oldResult == null) {
            throw new ServiceException(this.resolveTmMessage(messageKey, defaultMessage));
        }
        return oldResult;
    }

    /**
     * 查询机台最大班产。
     *
     * @param queryResult 查询条件
     * @return 最大班产
     * @throws ServiceException 未维护最大班产时抛出
     */
    private BigDecimal loadMachineCapacity(TmScheduleResult queryResult) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, queryResult.getFactoryCode());
        wrapper.eq(TmMachineInfo::getMachineCode, queryResult.getMachineCode());
        List<TmMachineInfo> machineInfoList = tmMachineInfoMapper.selectList(wrapper);
        if (CollUtil.isEmpty(machineInfoList) || !this.isPositive(machineInfoList.get(0).getMaxCapacity())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertMachineCapacityEmpty", "机台最大班产未维护"));
        }
        return machineInfoList.get(0).getMaxCapacity();
    }

    /**
     * 滚动单个机台从指定班次和顺序开始的局部窗口。
     *
     * @param baseResult      操作基础结果
     * @param startShiftOrder 起始班次
     * @param startSequence   起始顺序
     * @param existResultList 当前机台已有结果
     * @param machineCapacity 机台班产
     * @param replaceResultId 需要从原任务流排除的结果 ID
     * @param preserveStartSequence 是否保留起始任务在操作班次的原数值序号
     * @param extraTaskList   需要插入窗口起点的任务
     * @return 写入结果
     */
    private TmManualRollingWriteResult rollMachineWindow(TmScheduleResult baseResult, int startShiftOrder, int startSequence,
                                                         List<TmScheduleResult> existResultList, BigDecimal machineCapacity,
                                                         Long replaceResultId, boolean preserveStartSequence,
                                                         TmManualRollingTask... extraTaskList) {
        this.validateEditableResults(existResultList);
        Map<Long, TmScheduleResult> updateResultMap = new LinkedHashMap<>();
        List<TmScheduleResult> insertResultList = new ArrayList<>();
        LinkedList<TmManualRollingTask> rollingTaskQueue = this.buildRollingTaskQueue(existResultList, startShiftOrder,
                startSequence, replaceResultId, extraTaskList);
        List<TmManualRollingTask> startPrefixTaskList = this.buildPrefixTaskList(existResultList, startShiftOrder, startSequence,
                replaceResultId);

        this.clearAffectedShiftFields(existResultList, startShiftOrder);
        for (int shiftOrder = startShiftOrder; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            this.mergeCarryTaskToNextSameTask(rollingTaskQueue);
            BigDecimal remainCapacity = machineCapacity;
            int sequence = 1;
            if (shiftOrder == startShiftOrder) {
                for (TmManualRollingTask prefixTask : startPrefixTaskList) {
                    this.applyTaskToResult(prefixTask, shiftOrder, sequence, prefixTask.getPlanQty(), updateResultMap, insertResultList);
                    remainCapacity = BigDecimalUtils.sub(remainCapacity, prefixTask.getPlanQty());
                    sequence++;
                }
                if (preserveStartSequence && sequence < startSequence) {
                    // 目标已有同胎面时保留其原数值序号，允许目标窗口原有序号空洞继续存在。
                    sequence = startSequence;
                }
            }
            this.fillShiftByRollingQueue(baseResult, rollingTaskQueue, shiftOrder, sequence, remainCapacity,
                    updateResultMap, insertResultList);
        }
        return this.persistRollingResult(baseResult, rollingTaskQueue, updateResultMap, insertResultList);
    }

    /**
     * 构建窗口起点之后的任务流。
     *
     * @param resultList      当前机台结果
     * @param startShiftOrder 起始班次
     * @param startSequence   起始顺序
     * @param replaceResultId 需要排除的结果 ID
     * @param extraTaskList   额外插入任务
     * @return 任务流队列
     */
    private LinkedList<TmManualRollingTask> buildRollingTaskQueue(List<TmScheduleResult> resultList, int startShiftOrder,
                                                                  int startSequence, Long replaceResultId,
                                                                  TmManualRollingTask... extraTaskList) {
        List<TmManualRollingTask> rollingTaskList = new ArrayList<>();
        if (extraTaskList != null) {
            for (TmManualRollingTask extraTask : extraTaskList) {
                if (extraTask != null) {
                    rollingTaskList.add(extraTask);
                }
            }
        }
        for (int shiftOrder = startShiftOrder; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            List<TmManualRollingTask> shiftTaskList = this.buildShiftTaskList(resultList, shiftOrder, replaceResultId);
            for (TmManualRollingTask task : shiftTaskList) {
                if (shiftOrder == startShiftOrder && this.defaultSequence(task.getSequence()) < startSequence) {
                    continue;
                }
                rollingTaskList.add(task);
            }
        }
        return rollingTaskList.stream()
                .filter(task -> this.isPositive(task.getPlanQty()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * 构建起始班次插入点之前的前置任务。
     *
     * @param resultList      当前机台结果
     * @param startShiftOrder 起始班次
     * @param startSequence   起始顺序
     * @param replaceResultId 需要排除的结果 ID
     * @return 前置任务列表
     */
    private List<TmManualRollingTask> buildPrefixTaskList(List<TmScheduleResult> resultList, int startShiftOrder,
                                                          int startSequence, Long replaceResultId) {
        return this.buildShiftTaskList(resultList, startShiftOrder, replaceResultId).stream()
                .filter(task -> this.defaultSequence(task.getSequence()) < startSequence)
                .collect(Collectors.toList());
    }

    /**
     * 拆分单个班次的排程结果为任务列表。
     *
     * @param resultList      排程结果
     * @param shiftOrder      班次
     * @param replaceResultId 需要排除的结果 ID
     * @return 任务列表
     */
    private List<TmManualRollingTask> buildShiftTaskList(List<TmScheduleResult> resultList, int shiftOrder, Long replaceResultId) {
        return resultList.stream()
                .filter(result -> replaceResultId == null || !replaceResultId.equals(result.getId()))
                .map(result -> this.buildExistingTask(result, shiftOrder))
                .filter(task -> this.isPositive(task.getPlanQty()) || task.getSequence() != null)
                .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                .collect(Collectors.toList());
    }

    /**
     * 当前班次按剩余产能承接任务流。
     *
     * @param baseResult       操作基础结果
     * @param rollingTaskQueue 滚动任务队列
     * @param shiftOrder       当前班次
     * @param startSequence    当前班次起始顺序
     * @param remainCapacity   当前班次剩余产能
     * @param updateResultMap  待更新结果
     * @param insertResultList 待新增结果
     */
    private void fillShiftByRollingQueue(TmScheduleResult baseResult, LinkedList<TmManualRollingTask> rollingTaskQueue,
                                         int shiftOrder, int startSequence, BigDecimal remainCapacity,
                                         Map<Long, TmScheduleResult> updateResultMap, List<TmScheduleResult> insertResultList) {
        int sequence = startSequence;
        while (!rollingTaskQueue.isEmpty()) {
            TmManualRollingTask currentTask = rollingTaskQueue.removeFirst();
            BigDecimal assignedQty = this.resolveAssignedQty(currentTask, remainCapacity);
            BigDecimal overflowQty = BigDecimalUtils.sub(currentTask.getPlanQty(), assignedQty);
            if (this.isPositive(assignedQty)) {
                TmScheduleResult targetResult = this.applyTaskToResult(currentTask, shiftOrder, sequence, assignedQty,
                        updateResultMap, insertResultList);
                currentTask.setSourceResult(targetResult);
                remainCapacity = BigDecimalUtils.sub(remainCapacity, assignedQty);
                sequence++;
            }
            if (this.isPositive(overflowQty)) {
                TmManualRollingTask carryTask = this.buildCarryTask(currentTask, overflowQty);
                rollingTaskQueue.addFirst(carryTask);
                break;
            }
            if (!this.isPositive(remainCapacity)) {
                break;
            }
        }
    }

    /**
     * 下一班开始前，将上一班溢出的同物料任务合并到后续已有任务。
     *
     * @param rollingTaskQueue 滚动任务队列
     */
    private void mergeCarryTaskToNextSameTask(LinkedList<TmManualRollingTask> rollingTaskQueue) {
        if (rollingTaskQueue.size() < 2 || !rollingTaskQueue.getFirst().isCarryoverTask()) {
            return;
        }
        TmManualRollingTask carryTask = rollingTaskQueue.getFirst();
        TmManualRollingTask nextTask = rollingTaskQueue.get(1);
        if (!this.isSameMaterial(carryTask, nextTask)) {
            return;
        }
        nextTask.setPlanQty(BigDecimalUtils.add(nextTask.getPlanQty(), carryTask.getPlanQty()));
        rollingTaskQueue.removeFirst();
    }

    /**
     * 持久化滚动结果。
     *
     * @param baseResult       操作基础结果
     * @param remainTaskQueue  第 6 班后剩余任务
     * @param updateResultMap  待更新结果
     * @param insertResultList 待新增结果
     * @return 写入结果
     */
    private TmManualRollingWriteResult persistRollingResult(TmScheduleResult baseResult, LinkedList<TmManualRollingTask> remainTaskQueue,
                                                            Map<Long, TmScheduleResult> updateResultMap,
                                                            List<TmScheduleResult> insertResultList) {
        TmManualRollingWriteResult writeResult = new TmManualRollingWriteResult();
        for (TmManualRollingTask remainTask : remainTaskQueue) {
            if (this.isPositive(remainTask.getPlanQty())) {
                this.insertUnplanned(baseResult, remainTask, remainTask.getPlanQty());
                writeResult.setUnplannedCount(writeResult.getUnplannedCount() + 1);
                writeResult.setUnplannedQty(BigDecimalUtils.add(writeResult.getUnplannedQty(), remainTask.getPlanQty()));
            }
        }
        for (TmScheduleResult newResult : insertResultList) {
            writeResult.setInsertCount(writeResult.getInsertCount() + tmScheduleResultMapper.insert(newResult));
        }
        for (TmScheduleResult updateResult : updateResultMap.values()) {
            int updatedRows = tmScheduleResultMapper.updateById(updateResult);
            if (updatedRows != 1) {
                throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.operationConcurrentChanged",
                        "排程状态已变化，请刷新后重试"));
            }
            writeResult.setUpdateCount(writeResult.getUpdateCount() + updatedRows);
        }
        return writeResult;
    }

    /**
     * 校验滚动窗口内全部既有结果均允许人工编辑。
     *
     * @param resultList 滚动窗口结果
     * @throws ServiceException 任一记录处于发布中、发布失败或超时失败状态时抛出
     */
    private void validateEditableResults(List<TmScheduleResult> resultList) {
        boolean containsNonEditableResult = resultList.stream()
                .anyMatch(result -> !TmReleaseStatusTransition.isEditable(result.getReleaseStatus()));
        if (containsNonEditableResult) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
    }

    /**
     * 构造既有任务。
     *
     * @param result     排程结果
     * @param shiftOrder 班次
     * @return 人工滚动任务
     */
    private TmManualRollingTask buildExistingTask(TmScheduleResult result, int shiftOrder) {
        TmManualRollingTask task = new TmManualRollingTask();
        task.setResultId(result.getId());
        task.setSourceResult(result);
        task.setTemplateResult(result);
        task.setShiftOrder(shiftOrder);
        task.setSequence(this.getShiftSequence(result, shiftOrder));
        task.setPlanQty(this.getShiftPlanQty(result, shiftOrder));
        task.setFinishQty(this.getShiftFinishQty(result, shiftOrder));
        task.setMachineCode(result.getMachineCode());
        task.setTreadCode(result.getTreadCode());
        task.setGlueCode(result.getGlueCode());
        task.setMouthPlateCode(result.getMouthPlateCode());
        task.setDataSource(result.getDataSource());
        return task;
    }

    /**
     * 构造插单任务。
     *
     * @param insertResult    插单排程结果
     * @param startShiftOrder 插单班次
     * @param insertSequence  插单顺序
     * @return 插单任务
     */
    private TmManualRollingTask buildInsertTask(TmScheduleResult insertResult, int startShiftOrder, int insertSequence) {
        TmManualRollingTask task = new TmManualRollingTask();
        task.setTemplateResult(insertResult);
        task.setShiftOrder(startShiftOrder);
        task.setSequence(insertSequence);
        task.setPlanQty(this.getShiftPlanQty(insertResult, startShiftOrder));
        task.setFinishQty(BigDecimal.ZERO);
        task.setMachineCode(insertResult.getMachineCode());
        task.setTreadCode(insertResult.getTreadCode());
        task.setGlueCode(insertResult.getGlueCode());
        task.setMouthPlateCode(insertResult.getMouthPlateCode());
        task.setDataSource(INSERT_DATA_SOURCE);
        task.setInsertTask(true);
        return task;
    }

    /**
     * 清空受影响班次的可变字段。
     *
     * @param resultList      排程结果列表
     * @param startShiftOrder 插单开始班次
     */
    private void clearAffectedShiftFields(List<TmScheduleResult> resultList, int startShiftOrder) {
        for (TmScheduleResult result : resultList) {
            this.clearAffectedShiftFields(result, startShiftOrder);
        }
    }

    /**
     * 清空单条结果受影响班次的可变字段。
     *
     * @param result          排程结果
     * @param startShiftOrder 开始班次
     */
    private void clearAffectedShiftFields(TmScheduleResult result, int startShiftOrder) {
        for (int shiftOrder = startShiftOrder; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            this.setShiftSequence(result, shiftOrder, null);
            this.setShiftPlanQty(result, shiftOrder, null);
            result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_START_TIME_FIELD_TEMPLATE, shiftOrder), null);
            result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_END_TIME_FIELD_TEMPLATE, shiftOrder), null);
        }
    }

    /**
     * 计算当前班次承接量，保证已有完成量不被压低。
     *
     * @param task           当前任务
     * @param remainCapacity 当前班次剩余产能
     * @return 当前班次承接量
     */
    private BigDecimal resolveAssignedQty(TmManualRollingTask task, BigDecimal remainCapacity) {
        BigDecimal availableQty = this.isPositive(remainCapacity) ? remainCapacity : BigDecimal.ZERO;
        BigDecimal planQty = BigDecimalUtils.valueOf(task.getPlanQty());
        BigDecimal finishQty = BigDecimalUtils.valueOf(task.getFinishQty());
        if (planQty.compareTo(availableQty) <= 0) {
            return planQty;
        }
        if (finishQty.compareTo(BigDecimal.ZERO) > 0) {
            return finishQty.min(planQty).max(availableQty);
        }
        return availableQty;
    }

    /**
     * 将任务分配量写入目标排程结果。
     *
     * @param task             当前任务
     * @param shiftOrder       当前班次
     * @param sequence         当前顺序
     * @param assignedQty      承接量
     * @param updateResultMap  待更新结果
     * @param insertResultList 待新增结果
     * @return 被写入的排程结果
     */
    private TmScheduleResult applyTaskToResult(TmManualRollingTask task, int shiftOrder, int sequence, BigDecimal assignedQty,
                                               Map<Long, TmScheduleResult> updateResultMap, List<TmScheduleResult> insertResultList) {
        TmScheduleResult targetResult = task.getSourceResult();
        if (targetResult == null) {
            targetResult = this.copyBaseResult(task.getTemplateResult());
            targetResult.setDataSource(INSERT_DATA_SOURCE);
            targetResult.setReleaseStatus(ApsConstant.NO_RELEASE);
            insertResultList.add(targetResult);
        } else {
            this.markEditedReleaseStatus(targetResult);
            updateResultMap.put(targetResult.getId(), targetResult);
        }
        this.setShiftSequence(targetResult, shiftOrder, sequence);
        this.setShiftPlanQty(targetResult, shiftOrder, assignedQty);
        return targetResult;
    }

    /**
     * 按发布状态迁移矩阵处理人工编辑后的已发布记录。
     *
     * <p>保持原业务口径：仅已发布记录回退为待发布，其他可编辑状态保持不变。</p>
     *
     * @param scheduleResult 被人工编辑的排程结果
     * @throws ServiceException 状态迁移不合法时抛出
     */
    private void markEditedReleaseStatus(TmScheduleResult scheduleResult) {
        if (!TmReleaseStatusTransition.isEditable(scheduleResult.getReleaseStatus())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.illegalReleaseTransition",
                    "排程发布状态迁移不合法"));
        }
        if (!ApsConstant.IS_RELEASE.equals(scheduleResult.getReleaseStatus())) {
            return;
        }
        if (!TmReleaseStatusTransition.canTransit(scheduleResult.getReleaseStatus(), ApsConstant.WAIT_RELEASING)) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.illegalReleaseTransition", "排程发布状态迁移不合法"));
        }
        scheduleResult.setReleaseStatus(ApsConstant.WAIT_RELEASING);
    }

    /**
     * 构造跨班顺延任务。
     *
     * @param sourceTask  来源任务
     * @param overflowQty 溢出数量
     * @return 顺延任务
     */
    private TmManualRollingTask buildCarryTask(TmManualRollingTask sourceTask, BigDecimal overflowQty) {
        TmManualRollingTask carryTask = new TmManualRollingTask();
        carryTask.setSourceResult(sourceTask.getSourceResult());
        carryTask.setTemplateResult(sourceTask.getTemplateResult());
        carryTask.setShiftOrder(sourceTask.getShiftOrder() + 1);
        carryTask.setPlanQty(overflowQty);
        carryTask.setFinishQty(BigDecimal.ZERO);
        carryTask.setMachineCode(sourceTask.getMachineCode());
        carryTask.setTreadCode(sourceTask.getTreadCode());
        carryTask.setGlueCode(sourceTask.getGlueCode());
        carryTask.setMouthPlateCode(sourceTask.getMouthPlateCode());
        carryTask.setDataSource(sourceTask.getDataSource());
        carryTask.setInsertTask(sourceTask.isInsertTask());
        carryTask.setCarryoverTask(true);
        return carryTask;
    }

    /**
     * 写入第 6 班后仍无法容纳的未排量。
     *
     * @param baseResult   操作基础结果
     * @param sourceTask   来源任务
     * @param unplannedQty 未排数量
     */
    private void insertUnplanned(TmScheduleResult baseResult, TmManualRollingTask sourceTask, BigDecimal unplannedQty) {
        TmUnplannedReasonEnum reason = TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
        TmScheduleUnplanned unplanned = new TmScheduleUnplanned();
        unplanned.setFactoryCode(baseResult.getFactoryCode());
        unplanned.setBatchNo(baseResult.getBatchNo());
        unplanned.setScheduleDate(baseResult.getScheduleDate());
        unplanned.setTreadCode(sourceTask.getTreadCode());
        unplanned.setGlueCode(sourceTask.getGlueCode());
        unplanned.setMouthPlateCode(sourceTask.getMouthPlateCode());
        unplanned.setUnplannedReasonCode(reason.getCode());
        unplanned.setUnplannedReasonDesc(reason.getDesc());
        unplanned.setUnplannedEvidenceJson(String.format("{\"source\":\"MANUAL_ROLL\",\"machineCode\":\"%s\",\"unplannedQty\":%s}",
                baseResult.getMachineCode(), unplannedQty.stripTrailingZeros().toPlainString()));
        tmScheduleUnplannedMapper.insert(unplanned);
    }

    /**
     * 复制排程结果基础字段用于新增插单或顺延结果。
     *
     * @param templateResult 模板结果
     * @return 新排程结果
     */
    private TmScheduleResult copyBaseResult(TmScheduleResult templateResult) {
        TmScheduleResult result = new TmScheduleResult();
        result.setFactoryCode(templateResult.getFactoryCode());
        result.setBatchNo(templateResult.getBatchNo());
        result.setOrderNo(templateResult.getOrderNo());
        result.setScheduleDate(templateResult.getScheduleDate());
        result.setMachineCode(templateResult.getMachineCode());
        result.setTreadCode(templateResult.getTreadCode());
        result.setGlueCode(templateResult.getGlueCode());
        result.setBaseGlueCode(templateResult.getBaseGlueCode());
        result.setWholeGlueCode(templateResult.getWholeGlueCode());
        result.setGlueSeq(templateResult.getGlueSeq());
        result.setMouthPlateCode(templateResult.getMouthPlateCode());
        result.setTailFlag(templateResult.getTailFlag());
        return result;
    }

    /**
     * 解析人工操作班次。
     *
     * @param operationResult 操作入参
     * @param oldResult       原排程结果
     * @return 操作班次
     */
    private Integer resolveOperationShift(TmScheduleResult operationResult, TmScheduleResult oldResult) {
        return Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(operationResult))
                .orElseGet(() -> Optional.ofNullable(TmInsertPositionValidator.resolveShiftOrder(oldResult)).orElse(1));
    }

    /**
     * 判断两个任务是否为同胎面、同主胶、同口型。
     *
     * @param firstTask  第一个任务
     * @param secondTask 第二个任务
     * @return true 表示可合并
     */
    private boolean isSameMaterial(TmManualRollingTask firstTask, TmManualRollingTask secondTask) {
        return Objects.equals(firstTask.getTreadCode(), secondTask.getTreadCode())
                && Objects.equals(firstTask.getGlueCode(), secondTask.getGlueCode())
                && Objects.equals(firstTask.getMouthPlateCode(), secondTask.getMouthPlateCode());
    }

    /**
     * 读取班次计划量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 班次计划量
     */
    private BigDecimal getShiftPlanQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder)));
    }

    /**
     * 设置班次计划量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @param planQty    计划量
     */
    private void setShiftPlanQty(TmScheduleResult result, int shiftOrder, BigDecimal planQty) {
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder), planQty);
    }

    /**
     * 读取班次完成量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 完成量
     */
    private BigDecimal getShiftFinishQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder)));
    }

    /**
     * 读取班次顺序。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 班次顺序
     */
    private Integer getShiftSequence(TmScheduleResult result, int shiftOrder) {
        Object sequence = result.getFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
        return sequence instanceof Integer ? (Integer) sequence : null;
    }

    /**
     * 设置班次顺序。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @param sequence   班次顺序
     */
    private void setShiftSequence(TmScheduleResult result, int shiftOrder, Integer sequence) {
        result.setFieldValueByFieldName(String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder), sequence);
    }

    /**
     * 空顺序按最大值排序。
     *
     * @param sequence 班内顺序
     * @return 排序顺序
     */
    private Integer defaultSequence(Integer sequence) {
        return sequence == null ? Integer.MAX_VALUE : sequence;
    }

    /**
     * 判断数量是否大于 0。
     *
     * @param qty 数量
     * @return true 表示大于 0
     */
    private boolean isPositive(BigDecimal qty) {
        return qty != null && qty.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 读取胎面排程国际化提示，未命中时回退默认文案。
     *
     * @param messageKey     国际化 key
     * @param defaultMessage 默认提示
     * @return 当前语言环境下的提示文案
     */
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