package com.zlt.aps.factory.daylimit;

import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.EarliestConclusionLhGroupHelper;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日产能控制对象
 * 排产日、最大产能上限、最低产能，产能比例
 *
 * @author ZLT
 * @date 20250106
 */
@Getter
public class DayCapacityLimitVo implements Serializable {
    /**
     * 日产能控制信息
     * key=排产日 : value=日排产控制信息
     */
    private Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap;

    /**
     * 构建日产能控制对象
     *
     * @param dayCapacityLimitMap
     */
    public DayCapacityLimitVo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 更新日排产信息
     *
     * @param dayCapacityLimitMap
     */
    public void updateWholeDayLimitInfo(Map<Integer, DayCapacityLimitHelper> dayCapacityLimitMap) {
        this.dayCapacityLimitMap = dayCapacityLimitMap;
    }

    /**
     * 确认切换结构的上机排产日
     *
     * @param context               排产上下文
     * @param theoryChangeDay       理论的切换结构日
     * @param changeGroupName       切换后的结构
     * @param selectedCxMachineInfo 选中的机台
     * @param hasProductionDaySet   可排产日集合
     * @return
     */
    public Integer confirmStartDayByChangeGroup(Context context, Integer theoryChangeDay, String changeGroupName, CxMachineBaseInfoVo selectedCxMachineInfo, Set<Integer> hasProductionDaySet) {
        //没有切换结构
        if (!selectedCxMachineInfo.isChangeGroup(theoryChangeDay, changeGroupName)) {
            return theoryChangeDay;
        }
        Set<Integer> hasChangeGroupSet = getHasChangeGroupProductionDay(context);
        if (CollectionUtils.isEmpty(hasChangeGroupSet)) {
            return null;
        }
        if (hasChangeGroupSet.contains(theoryChangeDay)) {
            return theoryChangeDay;
        }
        //提取在theoryChangeDay后，首个最小的日期
        Set<Integer> afterTheoryChangeDayList = hasChangeGroupSet.stream().filter(singleDay -> singleDay >= theoryChangeDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(afterTheoryChangeDayList)) {
            return null;
        }
        Set<Integer> resultSet = hasChangeGroupSet.stream().filter(afterTheoryChangeDayList::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(resultSet)) {
            return null;
        }
        List<Integer> resultList = new ArrayList<>(resultSet);
        resultList.sort(Comparator.comparing(Integer::intValue));
        return resultList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 重新设置使用量信息
     * 包含日结构切换次数
     * 日换模次数
     * 日排产量
     */
    public void resetUsedQty() {
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        dayCapacityLimitMap.forEach((productionDay, dayLimit) -> dayLimit.resetUsedQty());
    }

    /**
     * 获取还有剩余产能可分配的排产日集合
     * 分配使用量 < 日产能上限的排产日集合
     *
     * @return
     */
    public Set<Integer> getHasCapacityProductionDayInfo() {
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return Collections.emptySet();
        }
        List<DayCapacityLimitHelper> hasAllocationCapacityList = dayCapacityLimitMap.values().stream().filter(singleDay -> singleDay.isAllocationCapacity()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAllocationCapacityList)) {
            return Collections.emptySet();
        }
        return hasAllocationCapacityList.stream().map(DayCapacityLimitHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 增加分组产能分配使用量信息
     *
     * @param context           排产上下文
     * @param allocationDay     分配日
     * @param addAllocationPlan 分配信息
     */
    public void addCxMachineGroupNameAllocationUsedQty(Context context, Integer allocationDay, CxMachineAllocationPlanHelper addAllocationPlan) {
        if (!isEffectiveParam(allocationDay, addAllocationPlan)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(allocationDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addCxMachineAllocationQty(context, allocationDay, addAllocationPlan);
    }

    /**
     * 减少分组产能分配使用量信息
     * 在结构提前收尾时，需将占用量释放
     *
     * @param context        排产上下文
     * @param allocationDay  分配日
     * @param allocationInfo 分配信息
     */
    public void deductionCxMachineGroupNameAllocationUsedQty(Context context, Integer allocationDay, CxMachineAllocationPlanHelper allocationInfo) {
        if (!isEffectiveParam(allocationDay, allocationInfo)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(allocationDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionCxMachineAllocationQty(context, allocationDay, allocationInfo);
    }


    /**
     * 获取可安排两副模具排产的排产日集合
     * 当天剩余可排产量 > 0
     *
     * @return
     */
    public Set<Integer> getEnableDoubleMouldProductionRange() {
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return new HashSet<>();
        }
        List<DayCapacityLimitHelper> dayLimitList = dayCapacityLimitMap.values().stream().collect(Collectors.toList());
        List<DayCapacityLimitHelper> enableList = dayLimitList.stream().filter(singleDay -> singleDay.isAddSkuProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(enableList)) {
            return new HashSet<>();
        }
        return enableList.stream().map(DayCapacityLimitHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 增加分组产能分配使用量信息
     *
     * @param context        排产上下文
     * @param productionDay  排产日
     * @param productionPlan 排产计划
     * @param doubleMould    排产模具
     * @param productionQty  排产量
     * @param lossQty        损耗量
     */
    public void addSkuDayProductionQty(Context context, Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, List<ProductionMouldInfoVo> doubleMould, Integer productionQty, Integer lossQty) {
        if (!isEffectiveParam(productionDay, productionPlan, doubleMould, productionQty, lossQty)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addSkuDayProductionQty(context, productionDay, productionPlan, doubleMould, productionQty, lossQty);
    }

    /**
     * 释放分组产能分配使用量信息
     *
     * @param context       排产上下文
     * @param productionDay 排产日
     * @param materialDesc  排产计划
     * @param usedMouldSet  排产模具
     * @param productionQty 排产量
     * @param lossQty       损耗量
     */
    public void deductionSkuDayProductionQty(Context context, Integer productionDay, String materialDesc, Set<String> usedMouldSet, Integer productionQty, Integer lossQty) {
        if (null == productionDay || StringUtils.isBlank(materialDesc) || CollectionUtils.isEmpty(usedMouldSet)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionSkuDayProductionQty(context, productionDay, materialDesc, usedMouldSet, productionQty, lossQty);
    }

    /**
     * 获取能切换分组的排产日集合
     *
     * @param context 排产上下文
     * @return
     */
    public Set<Integer> getHasChangeGroupProductionDay(Context context) {
        //没有限制时，所有的可排产日
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return context.getProductionDay();
        }
        List<DayCapacityLimitHelper> hasChangeGroupList = dayCapacityLimitMap.values().stream().filter(singleDay -> singleDay.getLeftOverUsedChangeGroupQty() >= BigDecimal.ONE.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasChangeGroupList)) {
            return Collections.emptySet();
        }
        return hasChangeGroupList.stream().map(DayCapacityLimitHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 获取还有换模能力的排产日集合
     *
     * @param context 排产上下文
     * @return
     */
    public Set<Integer> getHasChangeMouldProductionDay(Context context) {
        //没有限制时，所有的可排产日
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return context.getProductionDay();
        }
        List<DayCapacityLimitHelper> hasChangeMouldList = dayCapacityLimitMap.values().stream().filter(singleDay -> singleDay.getLeftOverUsedChangeMouldQty() >= BigDecimal.ONE.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasChangeMouldList)) {
            return Collections.emptySet();
        }
        return hasChangeMouldList.stream().map(DayCapacityLimitHelper::getProductionDay).collect(Collectors.toSet());
    }

    /**
     * 增加每日结构切换使用次数
     *
     * @param context              排产上下文
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组名
     */
    public void addChangeGroupNameUsedQty(Context context, Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (!isEffectiveParam(changeProductionDate, cxMachineCode, groupName)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeProductionDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addChangeGroupUsedQty(context, cxMachineCode, groupName);
    }

    /**
     * 减少每日结构切换使用次数
     *
     * @param context              排产上下文
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组名
     */
    public void deductionChangeGroupNameUsedQty(Context context, Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (!isEffectiveParam(changeProductionDate, cxMachineCode, groupName)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeProductionDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionChangeGroupUsedQty(context, cxMachineCode, groupName);
    }

    /**
     * 换模次数控制处理，调整其上机日
     *
     * @param context         排产上下文
     * @param lhGroup         收尾硫化组，含有上机日、收尾日
     * @param doubleMouldList 使用的模具
     */
    public void confirmStartDayByChangeMouldLimit(Context context, EarliestConclusionLhGroupHelper lhGroup, List<ProductionMouldInfoVo> doubleMouldList) {
        if (null == lhGroup || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        Integer startDay = lhGroup.getClosingDay();
        Integer endDay = lhGroup.getEndDay();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(context);
        //达到换模次数限制
        if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        //可进行换模
        if (hasChangeMouldDaySet.contains(startDay)) {
            return;
        }
        //开始时间需要推迟 提取在startDay后，首个最小的日期
        Set<Integer> afterTheoryChangeDayList = hasChangeMouldDaySet.stream().filter(singleDay -> singleDay >= startDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(afterTheoryChangeDayList)) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        List<Integer> resultList = new ArrayList<>(afterTheoryChangeDayList);
        resultList.sort(Comparator.comparing(Integer::intValue));
        Integer realChangeDay = resultList.get(BigDecimal.ZERO.intValue());
        if (realChangeDay > endDay) {
            lhGroup.updateProductionDateRange(null, null);
            return;
        }
        lhGroup.updateProductionDateRange(realChangeDay, endDay);
        return;
    }

    /**
     * 换模次数控制处理，调整其上机日
     *
     * @param context         排产上下文
     * @param newLhGroup      收尾硫化组，含有上机日、收尾日
     * @param doubleMouldList 使用的模具
     */
    public void confirmStartDayByChangeMouldLimit(Context context, CxLhProductionHelper newLhGroup, List<ProductionMouldInfoVo> doubleMouldList) {
        if (null == newLhGroup || CollectionUtils.isEmpty(doubleMouldList)) {
            return;
        }
        Integer startDay = newLhGroup.getProductionDay();
        Integer endDay = newLhGroup.getEndDay();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        DayCapacityLimitVo dayCapacityLimit = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<Integer> hasChangeMouldDaySet = dayCapacityLimit.getHasChangeMouldProductionDay(context);
        //达到换模次数限制
        if (CollectionUtils.isEmpty(hasChangeMouldDaySet)) {
            newLhGroup.updateProductionDateRange(null, null);
            return;
        }
        //可进行换模
        if (hasChangeMouldDaySet.contains(startDay)) {
            return;
        }
        //开始时间需要推迟 提取在startDay后，首个最小的日期
        Set<Integer> afterTheoryChangeDayList = hasChangeMouldDaySet.stream().filter(singleDay -> singleDay >= startDay).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(afterTheoryChangeDayList)) {
            newLhGroup.updateProductionDateRange(null, null);
            return;
        }
        List<Integer> resultList = new ArrayList<>(afterTheoryChangeDayList);
        resultList.sort(Comparator.comparing(Integer::intValue));
        Integer realChangeDay = resultList.get(BigDecimal.ZERO.intValue());
        if (realChangeDay > endDay) {
            newLhGroup.updateProductionDateRange(null, null);
            return;
        }
        newLhGroup.updateProductionDateRange(realChangeDay, endDay);
        return;
    }

    /**
     * 增加每日结构切换使用次数
     *
     * @param context         排产上下文
     * @param changeMouldDate 换模日
     * @param materialDesc    换模Sku
     * @param mouldCodeSet    模具信息
     */
    public void addChangeMouldUsedQty(Context context, Integer changeMouldDate, String materialDesc, Set<String> mouldCodeSet) {
        if (!isEffectiveParam(changeMouldDate, materialDesc, mouldCodeSet)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeMouldDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.addChangeMouldUsedQty(context, materialDesc, mouldCodeSet);
    }

    /**
     * 增加每日结构切换使用次数
     *
     * @param context         排产上下文
     * @param changeMouldDate 换模日
     * @param materialDesc    换模Sku
     * @param mouldCode       模具信息
     */
    public void deductionChangeMouldUsedQty(Context context, Integer changeMouldDate, String materialDesc, String mouldCode) {
        if (null == changeMouldDate || StringUtils.isBlank(materialDesc) || StringUtils.isBlank(mouldCode)) {
            return;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return;
        }
        DayCapacityLimitHelper dayLimit = dayCapacityLimitMap.get(changeMouldDate);
        if (null == dayLimit) {
            return;
        }
        dayLimit.deductionChangeMouldUsedQty(context, materialDesc, mouldCode);
    }

    /**
     * 是否有效参数
     * <p>
     * true 有效 false 无效
     *
     * @param productionDay  排产日
     * @param productionPlan 排产计划(不关注具体的计划ID)
     * @param doubleMould    排产模具
     * @param productionQty  排产量
     * @param lossQty        损耗量
     * @return
     */
    private boolean isEffectiveParam(Integer productionDay, MonthPlanProductionRequirePlanVo productionPlan, List<ProductionMouldInfoVo> doubleMould, Integer productionQty, Integer lossQty) {
        if (null == productionDay || null == productionPlan || CollectionUtils.isEmpty(doubleMould)) {
            return false;
        }
        Set<String> mouldSet = doubleMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        if (StringUtils.isBlank(productionPlan.getMaterialDesc()) || CollectionUtils.isEmpty(mouldSet) || mouldSet.size() != ProductionConstant.DOUBLE_MOULD_PRODUCTION) {
            return false;
        }
        Integer realProductionQty = BigDecimal.ZERO.intValue();
        if (null != productionQty && productionQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + productionQty;
        }
        if (null != lossQty && lossQty > BigDecimal.ZERO.intValue()) {
            realProductionQty = realProductionQty + lossQty;
        }
        return realProductionQty > BigDecimal.ZERO.intValue();
    }

    /**
     * 是否有效参数
     * <p>
     * true 有效 false 无效
     *
     * @param allocationDay     分配日
     * @param addAllocationPlan 分配信息
     * @return
     */
    private boolean isEffectiveParam(Integer allocationDay, CxMachineAllocationPlanHelper addAllocationPlan) {
        if (null == allocationDay || null == addAllocationPlan) {
            return false;
        }
        if (CollectionUtils.isEmpty(dayCapacityLimitMap)) {
            return false;
        }
        return true;
    }

    /**
     * 是否有效参数
     * <p>
     * true 有效 false 无效
     *
     * @param changeProductionDate 切换日
     * @param cxMachineCode        成型机台
     * @param groupName            分组计划名(TBR结构名)
     * @return
     */
    private boolean isEffectiveParam(Integer changeProductionDate, String cxMachineCode, String groupName) {
        if (null == changeProductionDate) {
            return false;
        }
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)) {
            return false;
        }
        return true;
    }

    /**
     * 是否有效参数
     * true 有效 false 无效
     *
     * @param changeMouldDate 换模日
     * @param materialDesc    换模物料
     * @param mouldCodeSet    换模模具
     * @return
     */
    private boolean isEffectiveParam(Integer changeMouldDate, String materialDesc, Set<String> mouldCodeSet) {
        if (null == changeMouldDate) {
            return false;
        }
        if (StringUtils.isBlank(materialDesc)) {
            return false;
        }
        return !CollectionUtils.isEmpty(mouldCodeSet);
    }
}
