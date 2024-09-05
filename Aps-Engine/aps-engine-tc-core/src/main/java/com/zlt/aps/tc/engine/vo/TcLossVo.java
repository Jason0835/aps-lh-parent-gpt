package com.zlt.aps.tc.engine.vo;

import lombok.Data;

/**
 * 损耗VO
 */
@Data
public class TcLossVo {

    /**
     * 损耗率key（物料编号#机台id）
     */
    private String lossKey;

    /**
     * 耗损率（百分比）
     */
    private Double lossRate;
}
