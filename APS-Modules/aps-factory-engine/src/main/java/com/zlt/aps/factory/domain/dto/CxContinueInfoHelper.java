package com.zlt.aps.factory.domain.dto;

import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产计划-在机结构续作信息
 * 包含在机的成型机台和对应的在机SKU
 * 已经SKU使用的模具-硫化机台信息
 *
 * @author ZLT
 * @date 20251215
 */
@Data
public class CxContinueInfoHelper implements Serializable {

    /**
     * 分组信息--TBR结构名
     */
    private String groupName;

    /**
     * 对应的成型机台
     */
    private Set<String> cxMachineCodeSet;
    /**
     * 续作SKU及模具数
     * key=materialDesc : value=续作sku信息(含使用的模具数)
     */
    private Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap;
    /**
     * 对应成型产能续作信息
     * 成型上在产的SKU和使用的模具数
     * key=cxMachineCode : value = { key = 物料描述 : value = cxContinueProductInfoHelper}
     * cxContinueProductInfoHelper = { 规格 主花纹 花纹 英寸 胎胚号 模具数}
     */
    private Map<String, Map<String, CxContinueProductInfoHelper>> cxMachineGroup;

    /**
     * 在机结构下成型产能的硫化配比信息集合
     */
    private List<ProductGroupCxCapacityInfo> cxCapacityInfoList;

    /**
     * 根据续作信息集合，构建以
     * 分组名(TBR-structureName)为key的续作成型信息
     * key = structureName(TBR) : value = 结构在产成型机及在产SKU、硫化机台数信息(模具数)
     * CxContinueInfoHelper.cxMachineGroup = { key = cxMachineCode : value = {key = materialDesc : value = 硫化机台数(模具数)}}
     *
     * @param continueProductionInfoList 排产续作信息
     * @param allCxMachineInfo           成型基础信息
     * @param structureLhRatioList       结构成型硫化配比
     * @return
     */
    public static Map<String, CxContinueInfoHelper> createGroupInfo(List<ContinueProductInfo> continueProductionInfoList, Map<String, CxMachineBaseInfoVo> allCxMachineInfo, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        if (CollectionUtils.isEmpty(continueProductionInfoList)) {
            return Collections.emptyMap();
        }
        //按分组名分组 TBR = structureName
        Map<String, List<ContinueProductInfo>> structureGroupMap = continueProductionInfoList.stream().collect(Collectors.groupingBy(ContinueProductInfo::getGroupName));
        Map<String, CxContinueInfoHelper> structureContinueGroup = new HashMap<>(structureGroupMap.size());
        //构建结构续作成型信息
        structureGroupMap.forEach((structureName, productionSkuList) -> {
            if (CollectionUtils.isEmpty(productionSkuList)) {
                return;
            }
            CxContinueInfoHelper helper = new CxContinueInfoHelper();
            helper.setGroupName(structureName);
            setContinueInfo(helper, allCxMachineInfo, productionSkuList);
            //在机结构的硫化配比(多机台情形下-用于续作判断优先下机的机台)
            helper.setCxCapacityInfoList(createGroupCxCapacityInfo(structureName, helper.getCxMachineCodeSet(), allCxMachineInfo, structureLhRatioList));
            structureContinueGroup.put(structureName, helper);
        });
        return structureContinueGroup;
    }

    /**
     * 根据续作排产信息，提取续作成型机台
     *
     * @param cxContinueInfoHelper 续作信息对象
     * @param allCxMachineGroup    所有成型机信息
     * @param continueSkuList      续作Sku信息
     * @return
     */
    private static void setContinueInfo(CxContinueInfoHelper cxContinueInfoHelper, Map<String, CxMachineBaseInfoVo> allCxMachineGroup, List<ContinueProductInfo> continueSkuList) {
        if (CollectionUtils.isEmpty(continueSkuList)) {
            cxContinueInfoHelper.setCxMachineCodeSet(new HashSet<>());
            cxContinueInfoHelper.setContinueSkuMouldNumberMap(new HashMap<>());
            return;
        }
        Set<String> cxMachineCodeSet = new HashSet<>();
        Map<String, CxContinueSkuInfoHelper> continueSkuMouldNumberMap = new HashMap<>();
        continueSkuList.forEach(continueProductInfo -> {
            continueProductInfo.extractEffectiveCxMachineCode(cxMachineCodeSet, allCxMachineGroup);
            continueProductInfo.extractSkuProductionMouldNumber(continueSkuMouldNumberMap);
        });
        cxContinueInfoHelper.setCxMachineCodeSet(cxMachineCodeSet);
        cxContinueInfoHelper.setContinueSkuMouldNumberMap(continueSkuMouldNumberMap);
    }

    /**
     * 某个分组名下，续作成型产能及对应续作SKU信息
     * key = cxMachineCode : value = { key = materialDesc : value = cxContinueProductInfoHelper }
     * cxContinueProductInfoHelper = { 规格 主花纹 花纹 英寸 胚胎号 模具数 }
     *
     * @param cxContinueGroup 结构分组成型续作信息
     * @return
     */
    private static Map<String, Map<String, CxContinueProductInfoHelper>> getCxMachineGroup(Map<String, List<ContinueProductInfo>> cxContinueGroup) {
        Map<String, Map<String, CxContinueProductInfoHelper>> cxMachineGroup = new HashMap<>();
        if (CollectionUtils.isEmpty(cxContinueGroup)) {
            return cxMachineGroup;
        }
        //某台成型产能下
        cxContinueGroup.forEach((cxMachineCode, continueSkuList) -> {
            if (CollectionUtils.isEmpty(continueSkuList)) {
                cxMachineGroup.put(cxMachineCode, Collections.emptyMap());
                return;
            }
            //按SKU分组
            Map<String, List<ContinueProductInfo>> continueSkuMap = continueSkuList.stream().collect(Collectors.groupingBy(ContinueProductInfo::getMaterialDesc));
            Map<String, CxContinueProductInfoHelper> continueSkuGroup = new HashMap<>();
            //某个续作SKU下，使用的模具数
            continueSkuMap.forEach((materialDesc, continueInfoList) -> {
                if (CollectionUtils.isEmpty(continueInfoList)) {
                    continueSkuGroup.put(materialDesc, new CxContinueProductInfoHelper());
                    return;
                }
                CxContinueProductInfoHelper helper = CxContinueProductInfoHelper.create(continueInfoList.get(BigDecimal.ZERO.intValue()));
                Integer sum = continueInfoList.stream().mapToInt(ContinueProductInfo::getLhMachineCount).sum();
                helper.setMouldNumber(sum);
                continueSkuGroup.put(materialDesc, helper);
            });
            cxMachineGroup.put(cxMachineCode, continueSkuGroup);
        });
        return cxMachineGroup;
    }

    /**
     * 构建在机结构的续作成型产能信息
     * 即每台成型对应的在产硫化机台数
     *
     * @param structureName         结构
     * @param continueCxMachineInfo 续作成型
     * @param allCxMachineInfo      成型基础信息
     * @param structureLhRatioList  结构成型配比信息
     * @return
     */
    private static List<ProductGroupCxCapacityInfo> createGroupCxCapacityInfo(String structureName, Set<String> continueCxMachineInfo, Map<String, CxMachineBaseInfoVo> allCxMachineInfo, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        if (CollectionUtils.isEmpty(continueCxMachineInfo)) {
            return Collections.emptyList();
        }
        List<ProductGroupCxCapacityInfo> cxCapacityList = new ArrayList<>(continueCxMachineInfo.size());
        continueCxMachineInfo.forEach(cxMachineCode -> {
            CxMachineBaseInfoVo baseInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == baseInfo) {
                return;
            }
            ProductGroupCxCapacityInfo capacityInfo = ProductGroupCxCapacityInfo.buildContinueCxCapacityInfo(structureName, cxMachineCode, baseInfo, structureLhRatioList);
            capacityInfo.setGroupName(structureName);
            capacityInfo.setCxMachineCode(cxMachineCode);
            cxCapacityList.add(capacityInfo);
        });
        return cxCapacityList;
    }

}
