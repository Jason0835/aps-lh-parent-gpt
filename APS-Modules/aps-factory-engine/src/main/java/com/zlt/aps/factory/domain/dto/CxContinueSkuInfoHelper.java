package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在机结构-续作Sku信息
 * 包含基础信息及当时使用的模具总数
 * 基础信息：分组名-结构、物料编码、物料描述、英寸
 * 胎胚号、规格、主花纹、花纹
 *
 * @author zlt
 * @date 20251224
 */
@Data
public class CxContinueSkuInfoHelper implements Serializable {

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料描述
     */
    private String materialDesc;

    /**
     * 英寸
     */
    private String proSize;

    /**
     * 胎胚号
     */
    private String embryoCode;

    /**
     * 规格
     */
    private String specifications;

    /**
     * 主花纹
     */
    private String mainPattern;

    /**
     * 花纹
     */
    private String pattern;
    /**
     * 模具数
     */
    private Integer mouldNumber;
    /**
     * 计划需求量--高优先级或是总排产量？
     */
    private Long planDemandQty;
    /**
     * 日硫化量(单模)
     */
    private Long dayVulcanizationQty;

    /**
     * 先从排产计划中获取materialDesc,如果没有匹配到，从续作中获取
     *
     * @param productionPlanList 分组排产计划
     * @param continueSkuMap     成型初始的续作Sku信息
     * @return
     */
    public static CxContinueSkuInfoHelper buildContinueProductInfo(String materialDesc, List<MonthPlanProductionRequirePlanVo> productionPlanList, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return continueSkuMap.get(materialDesc);
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanList = productionPlanList.stream().filter(groupPlan -> materialDesc.equals(groupPlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return continueSkuMap.get(materialDesc);
        }
        MonthPlanProductionRequirePlanVo plan = groupPlanList.get(BigDecimal.ZERO.intValue());
        CxContinueSkuInfoHelper continueSkuInfo = new CxContinueSkuInfoHelper();
        continueSkuInfo.setEmbryoCode(plan.getEmbryoCode());
        continueSkuInfo.setSpecifications(plan.getSpecifications());
        continueSkuInfo.setPattern(plan.getPattern());
        continueSkuInfo.setMainPattern(plan.getMainPattern());
        continueSkuInfo.setProSize(plan.getProSize());
        continueSkuInfo.setGroupName(plan.getStructureName());
        continueSkuInfo.setDayVulcanizationQty(plan.getDayVulcanizationQty());
        return continueSkuInfo;
    }
}
