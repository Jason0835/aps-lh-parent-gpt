package com.zlt.aps.tc.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class TcStockVo {

    /**
     * 胎侧代码
     */
    private String sidewallCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
