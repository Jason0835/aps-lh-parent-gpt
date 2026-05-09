package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.mapper.LhMoldAlterPlanFinishMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhParamsMapper;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.maindata.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mp.api.domain.entity.MdmSkuConstructionRef;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * 换模开产增加计划服务。
 * <p>负责校验模具交替计划完成状态，并在完成后回写当前排程记录中班及后续班次计划量。</p>
 *
 * @author Codex
 */
@Slf4j
@Service
public class LhIncreaseMouldStartPlanService {

    /**
     * 模具交替完成状态：已完成。
     */
    private static final String MOULD_FINISH_COMPLETED = ApsConstant.APS_STRING_1;

    /**
     * 中班起始班次索引。
     */
    private static final int MIDDLE_SHIFT_INDEX = 5;

    /**
     * 中班班次字典值。
     */
    private static final String MIDDLE_SHIFT_CLASS_INDEX = "03";

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Resource
    private LhMouldChangePlanEntityMapper mouldChangePlanMapper;

    @Resource
    private LhMoldAlterPlanFinishMapper moldAlterPlanFinishMapper;

    @Resource
    private LhParamsMapper lhParamsMapper;

    @Resource
    private MdmSkuConstructionRefEntityMapper mdmSkuConstructionRefEntityMapper;

    /**
     * 换模开产增加计划。
     *
     * @param scheduleResult 前端传入的当前排程结果
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult increase(LhScheduleResult scheduleResult) {
        if (Objects.isNull(scheduleResult) || Objects.isNull(scheduleResult.getId())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.chooseRecord"));
        }

        LhScheduleResult currentResult = scheduleResultMapper.selectById(scheduleResult.getId());
        if (Objects.isNull(currentResult)
                || !Objects.equals(currentResult.getIsDelete(), DeleteFlagEnum.NORMAL.getCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.recordNotExist"));
        }
        if (StringUtils.isNotBlank(scheduleResult.getFactoryCode())
                && !StringUtils.equals(scheduleResult.getFactoryCode(), currentResult.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.factoryNotMatch"));
        }

        String companyCode = currentResult.getFactoryCode();
        LhMouldChangePlan mouldChangePlan = getCurrentMouldChangePlan(currentResult, companyCode);
        if (!isMouldPlanCompleted(mouldChangePlan)) {
            syncMouldChangePlanFinishStatus(currentResult, companyCode);
            mouldChangePlan = getCurrentMouldChangePlan(currentResult, companyCode);
        }
        if (!isMouldPlanCompleted(mouldChangePlan)) {
            return AjaxResult.error(String.format(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.mouldStatusUnfinished"),
                    DateUtil.formatDate(DateUtil.offsetDay(scheduleResult.getScheduleDate(), -1)), currentResult.getLhMachineCode()));
        }

        recalculateShiftPlanQty(currentResult);
        int updateCount = scheduleResultMapper.updateById(currentResult);
        if (updateCount <= 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.fail"));
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.success"));
    }

    /**
     * 查询当前排程对应的模具交替计划。
     *
     * @param scheduleResult 排程结果
     * @param companyCode    分公司编码
     * @return 模具交替计划，不存在返回 null
     */
    private LhMouldChangePlan getCurrentMouldChangePlan(LhScheduleResult scheduleResult, String companyCode) {
        return mouldChangePlanMapper.selectOne(new LambdaQueryWrapper<LhMouldChangePlan>()
                .eq(LhMouldChangePlan::getFactoryCode, scheduleResult.getFactoryCode())
                .eq(LhMouldChangePlan::getScheduleDate, scheduleResult.getScheduleDate())
                .eq(LhMouldChangePlan::getLhMachineCode, scheduleResult.getLhMachineCode())
                .eq(LhMouldChangePlan::getClassIndex, MIDDLE_SHIFT_CLASS_INDEX)
                .eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
    }

    /**
     * 同步模具交替计划完成状态。
     * <p>若完成回报表中已存在“已完成”记录，则将对应换模计划的完成状态更新为已完成。</p>
     *
     * @param scheduleResult 当前排程结果
     * @param companyCode    分公司编码
     */
    private void syncMouldChangePlanFinishStatus(LhScheduleResult scheduleResult, String companyCode) {
        DateTime scheduleDate = DateUtil.offsetDay(scheduleResult.getScheduleDate(), -1);
        LhMoldAlterPlanFinish finishRecord = moldAlterPlanFinishMapper.selectOne(new LambdaQueryWrapper<LhMoldAlterPlanFinish>()
                .eq(LhMoldAlterPlanFinish::getFactoryCode, scheduleResult.getFactoryCode())
                .eq(StringUtils.isNotBlank(companyCode), LhMoldAlterPlanFinish::getCompanyCode, companyCode)
                .eq(LhMoldAlterPlanFinish::getScheduleDate, scheduleDate)
                .eq(LhMoldAlterPlanFinish::getFinishStatus, MOULD_FINISH_COMPLETED)
                .eq(LhMoldAlterPlanFinish::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (Objects.isNull(finishRecord)) {
            return;
        }

        int update = mouldChangePlanMapper.update(null, new LambdaUpdateWrapper<LhMouldChangePlan>()
                .set(LhMouldChangePlan::getMouldStatus, MOULD_FINISH_COMPLETED)
                .eq(LhMouldChangePlan::getFactoryCode, finishRecord.getFactoryCode())
                .eq(LhMouldChangePlan::getScheduleDate, finishRecord.getScheduleDate())
                .eq(LhMouldChangePlan::getOrderNo, finishRecord.getOrderNo())
                .eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
    }

    /**
     * 判断模具交替计划是否已完成。
     *
     * @param mouldChangePlan 模具交替计划
     * @return true-已完成
     */
    private boolean isMouldPlanCompleted(LhMouldChangePlan mouldChangePlan) {
        return Objects.nonNull(mouldChangePlan)
                && StringUtils.equals(MOULD_FINISH_COMPLETED, mouldChangePlan.getMouldStatus());
    }

    /**
     * 重算当前排程记录从中班开始的班次计划量。
     * <p>计算口径分为两段：</p>
     * <p>1. 先根据“中班开始时间 + 换模总耗时”得到实际开产时间，再用中班剩余可生产秒数折算中班计划量。</p>
     * <p>2. 后续班次不再按班次时长逐班折算，而是基于“当前累计已分配量”继续补齐后续班次计划量。</p>
     * <p>最后通过 {@link ShiftFieldUtil#syncDailyPlanQty(LhScheduleResult)} 将 1～8 班计划量重新汇总到日计划量。</p>
     *
     * @param scheduleResult 当前排程结果
     */
    private void recalculateShiftPlanQty(LhScheduleResult scheduleResult) {
        validateCalculationFields(scheduleResult);
        syncConstructionStage(scheduleResult);

        BigDecimal mouldChangeHours = getMouldChangeTotalHours(scheduleResult.getFactoryCode());
        Date middleShiftStartTime = Optional.ofNullable(ShiftFieldUtil.getShiftStartTime(scheduleResult, MIDDLE_SHIFT_INDEX))
                .orElseGet(() -> LhScheduleTimeUtil.getAfternoonShiftStart(null, scheduleResult.getScheduleDate()));
        Date middleShiftEndTime = ShiftFieldUtil.getShiftEndTime(scheduleResult, MIDDLE_SHIFT_INDEX);
        if (Objects.isNull(middleShiftEndTime)) {
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.middleShiftEndTimeEmpty"));
        }

        // 将换模总耗时从“小时”换算成“秒”，再叠加到中班开始时间上，得到换模结束后的真实开产时间。
        long mouldChangeSeconds = mouldChangeHours.multiply(BigDecimal.valueOf(3600L)).longValue();
        Date productionStartTime = new Date(middleShiftStartTime.getTime() + mouldChangeSeconds * 1000L);
        // 中班计划量只看“换模完成后到中班结束前”还能完整覆盖多少模次。
        int middleShiftPlanQty = calculateMiddleShiftPlanQty(productionStartTime, middleShiftEndTime, scheduleResult.getLhTime());
        ShiftFieldUtil.setShiftPlanQty(scheduleResult, MIDDLE_SHIFT_INDEX, middleShiftPlanQty,
                ShiftFieldUtil.getShiftStartTime(scheduleResult, MIDDLE_SHIFT_INDEX),
                ShiftFieldUtil.getShiftEndTime(scheduleResult, MIDDLE_SHIFT_INDEX));

        // allocatedQty 表示从中班开始已经累计回写到结果行的计划量，后续班次都基于这个累计值继续推算。
        int allocatedQty = middleShiftPlanQty;
        int lastUpdatedShiftIndex = middleShiftPlanQty > 0 ? MIDDLE_SHIFT_INDEX : -1;
        for (int shiftIndex = MIDDLE_SHIFT_INDEX + 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            // 后续班次沿用当前实现口径：不再看班次时长，只按“剩余量 + 单班产能规则”直接回写计划量。
            Integer planQty = calculateFollowingShiftPlanQty(scheduleResult, allocatedQty);
            ShiftFieldUtil.setShiftPlanQty(scheduleResult, shiftIndex, planQty,
                    ShiftFieldUtil.getShiftStartTime(scheduleResult, shiftIndex),
                    ShiftFieldUtil.getShiftEndTime(scheduleResult, shiftIndex));
            if (Objects.nonNull(planQty) && planQty > 0) {
                lastUpdatedShiftIndex = shiftIndex;
            }
            allocatedQty += Optional.ofNullable(planQty).orElse(0);
        }

        Integer lastPlanQty = lastUpdatedShiftIndex > 0
                ? ShiftFieldUtil.getShiftPlanQty(scheduleResult, lastUpdatedShiftIndex)
                : null;
        // 最后一个实际承接生产的班次若未达到单班标准产能，则视为收尾班次，标记为尾规格。
        if (Objects.nonNull(lastPlanQty) && lastPlanQty < scheduleResult.getSingleMouldShiftQty()) {
            scheduleResult.setIsEnd(ApsConstant.TRUE);
        }
        // 重新汇总 1～8 班计划量，保证 DAILY_PLAN_QTY 与班次字段保持一致。
        ShiftFieldUtil.syncDailyPlanQty(scheduleResult);
    }

    /**
     * 同步硫化结果的示方类型。
     * <p>按分厂、物料编码查询 SKU 与施工（示方书）关系，取第一条数据的产品状态并转换成施工阶段后回写到硫化结果。</p>
     *
     * @param scheduleResult 当前排程结果
     */
    private void syncConstructionStage(LhScheduleResult scheduleResult) {
        if (StringUtils.isAnyBlank(scheduleResult.getFactoryCode(), scheduleResult.getMaterialCode())) {
            return;
        }
        MdmSkuConstructionRef skuConstructionRef = Optional.ofNullable(mdmSkuConstructionRefEntityMapper.selectList(
                        new LambdaQueryWrapper<MdmSkuConstructionRef>()
                                .eq(MdmSkuConstructionRef::getFactoryCode, scheduleResult.getFactoryCode())
                                .eq(MdmSkuConstructionRef::getMaterialCode, scheduleResult.getMaterialCode())
                                .eq(MdmSkuConstructionRef::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .orderByAsc(MdmSkuConstructionRef::getId)))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        if (Objects.isNull(skuConstructionRef)) {
            return;
        }
        // 主数据表当前保存的是产品状态标记（S/T/X），这里按项目既有枚举口径转换成硫化结果使用的施工阶段（03/02/01/00）。
        scheduleResult.setConstructionStage(
                ConstructionStageEnum.matchByMarkFlag(skuConstructionRef.getTrialStatus()).getStage());
    }

    /**
     * 计算中班计划量。
     * <p>当前实现按“中班剩余可生产秒数 / 单模硫化时长”向上取整。</p>
     * <p>也就是说，只要班次尾部还能容纳 1 模的部分生产窗口，就会按 1 模计入计划量。</p>
     *
     * @param productionStartTime 换模完成后的开产时间
     * @param shiftEndTime        中班结束时间
     * @param lhTimeSeconds       硫化时长（秒）
     * @return 中班计划量
     */
    private int calculateMiddleShiftPlanQty(Date productionStartTime, Date shiftEndTime, Integer lhTimeSeconds) {
        long availableSeconds = Math.max(0L, LhScheduleTimeUtil.diffSeconds(productionStartTime, shiftEndTime));
        if (availableSeconds <= 0L) {
            return 0;
        }
        return BigDecimal.valueOf(availableSeconds)
                .divide(BigDecimal.valueOf(lhTimeSeconds), 0, RoundingMode.CEILING)
                .intValue();
    }

    /**
     * 计算后续班次计划量。
     * <p>当前实现不再按后续班次的起止时间重新折算可生产模次，而是只根据已分配量反推剩余量。</p>
     * <p>当仍有剩余量时，返回值取“剩余量”和“单班硫化量”的较小值；当无剩余量时返回 0。</p>
     * <p>该口径用于保证后续班次回写不会超过硫化余量，同时单班计划量也不会超过单班标准产能。</p>
     *
     * @param scheduleResult 当前排程结果
     * @param allocatedQty   已分配计划量累计
     * @return 班次计划量
     */
    private Integer calculateFollowingShiftPlanQty(LhScheduleResult scheduleResult, int allocatedQty) {
        int remainingQty = scheduleResult.getMouldSurplusQty() - allocatedQty;
        if (remainingQty <= 0) {
            return 0;
        }
        return Math.min(remainingQty, scheduleResult.getSingleMouldShiftQty());
    }

    /**
     * 读取换模总耗时参数。
     *
     * @param factoryCode 分厂编码
     * @return 换模总耗时（小时）
     */
    private BigDecimal getMouldChangeTotalHours(String factoryCode) {
        LhParams params = lhParamsMapper.selectOne(new LambdaQueryWrapper<LhParams>()
                .eq(LhParams::getFactoryCode, factoryCode)
                .eq(LhParams::getParamCode, LhScheduleParamConstant.MOULD_CHANGE_TOTAL_HOURS)
                .eq(LhParams::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .last("LIMIT 1"));
        if (Objects.isNull(params) || StringUtils.isBlank(params.getParamValue())) {
            throw new IllegalStateException(String.format(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.paramMissing"),
                    LhScheduleParamConstant.MOULD_CHANGE_TOTAL_HOURS));
        }
        try {
            return new BigDecimal(params.getParamValue().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(String.format(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.paramInvalid"),
                    LhScheduleParamConstant.MOULD_CHANGE_TOTAL_HOURS, params.getParamValue()), ex);
        }
    }

    /**
     * 校验计划量计算的必要字段。
     *
     * @param scheduleResult 当前排程结果
     */
    private void validateCalculationFields(LhScheduleResult scheduleResult) {
        if (Objects.isNull(scheduleResult.getScheduleDate())) {
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.scheduleDateEmpty"));
        }
        if (StringUtils.isBlank(scheduleResult.getLhMachineCode())) {
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.machineCodeEmpty"));
        }
        if (Objects.isNull(scheduleResult.getMouldSurplusQty())) {
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.mouldSurplusQtyEmpty"));
        }
        if (Objects.isNull(scheduleResult.getSingleMouldShiftQty())) {
            throw new IllegalStateException(I18nUtil.getMessage(
                    "ui.data.alert.lhScheduleResult.increaseMouldStartPlan.singleMouldShiftQtyEmpty"));
        }
        if (Objects.isNull(scheduleResult.getLhTime()) || scheduleResult.getLhTime() <= 0) {
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.alert.lhScheduleResult.increaseMouldStartPlan.lhTimeInvalid"));
        }
    }
}
