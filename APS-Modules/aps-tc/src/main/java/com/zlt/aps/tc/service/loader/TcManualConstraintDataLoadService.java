package com.zlt.aps.tc.service.loader;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintConfig;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.api.domain.entity.TcMachineSpeed;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.engine.domain.TcParamValue;
import com.zlt.aps.tc.engine.domain.TcScheduleContext;
import com.zlt.aps.tc.engine.domain.manual.TcManualRollingCommand;
import com.zlt.aps.tc.engine.domain.manual.TcManualRollingCommandBatch;
import com.zlt.aps.tc.engine.domain.manual.TcManualRollingContext;
import com.zlt.aps.tc.engine.domain.manual.TcManualTaskDraft;
import com.zlt.aps.tc.mapper.TcCurlRollMapper;
import com.zlt.aps.tc.mapper.TcMachineSpeedMapper;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.service.cache.TcAutoScheduleRedisCacheService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工排程约束数据装载服务。
 *
 * <p>服务按胎侧独立参数和基础资料装载切换、速度、卷曲长度、工装池及前日链尾，
 * 已由机台规则校验器扣除的维修产能不在此重复处理。</p>
 */
@Service
public class TcManualConstraintDataLoadService {

    private final TcParamsMapper tcParamsMapper;
    private final TcAutoScheduleRedisCacheService tcAutoScheduleRedisCacheService;
    private final TcMachineSpeedMapper tcMachineSpeedMapper;
    private final TcCurlRollMapper tcCurlRollMapper;
    private final TcScheduleResultMapper tcScheduleResultMapper;
    private final TcScheduleParamLoader tcScheduleParamLoader;

    /**
     * 创建胎侧人工约束装载服务。
     *
     * @param tcParamsMapper 参数 Mapper
     * @param tcAutoScheduleRedisCacheService 自动排程基础资料缓存服务
     * @param tcMachineSpeedMapper 机台速度 Mapper
     * @param tcCurlRollMapper 卷曲长度 Mapper
     * @param tcScheduleResultMapper 排程结果 Mapper
     */
    public TcManualConstraintDataLoadService(TcParamsMapper tcParamsMapper,
                                             TcAutoScheduleRedisCacheService tcAutoScheduleRedisCacheService,
                                             TcMachineSpeedMapper tcMachineSpeedMapper,
                                             TcCurlRollMapper tcCurlRollMapper,
                                             TcScheduleResultMapper tcScheduleResultMapper) {
        this.tcParamsMapper = tcParamsMapper;
        this.tcAutoScheduleRedisCacheService = tcAutoScheduleRedisCacheService;
        this.tcMachineSpeedMapper = tcMachineSpeedMapper;
        this.tcCurlRollMapper = tcCurlRollMapper;
        this.tcScheduleResultMapper = tcScheduleResultMapper;
        this.tcScheduleParamLoader = new TcScheduleParamLoader();
    }

    /**
     * 装载胎侧人工滚动约束快照。
     *
     * @param context 人工滚动上下文
     * @param machineCodeList 受影响机台
     * @param commandBatch 人工操作命令
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void enrich(TcManualRollingContext context, List<String> machineCodeList,
                       TcManualRollingCommandBatch commandBatch) {
        if (context == null || StrUtil.isBlank(context.getFactoryCode()) || context.getScheduleDate() == null) {
            throw new IllegalArgumentException("胎侧人工滚动约束上下文不能为空");
        }
        Set<String> machineCodeSet = Optional.ofNullable(machineCodeList).orElse(new ArrayList<>()).stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, TcParamValue> paramMap = this.loadParamMap(context.getFactoryCode());
        BigDecimal defaultSpeed = this.getParamValue(paramMap,
                TcScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED);
        BigDecimal defaultCurlLength = this.getParamValue(paramMap,
                TcScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        context.setConstraintConfig(this.buildConstraintConfig(paramMap));

        List<TcManualTaskDraft> constraintTaskList = new ArrayList<>(context.getTaskList());
        if (commandBatch != null && commandBatch.getCommandList() != null) {
            commandBatch.getCommandList().stream()
                    .map(TcManualRollingCommand::getInsertTask)
                    .filter(Objects::nonNull)
                    .forEach(constraintTaskList::add);
        }
        Map<String, BigDecimal> curlLengthMap = this.loadCurlLengthMap(
                context.getFactoryCode(), constraintTaskList, defaultCurlLength);
        this.fillTaskSpeedAndCurl(context, context.getFactoryCode(), machineCodeSet,
                constraintTaskList, curlLengthMap, defaultSpeed);
        context.setPredecessorTaskMap(this.loadPredecessorTaskMap(context, machineCodeSet));

        BigDecimal effectiveToolQty = this.getParamValue(paramMap, TcScheduleConstants.PARAM_TOOL_TOTAL_QTY)
                .multiply(this.getParamValue(paramMap, TcScheduleConstants.PARAM_VEHICLE_RATE));
        if (this.isPositive(effectiveToolQty)) {
            context.setInitialAvailableToolQty(this.resolveAffectedMachineAvailableToolQty(
                    context, machineCodeSet, effectiveToolQty, curlLengthMap, defaultCurlLength));
        }
    }

    /**
     * 复用胎侧自动排程参数装载器生成参数快照。
     *
     * @param context 人工滚动上下文
     * @param factoryCode 工厂编码
     * @return 参数快照
     */
    private Map<String, TcParamValue> loadParamMap(String factoryCode) {
        TcScheduleContext scheduleContext = new TcScheduleContext();
        scheduleContext.setFactoryCode(factoryCode);
        this.tcScheduleParamLoader.load(scheduleContext, tcParamsMapper, tcAutoScheduleRedisCacheService);
        return scheduleContext.getParamMap();
    }

    /**
     * 构建共用切换约束配置。
     *
     * @param paramMap 参数快照
     * @return 共用约束配置
     */
    private ScheduleConstraintConfig buildConstraintConfig(Map<String, TcParamValue> paramMap) {
        ScheduleConstraintConfig config = new ScheduleConstraintConfig();
        config.setSpecChangeMinutes(this.getParamValue(paramMap,
                TcScheduleConstants.PARAM_SPEC_CHANGE_MINUTES));
        config.setGlueChangeCapacityDeduct(this.getParamValue(paramMap,
                TcScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT));
        return config;
    }

    /**
     * 装载任务涉及胎侧编码的卷曲长度。
     *
     * @param factoryCode 工厂编码
     * @param taskList 任务列表
     * @param defaultCurlLength 默认卷曲长度
     * @return 胎侧编码到卷曲长度的映射
     */
    private Map<String, BigDecimal> loadCurlLengthMap(String factoryCode, List<TcManualTaskDraft> taskList,
                                                      BigDecimal defaultCurlLength) {
        Set<String> sidewallCodeSet = taskList.stream().map(TcManualTaskDraft::getSidewallCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, BigDecimal> curlLengthMap = new LinkedHashMap<>();
        if (!sidewallCodeSet.isEmpty()) {
            LambdaQueryWrapper<TcCurlRoll> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TcCurlRoll::getFactoryCode, factoryCode);
            wrapper.in(TcCurlRoll::getSidewallCode, sidewallCodeSet);
            Optional.ofNullable(tcCurlRollMapper.selectList(wrapper)).orElse(new ArrayList<>()).stream()
                    .filter(curlRoll -> curlRoll != null && this.isPositive(curlRoll.getCurlLength()))
                    .forEach(curlRoll -> curlLengthMap.put(curlRoll.getSidewallCode(), curlRoll.getCurlLength()));
        }
        sidewallCodeSet.forEach(sidewallCode -> curlLengthMap.putIfAbsent(sidewallCode, defaultCurlLength));
        return curlLengthMap;
    }

    /**
     * 按胎侧自动排程相同优先级补齐速度和卷曲长度。
     *
     * @param factoryCode 工厂编码
     * @param machineCodeSet 受影响机台
     * @param taskList 任务列表
     * @param curlLengthMap 卷曲长度映射
     * @param defaultSpeed 默认速度
     */
    private void fillTaskSpeedAndCurl(TcManualRollingContext context, String factoryCode,
                                      Set<String> machineCodeSet,
                                      List<TcManualTaskDraft> taskList,
                                      Map<String, BigDecimal> curlLengthMap,
                                      BigDecimal defaultSpeed) {
        LambdaQueryWrapper<TcMachineSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineSpeed::getFactoryCode, factoryCode);
        List<TcMachineSpeed> speedList = Optional.ofNullable(tcMachineSpeedMapper.selectList(wrapper))
                .orElse(new ArrayList<>());
        Map<String, BigDecimal> globalSidewallSpeedMap = new LinkedHashMap<>();
        Map<String, BigDecimal> machineDefaultSpeedMap = new LinkedHashMap<>();
        Map<String, BigDecimal> machineSidewallSpeedMap = new LinkedHashMap<>();
        speedList.stream().filter(speed -> speed != null && this.isPositive(speed.getProductSpeed()))
                .forEach(speed -> {
                    if (StrUtil.isBlank(speed.getMachineCode()) && StrUtil.isNotBlank(speed.getSidewallCode())) {
                        globalSidewallSpeedMap.putIfAbsent(speed.getSidewallCode(), speed.getProductSpeed());
                    } else if (machineCodeSet.contains(speed.getMachineCode())
                            && StrUtil.isBlank(speed.getSidewallCode())) {
                        machineDefaultSpeedMap.put(speed.getMachineCode(), speed.getProductSpeed());
                    } else if (machineCodeSet.contains(speed.getMachineCode())) {
                        machineSidewallSpeedMap.put(speed.getMachineCode() + "|" + speed.getSidewallCode(),
                                speed.getProductSpeed());
                    }
                });
        taskList.forEach(task -> {
            task.setMachineSpeed(this.resolveMachineSpeed(task.getMachineCode(), task.getSidewallCode(),
                    machineSidewallSpeedMap, globalSidewallSpeedMap, machineDefaultSpeedMap, defaultSpeed));
            task.setCurlRollLength(curlLengthMap.get(task.getSidewallCode()));
        });
        Set<String> sidewallCodeSet = taskList.stream().map(TcManualTaskDraft::getSidewallCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        machineCodeSet.forEach(machineCode -> sidewallCodeSet.forEach(sidewallCode ->
                context.getMachineSpecSpeedMap().put(machineCode + "|" + sidewallCode,
                        this.resolveMachineSpeed(machineCode, sidewallCode, machineSidewallSpeedMap,
                                globalSidewallSpeedMap, machineDefaultSpeedMap, defaultSpeed))));
    }

    /**
     * 按机台胎侧、全局胎侧、机台默认和参数默认顺序解析速度。
     *
     * @param machineCode 机台编码
     * @param sidewallCode 胎侧编码
     * @param machineSidewallSpeedMap 机台胎侧速度
     * @param globalSidewallSpeedMap 全局胎侧速度
     * @param machineDefaultSpeedMap 机台默认速度
     * @param defaultSpeed 参数默认速度
     * @return 有效速度
     */
    private BigDecimal resolveMachineSpeed(String machineCode, String sidewallCode,
                                           Map<String, BigDecimal> machineSidewallSpeedMap,
                                           Map<String, BigDecimal> globalSidewallSpeedMap,
                                           Map<String, BigDecimal> machineDefaultSpeedMap,
                                           BigDecimal defaultSpeed) {
        BigDecimal machineSpeed = machineSidewallSpeedMap.get(machineCode + "|" + sidewallCode);
        if (!this.isPositive(machineSpeed)) {
            machineSpeed = globalSidewallSpeedMap.get(sidewallCode);
        }
        if (!this.isPositive(machineSpeed)) {
            machineSpeed = machineDefaultSpeedMap.get(machineCode);
        }
        return this.isPositive(machineSpeed) ? machineSpeed : defaultSpeed;
    }

    /**
     * 装载前一排程日同机台最后有效任务。
     *
     * @param context 人工滚动上下文
     * @param machineCodeSet 受影响机台
     * @return key=机台编码的前日链尾
     */
    private Map<String, TcManualTaskDraft> loadPredecessorTaskMap(TcManualRollingContext context,
                                                                 Set<String> machineCodeSet) {
        Map<String, TcManualTaskDraft> predecessorTaskMap = new LinkedHashMap<>();
        if (machineCodeSet.isEmpty()) {
            return predecessorTaskMap;
        }
        Date previousDate = DateUtil.offsetDay(DateUtil.beginOfDay(context.getScheduleDate()), -1);
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, previousDate);
        wrapper.in(TcScheduleResult::getMachineCode, machineCodeSet);
        List<TcScheduleResult> resultList =
                Optional.ofNullable(tcScheduleResultMapper.selectList(wrapper)).orElse(new ArrayList<>());
        resultList.forEach(result -> {
            TcManualTaskDraft predecessor = this.resolveLatestPredecessor(result);
            TcManualTaskDraft exists = predecessorTaskMap.get(result.getMachineCode());
            if (predecessor != null && (exists == null || this.compareTaskPosition(predecessor, exists) > 0)) {
                predecessorTaskMap.put(result.getMachineCode(), predecessor);
            }
        });
        return predecessorTaskMap;
    }

    /**
     * 从横向结果中解析最后一个有计划量的班次任务。
     *
     * @param result 横向结果
     * @return 最后有效任务；不存在时返回空
     */
    private TcManualTaskDraft resolveLatestPredecessor(TcScheduleResult result) {
        if (result == null || StrUtil.isBlank(result.getMachineCode())) {
            return null;
        }
        TcManualTaskDraft latestTask = null;
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                    String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder)));
            Object sequenceValue = result.getFieldValueByFieldName(
                    String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
            if (!this.isPositive(planQty) || !(sequenceValue instanceof Number)) {
                continue;
            }
            TcManualTaskDraft task = new TcManualTaskDraft();
            task.setMachineCode(result.getMachineCode());
            task.setShiftOrder(shiftOrder);
            task.setSequence(((Number) sequenceValue).intValue());
            task.setSidewallCode(result.getSidewallCode());
            task.setGlueCode(result.getGlueCode());
            task.setBaseGlueCode(result.getBaseGlueCode());
            task.setMouthPlateCode(result.getMouthPlateCode());
            if (latestTask == null || this.compareTaskPosition(task, latestTask) > 0) {
                latestTask = task;
            }
        }
        return latestTask;
    }

    /**
     * 计算扣除其他机台占用后的受影响机台可用工装。
     *
     * @param context 人工滚动上下文
     * @param affectedMachineCodeSet 受影响机台
     * @param effectiveToolQty 整车率折算后的总工装
     * @param curlLengthMap 已装载卷曲长度
     * @param defaultCurlLength 默认卷曲长度
     * @return 受影响机台可用工装
     */
    private BigDecimal resolveAffectedMachineAvailableToolQty(TcManualRollingContext context,
                                                               Set<String> affectedMachineCodeSet,
                                                               BigDecimal effectiveToolQty,
                                                               Map<String, BigDecimal> curlLengthMap,
                                                               BigDecimal defaultCurlLength) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, context.getScheduleDate());
        wrapper.eq(StringUtils.isNotBlank(context.getBatchNo()), TcScheduleResult::getBatchNo, context.getBatchNo());
        wrapper.notIn(!affectedMachineCodeSet.isEmpty(), TcScheduleResult::getMachineCode, affectedMachineCodeSet);
        List<TcScheduleResult> unaffectedResultList =
                Optional.ofNullable(tcScheduleResultMapper.selectList(wrapper)).orElse(new ArrayList<>());
        Set<String> missingCodeSet = unaffectedResultList.stream().map(TcScheduleResult::getSidewallCode)
                .filter(StrUtil::isNotBlank).filter(code -> !curlLengthMap.containsKey(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingCodeSet.isEmpty()) {
            LambdaQueryWrapper<TcCurlRoll> curlWrapper = new LambdaQueryWrapper<>();
            curlWrapper.eq(TcCurlRoll::getFactoryCode, context.getFactoryCode());
            curlWrapper.in(TcCurlRoll::getSidewallCode, missingCodeSet);
            Optional.ofNullable(tcCurlRollMapper.selectList(curlWrapper)).orElse(new ArrayList<>()).stream()
                    .filter(curlRoll -> this.isPositive(curlRoll.getCurlLength()))
                    .forEach(curlRoll -> curlLengthMap.put(curlRoll.getSidewallCode(), curlRoll.getCurlLength()));
        }
        BigDecimal unaffectedToolUsage = unaffectedResultList.stream()
                .map(result -> this.calculateResultToolUsage(result,
                        curlLengthMap.getOrDefault(result.getSidewallCode(), defaultCurlLength)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return effectiveToolQty.subtract(unaffectedToolUsage).max(BigDecimal.ZERO);
    }

    /**
     * 计算一条胎侧横向结果的工装占用。
     *
     * @param result 横向结果
     * @param curlLength 卷曲长度
     * @return 工装占用
     */
    private BigDecimal calculateResultToolUsage(TcScheduleResult result, BigDecimal curlLength) {
        if (!this.isPositive(curlLength)) {
            return BigDecimal.ZERO;
        }
        BigDecimal planQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= TcScheduleConstants.TC_MAX_SHIFT_ORDER; shiftOrder++) {
            planQty = planQty.add(BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                    String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder))));
        }
        return planQty.divide(curlLength,
                TcScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 读取有效参数数值。
     *
     * @param paramMap 参数快照
     * @param paramCode 参数编码
     * @return 数值，空值返回 0
     */
    private BigDecimal getParamValue(Map<String, TcParamValue> paramMap, String paramCode) {
        TcParamValue paramValue = paramMap.get(paramCode);
        return BigDecimalUtils.valueOf(paramValue == null ? null : paramValue.getEffectiveValue());
    }

    /**
     * 比较任务在横向六班中的位置。
     *
     * @param left 左任务
     * @param right 右任务
     * @return 正数表示左任务更晚
     */
    private int compareTaskPosition(TcManualTaskDraft left, TcManualTaskDraft right) {
        int shiftCompare = Integer.compare(Optional.ofNullable(left.getShiftOrder()).orElse(0),
                Optional.ofNullable(right.getShiftOrder()).orElse(0));
        if (shiftCompare != 0) {
            return shiftCompare;
        }
        return Integer.compare(Optional.ofNullable(left.getSequence()).orElse(0),
                Optional.ofNullable(right.getSequence()).orElse(0));
    }

    /**
     * 判断数值是否大于零。
     *
     * @param value 数值
     * @return 大于零返回 true
     */
    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
