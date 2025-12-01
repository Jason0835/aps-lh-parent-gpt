package com.zlt.aps.tm.engine.vo;

import lombok.Data;

/**
 * 胎面中班和夜班总计划量Vo
 */
@Data
public class TmTotalPlanQtyVo {

    /**
     * 次日早班总计划量
     */
    private Double totalNextDayPlanQty;

    /**
     * 中班总计划量
     */
    private Double totalDayPlanQty;

    /**
     * 夜班总计划量
     */
    private Double totalNightPlanQty;

    public TmTotalPlanQtyVo() {
        this.totalDayPlanQty = 0D;
        this.totalNightPlanQty = 0D;
        this.totalPlanQty = 0D;
        this.totalNextDayPlanQty = 0D;
    }

    /**
     * 总计划量
     */
    private Double totalPlanQty;
}
