package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/**
 * 单条续作结果的实际消费记录。晚阶段精度重算先完整撤销本条消费，再按保留量重新扣账。
 * dayN记录实际变动日期及字段，不能用结果数量或账本尾部倒推消费来源。
 */
public final class ContinuousProductionConsumption {

    /** 实际生产余量消费量，已排除收尾允许超量。 */
    private final int productionQty;
    /** SKU级及汇总满班超量的本条增量。 */
    private final int shiftFillOverQty;
    /** 每个真实被消费dayN的字段增量，数组不向外暴露。 */
    private final Map<LocalDate, int[]> quotaDeltas;

    /** @param productionQty 实际生产消费 @param shiftFillOverQty 超量增量 @param quotaDeltas dayN增量 */
    private ContinuousProductionConsumption(int productionQty, int shiftFillOverQty, Map<LocalDate, int[]> quotaDeltas) {
        this.productionQty = productionQty;
        this.shiftFillOverQty = shiftFillOverQty;
        this.quotaDeltas = quotaDeltas;
    }

    /** @return 本条实际消费生产量 */
    public int getProductionQty() {
        return productionQty;
    }

    /**
     * 在公共扣账入口调用前保存本条可能修改的账本字段。
     * @param sku 当前来源SKU
     * @param productionRemainingQty 扣账前生产余量
     * @return 未消费的快照
     */
    public static Before captureBefore(SkuScheduleDTO sku, int productionRemainingQty) {
        Map<LocalDate, int[]> quotas = new LinkedHashMap<>();
        dailyQuotas(sku).forEach((date, quota) -> {
            if (Objects.nonNull(quota)) {
                quotas.put(date, values(quota));
            }
        });
        return new Before(productionRemainingQty, sku.getShiftFillOverQty(), quotas);
    }

    /**
     * 按原消费增量恢复dayN和超量，其他机台后续消费不受影响。
     * @param context 当前上下文
     * @param sku 本条来源SKU
     */
    public void restoreDailyQuota(LhScheduleContext context, SkuScheduleDTO sku) {
        quotaDeltas.forEach((date, delta) -> {
            SkuDailyPlanQuotaDTO quota = dailyQuotas(sku).get(date);
            if (Objects.isNull(quota)) {
                throw new IllegalStateException("精度重算缺少原消费dayN：" + date);
            }
            quota.setScheduledQty(quota.getScheduledQty() - delta[0]);
            quota.setRemainingQty(quota.getRemainingQty() - delta[1]);
            quota.setActualQty(quota.getActualQty() - delta[2]);
            quota.setFutureBorrowQty(quota.getFutureBorrowQty() - delta[3]);
            quota.setShiftFillOverQty(quota.getShiftFillOverQty() - delta[4]);
        });
        sku.setShiftFillOverQty(sku.getShiftFillOverQty() - shiftFillOverQty);
        if (shiftFillOverQty != 0) {
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
            context.getSkuShiftFillOverQtyMap().computeIfPresent(skuKey, (key, quantity) -> quantity - shiftFillOverQty);
        }
        SkuDailyPlanQuotaUtil.refreshRollingFields(sku.getDailyPlanQuotaMap());
    }

    /** @param quota dayN条目 @return 真实消费字段 */
    private static int[] values(SkuDailyPlanQuotaDTO quota) {
        return new int[] {quota.getScheduledQty(), quota.getRemainingQty(), quota.getActualQty(),
                quota.getFutureBorrowQty(), quota.getShiftFillOverQty()};
    }

    /** @param sku 当前SKU @return 未启用dayN时返回只读空账本 */
    private static Map<LocalDate, SkuDailyPlanQuotaDTO> dailyQuotas(SkuScheduleDTO sku) {
        return Objects.isNull(sku.getDailyPlanQuotaMap()) ? Collections.emptyMap() : sku.getDailyPlanQuotaMap();
    }

    /** 单次扣账前的临时快照，扣账后转换为只读消费记录，不放入运行态。 */
    public static final class Before {
        private final int productionRemainingQty;
        private final int shiftFillOverQty;
        private final Map<LocalDate, int[]> quotas;

        private Before(int productionRemainingQty, int shiftFillOverQty, Map<LocalDate, int[]> quotas) {
            this.productionRemainingQty = productionRemainingQty;
            this.shiftFillOverQty = shiftFillOverQty;
            this.quotas = quotas;
        }

        /** @param sku 扣账后SKU @param remainingQty 扣账后生产余量 @return 本条实际消费记录 */
        public ContinuousProductionConsumption complete(SkuScheduleDTO sku, int remainingQty) {
            Map<LocalDate, int[]> deltas = new LinkedHashMap<>();
            dailyQuotas(sku).forEach((date, quota) -> {
                if (Objects.nonNull(quota)) {
                    int[] before = quotas.getOrDefault(date, new int[5]);
                    int[] delta = values(quota);
                    for (int index = 0; index < delta.length; index++) {
                        delta[index] -= before[index];
                    }
                    deltas.put(date, delta);
                }
            });
            return new ContinuousProductionConsumption(Math.max(0, productionRemainingQty - remainingQty),
                    sku.getShiftFillOverQty() - shiftFillOverQty, deltas);
        }
    }
}
