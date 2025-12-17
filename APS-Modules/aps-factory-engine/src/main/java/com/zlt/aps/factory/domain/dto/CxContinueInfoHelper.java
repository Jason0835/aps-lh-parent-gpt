package com.zlt.aps.factory.domain.dto;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排产计划-成型续作信息
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
     * 对应成型产能续作信息
     * key: TBR structureName, value: key:物料描述 value 模具数 = 硫化机台数
     */
    private Map<String, Map<String, Integer>> cxMachineGroup;

    /**
     * 续作下成型产能信息集合
     */
    private List<ProductGroupCxCapacityInfo> cxCapacityInfoList;

    /**
     * 根据续作信息集合，构建以
     * 分组名(TBR-structureName)为key的续作成型信息
     *
     * @param continueProductionInfoList
     * @return
     */
    public static Map<String, CxContinueInfoHelper> createGroupInfo(List<ContinueProductInfo> continueProductionInfoList) {
        if (CollectionUtils.isEmpty(continueProductionInfoList)) {
            return Collections.emptyMap();
        }
        //按分组名分组 TBR = structureName
        Map<String, List<ContinueProductInfo>> structureGroupMap = continueProductionInfoList.stream().collect(Collectors.groupingBy(ContinueProductInfo::getGroupName));
        Map<String, CxContinueInfoHelper> structureContinueGroup = new HashMap<>(structureGroupMap.size());
        //获取续作成型信息
        structureGroupMap.forEach((structureName, productionSkuList) -> {
            if (CollectionUtils.isEmpty(productionSkuList)) {
                return;
            }
            CxContinueInfoHelper helper = new CxContinueInfoHelper();
            helper.setGroupName(structureName);
            Map<String, List<ContinueProductInfo>> cxContinueGroup = productionSkuList.stream().collect(Collectors.groupingBy(ContinueProductInfo::getCxMachineCode));
            Map<String, Map<String, Integer>> cxMachineGroup = getCxMachineGroup(cxContinueGroup);
            helper.setCxMachineGroup(cxMachineGroup);
            structureContinueGroup.put(structureName, helper);
        });
        return structureContinueGroup;
    }

    /**
     * 某个分组名下，续作成型产能及对应续作SKU信息
     *
     * @param cxContinueGroup
     * @return
     */
    private static Map<String, Map<String, Integer>> getCxMachineGroup(Map<String, List<ContinueProductInfo>> cxContinueGroup) {
        Map<String, Map<String, Integer>> cxMachineGroup = new HashMap<>();
        if (CollectionUtils.isEmpty(cxContinueGroup)) {
            return cxMachineGroup;
        }
        //某台成型产能下
        cxContinueGroup.forEach((cxMachineCode, continueSkuList) -> {
            if (CollectionUtils.isEmpty(continueSkuList)) {
                cxMachineGroup.put(cxMachineCode, Collections.emptyMap());
                return;
            }
            Map<String, List<ContinueProductInfo>> continueSkuMap = continueSkuList.stream().collect(Collectors.groupingBy(ContinueProductInfo::getMaterialDesc));
            Map<String, Integer> continueSkuGroup = new HashMap<>();
            //某个续作SKU下
            continueSkuMap.forEach((materialDesc, continueInfoList) -> {
                if (CollectionUtils.isEmpty(continueInfoList)) {
                    continueSkuGroup.put(materialDesc, BigDecimal.ZERO.intValue());
                    return;
                }
                Integer sum = continueInfoList.stream().mapToInt(ContinueProductInfo::getLhMachineCount).sum();
                continueSkuGroup.put(materialDesc, sum);
            });
            cxMachineGroup.put(cxMachineCode, continueSkuGroup);
        });
        return cxMachineGroup;
    }

}
