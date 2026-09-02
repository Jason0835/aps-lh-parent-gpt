package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 计划量计算公共结果模型。 */
@Data
public class SchedulePlanQtyResultModel {
    protected BigDecimal baseDemandQty;
    protected BigDecimal lossAddQty;
    protected BigDecimal toolLimitAdjustQty;
    protected BigDecimal toolOverflowQty;
    protected BigDecimal minStartAdjustQty;
    protected BigDecimal tailRoundAdjustQty;
    protected BigDecimal capacityAdjustQty;
    protected BigDecimal preLossPlanQty;
    protected BigDecimal planQtyBeforeToolLimit;
    protected BigDecimal finalPlanQty;
    protected String calcFormulaDesc;
}
