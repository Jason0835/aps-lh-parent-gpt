package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 指定直裁班次开始时点的预计库存结果。
 */
@Data
@Builder
public class Cd90InventoryProjection {

    /** 允许为负数的库存结余。 */
    private BigDecimal inventoryBalance;
    /** 最低为0的预计可用库存。 */
    private BigDecimal expectedAvailableStock;
    /** 库存结余为负时形成的累计缺料量。 */
    private BigDecimal accumulatedShortageQuantity;
}
