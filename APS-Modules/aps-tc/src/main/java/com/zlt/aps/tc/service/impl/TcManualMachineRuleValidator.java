package com.zlt.aps.tc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.*;
import com.zlt.aps.tc.api.enums.TcSpecifyMachineJobTypeEnum;
import com.zlt.aps.tc.mapper.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧人工普通转机台规则校验器。
 *
 * <p>该类是人工转机规则的统一入口。基础机台启用校验在此完成，口型、胶料、定点、禁排、
 * 共享机台和维修等资料由后续专用校验方法集中扩展，禁止调用胎面实现。</p>
 */
@Service
public class TcManualMachineRuleValidator {

    private final TcMachineInfoMapper machineInfoMapper;

    private final TcShiftConfigMapper shiftConfigMapper;

    private final TcMachineMaintenanceMapper machineMaintenanceMapper;

    private final TcMachineSpeedMapper machineSpeedMapper;

    private final TcMouthPlateMapper mouthPlateMapper;

    private final TcGlueMachineRealMapper glueMachineRealMapper;

    private final TcSpecifyMachineMapper specifyMachineMapper;

    private final TcDjSharedMachineMapper djSharedMachineMapper;

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcParamsMapper paramsMapper;

    /**
     * 构造人工转机台校验器。
     *
     * @param machineInfoMapper 胎侧机台 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     * @param machineMaintenanceMapper 维修计划 Mapper
     * @param machineSpeedMapper 机台速度 Mapper
     * @param mouthPlateMapper 口型板 Mapper
     * @param glueMachineRealMapper 胶料机台 Mapper
     * @param specifyMachineMapper 定点与禁排 Mapper
     * @param djSharedMachineMapper 胎侧垫胶共机 Mapper
     * @param scheduleResultMapper 排程结果 Mapper
     * @param paramsMapper 胎侧参数 Mapper
     */
    public TcManualMachineRuleValidator(TcMachineInfoMapper machineInfoMapper,
                                        TcShiftConfigMapper shiftConfigMapper,
                                        TcMachineMaintenanceMapper machineMaintenanceMapper,
                                        TcMachineSpeedMapper machineSpeedMapper,
                                        TcMouthPlateMapper mouthPlateMapper,
                                        TcGlueMachineRealMapper glueMachineRealMapper,
                                        TcSpecifyMachineMapper specifyMachineMapper,
                                        TcDjSharedMachineMapper djSharedMachineMapper,
                                        TcScheduleResultMapper scheduleResultMapper,
                                        TcParamsMapper paramsMapper) {
        this.machineInfoMapper = machineInfoMapper;
        this.shiftConfigMapper = shiftConfigMapper;
        this.machineMaintenanceMapper = machineMaintenanceMapper;
        this.machineSpeedMapper = machineSpeedMapper;
        this.mouthPlateMapper = mouthPlateMapper;
        this.glueMachineRealMapper = glueMachineRealMapper;
        this.specifyMachineMapper = specifyMachineMapper;
        this.djSharedMachineMapper = djSharedMachineMapper;
        this.scheduleResultMapper = scheduleResultMapper;
        this.paramsMapper = paramsMapper;
    }

    /**
     * 校验目标机台可用于指定班次的普通转机。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台编码
     * @param shiftOrder 待转班次
     * @throws ServiceException 目标机台为空、停用或未维护时抛出
     */
    public void validateTransfer(TcScheduleResult sourceResult, String targetMachineCode, Integer shiftOrder) {
        if (sourceResult == null || StringUtils.isBlank(targetMachineCode) || shiftOrder == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.invalidRequest"));
        }
        LambdaQueryWrapper<TcMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineInfo::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcMachineInfo::getMachineCode, targetMachineCode.trim());
        List<TcMachineInfo> machineInfoList = this.machineInfoMapper.selectList(wrapper);
        if (machineInfoList == null || machineInfoList.isEmpty()
                || !"1".equals(machineInfoList.get(0).getMachineStatus())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.machineDisabled"));
        }
        TcMachineInfo machineInfo = machineInfoList.get(0);
        TcShiftConfig shiftConfig = this.requireOpenShift(sourceResult, shiftOrder);
        this.validateMachineOpenShift(machineInfo, shiftConfig);
        this.validateMouthPlate(sourceResult, targetMachineCode);
        this.validateGlueMachine(sourceResult, targetMachineCode, shiftConfig.getShiftCode());
        this.validateSpecifyMachine(sourceResult, targetMachineCode);
        this.validateSharedMachine(sourceResult, targetMachineCode, shiftConfig.getShiftCode());
        this.validateCapacity(sourceResult, targetMachineCode, shiftOrder, shiftConfig, machineInfo);
    }

    /**
     * 查询并校验排程班次已开班。
     *
     * @param sourceResult 源结果
     * @param shiftOrder 班次顺序
     * @return 班次配置
     */
    private TcShiftConfig requireOpenShift(TcScheduleResult sourceResult, Integer shiftOrder) {
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcShiftConfig::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcShiftConfig::getShiftOrder, shiftOrder);
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(wrapper);
        if (shiftConfigList == null || shiftConfigList.isEmpty()
                || !"1".equals(shiftConfigList.get(0).getOpenFlag())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.shiftClosed"));
        }
        return shiftConfigList.get(0);
    }

    /**
     * 校验机台维护的开班编码包含当前班次。
     *
     * @param machineInfo 机台资料
     * @param shiftConfig 当前班次配置
     */
    private void validateMachineOpenShift(TcMachineInfo machineInfo, TcShiftConfig shiftConfig) {
        if (!this.isMachineShiftOpen(machineInfo, shiftConfig)) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.machineShiftClosed"));
        }
    }

    /**
     * 判断机台维护的开班编码是否包含指定班次。
     *
     * @param machineInfo 机台资料
     * @param shiftConfig 班次配置
     * @return true 表示机台允许该班次
     */
    private boolean isMachineShiftOpen(TcMachineInfo machineInfo, TcShiftConfig shiftConfig) {
        if (StringUtils.isBlank(machineInfo.getOpenShiftCode())) {
            return true;
        }
        Set<String> openShiftCodeSet = java.util.Arrays.stream(machineInfo.getOpenShiftCode().split(","))
                .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        return openShiftCodeSet.contains(shiftConfig.getShiftCode());
    }

    /**
     * 校验目标机台口型板能力。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     */
    private void validateMouthPlate(TcScheduleResult sourceResult, String targetMachineCode) {
        if (StringUtils.isBlank(sourceResult.getMouthPlateCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.mouthPlateInvalid"));
        }
        LambdaQueryWrapper<TcMouthPlate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMouthPlate::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcMouthPlate::getMouthPlateCode, sourceResult.getMouthPlateCode());
        List<TcMouthPlate> mouthPlateList = this.mouthPlateMapper.selectList(wrapper);
        boolean targetMatched = mouthPlateList != null && mouthPlateList.stream()
                .anyMatch(item -> Objects.equals(item.getMachineCode(), targetMachineCode)
                        && "1".equals(item.getPlateStatus()));
        if (!targetMatched) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.mouthPlateRejected"));
        }
    }

    /**
     * 校验目标机台胶料允许/禁止规则。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @param shiftCode 班次编码
     */
    private void validateGlueMachine(TcScheduleResult sourceResult, String targetMachineCode, String shiftCode) {
        LambdaQueryWrapper<TcGlueMachineReal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcGlueMachineReal::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcGlueMachineReal::getEnableStatus, "1");
        List<TcGlueMachineReal> ruleList = this.glueMachineRealMapper.selectList(wrapper);
        List<TcGlueMachineReal> relevantRuleList = ruleList == null ? Collections.emptyList()
                : ruleList.stream().filter(item -> Objects.equals(item.getGlueCode(), sourceResult.getGlueCode()))
                .filter(item -> StringUtils.isBlank(item.getBaseGlueCode())
                        || Objects.equals(item.getBaseGlueCode(), sourceResult.getBaseGlueCode()))
                .filter(item -> StringUtils.isBlank(item.getShiftCode())
                        || Objects.equals(item.getShiftCode(), shiftCode)).collect(Collectors.toList());
        if (relevantRuleList.isEmpty()) {
            return;
        }
        List<TcGlueMachineReal> targetRuleList = relevantRuleList.stream()
                .filter(item -> Objects.equals(item.getMachineCode(), targetMachineCode)).collect(Collectors.toList());
        boolean forbidden = targetRuleList.stream().anyMatch(item -> "0".equals(item.getAllowFlag()));
        boolean hasAllowRule = relevantRuleList.stream().anyMatch(item -> "1".equals(item.getAllowFlag()));
        boolean allowed = targetRuleList.stream().anyMatch(item -> "1".equals(item.getAllowFlag()));
        if (forbidden || hasAllowRule && !allowed) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.glueRejected"));
        }
    }

    /**
     * 校验胎侧定点生产和禁排规则。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     */
    private void validateSpecifyMachine(TcScheduleResult sourceResult, String targetMachineCode) {
        LambdaQueryWrapper<TcSpecifyMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcSpecifyMachine::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcSpecifyMachine::getSidewallCode, sourceResult.getSidewallCode());
        wrapper.eq(TcSpecifyMachine::getEnableStatus, "1");
        List<TcSpecifyMachine> ruleList = this.specifyMachineMapper.selectList(wrapper);
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        boolean targetForbidden = ruleList.stream().anyMatch(item -> Objects.equals(item.getMachineCode(),
                targetMachineCode) && TcSpecifyMachineJobTypeEnum.FORBID.getCode().equals(item.getJobType()));
        boolean hasFixedAllow = ruleList.stream().anyMatch(item ->
                TcSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(item.getJobType()));
        boolean targetAllowed = ruleList.stream().anyMatch(item -> Objects.equals(item.getMachineCode(),
                targetMachineCode) && TcSpecifyMachineJobTypeEnum.ALLOW.getCode().equals(item.getJobType()));
        if (targetForbidden || hasFixedAllow && !targetAllowed) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.specifyRejected"));
        }
    }

    /**
     * 校验胎侧垫胶共用机台错班配置。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @param shiftCode 当前胎侧班次编码
     */
    private void validateSharedMachine(TcScheduleResult sourceResult, String targetMachineCode, String shiftCode) {
        LambdaQueryWrapper<TcDjSharedMachine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcDjSharedMachine::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcDjSharedMachine::getMachineCode, targetMachineCode);
        wrapper.eq(TcDjSharedMachine::getEnableStatus, "1");
        List<TcDjSharedMachine> ruleList = this.djSharedMachineMapper.selectList(wrapper);
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        boolean allowed = ruleList.stream().map(TcDjSharedMachine::getTcShiftCode)
                .filter(StringUtils::isNotBlank).anyMatch(shiftCode::equals);
        if (!allowed) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.sharedShiftRejected"));
        }
    }

    /**
     * 校验维修后产能及单班 5500 米上限。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @param shiftOrder 班次顺序
     * @param shiftConfig 班次配置
     * @param machineInfo 机台资料
     */
    private void validateCapacity(TcScheduleResult sourceResult, String targetMachineCode, Integer shiftOrder,
                                  TcShiftConfig shiftConfig, TcMachineInfo machineInfo) {
        BigDecimal incomingPlanQty = this.readPlanQty(sourceResult, shiftOrder);
        BigDecimal assignedPlanQty = this.loadAssignedPlanQty(sourceResult, targetMachineCode, shiftOrder);
        BigDecimal effectiveCapacity = this.calculateEffectiveCapacity(sourceResult, targetMachineCode,
                shiftConfig, machineInfo);
        if (assignedPlanQty.add(incomingPlanQty).compareTo(effectiveCapacity) > 0) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.capacityRejected"));
        }
    }

    /**
     * 解析人工滚动时指定机台和班次的有效产能；关班、机台停用或未开放班次返回零。
     *
     * @param sourceResult 排程结果参考
     * @param targetMachineCode 目标机台编码
     * @param shiftOrder 班次顺序
     * @return 扣除维修影响后的有效产能
     */
    BigDecimal resolveRollingCapacity(TcScheduleResult sourceResult, String targetMachineCode, Integer shiftOrder) {
        if (sourceResult == null || StringUtils.isBlank(targetMachineCode) || shiftOrder == null) {
            return BigDecimal.ZERO;
        }
        LambdaQueryWrapper<TcMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
        machineWrapper.eq(TcMachineInfo::getFactoryCode, sourceResult.getFactoryCode());
        machineWrapper.eq(TcMachineInfo::getMachineCode, targetMachineCode);
        List<TcMachineInfo> machineInfoList = this.machineInfoMapper.selectList(machineWrapper);
        if (machineInfoList == null || machineInfoList.isEmpty()
                || !"1".equals(machineInfoList.get(0).getMachineStatus())) {
            return BigDecimal.ZERO;
        }
        LambdaQueryWrapper<TcShiftConfig> shiftWrapper = new LambdaQueryWrapper<>();
        shiftWrapper.eq(TcShiftConfig::getFactoryCode, sourceResult.getFactoryCode());
        shiftWrapper.eq(TcShiftConfig::getShiftOrder, shiftOrder);
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(shiftWrapper);
        if (shiftConfigList == null || shiftConfigList.isEmpty()
                || !"1".equals(shiftConfigList.get(0).getOpenFlag())
                || !this.isMachineShiftOpen(machineInfoList.get(0), shiftConfigList.get(0))) {
            return BigDecimal.ZERO;
        }
        return this.calculateEffectiveCapacity(sourceResult, targetMachineCode,
                shiftConfigList.get(0), machineInfoList.get(0));
    }

    /**
     * 解析人工滚动班次的计划起止时间。
     *
     * @param sourceResult 排程结果参考
     * @param shiftOrder 班次顺序
     * @return 起止时间列表，班次不存在或时间无效时返回空列表
     */
    List<Date> resolveRollingShiftWindow(TcScheduleResult sourceResult, Integer shiftOrder) {
        if (sourceResult == null || sourceResult.getScheduleDate() == null || shiftOrder == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcShiftConfig::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcShiftConfig::getShiftOrder, shiftOrder);
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(wrapper);
        if (shiftConfigList == null || shiftConfigList.isEmpty()
                || StringUtils.isBlank(shiftConfigList.get(0).getPlanStartTime())
                || StringUtils.isBlank(shiftConfigList.get(0).getPlanEndTime())) {
            return Collections.emptyList();
        }
        TcShiftConfig shiftConfig = shiftConfigList.get(0);
        try {
            String scheduleDate = DateUtil.formatDate(sourceResult.getScheduleDate());
            Date startTime = DateUtil.parseDateTime(scheduleDate + " "
                    + this.normalizeClockTime(shiftConfig.getPlanStartTime()));
            Date endTime = DateUtil.parseDateTime(scheduleDate + " "
                    + this.normalizeClockTime(shiftConfig.getPlanEndTime()));
            if ("1".equals(shiftConfig.getCrossDayFlag()) || !endTime.after(startTime)) {
                endTime = DateUtil.offsetDay(endTime, 1);
            }
            return Arrays.asList(startTime, endTime);
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    /**
     * 将 HH:mm 格式补齐为 HH:mm:ss。
     *
     * @param clockTime 时刻文本
     * @return 标准时刻文本
     */
    private String normalizeClockTime(String clockTime) {
        String trimmedClockTime = clockTime.trim();
        return trimmedClockTime.length() == 5 ? trimmedClockTime + ":00" : trimmedClockTime;
    }

    /**
     * 计算单机单班扣除参数上限和维修停机后的有效产能。
     *
     * @param sourceResult 排程结果参考
     * @param targetMachineCode 目标机台编码
     * @param shiftConfig 班次配置
     * @param machineInfo 机台资料
     * @return 有效产能
     */
    private BigDecimal calculateEffectiveCapacity(TcScheduleResult sourceResult, String targetMachineCode,
                                                  TcShiftConfig shiftConfig, TcMachineInfo machineInfo) {
        BigDecimal machineCapacity = machineInfo.getMaxCapacity() == null
                || machineInfo.getMaxCapacity().compareTo(BigDecimal.ZERO) <= 0
                ? new BigDecimal(TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY) : machineInfo.getMaxCapacity();
        BigDecimal shiftLimit = this.loadShiftCapacityLimit(sourceResult);
        BigDecimal effectiveCapacity = machineCapacity.min(shiftLimit);
        BigDecimal maintenanceHours = this.loadMaintenanceHours(sourceResult, targetMachineCode,
                shiftConfig.getShiftCode());
        BigDecimal productSpeed = this.loadProductSpeed(sourceResult, targetMachineCode);
        if (maintenanceHours.compareTo(BigDecimal.ZERO) > 0 && productSpeed.compareTo(BigDecimal.ZERO) > 0) {
            effectiveCapacity = effectiveCapacity.subtract(maintenanceHours.multiply(productSpeed))
                    .max(BigDecimal.ZERO);
        } else if (maintenanceHours.compareTo(BigDecimal.valueOf(
                shiftConfig.getShiftHours() == null ? 0 : shiftConfig.getShiftHours())) >= 0
                && maintenanceHours.compareTo(BigDecimal.ZERO) > 0) {
            effectiveCapacity = BigDecimal.ZERO;
        }
        return effectiveCapacity;
    }

    /**
     * 读取排程日生效的胎侧单班最大可排量参数。
     *
     * @param sourceResult 排程结果
     * @return 生效参数值，未配置或无效时返回 5500 米默认值
     */
    private BigDecimal loadShiftCapacityLimit(TcScheduleResult sourceResult) {
        LambdaQueryWrapper<TcParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcParams::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcParams::getParamCode, TcScheduleConstants.PARAM_SHIFT_MAX_CAPACITY);
        wrapper.eq(TcParams::getEnableStatus, "1");
        List<TcParams> paramsList = this.paramsMapper.selectList(wrapper);
        if (paramsList == null) {
            return new BigDecimal(TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY);
        }
        return paramsList.stream().map(item -> StringUtils.isNotBlank(item.getParamValue())
                        ? item.getParamValue() : item.getDefaultValue())
                .filter(StringUtils::isNotBlank).map(this::parsePositiveCapacity)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0).findFirst()
                .orElseGet(() -> new BigDecimal(TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY));
    }

    /**
     * 将参数文本解析为正数产能。
     *
     * @param value 参数文本
     * @return 解析值，格式非法时返回 0
     */
    private BigDecimal parsePositiveCapacity(String value) {
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 汇总目标机台当前班次已排量。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @param shiftOrder 班次顺序
     * @return 已排计划量
     */
    private BigDecimal loadAssignedPlanQty(TcScheduleResult sourceResult, String targetMachineCode,
                                           Integer shiftOrder) {
        LambdaQueryWrapper<TcScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcScheduleResult::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcScheduleResult::getScheduleDate, sourceResult.getScheduleDate());
        wrapper.eq(TcScheduleResult::getBatchNo, sourceResult.getBatchNo());
        wrapper.eq(TcScheduleResult::getMachineCode, targetMachineCode);
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(wrapper);
        return resultList == null ? BigDecimal.ZERO : resultList.stream()
                .map(item -> this.readPlanQty(item, shiftOrder)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 查询当前机台班次维修停机小时数。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @param shiftCode 班次编码
     * @return 维修小时数
     */
    private BigDecimal loadMaintenanceHours(TcScheduleResult sourceResult, String targetMachineCode,
                                            String shiftCode) {
        LambdaQueryWrapper<TcMachineMaintenance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineMaintenance::getFactoryCode, sourceResult.getFactoryCode());
        wrapper.eq(TcMachineMaintenance::getMachineCode, targetMachineCode);
        List<TcMachineMaintenance> maintenanceList = this.machineMaintenanceMapper.selectList(wrapper);
        return maintenanceList == null ? BigDecimal.ZERO : maintenanceList.stream()
                .filter(item -> item.getStopStartTime() != null
                        && DateUtil.isSameDay(item.getStopStartTime(), sourceResult.getScheduleDate()))
                .filter(item -> StringUtils.isBlank(item.getStopShift())
                        || Objects.equals(item.getStopShift(), shiftCode))
                .map(this::calculateMaintenanceHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 根据维修开始、结束时间计算停机小时数。
     *
     * @param maintenance 维修计划
     * @return 有效停机小时数；时间窗口无效时返回 0
     */
    private BigDecimal calculateMaintenanceHours(TcMachineMaintenance maintenance) {
        if (maintenance.getStopStartTime() == null || maintenance.getStopEndTime() == null
                || !maintenance.getStopEndTime().after(maintenance.getStopStartTime())) {
            return BigDecimal.ZERO;
        }
        long maintenanceMillis = maintenance.getStopEndTime().getTime()
                - maintenance.getStopStartTime().getTime();
        return BigDecimal.valueOf(maintenanceMillis)
                .divide(BigDecimal.valueOf(TcScheduleConstants.MILLIS_PER_HOUR), 4, RoundingMode.HALF_UP);
    }

    /**
     * 查询目标机台生产当前胎侧的速度。
     *
     * @param sourceResult 源排程结果
     * @param targetMachineCode 目标机台
     * @return 生产速度，缺失时为 0
     */
    private BigDecimal loadProductSpeed(TcScheduleResult sourceResult, String targetMachineCode) {
        LambdaQueryWrapper<TcMachineSpeed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcMachineSpeed::getFactoryCode, sourceResult.getFactoryCode());
        List<TcMachineSpeed> speedList = this.machineSpeedMapper.selectList(wrapper);
        if (speedList == null) {
            return BigDecimal.ZERO;
        }
        return speedList.stream().filter(item -> Objects.equals(item.getMachineCode(), targetMachineCode)
                && Objects.equals(item.getSidewallCode(), sourceResult.getSidewallCode()))
                .map(TcMachineSpeed::getProductSpeed).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> speedList.stream()
                        .filter(item -> StringUtils.isBlank(item.getMachineCode())
                                && Objects.equals(item.getSidewallCode(), sourceResult.getSidewallCode()))
                        .map(TcMachineSpeed::getProductSpeed).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> speedList.stream()
                        .filter(item -> Objects.equals(item.getMachineCode(), targetMachineCode)
                                && StringUtils.isBlank(item.getSidewallCode()))
                        .map(TcMachineSpeed::getProductSpeed).filter(Objects::nonNull).findFirst()
                        .orElse(BigDecimal.ZERO)));
    }

    /**
     * 动态读取班次计划量。
     *
     * @param result 排程结果
     * @param shiftOrder 班次顺序
     * @return 非空计划量
     */
    private BigDecimal readPlanQty(TcScheduleResult result, Integer shiftOrder) {
        Object value = result.getFieldValueByFieldName(String.format(
                TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }
}
