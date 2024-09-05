package com.zlt.aps.nc.engine.vo;

import lombok.Data;

/**
 * 胎面中班和夜班总计划量Vo
 */
@Data
public class NcTotalPlanQtyVo {

    public NcTotalPlanQtyVo() {
        this.totalDayPlanQty = 0D;
        this.totalNightPlanQty = 0D;
        this.totalPlanQty = 0D;
    }

    /**
     * 中班总计划量
     */
    private Double totalDayPlanQty;

    /**
     * 夜班总计划量
     */
    private Double totalNightPlanQty;

    /**
     * 总计划量
     */
    private Double totalPlanQty;
}
