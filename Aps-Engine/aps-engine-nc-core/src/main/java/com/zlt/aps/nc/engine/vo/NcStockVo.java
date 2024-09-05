package com.zlt.aps.nc.engine.vo;

import lombok.Data;

/**
 * 库存量VO
 */
@Data
public class NcStockVo {

    /**
     * 内衬代码
     */
    private String liningCode;

    /**
     * 库存量
     */
    private Double stockNum;
}
