package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈6班次总计划量统计VO。
 *
 * <p>对应6个班次：</p>
 * <ul>
 *   <li>1班：D日中班</li>
 *   <li>2班：D+1日夜班</li>
 *   <li>3班：D+1日早班</li>
 *   <li>4班：D+1日中班</li>
 *   <li>5班：D+2日夜班</li>
 *   <li>6班：D+2日早班</li>
 * </ul>
 *
 * @author APS
 */
@Data
public class GsqTotalPlanQtyVo {

    public GsqTotalPlanQtyVo() {
        this.totalClass1PlanQty = 0D;
        this.totalClass2PlanQty = 0D;
        this.totalClass3PlanQty = 0D;
        this.totalClass4PlanQty = 0D;
        this.totalClass5PlanQty = 0D;
        this.totalClass6PlanQty = 0D;
        this.totalPlanQty = 0D;
    }

    /** 1班总计划量（D日中班） */
    private Double totalClass1PlanQty;

    /** 2班总计划量（D+1日夜班） */
    private Double totalClass2PlanQty;

    /** 3班总计划量（D+1日早班） */
    private Double totalClass3PlanQty;

    /** 4班总计划量（D+1日中班） */
    private Double totalClass4PlanQty;

    /** 5班总计划量（D+2日夜班） */
    private Double totalClass5PlanQty;

    /** 6班总计划量（D+2日早班） */
    private Double totalClass6PlanQty;

    /** 总计划量（6班次合计） */
    private Double totalPlanQty;
}
