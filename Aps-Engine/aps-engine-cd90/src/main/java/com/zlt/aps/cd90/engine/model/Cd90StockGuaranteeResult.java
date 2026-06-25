package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存保证班数和供应时长计算结果。
 */
@Data
@Builder
public class Cd90StockGuaranteeResult {

    /** 库存保证班数，允许小数。 */
    private BigDecimal guaranteedShifts;
    /** 库存供应成型时长，单位小时。 */
    private BigDecimal supplyHours;
    /** 扣减需求后的剩余库存。 */
    private BigDecimal remainingStock;
}
