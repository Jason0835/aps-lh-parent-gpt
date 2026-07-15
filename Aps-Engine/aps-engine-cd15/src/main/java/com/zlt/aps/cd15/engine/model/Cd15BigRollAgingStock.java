package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CD15 GDYY大卷成熟流水。
 */
@Data
@Builder
public class Cd15BigRollAgingStock {

    /** 来源类型：ACTUAL_STOCK 或 GDYY_PLAN。 */
    private String sourceType;
    /** 来源记录主键或计划行标识。 */
    private String sourceId;
    /** 大卷代码。 */
    private String bigRollCode;
    /** 大卷条码。 */
    private String bigRollBarcode;
    /** 可供斜裁使用的米数。 */
    private BigDecimal availableQuantity;
    /** 已被当前排程上下文占用的米数。 */
    private BigDecimal allocatedQuantity;
    /** 实际入库时间或计划预计入库时间。 */
    private LocalDateTime stockInTime;
    /** 静置期满后的可斜裁时间。 */
    private LocalDateTime releaseTime;

    public BigDecimal getRemainingQuantity() {
        BigDecimal available = availableQuantity == null ? BigDecimal.ZERO : availableQuantity;
        BigDecimal allocated = allocatedQuantity == null ? BigDecimal.ZERO : allocatedQuantity;
        return available.subtract(allocated).max(BigDecimal.ZERO);
    }

    public void addAllocatedQuantity(BigDecimal quantity) {
        BigDecimal current = allocatedQuantity == null ? BigDecimal.ZERO : allocatedQuantity;
        allocatedQuantity = current.add(quantity == null ? BigDecimal.ZERO : quantity);
    }
}