package com.zlt.aps.mp.engine.handler;

import lombok.Getter;

import java.io.Serializable;

/**
 * 不同分组的主花纹下可降膜的Sku信息
 * 模具数大于2
 *
 * @author ZLT
 * @date 20260420
 */
@Getter
public class ContinueSkuDayUsedMouldInfoHelper implements Serializable {
    /**
     * Sku物料描述
     */
    private String materialDesc;
    /**
     * Sku物料编码
     */
    private String materialCode;
    /**
     * 排产日
     */
    private Integer productionDay;
    /**
     * 使用模具数
     */
    private Integer usedMouldNumber;
    /**
     * 计划降膜模具数
     */
    private Integer planDeductNumber;

    public ContinueSkuDayUsedMouldInfoHelper(String materialDesc, String materialCode, Integer productionDay, Integer usedMouldNumber) {
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.productionDay = productionDay;
        this.usedMouldNumber = usedMouldNumber;
    }

    /**
     * 剩余模具数
     *
     * @return
     */
    public Integer getLeftOverMouldNumber() {
        return usedMouldNumber - planDeductNumber;
    }

    public void setPlanDeductNumber(Integer planDeductNumber) {
        this.planDeductNumber = planDeductNumber;
    }
}
