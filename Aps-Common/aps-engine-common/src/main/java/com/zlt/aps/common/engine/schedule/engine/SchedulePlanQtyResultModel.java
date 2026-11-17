package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 计划量计算公共结果模型。 */
@Data
public class SchedulePlanQtyResultModel {
    /**
     * 损耗前基础净需求量（米）。
     */
    protected BigDecimal baseDemandQty;
    /** 工艺损耗补偿增加量（米）。 */
    protected BigDecimal lossAddQty;
    /** 工装受限调整量（米）。 */
    protected BigDecimal toolLimitAdjustQty;
    /** 因工装不足溢出的需求量（米）。 */
    protected BigDecimal toolOverflowQty;
    /** 最小起排量补齐调整量（米）。 */
    protected BigDecimal minStartAdjustQty;
    /** 卷曲定长整卷补齐调整量（米）。 */
    protected BigDecimal tailRoundAdjustQty;
    /** 机台剩余产能裁剪调整量（米）。 */
    protected BigDecimal capacityAdjustQty;
    /** 损耗前计划量（米）。 */
    protected BigDecimal preLossPlanQty;
    /** 工装限制校验前的计划量（米）。 */
    protected BigDecimal planQtyBeforeToolLimit;
    /** 经损耗、起排、整卷与产能修正后的最终计划量（米）。 */
    protected BigDecimal finalPlanQty;
    /** 计划量计算公式与步骤说明。 */
    protected String calcFormulaDesc;
}
