package com.zlt.aps.mp.engine.domain.vo;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.CxMachineFixedPriorityEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.handler.ContinuousProductionDayHandler;
import com.zlt.aps.mp.engine.logrecorder.DayLimitLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 成型基础信息对象
 *
 * @author ZLT
 * @date 20251215
 */
@Data
@Slf4j
public class CxMachineBaseInfoVo implements Serializable {

    /**
     * 工厂编号
     */
    private String factoryCode;

    /**
     * 成型机台
     */
    private String cxMachineCode;

    /**
     * 成型机型-品牌
     */
    private String cxMachineBrandCode;

    /**
     * 类型 机械等
     */
    private String cxMachineTypeCode;
    /**
     * 是否零度供料架
     */
    private String isZeroRack;

    /**
     * 硫化上限
     */
    private Integer lhMachineMaxQty;

    /**
     * 固定结构1
     */
    private String fixedStructure1;

    /**
     * 固定结构2
     */
    private String fixedStructure2;

    /**
     * 固定结构3
     */
    private String fixedStructure3;

    /**
     * 固定SKU
     */
    private String fixedMaterialCode;

    /**
     * 不可作业结构
     */
    private String disableStructure;

    /**
     * 不可作业SKU
     */
    private String disableMaterialCode;

    /**
     * 停产日(包含维修及全局停产日)信息
     */
    private Set<Integer> stopDayInfo;
    /**
     * 最大可排产天数(已经剔除了停产日)
     */
    private Integer maxProductionDays;
    /**
     * 理论的排产天数集合
     */
    private Set<Integer> theoryProductionDaySet;
    /**
     * 非续作结构使用-当前硫化配比
     */
    private Integer ratio;
    /**
     * 挑选时使用，可排产天数：与成型鼓取得交集后的天数
     */
    private Integer selectedProductionDys;
    /**
     * 挑选时使用，可排产天数与需求天数的差值
     */
    private Integer capacityDiffValue;
    /**
     * 挑选时使用，排产日集合
     */
    private Set<Integer> selectedProductionDaySet;
    /**
     * 针对计划的固定优先级
     */
    private Integer fixedPriority;
    /**
     * 分配的分组计划集合(TBR按结构)
     */
    private List<CxMachineAllocationPlanHelper> allocationList;
    /**
     * 已分配日集合
     */
    private Set<Integer> allocationDaySet;
    /**
     * 计划是否同规格
     */
    private String sameSpecifications;
    /**
     * 计划是否同英寸
     */
    private String sameProSize;
    /**
     * 计划是否断面宽范围
     */
    private String sectionWidthCondition;
    /**
     * 成型硫化配比最后一天排产分组信息
     */
    private Map<Integer, CxLhProductionHelper> cxLhRatioMap;
    /**
     * 日排产限制--只在第一轮按机台分配中使用-机台反选和计划挑选机台
     */
    private Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo;
    /**
     * 近1个月的上机日期-计划挑机台时使用
     */
    private Integer lastBoardingDate;
    /**
     * 近3个月的排产次数-计划挑机台时使用
     */
    private Integer productionCount;
    /**
     * 最后可分配天数--最后补量分配阶段使用
     */
    private Integer lastCanProductionDays;

    /**
     * 获取剩余产能，以剩余天数*此时的硫化配比
     * 用于判断后续剩余产能判断
     *
     * @return
     */
    public Integer getRemainCapacity() {
        Integer currentRemainDays = getRemainingDays();
        if (null == currentRemainDays) {
            currentRemainDays = BigDecimal.ZERO.intValue();
        }
        Integer currentRatio = ratio;
        if (null == currentRatio) {
            currentRatio = BigDecimal.ZERO.intValue();
        }
        return currentRemainDays * currentRatio;
    }

    /**
     * 剩余分配日
     * 最大排产日-已分配日
     *
     * @return
     */
    public Integer getRemainingDays() {
        if (null == maxProductionDays || maxProductionDays <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(allocationDaySet)) {
            return maxProductionDays;
        }
        return maxProductionDays - allocationDaySet.size();
    }

    /**
     * 根据选中的selectedGroup，确认可排产日范围
     * 从最后一个分配日开始
     *
     * @param context       排产上下文
     * @param selectedGroup 选中分组计划
     * @return
     */
    public Set<Integer> confirmProductionRange(Context context, ProductionPlanGroupInfo selectedGroup) {
        if (null == context || null == selectedGroup) {
            return Collections.emptySet();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        /**
         * 日控制限制：
         * 1、日成型工装数量限制
         * 2、日产能上限限制
         * 获取成型工装的排产日集合
         */
        GroupCapacityProductionLimitHelper limitResult = productionContext.getBaseDataContainer().getLeftOverProductionDayInfo(context, selectedGroup, this);
        Set<Integer> productionDayInfo = limitResult.getProductionDaySet();
        if (CollectionUtils.isEmpty(productionDayInfo)) {
            return Collections.emptySet();
        }
        String daysInfo = productionDayInfo.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        DayLimitLogRecorder.addCanProductionDayInfoLog(productionContext, this, selectedGroup.getGroupName(), selectedGroup.getProSizeInfo(), daysInfo);
        //最后下一个分配日
        Integer startDay = getNextStartDay();
        //成型机本身的排产日集合
        Set<Integer> localProductionInfo = getHasLeftOverProductionDayInfoByStartDay(startDay);
        if (CollectionUtils.isEmpty(localProductionInfo)) {
            return Collections.emptySet();
        }
        String cxMachineDaysInfo = localProductionInfo.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        DayLimitLogRecorder.addCxMachineCanProductionDayLog(productionContext, cxMachineCode, cxMachineDaysInfo);
        //取得交集
        Set<Integer> intersectionSet = localProductionInfo.stream().filter(productionDayInfo::contains).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(intersectionSet)) {
            return Collections.emptySet();
        }
        daysInfo = intersectionSet.stream().map(String::valueOf).collect(Collectors.joining(StringConstant.COMMA));
        DayLimitLogRecorder.addCanProductionDayInfoLog(productionContext, this, selectedGroup.getGroupName(), selectedGroup.getProSizeInfo(), daysInfo);
        //取得最早的一段连续时间
        Set<Integer> earliestContinuousSet = ContinuousProductionDayHandler.getEarliestContinuousRangeResultExcludeStop(intersectionSet, stopDayInfo);
        if (CollectionUtils.isEmpty(earliestContinuousSet)) {
            return Collections.emptySet();
        }
        return earliestContinuousSet;
    }

    /**
     * 获取确认的排产日范围，与workProductionInfo取交集
     *
     * @param context            排产上下文
     * @param workProductionInfo 可排产日集合(含成型工装、日产能上限控制)
     * @return
     */
    public Set<Integer> confirmProductionRange(Context context, Set<Integer> workProductionInfo) {
        if (null == context || CollectionUtils.isEmpty(workProductionInfo)) {
            return Collections.emptySet();
        }
        //成型机本身的排产日
        Set<Integer> localProductionInfo = getHasLeftOverProductionDayInfo();
        if (CollectionUtils.isEmpty(localProductionInfo)) {
            return Collections.emptySet();
        }
        //取得交集
        Set<Integer> intersectionSet = localProductionInfo.stream().filter(workProductionInfo::contains).collect(Collectors.toSet());
        //取得最早的一段连续时间
        Set<Integer> earliestContinuousSet = ContinuousProductionDayHandler.getEarliestContinuousRangeResultExcludeStop(intersectionSet, stopDayInfo);
        if (CollectionUtils.isEmpty(earliestContinuousSet)) {
            return Collections.emptySet();
        }
        return earliestContinuousSet;
    }

    /**
     * 增加日排产信息
     * 在模拟排产阶段，且机台选结构，结构反选机台场景使用
     * 20260304 补充Sku排产信息及排产模具
     */
    public void addDayProductionInfo(Context context, SkuDayProductionInfoHelper skuDayProductionInfo) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == skuDayProductionInfo) {
            return;
        }
        Integer productionDay = skuDayProductionInfo.getProductionDay();
        GroupPlanCxLhCapacityLimitHelper dayLimit = dayProductionLimitInfo.get(productionDay);
        if (null == dayLimit) {
            return;
        }
        String materialDesc = skuDayProductionInfo.getMaterialDesc();
        String embryoCode = skuDayProductionInfo.getEmbryoCode();
        Set<String> currentUsedMouldSet = skuDayProductionInfo.getUsedMouldSet();
        //更新生胎及模具
        if (checkCanAddEmbryoCode(context, skuDayProductionInfo)) {
            //增加日排产量<日换模数量时，不计算胎胚种类数 sandy+ 2026.3.24
            dayLimit.getProductionEmbryoCodeSet().add(embryoCode);
        }
        dayLimit.getProductionMouldSet().addAll(currentUsedMouldSet);
        //Sku排产模具信息
        Set<String> skuProductionMouldSet = dayLimit.getSkuProductionMouldMap().get(materialDesc);
        if (null == skuProductionMouldSet) {
            skuProductionMouldSet = new HashSet<>();
            dayLimit.getSkuProductionMouldMap().put(materialDesc, currentUsedMouldSet);
        }
        skuProductionMouldSet.addAll(currentUsedMouldSet);
        //更新排产Sku信息
        SkuDayProductionInfoHelper planned = dayLimit.getProductionSkuQtyInfo().get(materialDesc);
        if (null == planned) {
            dayLimit.getProductionSkuQtyInfo().put(materialDesc, skuDayProductionInfo);
            return;
        }
        //更新数量
        planned.addProductionDayQty(skuDayProductionInfo.getSumProductionQty(), skuDayProductionInfo.getLossQty());
    }

    /**
     * 检查能否加胎胚种类数
     *
     * @param context
     * @param skuDayProductionInfo
     * @return
     */
    private boolean checkCanAddEmbryoCode(Context context, SkuDayProductionInfoHelper skuDayProductionInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer changeMouldFirstQty = productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty();
        return skuDayProductionInfo.getSumProductionQty() >= changeMouldFirstQty;
    }

    /**
     * 获取固定信息的优先级
     * 固定SKU的优先级最高，其次是固定结构1,
     * 再次固定结构2，最后固定结构3
     *
     * @return
     */
    public Integer getFixedPriorityValue(ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return CxMachineFixedPriorityEnum.DEFAULT.getPriorityValue();
        }
        //无固定配置
        if (!hasFixed()) {
            return CxMachineFixedPriorityEnum.DEFAULT.getPriorityValue();
        }
        String structureName = groupPlanInfo.getGroupName();
        Integer fixedPriorityValue = getFixedStructurePriority(fixedStructure1, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_FIRST, structureName).getPriorityValue();
        Integer fixedPriorityValue2 = getFixedStructurePriority(fixedStructure2, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_SECOND, structureName).getPriorityValue();
        fixedPriorityValue = Math.min(fixedPriorityValue, fixedPriorityValue2);
        Integer fixedPriorityValue3 = getFixedStructurePriority(fixedStructure3, CxMachineFixedPriorityEnum.FIXED_STRUCTURE_THIRD, structureName).getPriorityValue();
        fixedPriorityValue = Math.min(fixedPriorityValue, fixedPriorityValue3);
        Integer fixedPrioritySku = getFixedMaterialCodePriority(groupPlanInfo).getPriorityValue();
        return Math.min(fixedPriorityValue, fixedPrioritySku);
    }

    /**
     * 判定机台是否为结构指定机台
     * 需要判断 指定结构和指定Sku
     * 先判断指定结构，后判断指定Sku
     *
     * @param groupPlanInfo 结构信息
     * @return
     */
    public boolean hasFixedMachine(ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return false;
        }
        //无固定配置
        if (!hasFixed()) {
            return false;
        }
        //判定结构
        Set<String> fixedStructureSet = new HashSet<>();
        if (StringUtils.isNotBlank(fixedStructure1)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure1.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (StringUtils.isNotBlank(fixedStructure2)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure2.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (StringUtils.isNotBlank(fixedStructure3)) {
            fixedStructureSet.addAll(Stream.of(fixedStructure3.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (fixedStructureSet.contains(groupPlanInfo.getGroupName())) {
            return true;
        }
        //判定Sku
        Set<String> fixedMaterialCodeSet = new HashSet<>();
        if (StringUtils.isNotBlank(fixedMaterialCode)) {
            fixedMaterialCodeSet.addAll(Stream.of(fixedMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet()));
        }
        if (CollectionUtils.isEmpty(fixedMaterialCodeSet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return false;
        }
        for (MonthPlanProductionRequirePlanVo singlePlan : groupPlanData) {
            if (fixedMaterialCodeSet.contains(singlePlan.getMaterialCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断机台排产的最后一个结构
     * 是否为特殊结构
     *
     * @return
     */
    public boolean lastProductionSpecialStructure() {
        CxMachineAllocationPlanHelper lastAllocationGroup = getLastAllocationInfo();
        if (null == lastAllocationGroup) {
            return false;
        }
        return lastAllocationGroup.getProductionPlanInfo().isSpecialMaterial();
    }

    /**
     * 将机台最后个分配的分组计划直接拉到月底最后
     *
     * @param context 排产上下文
     */
    public void addLastAllocationToFull(Context context) {
        if (CollectionUtils.isEmpty(allocationList)) {
            return;
        }
        if (CollectionUtils.isEmpty(theoryProductionDaySet)) {
            return;
        }
        CxMachineAllocationPlanHelper lastAllocationInfo = getLastAllocationInfo();
        if (null == lastAllocationInfo) {
            return;
        }
        List<Integer> theoryProductionDayList = new ArrayList<>(theoryProductionDaySet);
        theoryProductionDayList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        Integer maxDay = theoryProductionDayList.get(BigDecimal.ZERO.intValue());
        Integer endDay = lastAllocationInfo.getEndDay();
        Integer startDay = endDay + BigDecimal.ONE.intValue();
        Integer addDays = BigDecimal.ZERO.intValue();
        for (; startDay <= maxDay; startDay++) {
            addDays = addDays + BigDecimal.ONE.intValue();
        }
        Integer newEndDay = context.getMonthDays();
        lastAllocationInfo.addAllocationDayToFull(newEndDay, addDays);
    }

    /**
     * 获取成型机台当前排产结束日
     * 取得最后一个排产结构所处分配的结束日
     *
     * @return
     */
    public Integer getCurrentProductionEndDay() {
        if (CollectionUtils.isEmpty(allocationList)) {
            return BigDecimal.ZERO.intValue();
        }
        CxMachineAllocationPlanHelper lastGroup = getLastAllocationInfo();
        if (null == lastGroup) {
            return Integer.MAX_VALUE;
        }
        Integer endDay = lastGroup.getEndDay();
        if (null == endDay) {
            return Integer.MAX_VALUE;
        }
        return endDay;
    }

    /**
     * 设置最后可分配天数
     *
     * @param context
     * @return
     */
    public void setLeftOverDays(Context context) {
        lastCanProductionDays = getLeftOverDaysByLastAllocation(context);
    }

    /**
     * 获取成型机台在最后一个结构后可排产天数
     *
     * @return
     */
    public Integer getLeftOverDaysByLastAllocation(Context context) {
        CxMachineAllocationPlanHelper lastGroup = getLastAllocationInfo();
        if (null == lastGroup) {
            return BigDecimal.ZERO.intValue();
        }
        Integer startDay = lastGroup.getEndDay();
        Set<Integer> stopDayInfo = getStopDayInfo();
        if (null == stopDayInfo) {
            stopDayInfo = new HashSet<>();
        }
        //分配的天数
        int index = BigDecimal.ZERO.intValue();
        int monthDays = context.getProductionEndDay();
        Integer day = startDay + BigDecimal.ONE.intValue();
        for (; day <= monthDays; day++) {
            //停产日
            if (stopDayInfo.contains(day)) {
                day = day + BigDecimal.ONE.intValue();
                continue;
            }
            //超出月份周期
            if (day > monthDays) {
                break;
            }
            index = index + BigDecimal.ONE.intValue();
        }
        return index;
    }

    /**
     * 判断是否为不可作业结构
     * true 表示是不可作业结构
     * false 表示不是不可作业结构
     *
     * @param structureName 结构名
     * @return
     */
    public boolean isNoProductionStructure(String structureName) {
        if (StringUtils.isBlank(structureName)) {
            return false;
        }
        if (StringUtils.isBlank(disableStructure)) {
            return false;
        }
        Set<String> disableStructureSet = Stream.of(disableStructure.split(StringConstant.COMMA)).collect(Collectors.toSet());
        return disableStructureSet.contains(structureName);
    }

    /**
     * 判断是否为不可作业SKU
     * true 表示是不可作业SKU
     * false 表示不是不可作业SKU
     *
     * @param materialCode 物料编码
     * @return
     */
    public boolean isNoProductionMaterial(String materialCode) {
        if (StringUtils.isBlank(materialCode)) {
            return false;
        }
        if (StringUtils.isBlank(disableMaterialCode)) {
            return false;
        }
        Set<String> disableMaterialSet = Stream.of(disableMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet());
        return disableMaterialSet.contains(materialCode);
    }

    /**
     * 新增分配的分组计划信息
     * 同时，将成型工装的日使用量 + 1
     * 因都是3鼓机台，故而每种工装类型的日使用量都 + 1
     *
     * @param addAllocationPlan 分配信息对象
     */
    public void addAllocationPlanInfo(Context context, CxMachineAllocationPlanHelper addAllocationPlan) {
        if (null == addAllocationPlan) {
            return;
        }
        if (null == allocationList) {
            allocationList = new ArrayList<>();
        }
        ProductionPlanGroupInfo productionPlanInfo = addAllocationPlan.getProductionPlanInfo();
        CxMachineAllocationPlanHelper lastAllocation = getLastAllocationInfo();
        allocationList.add(addAllocationPlan);
        allocationList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getStartDay));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        handlerLimitInfo(productionContext, addAllocationPlan, productionPlanInfo);
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Integer startDay = addAllocationPlan.getStartDay();
        //20260121 切换结构
        if (isChangeGroup(context, lastAllocation, addAllocationPlan)) {
            String groupName = addAllocationPlan.getAllocationGroup();
            log.info(TbrProductionGroupLogRecorder.addCxMachineChangeGroupLog(productionContext, cxMachineCode, startDay, groupName));
            baseDataContainer.getDayCapacityLimit().addChangeGroupNameUsedQty(productionContext, startDay, cxMachineCode, groupName);
        }
    }

    /**
     * 对最后一个分配结构进行延长分配日期
     * 同时，将成型工装的日使用量 + 1
     * 因都是3鼓机台，故而每种工装类型的日使用量都 + 1
     *
     * @param addAllocationPlan 分配信息对象
     */
    public void updateLastAllocationPlanInfo(Context context, CxMachineAllocationPlanHelper addAllocationPlan) {
        if (null == addAllocationPlan) {
            return;
        }
        if (CollectionUtils.isEmpty(allocationList)) {
            return;
        }
        CxMachineAllocationPlanHelper lastAllocationInfo = getLastAllocationInfo();
        if (null == lastAllocationInfo) {
            return;
        }
        Integer newEndDay = addAllocationPlan.getEndDay();
        Integer addAllocationDays = addAllocationPlan.getAllocationDay();
        handlerLimitInfo(context, addAllocationPlan, lastAllocationInfo.getProductionPlanInfo());
        lastAllocationInfo.addAllocationDayToFull(newEndDay, addAllocationDays);
    }

    /**
     * 判断是否切换分组(TBR-结构)
     *
     * @param context         排产上下文
     * @param changeDay       切换日
     * @param changeGroupName 切换分组名
     * @return
     */
    public boolean isChangeGroup(Context context, Integer changeDay, String changeGroupName) {
        CxMachineAllocationPlanHelper lastAllocation = getLastAllocationInfo();
        return isChangeGroup(context, lastAllocation, changeDay, changeGroupName);
    }

    /**
     * 处理结构提前收尾
     * 需要处理切换结构使用量 -1
     * 需要将成型工装数量还原即使用数量 - 1
     *
     * @param context         排产上下文
     * @param allocationInfo  结构收尾的分配段信息
     * @param deductionDaySet 收尾的日期
     * @param groupPlanInfo   收尾的结构信息
     */
    public void handlerBeforeConclusion(Context context, CxMachineAllocationPlanHelper allocationInfo, Set<Integer> deductionDaySet, ProductionPlanGroupInfo groupPlanInfo) {
        //20260123 分配日清除
        if (!CollectionUtils.isEmpty(deductionDaySet)) {
            allocationDaySet.removeAll(deductionDaySet);
        }
        //切换结构
        handlerBeforeConclusionByAllocation(context, allocationInfo);
        String proSize = groupPlanInfo.getProSizeInfo();
        if (StringUtils.isBlank(proSize) || CollectionUtils.isEmpty(deductionDaySet)) {
            return;
        }
        //释放 成型工装使用占用量、日分配产能占用量
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        Integer sumDeductionDay = BigDecimal.ZERO.intValue();
        for (Integer beforeConclusionDay : deductionDaySet) {
            if (stopDayInfo.contains(beforeConclusionDay)) {
                return;
            }
            allocationDaySet.remove(beforeConclusionDay);
            sumDeductionDay = sumDeductionDay + BigDecimal.ONE.intValue();
            //20260120 释放成型工装使用量占用
            baseDataContainer.releaseUsedCount(beforeConclusionDay, proSize, cxMachineCode);
            //20260125 释放成型产能分配量占用
            if (null != dayCapacityLimit) {
                dayCapacityLimit.deductionCxMachineGroupNameAllocationUsedQty(context, beforeConclusionDay, allocationInfo);
            }
        }
        //todo 20260211 特殊材料库存分配量(释放)
        productionContext.updateSpecialMaterialInfoMap(groupPlanInfo, -sumDeductionDay);
    }

    /**
     * 获取成型机台当前最后一个分配信息的排产日集合，构建其对应的排产日集合信息
     *
     * @return
     */
    public Set<Integer> getLastProductionDayInfo() {
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptySet();
        }
        CxMachineAllocationPlanHelper lastInfo = getLastAllocationInfo();
        if (null == lastInfo) {
            return Collections.emptySet();
        }
        Integer startDay = lastInfo.getStartDay();
        Integer endDay = lastInfo.getEndDay();
        Set<Integer> productionSet = new HashSet<>();
        for (Integer day = startDay; day <= endDay; day++) {
            if (stopDayInfo.contains(day)) {
                continue;
            }
            productionSet.add(day);
        }
        return productionSet;
    }

    /**
     * 获取成型机台下，最早收尾的硫化机台组
     *
     * @return
     */
    public CxLhProductionHelper getEarliestConclusionLhGroup(Set<Integer> excludeDays) {
        //获取成型硫化组
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            return null;
        }
        List<CxLhProductionHelper> cxLhGroupList = new ArrayList<>(cxLhRatioMap.values());
        List<CxLhProductionHelper> hasProductionList = cxLhGroupList.stream().filter(singleGroup -> !YesOrNoEnum.NO.getValue().equals(singleGroup.getIsProduction())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return null;
        }
        if (!CollectionUtils.isEmpty(excludeDays)) {
            //20260113 剔除需要排除的收尾时间点
            hasProductionList = hasProductionList.stream().filter(singleGroup -> !excludeDays.contains(singleGroup.getProductionDay())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return null;
        }
        //按最后排产日，进行升序排序
        hasProductionList.sort(Comparator.comparing(CxLhProductionHelper::getProductionDay).thenComparing(CxLhProductionHelper::getLhGroupNo));
        //取得第一条：即最早收尾的硫化组
        CxLhProductionHelper earliestConclusionLhMachine = hasProductionList.get(BigDecimal.ZERO.intValue());
        return earliestConclusionLhMachine;
    }

    /**
     * 根据选择的Sku判断其符合胎胚种类数限制及其上机时间点和排产结束日
     *
     * @param context         排产上下文
     * @param addSkuInfo      需要上机的Sku
     * @param selectedLhGroup 预计选中
     * @param endDay          收尾日
     * @param selectedMould   选择的模具
     * @return
     */
    public CxLhProductionHelper getCorrectProductionDateRange(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, CxLhProductionHelper selectedLhGroup, Integer endDay, List<ProductionMouldInfoVo> selectedMould) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == selectedLhGroup) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        Integer preClosingDay = selectedLhGroup.getProductionDay();
        Integer preEndDay = endDay;
        String mouldSetCode = selectedMould.get(BigDecimal.ZERO.intValue()).getMouldSetCode();
        ChangeMouldInfo changeMouldInfo = ChangeMouldInfo.buildChangeMouldInfo(context, addSkuInfo, selectedLhGroup.getBeforeSku(), selectedLhGroup.getBeforeSku());
        boolean isChangeMould = changeMouldInfo.isChangeMould();
        //隔天换模
        if (isChangeMould && changeMouldInfo.isProductionNextDay()) {
            preClosingDay = context.getNextHasProductionDay(preClosingDay, stopDayInfo);
        }
        MouldProductionDayLimitHelper limitHelper = LhGroupProductionRangeCalculator.confirmProductionRange(productionContext, addSkuInfo, preClosingDay, preEndDay, selectedMould, dayLimitList, stopDayInfo, isChangeMould);
        Set<Integer> effectiveRangeSet = limitHelper.getProductionDaySet();
        if (CollectionUtils.isEmpty(effectiveRangeSet)) {
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuLimitLog(context, addSkuInfo.getStructureName(), cxMachineCode, addSkuInfo, mouldSetCode, limitHelper.getLimitType()));
            return null;
        }
        //拷贝，否则数据丢失
        CxLhProductionHelper newLhGroup = new CxLhProductionHelper();
        BeanUtils.copyProperties(selectedLhGroup, newLhGroup);
        List<Integer> sortList = new ArrayList<>(effectiveRangeSet);
        Collections.sort(sortList);
        int size = sortList.size();
        Integer newStartDay = sortList.get(BigDecimal.ZERO.intValue());
        newLhGroup.updateProductionDateRange(newStartDay, sortList.get(size - BigDecimal.ONE.intValue()));
        //20260122 换模次数判断
        if (!isChangeMould) {
            return newLhGroup;
        }
        //需要换模-换模次数处理
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Set<String> mouldCodeSet = selectedMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, newStartDay, addSkuInfo.getMaterialDesc(), mouldCodeSet);
        return newLhGroup;
    }

    /**
     * 设置成型机与当前加入的分组排产计划是否同规格、同花纹、同英寸、断面宽等信息
     *
     * @param addGroupPlan 即将要加入的分组计划
     * @param diffValue    断面宽差值范围
     */
    public void setSameInfoByCurrentGroupPlan(ProductionPlanGroupInfo addGroupPlan, Integer diffValue) {
        //没有排产信息，默认匹配
        if (CollectionUtils.isEmpty(allocationList)) {
            sameSpecifications = YesOrNoEnum.YES.getCode();
            sameProSize = YesOrNoEnum.YES.getCode();
            sectionWidthCondition = YesOrNoEnum.YES.getCode();
            return;
        }
        //取得最后一个分配的分组结构计划
        CxMachineAllocationPlanHelper lastHelper = getLastAllocationInfo();
        if (null == lastHelper) {
            sameSpecifications = YesOrNoEnum.YES.getCode();
            sameProSize = YesOrNoEnum.YES.getCode();
            sectionWidthCondition = YesOrNoEnum.YES.getCode();
            return;
        }
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getRealProductionPlanList();
        String sameSpecifications = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSameSpecifications(realProductionPlanList)) {
            sameSpecifications = YesOrNoEnum.YES.getCode();
        }
        this.sameSpecifications = sameSpecifications;
        String sameProSize = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSameProSize(realProductionPlanList)) {
            sameProSize = YesOrNoEnum.YES.getCode();
        }
        this.sameProSize = sameProSize;
        String sectionWidthCondition = YesOrNoEnum.NO.getCode();
        if (addGroupPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)) {
            sectionWidthCondition = YesOrNoEnum.YES.getCode();
        }
        this.sectionWidthCondition = sectionWidthCondition;
    }

    /**
     * 获取成型机台当前可分配的起始日
     *
     * @return
     */
    public Integer getLastAllocationStartDay() {
        if (CollectionUtils.isEmpty(allocationList)) {
            return ProductionConstant.MONTH_START_DAY;
        }
        CxMachineAllocationPlanHelper lastHelper = getLastAllocationInfo();
        if (null == lastHelper) {
            return ProductionConstant.MONTH_START_DAY;
        }
        return lastHelper.getEndDay() + BigDecimal.ONE.intValue();
    }

    /**
     * 获取下一个排产起始日，在当前最大的排产日基础上 + 1
     *
     * @return
     */
    public Integer getNextStartDay() {
        if (CollectionUtils.isEmpty(theoryProductionDaySet)) {
            return null;
        }
        if (CollectionUtils.isEmpty(allocationDaySet)) {
            return ProductionConstant.MONTH_START_DAY;
        }
        if (allocationDaySet.size() == theoryProductionDaySet.size()) {
            return null;
        }
        List<Integer> sortList = allocationDaySet.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        Integer maxDay = sortList.get(BigDecimal.ZERO.intValue());
        return maxDay + BigDecimal.ONE.intValue();
    }

    /**
     * 获取最后一个配置
     *
     * @return
     */
    public CxMachineAllocationPlanHelper getLastAllocationInfo() {
        if (CollectionUtils.isEmpty(allocationList)) {
            return null;
        }
        int size = allocationList.size();
        return allocationList.get(size - BigDecimal.ONE.intValue());
    }

    /**
     * 获取成型机台在startDay~周期结束日可排产的集合信息
     * 需要剔除自身的停产日
     *
     * @param context  排产上下文
     * @param startDay 开始日
     * @return
     */
    private Set<Integer> getHasProductionDayInfo(Context context, Integer startDay) {
        if (null == startDay || null == context) {
            return Collections.emptySet();
        }
        Integer endDay = context.getMonthDays();
        if (startDay > endDay) {
            return Collections.emptySet();
        }
        Set<Integer> productionDaySet = new HashSet<>(64);
        for (Integer productionDay = startDay; productionDay <= endDay; productionDay++) {
            //停产日及已排产日剔除
            if (stopDayInfo.contains(productionDay) || allocationDaySet.contains(productionDay)) {
                continue;
            }
            productionDaySet.add(productionDay);
        }
        return productionDaySet;
    }

    /**
     * 获取成型机台所有可排产日集合信息
     * 剔除停产日和已分配日
     * 停产日包含：
     * 1、机台本身的维修停产
     * 2、工作日历中的停工日
     *
     * @return
     */
    private Set<Integer> getHasLeftOverProductionDayInfo() {
        if (CollectionUtils.isEmpty(theoryProductionDaySet)) {
            return Collections.emptySet();
        }
        Set<Integer> productionDaySet = new HashSet<>(64);
        theoryProductionDaySet.forEach(productionDay -> {
            if (stopDayInfo.contains(productionDay) || allocationDaySet.contains(productionDay)) {
                return;
            }
            productionDaySet.add(productionDay);
        });
        return productionDaySet;
    }

    /**
     * 获取成型机台所有可排产日集合信息
     * 剔除停产日和已分配日
     * 停产日包含：
     * 1、机台本身的维修停产
     * 2、工作日历中的停工日
     *
     * @param startDay 开始排产日
     * @return
     */
    private Set<Integer> getHasLeftOverProductionDayInfoByStartDay(Integer startDay) {
        if (null == startDay) {
            return Collections.emptySet();
        }
        if (CollectionUtils.isEmpty(theoryProductionDaySet)) {
            return Collections.emptySet();
        }
        Set<Integer> productionDaySet = new HashSet<>(64);
        theoryProductionDaySet.forEach(productionDay -> {
            if (productionDay < startDay) {
                return;
            }
            if (stopDayInfo.contains(productionDay) || allocationDaySet.contains(productionDay)) {
                return;
            }
            productionDaySet.add(productionDay);
        });
        return productionDaySet;
    }

    /**
     * 根据固定结构值及优先级，得到其真实优先级
     *
     * @param fixedStructure 固定结构
     * @param fixedPriority  固定优先级
     * @param structureName  结构名
     * @return
     */
    private CxMachineFixedPriorityEnum getFixedStructurePriority(String fixedStructure, CxMachineFixedPriorityEnum fixedPriority, String structureName) {
        if (StringUtils.isBlank(fixedStructure) || StringUtils.isBlank(structureName)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        Set<String> fixedStructureSet = Stream.of(fixedStructure.split(StringConstant.COMMA)).collect(Collectors.toSet());
        if (fixedStructureSet.contains(structureName)) {
            return fixedPriority;
        }
        return CxMachineFixedPriorityEnum.DEFAULT;
    }

    /**
     * 获取固定SKU的优先级值
     *
     * @param groupPlanInfo
     * @return
     */
    private CxMachineFixedPriorityEnum getFixedMaterialCodePriority(ProductionPlanGroupInfo groupPlanInfo) {
        if (StringUtils.isBlank(fixedMaterialCode)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return CxMachineFixedPriorityEnum.DEFAULT;
        }
        Set<String> materialCodePlanSet = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialCode).collect(Collectors.toSet());
        Set<String> fixedMaterialCodeSet = Stream.of(fixedMaterialCode.split(StringConstant.COMMA)).collect(Collectors.toSet());
        for (String materialCode : materialCodePlanSet) {
            if (fixedMaterialCodeSet.contains(materialCode)) {
                return CxMachineFixedPriorityEnum.FIXED_SKU;
            }
        }
        return CxMachineFixedPriorityEnum.DEFAULT;
    }

    /**
     * 是否有固定
     * fixedStructure1,fixedStructure2,fixedStructure3,fixedMaterialCode
     * 都为空，则无固定 = false;
     *
     * @return
     */
    private boolean hasFixed() {
        if (StringUtils.isNotBlank(fixedStructure1)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedStructure2)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedStructure3)) {
            return true;
        }
        if (StringUtils.isNotBlank(fixedMaterialCode)) {
            return true;
        }
        return false;
    }

    /**
     * 结构提前收尾段处理
     * 分配信息是否需要移除
     *
     * @param context        排产上下文
     * @param allocationInfo 提前收尾段
     */
    private void handlerBeforeConclusionByAllocation(Context context, CxMachineAllocationPlanHelper allocationInfo) {
        if (null == allocationInfo) {
            return;
        }
        if (CollectionUtils.isEmpty(allocationList)) {
            return;
        }
        if (allocationInfo.getAllocationDay() > BigDecimal.ZERO.intValue()) {
            return;
        }
        //如果分配量为零，则直接删除
        boolean isDeleted = allocationList.remove(allocationInfo);
        if (!isDeleted) {
            return;
        }
        Integer changeDay = allocationInfo.getStartDay();
        CxMachineAllocationPlanHelper lastInfo = getLastAllocationInfo();
        if (isChangeGroup(context, lastInfo, allocationInfo)) {
            TbrProductionContext productionContext = (TbrProductionContext) context;
            String groupName = allocationInfo.getAllocationGroup();
            TbrProductionGroupLogRecorder.addReleaseCxMachineChangeGroupLog(productionContext, cxMachineCode, changeDay, groupName);
            productionContext.getBaseDataContainer().getDayCapacityLimit().deductionChangeGroupNameUsedQty(productionContext, changeDay, cxMachineCode, groupName);
        }
    }

    /**
     * 处理限制信息业务
     * 1、成型工装的日使用量
     * 2、成型已分配日
     * 3、分组每日占用日产能
     * 4、特殊材料结构时，分配占用量
     *
     * @param context            排产上下文
     * @param addAllocationPlan  分配信息
     * @param productionPlanInfo 分配分组计划
     */
    private void handlerLimitInfo(Context context, CxMachineAllocationPlanHelper addAllocationPlan, ProductionPlanGroupInfo productionPlanInfo) {
        //20260119 处理成型工装的日使用量
        Integer startDay = addAllocationPlan.getStartDay();
        Integer endDay = addAllocationPlan.getEndDay();
        String proSize = productionPlanInfo.getProSizeInfo();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        baseDataContainer.getCxMachineBaseInfo().get(cxMachineCode);
        DayCapacityLimitVo dayCapacityLimit = baseDataContainer.getDayCapacityLimit();
        for (Integer productionDay = startDay; productionDay <= endDay; productionDay++) {
            if (stopDayInfo.contains(productionDay)) {
                continue;
            }
            //20260123 成型已分配日
            allocationDaySet.add(productionDay);
            //20260120 成型工装占用量
            baseDataContainer.addUsedCount(productionDay, proSize, cxMachineCode);
            //20260125 分组占用每日产能
            dayCapacityLimit.addCxMachineGroupNameAllocationUsedQty(context, productionDay, addAllocationPlan);
        }
        //todo 20260211 特殊材料分配库存更新
        Integer allocationDay = addAllocationPlan.getAllocationDay();
        productionContext.updateSpecialMaterialInfoMap(productionPlanInfo, allocationDay);
    }

    /**
     * 判断是否切换结构
     *
     * @param beforeAllocation 前一个配置
     * @param afterAllocation  后一个配置
     * @return
     */
    private boolean isChangeGroup(Context context, CxMachineAllocationPlanHelper beforeAllocation, CxMachineAllocationPlanHelper afterAllocation) {
        if (null == afterAllocation) {
            return false;
        }
        Integer startDay = afterAllocation.getStartDay();
        String groupName = afterAllocation.getAllocationGroup();
        return isChangeGroup(context, beforeAllocation, startDay, groupName);
    }

    /**
     * 判断是否切换分组(TBR为结构)
     *
     * @param context          排产上下文
     * @param beforeAllocation 前一配置
     * @param changeStartDay   切换日
     * @param changeGroupName  切换的分组名
     * @return
     */
    private boolean isChangeGroup(Context context, CxMachineAllocationPlanHelper beforeAllocation, Integer changeStartDay, String changeGroupName) {
        if (null == changeStartDay || StringUtils.isBlank(changeGroupName)) {
            return false;
        }
//        //第一天先不判断
//        if (ProductionConstant.MONTH_START_DAY.equals(changeStartDay)) {
//            return false;
//        }
        //没有分配信息，看续作
        if (null == beforeAllocation) {
            TbrProductionContext productionContext = (TbrProductionContext) context;
            Map<String, String> continueGroupInfoMap = productionContext.getContinueStructureMap();
            if (CollectionUtils.isEmpty(continueGroupInfoMap)) {
                return false;
            }
            String continueGroupName = continueGroupInfoMap.get(cxMachineCode);
            return !changeGroupName.equals(continueGroupName);
        }
        //分组名是否相同
        return !beforeAllocation.getAllocationGroup().equals(changeGroupName);
    }

}
