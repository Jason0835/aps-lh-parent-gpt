package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 钢丝圈月度汇总VO
 */
@Data
public class GsqMonthSurplusVo {

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
