package com.zlt.aps.common.engine.quantity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 汇总计划量按来源权重分摊工具。
 *
 * <p>分摊时按来源业务键稳定排序，除最后一项外统一按指定精度四舍五入，
 * 最后一项吸收尾差，确保分摊数量之和严格等于汇总数量。</p>
 */
public final class PlanQuantityAllocationUtils {

    private PlanQuantityAllocationUtils() {
    }

    /**
     * 按权重分摊汇总数量。
     *
     * @param totalQty 汇总数量，空值按零处理
     * @param itemList 来源分摊项
     * @param scale    计算精度
     * @return 按业务键稳定排序后的分摊结果；来源为空时返回空列表
     * @throws IllegalArgumentException 计算精度小于零时抛出
     */
    public static List<PlanQuantityAllocationItem> allocate(BigDecimal totalQty,
                                                            List<PlanQuantityAllocationItem> itemList,
                                                            int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("计划量分摊精度不能小于0");
        }
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal normalizedTotalQty = nvl(totalQty);
        List<PlanQuantityAllocationItem> sortedItemList = new ArrayList<>();
        itemList.stream()
                .filter(item -> item != null && item.getSourceBusinessKey() != null)
                .sorted(Comparator.comparing(PlanQuantityAllocationItem::getSourceBusinessKey))
                .forEach(item -> sortedItemList.add(new PlanQuantityAllocationItem(
                        item.getSourceBusinessKey(), nvl(item.getWeight()).max(BigDecimal.ZERO), BigDecimal.ZERO)));
        if (sortedItemList.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal totalWeight = sortedItemList.stream()
                .map(PlanQuantityAllocationItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            sortedItemList.get(sortedItemList.size() - 1).setAllocatedQty(normalizedTotalQty);
            return sortedItemList;
        }
        BigDecimal allocatedTotalQty = BigDecimal.ZERO;
        for (int index = 0; index < sortedItemList.size(); index++) {
            PlanQuantityAllocationItem item = sortedItemList.get(index);
            BigDecimal allocatedQty;
            if (index == sortedItemList.size() - 1) {
                allocatedQty = normalizedTotalQty.subtract(allocatedTotalQty);
            } else {
                allocatedQty = normalizedTotalQty.multiply(item.getWeight())
                        .divide(totalWeight, scale, RoundingMode.HALF_UP);
                allocatedTotalQty = allocatedTotalQty.add(allocatedQty);
            }
            item.setAllocatedQty(allocatedQty);
        }
        return sortedItemList;
    }

    /**
     * 空数量转零。
     *
     * @param value 原数量
     * @return 非空数量
     */
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

