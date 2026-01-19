package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        ProductGroupCxCapacityInfo capacityInfo = buildContinueCxCapacityInfo(structureName, cxMachineCode, baseInfo, structureLhRatioList);
        //实际硫化机台数
        List<CxContinueProductInfoHelper> realMouldCountList = new ArrayList<>(continueSkuInfo.values());
        if (CollectionUtils.isEmpty(realMouldCountList)) {
            return capacityInfo;
        }
        Integer lhMachineCount = realMouldCountList.stream().mapToInt(CxContinueProductInfoHelper::getMouldNumber).sum() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        capacityInfo.setRealMaxLhMachineCount(lhMachineCount);
        return capacityInfo;
    }

    /**
     * 构建在机结构-在机机台的产能相关信息，结构成型硫化配比
     *
     * @param structureName        结构名
     * @param cxMachineCode        成型机台编码
     * @param baseInfo             机台基础信息
     * @param structureLhRatioList 结构成型硫化配比集合
     * @return
     */
    public static ProductGroupCxCapacityInfo buildContinueCxCapacityInfo(String structureName, String cxMachineCode, CxMachineBaseInfoVo baseInfo, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        ProductGroupCxCapacityInfo capacityInfo = createEmptyGroupCxCapacityInfo(structureName, cxMachineCode);
        //没有结构成型硫化配比信息及机台信息，则返回空实例
        if (CollectionUtils.isEmpty(structureLhRatioList) || null == baseInfo) {
            return capacityInfo;
        }
        //得到结构成型类型的配比
        MonthPlanStructureLhRatioVo lhRatio = getLhRation(baseInfo.getCxMachineTypeCode(), structureName, structureLhRatioList);
        if (null == lhRatio) {
            return capacityInfo;
        }
        //设置对应的最大胎胚数和最大硫化机台数、最低硫化机台数
        capacityInfo.setMaxEmbryoCodeCount(lhRatio.getMaxEmbryoQty());
        capacityInfo.setMaxLhMachineCount(lhRatio.getLhMachineMaxQty());
        capacityInfo.setMinLhMachineCount(lhRatio.getLhMachineMinQty());
        return capacityInfo;
    }

    /**
     * 根据机台、结构成型硫化配比配置
     * 转化成结构产能对象
     *
     * @param cxMachineCode 成型机台
     * @param lhRatio       对应的结构成型硫化配比配置
     * @return
     */
    public static ProductGroupCxCapacityInfo buildCxCapacityInfo(String cxMachineCode, MonthPlanStructureLhRatioVo lhRatio) {
        if (null == lhRatio || StringUtils.isBlank(cxMachineCode)) {
            return null;
        }
        ProductGroupCxCapacityInfo capacityInfo = createEmptyGroupCxCapacityInfo(lhRatio.getStructureName(), cxMachineCode);
        //设置对应的最大胎胚数和最大硫化机台数、最低硫化机台数
        capacityInfo.setMaxEmbryoCodeCount(lhRatio.getMaxEmbryoQty());
        capacityInfo.setMaxLhMachineCount(lhRatio.getLhMachineMaxQty());
        capacityInfo.setMinLhMachineCount(lhRatio.getLhMachineMinQty());
        return capacityInfo;
    }

    /**
     * 构建空的成型产能对象实例
     * 只有分组名、成型机台编号
     *
     * @param groupName     分组名
     * @param cxMachineCode 成型机台编号
     * @return
     */
    public static ProductGroupCxCapacityInfo createEmptyGroupCxCapacityInfo(String groupName, String cxMachineCode) {
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)) {
            return null;
        }
        return new ProductGroupCxCapacityInfo(groupName, cxMachineCode);
    }

    /**
     * 构建初始带有分组、成型机台的实例对象
     *
     * @param groupName     分组名
     * @param cxMachineCode 成型机台编号
     */
    private ProductGroupCxCapacityInfo(String groupName, String cxMachineCode) {
        this.groupName = groupName;
        this.cxMachineCode = cxMachineCode;
        this.maxEmbryoCodeCount = BigDecimal.ZERO.intValue();
        this.realMaxLhMachineCount = BigDecimal.ZERO.intValue();
        this.maxLhMachineCount = BigDecimal.ZERO.intValue();
        this.minLhMachineCount = BigDecimal.ZERO.intValue();
    }

    /**
     * 根据成型机型及结构名，得到其配比信息
     *
     * @param machineTypeCode      成型机机型
     * @param structureName        分组结构名
     * @param structureLhRatioList 成型硫化配比信息
     * @return
     */
    private static MonthPlanStructureLhRatioVo getLhRation(String machineTypeCode, String structureName, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        if (CollectionUtils.isEmpty(structureLhRatioList) || StringUtils.isBlank(machineTypeCode) || StringUtils.isBlank(structureName)) {
            return null;
        }
        List<MonthPlanStructureLhRatioVo> groupList = structureLhRatioList.stream().filter(lhRatio -> structureName.equals(lhRatio.getStructureName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(groupList)) {
            return null;
        }
        Map<String, MonthPlanStructureLhRatioVo> typeCodeMap = groupList.stream().collect(Collectors.toMap(MonthPlanStructureLhRatioVo::getCxMachineTypeCode, Function.identity(), (before, after) -> after));
        MonthPlanStructureLhRatioVo find = typeCodeMap.get(machineTypeCode);
        if (null != find) {
            return find;
        }
        return typeCodeMap.get(ProductionConstant.ALL_BRAND_CODE_MATCH);

    }
}
