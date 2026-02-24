package com.zlt.aps.mp.engine.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 硫化排产量辅助对象
 * 用以值传递，没有其它特殊含义
 *
 * @author ZLT
 * @date 20251219
 */
@Data
public class LhProductionQtyHelper implements Serializable {
    /**
     * 分组计划信息对象
     */
    private ProductionPlanGroupInfo productionPlanInfo;
    /**
     * 成型机台
     */
    private Set<String> cxMachineInfo;
    /**
     * 成型机台的硫化组
     */
    private CxLhProductionHelper cxLhGroup;
    /**
     * 需要排产的总量
     */
    private Integer sumProductionQty;
    /**
     * 实际排产总量
     */
    private Integer realSumProductionQty;
    /**
     * 双模日硫化量
     */
    private Integer dayMaxProductionQty;

    /**
     * 构建对象实例
     *
     * @param productionPlanInfo   分组计划信息对象
     * @param cxMachineInfo        成型机台，可以为空
     * @param cxLhGroup            成型硫化组
     * @param sumProductionQty     需要排产的总量
     * @param realSumProductionQty 实际排产总量
     * @param dayMaxProductionQty  日双模最大硫化量
     */
    public LhProductionQtyHelper(ProductionPlanGroupInfo productionPlanInfo, Set<String> cxMachineInfo, CxLhProductionHelper cxLhGroup, Integer sumProductionQty, Integer realSumProductionQty, Integer dayMaxProductionQty) {
        this.productionPlanInfo = productionPlanInfo;
        this.cxMachineInfo = cxMachineInfo;
        this.cxLhGroup = cxLhGroup;
        this.sumProductionQty = sumProductionQty;
        this.realSumProductionQty = realSumProductionQty;
        this.dayMaxProductionQty = dayMaxProductionQty;
    }
}
