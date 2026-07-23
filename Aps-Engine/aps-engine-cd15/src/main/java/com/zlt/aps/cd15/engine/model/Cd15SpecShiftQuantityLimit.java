package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 当前班同一钢带代码的累计计划量限制。 */
@Data
@Builder
public class Cd15SpecShiftQuantityLimit {

    /** 当前班实际采用的单规格上限。 */
    private BigDecimal shiftLimit;
    /** 当前班该钢带已经提交的计划量。 */
    private BigDecimal scheduledQuantity;
    /** 当前班该钢带剩余可排量。 */
    private BigDecimal remainingQuantity;
    /** 是否为停班后恢复生产的实际复产班次。 */
    private boolean restartStockMode;
}
