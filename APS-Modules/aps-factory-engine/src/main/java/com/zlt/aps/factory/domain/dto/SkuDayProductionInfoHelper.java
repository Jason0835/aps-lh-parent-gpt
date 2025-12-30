package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;

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
     * 创建空排产数据对象
     *
     * @param productionDay  排产日
     * @param productionPlan 排产计划
     */
    public static SkuDayProductionInfoHelper buildEmpty(Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan) {
        String materialDesc = productionPlan.getMaterialDesc();
        String materialCode = productionPlan.getMaterialCode();
        String groupName = productionPlan.getStructureName();
        Integer dayVulcanizationQty = productionPlan.getDayVulcanizationQty().intValue();
        SkuDayProductionInfoHelper helper = new SkuDayProductionInfoHelper(productionDay, materialDesc, materialCode, groupName, dayVulcanizationQty);
        return helper;
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
}
