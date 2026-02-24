package com.zlt.aps.mp.engine.domain.dto;

import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Integer planDemandQty;
    /**
     * 日硫化量(单模)
     */
    private Integer dayVulcanizationQty;
    /**
     * 在机结构构建续作信息时赋值
     * 续作sku的排产计划集合
     */
    private List<MonthPlanProductionRequirePlanVo> continueSkuPlanList;
    /**
     * 续作Sku-开始在机成型机台
     */
    private Set<String> onLineCxMachineSet;

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

    /**
     * 是否需要排产
     * 计划排产量大于零
     *
     * @return
     */
    public boolean hasProduction() {
        if (null == planDemandQty) {
            return false;
        }
        return planDemandQty > BigDecimal.ZERO.intValue();
    }

    /**
     * 获取天单硫化机台产能
     * 单硫化机台天产能 = 双模产能 = 单模天产能 * 2
     *
     * @return
     */
    public Integer getMaxDaySingleLhMachineQty() {
        return dayVulcanizationQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 根据模具数，得到使用的硫化机台数
     *
     * @return
     */
    public Integer getUsedLhMachineCountByMouldNumber() {
        return mouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }

    /**
     * 根据使用的硫化机台组信息，获取使用模具集合所处下标
     *
     * @param assignedCount 当前使用的硫化机台组
     * @return
     */
    public Integer getUsedMouldIndex(int assignedCount) {
        return assignedCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
    }
}
