package com.zlt.aps.tm.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.enums.TmUnplannedReasonEnum;
import com.zlt.aps.tm.domain.vo.TmManualRollingTask;
import com.zlt.aps.tm.engine.validator.TmInsertPositionValidator;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工插单局部滚动服务。
 *
 * <p>该服务只处理同工厂、同排程日期、同机台，从插单班次到第 6 班的局部窗口。
 * 数据库读取、横向字段回写、发布状态回退和未排写入保留在 aps-tm 业务模块内。</p>
 */
@Slf4j
@Service
public class TmManualInsertRollingService {

    private static final int MAX_SHIFT_ORDER = 6;

    private static final String INSERT_DATA_SOURCE = "INSERT";

    private final TmScheduleResultMapper tmScheduleResultMapper;

    private final TmMachineInfoMapper tmMachineInfoMapper;

    private final TmScheduleUnplannedMapper tmScheduleUnplannedMapper;

    /**
     * 构造人工插单局部滚动服务。
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
    @Transactional(rollbackFor = Exception.class)
    public int insertAndRoll(TmScheduleResult insertResult) {
        Integer startShiftOrder = TmInsertPositionValidator.resolveShiftOrder(insertResult);
        Integer insertSequence = TmInsertPositionValidator.resolveSequence(insertResult, startShiftOrder);
        if (startShiftOrder == null || insertSequence == null) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertShiftEmpty", "插单班次和顺序不能为空"));
        }

        BigDecimal machineCapacity = this.loadMachineCapacity(insertResult);
        List<TmScheduleResult> existResultList = this.loadSameMachineResults(insertResult);
        List<TmManualRollingTask> allTaskList = this.buildExistingTaskList(existResultList, startShiftOrder);
        TmManualRollingTask insertTask = this.buildInsertTask(insertResult, startShiftOrder, insertSequence);

        Map<Long, TmScheduleResult> updateResultMap = new LinkedHashMap<>();
        Set<TmScheduleResult> insertResultSet = new LinkedHashSet<>();
        List<TmManualRollingTask> carryTaskList = new ArrayList<>();
        BigDecimal unplannedQty = BigDecimal.ZERO;
        int affectedCount = 0;

        this.clearAffectedShiftFields(existResultList, startShiftOrder);
        for (int shiftOrder = startShiftOrder; shiftOrder <= MAX_SHIFT_ORDER; shiftOrder++) {
            List<TmManualRollingTask> currentTaskList = this.resolveCurrentShiftTasks(allTaskList, insertTask, carryTaskList,
                    shiftOrder, startShiftOrder, insertSequence);
            carryTaskList = new ArrayList<>();
            BigDecimal remainCapacity = machineCapacity;
            int sequence = 1;
            for (TmManualRollingTask currentTask : currentTaskList) {
                currentTask.setSequence(sequence);
                BigDecimal assignedQty = this.resolveAssignedQty(currentTask, remainCapacity);
                BigDecimal overflowQty = BigDecimalUtils.sub(currentTask.getPlanQty(), assignedQty);
                if (this.isPositive(assignedQty)) {
                    TmScheduleResult targetResult = this.applyTaskToResult(currentTask, shiftOrder, sequence, assignedQty,
                            updateResultMap, insertResultSet);
                    if (targetResult != null) {
                        affectedCount++;
                    }
                    remainCapacity = BigDecimalUtils.sub(remainCapacity, assignedQty);
                }
                if (this.isPositive(overflowQty)) {
                    if (shiftOrder < MAX_SHIFT_ORDER) {
                        carryTaskList.add(this.buildCarryTask(currentTask, overflowQty));
                    } else {
                        unplannedQty = BigDecimalUtils.add(unplannedQty, overflowQty);
                        this.insertUnplanned(insertResult, currentTask, overflowQty);
                    }
                }
                sequence++;
            }
        }

        int insertCount = 0;
        for (TmScheduleResult newResult : insertResultSet) {
            insertCount += tmScheduleResultMapper.insert(newResult);
        }
        for (TmScheduleResult updateResult : updateResultMap.values()) {
            tmScheduleResultMapper.updateById(updateResult);
        }

        log.info("[TM_MANUAL_INSERT_ROLL] factoryCode={}, scheduleDate={}, machineCode={}, startShiftOrder={}, affectedCount={}, unplannedQty={}",
                insertResult.getFactoryCode(), insertResult.getScheduleDate(), insertResult.getMachineCode(), startShiftOrder,
                affectedCount, unplannedQty);
        return insertCount;
    }

    /**
     * 查询同工厂、同排程日期、同机台排程结果。
     *
     * @param insertResult 插单结果
     * @return 同机台排程结果
     */
    private List<TmScheduleResult> loadSameMachineResults(TmScheduleResult insertResult) {
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, insertResult.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, insertResult.getScheduleDate());
        wrapper.eq(TmScheduleResult::getMachineCode, insertResult.getMachineCode());
        return tmScheduleResultMapper.selectList(wrapper);
    }

    /**
     * 查询机台最大班产。
     *
     * @param insertResult 插单结果
     * @return 最大班产
     * @throws ServiceException 未维护最大班产时抛出
     */
    private BigDecimal loadMachineCapacity(TmScheduleResult insertResult) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, insertResult.getFactoryCode());
        wrapper.eq(TmMachineInfo::getMachineCode, insertResult.getMachineCode());
        List<TmMachineInfo> machineInfoList = tmMachineInfoMapper.selectList(wrapper);
        if (CollUtil.isEmpty(machineInfoList) || !this.isPositive(machineInfoList.get(0).getMaxCapacity())) {
            throw new ServiceException(this.resolveTmMessage("ui.data.alert.tm.schedule.insertMachineCapacityEmpty", "插单机台最大班产未维护"));
        }
        return machineInfoList.get(0).getMaxCapacity();
    }

    /**
     * 将排程结果横向字段拆分为任务级列表。
     *
     * @param resultList      排程结果列表
     * @param startShiftOrder 插单开始班次
     * @return 任务级列表
     */
    private List<TmManualRollingTask> buildExistingTaskList(List<TmScheduleResult> resultList, int startShiftOrder) {
        List<TmManualRollingTask> taskList = new ArrayList<>();
        for (TmScheduleResult result : resultList) {
            for (int shiftOrder = startShiftOrder; shiftOrder <= MAX_SHIFT_ORDER; shiftOrder++) {
                BigDecimal planQty = this.getShiftPlanQty(result, shiftOrder);
                Integer sequence = this.getShiftSequence(result, shiftOrder);
                if (!this.isPositive(planQty) && sequence == null) {
                    continue;
                }
                TmManualRollingTask task = new TmManualRollingTask();
                task.setResultId(result.getId());
                task.setSourceResult(result);
                task.setTemplateResult(result);
                task.setShiftOrder(shiftOrder);
                task.setSequence(sequence);
                task.setPlanQty(planQty);
                task.setFinishQty(this.getShiftFinishQty(result, shiftOrder));
                task.setMachineCode(result.getMachineCode());
                task.setTreadCode(result.getTreadCode());
                task.setGlueCode(result.getGlueCode());
                task.setMouthPlateCode(result.getMouthPlateCode());
                task.setDataSource(result.getDataSource());
                taskList.add(task);
            }
        }
        return taskList;
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
            for (int shiftOrder = startShiftOrder; shiftOrder <= MAX_SHIFT_ORDER; shiftOrder++) {
                this.setShiftSequence(result, shiftOrder, null);
                this.setShiftPlanQty(result, shiftOrder, null);
                result.setFieldValueByFieldName(String.format("class%dStartTime", shiftOrder), null);
                result.setFieldValueByFieldName(String.format("class%dEndTime", shiftOrder), null);
            }
        }
    }

    /**
     * 获取当前班次待滚动任务，并将上一班溢出优先合并到同胎面任务。
     *
     * @param allTaskList     全部任务
     * @param insertTask      插单任务
     * @param carryTaskList   上一班溢出任务
     * @param shiftOrder      当前班次
     * @param startShiftOrder 插单开始班次
     * @param insertSequence  插单顺序
     * @return 当前班次任务列表
     */
    private List<TmManualRollingTask> resolveCurrentShiftTasks(List<TmManualRollingTask> allTaskList, TmManualRollingTask insertTask,
                                                               List<TmManualRollingTask> carryTaskList, int shiftOrder,
                                                               int startShiftOrder, int insertSequence) {
        List<TmManualRollingTask> shiftTaskList = allTaskList.stream()
                .filter(task -> Integer.valueOf(shiftOrder).equals(task.getShiftOrder()))
                .sorted(Comparator.comparing(task -> this.defaultSequence(task.getSequence())))
                .collect(Collectors.toList());
        if (shiftOrder == startShiftOrder) {
            List<TmManualRollingTask> resultList = new ArrayList<>();
            resultList.addAll(shiftTaskList.stream()
                    .filter(task -> this.defaultSequence(task.getSequence()) < insertSequence)
                    .collect(Collectors.toList()));
            resultList.add(insertTask);
            resultList.addAll(shiftTaskList.stream()
                    .filter(task -> this.defaultSequence(task.getSequence()) >= insertSequence)
                    .collect(Collectors.toList()));
            return resultList;
        }

        List<TmManualRollingTask> resultList = new ArrayList<>();
        List<TmManualRollingTask> notMergedCarryTaskList = new ArrayList<>();
        for (TmManualRollingTask carryTask : carryTaskList) {
            TmManualRollingTask mergeTarget = this.findMergeTarget(shiftTaskList, carryTask);
            if (mergeTarget == null) {
                notMergedCarryTaskList.add(carryTask);
            } else {
                mergeTarget.setPlanQty(BigDecimalUtils.add(mergeTarget.getPlanQty(), carryTask.getPlanQty()));
            }
        }
        resultList.addAll(notMergedCarryTaskList);
        resultList.addAll(shiftTaskList);
        return resultList;
    }

    /**
     * 查找可合并的同胎面、同主胶、同口型任务。
     *
     * @param shiftTaskList 当前班次已有任务
     * @param carryTask     溢出任务
     * @return 可合并任务，未找到返回 null
     */
    private TmManualRollingTask findMergeTarget(List<TmManualRollingTask> shiftTaskList, TmManualRollingTask carryTask) {
        return shiftTaskList.stream()
                .filter(task -> Objects.equals(task.getTreadCode(), carryTask.getTreadCode()))
                .filter(task -> Objects.equals(task.getGlueCode(), carryTask.getGlueCode()))
                .filter(task -> Objects.equals(task.getMouthPlateCode(), carryTask.getMouthPlateCode()))
                .findFirst()
                .orElse(null);
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
     * @param task            当前任务
     * @param shiftOrder      当前班次
     * @param sequence        当前顺序
     * @param assignedQty     承接量
     * @param updateResultMap 待更新结果
     * @param insertResultSet 待新增结果
     * @return 被写入的排程结果
     */
    private TmScheduleResult applyTaskToResult(TmManualRollingTask task, int shiftOrder, int sequence, BigDecimal assignedQty,
                                               Map<Long, TmScheduleResult> updateResultMap, Set<TmScheduleResult> insertResultSet) {
        TmScheduleResult targetResult = task.getSourceResult();
        if (targetResult == null) {
            targetResult = this.copyBaseResult(task.getTemplateResult());
            targetResult.setDataSource(INSERT_DATA_SOURCE);
            targetResult.setReleaseStatus(ApsConstant.NO_RELEASE);
            insertResultSet.add(targetResult);
        } else {
            if (ApsConstant.IS_RELEASE.equals(targetResult.getReleaseStatus())) {
                targetResult.setReleaseStatus(ApsConstant.WAIT_RELEASING);
            }
            updateResultMap.put(targetResult.getId(), targetResult);
        }
        this.setShiftSequence(targetResult, shiftOrder, sequence);
        this.setShiftPlanQty(targetResult, shiftOrder, assignedQty);
        return targetResult;
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
        carryTask.setTemplateResult(sourceTask.getTemplateResult());
        carryTask.setShiftOrder(sourceTask.getShiftOrder() + 1);
        carryTask.setPlanQty(overflowQty);
        carryTask.setFinishQty(BigDecimal.ZERO);
        carryTask.setMachineCode(sourceTask.getMachineCode());
        carryTask.setTreadCode(sourceTask.getTreadCode());
        carryTask.setGlueCode(sourceTask.getGlueCode());
        carryTask.setMouthPlateCode(sourceTask.getMouthPlateCode());
        carryTask.setDataSource(INSERT_DATA_SOURCE);
        carryTask.setInsertTask(sourceTask.isInsertTask());
        carryTask.setCarryoverTask(true);
        return carryTask;
    }

    /**
     * 写入第 6 班后仍无法容纳的未排量。
     *
     * @param insertResult 插单结果
     * @param sourceTask   来源任务
     * @param unplannedQty 未排数量
     */
    private void insertUnplanned(TmScheduleResult insertResult, TmManualRollingTask sourceTask, BigDecimal unplannedQty) {
        TmUnplannedReasonEnum reason = TmUnplannedReasonEnum.CAPACITY_NOT_ENOUGH;
        TmScheduleUnplanned unplanned = new TmScheduleUnplanned();
        unplanned.setFactoryCode(insertResult.getFactoryCode());
        unplanned.setBatchNo(insertResult.getBatchNo());
        unplanned.setScheduleDate(insertResult.getScheduleDate());
        unplanned.setTreadCode(sourceTask.getTreadCode());
        unplanned.setGlueCode(sourceTask.getGlueCode());
        unplanned.setMouthPlateCode(sourceTask.getMouthPlateCode());
        unplanned.setUnplannedReasonCode(reason.getCode());
        unplanned.setUnplannedReasonDesc(reason.getDesc());
        unplanned.setUnplannedEvidenceJson(String.format("{\"source\":\"MANUAL_INSERT_ROLL\",\"machineCode\":\"%s\",\"unplannedQty\":%s}",
                insertResult.getMachineCode(), unplannedQty.stripTrailingZeros().toPlainString()));
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
     * 读取班次计划量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 班次计划量
     */
    private BigDecimal getShiftPlanQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format("class%dPlanQty", shiftOrder)));
    }

    /**
     * 设置班次计划量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @param planQty    计划量
     */
    private void setShiftPlanQty(TmScheduleResult result, int shiftOrder, BigDecimal planQty) {
        result.setFieldValueByFieldName(String.format("class%dPlanQty", shiftOrder), planQty);
    }

    /**
     * 读取班次完成量。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 完成量
     */
    private BigDecimal getShiftFinishQty(TmScheduleResult result, int shiftOrder) {
        return BigDecimalUtils.valueOf(result.getFieldValueByFieldName(String.format("class%dFinishQty", shiftOrder)));
    }

    /**
     * 读取班次顺序。
     *
     * @param result     排程结果
     * @param shiftOrder 班次顺序
     * @return 班次顺序
     */
    private Integer getShiftSequence(TmScheduleResult result, int shiftOrder) {
        Object sequence = result.getFieldValueByFieldName(String.format("class%dSequence", shiftOrder));
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
        result.setFieldValueByFieldName(String.format("class%dSequence", shiftOrder), sequence);
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
        String message = I18nUtil.getMessage(messageKey);
        return StringUtils.isBlank(message) || messageKey.equals(message) ? defaultMessage : message;
    }
}
