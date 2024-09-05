package com.zlt.aps.gsq.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class GsqStockVo {

    /**
     * 钢丝圈代码
     */
    private String steelRingCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
