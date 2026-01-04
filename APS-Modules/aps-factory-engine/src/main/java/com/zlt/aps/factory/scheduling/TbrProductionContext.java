package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.domain.vo.SpecialMaterialInfoVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 全钢排产上下文
 *
 * @author ZLT
 * @date 20251210
 */
@Data
public class TbrProductionContext extends Context {
    /**
     * 基础数据容器对象
     */
    private BaseDataContainer baseDataContainer;
    /**
     * 排产计划信息-按Sku分组
     */
    Map<String, List<MonthPlanProductionRequirePlanVo>> allSkuProductionPlan;
    /**
     * 所有排产计划，以计划Id为key
     */
    Map<Long, MonthPlanProductionRequirePlanVo> allProductionPlan;
    /**
     * sku的已排产量统计
     */
    Map<String, Integer> skuPlannedQtyMap;
    /**
     * sku损耗量统计(因换模、换活字块导致)
     */
    Map<String, Integer> skuWastageQtyMap;
    /**
     * 分组排产计划
     * key 结构名 value 排产计划集合
     */
    Map<String, ProductionPlanGroupInfo> groupProductionInfo;

    /**
     * 特殊原材料信息
     * key=特殊原材料编码 ： value={key=标准长 ：value=特殊原材料库存对象实例 }
     */
    Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap;
    /**
     * 反向匹配成型机台
     */
    Set<String> reverseFindSet;

    /**
     * 超6个月库存量
     */
    Map<String, Integer> overSixMonthStockMap;

    /**
     * 加入收尾，方向匹配结构集合
     *
     * @param cxMachineCode
     */
    public void addReverseMachine(String cxMachineCode) {
        if (StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        if (null == reverseFindSet) {
            reverseFindSet = new HashSet<>();
        }
        reverseFindSet.add(cxMachineCode);
    }

    /**
     * 增加sku已排产量及排产损耗量
     *
     * @param materialDesc 物料描述
     * @param plannedQty   增加的排产量
     * @param wastageQty   增加的损耗量
     */
    public void addSkuProductionAndWastageQty(String materialDesc, Integer plannedQty, Integer wastageQty) {
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        addQtyHandler(materialDesc, plannedQty, skuPlannedQtyMap);
        addQtyHandler(materialDesc, wastageQty, skuWastageQtyMap);
        //重新计算库销比
        resetCalculateInventorySalesRatio(materialDesc);
    }

    /**
     * 获取materialDesc在startDay~endDay范围内可排产的两副模具
     * 在多幅的情形下，共用性差的优先，否则编号大的优先
     *
     * @param materialDesc 物料描述
     * @param startDay     排产开始日
     * @param endDay       排产结束日
     * @return
     */
    public List<ProductionMouldInfoVo> selectedDoubleMouldByRange(String materialDesc, Integer startDay, Integer endDay) {
        if (StringUtils.isBlank(materialDesc) || null == startDay || null == endDay || startDay > endDay) {
            return Collections.emptyList();
        }
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptyList();
        }
        List<ProductionMouldInfoVo> effectiveList = getEffectiveByRange(skuRelationList, startDay, endDay);
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        if (effectiveList.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptyList();
        }
        effectiveList.sort(Comparator.comparing(ProductionMouldInfoVo::getCommonalityValue).thenComparing(ProductionMouldInfoVo::getMouldCode, Comparator.reverseOrder()));
        return effectiveList.subList(BigDecimal.ZERO.intValue(), ProductionConstant.DOUBLE_MOULD_PRODUCTION);
    }

    /**
     * 根据sku集合，获取在startDay~endDay还有模具产能(符合数量的模具)的sku集合
     *
     * @param mouldNumber     满足模具数量
     * @param materialDescSet sku集合
     * @param startDay        排产开始日
     * @param endDay          排产结束日
     * @return
     */
    public Set<String> getHasMouldCapacity(Integer mouldNumber, Set<String> materialDescSet, Integer startDay, Integer endDay) {
        if (null == mouldNumber || CollectionUtils.isEmpty(materialDescSet)) {
            return Collections.emptySet();
        }
        if (null == startDay || null == endDay) {
            return Collections.emptySet();
        }
        if (startDay >= endDay) {
            return Collections.emptySet();
        }
        Set<String> enableSet = new HashSet<>();
        materialDescSet.forEach(materialDesc -> {
            List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
            if (CollectionUtils.isEmpty(skuRelationList)) {
                return;
            }
            List<ProductionMouldInfoVo> mouldRelationList = getEffectiveByRange(skuRelationList, startDay, endDay);
            if (mouldRelationList.size() >= mouldNumber) {
                enableSet.add(materialDesc);
            }
        });
        return enableSet;
    }

    /**
     * 根据materialDesc判断在startDay~endDay，得到共用模具受限的其它Sku信息
     *
     * @param materialDesc 物料描述
     * @param startDay     排产开始日
     * @param endDay       排产结束日
     * @return
     */
    public Set<String> getLimitShareMouldOtherSku(String materialDesc, Integer startDay, Integer endDay) {
        if (StringUtils.isBlank(materialDesc) || null == startDay || null == endDay || startDay > endDay) {
            return Collections.emptySet();
        }
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return Collections.emptySet();
        }
        List<ProductionMouldInfoVo> mouldRelationList = getEffectiveByRange(skuRelationList, startDay, endDay);
        if (mouldRelationList.size() > ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return Collections.emptySet();
        }
        //最后两副模具，则判断是否有共享模具物料
        Set<String> shareSkuSet = new HashSet<>();
        mouldRelationList.forEach(mouldInfo -> shareSkuSet.addAll(mouldInfo.getAssociationMaterialSet()));
        Set<String> limitSkuSet = new HashSet<>();
        shareSkuSet.forEach(shareMaterialDesc -> {
            if (materialDesc.equals(shareMaterialDesc)) {
                return;
            }
            List<MonthPlanProductMouldInfoVo> shareOtherRelationList = baseDataContainer.getSkuMouldRelationMap().get(shareMaterialDesc);
            if (CollectionUtils.isEmpty(shareOtherRelationList)) {
                return;
            }
            List<ProductionMouldInfoVo> shareMouldRelationList = getEffectiveByRange(shareOtherRelationList, startDay, endDay);
            if (shareMouldRelationList.size() == ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
                limitSkuSet.add(shareMaterialDesc);
            }
        });
        return limitSkuSet;
    }

    /**
     * 增加已排产量处理
     *
     * @param materialDesc  物料描述
     * @param addQty        增加的量
     * @param handlerQtyMap 处理的集合
     */
    private void addQtyHandler(String materialDesc, Integer addQty, Map<String, Integer> handlerQtyMap) {
        //已排产量处理
        if (null == addQty || addQty <= BigDecimal.ZERO.longValue()) {
            addQty = BigDecimal.ZERO.intValue();
        }
        Integer sumQty = handlerQtyMap.get(materialDesc);
        if (null == sumQty) {
            sumQty = BigDecimal.ZERO.intValue();
        }
        sumQty = sumQty + addQty;
        handlerQtyMap.put(materialDesc, sumQty);
    }

    /**
     * 重新计算库销比
     *
     * @param materialDesc 物料描述
     */
    private void resetCalculateInventorySalesRatio(String materialDesc) {
        if (StringUtils.isBlank(materialDesc)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> skuPlanList = allSkuProductionPlan.get(materialDesc);
        if (CollectionUtils.isEmpty(skuPlanList)) {
            return;
        }
        Integer sumPlannedQty = skuPlannedQtyMap.get(materialDesc);
        if (null == sumPlannedQty) {
            sumPlannedQty = BigDecimal.ZERO.intValue();
        }
        Integer plannedQty = sumPlannedQty;
        skuPlanList.forEach(plan -> plan.calculateInventorySalesRatio(plannedQty));
    }

    /**
     * 根据模具关系，获取在startDay~endDay有效排产的模具信息
     *
     * @param skuRelationList 配置的模具关系
     * @param startDay        排产开始日
     * @param endDay          排产结束日
     * @return
     */
    private List<ProductionMouldInfoVo> getEffectiveByRange(List<MonthPlanProductMouldInfoVo> skuRelationList, Integer startDay, Integer endDay) {
        List<ProductionMouldInfoVo> effectiveList = new ArrayList<>();
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = baseDataContainer.getMouldInfoMap().get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            if (!mouldInfo.isProduction(startDay, endDay)) {
                return;
            }
            effectiveList.add(mouldInfo);
        });
        return effectiveList;
    }
}
