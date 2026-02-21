package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 结构原特殊材料业务处理
 *
 * @author ZLT
 * @date 20260206
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpecialMaterialScheduleHandler {

    /**
     * 根据结构特殊材料情况重算特殊结构需要排产的天数
     *
     * @param allocationHelper   分配信息
     * @param productionContext  排产上下文
     * @param productionPlanInfo 分组计划
     * @return
     */
    public Integer calculateConfirmAllocationDaysBySpecialMaterial(CxMachineAllocationPlanHelper allocationHelper, TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo) {
        if (null == allocationHelper || null == productionPlanInfo) {
            return null;
        }
        //理论分配天数
        Integer needAllocationDays = allocationHelper.getAllocationDay();
        boolean isContinue = hasFindOtherGroupBySpecialMaterial(productionContext, productionPlanInfo, allocationHelper);
        if( !isContinue){
            return needAllocationDays;
        }
        //取出与本结构使用相同特殊材料的其他结构信息(过滤使用相同特殊材料的结构)
        List<ProductionPlanGroupInfo> otherNeedProductionSpecialPlanList = getSameSpecialMaterialOtherGroupList(productionContext, productionPlanInfo);
        //其他特殊规格有任意一个没有排完，说明还不是最后一个结构，跳过
        if (!isLastSpecialMaterialGroup(otherNeedProductionSpecialPlanList)) {
            return needAllocationDays;
        }
        //打上最后一个分组的标记
        productionPlanInfo.setIsLatestSpecialMaterial(true);
        //本结构涉及的特殊材料清单
        Map<String, BigDecimal> materialMap = productionPlanInfo.getEmbryoSpecialMaterialInfoMap();
        //特殊材料库存列表
        Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap = productionContext.getSpecialMaterialInfoMap();
        //特殊材料库存的可生产上限
        Integer limitProductionQty = calculateLimitProductionQtyByStock(materialMap, specialMaterialInfoMap);
        //可生产上限不足，则不能排产
        if (limitProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        /**
         * 判断同特殊材料排排产量落在哪个区间：
         * 1、计划量*单号模除标准长度，如果余数小于日硫化量 * 配比*单耗：舍弃余数部分
         * 2、计划量*单号模除标准长度，如果余数大于等于日硫化量 * 配比*单耗：计划量补标准长度 - 日硫化量 * 配比*单耗
         * 统计已排量 = 日硫化量 * 配比 * 已排天数
         */
//        Integer otherAllocationQty = otherNeedProductionSpecialPlanList.stream().mapToInt(ProductionPlanGroupInfo::getSumPlanQty).sum();
        Long otherAllocationQty = productionContext.getSpecialMaterialSumProductionQty(productionPlanInfo);
        // 同特殊材料结构总预计排产量 productionPlanInfo.getSumPlanQty()
//        Integer sumPlanQty = otherAllocationQty + productionPlanInfo.getSumPlanQty();
        // 取出各结构的特殊材料清单交集
        Map<String, BigDecimal> specialIntersectionMap = new HashMap<>();
        specialIntersectionMap.putAll(materialMap);
        for (ProductionPlanGroupInfo plan : otherNeedProductionSpecialPlanList) {
            plan.getEmbryoSpecialMaterialInfoMap().keySet().stream().forEach(materialCode -> {
                if (!specialIntersectionMap.containsKey(materialCode)) {
                    specialIntersectionMap.remove(materialCode);
                }
            });
        }
        // 都没有交集，直接重置为本结构的物料清单
        if (CollectionUtils.isEmpty(specialIntersectionMap)) {
            specialIntersectionMap.putAll(materialMap);
        }
        Map.Entry<String, BigDecimal> entry = specialIntersectionMap.entrySet().stream().findFirst().get();
        // 单耗
        BigDecimal unitConsumeLength = entry.getValue();
        // 标准长度
        BigDecimal standardLength = specialMaterialInfoMap.get(entry.getKey()).keySet().stream().findFirst().map(BigDecimalUtils::valueOf).get();
        Integer standardQty = standardLength.divide(unitConsumeLength, BigDecimal.ZERO.intValue(), RoundingMode.DOWN).intValue();
        // 需求量
        BigDecimal sumPlanQty = BigDecimalUtils.multiply(productionPlanInfo.getSumPlanQty(), unitConsumeLength);
        sumPlanQty = BigDecimalUtils.add(sumPlanQty, otherAllocationQty); // 加上已排量
        //计算余数
        BigDecimal remainderLength = sumPlanQty.remainder(standardLength); // 长度
        BigDecimal remainderQty = BigDecimalUtils.div(remainderLength, standardLength, 0); // 条数
        // 区间阈值 = 硫化量 * 配比*单耗
        Integer floatThreshold = productionPlanInfo.getThreshold();
        // 区间阈值 = 硫化量 * 配比*单耗
        BigDecimal thresholdLength = BigDecimalUtils.multiply(floatThreshold, unitConsumeLength);
        boolean isAddQty = false;
        Integer productionQty = Math.min(limitProductionQty, productionPlanInfo.getSumPlanQty()); // 取生产上限与需求量的较小值
//        Integer productionQty = productionPlanInfo.getRemainingMaxProductionQty();
        // 重算实际的量
        Integer realProductionQty = BigDecimal.ZERO.intValue();
        // 超过阈值，尝试补量
        if (remainderLength.compareTo(thresholdLength) >= BigDecimal.ZERO.intValue()) {
            if (limitProductionQty >= productionQty + standardQty - floatThreshold) {
                //检查补量后不超过可生产上限才进行补量
                isAddQty = true;
                realProductionQty = productionQty + standardQty - floatThreshold;
            }
        }
        // 不补量，则需要将计划量扣减掉余数部分
        if (!isAddQty) {
            realProductionQty = productionQty - remainderQty.intValue();
        }
        // 可生产上限不足，则不能排产
        if (realProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //计算新的排产天数 = ceil(计划量 / 日硫化量 / 配比)
        BigDecimal theoryDays = BigDecimalUtils.div(realProductionQty, floatThreshold, 2, false);
        Integer resultTheoryDays = theoryDays.setScale(0, RoundingMode.UP).intValue();
//        // 算天数
//        Integer mouldCount = productionPlanInfo.getGroupPlanData().stream().map(p -> Optional
//                .ofNullable(productionContext.getBaseDataContainer().getSkuMouldRelationMap().get(p.getMaterialDesc()))
//                .map(s -> s.size()).orElse(0)).max(Integer::compareTo).orElse(0);
//        Integer firstQty = productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty();
//        productionPlanInfo.getSumPlanQty()
        
        // 算其他特殊材料有交集的结构已排天数，不到最小排产天数的给加到最小排产天数
        Integer otherTheoryDays = otherNeedProductionSpecialPlanList.stream().mapToInt(g -> g.getTheoryDays()).sum();
        Integer minAllocationDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
        if (resultTheoryDays + otherTheoryDays < minAllocationDays) {
            resultTheoryDays = minAllocationDays;
        }
        if (isAddQty) {
            //拉量时，不能进行提前收尾处理
            productionPlanInfo.setHasBeforeConclusionHandler(false);
        }
        return resultTheoryDays;
    }


    /**
     * 是否需要执行查找其它还需排产的特殊材料分组计划
     * true 不需要执行 false 需要执行
     *
     * @param productionContext  排产上下文
     * @param productionPlanInfo 当前排产分组
     * @param allocationHelper   分配情况
     * @return
     */
    private boolean hasFindOtherGroupBySpecialMaterial(TbrProductionContext productionContext, ProductionPlanGroupInfo productionPlanInfo, CxMachineAllocationPlanHelper allocationHelper) {
        //非特殊结构直接跳过
        if (!productionPlanInfo.isSpecialMaterial()) {
            return false;
        }
        //如果有特殊材料分组(结构)已经排产到月底，则不需要拉量或是舍弃
        if (hasLastDayProductionSpecialMaterial(productionContext)) {
            return false;
        }
        //如果自己是本月排产最后一天，直接跳过，不需要拉量或者舍弃
        Integer endDay = allocationHelper.getEndDay();
        if (productionContext.isProductionEndDay(endDay)) {
            return false;
        }
        return true;
    }

    /**
     * 获取与当前分组计划相同原材料的其他还有排产的分组计划
     * TBR 为结构
     *
     * @param productionContext      排产上下文
     * @param currentProductionGroup 当前排产分组
     */
    private List<ProductionPlanGroupInfo> getSameSpecialMaterialOtherGroupList(TbrProductionContext productionContext, ProductionPlanGroupInfo currentProductionGroup) {
        //当前排产分组的特殊材料清单
        Map<String, BigDecimal> materialMap = currentProductionGroup.getEmbryoSpecialMaterialInfoMap();
        //所有分组计划
        List<ProductionPlanGroupInfo> allGroupPlanList = productionContext.getGroupProductionInfo().values().stream().collect(Collectors.toList());
        // 取出与本结构使用相同特殊材料的其他结构信息(过滤使用相同特殊材料的结构)
        List<ProductionPlanGroupInfo> specialPlanList = allGroupPlanList.stream().filter(plan -> {
            //检查本结构之外的特殊结构
            if (plan == currentProductionGroup) {
                return false;
            }
            if (null == plan.getMinLhDayCapacityQty() || null == plan.getMinLhMachineCount()) {
                return false;
            }
//            if (plan.getRemainingNeedAllocationDays() <= BigDecimal.ZERO.intValue()) {
//                return false;
//            }
            //涉及的特殊材料清单
            Map<String, BigDecimal> otherMaterialMap = plan.getEmbryoSpecialMaterialInfoMap();
            if (CollectionUtils.isEmpty(otherMaterialMap)) {
                return false;
            }
            //特殊材料与新增结构的特殊材料清单有交集
            return materialMap.keySet().stream().anyMatch(material -> otherMaterialMap.containsKey(material));
        }).collect(Collectors.toList());
        return specialPlanList;
    }

    /**
     * 判断是否为最后一个特殊材料排产
     * 1、没有其它特殊材料的结构 = true
     * 2、其它特殊材料的结构没有还需排产的量(剩余需分配的量) = true
     *
     * @param otherSpecialMaterialGroupList 其它特殊材料的结构
     * @return
     */
    private boolean isLastSpecialMaterialGroup(List<ProductionPlanGroupInfo> otherSpecialMaterialGroupList) {
        if (CollectionUtils.isEmpty(otherSpecialMaterialGroupList)) {
            return true;
        }
        return !otherSpecialMaterialGroupList.stream().anyMatch(plan -> plan.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue());
    }

    /**
     * 计算特殊材料库存的最大可排产量
     *
     * @param materialMap            特殊材料用量清单
     * @param specialMaterialInfoMap 特殊材料库存
     * @return
     */
    private Integer calculateLimitProductionQtyByStock(Map<String, BigDecimal> materialMap, Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap) {
        //根据各材料的库存使用情况限制排产量
        Integer limitProductionQty = null;
        // 预估本结构的用量
        for (Map.Entry<String, BigDecimal> entry : materialMap.entrySet()) {
            //特殊材料物料
            String materialCode = entry.getKey();
            //单胎消耗量
            BigDecimal unitConsumeQty = entry.getValue();
            //取出各标准用量的特殊材料库存
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = specialMaterialInfoMap.get(materialCode);
            if (null == specialMaterialInfo) {
                limitProductionQty = BigDecimal.ZERO.intValue();
                break;
            }
            // 累计可用库存
            Long totalStock = specialMaterialInfo.values().stream().mapToLong(s -> s.getStock() - s.getSumProductionQty()).sum();
            if (totalStock <= BigDecimal.ZERO.intValue()) {
                limitProductionQty = BigDecimal.ZERO.intValue();
                break;
            }
            // 换算成成品数
            Integer stockCanProductionQty = BigDecimalUtils.div(totalStock, unitConsumeQty, 2)
                    .setScale(0, RoundingMode.DOWN).intValue();
            // 如果未分配量还有剩余，需要更新可排产量
            if (null == limitProductionQty) {
                limitProductionQty = stockCanProductionQty;
            } else {
                limitProductionQty = Math.min(limitProductionQty, stockCanProductionQty);
            }
        }
        return limitProductionQty;
    }

    /**
     * 判断已排产的特殊材料分组(结构)是否有月底最后一天
     * 只要已排产的特殊材料分组(结构)中有一个在月底最后一天排产，则表示排产到月底
     * 看分配的天数是否到月底
     *
     * @param productionContext 排产上下文
     * @return
     */
    private boolean hasLastDayProductionSpecialMaterial(TbrProductionContext productionContext) {
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return false;
        }
        return allCxMachineInfo.values().stream().anyMatch(singleCxMachine -> {
            //判断只要有一台成型机分配的特殊材料在月底，就表示排产到月底
            List<CxMachineAllocationPlanHelper> allocationList = singleCxMachine.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return false;
            }
            return allocationList.stream().anyMatch(singleAllocation -> {
                //判断只要有一段排产的特殊材料分组(结构)在月底，则表示月底
                ProductionPlanGroupInfo productionPlanInfo = singleAllocation.getProductionPlanInfo();
                if (null == productionPlanInfo || !productionPlanInfo.isSpecialMaterial()) {
                    return false;
                }
                if (singleAllocation.getAllocationDay() <= BigDecimal.ZERO.intValue()) {
                    return false;
                }
                return productionContext.isProductionEndDay(singleAllocation.getEndDay());
            });
        });
    }

}
