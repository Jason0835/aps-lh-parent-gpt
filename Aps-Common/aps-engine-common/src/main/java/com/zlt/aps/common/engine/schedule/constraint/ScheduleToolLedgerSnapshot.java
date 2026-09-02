package com.zlt.aps.common.engine.schedule.constraint;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 自动排程任务工装账本快照。
 */
@Data
public class ScheduleToolLedgerSnapshot {

    /** 结算前可用工装数量。 */
    private BigDecimal availableToolQty;

    /** 本次实际生产占用工装数量，不包含成型释放。 */
    private BigDecimal productionOccupationQty;

    /** 本次成型释放工装数量。 */
    private BigDecimal formingReleaseQty;

    /** 本次结算净占用工装数量。 */
    private BigDecimal toolUsedQty;

    /** 结算后剩余工装数量。 */
    private BigDecimal remainingToolQty;

    /** 本次自动排程采用的有效工装上限。 */
    private BigDecimal effectiveToolQtyLimit;
}
