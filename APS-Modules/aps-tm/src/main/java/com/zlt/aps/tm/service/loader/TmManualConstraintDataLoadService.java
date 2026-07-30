package com.zlt.aps.tm.service.loader;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.schedule.constraint.ScheduleConstraintConfig;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.*;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.manual.TmManualRollingCommand;
import com.zlt.aps.tm.engine.domain.manual.TmManualRollingCommandBatch;
import com.zlt.aps.tm.engine.domain.manual.TmManualRollingContext;
import com.zlt.aps.tm.engine.domain.manual.TmManualTaskDraft;
import com.zlt.aps.tm.mapper.*;
import com.zlt.aps.tm.service.cache.TmAutoScheduleRedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工排程约束数据装载服务。
 *
 * <p>本服务只读取参数、速度、维修、前日链尾和同批次工装占用，并写入独立运行态上下文；
 * 不修改排程结果，不执行人工滚动，也不持有数据库实体供引擎修改。</p>
 */
@Slf4j
@Service
public class TmManualConstraintDataLoadService {

    private final TmParamsMapper tmParamsMapper;
    private final TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService;
    private final TmMachineSpeedMapper tmMachineSpeedMapper;
    private final TmMachineMaintenanceMapper tmMachineMaintenanceMapper;
    private final TmShiftConfigMapper tmShiftConfigMapper;
    private final TmScheduleResultMapper tmScheduleResultMapper;
    private final TmScheduleResultExplainMapper tmScheduleResultExplainMapper;
    private final TmScheduleParamLoader tmScheduleParamLoader;

    /**
     * 创建胎面人工约束数据装载服务。
     *
     * @param tmParamsMapper 参数 Mapper
     * @param tmAutoScheduleRedisCacheService 自动排程基础资料缓存服务
     * @param tmMachineSpeedMapper 机台速度 Mapper
     * @param tmMachineMaintenanceMapper 机台维修 Mapper
     * @param tmShiftConfigMapper 班次配置 Mapper
     * @param tmScheduleResultMapper 排程结果 Mapper
     * @param tmScheduleResultExplainMapper 排程解释 Mapper
     */
    public TmManualConstraintDataLoadService(TmParamsMapper tmParamsMapper,
                                             TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService,
                                             TmMachineSpeedMapper tmMachineSpeedMapper,
                                             TmMachineMaintenanceMapper tmMachineMaintenanceMapper,
                                             TmShiftConfigMapper tmShiftConfigMapper,
                                             TmScheduleResultMapper tmScheduleResultMapper,
                                             TmScheduleResultExplainMapper tmScheduleResultExplainMapper) {
        this.tmParamsMapper = tmParamsMapper;
        this.tmAutoScheduleRedisCacheService = tmAutoScheduleRedisCacheService;
        this.tmMachineSpeedMapper = tmMachineSpeedMapper;
        this.tmMachineMaintenanceMapper = tmMachineMaintenanceMapper;
        this.tmShiftConfigMapper = tmShiftConfigMapper;
        this.tmScheduleResultMapper = tmScheduleResultMapper;
        this.tmScheduleResultExplainMapper = tmScheduleResultExplainMapper;
        this.tmScheduleParamLoader = new TmScheduleParamLoader();
    }

    /**
     * 装载人工滚动所需的全部约束快照。
     *
     * @param context 人工滚动上下文
     * @param machineCodeList 本次受影响机台
     * @param commandBatch 人工操作命令批次
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void enrich(TmManualRollingContext context, List<String> machineCodeList,
                       TmManualRollingCommandBatch commandBatch) {
        if (context == null || StrUtil.isBlank(context.getFactoryCode()) || context.getScheduleDate() == null) {
            throw new IllegalArgumentException("胎面人工滚动约束上下文不能为空");
        }
        Set<String> machineCodeSet = Optional.ofNullable(machineCodeList).orElse(new ArrayList<>()).stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, TmParamValue> paramMap = this.loadParamMap(context.getFactoryCode());
        BigDecimal defaultSpeed = this.getParamValue(paramMap,
                TmScheduleConstants.PARAM_DEFAULT_PRODUCTION_SPEED);
        BigDecimal defaultCurlLength = this.getParamValue(paramMap,
                TmScheduleConstants.PARAM_DEFAULT_CURL_LENGTH);
        context.setConstraintConfig(this.buildConstraintConfig(paramMap));

        List<TmManualTaskDraft> constraintTaskList = new ArrayList<>(context.getTaskList());
        if (commandBatch != null && commandBatch.getCommandList() != null) {
            commandBatch.getCommandList().stream()
                    .map(TmManualRollingCommand::getInsertTask)
                    .filter(Objects::nonNull)
                    .forEach(constraintTaskList::add);
        }
        this.fillTaskSpeedAndCurl(context, context.getFactoryCode(), machineCodeSet, constraintTaskList,
                defaultSpeed, defaultCurlLength);
        context.setMaintenanceHoursMap(this.loadMaintenanceHoursMap(context, machineCodeSet));
        context.setPredecessorTaskMap(this.loadPredecessorTaskMap(context, machineCodeSet));
        BigDecimal totalToolQty = this.getParamValue(paramMap, TmScheduleConstants.PARAM_TOOL_TOTAL_QTY);
        if (this.isPositive(totalToolQty)) {
            context.setTotalToolQty(totalToolQty);
            context.setInitialAvailableToolQty(this.resolveFinalToolLedgerBalance(context));
        }
    }

    /**
     * 复用自动排程参数装载器生成参数快照。
     *
     * @param context 人工滚动上下文
     * @param factoryCode 工厂编码
     * @return 参数快照
     */
    private Map<String, TmParamValue> loadParamMap(String factoryCode) {
        TmScheduleContext scheduleContext = new TmScheduleContext();
        scheduleContext.setFactoryCode(factoryCode);
        this.tmScheduleParamLoader.load(scheduleContext, tmParamsMapper, tmAutoScheduleRedisCacheService);
        return scheduleContext.getParamMap();
    }

    /**
     * 构建共用切换约束配置。
     *
     * @param paramMap 参数快照
     * @return 共用切换约束配置
     */
    private ScheduleConstraintConfig buildConstraintConfig(Map<String, TmParamValue> paramMap) {
        ScheduleConstraintConfig config = new ScheduleConstraintConfig();
        config.setSpecChangeMinutes(this.getParamValue(paramMap,
                TmScheduleConstants.PARAM_SPEC_CHANGE_MINUTES));
        config.setGlueChangeCapacityDeduct(this.getParamValue(paramMap,
                TmScheduleConstants.PARAM_GLUE_CHANGE_CAPACITY_DEDUCT));
        return config;
    }

    /**
     * 按自动排程相同优先级补齐任务机台速度和卷曲长度。
     *
     * @param factoryCode 工厂编码
     * @param machineCodeSet 受影响机台
     * @param taskList 任务列表
     * @param defaultSpeed 默认速度
     * @param defaultCurlLength 默认卷曲长度
     */
    private void fillTaskSpeedAndCurl(TmManualRollingContext context, String factoryCode,
                                      Set<String> machineCodeSet,
                                      List<TmManualTaskDraft> taskList, BigDecimal defaultSpeed,
                                      BigDecimal defaultCurlLength) {
        LambdaQueryWrapper<TmMachineSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineSpeed::getFactoryCode, factoryCode);
        List<TmMachineSpeed> speedList = Optional.ofNullable(tmMachineSpeedMapper.selectList(wrapper))
                .orElse(new ArrayList<>());
        Map<String, BigDecimal> globalTreadSpeedMap = new LinkedHashMap<>();
        Map<String, BigDecimal> machineDefaultSpeedMap = new LinkedHashMap<>();
        Map<String, BigDecimal> machineTreadSpeedMap = new LinkedHashMap<>();
        speedList.stream()
                .filter(speed -> speed != null && this.isPositive(speed.getProductSpeed()))
                .forEach(speed -> {
                    if (StrUtil.isBlank(speed.getMachineCode()) && StrUtil.isNotBlank(speed.getTreadCode())) {
                        globalTreadSpeedMap.putIfAbsent(speed.getTreadCode(), speed.getProductSpeed());
                    } else if (machineCodeSet.contains(speed.getMachineCode())
                            && StrUtil.isBlank(speed.getTreadCode())) {
                        machineDefaultSpeedMap.put(speed.getMachineCode(), speed.getProductSpeed());
                    } else if (machineCodeSet.contains(speed.getMachineCode())) {
                        machineTreadSpeedMap.put(speed.getMachineCode() + "|" + speed.getTreadCode(),
                                speed.getProductSpeed());
                    }
                });
        taskList.forEach(task -> {
            task.setMachineSpeed(this.resolveMachineSpeed(task.getMachineCode(), task.getTreadCode(),
                    machineTreadSpeedMap, globalTreadSpeedMap, machineDefaultSpeedMap, defaultSpeed));
            if (!this.isPositive(task.getCurlRollLength())) {
                task.setCurlRollLength(defaultCurlLength);
            }
        });
        Set<String> treadCodeSet = taskList.stream().map(TmManualTaskDraft::getTreadCode)
                .filter(StrUtil::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
        machineCodeSet.forEach(machineCode -> treadCodeSet.forEach(treadCode ->
                context.getMachineSpecSpeedMap().put(machineCode + "|" + treadCode,
                        this.resolveMachineSpeed(machineCode, treadCode, machineTreadSpeedMap,
                                globalTreadSpeedMap, machineDefaultSpeedMap, defaultSpeed))));
    }

    /**
     * 按机台胎面、全局胎面、机台默认和参数默认顺序解析速度。
     *
     * @param machineCode 机台编码
     * @param treadCode 胎面编码
     * @param machineTreadSpeedMap 机台胎面速度
     * @param globalTreadSpeedMap 全局胎面速度
     * @param machineDefaultSpeedMap 机台默认速度
     * @param defaultSpeed 参数默认速度
     * @return 有效速度
     */
    private BigDecimal resolveMachineSpeed(String machineCode, String treadCode,
                                           Map<String, BigDecimal> machineTreadSpeedMap,
                                           Map<String, BigDecimal> globalTreadSpeedMap,
                                           Map<String, BigDecimal> machineDefaultSpeedMap,
                                           BigDecimal defaultSpeed) {
        BigDecimal machineSpeed = machineTreadSpeedMap.get(machineCode + "|" + treadCode);
        if (!this.isPositive(machineSpeed)) {
            machineSpeed = globalTreadSpeedMap.get(treadCode);
        }
        if (!this.isPositive(machineSpeed)) {
            machineSpeed = machineDefaultSpeedMap.get(machineCode);
        }
        return this.isPositive(machineSpeed) ? machineSpeed : defaultSpeed;
    }

    /**
     * 装载机台班次维修重叠小时。
     *
     * @param context 人工滚动上下文
     * @param machineCodeSet 受影响机台
     * @return key=机台|班次顺序的维修小时
     */
    private Map<String, BigDecimal> loadMaintenanceHoursMap(TmManualRollingContext context,
                                                            Set<String> machineCodeSet) {
        Map<String, Date[]> shiftWindowMap = this.buildShiftWindowMap(context);
        Map<String, BigDecimal> maintenanceHoursMap = new LinkedHashMap<>();
        if (machineCodeSet.isEmpty() || shiftWindowMap.isEmpty()) {
            return maintenanceHoursMap;
        }
        LambdaQueryWrapper<TmMachineMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineMaintenance::getFactoryCode, context.getFactoryCode());
        wrapper.in(TmMachineMaintenance::getMachineCode, machineCodeSet);
        List<TmMachineMaintenance> maintenanceList =
                Optional.ofNullable(tmMachineMaintenanceMapper.selectList(wrapper)).orElse(new ArrayList<>());
        maintenanceList.stream()
                .filter(maintenance -> maintenance != null
                        && maintenance.getStopStartTime() != null && maintenance.getStopEndTime() != null)
                .forEach(maintenance -> shiftWindowMap.forEach((shiftKey, shiftWindow) -> {
                    BigDecimal overlapHours = this.calculateOverlapHours(maintenance.getStopStartTime(),
                            maintenance.getStopEndTime(), shiftWindow[0], shiftWindow[1]);
                    if (this.isPositive(overlapHours)) {
                        maintenanceHoursMap.merge(maintenance.getMachineCode() + "|" + shiftKey,
                                overlapHours, BigDecimal::add);
                    }
                }));
        return maintenanceHoursMap;
    }

    /**
     * 构建排程日启用班次时间窗口，配置缺失时使用结果快照中的班次起止时间兜底。
     *
     * @param context 人工滚动上下文
     * @return key=班次顺序文本的时间窗口
     */
    private Map<String, Date[]> buildShiftWindowMap(TmManualRollingContext context) {
        LambdaQueryWrapper<TmShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmShiftConfig::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmShiftConfig::getOpenFlag, TmYesNoEnum.YES.getCode());
        List<TmShiftConfig> configList = Optional.ofNullable(tmShiftConfigMapper.selectList(wrapper))
                .orElse(new ArrayList<>()).stream()
                .filter(config -> config != null && config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TmShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
        Map<String, Date[]> shiftWindowMap = new LinkedHashMap<>();
        String scheduleDateText = DateUtil.formatDate(context.getScheduleDate());
        Date previousEndTime = null;
        for (TmShiftConfig config : configList) {
            if (StrUtil.isBlank(config.getPlanStartTime()) || StrUtil.isBlank(config.getPlanEndTime())) {
                continue;
            }
            try {
                Date startTime = DateUtil.parse(scheduleDateText + " " + config.getPlanStartTime());
                Date endTime = DateUtil.parse(scheduleDateText + " " + config.getPlanEndTime());
                if (TmYesNoEnum.YES.getCode().equals(config.getCrossDayFlag()) || !endTime.after(startTime)) {
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                while (previousEndTime != null && startTime.before(previousEndTime)) {
                    startTime = DateUtil.offsetDay(startTime, 1);
                    endTime = DateUtil.offsetDay(endTime, 1);
                }
                shiftWindowMap.put(String.valueOf(config.getShiftOrder()), new Date[]{startTime, endTime});
                previousEndTime = endTime;
            } catch (Exception exception) {
                log.warn("[TM_MANUAL_SHIFT_WINDOW_PARSE_FAIL] factoryCode={}, scheduleDate={}, shiftOrder={}",
                        context.getFactoryCode(), scheduleDateText, config.getShiftOrder(), exception);
            }
        }
        if (!shiftWindowMap.isEmpty()) {
            return shiftWindowMap;
        }
        context.getTaskList().stream()
                .filter(task -> task.getShiftOrder() != null
                        && task.getSourceStartTime() != null && task.getSourceEndTime() != null)
                .forEach(task -> shiftWindowMap.putIfAbsent(String.valueOf(task.getShiftOrder()),
                        new Date[]{task.getSourceStartTime(), task.getSourceEndTime()}));
        return shiftWindowMap;
    }

    /**
     * 计算两个时间窗口的重叠小时。
     *
     * @param sourceStart 源开始时间
     * @param sourceEnd 源结束时间
     * @param targetStart 目标开始时间
     * @param targetEnd 目标结束时间
     * @return 重叠小时
     */
    private BigDecimal calculateOverlapHours(Date sourceStart, Date sourceEnd,
                                             Date targetStart, Date targetEnd) {
        Date overlapStart = sourceStart.after(targetStart) ? sourceStart : targetStart;
        Date overlapEnd = sourceEnd.before(targetEnd) ? sourceEnd : targetEnd;
        if (!overlapStart.before(overlapEnd)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(DateUtil.between(overlapStart, overlapEnd, DateUnit.MINUTE))
                .divide(BigDecimal.valueOf(TmScheduleConstants.MINUTES_PER_HOUR),
                        TmScheduleConstants.DECIMAL_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 装载前一排程日同机台最后有效任务。
     *
     * @param context 人工滚动上下文
     * @param machineCodeSet 受影响机台
     * @return key=机台编码的前日链尾任务
     */
    private Map<String, TmManualTaskDraft> loadPredecessorTaskMap(TmManualRollingContext context,
                                                                 Set<String> machineCodeSet) {
        Map<String, TmManualTaskDraft> predecessorTaskMap = new LinkedHashMap<>();
        if (machineCodeSet.isEmpty()) {
            return predecessorTaskMap;
        }
        Date previousDate = DateUtil.offsetDay(DateUtil.beginOfDay(context.getScheduleDate()), -1);
        LambdaQueryWrapper<TmScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResult::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmScheduleResult::getScheduleDate, previousDate);
        wrapper.in(TmScheduleResult::getMachineCode, machineCodeSet);
        List<TmScheduleResult> resultList =
                Optional.ofNullable(tmScheduleResultMapper.selectList(wrapper)).orElse(new ArrayList<>());
        resultList.forEach(result -> {
            TmManualTaskDraft predecessor = this.resolveLatestPredecessor(result);
            TmManualTaskDraft exists = predecessorTaskMap.get(result.getMachineCode());
            if (predecessor != null && (exists == null || this.compareTaskPosition(predecessor, exists) > 0)) {
                predecessorTaskMap.put(result.getMachineCode(), predecessor);
            }
        });
        return predecessorTaskMap;
    }

    /**
     * 从横向结果中解析最后一个有计划量的班次任务。
     *
     * @param result 横向排程结果
     * @return 最后有效任务；不存在时返回空
     */
    private TmManualTaskDraft resolveLatestPredecessor(TmScheduleResult result) {
        if (result == null || StrUtil.isBlank(result.getMachineCode())) {
            return null;
        }
        TmManualTaskDraft latestTask = null;
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal planQty = BigDecimalUtils.valueOf(result.getFieldValueByFieldName(
                    String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder)));
            Object sequenceValue = result.getFieldValueByFieldName(
                    String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder));
            if (!this.isPositive(planQty) || !(sequenceValue instanceof Integer)) {
                continue;
            }
            TmManualTaskDraft task = new TmManualTaskDraft();
            task.setMachineCode(result.getMachineCode());
            task.setShiftOrder(shiftOrder);
            task.setSequence((Integer) sequenceValue);
            task.setTreadCode(result.getTreadCode());
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
     * 计算局部重放前受影响机台可使用的剩余工装。
     *
     * @param context 人工滚动上下文
     * @param affectedMachineCodeSet 受影响机台
     * @param totalToolQty 工装总数
     * @param defaultCurlLength 默认卷曲长度
     * @return 扣除其他机台占用后的可用工装
     */
    private BigDecimal resolveFinalToolLedgerBalance(TmManualRollingContext context) {
        LambdaQueryWrapper<TmScheduleResultExplain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmScheduleResultExplain::getFactoryCode, context.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(context.getBatchNo()),
                TmScheduleResultExplain::getBatchNo, context.getBatchNo());
        wrapper.isNotNull(TmScheduleResultExplain::getToolLedgerOrder);
        wrapper.isNotNull(TmScheduleResultExplain::getRemainingToolQty);
        wrapper.orderByDesc(TmScheduleResultExplain::getToolLedgerOrder);
        wrapper.last("LIMIT 1");
        TmScheduleResultExplain latestLedger =
                tmScheduleResultExplainMapper.selectOne(wrapper);
        if (latestLedger == null) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.manualToolLedgerMissing"));
        }
        return latestLedger.getRemainingToolQty().max(BigDecimal.ZERO);
    }

    /**
     * 读取有效参数的数值。
     *
     * @param paramMap 参数快照
     * @param paramCode 参数编码
     * @return 参数数值，空值返回 0
     */
    private BigDecimal getParamValue(Map<String, TmParamValue> paramMap, String paramCode) {
        TmParamValue paramValue = paramMap.get(paramCode);
        return BigDecimalUtils.valueOf(paramValue == null ? null : paramValue.getEffectiveValue());
    }

    /**
     * 比较任务在横向六班中的先后位置。
     *
     * @param left 左任务
     * @param right 右任务
     * @return 正数表示左任务更晚
     */
    private int compareTaskPosition(TmManualTaskDraft left, TmManualTaskDraft right) {
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
