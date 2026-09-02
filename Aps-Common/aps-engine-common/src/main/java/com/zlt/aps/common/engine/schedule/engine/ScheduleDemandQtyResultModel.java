package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 需求量计算公共结果模型。 */
@Data
public class ScheduleDemandQtyResultModel {
    protected BigDecimal currentShiftDemandQty;
    protected BigDecimal guardDemandQty;
    protected BigDecimal rollingStockQty;
    protected BigDecimal currentShiftStockGapQty;
    protected BigDecimal stockGapQty;
    protected BigDecimal demandQty;
    protected Integer guardShiftCount;
    protected BigDecimal supplyHours;
    protected String calcDesc;
}
