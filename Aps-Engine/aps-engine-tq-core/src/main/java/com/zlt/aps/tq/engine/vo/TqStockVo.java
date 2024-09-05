package com.zlt.aps.tq.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class TqStockVo {

    /**
     * 胎圈代码
     */
    private String beadCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
