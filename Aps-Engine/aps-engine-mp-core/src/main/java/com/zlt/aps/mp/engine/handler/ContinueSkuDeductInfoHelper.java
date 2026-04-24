package com.zlt.aps.mp.engine.handler;

import lombok.Getter;

import java.io.Serializable;

/**
 * 不同分组的主花纹下可降膜的Sku信息
 *
 * @author ZLT
 * @date 20260420
 */
@Getter
public class ContinueSkuDeductInfoHelper implements Serializable {
    /**
     * Sku信息
     */
    private String materialDesc;
    /**
     * 降膜日
     */
    private Integer deductDay;
    /**
     * 降膜数
     */
    private Integer deductMoldNumber;

    public ContinueSkuDeductInfoHelper(String materialDesc, Integer deductDay, Integer deductMoldNumber) {
        this.materialDesc = materialDesc;
        this.deductDay = deductDay;
        this.deductMoldNumber = deductMoldNumber;
    }
}
