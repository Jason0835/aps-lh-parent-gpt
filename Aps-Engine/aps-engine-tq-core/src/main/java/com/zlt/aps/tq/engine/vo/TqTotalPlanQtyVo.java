package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 胎圈中班和夜班总计划量Vo
 */
@Data
public class TqTotalPlanQtyVo {

    public TqTotalPlanQtyVo() {
        this.totalDayPlanQty = 0D;
        this.totalMidPlanQty = 0D;
        this.totalNightPlanQty = 0D;
        this.totalNextMidPlanQty = 0D;
        this.totalPlanQty = 0D;
    }

    /**
     * 中班总计划量
     */
    private Double totalMidPlanQty;

    /**
     * 夜班总计划量
     */
    private Double totalNightPlanQty;

    /**
     * 白班总计划量
     */
    private Double totalDayPlanQty;

    /**
     * 次日中班总计划量
     */
    private Double totalNextMidPlanQty;

    /**
     * 总计划量
     */
    private Double totalPlanQty;
}
