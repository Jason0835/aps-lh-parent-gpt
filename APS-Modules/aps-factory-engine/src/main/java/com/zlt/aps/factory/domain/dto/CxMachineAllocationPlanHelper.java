package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    private Map<String, CxContinueProductInfoHelper> continueSkuMap;
    /**
     * 当前硫化配比
     */
    private Integer ratio;
    /**
     * 实际排产规格计划
     */
    private List<MonthPlanProductionRequirePlanVo> realProductionPlanList;

    /**
     * 构造函数
     *
     * @param productionPlanInfo 分配的分组计划信息
     * @param ratio              硫化配比
     * @param continueSkuMap     续作规格信息
     * @param allocationDay      分配的天数
     * @param startDay           起始天数
     * @param endDay             结束天数
     */
    public CxMachineAllocationPlanHelper(ProductionPlanGroupInfo productionPlanInfo, Integer ratio, Map<String, CxContinueProductInfoHelper> continueSkuMap, Integer allocationDay, Integer startDay, Integer endDay) {
        this.productionPlanInfo = productionPlanInfo;
        this.ratio = ratio;
        this.continueSkuMap = continueSkuMap;
        this.allocationDay = allocationDay;
        this.startDay = startDay;
        this.endDay = endDay;
        this.realProductionPlanList = new ArrayList<>();
    }

    /**
     * 增加排产计划
     * 模具排产后，需增加
     *
     * @param productionPlan 排产计划
     */
    public void addProductionPlan(MonthPlanProductionRequirePlanVo productionPlan) {
        if (null == productionPlan) {
            return;
        }
        realProductionPlanList.add(productionPlan);
    }

}
