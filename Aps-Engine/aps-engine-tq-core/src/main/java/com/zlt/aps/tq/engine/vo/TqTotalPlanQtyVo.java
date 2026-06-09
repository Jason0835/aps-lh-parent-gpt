package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 胎圈6个班次总计划量Vo
 */
@Data
public class TqTotalPlanQtyVo {

    public TqTotalPlanQtyVo() {
        this.totalClass1PlanQty = 0D;
        this.totalClass2PlanQty = 0D;
        this.totalClass3PlanQty = 0D;
        this.totalClass4PlanQty = 0D;
        this.totalClass5PlanQty = 0D;
        this.totalClass6PlanQty = 0D;
        this.totalPlanQty = 0D;
    }

    /**
     * 1班(T-1日夜班)总计划量
     */
    private Double totalClass1PlanQty;

    /**
     * 2班(T-1日早班)总计划量
     */
    private Double totalClass2PlanQty;

    /**
     * 3班(T-1日中班)总计划量
     */
    private Double totalClass3PlanQty;

    /**
     * 4班(T日夜班)总计划量
     */
    private Double totalClass4PlanQty;

    /**
     * 5班(T日早班)总计划量
     */
    private Double totalClass5PlanQty;

    /**
     * 6班(T日中班)总计划量
     */
    private Double totalClass6PlanQty;

    /**
     * 总计划量
     */
    private Double totalPlanQty;
}
