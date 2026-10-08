package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品级实际收尾运行态。
 *
 * <p>状态只存在于单批次上下文中，按工序产品隔离；首次确认后的实际收尾班次
 * 固定保存，后续重复标识不能重置该起点。</p>
 */
@Data
public class SchedulePostTailCloseOutState {

    /**
     * 工序产品编码。
     */
    private String processCode;

    /**
     * 是否已经在实际库存关账后确认收尾。
     */
    private Boolean confirmed;

    /**
     * 实际收尾逻辑班次 C。
     */
    private Integer actualCloseOutShiftOrder;

    /**
     * 确认时使用的当前班次。
     */
    private Integer confirmationShiftOrder;

    /**
     * 收尾来源唯一键摘要。
     */
    private String sourceKeySummary;

    /**
     * 确认依据或未确认原因。
     */
    private String confirmationReason;

    /**
     * 实际关账后产品可用库存。
     */
    private BigDecimal closedStockQty;

    /**
     * 扣除用于弥补历史真实短缺后，可用于收尾后续需求的关账库存。
     */
    private BigDecimal availableCloseOutStockQty;

    /**
     * 收尾范围内尚未弥补的真实短缺。
     */
    private BigDecimal unresolvedShortageQty;

    /**
     * 收尾范围内后续尚未消耗的有效需求。
     */
    private BigDecimal remainingCloseOutDemandQty;
}
