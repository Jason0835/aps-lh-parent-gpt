package com.zlt.aps.factory.domain.dto;

import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

/**
 * 成型机台-分配分组计划信息
 * TBR按结构
 *
 * @author ZLT
 * @date 20251215
 */
@Getter
public class CxMachineAllocationPlanHelper implements Serializable {

    /**
     * 分组计划信息
     */
    private ProductionPlanGroupInfo productionPlanInfo;

    /**
     * 分配的天数
     */
    private Integer allocationDay;

    /**
     * 起始天数
     */
    private Integer startDay;

    /**
     * 结束天数
     */
    private Integer endDay;

    /**
     * 续作规格信息
     */
    private Map<String, Integer> continueSkuMap;

    /**
     * 构造函数
     *
     * @param productionPlanInfo 分配的分组计划信息
     * @param continueSkuMap     续作规格信息
     * @param allocationDay      分配的天数
     * @param startDay           起始天数
     * @param endDay             结束天数
     */
    public CxMachineAllocationPlanHelper(ProductionPlanGroupInfo productionPlanInfo, Map<String, Integer> continueSkuMap, Integer allocationDay, Integer startDay, Integer endDay) {
        this.productionPlanInfo = productionPlanInfo;
        this.allocationDay = allocationDay;
        this.startDay = startDay;
        this.endDay = endDay;
    }

}
