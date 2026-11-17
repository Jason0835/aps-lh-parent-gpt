package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/** TM/TC 需求量计算公共结果模型。 */
@Data
public class ScheduleDemandQtyResultModel {
    /**
     * 当班成型需求量（米）。
     */
    protected BigDecimal currentShiftDemandQty;
    /** 库存保证窗口内的累计成型需求量（米）。 */
    protected BigDecimal guardDemandQty;
    /** 班初可用滚动库存（米）。 */
    protected BigDecimal rollingStockQty;
    /** 当班库存净缺口（米）。 */
    protected BigDecimal currentShiftStockGapQty;
    /** 保证窗口总库存净缺口（米）。 */
    protected BigDecimal stockGapQty;
    /** 最终计算出的目标需求量（米）。 */
    protected BigDecimal demandQty;
    /** 本次计算实际使用的库存保证班数。 */
    protected Integer guardShiftCount;
    /** 当前库存可支撑供应时长（小时）。 */
    protected BigDecimal supplyHours;
    /** 需求量计算口径与过程说明。 */
    protected String calcDesc;
}
