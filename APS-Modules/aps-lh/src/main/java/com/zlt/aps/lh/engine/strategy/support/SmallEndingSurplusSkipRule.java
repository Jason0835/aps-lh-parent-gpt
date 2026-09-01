package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * 收尾小余量不排产规则。
 *
 * <p>规则口径：收尾 SKU 的有效硫化余量小于等于允许欠产偏差值时，先判断余量占本轮目标月
 * 原始TOTAL_QTY的比例；达到不忽略阈值时继续排产，未达到且业务目标日前一日排程结果中
 * T+1日夜班未排满时，本次不排产并进入未排结果。</p>
 *
 * @author APS
 */
@Slf4j
public final class SmallEndingSurplusSkipRule {

    /** 收尾小余量且前日 T+1 夜班未排满的统一未排原因 */
    public static final String UNSCHEDULED_REASON =
            "收尾余量小于等于允许欠产偏差值，且前日 T+1 夜班未排满，本次不排产";

    private static final int T1_DATE_OFFSET = 1;

    private SmallEndingSurplusSkipRule() {
    }

    /**
     * 判断 SKU 是否命中收尾小余量不排产规则。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param isEnding 是否收尾
     * @return true-命中规则，本次不排产；false-命中比例豁免或继续按原排程逻辑处理
     */
    public static boolean shouldSkip(LhScheduleContext context, SkuScheduleDTO sku, boolean isEnding) {
        int genericSurplusQty = Objects.isNull(sku)
                ? 0 : Math.max(0, sku.getSurplusQty());
        return shouldSkip(context, sku, isEnding, genericSurplusQty);
    }

    /**
     * 使用调用场景明确传入的有效余量判断是否命中收尾小余量不排产规则。
     *
     * <p>提前生产中心运行视图使用实际消费账本剩余量；其他场景通过原重载继续使用通用
     * 硫化余量。该方法只统一规则判断，不负责计算提前生产目标量。</p>
     *
     * @param context 排程上下文
     * @param sku SKU
     * @param isEnding 是否收尾
     * @param ruleSurplusQty 当前运行场景下的有效余量
     * @return true-命中规则，本次不排产；false-命中比例豁免或继续按原排程逻辑处理
     */
    public static boolean shouldSkip(LhScheduleContext context,
                                     SkuScheduleDTO sku,
                                     boolean isEnding,
                                     int ruleSurplusQty) {
        if (!isEnding || Objects.isNull(sku)) {
            return false;
        }
        int surplusQty = Math.max(0, ruleSurplusQty);
        int toleranceQty = resolveToleranceQty(context);
        if (surplusQty > toleranceQty) {
            return false;
        }
        int originalMonthPlanTotalQty = Math.max(0, sku.getOriginalMonthPlanTotalQty());
        int keepRatioPercent = resolveKeepRatioPercent(context);
        boolean keepScheduleByRatio = isKeepScheduleBySurplusRatio(
                surplusQty, originalMonthPlanTotalQty, keepRatioPercent);
        int previousT1NightPlanQty = resolveTargetPreviousT1NightPlanQty(context, sku.getMaterialCode());
        boolean previousT1NightFull = isTargetPreviousT1NightFull(context, sku);
        boolean skipSchedule = !keepScheduleByRatio && !previousT1NightFull;
        log.info("收尾小余量统一判断, factoryCode: {}, batchNo: {}, materialCode: {}, productStatus: {}, "
                        + "surplusQty: {}, toleranceQty: {}, originalMonthPlanTotalQty: {}, "
                        + "keepRatioPercent: {}, keepScheduleByRatio: {}, previousT1NightPlanQty: {}, "
                        + "shiftCapacity: {}, previousT1NightFull: {}, skipSchedule: {}",
                Objects.isNull(context) ? null : context.getFactoryCode(),
                Objects.isNull(context) ? null : context.getBatchNo(),
                sku.getMaterialCode(), sku.getProductStatus(), surplusQty, toleranceQty,
                originalMonthPlanTotalQty, keepRatioPercent, keepScheduleByRatio,
                previousT1NightPlanQty, sku.getShiftCapacity(), previousT1NightFull, skipSchedule);
        // 前日 T+1 夜班未排满是本规则的必要条件，已排满时仍按原收尾排产规则继续。
        return skipSchedule;
    }

    /**
     * 判断小余量SKU是否达到继续排产比例。
     *
     * <p>使用long交叉相乘避免浮点舍入误差；月计划原始TOTAL_QTY不大于0时比例无有效分母，
     * 不触发继续排产豁免。</p>
     *
     * @param surplusQty 当前场景有效硫化余量
     * @param originalMonthPlanTotalQty 本轮目标月原始TOTAL_QTY
     * @param keepRatioPercent 继续排产比例阈值
     * @return true-不忽略小余量并继续排产；false-继续执行现有小余量跳过逻辑
     */
    static boolean isKeepScheduleBySurplusRatio(int surplusQty,
                                                int originalMonthPlanTotalQty,
                                                int keepRatioPercent) {
        if (originalMonthPlanTotalQty <= 0 || keepRatioPercent < 0) {
            return false;
        }
        return (long) Math.max(0, surplusQty) * 100L
                >= (long) originalMonthPlanTotalQty * keepRatioPercent;
    }

    /**
     * 解析收尾小余量允许欠产偏差值。
     *
     * @param context 排程上下文
     * @return 允许不排产的最大收尾余量
     */
    public static int resolveToleranceQty(LhScheduleContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleConfig())) {
            return LhScheduleConstant.CONTINUOUS_ENDING_SURPLUS_TOLERANCE_QTY;
        }
        return context.getScheduleConfig().getContinuousEndingSurplusToleranceQty();
    }

    /**
     * 解析收尾小余量继续排产比例阈值。
     *
     * @param context 排程上下文
     * @return 继续排产的最低百分比
     */
    public static int resolveKeepRatioPercent(LhScheduleContext context) {
        if (Objects.isNull(context) || Objects.isNull(context.getScheduleConfig())) {
            return LhScheduleConstant.SMALL_ENDING_SURPLUS_KEEP_RATIO_PERCENT;
        }
        return context.getScheduleConfig().getSmallEndingSurplusKeepRatioPercent();
    }

    /**
     * 判断业务目标日前一日排程结果中该 SKU 的 T+1 日夜班是否已排满。
     *
     * @param context 排程上下文
     * @param sku SKU
     * @return true-业务目标日前一日 T+1 夜班排产量大于等于班产；false-无业务目标日前一日数据或未排满
     */
    public static boolean isTargetPreviousT1NightFull(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || sku.getShiftCapacity() <= 0) {
            return false;
        }
        return resolveTargetPreviousT1NightPlanQty(context, sku.getMaterialCode()) >= sku.getShiftCapacity();
    }

    /**
     * 汇总业务目标日前一日排程结果中指定物料的 T+1 日夜班排产量。
     *
     * @param context 排程上下文
     * @param materialCode 物料编码
     * @return 业务目标日前一日 T+1 夜班排产量，无数据时返回0
     */
    public static int resolveTargetPreviousT1NightPlanQty(LhScheduleContext context, String materialCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(context.getTargetPreviousScheduleResultList())) {
            return 0;
        }
        Integer nightShiftIndex = resolveT1NightShiftIndex(context);
        if (Objects.isNull(nightShiftIndex)) {
            return 0;
        }
        int totalQty = 0;
        for (LhScheduleResult result : context.getTargetPreviousScheduleResultList()) {
            if (Objects.isNull(result) || !StringUtils.equals(materialCode, result.getMaterialCode())) {
                continue;
            }
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, nightShiftIndex);
            totalQty += Objects.isNull(shiftPlanQty) ? 0 : Math.max(0, shiftPlanQty);
        }
        return totalQty;
    }

    /**
     * 解析当前排程模板中 T+1 日夜班对应的班次索引。
     *
     * @param context 排程上下文
     * @return T+1 夜班索引，未配置时返回 null
     */
    private static Integer resolveT1NightShiftIndex(LhScheduleContext context) {
        List<LhShiftConfigVO> shifts = context.getScheduleWindowShifts();
        if (CollectionUtils.isEmpty(shifts)) {
            shifts = LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate());
        }
        return LhScheduleTimeUtil.findFirstNightShiftIndexWithOffset(shifts, T1_DATE_OFFSET);
    }
}
