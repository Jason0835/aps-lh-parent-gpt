package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmGlueMachineReal;
import com.zlt.aps.tm.api.domain.entity.TmMouthPlate;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.mapper.TmGlueMachineRealMapper;
import com.zlt.aps.tm.mapper.TmMouthPlateMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎面人工排程口型板、胶料机台关系校验器。
 *
 * <p>仅在插单、转入、调增或滚动新增计划量时校验新落点；减量、清零和删除不阻断。
 * 关系语义与自动排程一致：对象存在工厂级关系时按配置机台白名单限制，未配置对象不限制机台。</p>
 */
@Service
public class TmManualMachineRelationValidator {

    private final TmMouthPlateMapper mouthPlateMapper;

    private final TmGlueMachineRealMapper glueMachineRealMapper;

    /**
     * 创建胎面人工机台关系校验器。
     *
     * @param mouthPlateMapper 口型板关系 Mapper
     * @param glueMachineRealMapper 胶料机台关系 Mapper
     */
    public TmManualMachineRelationValidator(TmMouthPlateMapper mouthPlateMapper,
                                            TmGlueMachineRealMapper glueMachineRealMapper) {
        this.mouthPlateMapper = mouthPlateMapper;
        this.glueMachineRealMapper = glueMachineRealMapper;
    }

    /**
     * 校验任务能否在目标机台新增正计划量。
     *
     * @param scheduleResult 待新增或转入的排程结果
     * @param targetMachineCode 目标机台编码
     * @throws ServiceException 口型板缺失或目标机台未命中关系白名单时抛出
     */
    public void validatePlacement(TmScheduleResult scheduleResult, String targetMachineCode) {
        if (scheduleResult == null || StringUtils.isBlank(targetMachineCode)) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.machineRelationInvalid"));
        }
        this.validateMouthPlate(scheduleResult, targetMachineCode);
        this.validateGlueMachine(scheduleResult, targetMachineCode);
    }

    /**
     * 仅在调量请求增加计划量时校验当前机台关系。
     *
     * @param currentResult 当前数据库结果
     * @param requestResult 调量请求
     * @throws ServiceException 调增后当前机台未命中关系白名单时抛出
     */
    public void validateIncrease(TmScheduleResult currentResult, TmScheduleResult requestResult) {
        boolean increased = false;
        for (int shiftOrder = 1; shiftOrder <= TmScheduleConstants.TM_MAX_SHIFT_ORDER; shiftOrder++) {
            if (this.readPlanQty(requestResult, shiftOrder)
                    .compareTo(this.readPlanQty(currentResult, shiftOrder)) > 0) {
                increased = true;
                break;
            }
        }
        if (increased) {
            this.validatePlacement(currentResult, currentResult.getMachineCode());
        }
    }

    /**
     * 校验口型板关系白名单。
     *
     * @param scheduleResult 排程结果
     * @param targetMachineCode 目标机台编码
     * @throws ServiceException 口型板缺失或目标机台未配置该口型板时抛出
     */
    private void validateMouthPlate(TmScheduleResult scheduleResult, String targetMachineCode) {
        if (StringUtils.isBlank(scheduleResult.getMouthPlateCode())) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.mouthPlateInvalid"));
        }
        List<TmMouthPlate> mouthPlateList = this.mouthPlateMapper.selectList(
                new LambdaQueryWrapper<TmMouthPlate>()
                        .eq(TmMouthPlate::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(TmMouthPlate::getPlateStatus, TmYesNoEnum.YES.getCode()));
        List<TmMouthPlate> relevantMouthPlateList = mouthPlateList == null
                ? Collections.emptyList() : mouthPlateList.stream()
                .filter(item -> TmYesNoEnum.YES.getCode().equals(item.getPlateStatus()))
                .filter(item -> Objects.equals(item.getMouthPlateCode(), scheduleResult.getMouthPlateCode()))
                .collect(Collectors.toList());
        if (!relevantMouthPlateList.isEmpty() && relevantMouthPlateList.stream()
                .noneMatch(item -> Objects.equals(item.getMachineCode(), targetMachineCode))) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.mouthPlateRejected"));
        }
    }

    /**
     * 校验主胶料关系白名单。
     *
     * <p>仅启用关系参与判断，allowFlag、基部胶和优先级暂不参与排程匹配。</p>
     *
     * @param scheduleResult 排程结果
     * @param targetMachineCode 目标机台编码
     * @throws ServiceException 目标机台未配置任务主胶料时抛出
     */
    private void validateGlueMachine(TmScheduleResult scheduleResult, String targetMachineCode) {
        List<TmGlueMachineReal> glueRuleList = this.glueMachineRealMapper.selectList(
                new LambdaQueryWrapper<TmGlueMachineReal>()
                        .eq(TmGlueMachineReal::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(TmGlueMachineReal::getEnableStatus, TmYesNoEnum.YES.getCode()));
        List<TmGlueMachineReal> relevantRuleList = glueRuleList == null
                ? Collections.emptyList() : glueRuleList.stream()
                .filter(item -> TmYesNoEnum.YES.getCode().equals(item.getEnableStatus()))
                .filter(item -> Objects.equals(item.getGlueCode(), scheduleResult.getGlueCode()))
                .collect(Collectors.toList());
        if (!relevantRuleList.isEmpty() && relevantRuleList.stream()
                .noneMatch(item -> Objects.equals(item.getMachineCode(), targetMachineCode))) {
            throw new ServiceException(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.glueMachineRejected"));
        }
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
