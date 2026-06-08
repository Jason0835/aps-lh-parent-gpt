package com.zlt.aps.dj.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class DjStockVo {

    /**
     * 垫胶代码
     */
    private String liningCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
