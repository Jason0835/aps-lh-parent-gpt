package com.zlt.aps.nc.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class NcStockVo {

    /**
     * 垫胶代码
     */
    private String paddingCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
