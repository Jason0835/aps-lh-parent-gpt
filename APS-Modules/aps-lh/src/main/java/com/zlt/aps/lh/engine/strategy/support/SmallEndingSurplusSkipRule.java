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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收尾小余量不排产规则。
 *
 * <p>规则口径：收尾 SKU 的有效硫化余量小于等于允许欠产偏差值时，先判断余量占本轮目标月
 * 原始TOTAL_QTY的比例；达到不忽略阈值时继续排产。未达到且前次同物料、同产品状态的
 * 全机台首班至T+1夜班累计计划已覆盖前次硫化余量时，本次不排产并进入未排结果。</p>
 *
 * @author APS
 */
@Slf4j
public final class SmallEndingSurplusSkipRule {

    /**
     * 收尾小余量且前次已计划收完的统一未排原因。
     */
    public static final String UNSCHEDULED_REASON =
            "收尾余量小于等于允许欠产偏差值，且前次首班至T+1夜班累计计划已覆盖前次硫化余量，本次不排产";

    /**
     * 前次完成判断的截止夜班日期偏移。
     */
    private static final int T1_DATE_OFFSET = 1;

    /**
     * 禁止实例化无状态规则。
     */
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
        long previousPlannedQty = resolvePreviousPlannedQty(context, sku);
        Integer previousSurplusQty = resolvePreviousSurplusQty(context, sku);
        boolean previousPlanCompleted = Objects.nonNull(previousSurplusQty)
                && previousPlannedQty >= previousSurplusQty;
        boolean skipSchedule = !keepScheduleByRatio && previousPlanCompleted;
        log.info("收尾小余量统一判断, factoryCode: {}, batchNo: {}, materialCode: {}, productStatus: {}, "
                        + "surplusQty: {}, toleranceQty: {}, originalMonthPlanTotalQty: {}, "
                        + "keepRatioPercent: {}, keepScheduleByRatio: {}, previousPlannedQty: {}, "
                        + "previousSurplusQty: {}, previousPlanCompleted: {}, skipSchedule: {}",
                Objects.isNull(context) ? null : context.getFactoryCode(),
                Objects.isNull(context) ? null : context.getBatchNo(),
                sku.getMaterialCode(), sku.getProductStatus(), surplusQty, toleranceQty,
                originalMonthPlanTotalQty, keepRatioPercent, keepScheduleByRatio,
                previousPlannedQty, previousSurplusQty, previousPlanCompleted, skipSchedule);
        // 比较的是前次余量；本轮余量只决定阈值与未排数量，不能替代历史完成依据。
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
     * 判断前次同物料、同产品状态的累计计划是否已覆盖前次余量。
     *
     * @param context 排程上下文
     * @param sku 当前物料及产品状态
     * @return 历史余量有效一致且累计计划已覆盖时返回true
     */
    public static boolean isPreviousPlanCompleted(LhScheduleContext context, SkuScheduleDTO sku) {
        Integer previousSurplusQty = resolvePreviousSurplusQty(context, sku);
        return Objects.nonNull(previousSurplusQty)
                && resolvePreviousPlannedQty(context, sku) >= previousSurplusQty;
    }

    /**
     * 汇总前次同SKU全部机台从首班至T+1夜班（含）的有效计划量。
     *
     * @param context 已加载前次有效排程快照的上下文
     * @param sku 当前物料及产品状态
     * @return 指定班次前缀累计量；无快照或无截止班次时返回0
     */
    public static long resolvePreviousPlannedQty(LhScheduleContext context, SkuScheduleDTO sku) {
        List<LhScheduleResult> results = resolvePreviousResults(context, sku);
        if (results.isEmpty()) {
            return 0L;
        }
        Integer nightShiftIndex = resolveT1NightShiftIndex(context);
        if (Objects.isNull(nightShiftIndex)) {
            return 0L;
        }
        long totalQty = 0L;
        for (LhScheduleResult result : results) {
            for (int shiftIndex = 1; shiftIndex <= nightShiftIndex; shiftIndex++) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
                totalQty += Objects.isNull(quantity) ? 0 : Math.max(0, quantity);
            }
        }
        return totalQty;
    }

    /**
     * 读取前次SKU余量；各机台重复保存的SKU余量只取一次，缺失或不一致不推定已收完。
     *
     * @param context 已加载前次有效排程快照的上下文
     * @param sku 当前物料及产品状态
     * @return 一致的非负历史余量；缺少证据时返回null
     */
    public static Integer resolvePreviousSurplusQty(LhScheduleContext context, SkuScheduleDTO sku) {
        List<LhScheduleResult> results = resolvePreviousResults(context, sku);
        if (results.isEmpty() || Objects.isNull(resolveT1NightShiftIndex(context))) {
            return null;
        }
        Integer previousSurplusQty = results.get(0).getMouldSurplusQty();
        if (Objects.isNull(previousSurplusQty) || previousSurplusQty < 0
                || results.stream().anyMatch(result -> !Objects.equals(previousSurplusQty, result.getMouldSurplusQty()))) {
            log.info("小余量前次完成依据不足, factoryCode: {}, batchNo: {}, materialCode: {}, productStatus: {}, "
                            + "reason: 前次硫化余量缺失、负数或跨机台不一致",
                    context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus());
            return null;
        }
        return previousSurplusQty;
    }

    /**
     * 从已加载的前次快照筛选同物料、同产品状态的有效结果，保留所有机台与分段行。
     *
     * @param context 排程上下文
     * @param sku 当前物料及产品状态
     * @return 同SKU有效结果，无证据时返回空集合
     */
    private static List<LhScheduleResult> resolvePreviousResults(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isBlank(sku.getMaterialCode())
                || StringUtils.isBlank(sku.getProductStatus())
                || CollectionUtils.isEmpty(context.getTargetPreviousScheduleResultList())) {
            return Collections.emptyList();
        }
        return context.getTargetPreviousScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> !Objects.equals(1, result.getIsDelete()))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .collect(Collectors.toList());
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
