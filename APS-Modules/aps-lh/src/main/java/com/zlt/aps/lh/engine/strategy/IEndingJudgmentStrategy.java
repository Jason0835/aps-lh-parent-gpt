package com.zlt.aps.lh.engine.strategy;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.Objects;

/**
 * 收尾判定策略接口
 * <p>统一收尾判定逻辑，避免分散在多处导致不一致</p>
 *
 * @author APS
 */
public interface IEndingJudgmentStrategy {

    /**
     * 判断SKU是否处于收尾状态
     * <p>
     * 收尾判定规则（按优先级）：
     * <ol>
     *   <li>已明确标记为收尾（SkuTag.ENDING）</li>
     *   <li>硫化余量 <= 排程期内可生产总产能</li>
     *   <li>待排量 < 日产能（非满产运行）</li>
     * </ol>
     * </p>
     *
     * @param context 排程上下文
     * @param sku     SKU排程DTO
     * @return true表示处于收尾状态
     */
    boolean isEnding(LhScheduleContext context, SkuScheduleDTO sku);

    /**
     * 判断 SKU 是否为排前预计收尾。
     * <p>预计收尾仅用于排序日志、分析和排前参考，不直接控制实际排产控量。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return true-预计收尾；false-非预计收尾
     */
    default boolean isExpectedEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        return isEnding(context, sku);
    }

    /**
     * 判断 SKU 是否为当前排程窗口收尾。
     * <p>当前窗口收尾用于实际排产控量，调用方应在实际机台集合确定后、生成班次计划量前调用。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return true-当前窗口收尾；false-当前窗口非收尾
     */
    default boolean isCurrentWindowEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        return isEnding(context, sku);
    }

    /**
     * 判断 SKU 排后最终是否收尾。
     * <p>isEnd 只标识硫化余量收尾：排完全部硫化余量才标识为收尾，不因胎胚收尾（胎胚库存清空）
     * 而提前标识。因此硫化余量大于胎胚库存且命中胎胚收尾时，胎胚库存排完但余量仍有剩余，
     * 必须判定为非收尾。</p>
     * <p>硫化余量为 0（月计划已完成）时视为余量已排完，本次只要仍有实际排产量即标识收尾。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param actualScheduledQty 本次窗口实际总排产量
     * @return true-最终收尾；false-最终非收尾
     */
    default boolean isFinalEnding(LhScheduleContext context, SkuScheduleDTO sku, int actualScheduledQty) {
        if (Objects.isNull(sku)) {
            return false;
        }
        int actualQty = Math.max(0, actualScheduledQty);
        int surplusQty = Math.max(0, sku.getSurplusQty());
        if (surplusQty <= 0) {
            // 无硫化余量：本次有实际排产量即视为余量已收尾。
            return actualQty > 0;
        }
        return actualQty >= surplusQty;
    }

    /**
     * 判断 SKU 是否命中结构排序专用收尾。
     * <p>结构收尾只用于 SKU 排序优先级，不直接决定实际排产控量。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return true-结构排序收尾；false-未命中结构排序收尾
     */
    default boolean isStructureEndingForPriority(LhScheduleContext context, SkuScheduleDTO sku) {
        return isExpectedEnding(context, sku) && calculateEndingDaysForStructurePriority(context, sku) >= 0;
    }

    /**
     * 判断 SKU 排产过程中的运行态收尾标识。
     * <p>该标识只写入结果行供排产过程中读取 isEnd 的既有逻辑使用（共用胎胚活跃集合、
     * 收尾均衡、结构保机、结果替换等），沿用改动前的胎胚库存硬目标口径：
     * 非共用胎胚取 {@code max(硫化余量, 胎胚库存)}。命中胎胚库存硬目标时由具体实现
     * 优先取精确硬目标。</p>
     * <p>运行态标识不落库，排产全部结束后会由 {@link #isFinalEnding} 按
     * “只按硫化余量”口径统一覆写。因此本方法 MUST NOT 用于最终落库判定，
     * 保留旧口径只为保证排产行为与改动前一致。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @param actualScheduledQty 当前已汇总的实际排产量
     * @return true-运行态收尾；false-运行态非收尾
     */
    default boolean isRuntimeEnding(LhScheduleContext context, SkuScheduleDTO sku, int actualScheduledQty) {
        if (Objects.isNull(sku)) {
            return false;
        }
        int surplusQty = Math.max(0, sku.getSurplusQty());
        int embryoStock = Math.max(0, sku.getEmbryoStock());
        int endingDemandQty = Math.max(surplusQty, embryoStock);
        return endingDemandQty > 0 && Math.max(0, actualScheduledQty) >= endingDemandQty;
    }

    /**
     * 计算预计收尾所需班次数
     *
     * @param context 排程上下文
     * @param sku     SKU排程DTO
     * @return 预计收尾班次数，-1表示无法判定
     */
    int calculateEndingShifts(LhScheduleContext context, SkuScheduleDTO sku);

    /**
     * 计算预计收尾天数
     *
     * @param context 排程上下文
     * @param sku     SKU排程DTO
     * @return 预计收尾天数
     */
    int calculateEndingDays(LhScheduleContext context, SkuScheduleDTO sku);

    /**
     * 计算结构排序使用的真实收尾天数。
     * <p>默认复用通用收尾天数，具体实现可按候选机台真实窗口口径覆写。</p>
     *
     * @param context 排程上下文
     * @param sku SKU排程DTO
     * @return 结构排序使用的预计收尾天数
     */
    default int calculateEndingDaysForStructurePriority(LhScheduleContext context, SkuScheduleDTO sku) {
        return calculateEndingDays(context, sku);
    }
}
