package com.zlt.aps.mp.engine.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * SKU优先级信息类
 *
 * @author Yelq
 * @date 20260112
 */
@Data
public class SkuPriorityInfo {
    /**
     * 物料描述
     */
    private String sku;
    /**
     * 是否供应链优先标记：即
     */
    private boolean hasSupplyChainPriority;
    private boolean hasHeightPriority;
    private boolean hasProduction;
    private boolean hasMoldCapacityLimit;
    private int moldLimitedNetRequirement;
    private double inventorySaleRatio;
    private boolean isLessMinQty;
    private int totalNetRequirement;
    private List<MonthPlanProductionRequirePlanVo> plans;

    @Override
    public String toString() {
        return String.format("SKU: %s, 供应链优先: %s, 高优先级：%s,已排产：%s,模具受限: %s, 受限净需求: %d, " +
                        "库销比: %.2f, 小于最小排产量: %s, 总净需求: %d",
                sku, hasSupplyChainPriority, hasHeightPriority, hasProduction, hasMoldCapacityLimit,
                moldLimitedNetRequirement, inventorySaleRatio,
                isLessMinQty, totalNetRequirement);
    }
}
