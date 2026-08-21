package com.zlt.aps.tm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.MachineOpenShiftCodeUtil;
import com.zlt.aps.common.core.utils.SixShiftWorkCalendarUtil;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.api.enums.TmProcessCodeEnum;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;
import com.zlt.aps.tm.mapper.TmAutoScheduleDataLoadMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工排程机台开机班次校验器。
 *
 * <p>统一校验插单、加量和转入目标机台时的开机班次，并为人工滚动提供班次基础产能。
 * 减量、清零、删除和转出不在本类阻断，便于修复历史未开班计划。</p>
 */
@Service
public class TmMachineOpenShiftValidator {

    private final TmMachineInfoMapper machineInfoMapper;

    private final TmShiftConfigMapper shiftConfigMapper;

    private final TmAutoScheduleDataLoadMapper autoScheduleDataLoadMapper;

    /**
     * 创建机台开机班次校验器。
     *
     * @param machineInfoMapper 机台资料 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     * @param autoScheduleDataLoadMapper 工作日历查询 Mapper
     */
    public TmMachineOpenShiftValidator(TmMachineInfoMapper machineInfoMapper,
                                       TmShiftConfigMapper shiftConfigMapper,
                                       TmAutoScheduleDataLoadMapper autoScheduleDataLoadMapper) {
        this.machineInfoMapper = machineInfoMapper;
        this.shiftConfigMapper = shiftConfigMapper;
        this.autoScheduleDataLoadMapper = autoScheduleDataLoadMapper;
    }

    /**
     * 校验插单中全部正计划量班次均为目标机台开机班次。
     *
     * @param scheduleResult 插单结果
     * @throws ServiceException 机台或班次不存在、当前班次未开机时抛出
     */
    public void validateInsert(TmScheduleResult scheduleResult) {
        this.validatePositivePlanShifts(scheduleResult, scheduleResult.getMachineCode(),
                new TmMachineOpenShiftValidationCache());
    }

    /**
     * 校验转入目标机台的全部正计划量班次。
     *
     * @param sourceResult 当前数据库结果
     * @param targetMachineCode 目标机台编码
     * @throws ServiceException 目标机台对应班次未开机时抛出
     */
    public void validateTransfer(TmScheduleResult sourceResult, String targetMachineCode) {
        this.validatePositivePlanShifts(sourceResult, targetMachineCode,
                new TmMachineOpenShiftValidationCache());
    }

    /**
     * 仅校验人工调量中增加的计划量班次。
     *
     * @param currentResult 当前数据库结果
     * @param requestResult 调量请求
     * @throws ServiceException 加量班次未开机时抛出
     */
    public void validateIncrease(TmScheduleResult currentResult, TmScheduleResult requestResult) {
        TmMachineOpenShiftValidationCache validationCache = new TmMachineOpenShiftValidationCache();
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            BigDecimal currentQty = this.readPlanQty(currentResult, shiftOrder);
            BigDecimal requestQty = this.readPlanQty(requestResult, shiftOrder);
            if (requestQty.compareTo(currentQty) > 0) {
                this.requireWorkCalendarShiftOpen(currentResult, shiftOrder, validationCache);
                this.requireMachineShiftOpen(currentResult.getFactoryCode(), currentResult.getMachineCode(), shiftOrder,
                        validationCache);
            }
        }
    }

    /**
     * 解析人工滚动指定机台班次的基础产能。
     *
     * @param reference 排程范围参考
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     * @return 已开班返回机台最大班产，未开班或资料缺失返回零
     */
    public BigDecimal resolveRollingCapacity(TmScheduleResult reference, String machineCode, Integer shiftOrder) {
        if (reference == null || StrUtil.isBlank(machineCode) || shiftOrder == null) {
            return BigDecimal.ZERO;
        }
        TmMachineOpenShiftValidationCache validationCache = new TmMachineOpenShiftValidationCache();
        if (!this.isWorkCalendarShiftOpen(reference, shiftOrder, validationCache)) {
            return BigDecimal.ZERO;
        }
        TmMachineInfo machineInfo = this.loadMachine(reference.getFactoryCode(), machineCode, validationCache);
        TmShiftConfig shiftConfig = this.loadShiftConfig(reference.getFactoryCode(), shiftOrder, validationCache);
        if (machineInfo == null || shiftConfig == null || !"1".equals(machineInfo.getMachineStatus())
                || !this.isMachineShiftOpen(machineInfo, shiftConfig)) {
            return BigDecimal.ZERO;
        }
        return machineInfo.getMaxCapacity() == null ? BigDecimal.ZERO : machineInfo.getMaxCapacity();
    }

    /**
     * 校验结果中的正计划量班次。
     *
     * @param scheduleResult 排程结果
     * @param machineCode 待承接机台编码
     * @param validationCache 本次调用的查询缓存
     */
    private void validatePositivePlanShifts(TmScheduleResult scheduleResult, String machineCode,
                                            TmMachineOpenShiftValidationCache validationCache) {
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.readPlanQty(scheduleResult, shiftOrder).compareTo(BigDecimal.ZERO) > 0) {
                this.requireWorkCalendarShiftOpen(scheduleResult, shiftOrder, validationCache);
                this.requireMachineShiftOpen(scheduleResult.getFactoryCode(), machineCode, shiftOrder, validationCache);
            }
        }
    }

    /**
     * 要求机台开放指定班次。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @param shiftOrder 班次顺序
     * @param validationCache 本次调用的查询缓存
     * @throws ServiceException 机台或班次不存在、班次未开放时抛出
     */
    private void requireMachineShiftOpen(String factoryCode, String machineCode, Integer shiftOrder,
                                         TmMachineOpenShiftValidationCache validationCache) {
        TmMachineInfo machineInfo = this.loadMachine(factoryCode, machineCode, validationCache);
        TmShiftConfig shiftConfig = this.loadShiftConfig(factoryCode, shiftOrder, validationCache);
        if (machineInfo == null || shiftConfig == null || !"1".equals(machineInfo.getMachineStatus())
                || !this.isMachineShiftOpen(machineInfo, shiftConfig)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.machineShiftClosed"));
        }
    }

    /**
     * 要求胎面工作日历开放指定结果班次。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     结果班次顺序
     * @param validationCache 本次调用的查询缓存
     * @throws ServiceException 工作日历停产或查询异常时抛出
     */
    private void requireWorkCalendarShiftOpen(TmScheduleResult scheduleResult, Integer shiftOrder,
                                              TmMachineOpenShiftValidationCache validationCache) {
        if (!this.isWorkCalendarShiftOpen(scheduleResult, shiftOrder, validationCache)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.workCalendarShiftStopped"));
        }
    }

    /**
     * 查询胎面工作日历并判断结果班次是否开放。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder     结果班次顺序
     * @param validationCache 本次调用的查询缓存
     * @return true表示开放，日历缺失或班次标志为空时兼容为开放
     * @throws ServiceException 工作日历查询异常时抛出
     */
    private boolean isWorkCalendarShiftOpen(TmScheduleResult scheduleResult, Integer shiftOrder,
                                            TmMachineOpenShiftValidationCache validationCache) {
        if (scheduleResult == null || scheduleResult.getScheduleDate() == null || shiftOrder == null) {
            return false;
        }
        try {
            Date productionDate = SixShiftWorkCalendarUtil.resolveProductionDate(
                    scheduleResult.getScheduleDate(), shiftOrder);
            List<TmWorkCalendarRowVo> calendarList = this.loadWorkCalendar(scheduleResult.getFactoryCode(),
                    productionDate, validationCache);
            if (calendarList == null || calendarList.isEmpty()) {
                return true;
            }
            TmWorkCalendarRowVo calendar = calendarList.get(0);
            if ("0".equals(calendar.getDayFlag())) {
                return false;
            }
            int calendarShiftOrder = SixShiftWorkCalendarUtil.resolveCalendarShiftOrder(shiftOrder);
            String shiftFlag = calendarShiftOrder == 1 ? calendar.getOneShiftFlag()
                    : (calendarShiftOrder == 2 ? calendar.getTwoShiftFlag() : calendar.getThreeShiftFlag());
            return shiftFlag == null || !"0".equals(shiftFlag);
        } catch (RuntimeException exception) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.workCalendarQueryFailed"), exception);
        }
    }

    /**
     * 查询并缓存同一调用内相同生产日的工作日历。
     *
     * @param factoryCode 工厂编码
     * @param productionDate 生产日期
     * @param validationCache 本次调用的查询缓存
     * @return 工作日历列表，未查询到时返回 null 或空列表，保持 Mapper 原有语义
     */
    private List<TmWorkCalendarRowVo> loadWorkCalendar(String factoryCode, Date productionDate,
                                                        TmMachineOpenShiftValidationCache validationCache) {
        if (validationCache.containsWorkCalendar(factoryCode, productionDate)) {
            return validationCache.getWorkCalendar(factoryCode, productionDate);
        }
        List<TmWorkCalendarRowVo> calendarList = this.autoScheduleDataLoadMapper.selectWorkCalendarRows(
                factoryCode, TmProcessCodeEnum.TREAD.getCode(), productionDate);
        validationCache.cacheWorkCalendar(factoryCode, productionDate, calendarList);
        return calendarList;
    }

    /**
     * 查询机台资料。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @param validationCache 本次调用的查询缓存
     * @return 机台资料，不存在时返回 null
     */
    private TmMachineInfo loadMachine(String factoryCode, String machineCode,
                                      TmMachineOpenShiftValidationCache validationCache) {
        if (validationCache.containsMachine(factoryCode, machineCode)) {
            return validationCache.getMachine(factoryCode, machineCode);
        }
        List<TmMachineInfo> machineInfoList = this.machineInfoMapper.selectList(
                new LambdaQueryWrapper<TmMachineInfo>()
                        .eq(TmMachineInfo::getFactoryCode, factoryCode)
                        .eq(TmMachineInfo::getMachineCode, StrUtil.trim(machineCode)));
        TmMachineInfo machineInfo = machineInfoList == null || machineInfoList.isEmpty() ? null : machineInfoList.get(0);
        validationCache.cacheMachine(factoryCode, machineCode, machineInfo);
        return machineInfo;
    }

    /**
     * 查询班次配置。
     *
     * @param factoryCode 工厂编码
     * @param shiftOrder 班次顺序
     * @param validationCache 本次调用的查询缓存
     * @return 班次配置，不存在时返回 null
     */
    private TmShiftConfig loadShiftConfig(String factoryCode, Integer shiftOrder,
                                          TmMachineOpenShiftValidationCache validationCache) {
        if (validationCache.containsShiftConfig(factoryCode, shiftOrder)) {
            return validationCache.getShiftConfig(factoryCode, shiftOrder);
        }
        List<TmShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(
                new LambdaQueryWrapper<TmShiftConfig>()
                        .eq(TmShiftConfig::getFactoryCode, factoryCode)
                        .eq(TmShiftConfig::getShiftOrder, shiftOrder));
        TmShiftConfig shiftConfig = shiftConfigList == null || shiftConfigList.isEmpty() ? null : shiftConfigList.get(0);
        validationCache.cacheShiftConfig(factoryCode, shiftOrder, shiftConfig);
        return shiftConfig;
    }

    /**
     * 判断机台是否开放指定班次编码。
     *
     * @param machineInfo 机台资料
     * @param shiftConfig 班次配置
     * @return true 表示机台允许当前班次
     */
    private boolean isMachineShiftOpen(TmMachineInfo machineInfo, TmShiftConfig shiftConfig) {
        if (machineInfo == null || shiftConfig == null || StrUtil.isBlank(shiftConfig.getShiftCode())) {
            return false;
        }
        Set<String> openShiftCodes = StrUtil.isBlank(machineInfo.getOpenShiftCode())
                ? Collections.emptySet() : Arrays.stream(machineInfo.getOpenShiftCode().split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return MachineOpenShiftCodeUtil.isMachineShiftOpen(openShiftCodes, shiftConfig.getShiftCode());
    }

    /**
     * 读取指定班次计划量。
     *
     * @param scheduleResult 排程结果
     * @param shiftOrder 班次顺序
     * @return 非空计划量
     */
    private BigDecimal readPlanQty(TmScheduleResult scheduleResult, int shiftOrder) {
        if (scheduleResult == null) {
            return BigDecimal.ZERO;
        }
        Object value = scheduleResult.getFieldValueByFieldName(
                String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder));
        return value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.ZERO;
    }
}
