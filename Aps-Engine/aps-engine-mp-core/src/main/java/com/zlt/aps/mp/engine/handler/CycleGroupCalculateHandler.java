package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Maps;
import com.zlt.aps.enums.ProductionGroupTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.CycleProductionModeEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
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
     * 场景：周期储备量无需排产太多，
     * 根据参数比例可知当前结构最大储备量上限
     * 排产时，检测当前Sku排产的周期储备量是否达到结构最大储备量上限
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
        List<MonthPlanProductionRequirePlanVo> allSkuRequirePlanList = allSkuPlanList.stream().filter(single -> !YesOrNoEnum.NO.getCode().equals(single.getIsProduction())).collect(Collectors.toList());
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
        //获取分组结构内：所有Sku的周期排产量信息
        Map<String, Integer> skuCycleProductionQtyMap = getScheduledProductionCycleQty(context, allSkuRequirePlanList, dayProductionLimitInfo);
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
     * 获取续作Sku最大可排产量
     * 周期结构 ： 实单量 + 最大可排产周期量
     * 非周期结构：直接
     *
     * @param context            排产上下文
     * @param continueSkuInfo    续作Sku信息
     * @param productionPlanInfo 分组计划
     * @return
     */
    public static Integer getSingleSkuMaxQty(Context context, CxContinueSkuInfoHelper continueSkuInfo, ProductionPlanGroupInfo productionPlanInfo) {
        if (null == productionPlanInfo || null == continueSkuInfo) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> allSkuPlanList = productionPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> allSkuRequirePlanList = allSkuPlanList.stream().filter(single -> !YesOrNoEnum.NO.getCode().equals(single.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allSkuPlanList)) {
            return null;
        }
        //非周期结构
        if (!productionPlanInfo.isCycleType()) {
            return continueSkuInfo.getPlanDemandQty();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = productionPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return continueSkuInfo.getPlanDemandQty();
        }
        //20260710+ 周期结构-排产模式
        Integer maxCycleQty = productionPlanInfo.getMaxCycleQty();
        //获取分组结构内：所有Sku的周期排产量信息
        Map<String, Integer> skuCycleProductionQtyMap = getScheduledProductionCycleQty(context, allSkuRequirePlanList, dayProductionLimitInfo);
        Integer sumProductionCycleQty = BigDecimal.ZERO.intValue();
        if (!CollectionUtils.isEmpty(skuCycleProductionQtyMap)) {
            sumProductionCycleQty = skuCycleProductionQtyMap.values().stream().mapToInt(Integer::intValue).sum();
        }
        //得到剩余还可排产周期储备量
        Integer leftOverCycleQty = maxCycleQty - sumProductionCycleQty;
        if (leftOverCycleQty <= BigDecimal.ZERO.intValue()) {
            leftOverCycleQty = BigDecimal.ZERO.intValue();
        }
        Integer sumActualQuantity = getCycleActualQuantity(context, allSkuPlanList, continueSkuInfo.getMaterialDesc());
        Integer planCycleQty = continueSkuInfo.getPlanDemandQty() - sumActualQuantity;
        if (planCycleQty <= BigDecimal.ZERO.intValue()) {
            planCycleQty = BigDecimal.ZERO.intValue();
        }
        return sumActualQuantity + Math.min(planCycleQty, leftOverCycleQty);
    }

    /**
     * 从日排产信息中获取某个Sku的周期排产量
     *
     * @param context                排产上下文
     * @param dayProductionLimitInfo 日排产信息集合
     * @param allSkuPlanList         所有可排产Sku量
     * @param materialDesc           sku
     * @return
     */
    private static Integer getSkuCycleProductionQty(Context context, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo, List<MonthPlanProductionRequirePlanVo> allSkuPlanList, String materialDesc) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        Integer sumActualQuantity = getCycleActualQuantity(context, allSkuPlanList, materialDesc);
        if (null == sumActualQuantity) {
            return BigDecimal.ZERO.intValue();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionCapacityParamConfiguration paramConfiguration = productionContext.getBaseDataContainer().getParamConfiguration();
        String isWriteLog = Optional.ofNullable(paramConfiguration.getIsWriteCycleLog()).orElse("N");
        String groupName = allSkuPlanList.get(BigDecimal.ZERO.intValue()).getStructureName();
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
        if (ProductionConstant.YES_VALUE.equals(isWriteLog)) {
            TbrMouldProductionLogRecorder.addProductionCycleQtyDetailLog(context, groupName, materialDesc, sumProductionQty, sumActualQuantity);
        }
        if (sumProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (sumProductionQty <= sumActualQuantity) {
            return BigDecimal.ZERO.intValue();
        }
        return sumProductionQty - sumActualQuantity;
    }

    /**
     * 从分组的日排产信息中提取各Sku已排产的周期储备量信息
     * Key 排产Sku物料描述，Value 周期储备已排产量
     *
     * @param context                        排产上下文
     * @param allEffectiveSkuRequirePlanList 分组对象中所有有效的计划集合
     * @param dayProductionLimitInfo         分组对象-日排产信息
     * @return
     */
    private static Map<String, Integer> getScheduledProductionCycleQty(Context context, List<MonthPlanProductionRequirePlanVo> allEffectiveSkuRequirePlanList, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        if (CollectionUtils.isEmpty(allEffectiveSkuRequirePlanList) || CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyMap();
        }
        Map<String, Integer> skuCycleProductionQtyMap = Maps.newHashMap();
        //提取可能有排产了周期储备量的Sku信息
        Set<String> needProductionSkuInfoSet = allEffectiveSkuRequirePlanList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        needProductionSkuInfoSet.forEach(materialDesc -> skuCycleProductionQtyMap.put(materialDesc, getSkuCycleProductionQty(context, dayProductionLimitInfo, allEffectiveSkuRequirePlanList, materialDesc)));
        if (CollectionUtils.isEmpty(skuCycleProductionQtyMap)) {
            return Collections.emptyMap();
        }
        return skuCycleProductionQtyMap;
    }

    /**
     * 获取周期结构某个Sku的实单量
     *
     * @param context        排产上下文
     * @param allSkuPlanList 分组计划所有计划
     * @param materialDesc   某个Sku
     * @return
     */
    private static Integer getCycleActualQuantity(Context context, List<MonthPlanProductionRequirePlanVo> allSkuPlanList, String materialDesc) {
        if (CollectionUtils.isEmpty(allSkuPlanList) || StringUtils.isBlank(materialDesc)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> skuPlanList = allSkuPlanList.stream().filter(single -> {
            if (YesOrNoEnum.NO.getCode().equals(single.getIsProduction())) {
                return false;
            }
            if (!materialDesc.equals(single.getMaterialDesc())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuPlanList)) {
            return null;
        }
        //实单量 奇数+3 偶数+2
        Integer sumActualQuantity = skuPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getActualQuantity).sum();
        sumActualQuantity = getQuantity(sumActualQuantity);
        Integer minQty = skuPlanList.get(BigDecimal.ZERO.intValue()).getMinProductionQty();
        if (sumActualQuantity < minQty) {
            sumActualQuantity = minQty;
        }
        return sumActualQuantity;
    }

    /**
     * 奇数+3 偶数+2
     *
     * @param qty
     * @return
     */
    private static Integer getQuantity(Integer qty) {
        if (qty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //奇数+3
        if ((qty & BigDecimal.ONE.intValue()) != BigDecimal.ZERO.intValue()) {
            return qty + ProductionConstant.ADD_LOSS_QTY_ODD_NUMBER;
        }
        //偶数+2
        return qty + ProductionConstant.ADD_LOSS_QTY_EVEN_NUMBER;
    }

}
