package com.zlt.aps.dj.engine.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 胎面中班和夜班总计划量Vo
 */
@Data
public class DjTotalPlanQtyVo {

    /**
     * 次日早班总计划量
     */
    private BigDecimal totalNextDayPlanQty;

    /**
     * 中班总计划量
     */
    private BigDecimal totalDayPlanQty;

    /**
     * 夜班总计划量
     */
    private BigDecimal totalNightPlanQty;

    public DjTotalPlanQtyVo() {
        this.totalDayPlanQty = BigDecimal.ZERO;
        this.totalNightPlanQty = BigDecimal.ZERO;
        this.totalPlanQty = BigDecimal.ZERO;
        this.totalNextDayPlanQty = BigDecimal.ZERO;
    }

    /**
     * 总计划量
     */
    private BigDecimal totalPlanQty;
}
