package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.SkuTagEnum;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.IEndingJudgmentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 默认收尾判定策略实现
 * <p>统一收尾判定逻辑，确保一致性</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DefaultEndingJudgmentStrategy implements IEndingJudgmentStrategy {

    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    @Override
    public boolean isEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        return isCurrentWindowEnding(context, sku);
    }

    @Override
    public boolean isExpectedEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return false;
        }
        // 规则1：已明确标记为收尾
        if (SkuTagEnum.ENDING.getCode().equals(sku.getSkuTag())) {
            return true;
        }

        int targetScheduleQty = sku.resolveTargetScheduleQty();
        boolean fullCapacityMode = isFullCapacityMode(context);
        boolean endingBySurplusInFullModeEnabled = isEndingBySurplusInFullModeEnabled(context);

        // 规则2：排产目标量 <= 多台可用机台在排程窗口内的合计产能。
        // 不再使用单SKU理论产能（shiftCapacity * totalScheduleShifts），改为基于实际候选机台计算。
        // 满排模式下可选"按余量判定"开关，避免目标量封顶导致误判。
        int rule2CandidateQty = resolveRule2CandidateQty(context, sku, targetScheduleQty, fullCapacityMode,
                endingBySurplusInFullModeEnabled);
        boolean skipRule2ByWindowRemainingPlanQty = rule2CandidateQty > 0
                && shouldSkipRule2ByWindowRemainingPlanQty(sku, fullCapacityMode,
                endingBySurplusInFullModeEnabled, rule2CandidateQty);
        if (rule2CandidateQty > 0 && !skipRule2ByWindowRemainingPlanQty) {
            int totalAvailableCapacity = getTargetScheduleQtyResolver()
                    .calcSkuTotalAvailableCapacityInWindow(context, sku);
            if (totalAvailableCapacity > 0 && rule2CandidateQty <= totalAvailableCapacity) {
                log.debug("SKU[{}]判定为收尾(规则2): 比较量{} <= 多机台合计产能{} (满排模式:{}, 满排余量开关:{})",
                        sku.getMaterialCode(), rule2CandidateQty, totalAvailableCapacity, fullCapacityMode,
                        endingBySurplusInFullModeEnabled);
                return true;
            }
        }
        if (skipRule2ByWindowRemainingPlanQty
                && getTargetScheduleQtyResolver().canFinishSurplusInActualWindow(context, sku)) {
            log.debug("SKU[{}]判定为收尾(满排余量窗口产能): 硫化余量可在当前排程窗口内完成",
                    sku.getMaterialCode());
            return true;
        }

        // 规则3：待排量 < 日产能。
        // 满排模式且启用"按余量判收尾"时，优先按实际收尾需求量（max(余量,胎胚库存)）判断，
        // 避免全月待排量过大导致收尾漏判。
        int dailyCapacity = sku.getDailyCapacity();
        int rule3CandidateQty = (fullCapacityMode && endingBySurplusInFullModeEnabled)
                ? resolveTailTargetQty(context, sku)
                : targetScheduleQty;
        if (dailyCapacity > 0 && rule3CandidateQty < dailyCapacity && rule3CandidateQty > 0) {
            log.debug("SKU[{}]判定为收尾(规则3): 比较量{} < 日产能{} (满排模式:{}, 满排余量开关:{})",
                    sku.getMaterialCode(), rule3CandidateQty, dailyCapacity,
                    fullCapacityMode, endingBySurplusInFullModeEnabled);
            return true;
        }

        return false;
    }

    @Override
    public boolean isCurrentWindowEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return false;
        }
        int tailTargetQty = resolveTailTargetQty(context, sku);
        if (tailTargetQty <= 0) {
            if (!context.isNewSpecProposalPreview()) {
                log.info("SKU当前窗口收尾判断, materialCode: {}, tailTargetQty: {}, currentWindowTailFlag: false",
                        sku.getMaterialCode(), tailTargetQty);
            }
            return false;
        }
        int totalAvailableCapacity = getTargetScheduleQtyResolver()
                .calcSkuEndingAvailableCapacityInWindow(context, sku);
        boolean currentWindowTailFlag = totalAvailableCapacity >= tailTargetQty;
        boolean sharedEmbryo = getTargetScheduleQtyResolver().isSharedEmbryoInWindow(context, sku);
        if (!context.isNewSpecProposalPreview()) {
            log.info("SKU当前窗口收尾判断, materialCode: {}, window: 3天/8班, sharedEmbryo: {}, "
                            + "surplusQty: {}, embryoStock: {}, tailTargetQty: {}, totalAvailableCapacity: {}, "
                            + "currentWindowTailFlag: {}",
                    sku.getMaterialCode(), sharedEmbryo, Math.max(0, sku.getSurplusQty()),
                    Math.max(0, sku.getEmbryoStock()), tailTargetQty, totalAvailableCapacity,
                    currentWindowTailFlag);
        }
        return currentWindowTailFlag;
    }

    /**
     * 判断 SKU 排后最终是否收尾。
     * <p>isEnd 只标识硫化余量收尾：把本 SKU 的硫化余量排完才标识为收尾，
     * 不因胎胚收尾（胎胚库存清空）而提前标识。因此余量大于胎胚库存且命中胎胚收尾时，
     * 胎胚库存排完但余量仍有剩余，必须判定为非收尾。</p>
     * <p>硫化余量为 0（月计划已完成）时视为余量已排完，本次只要仍有实际排产量即标识收尾。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param actualScheduledQty 本次窗口实际总排产量
     * @return true-最终收尾；false-最终非收尾
     */
    @Override
    public boolean isFinalEnding(LhScheduleContext context, SkuScheduleDTO sku, int actualScheduledQty) {
        if (Objects.isNull(sku)) {
            return false;
        }
        int actualQty = Math.max(0, actualScheduledQty);
        int tailTargetQty = resolveFinalReviewTailTargetQty(context, sku);
        boolean finalTailFlag;
        if (tailTargetQty <= 0) {
            // 无硫化余量：本次有实际排产量即视为余量已收尾，避免0量占位结果行被误标收尾。
            finalTailFlag = actualQty > 0;
        } else {
            finalTailFlag = actualQty >= tailTargetQty;
        }
        log.info("SKU排后最终收尾判断, materialCode: {}, actualScheduledQty: {}, tailTargetQty: {}, "
                        + "rule: 仅按硫化余量判定, finalTailFlag: {}",
                sku.getMaterialCode(), actualQty, tailTargetQty, finalTailFlag);
        return finalTailFlag;
    }

    @Override
    public boolean isStructureEndingForPriority(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku)) {
            return false;
        }
        int structureEndingDays = context.getScheduleConfig() != null
                ? context.getScheduleConfig().getStructureEndingDays()
                : LhScheduleConstant.DEFAULT_STRUCTURE_ENDING_DAYS;
        int actualEndingDays = calculateEndingDaysForStructurePriority(context, sku);
        boolean expectedTailFlag = isExpectedEnding(context, sku);
        boolean structureTailFlag = expectedTailFlag
                && actualEndingDays >= 0
                && actualEndingDays <= structureEndingDays;
        log.debug("SKU结构收尾排序判断, materialCode: {}, structureEndingDays: {}, actualEndingDays: {}, "
                        + "expectedTailFlag: {}, structureTailFlag: {}",
                sku.getMaterialCode(), structureEndingDays, actualEndingDays, expectedTailFlag, structureTailFlag);
        return structureTailFlag;
    }

    @Override
    public int calculateEndingShifts(LhScheduleContext context, SkuScheduleDTO sku) {
        int shiftCapacity = sku.getShiftCapacity();
        if (shiftCapacity <= 0) {
            return -1;
        }

        int targetScheduleQty = sku.resolveTargetScheduleQty();
        if (targetScheduleQty <= 0) {
            return 0;
        }

        // 向上取整计算所需班次
        return (int) Math.ceil((double) targetScheduleQty / shiftCapacity);
    }

    @Override
    public int calculateEndingDays(LhScheduleContext context, SkuScheduleDTO sku) {
        int shifts = calculateEndingShifts(context, sku);
        if (shifts < 0) {
            return -1;
        }
        if (shifts == 0) {
            return 0;
        }
        return (int) Math.ceil((double) shifts / LhScheduleConstant.DEFAULT_SHIFTS_PER_DAY);
    }

    @Override
    public int calculateEndingDaysForStructurePriority(LhScheduleContext context, SkuScheduleDTO sku) {
        if (context == null || sku == null) {
            return -1;
        }
        TargetScheduleQtyResolver.StructureEndingCapacitySnapshot snapshot =
                getTargetScheduleQtyResolver().evaluateStructureEndingCapacity(context, sku);
        if (snapshot != null) {
            return snapshot.getEndingDaysWithinStructureWindow();
        }
        return calculateEndingDays(context, sku);
    }

    /**
     * 判断当前是否为按产能满排模式。
     *
     * @param context 排程上下文
     * @return true-按产能满排，false-按需求排产
     */
    private boolean isFullCapacityMode(LhScheduleContext context) {
        return context != null
                && context.getScheduleConfig() != null
                && context.getScheduleConfig().isFullCapacitySchedulingEnabled();
    }

    /**
     * 满排模式下是否启用"按余量判定规则2"。
     *
     * @param context 排程上下文
     * @return true-启用，false-关闭
     */
    private boolean isEndingBySurplusInFullModeEnabled(LhScheduleContext context) {
        if (context != null && context.getScheduleConfig() != null) {
            return context.getScheduleConfig().isEndingBySurplusInFullModeEnabled();
        }
        return LhScheduleConstant.ENABLE_ENDING_BY_SURPLUS_IN_FULL_MODE == 1;
    }

    /**
     * 解析规则2的比较量。
     *
     * @param sku SKU
     * @param targetScheduleQty 目标排产量
     * @param fullCapacityMode 是否满排模式
     * @param endingBySurplusInFullModeEnabled 满排按余量判收尾开关
     * @return 规则2比较量，<=0 表示本轮不执行规则2
     */
    private int resolveRule2CandidateQty(LhScheduleContext context,
                                         SkuScheduleDTO sku,
                                         int targetScheduleQty,
                                         boolean fullCapacityMode,
                                         boolean endingBySurplusInFullModeEnabled) {
        if (!fullCapacityMode) {
            return targetScheduleQty;
        }
        if (!endingBySurplusInFullModeEnabled) {
            return 0;
        }
        return resolveTailTargetQty(context, sku);
    }

    /**
     * 判断规则2是否应被窗口剩余额度拦截。
     * <p>仅在满排模式且启用"按余量判收尾"时生效，避免窗口 dayN 额度明显不足时提前命中收尾。</p>
     *
     * @param sku SKU
     * @param fullCapacityMode 是否满排模式
     * @param endingBySurplusInFullModeEnabled 满排按余量判收尾开关
     * @param rule2CandidateQty 规则2比较量
     * @return true-跳过规则2，false-保留原判定
     */
    private boolean shouldSkipRule2ByWindowRemainingPlanQty(SkuScheduleDTO sku,
                                                            boolean fullCapacityMode,
                                                            boolean endingBySurplusInFullModeEnabled,
                                                            int rule2CandidateQty) {
        if (sku == null || !fullCapacityMode || !endingBySurplusInFullModeEnabled || rule2CandidateQty <= 0) {
            return false;
        }
        int windowRemainingPlanQty = Math.max(0, sku.getWindowRemainingPlanQty());
        if (windowRemainingPlanQty <= 0 || rule2CandidateQty <= windowRemainingPlanQty) {
            return false;
        }
        log.debug("SKU[{}]跳过收尾规则2: 比较量{} > 窗口剩余额度{}",
                sku.getMaterialCode(), rule2CandidateQty, windowRemainingPlanQty);
        return true;
    }

    /**
     * 计算收尾比较量。
     * <p>统一委托目标量解析器：胎胚库存硬目标保持精确数量，普通收尾继续沿用现有模台数归整口径。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return 收尾比较量
     */
    private int resolveTailTargetQty(LhScheduleContext context, SkuScheduleDTO sku) {
        // 排前收尾预判使用运行态活跃生产单元判断共用/单胎胚。
        return getTargetScheduleQtyResolver().resolveFinalEndingTargetQty(context, sku);
    }

    /**
     * 解析排后最终复核使用的余量收尾比较目标量。
     * <p>isEnd 只标识硫化余量是否排完，因此排后复核固定按硫化余量口径，不再读取胎胚库存、
     * 也不按模台数归整；胎胚静态关系只服务于排产控量阶段的胎胚收尾判断，
     * 不参与 isEnd 目标量，避免"胎胚库存排完但余量仍有剩余"被误标为收尾。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return 排后最终收尾比较目标量；0 表示无硫化余量
     */
    private int resolveFinalReviewTailTargetQty(LhScheduleContext context, SkuScheduleDTO sku) {
        return getTargetScheduleQtyResolver().resolveSurplusEndingTargetQty(context, sku);
    }

    /**
     * 判断 SKU 排产过程中的运行态收尾标识。
     * <p>仅供排产过程中读取结果行 isEnd 的既有逻辑使用，沿用改动前的胎胚库存硬目标口径，
     * 保证共用胎胚活跃集合、收尾均衡、结构保机和结果替换等行为与改动前一致。
     * 该标识不落库，排产结束后由 {@link #isFinalEnding} 按硫化余量口径统一覆写。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param actualScheduledQty 当前已汇总的实际排产量
     * @return true-运行态收尾；false-运行态非收尾
     */
    @Override
    public boolean isRuntimeEnding(LhScheduleContext context, SkuScheduleDTO sku, int actualScheduledQty) {
        if (Objects.isNull(sku)) {
            return false;
        }
        int tailTargetQty = resolveRuntimeEndingTailTargetQty(context, sku);
        boolean runtimeTailFlag = tailTargetQty > 0 && Math.max(0, actualScheduledQty) >= tailTargetQty;
        log.debug("SKU运行态收尾标识判断, materialCode: {}, actualScheduledQty: {}, tailTargetQty: {}, "
                        + "rule: 运行态沿用胎胚库存硬目标口径, runtimeTailFlag: {}",
                sku.getMaterialCode(), Math.max(0, actualScheduledQty), tailTargetQty, runtimeTailFlag);
        return runtimeTailFlag;
    }

    /**
     * 解析运行态收尾标识使用的比较目标量。
     * <p>按胎胚静态关系判断共用/单胎胚，命中胎胚库存硬目标时取精确硬目标，
     * 普通收尾按模台数归整；该口径只服务运行态标识，不参与最终落库判定。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return 运行态收尾比较目标量
     */
    private int resolveRuntimeEndingTailTargetQty(LhScheduleContext context, SkuScheduleDTO sku) {
        return getTargetScheduleQtyResolver().resolveFinalEndingTargetQtyByStaticRelation(context, sku);
    }

    /**
     * 获取目标排产量解析器（带空安全回退）。
     *
     * @return 目标排产量解析器
     */
    private TargetScheduleQtyResolver getTargetScheduleQtyResolver() {
        return targetScheduleQtyResolver != null
                ? targetScheduleQtyResolver
                : new TargetScheduleQtyResolver();
    }
}
