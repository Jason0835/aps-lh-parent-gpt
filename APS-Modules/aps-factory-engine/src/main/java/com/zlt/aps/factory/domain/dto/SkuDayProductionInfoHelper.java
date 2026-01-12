package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Sku日排产信息对象
 * 用以辅助判断 收尾时间点等信息
 *
 * @author ZLT
 * @date 20251230
 */
@Getter
public class SkuDayProductionInfoHelper implements Serializable {
    /**
     * 排产日
     */
    private Integer productionDay;
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
     * 使用的模具编号
     */
    private Set<String> usedMouldSet;
    /**
     * 分组名
     * TBR 结构名
     * PCR 英寸
     */
    private String groupName;
    /**
     * 日总排产量
     */
    private Integer sumProductionQty;
    /**
     * 单模日硫化量
     */
    private Integer dayVulcanizationQty;
    /**
     * 换模或是换活字块的损耗量
     */
    private Integer lossQty;

    /**
     * 创建空排产数据对象
     *
     * @param productionDay  排产日
     * @param productionPlan 排产计划--无关具体Id
     * @param productionQty  实际排产量
     * @param lossQty        损耗量(换模或是换活字块)
     * @param usedMouldSet   排产的模具集合
     */
    public static SkuDayProductionInfoHelper buildEmpty(Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, Integer productionQty, Integer lossQty, Set<String> usedMouldSet) {
        String materialDesc = productionPlan.getMaterialDesc();
        String materialCode = productionPlan.getMaterialCode();
        String groupName = productionPlan.getStructureName();
        Integer dayVulcanizationQty = productionPlan.getDayVulcanizationQty().intValue();
        SkuDayProductionInfoHelper helper = new SkuDayProductionInfoHelper(productionDay, materialDesc, materialCode, groupName, dayVulcanizationQty);
        helper.sumProductionQty = productionQty;
        helper.embryoCode = productionPlan.getEmbryoCode();
        helper.usedMouldSet = usedMouldSet;
        helper.lossQty = lossQty;
        return helper;
    }

    /**
     * 增加排产量
     *
     * @param productionQty 需要增加的排产量
     */
    public void addProductionDayQty(Integer productionQty) {
        if (null == sumProductionQty) {
            sumProductionQty = BigDecimal.ZERO.intValue();
        }
        if (null == productionQty) {
            productionQty = BigDecimal.ZERO.intValue();
        }
        sumProductionQty = sumProductionQty + productionQty;
    }

    /**
     * 是否匹配同日同Sku
     *
     * @param currentProduction
     * @return
     */
    public boolean isMatchSameDayAndSku(SkuDayProductionInfoHelper currentProduction) {
        if (!productionDay.equals(currentProduction.getProductionDay())) {
            return false;
        }
        return embryoCode.equals(currentProduction.getEmbryoCode());
    }

    /**
     * 构建函数
     *
     * @param productionDay       排产日
     * @param materialDesc        物料描述
     * @param materialCode        物料编码
     * @param groupName           分组名
     * @param dayVulcanizationQty 日硫化产能
     */
    private SkuDayProductionInfoHelper(Integer productionDay, String materialDesc, String materialCode, String groupName, Integer dayVulcanizationQty) {
        this.productionDay = productionDay;
        this.materialDesc = materialDesc;
        this.materialCode = materialCode;
        this.groupName = groupName;
        this.sumProductionQty = BigDecimal.ZERO.intValue();
        this.dayVulcanizationQty = dayVulcanizationQty;
    }

    /**
     * 最后余量
     *
     * @return
     */
    public Integer getLastRemainder() {
        return sumProductionQty % getDayLhMachineQty();
    }

    /**
     * 硫化机台日硫化量
     *
     * @return
     */
    public Integer getDayLhMachineQty() {
        return dayVulcanizationQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }
}
