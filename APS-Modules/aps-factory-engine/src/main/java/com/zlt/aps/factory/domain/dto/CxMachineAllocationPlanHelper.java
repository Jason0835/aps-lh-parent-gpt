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
     * 成型机台编号
     */
    private String cxMachineCode;
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
    private Map<String, CxContinueSkuInfoHelper> continueSkuMap;
    /**
     * 当前最大硫化配比
     */
    private Integer maxRatio;
    /**
     * 当前最低硫化配比
     */
    private Integer minRatio;
    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;
    /**
     * 实际排产规格计划
     */
    private List<MonthPlanProductionRequirePlanVo> realProductionPlanList;

    /**
     * 构造函数
     *
     * @param cxMachineCode      成型机台
     * @param productionPlanInfo 分配的分组计划信息
     * @param lhRatio            硫化配比信息
     * @param continueSkuMap     续作规格信息
     * @param allocationDay      分配的天数
     * @param startDay           起始天数
     * @param endDay             结束天数
     */
    public CxMachineAllocationPlanHelper(String cxMachineCode, ProductionPlanGroupInfo productionPlanInfo, ProductGroupCxCapacityInfo lhRatio, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Integer allocationDay, Integer startDay, Integer endDay) {
        this.cxMachineCode = cxMachineCode;
        this.productionPlanInfo = productionPlanInfo;
        this.maxRatio = lhRatio.getMaxLhMachineCount();
        this.minRatio = productionPlanInfo.getClosureMinLhRatio();
        this.maxEmbryoCodeCount = lhRatio.getMaxEmbryoCodeCount();
        this.continueSkuMap = continueSkuMap;
        this.allocationDay = allocationDay;
        this.startDay = startDay;
        this.endDay = endDay;
        this.realProductionPlanList = new ArrayList<>();
    }

    /**
     * 机台分配的天产能范围只能有一个结构
     *
     * @return
     */
    public String getDuplicateKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, cxMachineCode, startDay, endDay);
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
