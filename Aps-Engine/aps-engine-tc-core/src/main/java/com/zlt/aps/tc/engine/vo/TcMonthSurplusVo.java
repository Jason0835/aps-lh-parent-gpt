package com.zlt.aps.tc.engine.vo;

import lombok.Data;

/**
 * 胎侧月度汇总VO
 */
@Data
public class TcMonthSurplusVo {

    /**
     * 物料代码
     */
    private String materialCode;

    /**
     * 月度计划完成量
     */
    private Double monthFinishQty;

    /**
     * 月度剩余量
     */
    private Double monthRemainQty;
}
