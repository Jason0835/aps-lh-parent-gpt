package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.enums.ProductionGroupTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 周期分组计算处理器
 *
 * @author ZLT
 * @date 20260326
 */
@Slf4j
public class CycleGroupCalculateHandler {

    /**
     * 获取结构下周期储备是否可排产
     *
     * @param context            排产上下文
     * @param skuMaterialDesc    排产Sku信息
     * @param productionPlanInfo 分组信息
     * @return
     */
    public static boolean checkCycleGroupHasProductionQty(Context context, String skuMaterialDesc, ProductionPlanGroupInfo productionPlanInfo) {
        if (null == productionPlanInfo) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> allSkuPlanList = productionPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> allSkuRequirePlanList = allSkuPlanList.stream().filter(single -> !YesOrNoEnum.NO.getCode().equals(single.getProductionFlag())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return false;
        }
        String structureType = allSkuRequirePlanList.get(BigDecimal.ZERO.intValue()).getStructureType();
        //非周期结构
        if (!ProductionGroupTypeEnum.CYCLE.getGroupType().equals(structureType)) {
            return true;
        }
        String groupName = productionPlanInfo.getGroupName();
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = productionPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return false;
        }
        Integer maxCycleQty = productionPlanInfo.getMaxCycleQty();
        Map<String, Integer> skuCycleProductionQtyMap = Maps.newHashMap();
        Set<String> needProductionSkuInfoSet = allSkuRequirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        needProductionSkuInfoSet.forEach(materialDesc -> skuCycleProductionQtyMap.put(materialDesc, getSkuCycleProductionQty(context, dayProductionLimitInfo, allSkuRequirePlanList, materialDesc)));
        if (CollectionUtils.isEmpty(skuCycleProductionQtyMap)) {
            TbrMouldProductionLogRecorder.addProductionCycleQtyInfoLog(context, groupName, skuMaterialDesc, BigDecimal.ZERO.intValue(), maxCycleQty);
            return maxCycleQty >= BigDecimal.ZERO.intValue();
        }
        Integer currentSkuCycleQty = skuCycleProductionQtyMap.get(skuMaterialDesc);
        if (null == currentSkuCycleQty || currentSkuCycleQty <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        Integer sumCycleQty = skuCycleProductionQtyMap.values().stream().mapToInt(Integer::intValue).sum();
        TbrMouldProductionLogRecorder.addProductionCycleQtyInfoLog(context, groupName, skuMaterialDesc, sumCycleQty, maxCycleQty);
        return maxCycleQty >= sumCycleQty;
    }

    /**
     * 从日排产信息中获取某个Sku的周期排产量
     *
     * @param dayProductionLimitInfo 日排产信息集合
     * @param allSkuPlanList         所有可排产Sku量
     * @param materialDesc           sku
     * @return
     */
    private static Integer getSkuCycleProductionQty(Context context, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo, List<MonthPlanProductionRequirePlanVo> allSkuPlanList, String materialDesc) {
        if (CollectionUtils.isEmpty(allSkuPlanList) || StringUtils.isBlank(materialDesc)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> skuPlanList = allSkuPlanList.stream().filter(single -> {
            if (YesOrNoEnum.NO.getCode().equals(single.getProductionFlag())) {
                return false;
            }
            if (!materialDesc.equals(single.getMaterialDesc())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuPlanList)) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        //实单量 奇数+3 偶数+2
        Integer minQty = skuPlanList.get(BigDecimal.ZERO.intValue()).getMinProductionQty();
        Integer sumActualQuantity = skuPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
        if (sumActualQuantity > BigDecimal.ZERO.intValue()) {
            if ((sumActualQuantity & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
                sumActualQuantity = sumActualQuantity + ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
            } else {
                sumActualQuantity = sumActualQuantity + ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
            }
        }
        if (sumActualQuantity < minQty) {
            sumActualQuantity = minQty;
        }
        String groupName = skuPlanList.get(BigDecimal.ZERO.intValue()).getStructureName();
        Map<Integer, Integer> dayProductionQtyMap = new HashMap<>();
        dayProductionLimitInfo.forEach((productionDay, dayLimitInfo) -> {
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = dayLimitInfo.getProductionSkuQtyInfo();
            if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
                dayProductionQtyMap.put(productionDay, BigDecimal.ZERO.intValue());
                return;
            }
            SkuDayProductionInfoHelper skuProductionInfo = productionSkuQtyInfo.get(materialDesc);
            if (null == skuProductionInfo) {
                dayProductionQtyMap.put(productionDay, BigDecimal.ZERO.intValue());
                return;
            }
            Integer dayProductionQty = Optional.ofNullable(skuProductionInfo.getSumProductionQty()).orElse(BigDecimal.ZERO.intValue());
            dayProductionQtyMap.put(productionDay, dayProductionQty);
        });
        Integer sumProductionQty = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(dayProductionQtyMap)) {
            sumProductionQty = dayProductionQtyMap.values().stream().mapToInt(Integer::intValue).sum();
        }
        TbrMouldProductionLogRecorder.addProductionCycleQtyDetailLog(context, groupName, materialDesc, sumProductionQty, sumActualQuantity);
        if (sumProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (sumProductionQty <= sumActualQuantity) {
            return BigDecimal.ZERO.intValue();
        }
        return sumProductionQty - sumActualQuantity;
    }


}
