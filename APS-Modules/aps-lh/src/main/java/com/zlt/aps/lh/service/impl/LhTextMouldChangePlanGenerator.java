package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.dto.LhGenerateTextMouldPlanDTO;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.component.OrderNoGenerator;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.util.LeftRightMouldUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文字示方交替计划生成服务。
 * <p>负责将单条硫化排程结果转换为次日中班第一顺位的模具交替计划。</p>
 *
 * @author Codex
 */
@Slf4j
@Service
public class LhTextMouldChangePlanGenerator {

    /**
     * 中班班次字典值。
     */
    private static final String MIDDLE_SHIFT_CLASS_INDEX = "2";

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Resource
    private LhMouldChangePlanEntityMapper mouldChangePlanMapper;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    /**
     * 生成文字示方交替计划。
     *
     * @param dto 生成入参
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generate(LhGenerateTextMouldPlanDTO dto) {
        if (Objects.isNull(dto) || Objects.isNull(dto.getId())) {
            return AjaxResult.error(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.noData"));
        }

        LhScheduleResult scheduleResult = scheduleResultMapper.selectById(dto.getId());
        if (Objects.isNull(scheduleResult) || !Objects.equals(scheduleResult.getIsDelete(), DeleteFlagEnum.NORMAL.getCode())) {
            return AjaxResult.error(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.scheduleNotExist"));
        }
        if (StringUtils.isNotBlank(dto.getFactoryCode())
                && !StringUtils.equals(dto.getFactoryCode(), scheduleResult.getFactoryCode())) {
            return AjaxResult.error(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.factoryCodeMismatch"));
        }
        // 次日中班计划量不为0，不可生成。
        if (!Objects.equals(scheduleResult.getClass5PlanQty(), 0)) {
            return AjaxResult.error(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.midPlanReqZero"));
        }
        // 次日中班（class5StartTime）开始日期必须等于当前时间+1天，历史记录不可生成。
        String class5StartTimeValidateMessage = validateNextMiddleShiftStartTime(scheduleResult);
        if (StringUtils.isNotBlank(class5StartTimeValidateMessage)) {
            return AjaxResult.error(class5StartTimeValidateMessage);
        }

        String validateMessage = validateScheduleResult(scheduleResult);
        if (StringUtils.isNotBlank(validateMessage)) {
            return AjaxResult.error(validateMessage);
        }

        Date targetScheduleDate = LhScheduleTimeUtil.clearTime(scheduleResult.getClass5StartTime());
        List<LhMouldChangePlan> targetPlans = listTargetMiddleShiftPlans(scheduleResult.getFactoryCode(), targetScheduleDate);

        String releaseValidateMessage = validateTargetPlansCanInsert(targetPlans, scheduleResult.getLhMachineCode());
        if (StringUtils.isNotBlank(releaseValidateMessage)) {
            return AjaxResult.error(releaseValidateMessage);
        }
        if (hasSameMachineUnreleasedPlan(targetPlans, scheduleResult.getLhMachineCode())
                && !Boolean.TRUE.equals(dto.getConfirmReplace())) {
            return AjaxResult.success(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.replaceConfirm"))
                    .put("needConfirm", true);
        }

        removeSameMachinePlans(targetPlans, scheduleResult.getLhMachineCode());
        reorderRemainingPlans(targetPlans);

        LhMouldChangePlan newPlan = buildMouldChangePlan(scheduleResult, targetScheduleDate);
        mouldChangePlanMapper.insert(newPlan);
        log.info("文字示方交替计划生成成功, 排程结果ID: {}, 机台: {}, 目标日期: {}",
                scheduleResult.getId(), scheduleResult.getLhMachineCode(), DateUtil.formatDate(targetScheduleDate));
        return AjaxResult.success(message("ui.data.alert.lhMouldChangePlan.generateTextPlan.success"));
    }

    /**
     * 校验排程结果是否具备生成交替计划的必要字段。
     *
     * @param scheduleResult 排程结果
     * @return 校验失败信息；为空表示通过
     */
    private String validateScheduleResult(LhScheduleResult scheduleResult) {
        if (StringUtils.isBlank(scheduleResult.getFactoryCode())) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.factoryCodeRequired");
        }
        if (Objects.isNull(scheduleResult.getScheduleDate())) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.scheduleDateRequired");
        }
        if (StringUtils.isBlank(scheduleResult.getLhMachineCode())) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.machineCodeRequired");
        }
        if (StringUtils.isBlank(scheduleResult.getMaterialCode())) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.materialCodeRequired");
        }
        return null;
    }

    /**
     * 校验次日中班开始时间是否为当前日期的下一天。
     *
     * @param scheduleResult 排程结果
     * @return 校验失败信息；为空表示通过
     */
    private String validateNextMiddleShiftStartTime(LhScheduleResult scheduleResult) {
        if (Objects.isNull(scheduleResult.getClass5StartTime())) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.class5StartTimeRequired");
        }
        Date expectedTargetDate = LhScheduleTimeUtil.addDays(LhScheduleTimeUtil.clearTime(new Date()), 1);
        Date class5StartDate = LhScheduleTimeUtil.clearTime(scheduleResult.getClass5StartTime());
        if (!DateUtil.isSameDay(class5StartDate, expectedTargetDate)) {
            return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.onlyTomorrowAllowed");
        }
        return null;
    }

    /**
     * 查询目标日中班的交替计划列表。
     *
     * @param factoryCode 分厂编号
     * @param targetScheduleDate 目标排程日期
     * @return 目标时段交替计划列表
     */
    private List<LhMouldChangePlan> listTargetMiddleShiftPlans(String factoryCode, Date targetScheduleDate) {
        return Optional.ofNullable(mouldChangePlanMapper.selectList(new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .eq(LhMouldChangePlan::getScheduleDate, targetScheduleDate)
                        .eq(LhMouldChangePlan::getClassIndex, MIDDLE_SHIFT_CLASS_INDEX)
                        .eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(LhMouldChangePlan::getPlanOrder)
                        .orderByAsc(LhMouldChangePlan::getLhMachineCode)
                        .orderByAsc(LhMouldChangePlan::getId)))
                .orElseGet(java.util.ArrayList::new);
    }

    /**
     * 校验目标时段计划是否允许前插到第一顺位。
     *
     * @param targetPlans 目标时段计划
     * @param machineCode 当前机台编码
     * @return 校验失败信息；为空表示通过
     */
    private String validateTargetPlansCanInsert(List<LhMouldChangePlan> targetPlans, String machineCode) {
        for (LhMouldChangePlan targetPlan : targetPlans) {
            if (!StringUtils.equals(ReleaseStatusEnum.NOT_RELEASED.getCode(), targetPlan.getIsRelease())) {
                if (StringUtils.equals(machineCode, targetPlan.getLhMachineCode())) {
                    return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.sameMachineReleasedExists");
                }
                return message("ui.data.alert.lhMouldChangePlan.generateTextPlan.targetShiftReleasedExists");
            }
        }
        return null;
    }

    /**
     * 判断目标时段内是否存在同机台未发布计划。
     *
     * @param targetPlans 目标时段计划
     * @param machineCode 当前机台编码
     * @return true-存在可替换旧计划
     */
    private boolean hasSameMachineUnreleasedPlan(List<LhMouldChangePlan> targetPlans, String machineCode) {
        return targetPlans.stream().anyMatch(plan -> StringUtils.equals(machineCode, plan.getLhMachineCode())
                && StringUtils.equals(ReleaseStatusEnum.NOT_RELEASED.getCode(), plan.getIsRelease()));
    }

    /**
     * 删除目标时段下同机台的旧计划。
     *
     * @param targetPlans 目标时段计划
     * @param machineCode 当前机台编码
     */
    private void removeSameMachinePlans(List<LhMouldChangePlan> targetPlans, String machineCode) {
        List<LhMouldChangePlan> sameMachinePlans = targetPlans.stream()
                .filter(plan -> StringUtils.equals(machineCode, plan.getLhMachineCode()))
                .collect(Collectors.toList());
        for (LhMouldChangePlan sameMachinePlan : sameMachinePlans) {
            mouldChangePlanMapper.deleteById(sameMachinePlan.getId());
        }
        targetPlans.removeIf(plan -> StringUtils.equals(machineCode, plan.getLhMachineCode()));
    }

    /**
     * 将剩余目标时段计划整体顺延到新计划之后。
     *
     * @param targetPlans 需要顺延的计划列表
     */
    private void reorderRemainingPlans(List<LhMouldChangePlan> targetPlans) {
        List<LhMouldChangePlan> sortedPlans = targetPlans.stream()
                .sorted(Comparator.comparing(LhMouldChangePlan::getPlanOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LhMouldChangePlan::getLhMachineCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(LhMouldChangePlan::getId, Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
        int nextOrder = 2;
        for (LhMouldChangePlan sortedPlan : sortedPlans) {
            sortedPlan.setPlanOrder(nextOrder++);
            mouldChangePlanMapper.updateById(sortedPlan);
        }
    }

    /**
     * 构建新的交替计划实体。
     *
     * @param scheduleResult 源排程结果
     * @param targetScheduleDate 目标排程日期
     * @return 新交替计划
     */
    private LhMouldChangePlan buildMouldChangePlan(LhScheduleResult scheduleResult, Date targetScheduleDate) {
        LhMouldChangePlan plan = new LhMouldChangePlan();
        plan.setFactoryCode(scheduleResult.getFactoryCode());
        plan.setLhResultBatchNo(scheduleResult.getBatchNo());
        plan.setOrderNo(orderNoGenerator.generateMouldChangeOrderNo(targetScheduleDate));
        plan.setScheduleDate(targetScheduleDate);
        plan.setPlanDate(targetScheduleDate);
        plan.setPlanOrder(1);
        plan.setClassIndex(MIDDLE_SHIFT_CLASS_INDEX);
        plan.setLhMachineCode(scheduleResult.getLhMachineCode());
        plan.setLhMachineName(scheduleResult.getLhMachineName());
        plan.setLeftRightMould(LeftRightMouldUtil.resolveLeftRightMould(
                scheduleResult.getLeftRightMould(), scheduleResult.getLhMachineCode()));
        plan.setAfterMaterialCode(scheduleResult.getMaterialCode());
        plan.setAfterMaterialDesc(scheduleResult.getMaterialDesc());
        plan.setMouldCode(scheduleResult.getMouldCode());
        plan.setChangeMouldType(determineChangeMouldType(scheduleResult));
        plan.setChangeTime(LhScheduleTimeUtil.getAfternoonShiftStart(null, targetScheduleDate));
        plan.setIsRelease(ReleaseStatusEnum.NOT_RELEASED.getCode());
        plan.setMouldStatus(ApsConstant.STATUS_ENABLE);
        plan.setIsDelete(DeleteFlagEnum.NORMAL.getCode());
        plan.setRemark(message("ui.data.column.lhMouldChangePlan.generateTextPlanRemark"));
        fillBeforeMaterial(plan, scheduleResult, targetScheduleDate);
        return plan;
    }

    /**
     * 统一读取国际化文案，避免业务代码中散落硬编码提示。
     *
     * @param key 国际化键
     * @return 国际化文案
     */
    private String message(String key) {
        return I18nUtil.getMessage(key);
    }

    /**
     * 补齐前规格信息。
     * <p>优先读取最近一条有效交替计划的后规格；若未命中，则回退到最近一条其他排程结果。</p>
     *
     * @param plan 新交替计划
     * @param scheduleResult 源排程结果
     * @param targetScheduleDate 目标排程日期
     */
    private void fillBeforeMaterial(LhMouldChangePlan plan, LhScheduleResult scheduleResult, Date targetScheduleDate) {
        LhMouldChangePlan previousPlan = Optional.ofNullable(mouldChangePlanMapper.selectList(new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(LhMouldChangePlan::getLhMachineCode, scheduleResult.getLhMachineCode())
                        .lt(LhMouldChangePlan::getScheduleDate, targetScheduleDate)
                        .eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .orderByDesc(LhMouldChangePlan::getScheduleDate)
                        .orderByDesc(LhMouldChangePlan::getPlanOrder)
                        .orderByDesc(LhMouldChangePlan::getId)))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        if (Objects.nonNull(previousPlan)) {
            plan.setBeforeMaterialCode(StringUtils.defaultIfBlank(previousPlan.getAfterMaterialCode(), previousPlan.getBeforeMaterialCode()));
            plan.setBeforeMaterialDesc(StringUtils.defaultIfBlank(previousPlan.getAfterMaterialDesc(), previousPlan.getBeforeMaterialDesc()));
            return;
        }

        LhScheduleResult previousResult = Optional.ofNullable(scheduleResultMapper.selectList(new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getFactoryCode, scheduleResult.getFactoryCode())
                        .eq(LhScheduleResult::getLhMachineCode, scheduleResult.getLhMachineCode())
                        .lt(LhScheduleResult::getScheduleDate, targetScheduleDate)
                        .ne(LhScheduleResult::getId, scheduleResult.getId())
                        .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .orderByDesc(LhScheduleResult::getScheduleDate)
                        .orderByDesc(LhScheduleResult::getSpecEndTime)
                        .orderByDesc(LhScheduleResult::getId)))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        if (Objects.nonNull(previousResult)) {
            plan.setBeforeMaterialCode(previousResult.getMaterialCode());
            plan.setBeforeMaterialDesc(previousResult.getMaterialDesc());
            return;
        }

        log.warn("文字示方交替计划未找到前规格参考数据, 排程结果ID: {}, 机台: {}, 目标日期: {}",
                scheduleResult.getId(), scheduleResult.getLhMachineCode(), DateUtil.formatDate(targetScheduleDate));
    }

    /**
     * 按既有规则判定换模类型。
     *
     * @param scheduleResult 源排程结果
     * @return 换模类型编码
     */
    private String determineChangeMouldType(LhScheduleResult scheduleResult) {
        if ("1".equals(scheduleResult.getIsTypeBlock())) {
            return "02";
        }
        if ("02".equals(scheduleResult.getScheduleType())) {
            return "01";
        }
        return "01";
    }
}
