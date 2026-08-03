package com.zlt.aps.common.engine.schedule.constraint;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面、胎侧共用的计划量损耗及取整结算结果。
 */
@Data
public class SchedulePlanQtyAdjustmentResult {

    /** 损耗计算前计划量 */
    private BigDecimal preLossPlanQty;

    /** 损耗补偿量 */
    private BigDecimal lossAddQty;

    /** 损耗计算后计划量 */
    private BigDecimal planQtyAfterLoss;

    /** 最小起排调整量 */
    private BigDecimal minStartAdjustQty;

    /** 最小起排调整后计划量 */
    private BigDecimal planQtyAfterMinStart;

    /** 卷曲取整调整量 */
    private BigDecimal roundAdjustQty;

    /** 损耗、最小起排和卷曲取整后的最终计划量 */
    private BigDecimal finalPlanQty;
}
