package com.zlt.aps.common.engine.quantity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 计划量分摊项。
 *
 * <p>用于在汇总计划量与原始来源任务之间传递稳定业务键、分摊权重和分摊结果。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanQuantityAllocationItem {

    /** 来源任务业务键 */
    private String sourceBusinessKey;

    /** 分摊权重 */
    private BigDecimal weight;

    /** 分摊数量 */
    private BigDecimal allocatedQty;
}

