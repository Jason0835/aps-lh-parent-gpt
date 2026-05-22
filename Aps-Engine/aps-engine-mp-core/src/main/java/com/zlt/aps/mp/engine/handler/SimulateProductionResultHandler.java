package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模拟排产后结果处理器
 *
 * @author ZLT
 * @date 20260522
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulateProductionResultHandler {

    /**
     * 检测续作Sku，在只有中优先级需求量时，是否直接续作排产
     * 1、只有中优先级(包含周期结构的周期储备)
     * 2、在模拟排产阶段又可以排产上
     * 则正式排产时，直接在续作阶段就排产
     *
     * @param productionContext 排产上下文
     * @param cxContinueInfoMap 续作Sku信息
     */
    public void checkSimulateResultHandler(TbrProductionContext productionContext, Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        if (CollectionUtils.isEmpty(cxContinueInfoMap)) {
            return;
        }
        //所有分组计划
        Map<String, ProductionPlanGroupInfo> allGroupDataMap = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupDataMap)) {
            return;
        }
        Set<String> allContinueConditionSkuSet = Sets.newHashSet();
        cxContinueInfoMap.forEach((structureName, allContinueSkuInfo) -> {
            Set<String> groupConditionSkuSet = getNoHeightQtyAndHasSimulateProductionQtySku(productionContext, allGroupDataMap, allContinueSkuInfo);
            if (CollectionUtils.isEmpty(groupConditionSkuSet)) {
                return;
            }
            allContinueConditionSkuSet.addAll(groupConditionSkuSet);
        });
        if (CollectionUtils.isEmpty(allContinueConditionSkuSet)) {
            return;
        }
        productionContext.getSimulateResult().setContinueSkuCanFirstInfo(allContinueConditionSkuSet);
    }

    /**
     * 获取当前在产结构的续作Sku是否符合条件
     * 1、续作Sku没有高优先级量，但有净需求量
     * 2、在模拟排产阶段，续作Sku有过排产
     *
     * @param productionContext    排产上下文
     * @param allGroupDataMap      分组下所有计划
     * @param groupContinueSkuInfo 分组下的所有续作Sku
     * @return
     */
    private Set<String> getNoHeightQtyAndHasSimulateProductionQtySku(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupDataMap, CxContinueInfoHelper groupContinueSkuInfo) {
        if (null == groupContinueSkuInfo || CollectionUtils.isEmpty(allGroupDataMap)) {
            return Collections.emptySet();
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = groupContinueSkuInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuMap)) {
            return Collections.emptySet();
        }
        String structureName = groupContinueSkuInfo.getGroupName();
        ProductionPlanGroupInfo groupInfo = allGroupDataMap.get(structureName);
        if (null == groupInfo) {
            return Collections.emptySet();
        }
        List<MonthPlanProductionRequirePlanVo> allGroupPlanList = groupInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(allGroupPlanList)) {
            return Collections.emptySet();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = allGroupPlanList.stream().filter(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return Collections.emptySet();
        }
        Set<String> conditionSet = Sets.newHashSet();
        continueSkuMap.forEach((materialDesc, continueSkuInfo) -> {
            if (!isNoHeightRequireQty(hasProductionList, materialDesc)) {
                return;
            }
            //模拟阶段，没有排产过
            if (!hasProductionQtyBySimulateProduction(productionContext, materialDesc)) {
                return;
            }
            conditionSet.add(materialDesc);
        });
        if (CollectionUtils.isEmpty(conditionSet)) {
            return Collections.emptySet();
        }
        return conditionSet;
    }

    /**
     * 是否没有高需求量，而有净需求
     *
     * @param hasProductionList 分组下所有可排产计划
     * @param materialDesc      续作Sku
     * @return
     */
    private boolean isNoHeightRequireQty(List<MonthPlanProductionRequirePlanVo> hasProductionList, String materialDesc) {
        if (CollectionUtils.isEmpty(hasProductionList) || StringUtils.isBlank(materialDesc)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> skuRequireList = hasProductionList.stream().filter(singlePlan -> materialDesc.equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuRequireList)) {
            return false;
        }
        Integer sumHeightRequireQty = skuRequireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginHeightProductionQty).sum();
        if (null != sumHeightRequireQty && sumHeightRequireQty > BigDecimal.ZERO.intValue()) {
            return false;
        }
        Integer sumNetRequireQty = skuRequireList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getOriginProductionQty).sum();
        if (null == sumNetRequireQty && sumNetRequireQty > BigDecimal.ZERO.intValue()) {
            return true;
        }
        return false;
    }

    /**
     * 在模拟排产阶段，是否有排产
     * 只要有一副模具排产过，则表示有排产
     *
     * @param productionContext 排产上下文
     * @param materialDesc      续作Sku
     * @return
     */
    private boolean hasProductionQtyBySimulateProduction(TbrProductionContext productionContext, String materialDesc) {
        if (StringUtils.isBlank(materialDesc)) {
            return false;
        }
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Map<String, List<MonthPlanProductMouldInfoVo>> allSkuRelateMap = baseDataContainer.getSkuMouldRelationMap();
        if (CollectionUtils.isEmpty(allSkuRelateMap)) {
            return false;
        }
        List<MonthPlanProductMouldInfoVo> skuMoldList = allSkuRelateMap.get(materialDesc);
        if (CollectionUtils.isEmpty(skuMoldList)) {
            return false;
        }
        Map<String, ProductionMouldInfoVo> allMoldProductionInfoMap = baseDataContainer.getMouldInfoMap();
        if (CollectionUtils.isEmpty(allMoldProductionInfoMap)) {
            return false;
        }
        for (MonthPlanProductMouldInfoVo skuMoldInfo : skuMoldList) {
            String mouldCode = skuMoldInfo.getMouldCode();
            if (isProductionSkuByMold(allMoldProductionInfoMap, mouldCode, materialDesc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断模具是否排产了指定Sku
     *
     * @param allMoldProductionInfoMap 所有模具信息
     * @param mouldCode                模具编号
     * @param materialDesc             排产Sku
     * @return
     */
    private boolean isProductionSkuByMold(Map<String, ProductionMouldInfoVo> allMoldProductionInfoMap, String mouldCode, String materialDesc) {
        if (StringUtils.isBlank(materialDesc) || StringUtils.isBlank(mouldCode) || CollectionUtils.isEmpty(allMoldProductionInfoMap)) {
            return false;
        }
        ProductionMouldInfoVo moldInfo = allMoldProductionInfoMap.get(mouldCode);
        if (null == moldInfo) {
            return false;
        }
        return moldInfo.hasProductionSku(materialDesc);
    }

}
