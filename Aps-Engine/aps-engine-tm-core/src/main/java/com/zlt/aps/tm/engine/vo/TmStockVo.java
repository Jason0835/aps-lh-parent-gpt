package com.zlt.aps.tm.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class TmStockVo {

    /**
     * 胎面代码
     */
    private String treadCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
