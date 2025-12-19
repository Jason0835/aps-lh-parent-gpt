package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分组计划-成型产能信息对象
 * TBR-结构
 * 对应的机台-最大胎胚种类数
 * 最大硫化机台数
 * 最低硫化机台数
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class ProductGroupCxCapacityInfo implements Serializable {

    /**
     * 分组名 TBR为结构
     */
    private String groupName;

    /**
     * 成型产能机台
     */
    private String cxMachineCode;

    /**
     * 最大胎胚种类数
     */
    private Integer maxEmbryoCodeCount;

    /**
     * 最后一天实际硫化配比
     */
    private Integer realMaxLhMachineCount;
    /**
     * 最大硫化机台数
     */
    private Integer maxLhMachineCount;

    /**
     * 最低硫化机台数
     */
    private Integer minLhMachineCount;

    /**
     * 构建在机结构-在机机台的产能相关信息，结构成型硫化配比
     *
     * @param structureName        结构名
     * @param cxMachineCode        成型机台编码
     * @param continueSkuInfo      机台续作SKU及使用模具数信息
     * @param baseInfo             机台基础信息
     * @param structureLhRatioList 结构成型硫化配比集合
     * @return
     */
    public static ProductGroupCxCapacityInfo buildContinueCxCapacityInfo(String structureName, String cxMachineCode, Map<String, CxContinueProductInfoHelper> continueSkuInfo, CxMachineBaseInfoVo baseInfo, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        ProductGroupCxCapacityInfo capacityInfo = new ProductGroupCxCapacityInfo();
        capacityInfo.setGroupName(structureName);
        capacityInfo.setCxMachineCode(cxMachineCode);
        //实际硫化机台数
        List<CxContinueProductInfoHelper> realMouldCountList = new ArrayList<>(continueSkuInfo.values());
        if (CollectionUtils.isEmpty(realMouldCountList)) {
            capacityInfo.setRealMaxLhMachineCount(BigDecimal.ZERO.intValue());
        } else {
            Integer lhMachineCount = realMouldCountList.stream().mapToInt(CxContinueProductInfoHelper::getMouldNumber).sum() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            capacityInfo.setRealMaxLhMachineCount(lhMachineCount);
        }
        //原始配置的胎胚种类数和硫化机台配比
        capacityInfo.setMaxEmbryoCodeCount(BigDecimal.ZERO.intValue());
        capacityInfo.setMaxLhMachineCount(BigDecimal.ZERO.intValue());
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return capacityInfo;
        }
        if (null == baseInfo) {
            return capacityInfo;
        }
        //得到结构成型类型的配比
        MonthPlanStructureLhRatioVo lhRatio = structureLhRatioList.stream().filter(match -> match.isMatch(structureName, baseInfo.getCxMachineBrandCode())).findFirst().orElse(null);
        if (null == lhRatio) {
            return capacityInfo;
        }
        capacityInfo.setMaxEmbryoCodeCount(lhRatio.getMaxEmbryoQty());
        capacityInfo.setMaxLhMachineCount(lhRatio.getLhMachineMaxQty());
        return capacityInfo;
    }
}
