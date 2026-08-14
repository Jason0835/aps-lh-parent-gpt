package com.zlt.aps.mp.engine.handler.appoint;

import lombok.Getter;

import java.io.Serializable;

/**
 * 在机结构续作Sku强制下机优先级信息
 * 指定机台时间段业务辅助类
 *
 * @author ZLT
 * @date 20260810
 */
@Getter
public class DayReduceLhMachinePriorityInfo implements Serializable {
    /**
     * 物料描述
     */
    private String materialDesc;
    /**
     * 物料编码
     */
    private String materialCode;
    /**
     * 生胎代码
     */
    private String embryoCode;
    /**
     * 是否为长荣品牌
     */
    private Boolean isOemBrand;
    /**
     * 是否模具受限
     */
    private Boolean isMoldCapacityLimit;
    /**
     * 是否有高优先级量
     */
    private Boolean hasHeightPriority;
    /**
     * 排产Sku量
     */
    private Integer productionQty;
    /**
     * 库销比
     */
    private double inventorySaleRatio;

    /**
     * 构造函数
     *
     * @param materialDesc
     * @param materialCode
     * @param embryoCode
     * @param isOemBrand
     * @param isMoldCapacityLimit
     * @param hasHeightPriority
     * @param productionQty
     * @param inventorySaleRatio
     */
    public DayReduceLhMachinePriorityInfo(String materialDesc,
                                          String materialCode,
                                          String embryoCode,
                                          Boolean isOemBrand,
                                          Boolean isMoldCapacityLimit,
                                          Boolean hasHeightPriority,
                                          Integer productionQty,
                                          double inventorySaleRatio) {
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.embryoCode = embryoCode;
        this.isOemBrand = isOemBrand;
        this.isMoldCapacityLimit = isMoldCapacityLimit;
        this.hasHeightPriority = hasHeightPriority;
        this.productionQty = productionQty;
        this.inventorySaleRatio = inventorySaleRatio;
    }
}
