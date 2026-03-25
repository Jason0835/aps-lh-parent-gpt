package com.zlt.aps.mp.engine.scheduling;

import com.google.common.collect.Lists;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.mp.engine.handler.ContinuousProductionDayHandler;
import com.zlt.aps.mp.engine.handler.SkuProductionCounter;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private Map<String, List<MonthPlanProductionRequirePlanVo>> allSkuProductionPlan;
    /**
     * 所有排产计划，以计划Id为key
     */
    private Map<Long, MonthPlanProductionRequirePlanVo> allProductionPlan;
    /**
     * sku的已排产量统计
     */
    private Map<String, Integer> skuPlannedQtyMap;
    /**
     * sku损耗量统计(因换模、换活字块导致)
     */
    private Map<String, Integer> skuWastageQtyMap;
    /**
     * 分组排产计划
     * key 结构名 value 排产计划集合
     */
    private Map<String, ProductionPlanGroupInfo> groupProductionInfo;
    /**
     * 特殊原材料信息
     * key=特殊原材料编码 ： value={key=标准长 ：value=特殊原材料库存对象实例 }
     */
    private Map<String, Map<Long, SpecialMaterialInfoVo>> specialMaterialInfoMap;
    /**
     * 反向匹配成型机台
     */
    private Set<String> reverseFindSet;
    /**
     * 超6个月库存量
     */
    private Map<String, Integer> overSixMonthStockMap;
    /**
     * 不排产记录，用于未排计划使用
     */
    private Map<Long, MonthPlanNoProductionPlan> noProductionRecordMap;
    /**
     * 排产计数器
     */
    private SkuProductionCounter productionCounter;

    /**
     * 机台续作结构Map, continueStructureMap <机台，续作结构>
     */
    private Map<String,String> continueStructureMap;

    /**
     * 模具排产限制信息记录
     * key=物料描述 ： value=限制原因集合
     */
    private Map<String, List<MouldProductionLimitTypeEnum>> skuProductionLimitInfo;
    /**
     * 特殊材料于结构关系
     * key=物料描述 ： value=结构名称列表
     */
    private Map<String, Set<String>> specialMaterialStructureRelationMap;
    /**
     * 是否达到特殊最小起排量 false表示没有达到需要预警
     */
    private Boolean reachMinSpecialMaterialStandard;
    /**
     * 临时日志存储器
     */
    private StringBuilder tempLogBuilder;
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
     * 重置Sku的已排产量及排产损耗量
     */
    public void resetSkuProductionAndWastageQty() {
        skuPlannedQtyMap = new HashMap<>(64);
        skuWastageQtyMap = new HashMap<>(64);
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
     * 根据排产计划，获取其可用的最大模具量
     * 模具分配比例(不同结构间)
     *
     * @param productionPlan 排产计划
     * @return
     */
    public Integer getMouldAllocationLimitQty(MonthPlanProductionRequirePlanVo productionPlan) {
        MouldAllocationInfoVo limitInfo = getMouldAllocationInfo(productionPlan);
        if (null == limitInfo) {
            return Integer.MAX_VALUE;
        }
        return limitInfo.getLeftOverUsedQtyByContinueSku();
    }

    /**
     * 根据排产计划，获取满足模具分配比例的排产日范围
     *
     * @param productionPlan
     * @return
     */
    public Set<Integer> getMouldAllocationRange(MonthPlanProductionRequirePlanVo productionPlan) {
        MouldAllocationInfoVo limitInfo = getMouldAllocationInfo(productionPlan);
        if (null == limitInfo) {
            return getProductionDay();
        }
        return limitInfo.getEnableDoubleMouldProductionRange();
    }

    /**
     * 根据排产计划，获取其模具分配控制对象实例信息
     * 模具分配比例(不同结构间)
     *
     * @param productionPlan 排产计划
     * @return
     */
    public MouldAllocationInfoVo getMouldAllocationInfo(MonthPlanProductionRequirePlanVo productionPlan) {
        if (null == productionPlan) {
            return null;
        }
        String controlDimensionKey = productionPlan.getMouldAllocationControlDimensionKey();
        if (StringUtils.isBlank(controlDimensionKey)) {
            return null;
        }
        Map<String, MouldAllocationInfoVo> allMouldAllocationInfoMap = baseDataContainer.getGroupMainPatternAllocationLimitMap();
        if (CollectionUtils.isEmpty(allMouldAllocationInfoMap)) {
            return null;
        }
        return allMouldAllocationInfoMap.get(controlDimensionKey);
    }

    /**
     * 清空所有模具分配比例使用量
     */
    public void clearAllMouldAllocationUsed() {
        Map<String, MouldAllocationInfoVo> allMouldAllocationLimitMap = baseDataContainer.getGroupMainPatternAllocationLimitMap();
        if (CollectionUtils.isEmpty(allMouldAllocationLimitMap)) {
            return;
        }
        allMouldAllocationLimitMap.forEach((controlDimensionKey, limit) -> limit.clearDayUsed());
    }

    /**
     * 根据排产计划，获取其可用的最大模具量
     * 胶囊卡盘
     *
     * @param productionPlan 排产计划
     * @return
     */
    public Integer getCapsuleChuckLimitQty(MonthPlanProductionRequirePlanVo productionPlan) {
        CapsuleChuckInfoVo limitInfo = getCapsuleChuckInfo(productionPlan);
        if (null == limitInfo) {
            return BigDecimal.ZERO.intValue();
        }
        return limitInfo.getLeftOverUsedQtyByContinueSku();
    }

    /**
     * 根据排产计划，获取满足胶囊卡盘限制的排产日范围
     *
     * @param productionPlan
     * @return
     */
    public Set<Integer> getCapsuleChuckRange(MonthPlanProductionRequirePlanVo productionPlan) {
        CapsuleChuckInfoVo limitInfo = getCapsuleChuckInfo(productionPlan);
        if (null == limitInfo) {
            return Collections.emptySet();
        }
        return limitInfo.getEnableDoubleMouldProductionRange();
    }

    /**
     * 根据排产计划，获取其模具分配控制对象实例信息
     * 模具分配比例(不同结构间)
     *
     * @param productionPlan 排产计划
     * @return
     */
    public CapsuleChuckInfoVo getCapsuleChuckInfo(MonthPlanProductionRequirePlanVo productionPlan) {
        if (null == productionPlan) {
            return null;
        }
        Map<String, CapsuleChuckInfoVo> allCapsuleChuckInfoMap = baseDataContainer.getCapsuleChuckInfoMap();
        if (CollectionUtils.isEmpty(allCapsuleChuckInfoMap)) {
            return null;
        }
        List<CapsuleChuckInfoVo> findCapsuleChuckInfoList = allCapsuleChuckInfoMap.values().stream().filter(singleCapsuleChuck -> singleCapsuleChuck.isMatch(productionPlan)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(findCapsuleChuckInfoList)) {
            return null;
        }
        findCapsuleChuckInfoList.sort(Comparator.comparing(CapsuleChuckInfoVo::getGroupId));
        return findCapsuleChuckInfoList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 清空所有模具分配比例使用量
     */
    public void clearAllCapsuleChuckUsed() {
        Map<String, CapsuleChuckInfoVo> allCapsuleChuckInfoMap = baseDataContainer.getCapsuleChuckInfoMap();
        if (CollectionUtils.isEmpty(allCapsuleChuckInfoMap)) {
            return;
        }
        allCapsuleChuckInfoMap.forEach((groupId, limit) -> limit.clearDayUsed());
    }

    /**
     * 获取模壳可放两副模具的日期集合
     *
     * @param selectedMould 模具
     * @return
     */
    public Set<Integer> getMouldShellRange(ProductionMouldInfoVo selectedMould) {
        MouldShellBaseInfoVo mouldShellInfo = getMouldShellInfo(selectedMould);
        if (null == mouldShellInfo) {
            return Collections.emptySet();
        }
        return mouldShellInfo.getEnableDoubleMouldProductionRange();
    }

    /**
     * 根据排产模具，获取模壳信息
     *
     * @param selectedMould 模具信息
     * @return
     */
    public MouldShellBaseInfoVo getMouldShellInfo(ProductionMouldInfoVo selectedMould) {
        if (null == selectedMould) {
            return null;
        }
        String mouldSetCode = selectedMould.getMouldSetCode();
        if (StringUtils.isBlank(mouldSetCode)) {
            return null;
        }
        Map<String, MouldShellBaseInfoVo> allMouldShellMap = baseDataContainer.getMouldShellMap();
        if (CollectionUtils.isEmpty(allMouldShellMap)) {
            return null;
        }
        return allMouldShellMap.get(mouldSetCode);
    }

    /**
     * 清空所有模壳的使用量
     */
    public void clearAllMouldShellUsed() {
        Map<String, MouldShellBaseInfoVo> allMouldShellMap = baseDataContainer.getMouldShellMap();
        if (CollectionUtils.isEmpty(allMouldShellMap)) {
            return;
        }
        allMouldShellMap.forEach((mouldSetCode, mouldShellInfo) -> mouldShellInfo.clearDayUsed());
    }

    /**
     * 增加Sku排产受限信息
     *
     * @param materialDesc 物料描述
     * @param limitType    限制类型
     */
    public void addSkuProductionLimitInfo(String materialDesc, MouldProductionLimitTypeEnum limitType) {
        if (StringUtils.isBlank(materialDesc) || null == limitType) {
            return;
        }
        if (MouldProductionLimitTypeEnum.NO_LIMIT == limitType) {
            return;
        }
        if (null == skuProductionLimitInfo) {
            skuProductionLimitInfo = new HashMap<>(64);
        }
        List<MouldProductionLimitTypeEnum> skuLimitInfo = skuProductionLimitInfo.get(materialDesc);
        if (null == skuLimitInfo) {
            skuLimitInfo = new ArrayList<>(16);
            skuProductionLimitInfo.put(materialDesc, skuLimitInfo);
        }
        skuLimitInfo.add(limitType);
    }

    /**
     * 获取有特殊原材料库存限制排产的Sku集合
     *
     * @return
     */
    public Set<String> getSpecialMaterialStockLimitSkuInfo() {
        if (CollectionUtils.isEmpty(skuProductionLimitInfo)) {
            return Collections.emptySet();
        }
        Set<String> specialMaterialStockLimitSet = new HashSet<>();
        skuProductionLimitInfo.forEach((materialDesc, skuLimitInfo) -> {
            if (CollectionUtils.isEmpty(skuLimitInfo)) {
                return;
            }
            skuLimitInfo.forEach(singleLimitInfo -> {
                if (MouldProductionLimitTypeEnum.SPECIAL_MATERIAL_STOCK_LIMIT == singleLimitInfo) {
                    specialMaterialStockLimitSet.add(materialDesc);
                }
            });
        });
        return specialMaterialStockLimitSet;
    }

    /**
     * 清空排产限制情况信息
     */
    public void clearSkuProductionLimitInfo() {
        skuProductionLimitInfo = new HashMap<>(64);
    }

    /**
     * 获取还有可排产量的日期集合
     *
     * @return
     */
    public Set<Integer> getDayCapacityLimitRange() {
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        if (null == dayCapacityLimit) {
            return Collections.emptySet();
        }
        Set<Integer> hasDayCapacitySet = dayCapacityLimit.getEnableDoubleMouldProductionRange();
        if (CollectionUtils.isEmpty(hasDayCapacitySet)) {
            return Collections.emptySet();
        }
        return hasDayCapacitySet;
    }

    /**
     * 获取贴牌还有可排产量的日期集合
     *
     * @return
     */
    public Set<Integer> getOemBrandCapacityLimitRange(TbrProductionContext productionContext,MonthPlanProductionRequirePlanVo addSkuInfo) {
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        if (null == dayCapacityLimit) {
            return Collections.emptySet();
        }
        Set<Integer> hasDayCapacitySet = dayCapacityLimit.getEnableOemBrandProductionRange(productionContext,addSkuInfo);
        if (CollectionUtils.isEmpty(hasDayCapacitySet)) {
            return Collections.emptySet();
        }
        return hasDayCapacitySet;
    }

    /**
     * 清空所有的换模使用量
     */
    public void clearAllDayLimitUsed() {
        DayCapacityLimitVo dayCapacityLimitVo = baseDataContainer.getDayCapacityLimit();
        if (null == dayCapacityLimitVo) {
            return;
        }
        dayCapacityLimitVo.resetUsedQty();
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
        if (startDay > endDay) {
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


    /**
     * 根据SKU获取模具信息列表
     *
     * @param materialDesc SKU
     * @return 模具信息列表
     */
    public List<ProductionMouldInfoVo> findMouldInfoByMaterialDesc(String materialDesc) {
        List<ProductionMouldInfoVo> mouldInfos = Lists.newArrayList();
        if (StringUtils.isBlank(materialDesc)) {
            return mouldInfos;
        }
        List<MonthPlanProductMouldInfoVo> skuRelationList = baseDataContainer.getSkuMouldRelationMap().get(materialDesc);
        if (CollectionUtils.isEmpty(skuRelationList)) {
            return mouldInfos;
        }
        skuRelationList.forEach(skuRelation -> {
            ProductionMouldInfoVo mouldInfo = baseDataContainer.getMouldInfoMap().get(skuRelation.getMouldCode());
            if (null == mouldInfo) {
                return;
            }
            mouldInfos.add(mouldInfo);
        });
        return mouldInfos.size() < ProductionConstant.DOUBLE_MOULD_PRODUCTION ? Collections.emptyList() : mouldInfos;
    }

    /**
     * 获取结构对应的特殊材料的已分配库存数
     *
     * @param productionPlanInfo 结构
     */
    public Long getSpecialMaterialSumProductionQty(ProductionPlanGroupInfo productionPlanInfo) {
        if (!productionPlanInfo.isSpecialMaterial()) {
            return 0L;
        }

        Long resultQty = null;
        for (Entry<String, BigDecimal> entry : productionPlanInfo.getEmbryoSpecialMaterialInfoMap().entrySet()) {
            Map<Long, SpecialMaterialInfoVo> stockMap = this.specialMaterialInfoMap.get(entry.getKey());
            if (stockMap == null) {
                continue;
            }
            Long sumProductionQty = stockMap.values().stream().mapToLong(SpecialMaterialInfoVo::getSumProductionQty).sum();
            if (resultQty == null) {
                resultQty = sumProductionQty;
            } else {
                resultQty = Math.min(resultQty, sumProductionQty);
            }
        }
        return resultQty;
    }

    /**
     * 更新特殊材料库存<br/>
     * 根据结构分组的分配天数变化量，更新涉及特殊材料的已排库存信息
     *
     * @param groupInfo      结构分组信息
     * @param allocationDays 分配天数，可以传负数
     */
    public void updateSpecialMaterialInfoMap(ProductionPlanGroupInfo groupInfo, Integer allocationDays) {
        // 非特殊结构，直接结束
        if (!groupInfo.isSpecialMaterial()) {
            return;
        }
        // 计算分配后剩余可分配天数的变化
        if (null == allocationDays || allocationDays == BigDecimal.ZERO.intValue()) {
            return;
        }
        // 保留一天作为换模日，其余天才满额生产
        Integer firstQty = this.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty();
        BigDecimal lhMachineCount = BigDecimalUtils.valueOf(groupInfo.getMinLhMachineCountBymould());
        Integer otherDay = allocationDays - 1;
        BigDecimal firstDayProductionQty = BigDecimalUtils.multiply(firstQty, lhMachineCount); // 首日排产量
        BigDecimal otherDayProductionQty = BigDecimalUtils.multiply(otherDay, groupInfo.getThreshold()); // 其余日排产量
        BigDecimal realProductionQty = BigDecimalUtils.add(firstDayProductionQty, otherDayProductionQty);
        this.allocationSpecialMaterialStock(groupInfo, realProductionQty, SpecialMaterialInfoVo::getSumNoRoundProductionQty,
                SpecialMaterialInfoVo::setSumNoRoundProductionQty, SpecialMaterialInfoVo::getStock);

        this.roundSpecialMaterialPlanQtyStandardLength(groupInfo);
    }

    /**
     * 更新特殊材料库存<br/>
     * 根据结构分组的分配数量量，更新涉及特殊材料的已排库存信息
     *
     * @param groupInfo     结构分组信息
     * @param productionQty 分配生产量，可以传负数
     */
    public void updateSpecialMaterialInfoSkuAllocateQty(ProductionPlanGroupInfo groupInfo, Integer productionQty) {
        // 非特殊结构，直接结束
        if (!groupInfo.isSpecialMaterial()) {
            return;
        }
        // 计算分配后剩余可分配天数的变化
        if (null == productionQty || productionQty == BigDecimal.ZERO.intValue()) {
            return;
        }
        // 天数换算成排产量 = 天数 * 日硫化量 * 配比
        BigDecimal realProductionQty = BigDecimalUtils.valueOf(productionQty);
        allocationSpecialMaterialStock(groupInfo, realProductionQty, SpecialMaterialInfoVo::getSumSkuAllocateQty,
                SpecialMaterialInfoVo::setSumSkuAllocateQty, SpecialMaterialInfoVo::getSumProductionQty);
    }

    /**
     * 清空特殊原材料的Sku排产消耗量
     * 在正式排产前，需要清除模拟时的消耗
     */
    public void clearSpecialMaterialInfoSkuAllocationQty() {
        if (CollectionUtils.isEmpty(specialMaterialInfoMap)) {
            return;
        }
        specialMaterialInfoMap.forEach((specialMaterialCode, specialMaterialStockInfo) -> {
            if (CollectionUtils.isEmpty(specialMaterialStockInfo)) {
                return;
            }
            specialMaterialStockInfo.forEach((standardId, stockInfo) -> {
                if (null == stockInfo) {
                    return;
                }
                stockInfo.setSumSkuAllocateQty(BigDecimal.ZERO.longValue());
            });
        });
    }

    /**
     * 获取结构目前对应的特殊材料库存可生产量
     *
     * @param groupInfo
     * @param productionQty
     * @return
     */
    public Integer getSpecialMaterialProductionQtyByGroupInfo(ProductionPlanGroupInfo groupInfo, Integer productionQty) {
        if (!groupInfo.isSpecialMaterial()) {
            return productionQty;
        }
        return getSpecialMaterialProductionQty(groupInfo, productionQty, SpecialMaterialInfoVo::getSumProductionQty,
                SpecialMaterialInfoVo::getStock);
    }

    /**
     * 获取SKU目前对应的特殊材料
     *
     * @param groupInfo
     * @param productionQty
     * @return
     */
    public Integer getSpecialMaterialProductionQtyBySku(ProductionPlanGroupInfo groupInfo, Integer productionQty) {
        if (!groupInfo.isSpecialMaterial()) {
            return productionQty;
        }
        return getSpecialMaterialProductionQty(groupInfo, productionQty, SpecialMaterialInfoVo::getSumSkuAllocateQty,
                SpecialMaterialInfoVo::getSumProductionQty);
    }

    /**
     * 获取特殊材料批次剩余量，用于搭配，返回最近一个批次的剩余量
     *
     * @param groupInfo     结构
     * @param productionQty 预计生产量
     * @param isNewRoll       库存不足预计生产量的情况下，是否新开一卷
     * @return
     */
    public Integer getSpecialMaterialBatchRemainQty(ProductionPlanGroupInfo groupInfo, Integer productionQty, boolean isNewRoll) {
        if (!groupInfo.isSpecialMaterial()) {
            return 0;
        }
        // 检查是否有特殊材料库存不足的情况，需要按剩余库存换算后生产条数最少的生产量为准
        Integer remainProductQty = productionQty;
        Map<String, BigDecimal> embryoSpecialMaterialInfoMap = groupInfo.getEmbryoSpecialMaterialInfoMap(); // 本结构特殊材料清单
        for (Entry<String, BigDecimal> entry : embryoSpecialMaterialInfoMap.entrySet()) {
            String materialCode = entry.getKey(); // 特殊材料物料
            BigDecimal unitConsumeQty = entry.getValue(); // 单胎消耗
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = this.specialMaterialInfoMap.get(materialCode);
            if (specialMaterialInfo == null) {
                remainProductQty = 0;
                break;
            }
            // 1.1统计剩余库存
            // 计算标准库存
            Long remainStock = specialMaterialInfo.values().stream()
                    .mapToLong(s -> BigDecimalUtils
                            .ceil(s.getSumSkuAllocateQty(), BigDecimalUtils.valueOf(s.getStandardLength())).longValue()
                            - s.getSumSkuAllocateQty())
                    .sum();
            // 1.2剩余库存换算成条数
            Integer canProductQty = BigDecimalUtils.div(remainStock, unitConsumeQty, 0).intValue();
            if (canProductQty < remainProductQty && isNewRoll) { // 如果剩余库存不足够生产出需求量，多加一卷
                Long standardLength = specialMaterialInfo.keySet().stream().min(Long::compareTo).orElse(0L);
                Long unAllocationQty = specialMaterialInfo.values().stream().mapToLong(s -> s.getStock() - s.getSumSkuAllocateQty()).sum();
                if (remainStock + standardLength <= unAllocationQty) {
                    remainProductQty = BigDecimalUtils.div(remainStock + standardLength, unitConsumeQty, 0).intValue();
                } else {
                    remainProductQty = canProductQty;
                }
            } else {
                remainProductQty = canProductQty;
            }
            if (remainProductQty <= 0) {
                break;
            }
        }
        //偶数
        remainProductQty = remainProductQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION * ProductionConstant.DOUBLE_MOULD_QTY;
        return remainProductQty;
    }

    /**
     * 对特殊材料已占用库存做标准长度取整处理
     *
     * @param groupInfo
     */
    private void roundSpecialMaterialPlanQtyStandardLength(ProductionPlanGroupInfo groupInfo) {
        groupInfo.getEmbryoSpecialMaterialInfoMap().entrySet().forEach(entry -> {
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = this.specialMaterialInfoMap.get(entry.getKey());
            if (specialMaterialInfo == null) {
                return;
            }
            specialMaterialInfo.values().stream()
                    .forEach(stockInfo -> {
                        BigDecimal sumProductionQty = BigDecimalUtils.valueOf(stockInfo.getSumNoRoundProductionQty()); // 取出未取整数量
                        BigDecimal stockQty = BigDecimalUtils.valueOf(stockInfo.getStock()); // 库存数
                        BigDecimal standardLength = BigDecimalUtils.valueOf(stockInfo.getStandardLength()); // 标准长度
                        BigDecimal finalProductionQty = sumProductionQty;
                        // 如果需求量超过实际库存量，则最多只能处理至低于库存量的最大批次数
                        // 需小于最小批次数则不需要处理
                        if (sumProductionQty.compareTo(stockQty) > 0) {
                            finalProductionQty = BigDecimalUtils.floor(stockQty, standardLength);
                        } else if (sumProductionQty.compareTo(standardLength) > 0) {
                            finalProductionQty = BigDecimalUtils.floor(sumProductionQty, standardLength);
                        }
                        stockInfo.setSumProductionQty(finalProductionQty.longValue()); // 计算结果设置到取整后的
                    });

        });
    }

    /**
     * 获取结构目前对应的特殊材料库存可生产量
     *
     * @param groupInfo
     * @param productionQty
     * @return
     */
    private Integer getSpecialMaterialProductionQty(ProductionPlanGroupInfo groupInfo, Integer productionQty,
                                                    Function<SpecialMaterialInfoVo, Long> sumQtyGetter,
                                                    Function<SpecialMaterialInfoVo, Long> stockGetter) {
        Map<String, BigDecimal> embryoSpecialMaterialInfoMap = groupInfo.getEmbryoSpecialMaterialInfoMap(); // 本结构特殊材料清单

        // 检查是否有特殊材料库存不足的情况，需要按剩余库存换算后生产条数最少的生产量为准
        Integer minProductQty = productionQty;
        for (Entry<String, BigDecimal> entry : embryoSpecialMaterialInfoMap.entrySet()) {
            String materialCode = entry.getKey(); // 特殊材料物料
            BigDecimal unitConsumeQty = entry.getValue(); // 单胎消耗
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = this.specialMaterialInfoMap.get(materialCode);
            if (specialMaterialInfo == null) {
                minProductQty = 0;
                break;
            }
            // 1.1统计剩余库存
            // 可用库存
            Long stock = specialMaterialInfo.values().stream().mapToLong(s -> stockGetter.apply(s) - sumQtyGetter.apply(s)).sum();
            // 1.2剩余库存换算成条数
            Integer canProductQty = BigDecimalUtils.div(stock, unitConsumeQty, 0).intValue();
            if (canProductQty < this.baseDataContainer.getParamConfiguration().getChangeMouldFirstQty()) {
                canProductQty = 0; // 如果剩余库存小于首日排产量，直接返回0
            }
            minProductQty = Math.min(minProductQty, canProductQty);
            if (minProductQty <= 0) {
                break;
            }
        }
        //偶数
        minProductQty = minProductQty / ProductionConstant.DOUBLE_MOULD_PRODUCTION * ProductionConstant.DOUBLE_MOULD_QTY;
        return minProductQty;
    }

    /**
     * 分配特殊材料库存
     *
     * @param groupInfo         结构
     * @param realProductionQty 分配
     * @param sumQtyGetter      分配量获取方法
     * @param sumQtySetter      分配量更新方法
     * @param stockGetter       库存量获取方法
     */
    private void allocationSpecialMaterialStock(ProductionPlanGroupInfo groupInfo, BigDecimal realProductionQty,
                                                Function<SpecialMaterialInfoVo, Long> sumQtyGetter,
                                                BiConsumer<SpecialMaterialInfoVo, Long> sumQtySetter,
                                                Function<SpecialMaterialInfoVo, Long> stockGetter) {
        Map<String, BigDecimal> embryoSpecialMaterialInfoMap = groupInfo.getEmbryoSpecialMaterialInfoMap();
        // 预估本结构的用量
        for (Entry<String, BigDecimal> entry : embryoSpecialMaterialInfoMap.entrySet()) {
            // 特殊材料物料
            String materialCode = entry.getKey();
            // 单胎消耗量
            BigDecimal unitConsumeQty = entry.getValue();
            // 总消耗量
            Long materialConsumeQty = BigDecimalUtils.multiply(unitConsumeQty, realProductionQty, true).longValue();
            // 取出各标准用量的特殊材料库存
            Map<Long, SpecialMaterialInfoVo> specialMaterialInfo = this.specialMaterialInfoMap.get(materialCode);
            if (CollectionUtils.isEmpty(specialMaterialInfo)) {
                continue;
            }
            // 取出库存还有剩余的库存信息
            List<SpecialMaterialInfoVo> stockList = specialMaterialInfo.values().stream()
                    .sorted((s1, s2) -> {
                        // 第一顺位：从已排量最大的开始分配
                        Long sumProductionQty1 = sumQtyGetter.apply(s1);
                        Long sumProductionQty2 = sumQtyGetter.apply(s2);
                        int result = sumProductionQty2.compareTo(sumProductionQty1);
                        if (result != BigDecimal.ZERO.intValue()) {
                            return result;
                        }
                        // 第二顺位：从剩余库存大于本结构用量且最接近的开始分配：abs(库存-已分配-需求量)
                        Boolean isEnoughStock1 = stockGetter.apply(s1) - sumProductionQty1 > materialConsumeQty;
                        Boolean isEnoughStock2 = stockGetter.apply(s2) - sumProductionQty2 > materialConsumeQty;
                        // 任意一个可用库存小于结构用量，都结束，且优先使用大于结构用量的
                        if (!isEnoughStock1 || !isEnoughStock2) {
                            return isEnoughStock2.compareTo(isEnoughStock1);
                        }
                        Long remainQty1 = stockGetter.apply(s1) - sumProductionQty1 - materialConsumeQty;
                        Long remainQty2 = stockGetter.apply(s2) - sumProductionQty2 - materialConsumeQty;
                        result = remainQty1.compareTo(remainQty2);
                        return result;
                    }).collect(Collectors.toList());
            Long unAllocationQty = materialConsumeQty;
            for (SpecialMaterialInfoVo stockInfo : stockList) { // 顺序分配至每一个定长的库存上，库存不够扣的切换到其他定长
                Long oldQty = sumQtyGetter.apply(stockInfo);
                Long newQty = 0L;
                Long tempQty = oldQty + unAllocationQty; // 旧值直接加上分配量
                if (unAllocationQty > 0) { // 扣减库存，则结果（分配量）不能超过库存
                    newQty = Math.min(tempQty, stockInfo.getStock());
                } else { // 回退，则结果（分配量）不能低于0
                    newQty = Math.max(tempQty, BigDecimal.ZERO.longValue());
                }
                unAllocationQty -= (newQty - oldQty);
                sumQtySetter.accept(stockInfo, newQty);
                if (unAllocationQty == 0) {
                    break;
                }
            }
        }
    }

    /**
     * 根据结构的特殊材料列表刷新上下文的特殊材料结构关系表
     *
     * @param groupInfo 物料描述
     */
    public void updateSpecialMaterialStructureRelationMap(ProductionPlanGroupInfo groupInfo) {
        if (!groupInfo.isSpecialMaterial()) {
            return;
        }
        String strucureName = groupInfo.getGroupName();
        Set<String> specialMaterialCodeSet = groupInfo.getEmbryoSpecialMaterialInfoMap().keySet();
        for (String specialMaterialCode : specialMaterialCodeSet) {
            Set<String> strucureSet = this.specialMaterialStructureRelationMap.get(specialMaterialCode);
            if (strucureSet == null) {
                strucureSet = new HashSet<>();
                this.specialMaterialStructureRelationMap.put(specialMaterialCode, strucureSet);
            }
            strucureSet.add(strucureName);
        }
    }
}
