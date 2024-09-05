package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈中班和夜班总计划量Vo
 */
@Data
public class GsqTotalPlanQtyVo {

    public GsqTotalPlanQtyVo() {
        this.totalDayPlanQty = 0D;
        this.totalMidPlanQty = 0D;
        this.totalNightPlanQty = 0D;
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
     * 总计划量
     */
    private Double totalPlanQty;
}
