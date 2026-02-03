package com.zlt.aps.factory.domain.dto;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
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
     * 备注说明
     */
    private String remark;

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
     * 是否匹配当前段排产结构
     * 1、如果没有排产天数 = false
     * 2、有排产天数，则判断是否同机台
     * 2.1、如果是同机台，则判断是否同结构
     * 2.1.1、如果是同结构
     * 2.1.1.1、则判断是否同段，同段排除 = false
     * 2.1.1.2、如果不同段，则 = true
     * 2.1.2、如果是不同结构，则 = false
     * 2.2、如果是不同机台，则判断是否同结构
     * 2.2.1 如果是同结构 = true
     * 2.2.2 如果不是同结构 = false
     *
     * @param currentProductionInfo
     * @return
     */
    public boolean hasMatchProduction(CxMachineAllocationPlanHelper currentProductionInfo) {
        if (null == currentProductionInfo) {
            return false;
        }
        String currentGroupName = currentProductionInfo.getAllocationGroup();
        if (StringUtils.isBlank(currentGroupName)) {
            return false;
        }
        if (null == allocationDay || allocationDay <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //同机台
        if (cxMachineCode.equals(currentProductionInfo.getCxMachineCode())) {
            if (!currentGroupName.equals(getAllocationGroup())) {
                return false;
            }
            String productionDuplicateKey = currentProductionInfo.getProductionDuplicateKey();
            return !productionDuplicateKey.equals(getProductionDuplicateKey());
        }
        //不同机台，只判断结构
        return currentGroupName.equals(getAllocationGroup());
    }

    /**
     * 同机台分配的天产能范围只能有一个结构
     * 结构|%|开始日|*|结束日
     *
     * @return
     */
    public String getProductionDuplicateKey() {
        String keyFormat = "%s|*|%s|*|%s";
        return String.format(keyFormat, productionPlanInfo.getGroupName(), startDay, endDay);
    }

    /**
     * 分配的分组名
     *
     * @return
     */
    public String getAllocationGroup() {
        if (null == productionPlanInfo) {
            return "";
        }
        return productionPlanInfo.getGroupName();
    }

    /**
     * 结构提前收尾处理
     * 新的收尾时间点及提前收尾的天数
     *
     * @param conclusionDay
     * @param deductionDay
     */
    public void beforeConclusion(Integer conclusionDay, Integer deductionDay) {
        endDay = conclusionDay - BigDecimal.ONE.intValue();
        String tisFormat = I18nUtil.getMessage("alg.data.groupCapacity.beforeConclusion");
        remark = String.format(tisFormat, conclusionDay);
        if (allocationDay <= deductionDay) {
            allocationDay = BigDecimal.ZERO.intValue();
            return;
        }
        allocationDay = allocationDay - deductionDay;
    }

    /**
     * 获取日分配Key
     *
     * @return
     */
    public String getDayAllocationKey() {
        String allocationKeyFormat = "%s|*|%s";
        return String.format(allocationKeyFormat, productionPlanInfo.getGroupName(), cxMachineCode);
    }

    /**
     * 获取分配的最小日排产量
     * = 硫化机台配比 * Sku日最小硫化量
     */
    public Integer getDayMinAllocationQty() {
        if (null == productionPlanInfo) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == maxRatio) {
            return BigDecimal.ZERO.intValue();
        }
        return productionPlanInfo.getDayMinCapacityByLhRatio(maxRatio);
    }

}
