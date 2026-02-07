package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.ProductSpecificationsUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.daylimit.*;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.enums.ContinueTypeEnum;
import com.zlt.aps.factory.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.factory.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.ProductionCapacityParamConfiguration;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 排产计划分组信息对象
 * TBR 则分组名为结构
 *
 * @author ZLT
 * @date 20251212
 */
@Data
@Slf4j
public class ProductionPlanGroupInfo {
    /**
     * 产品品类 TBR 全钢 PCR 半钢
     */
    private ProductTypeEnum productType;
    /**
     * 分组值 TBR为结构
     */
    private String groupName;
    /**
     * 是否零度结构 1 是 0 否
     */
    private String isZero;
    /**
     * 分配产能的总需求量
     */
    private Integer sumPlanQty;
    /**
     * 最小硫化机台数(结构与硫化配比，取最小)
     */
    private Integer minLhMachineCount;
    /**
     * 结构的SKU中最小的日硫化产能
     */
    private Integer minLhDayCapacityQty;
    /**
     * 分组的计划信息
     */
    private List<MonthPlanProductionRequirePlanVo> groupPlanData;
    /**
     * 结构指定机台集合
     */
    private Set<String> fixedCxMachineSet;
    /**
     * 估算需要的机台数
     */
    private BigDecimal needCxCapacityMachineCount;
    /**
     * 估算需要的天数
     */
    private Integer theoryDays;
    /**
     * 剩余需要分配的天数
     */
    private Integer leftOverNeedAllocationDays;
    /**
     * 成型-硫化配比信息
     * key=机型typeCode ： value=配比信息
     */
    private Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap;
    /**
     * 针对成型机的固定优先级
     */
    private Integer fixedPriority;
    /**
     * 近1个月的上机日期-机台挑计划时使用
     */
    private Integer lastBoardingDate;
    /**
     * 近3个月的排产次数-机台挑计划时使用
     */
    private Integer productionCount;

    /**
     * 排产-成型硫化产能限制
     * 包含 最大胎胚数
     * 最大硫化机台数
     * 实单最低硫化机台数
     * key=day : value=日成型硫化产能限制实例
     */
    private Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo;
    /**
     * 分配的成型机台
     */
    private Set<String> allocationCxMachineCodeSet;
    /**
     * 确定结构机台分配后，成型硫化配比最后一天排产分组信息
     * 在机结构-需要考虑后续新增机台场景
     */
    @Deprecated
    private Map<Integer, CxLhProductionHelper> cxLhRatioMap;
    /**
     * 日排产信息
     * 数据存储
     */
    @Deprecated
    private Map<Integer, List<GroupPlanDayProductionInfoHelper>> dayProductionInfo;
    /**
     * 是否分配完毕 1 分配完成
     */
    private Integer isAllocationFinish;

    /**
     * 特殊材料清单，同结构的特殊材料用量相同
     */
    private Map<String, BigDecimal> embryoSpecialMaterialInfoMap;

    /**
     * 是否最后一个特殊材料结构
     */
    private Boolean isLatestSpecialMaterial;

    /**
     * 构建初始化分组信息对象
     *
     * @param groupName     分组名 TBR 结构 PCR 英寸
     * @param productType   产品品类 TBR PCR
     * @param groupPlanData 分组所有计划
     * @return
     */
    public static ProductionPlanGroupInfo createInitByGroupList(Context context, String groupName, ProductTypeEnum productType, List<MonthPlanProductionRequirePlanVo> groupPlanData) {
        ProductionPlanGroupInfo groupInfo = new ProductionPlanGroupInfo();
        groupInfo.setGroupName(groupName);
        groupInfo.setProductType(productType);
        groupInfo.setIsZero(YesOrNoEnum.NO.getCode());
        groupInfo.setGroupPlanData(groupPlanData);
        groupInfo.setFixedCxMachineSet(new HashSet<>());
        groupInfo.setIsLatestSpecialMaterial(false);
        // 处理特殊材料清单
        if (!CollectionUtils.isEmpty(groupPlanData)) {
            TbrProductionContext productionContext = (TbrProductionContext) context;
            // 判断如果是特殊结构，需要判断是否最后一个结构
            Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap = productionContext.getBaseDataContainer()
                    .getEmbryoSpecialMaterialInfoMap(); // 胎胚与特殊材料对应关系清单
            Map<String, BigDecimal> materilMap = embryoSpecialMaterialInfoMap
                    .get(CollectionUtils.firstElement(groupPlanData).getEmbryoCode()); // 本结构涉及的特殊材料清单
            if (materilMap == null) {
                materilMap = new HashMap<>();
            }
            groupInfo.setEmbryoSpecialMaterialInfoMap(materilMap);
        }
        return groupInfo;
    }

    /**
     * 检测特殊材料数据是否正常
     *
     * @param context 排产上下文
     */
    public void checkSpecialMaterialData(Context context) {
        if (!isSpecialMaterial()) {
            return;
        }
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = groupPlanData.stream().filter(singlePlan -> singlePlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return;
        }
        Set<String> embryoCodeSet = effectiveList.stream().map(MonthPlanProductionRequirePlanVo::getEmbryoCode).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(embryoCodeSet)) {
            return;
        }
        // 胎胚与特殊材料对应关系清单
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap = ((TbrProductionContext) context).getBaseDataContainer().getEmbryoSpecialMaterialInfoMap();
        Map<String, BigDecimal> firstConfiguration = null;
        boolean isSameConfiguration = true;
        for (String embryoCode : embryoCodeSet) {
            Map<String, BigDecimal> singleConfiguration = embryoSpecialMaterialInfoMap.get(embryoCode);
            if (CollectionUtils.isEmpty(singleConfiguration)) {
                singleConfiguration = Collections.emptyMap();
            }
            if (null == firstConfiguration) {
                firstConfiguration = singleConfiguration;
                continue;
            }
            Set<String> theorySet = firstConfiguration.keySet();
            int theorySize = theorySet.size();
            Set<String> singleConfigurationSet = singleConfiguration.keySet();
            if (theorySize != singleConfigurationSet.size()) {
                isSameConfiguration = false;
                break;
            }
            Set<String> intersectionSet = theorySet.stream().filter(singleConfigurationSet::contains).collect(Collectors.toSet());
            int resultSize = intersectionSet.size();
            if (theorySize != resultSize) {
                isSameConfiguration = false;
                break;
            }
        }
        if (isSameConfiguration) {
            return;
        }
        //特殊材料配置不一致，不排
        String specialMaterialNoSameReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.GROUP_SPECIAL_MATERIAL_NO_SAME);
        effectiveList.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(specialMaterialNoSameReason));
    }

    /**
     * 获取所有有效需求量
     *
     * @return
     */
    public Integer getAllDemandQty() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = groupPlanData.stream().filter(singlePlan -> singlePlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return BigDecimal.ZERO.intValue();
        }
        return effectiveList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
    }

    /**
     * 获取结构下的英寸信息，随意一条计划的因此即可
     *
     * @return
     */
    public String getProSizeInfo() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return null;
        }
        List<MonthPlanProductionRequirePlanVo> hasProSizeList = groupPlanData.stream().filter(singlePlan -> StringUtils.isNotBlank(singlePlan.getProSize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProSizeList)) {
            return null;
        }
        Set<String> proSizeSet = hasProSizeList.stream().map(MonthPlanProductionRequirePlanVo::getProSize).collect(Collectors.toSet());
        return new ArrayList<>(proSizeSet).get(BigDecimal.ZERO.intValue());
    }

    /**
     * 更新设置整个分组计划不排产
     * 没有达到起排量
     */
    public void setNoProductionNoReachMinProductionDays(Integer minProductionDays) {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        theoryDays = BigDecimal.ZERO.intValue();
        leftOverNeedAllocationDays = BigDecimal.ZERO.intValue();
        needCxCapacityMachineCount = BigDecimal.ZERO;
        String noReachMinProductionDaysReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MIN_CX_CAPACITY_WHOLE_STRUCTURE_NAME, minProductionDays);
        groupPlanData.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(noReachMinProductionDaysReason));
    }

    /**
     * 更新设置整个分组不排产-没有成型硫化配比配置
     */
    public void setNoProductionNoCxMachineLhRatio() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        String noConfigurationLhRatioReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.GROUP_NO_CONFIGURATION_LH_RATION);
        groupPlanData.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(noConfigurationLhRatioReason));
    }

    /**
     * 更新设置整个分组计划不排产
     * 因提前收尾导致不满足最低排产天数
     */
    public void setNoProductionLowMinLhMachineNoReachMinProductionDays(Integer minLhMachineCount, Integer minProductionDays) {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        String lowMinLhMachineNoReachMinProductionDaysReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_LOW_MIN_LH_MACHINE_COUNT_WHOLE_STRUCTURE_NAME, minLhMachineCount, minProductionDays);
        groupPlanData.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(lowMinLhMachineNoReachMinProductionDaysReason));
    }

    /**
     * 粗步计算 结构需求量需要的成型产能分配
     * 结构有效总需求量/(结构下SKU最小日硫化量 * 结构最小硫化配比值 * 月份生产天数
     * 保留1位小数
     * 如果 小数部分 > 0.9，则向上取整
     *
     * @param context         排产上下文
     * @param requirePlanList 需排产的计划
     * @return
     */
    public static Map<String, ProductionPlanGroupInfo> statisticsAndEstimateCxAllocationByGroup(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        BaseDataContainer baseDataContainer = ((TbrProductionContext) context).getBaseDataContainer();
        List<MonthPlanStructureLhRatioVo> structureLhRatioList = baseDataContainer.getStructureLhRatioList();
        //根据结构成型硫化配比信息，提取结构最小的硫化配比和结构分组成型硫化配比
        Map<String, List<MonthPlanStructureLhRatioVo>> structureGroupMap = getStructureGroupInfo(structureLhRatioList);
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = getMinLhRatioMap(structureGroupMap);
        //1、对计划按结构分组，构建结构分组对象ProductionPlanGroupInfo
        Map<String, ProductionPlanGroupInfo> groupInfoMap = buildGroupPlanInfoMap(context, requirePlanList, structureGroupMap);
        //2、提取有效净需求--剔除不可排产的，且没有超出模具产能-汇总需求量，并获得分组下最小日硫化产能
        calculateEffectiveByMouldMaxCapacity(context, groupInfoMap);
        //3、根据结构的硫化配比及最小的硫化机台数 估算需要的成型机台数
        groupInfoMap.forEach((structureName, groupInfo) -> {
            MonthPlanStructureLhRatioVo ratioInfo = minLhRatioMap.get(structureName);
            if (null == ratioInfo) {
                groupInfo.setAllocationZero();
                log.info(TbrProductionGroupLogRecorder.addGroupLhRatioEmptyLog(context, structureName));
                return;
            }
            groupInfo.setMinLhMachineCount(ratioInfo.getLhMachineMaxQty());
            //粗算所需成型机台数
            groupInfo.calculateNeedCxCapacityMachineCount(context, context.getMaxProductionDays());
        });
        //4、对分组计划中没有满足最低上机天数的，将天数上调到最低上机天数-由模拟排产阶段决定是否要提前收尾
        ProductionCapacityParamConfiguration paramConfiguration = baseDataContainer.getParamConfiguration();
        Integer minProductionDays = paramConfiguration.getMinProductionDays();
        Integer minAllocationDays = paramConfiguration.getMinAllocationDays();
        groupInfoMap.forEach((structureName, groupInfo) -> {
            Integer theoryDays = groupInfo.getTheoryDays();
            //分配天数为零，或是小于最小要求天数，则设置不排产
            if (groupInfo.isBelowMinProductionDays(minProductionDays)) {
                groupInfo.setNoProductionNoReachMinProductionDays(minProductionDays);
                return;
            }
            Integer realTheoryDays = Math.max(theoryDays, minAllocationDays);
            groupInfo.setTheoryDays(realTheoryDays);
            groupInfo.setLeftOverNeedAllocationDays(realTheoryDays);
            if (realTheoryDays.equals(theoryDays)) {
                return;
            }
            //重新计算估算的机台数
            BigDecimal newNeedCxCapacityMachineCount = BigDecimal.valueOf(realTheoryDays).divide(BigDecimal.valueOf(context.getMonthDays()), 1, RoundingMode.UP);
            groupInfo.setNeedCxCapacityMachineCount(newNeedCxCapacityMachineCount);
        });
        return groupInfoMap;
    }

    /**
     * 理论需排产天数是否低于最小要求排产天数
     * 如果theoryDays或是minProductionDays为空，
     * 则都认为低于
     * 否则 theoryDays < minProductionDays
     *
     * @param minProductionDays
     * @return
     */
    public boolean isBelowMinProductionDays(Integer minProductionDays) {
        if (null == minProductionDays || null == theoryDays) {
            return true;
        }
        return theoryDays < minProductionDays;
    }

    /**
     * 获取续作Sku的收尾时间点
     * 1、如果dayProductionLimit没有数据，则取周期排产天数
     * 2、dayProductionLimit有值，则取其最大的排产天
     *
     * @param context
     * @return
     */
    public Integer getContinueSkuDeadLineDay(Context context) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return context.getMonthDays();
        }
        List<Integer> productionDayList = new ArrayList<>(dayProductionLimitInfo.keySet());
        Collections.sort(productionDayList);
        return productionDayList.get(productionDayList.size() - BigDecimal.ONE.intValue());
    }

    /**
     * 更新剩余分配天数
     *
     * @param allocationDays 当前分配天数
     */
    public void updateLeftOverNeedAllocationDays(Integer allocationDays) {
        if (null == allocationDays || allocationDays <= BigDecimal.ZERO.intValue()) {
            return;
        }
        if (null == leftOverNeedAllocationDays) {
            return;
        }
        if (leftOverNeedAllocationDays <= allocationDays) {
            leftOverNeedAllocationDays = BigDecimal.ZERO.intValue();
        } else {
            leftOverNeedAllocationDays = leftOverNeedAllocationDays - allocationDays;
        }
        if (leftOverNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            isAllocationFinish = YesOrNoEnum.YES.getValue();
            leftOverNeedAllocationDays = BigDecimal.ZERO.intValue();
        }
    }

    /**
     * 获取结构下，最早收尾的硫化机台组
     *
     * @return
     */
    @Deprecated
    public CxLhProductionHelper getEarliestConclusionLhGroup() {
        //获取成型硫化组
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            return null;
        }
        List<CxLhProductionHelper> cxLhGroupList = new ArrayList<>(cxLhRatioMap.values());
        //剔除一开始没有排产的？
        List<CxLhProductionHelper> hasProductionList = cxLhGroupList.stream().filter(cxLhGroup -> null != cxLhGroup.getProductionDay()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return null;
        }
        //按最后排产日，进行升序排序
        hasProductionList.sort(Comparator.comparing(CxLhProductionHelper::getProductionDay).thenComparing(CxLhProductionHelper::getLhGroupNo));
        //取得第一条：即最早收尾的硫化组
        return hasProductionList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 获取成型配比配置信息
     *
     * @param cxMachineInfo
     * @return
     */
    public ProductGroupCxCapacityInfo getLhRatioByCxMachine(CxMachineBaseInfoVo cxMachineInfo) {
        MonthPlanStructureLhRatioVo lhRatio = getLhRatio(cxMachineInfo);
        if (null == lhRatio) {
            return null;
        }
        return ProductGroupCxCapacityInfo.buildCxCapacityInfo(cxMachineInfo.getCxMachineCode(), lhRatio);
    }

    /**
     * 从结构的成型硫化配比中，获取对应成型机机型的成型硫化配比
     * 如果对应机型没有找到，则匹配机型为空的配比-**
     *
     * @param cxMachineInfo
     * @return
     */
    public MonthPlanStructureLhRatioVo getLhRatio(CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || StringUtils.isBlank(cxMachineInfo.getCxMachineTypeCode())) {
            return null;
        }
        if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
            return null;
        }
        MonthPlanStructureLhRatioVo lhRatio = cxMachineLhRationMap.get(cxMachineInfo.getCxMachineTypeCode());
        if (null != lhRatio) {
            return lhRatio;
        }
        return cxMachineLhRationMap.get(ProductionConstant.ALL_BRAND_CODE_MATCH);
    }

    /**
     * 结构最晚收尾日
     *
     * @return
     */
    public Integer getLatestEndDay() {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return BigDecimal.ZERO.intValue();
        }
        List<Integer> productionDayList = dayProductionLimitInfo.keySet().stream().collect(Collectors.toList());
        productionDayList.sort(Comparator.reverseOrder());
        return productionDayList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 获取结构下，最早续作收尾的硫化组信息
     *
     * @return
     */
    public EarliestConclusionLhGroupHelper getEarliestConclusionLhInfoByContinueSku(Context context, Map<String, CxContinueSkuInfoHelper> continueSkuMap) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(continueSkuMap)) {
            return null;
        }
        //得到续作最大硫化组可使用的模具数
        Integer sumMouldNumber = continueSkuMap.values().stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        List<GroupPlanCxLhCapacityLimitHelper> hasAddContinueSkuList = dayLimitList.stream().filter(dayLimit -> dayLimit.getProductionMouldSet().size() < sumMouldNumber).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddContinueSkuList)) {
            return null;
        }
        //按日期由小到大排序，找出最早的
        hasAddContinueSkuList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        GroupPlanCxLhCapacityLimitHelper selectedDayLimit = hasAddContinueSkuList.get(BigDecimal.ZERO.intValue());
        GroupPlanCxLhCapacityLimitHelper endDayLimit = hasAddContinueSkuList.get(hasAddContinueSkuList.size() - BigDecimal.ONE.intValue());
        Integer conclusionDay = selectedDayLimit.getDay();
        Integer endDay = endDayLimit.getDay();
        if (isGroupStartDayByFormalProduction(conclusionDay)) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        Integer previousDay = getPreviousDay(conclusionDay);
        if (null == previousDay) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        GroupPlanCxLhCapacityLimitHelper previousLimit = dayProductionLimitInfo.get(previousDay);
        Integer canAddCount = previousLimit.getReleaseLhMachineCount(selectedDayLimit);
        if (canAddCount <= BigDecimal.ZERO.intValue()) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        SkuDayProductionInfoHelper previousSku = selectedDayLimit.getEarliestConclusionSkuInfo(previousLimit, canAddCount);
        if (null == previousSku) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        return EarliestConclusionLhGroupHelper.createEarliestConclusionLhGroup(conclusionDay, endDay, previousSku, true);
    }

    /**
     * 获取结构下，最早收尾的硫化信息
     *
     * @param context     排产上下文
     * @param excludeDays 排除的收尾时间点
     * @return
     */
    public EarliestConclusionLhGroupHelper getEarliestConclusionLhInfo(Context context, Set<Integer> excludeDays) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return null;
        }
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList = dayLimitList.stream().filter(dayLimit -> {
            //前一日排产情况
            GroupPlanCxLhCapacityLimitHelper previousDayLimit = getPreviousDayInfo(dayLimit);
            //后一日排产情况
            GroupPlanCxLhCapacityLimitHelper nexDayLimit = getNextDayInfo(dayLimit);
            return !dayLimit.isReachLimitByMouldNumber(previousDayLimit, nexDayLimit);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        if (!CollectionUtils.isEmpty(excludeDays)) {
            //20260113 剔除需要排除的收尾时间点
            hasAddSkuList = hasAddSkuList.stream().filter(singleGroup -> !excludeDays.contains(singleGroup.getDay())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        hasAddSkuList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        GroupPlanCxLhCapacityLimitHelper selectedDayLimit = hasAddSkuList.get(BigDecimal.ZERO.intValue());
        GroupPlanCxLhCapacityLimitHelper endDayLimit = hasAddSkuList.get(hasAddSkuList.size() - BigDecimal.ONE.intValue());
        Integer conclusionDay = selectedDayLimit.getDay();
        Integer endDay = endDayLimit.getDay();
        if (isGroupStartDayByFormalProduction(conclusionDay)) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        Integer previousDay = getPreviousDay(conclusionDay);
        if (null == previousDay) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        GroupPlanCxLhCapacityLimitHelper previousLimit = dayProductionLimitInfo.get(previousDay);
        //可释放的机台 Integer canAddCount = previousLimit.getUsedLhMachineCount() - selectedDayLimit.getUsedLhMachineCount();
        Integer canAddCount = previousLimit.getReleaseLhMachineCount(selectedDayLimit);
        if (canAddCount <= BigDecimal.ZERO.intValue()) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        SkuDayProductionInfoHelper previousSku = selectedDayLimit.getEarliestConclusionSkuInfo(previousLimit, canAddCount);
        if (null == previousSku) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        return EarliestConclusionLhGroupHelper.createEarliestConclusionLhGroup(conclusionDay, endDay, previousSku, true);
    }

    /**
     * 根据选择的Sku判断其符合胎胚种类数限制及其上机时间点和排产结束日
     * 兼容考虑模具的上机时间--新模具到货计划
     *
     * @param context           排产上下文
     * @param addSkuInfo        需要上机的Sku
     * @param preSelected       预计选中
     * @param selectedMould     选中的模具
     * @param onLineMachineInfo 在产机台信息
     * @return
     */
    public void correctProductionDateRange(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, EarliestConclusionLhGroupHelper preSelected, List<ProductionMouldInfoVo> selectedMould, String onLineMachineInfo) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == preSelected) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        Integer preClosingDay = preSelected.getClosingDay();
        Integer preEndDay = preSelected.getEndDay();
        MouldProductionDayLimitHelper limitHelper = LhGroupProductionRangeCalculator.confirmProductionRange(productionContext, addSkuInfo, preClosingDay, preEndDay, selectedMould, dayLimitList, productionContext.getStopDays());
        Set<Integer> effectiveRangeSet = limitHelper.getProductionDaySet();
        if (CollectionUtils.isEmpty(effectiveRangeSet)) {
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuLimitLog(context, groupName, onLineMachineInfo, addSkuInfo.getMaterialDesc(), limitHelper.getLimitType()));
            preSelected.updateProductionDateRange(null, null);
            return;
        }
        List<Integer> sortList = new ArrayList<>(effectiveRangeSet);
        Collections.sort(sortList);
        int size = sortList.size();
        Integer newClosingDay = sortList.get(BigDecimal.ZERO.intValue());
        Integer newEndDay = sortList.get(size - BigDecimal.ONE.intValue());
        preSelected.updateProductionDateRange(newClosingDay, newEndDay);
        //20260122 换模次数控制
        if (!preSelected.isChangeMould(addSkuInfo)) {
            return;
        }
        //需要换模
        DayCapacityLimitVo changeMouldLimitHandler = productionContext.getBaseDataContainer().getDayCapacityLimit();
        changeMouldLimitHandler.confirmStartDayByChangeMouldLimit(productionContext, preSelected, selectedMould);
        Integer changeMouldDay = preSelected.getClosingDay();
        if (null == changeMouldDay || null == preSelected.getEndDay()) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addLhGroupSkuLimitLog(context, groupName, onLineMachineInfo, addSkuInfo.getMaterialDesc(), MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT));
            productionContext.addSkuProductionLimitInfo(addSkuInfo.getMaterialDesc(), MouldProductionLimitTypeEnum.CHANGE_MOULD_LIMIT);
            return;
        }
        //换模次数处理
        Set<String> mouldCodeSet = selectedMould.stream().map(ProductionMouldInfoVo::getMouldCode).collect(Collectors.toSet());
        changeMouldLimitHandler.addChangeMouldUsedQty(productionContext, changeMouldDay, addSkuInfo.getMaterialDesc(), mouldCodeSet);
    }

    /**
     * 获取结构提前收尾的最低硫化配比信息
     *
     * @return
     */
    public Integer getClosureMinLhRatio() {
        if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
            return null;
        }
        List<MonthPlanStructureLhRatioVo> lhRatioList = new ArrayList<>(cxMachineLhRationMap.values());
        if (CollectionUtils.isEmpty(lhRatioList)) {
            return null;
        }
        return lhRatioList.get(BigDecimal.ZERO.intValue()).getLhMachineMinQty();
    }

    /**
     * 计算需要分配的成型产能机台数，保留1位小数
     * 双模方式
     * 有效总需求量 / (SKU最小日硫化量 * 2 * 结构最小硫化配比 * 月度可排产天数),两位小数
     * 如果 小数部分 >0.9，则向上取整
     * 否则 = 保留1位小数
     *
     * @param context                排产上下文
     * @param monthMaxProductionDays 月度最大可生产天数
     */
    public void calculateNeedCxCapacityMachineCount(Context context, Integer monthMaxProductionDays) {
        if (sumPlanQty <= BigDecimal.ZERO.intValue()) {
            setAllocationZero();
            return;
        }
        if (minLhMachineCount <= BigDecimal.ZERO.intValue()) {
            setAllocationZero();
            return;
        }
        if (minLhDayCapacityQty <= BigDecimal.ZERO.intValue()) {
            setAllocationZero();
            return;
        }
        BigDecimal monthMaxDays = BigDecimal.valueOf(Long.valueOf(monthMaxProductionDays));
        //单台成型日产能 = 最低硫化机台数 * 最小硫化量(单模) * 2
        BigDecimal singleMinDayCapacity = getDayCapacityByLhRatio(minLhMachineCount);
        //理论需排产天数
        Integer theoryDays = BigDecimal.valueOf(sumPlanQty).divide(singleMinDayCapacity, 0, RoundingMode.UP).intValue();
        //单台成型月产能 = 单台成型日产能 * 月份可排产天数(排除停产日)
        BigDecimal singleCxMonthCapacity = singleMinDayCapacity.multiply(monthMaxDays);
        BigDecimal machineCount = BigDecimal.valueOf(sumPlanQty).divide(singleCxMonthCapacity, 2, RoundingMode.HALF_UP);
        //取整数部分，向下取整
        BigDecimal integerPart = machineCount.setScale(0, RoundingMode.DOWN);
        //小数部分
        BigDecimal decimalPart = machineCount.subtract(integerPart);
        if (decimalPart.compareTo(BigDecimal.valueOf(ProductionConstant.REPAIR_WHOLE)) > BigDecimal.ZERO.intValue()) {
            needCxCapacityMachineCount = integerPart.add(BigDecimal.ONE);
            theoryDays = needCxCapacityMachineCount.multiply(monthMaxDays).intValue();
            this.theoryDays = theoryDays;
            this.leftOverNeedAllocationDays = theoryDays;
            log.info(TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(context, groupName, sumPlanQty, minLhMachineCount, minLhDayCapacityQty, theoryDays, needCxCapacityMachineCount));
            return;
        }
        this.theoryDays = theoryDays;
        this.leftOverNeedAllocationDays = theoryDays;
        needCxCapacityMachineCount = machineCount.setScale(1, RoundingMode.UP);
        log.info(TbrProductionGroupLogRecorder.addGroupCalculateCxMachineCountLog(context, groupName, sumPlanQty, minLhMachineCount, minLhDayCapacityQty, theoryDays, needCxCapacityMachineCount));
    }

    /**
     * 得到计划上机日
     *
     * @param productionPlan 排产计划
     * @param startDay       理论起始排产日
     * @param endDay         理论结束日
     * @return
     */
    public Integer getRealOnlineMachineDay(MonthPlanProductionRequirePlanVo productionPlan, Integer startDay, Integer endDay) {
        Integer realStartDay = startDay;
        boolean canProduction = false;
        for (; realStartDay <= endDay; ) {
            if (isAddSkuProductionByOneLhMachine(productionPlan, realStartDay)) {
                canProduction = true;
                break;
            }
            realStartDay = realStartDay + BigDecimal.ONE.intValue();
        }
        if (!canProduction) {
            return null;
        }
        return realStartDay;
    }

    /**
     * 获取结构剩余待分配天数
     * 不能根据剩余待排产量来估算，
     * 因为有超出模具产能的干扰
     *
     * @return
     */
    public Integer getRemainingNeedAllocationDays() {
        //标记是否分配完毕
        if (YesOrNoEnum.YES.getValue().equals(isAllocationFinish)) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == leftOverNeedAllocationDays) {
            return BigDecimal.ZERO.intValue();
        }
        return leftOverNeedAllocationDays;
    }

    /**
     * 根据成型对应硫化配比，得到剩余需求量需要分配的天数
     *
     * @param lhRatio 硫化配比
     * @return
     */
    @Deprecated
    public Integer calculateNeedDays(Integer lhRatio) {
        if (minLhDayCapacityQty <= BigDecimal.ZERO.intValue() || null == lhRatio || lhRatio <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //剩余需求量
        Integer remainingProductionQty = getRemainingProductionQty();
        if (remainingProductionQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal dayCapacity = getDayCapacityByLhRatio(lhRatio);
        return BigDecimal.valueOf(remainingProductionQty).divide(dayCapacity, 0, RoundingMode.UP).intValue();
    }

    /**
     * 获取结构分组下剩余还需排产量
     *
     * @return
     */
    @Deprecated
    private Integer getRemainingProductionQty() {
        //标记是否分配完毕
        if (YesOrNoEnum.YES.getValue().equals(isAllocationFinish)) {
            return BigDecimal.ZERO.intValue();
        }
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        return hasProductionList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
    }

    /**
     * 根据续作类型，获取续作可排产信息
     * 1、同规格同花纹
     * 2、同生胎同模具
     *
     * @param continueType              续作类型
     * @param materialDesc              续作Sku
     * @param shareMouldMaterialDescSet 共用模具的物料集合
     * @param continueProductInfoHelper 续作Sku详细信息
     * @return
     */
    public List<MonthPlanProductionRequirePlanVo> getContinueListByType(ContinueTypeEnum continueType, String materialDesc, Set<String> shareMouldMaterialDescSet, CxContinueSkuInfoHelper continueProductInfoHelper) {
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
            return getSameSpecificationsAndPatternPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        }
        if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
            return getSameEmbryoCodeAndMouldPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        }
        return Collections.emptyList();
    }

    /**
     * 获取分组下同规格同花纹的Sku计划
     *
     * @param materialDesc              前规格
     * @param shareMouldMaterialDescSet 共用模具的sku集合
     * @param continueProductInfoHelper 前规格详情信息
     * @return
     */
    public List<MonthPlanProductionRequirePlanVo> getSameSpecificationsAndPatternPlan(String materialDesc, Set<String> shareMouldMaterialDescSet, CxContinueSkuInfoHelper continueProductInfoHelper) {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionRequirePlanVo> sameSpecificationsAndPatternList = new ArrayList<>();
        groupPlanData.stream().forEach(groupPlan -> {
            if (!groupPlan.hasProduction()) {
                return;
            }
            if (materialDesc.equals(groupPlan.getMaterialDesc())) {
                return;
            }
            //不共用模具
            if (!shareMouldMaterialDescSet.contains(groupPlan.getMaterialDesc())) {
                return;
            }
            if (groupPlan.isSameSpecificationsAndPattern(continueProductInfoHelper)) {
                sameSpecificationsAndPatternList.add(groupPlan);
            }
        });
        return sameSpecificationsAndPatternList;
    }

    /**
     * 获取分组下同生胎同模具下的Sku计划
     *
     * @param materialDesc              前规格
     * @param shareMouldMaterialDescSet 共用模具的sku集合
     * @param continueProductInfoHelper 前规格详情信息
     */
    public List<MonthPlanProductionRequirePlanVo> getSameEmbryoCodeAndMouldPlan(String materialDesc, Set<String> shareMouldMaterialDescSet, CxContinueSkuInfoHelper continueProductInfoHelper) {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            Collections.emptyList();
        }
        List<MonthPlanProductionRequirePlanVo> sameEmbryoCodeAndMouldList = new ArrayList<>();
        groupPlanData.stream().forEach(groupPlan -> {
            //剔除不排产
            if (!groupPlan.hasProduction()) {
                return;
            }
            //排除自己
            if (materialDesc.equals(groupPlan.getMaterialDesc())) {
                return;
            }
            //剔除不共用模具
            if (!shareMouldMaterialDescSet.contains(groupPlan.getMaterialDesc())) {
                return;
            }
            //同生胎
            if (groupPlan.isSameEmbryoCode(continueProductInfoHelper)) {
                sameEmbryoCodeAndMouldList.add(groupPlan);
            }
        });
        return sameEmbryoCodeAndMouldList;
    }

    /**
     * 获取剩余排产中高优先级的SKU个数
     *
     * @return
     */
    public Integer getHeightPriorityCount() {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasHeightProductionList = hasProductionList.stream().filter(heightProductionPlan -> heightProductionPlan.getHeightProductionQty() > BigDecimal.ZERO.longValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasHeightProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<String> materialSet = hasHeightProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(materialSet)) {
            return BigDecimal.ZERO.intValue();
        }
        return materialSet.size();
    }

    /**
     * 含有特殊材料的SKU个数
     *
     * @return
     */
    public Integer getSpecialMaterialsCount() {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return BigDecimal.ZERO.intValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasSpecialMaterialList = hasProductionList.stream().filter(specialMaterialPlan -> YesOrNoEnum.YES.getCode().equals(specialMaterialPlan.getIsSpecialMaterials())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasSpecialMaterialList)) {
            return BigDecimal.ZERO.intValue();
        }
        Set<String> materialSet = hasSpecialMaterialList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(materialSet)) {
            return BigDecimal.ZERO.intValue();
        }
        return materialSet.size();
    }

    /**
     * 是否含有特殊材料
     *
     * @return
     */
    public boolean isSpecialMaterial() {
        return !CollectionUtils.isEmpty(embryoSpecialMaterialInfoMap);
    }

    /**
     * 判断是否可进行下一次分配
     * 特殊材料结构不进行最小分配天数控制
     * 非特殊材料结构，需要判断剩余可分配天数要 >= minAllocationDays
     *
     * @param minAllocationDays 最小要求分配天数
     * @return
     */
    public boolean isNextAllocation(int minAllocationDays) {
        if (isSpecialMaterial()) {
            return leftOverNeedAllocationDays > BigDecimal.ZERO.intValue();
        }
        return leftOverNeedAllocationDays >= minAllocationDays;
    }

    /**
     * 两个结构分组是否含有同规格
     * true 含有同规格
     * false 不含有同规格
     *
     * @param beforeProductionPlanList 前排产结构
     * @return
     */
    public boolean hasSameSpecifications(List<MonthPlanProductionRequirePlanVo> beforeProductionPlanList) {
        if (CollectionUtils.isEmpty(beforeProductionPlanList)) {
            return false;
        }
        Set<String> specificationSet = beforeProductionPlanList.stream().map(MonthPlanProductionRequirePlanVo::getSpecifications).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(specificationSet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> currentProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(currentProductionList)) {
            return false;
        }
        Set<String> currentSpecificationSet = currentProductionList.stream().map(MonthPlanProductionRequirePlanVo::getSpecifications).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(currentSpecificationSet)) {
            return false;
        }
        for (String currentSpecification : currentSpecificationSet) {
            if (specificationSet.contains(currentSpecification)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 两个结构分组断面宽是否在±10范围内
     * true 在±10范围内
     * false 不在±10范围内
     *
     * @param beforeProductionPlanList 前排产结构
     * @param diffValue                断面宽差值范围
     * @return
     */
    public boolean hasSectionWidthCondition(List<MonthPlanProductionRequirePlanVo> beforeProductionPlanList, Integer diffValue) {
        if (CollectionUtils.isEmpty(beforeProductionPlanList)) {
            return false;
        }
        Set<String> specificationSet = beforeProductionPlanList.stream().map(MonthPlanProductionRequirePlanVo::getSpecifications).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(specificationSet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> currentProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(currentProductionList)) {
            return false;
        }
        Set<String> currentSpecificationSet = currentProductionList.stream().map(MonthPlanProductionRequirePlanVo::getSpecifications).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(currentSpecificationSet)) {
            return false;
        }
        String specification = new ArrayList<>(specificationSet).get(BigDecimal.ZERO.intValue());
        List<Integer> sectionWidthAndAspectRatioList = ProductSpecificationsUtils.parseSectionWidthAndAspectRatio(specification);
        Integer sectionWidth;
        if (CollectionUtils.isEmpty(sectionWidthAndAspectRatioList)) {
            sectionWidth = BigDecimal.ZERO.intValue();
        } else {
            sectionWidth = sectionWidthAndAspectRatioList.get(BigDecimal.ZERO.intValue());
        }
        String currentSpecification = new ArrayList<>(currentSpecificationSet).get(BigDecimal.ZERO.intValue());
        List<Integer> currentSectionWidthAndAspectRatioList = ProductSpecificationsUtils.parseSectionWidthAndAspectRatio(currentSpecification);
        Integer currentSectionWidth;
        if (CollectionUtils.isEmpty(currentSectionWidthAndAspectRatioList)) {
            currentSectionWidth = BigDecimal.ZERO.intValue();
        } else {
            currentSectionWidth = currentSectionWidthAndAspectRatioList.get(BigDecimal.ZERO.intValue());
        }
        int diff = Math.abs(sectionWidth - currentSectionWidth);
        return diff <= diffValue;
    }

    /**
     * 两个结构分组是否含有同英寸
     * true 含有同英寸
     * false 不含有同英寸
     *
     * @param beforeProductionPlanList 前排产结构
     * @return
     */
    public boolean hasSameProSize(List<MonthPlanProductionRequirePlanVo> beforeProductionPlanList) {
        if (CollectionUtils.isEmpty(beforeProductionPlanList)) {
            return false;
        }
        Set<String> proSizeSet = beforeProductionPlanList.stream().map(MonthPlanProductionRequirePlanVo::getProSize).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(proSizeSet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> currentProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(currentProductionList)) {
            return false;
        }
        Set<String> currentProSizeSet = currentProductionList.stream().map(MonthPlanProductionRequirePlanVo::getProSize).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(currentProSizeSet)) {
            return false;
        }
        for (String currentSpecification : currentProSizeSet) {
            if (proSizeSet.contains(currentSpecification)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在机结构根据在产机台分配转化成排产限制
     * 对在机结构来说，该部分为初始化
     *
     * @param continueCxMachineAllocation
     */
    public void buildDayProductionLimitInfoByContinue(Context context, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
        }
        //分配的成型机台
        Set<String> cxMachineCodeSet = continueCxMachineAllocation.stream().map(CxMachineAllocationPlanHelper::getCxMachineCode).collect(Collectors.toSet());
        allocationCxMachineCodeSet = cxMachineCodeSet;
        Set<Integer> allProductionDaySet = getAllContinueProductionDaySet(context, continueCxMachineAllocation);
        if (CollectionUtils.isEmpty(allProductionDaySet)) {
            return;
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayLimitInfo = new HashMap<>(64);
        allProductionDaySet.forEach(productionDay -> {
            List<CxMachineAllocationPlanHelper> dayConfigurationList = getContinueProductionConfiguration(context, productionDay, continueCxMachineAllocation);
            GroupPlanCxLhCapacityLimitHelper dayLimit = GroupPlanCxLhCapacityLimitHelper.buildByContinueCxMachineAllocation(context, productionDay, dayConfigurationList);
            dayLimitInfo.put(productionDay, dayLimit);
        });
        //如果没有
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            dayProductionLimitInfo = dayLimitInfo;
            return;
        }
        dayLimitInfo.forEach((productionDay, dayLimit) -> {
            GroupPlanCxLhCapacityLimitHelper old = dayProductionLimitInfo.get(productionDay);
            if (null == old) {
                dayProductionLimitInfo.put(productionDay, dayLimit);
                return;
            }
            old.updateInfo(dayLimit);
        });
        //如果没有，则需要移除
        Set<Integer> removeDay = new HashSet<>();
        dayProductionLimitInfo.forEach((productionDay, dayLimit) -> {
            if (dayLimitInfo.containsKey(productionDay)) {
                return;
            }
            removeDay.add(productionDay);
        });
        dayProductionLimitInfo.keySet().removeIf(new ArrayList<>(removeDay)::contains);
    }

    /**
     * 根据结构转产配置表，重新构建真个分组几乎的日产能限制信息
     *
     * @param context             排产上下文
     * @param groupAllocationList 结构转产配置集合
     */
    public void buildDayProductionLimitInfoByStructureAllocation(Context context, List<MpStructureAllocation> groupAllocationList) {
        Integer monthDays = context.getMonthDays();
        if (CollectionUtils.isEmpty(groupAllocationList)) {
            dayProductionLimitInfo = new HashMap<>(monthDays);
            return;
        }
        List<MpStructureAllocation> effectiveList = groupAllocationList.stream().filter(singleAllocation -> groupName.equals(singleAllocation.getStructureName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            dayProductionLimitInfo = new HashMap<>(monthDays);
            return;
        }
        Set<Integer> allProductionDaySet = getAllProductionDaySet(context, effectiveList);
        if (CollectionUtils.isEmpty(allProductionDaySet)) {
            dayProductionLimitInfo = new HashMap<>(monthDays);
            return;
        }
        Set<String> allocationCxMachineCodeSet = effectiveList.stream().map(MpStructureAllocation::getCxMachineCode).collect(Collectors.toSet());
        this.allocationCxMachineCodeSet = allocationCxMachineCodeSet;
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayLimitInfo = new HashMap<>(64);
        allProductionDaySet.forEach(productionDay -> {
            List<MpStructureAllocation> dayConfigurationList = getProductionConfiguration(context, productionDay, effectiveList);
            GroupPlanCxLhCapacityLimitHelper dayLimit = GroupPlanCxLhCapacityLimitHelper.buildByStructureAllocation(context, productionDay, dayConfigurationList);
            dayLimitInfo.put(productionDay, dayLimit);

        });
        dayProductionLimitInfo = dayLimitInfo;
    }

    /**
     * 增加结构的Sku日排产信息
     *
     * @param skuDayProductionInfo 排产SKu信息
     */
    public void addDayProductionInfo(SkuDayProductionInfoHelper skuDayProductionInfo) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == skuDayProductionInfo) {
            return;
        }
        Integer productionDay = skuDayProductionInfo.getProductionDay();
        String materialDesc = skuDayProductionInfo.getMaterialDesc();
        if (null == productionDay || StringUtils.isBlank(materialDesc)) {
            return;
        }
        GroupPlanCxLhCapacityLimitHelper limitInfo = dayProductionLimitInfo.get(productionDay);
        if (null == limitInfo) {
            return;
        }
        Set<String> currentUsedMouldSet = skuDayProductionInfo.getUsedMouldSet();
        //更新日限制对象-生胎、Sku排产模具等信息
        limitInfo.getProductionEmbryoCodeSet().add(skuDayProductionInfo.getEmbryoCode());
        limitInfo.getProductionMouldSet().addAll(currentUsedMouldSet);
        Set<String> skuProductionMouldSet = limitInfo.getSkuProductionMouldMap().get(materialDesc);
        if (null == skuProductionMouldSet) {
            skuProductionMouldSet = new HashSet<>();
            limitInfo.getSkuProductionMouldMap().put(materialDesc, skuProductionMouldSet);
        }
        skuProductionMouldSet.addAll(currentUsedMouldSet);
        //更新排产Sku信息
        SkuDayProductionInfoHelper planned = limitInfo.getProductionSkuQtyInfo().get(materialDesc);
        if (null == planned) {
            limitInfo.getProductionSkuQtyInfo().put(materialDesc, skuDayProductionInfo);
            return;
        }
        //更新使用模具
        planned.getUsedMouldSet().addAll(currentUsedMouldSet);
        //更新数量
        planned.addProductionDayQty(skuDayProductionInfo.getSumProductionQty(), skuDayProductionInfo.getLossQty());
    }

    /**
     * 设置可排产计划在本轮可进行参与排产
     */
    public void setThisRoundCanProduction() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        //设置可排产的计划在本轮次可进行排产
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return;
        }
        hasProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.YES.getValue()));
    }

    /**
     * 增加日排产信息
     *
     * @param singleDayProductionInfo
     */
    @Deprecated
    public void addDayProductionInfo(GroupPlanDayProductionInfoHelper singleDayProductionInfo) {
        if (null == dayProductionInfo) {
            dayProductionInfo = new HashMap<>();
        }
        if (null == singleDayProductionInfo || !singleDayProductionInfo.isEffective()) {
            return;
        }
        Integer productionDay = singleDayProductionInfo.getProductionDay();
        if (null == productionDay) {
            return;
        }
        List<GroupPlanDayProductionInfoHelper> plannedProductionList = dayProductionInfo.get(productionDay);
        if (null == plannedProductionList) {
            plannedProductionList = new ArrayList<>();
            dayProductionInfo.put(productionDay, plannedProductionList);
        }
        if (CollectionUtils.isEmpty(plannedProductionList)) {
            plannedProductionList.add(singleDayProductionInfo);
            return;
        }
        String key = singleDayProductionInfo.getDuplicateKey();
        GroupPlanDayProductionInfoHelper find = plannedProductionList.stream().filter(plannedProduction -> key.equals(plannedProduction.getDuplicateKey())).findFirst().orElse(null);
        if (null == find) {
            plannedProductionList.add(singleDayProductionInfo);
            return;
        }
    }

    /**
     * 获取分组的日排产汇总信息集合
     * 当前为胎胚种类信息和硫化组信息
     *
     * @return
     */
    public List<GroupDayProductionSummaryHelper> getGroupProductionSummary() {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyList();
        }
        List<GroupDayProductionSummaryHelper> summaryList = new ArrayList<>(dayProductionLimitInfo.size());
        dayProductionLimitInfo.forEach((productionDay, lhProductionLimit) -> {
            GroupDayProductionSummaryHelper daySummary = GroupDayProductionSummaryHelper.buildEmpty(groupName, productionDay);
            summaryList.add(daySummary);
            //胎胚种类数信息
            Set<String> embryoCodeSet = lhProductionLimit.getProductionEmbryoCodeSet();
            daySummary.setEmbryoCodeSet(embryoCodeSet);
            daySummary.setEmbryoCount(embryoCodeSet.size());
            //硫化分组信息
            daySummary.setLhGroupCount(lhProductionLimit.getUsedLhMachineCount());
        });
        return summaryList;
    }

    /**
     * 根据硫化配比，计算成型单日最小产能
     * = 最小日硫化量(单模) * 2 * lhRatio
     *
     * @param lhRatio 成型硫化配比
     * @return
     */
    public Integer getDayMinCapacityByLhRatio(Integer lhRatio) {
        if (null == lhRatio || lhRatio <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        BigDecimal minCapacity = getDayCapacityByLhRatio(lhRatio);
        if (null == minCapacity) {
            return BigDecimal.ZERO.intValue();
        }
        return minCapacity.setScale(BigDecimal.ZERO.intValue(), RoundingMode.DOWN).intValue();
    }

    /**
     * 获取当前sku已排产日期集合
     *
     * @param materialDesc 物料描述
     * @return
     */
    public List<Integer> getProductionDaySetBySku(String materialDesc) {
        if (StringUtils.isEmpty(materialDesc)) {
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyList();
        }
        List<Integer> productionDayList = new ArrayList<>(64);
        dayProductionLimitInfo.forEach((day, dayProductionLimit) -> {
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyInfo = dayProductionLimit.getProductionSkuQtyInfo();
            if (CollectionUtils.isEmpty(productionSkuQtyInfo)) {
                return;
            }
            if (productionSkuQtyInfo.containsKey(materialDesc)) {
                productionDayList.add(day);
            }
        });
        return productionDayList;
    }

    /**
     * 按结构分组硫化配比
     *
     * @param structureLhRatioList 成型硫化配比集合
     * @return
     */
    private static Map<String, List<MonthPlanStructureLhRatioVo>> getStructureGroupInfo(List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        if (CollectionUtils.isEmpty(structureLhRatioList)) {
            return Collections.emptyMap();
        }
        return structureLhRatioList.stream().collect(Collectors.groupingBy(MonthPlanStructureLhRatioVo::getStructureName));
    }

    /**
     * 按结构提取最小硫化配比信息
     *
     * @param structureGroupMap 结构配比分组
     * @return
     */
    private static Map<String, MonthPlanStructureLhRatioVo> getMinLhRatioMap(Map<String, List<MonthPlanStructureLhRatioVo>> structureGroupMap) {
        if (CollectionUtils.isEmpty(structureGroupMap)) {
            return Collections.emptyMap();
        }
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = new HashMap<>();
        structureGroupMap.forEach((structureName, ratioList) -> {
            if (CollectionUtils.isEmpty(ratioList)) {
                return;
            }
            ratioList.sort(Comparator.comparing(MonthPlanStructureLhRatioVo::getLhMachineMaxQty));
            minLhRatioMap.put(structureName, ratioList.get(BigDecimal.ZERO.intValue()));
        });
        return minLhRatioMap;
    }

    /**
     * 构建结构分组基础信息
     * 结构对应的计划，以及结构下所有的硫化配比信息
     *
     * @param context            排产上下文
     * @param allRequirePlanList 所有排产计划
     * @param structureGroupMap  结构硫化配比配置信息
     */
    private static Map<String, ProductionPlanGroupInfo> buildGroupPlanInfoMap(Context context, List<MonthPlanProductionRequirePlanVo> allRequirePlanList, Map<String, List<MonthPlanStructureLhRatioVo>> structureGroupMap) {
        if (CollectionUtils.isEmpty(allRequirePlanList)) {
            return Collections.emptyMap();
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = allRequirePlanList.stream().filter(singlePlan -> StringUtils.isNotBlank(singlePlan.getStructureName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyMap();
        }
        //TBR-按结构名分组排产计划
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupPlanMap = effectiveList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getStructureName));
        Map<String, ProductionPlanGroupInfo> groupInfoMap = new HashMap<>(groupPlanMap.size());
        groupPlanMap.forEach((structureName, planList) -> {
            ProductionPlanGroupInfo groupInfo = ProductionPlanGroupInfo.createInitByGroupList(context, structureName, context.getProductType(), planList);
            //是否零度结构
            List<MonthPlanProductionRequirePlanVo> isZeroRackList = planList.stream().filter(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getIsZeroRack())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(isZeroRackList)) {
                groupInfo.setIsZero(YesOrNoEnum.YES.getCode());
            }
            //设置对应的硫化配比信息：分不同机型有不同配比
            List<MonthPlanStructureLhRatioVo> cxLhRatioList = structureGroupMap.get(structureName);
            if (CollectionUtils.isEmpty(cxLhRatioList)) {
                groupInfo.setCxMachineLhRationMap(Collections.emptyMap());
            } else {
                Map<String, MonthPlanStructureLhRatioVo> allCxLhRatioMap = cxLhRatioList.stream().collect(Collectors.toMap(MonthPlanStructureLhRatioVo::getCxMachineTypeCode, Function.identity(), (before, after) -> after));
                groupInfo.setCxMachineLhRationMap(allCxLhRatioMap);
            }
            groupInfoMap.put(structureName, groupInfo);
        });
        return groupInfoMap;
    }

    /**
     * 计算结构的有效需求量，需要根据模具信息
     *
     * @param context      排产上下文
     * @param groupInfoMap 结构分组计划
     */
    private static void calculateEffectiveByMouldMaxCapacity(Context context, Map<String, ProductionPlanGroupInfo> groupInfoMap) {
        //得到结构主花纹下最大可用模具数-按物料描述分组取最大
        Map<String, Integer> structureMainPatternMaxMouldGroup = getStructureMainPatternMaxMouldNumber(context);
        /**
         * 1、剔除不可排产的
         * 2、剔除超出模具产能部分
         * 3、设置结构分组的总的有效需求和最小日硫化量
         */
        //按结构+主花纹分组
        String groupKeyFormat = "%s|*|%s";
        groupInfoMap.forEach((structureName, groupInfo) -> {
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.intValue());
                return;
            }
            //剔除不排产的计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupPlanData.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getProductionFlag())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionPlanList)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.intValue());
                return;
            }
            //最小日硫化量
            Integer minDayLhCapacity = productionPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).min().getAsInt();
            groupInfo.setMinLhDayCapacityQty(minDayLhCapacity);
            List<Integer> mainPatternEffectiveQty = new ArrayList<>();
            //按主花纹分组需求
            Map<String, List<MonthPlanProductionRequirePlanVo>> mainPatternGroup = productionPlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMainPattern));
            mainPatternGroup.forEach((mainPattern, groupPlanList) -> {
                String groupKey = String.format(groupKeyFormat, structureName, mainPattern);
                Integer maxMouldNumber = structureMainPatternMaxMouldGroup.get(groupKey);
                if (null == maxMouldNumber) {
                    maxMouldNumber = BigDecimal.ZERO.intValue();
                }
                Integer lhMachineCount = maxMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                Integer sumPlanQty = groupPlanList.stream().mapToInt(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
                Integer maxMouldCapacity = lhMachineCount * minDayLhCapacity * ProductionConstant.DOUBLE_MOULD_PRODUCTION * context.getMaxProductionDays();
                log.info(TbrProductionGroupLogRecorder.addGroupCalculateCapacityLog(context, groupKey, sumPlanQty, maxMouldNumber, maxMouldCapacity));
                mainPatternEffectiveQty.add(Math.min(sumPlanQty, maxMouldCapacity));
            });
            Integer sumPlanQty = mainPatternEffectiveQty.stream().mapToInt(Integer::intValue).sum();
            log.info(TbrProductionGroupLogRecorder.addGroupCalculateCapacityLog(context, structureName, sumPlanQty));
            groupInfo.setSumPlanQty(sumPlanQty);
        });
    }

    /**
     * 获取 结构 + 主花纹下可使用的模具最大数量
     *
     * @param context
     * @return
     */
    private static Map<String, Integer> getStructureMainPatternMaxMouldNumber(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanProductMouldInfoVo> allMouldRelation = productionContext.getBaseDataContainer().getSkuMouldRelationMap().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allMouldRelation)) {
            return Collections.emptyMap();
        }
        //按结构+主花纹分组模具信息
        Map<String, List<MonthPlanProductMouldInfoVo>> structureMainPatternGroup = allMouldRelation.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getStructureNameAndMainPattern));
        Map<String, Integer> structureAndMainPatternMap = new HashMap<>();
        Map<String, MouldAllocationInfoVo> structureMainPatternAllocationLimit = productionContext.getBaseDataContainer().getGroupMainPatternAllocationLimitMap();
        structureMainPatternGroup.forEach((structureAndMainPattern, mouldRelationList) -> {
            Integer maxMouldNumber = BigDecimal.ZERO.intValue();
            if (CollectionUtils.isEmpty(mouldRelationList)) {
                structureAndMainPatternMap.put(structureAndMainPattern, maxMouldNumber);
                return;
            }
            Map<String, Long> materialGroup = mouldRelationList.stream().collect(Collectors.groupingBy(MonthPlanProductMouldInfoVo::getMaterialDesc, Collectors.counting()));
            List<Long> mouldNumberList = new ArrayList<>(materialGroup.values());
            mouldNumberList.sort(Comparator.comparing(Long::valueOf, Comparator.reverseOrder()));
            maxMouldNumber = mouldNumberList.get(BigDecimal.ZERO.intValue()).intValue();
            //增加与分配比例的比较
            if (!structureMainPatternAllocationLimit.containsKey(structureAndMainPattern)) {
                structureAndMainPatternMap.put(structureAndMainPattern, maxMouldNumber);
                return;
            }
            //分配比例与最大数，二者取最小
            Integer limitNumber = structureMainPatternAllocationLimit.get(structureAndMainPattern).getAllocationQty();
            log.info(TbrProductionGroupLogRecorder.addGroupMainPatternMaxMouldNumberLog(context, structureAndMainPattern, limitNumber, maxMouldNumber));
            maxMouldNumber = Math.min(maxMouldNumber, limitNumber);
            structureAndMainPatternMap.put(structureAndMainPattern, maxMouldNumber);
        });
        return structureAndMainPatternMap;
    }

    /**
     * 根据硫化配比，计算成型单日产能量
     * = 最小日硫化量(单模) * 2 * lhRatio
     *
     * @param lhRatio
     * @return
     */
    private BigDecimal getDayCapacityByLhRatio(Integer lhRatio) {
        return BigDecimal.valueOf(minLhDayCapacityQty).multiply(BigDecimal.valueOf(ProductionConstant.DOUBLE_MOULD_PRODUCTION)).multiply(BigDecimal.valueOf(lhRatio));
    }

    /**
     * 获取当前排产日前一日排产信息
     *
     * @param currentLimit 当前排产限制信息
     * @return
     */
    private GroupPlanCxLhCapacityLimitHelper getPreviousDayInfo(GroupPlanCxLhCapacityLimitHelper currentLimit) {
        if (null == currentLimit) {
            return null;
        }
        Integer productionDay = currentLimit.getDay();
        if (null == productionDay || !dayProductionLimitInfo.containsKey(productionDay)) {
            return null;
        }
        //获取所有排产日，并按排产日排序
        List<Integer> productionDayList = new ArrayList<>(dayProductionLimitInfo.keySet());
        Collections.sort(productionDayList);
        //获取各排产日所在位置
        Map<Integer, Integer> productionDayPositionMap = getProductionDayPositionInfo();
        //得到排产日所在位置
        int productionPosition = productionDayPositionMap.get(productionDay);
        if (productionPosition == BigDecimal.ZERO.intValue()) {
            return null;
        }
        //得到前一个排产日-根据位置
        Integer previousDay = productionDayList.get(productionPosition - BigDecimal.ONE.intValue());
        return dayProductionLimitInfo.get(previousDay);
    }

    /**
     * 得到当前排产信息后一日的排产信息
     *
     * @param currentLimit 当前排产信息
     * @return
     */
    private GroupPlanCxLhCapacityLimitHelper getNextDayInfo(GroupPlanCxLhCapacityLimitHelper currentLimit) {
        if (null == currentLimit) {
            return null;
        }
        Integer productionDay = currentLimit.getDay();
        if (null == productionDay || !dayProductionLimitInfo.containsKey(productionDay)) {
            return null;
        }
        //获取所有排产日，并按排产日排序
        List<Integer> productionDayList = new ArrayList<>(dayProductionLimitInfo.keySet());
        Collections.sort(productionDayList);
        //获取各排产日所在位置
        Map<Integer, Integer> productionDayPositionMap = getProductionDayPositionInfo();
        //得到排产日所在位置
        int productionPosition = productionDayPositionMap.get(productionDay);
        int maxIndex = productionDayList.size() - BigDecimal.ONE.intValue();
        if (productionPosition == maxIndex) {
            return null;
        }
        //得到后一个排产日-根据位置
        Integer nextDay = productionDayList.get(productionPosition + BigDecimal.ONE.intValue());
        return dayProductionLimitInfo.get(nextDay);
    }

    /**
     * 判断在productionDay是否可排产productionPlan
     * 胎胚种类数是否达到限制，硫化配比是否达到限制
     *
     * @param productionPlan 排产计划
     * @param productionDay  排产日
     * @return
     */
    private boolean isAddSkuProductionByOneLhMachine(MonthPlanProductionRequirePlanVo productionPlan, Integer productionDay) {
        GroupPlanCxLhCapacityLimitHelper limit = dayProductionLimitInfo.get(productionDay);
        if (null == limit) {
            return false;
        }
        Set<String> plannedEmbryoCodeSet = limit.getProductionEmbryoCodeSet();
        if (plannedEmbryoCodeSet.contains(productionPlan.getEmbryoCode())) {
            if (limit.getUsedLhMachineCount() >= limit.getMaxLhMachineCount()) {
                return false;
            }
            return true;
        }
        if (plannedEmbryoCodeSet.size() >= limit.getMaxEmbryoCodeCount()) {
            return false;
        }
        return true;
    }

    /**
     * 判断上机日是否为分组计划正式排产的最早排产日
     *
     * @return
     */
    private boolean isGroupStartDayByFormalProduction(Integer machineDay) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return true;
        }
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        dayLimitList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        return machineDay.equals(dayLimitList.get(BigDecimal.ZERO.intValue()).getDay());
    }

    /**
     * 获取前一天
     *
     * @param currentDay
     * @return
     */
    public Integer getPreviousDay(Integer currentDay) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return currentDay;
        }
        Integer previousDay = currentDay - BigDecimal.ONE.intValue();
        Set<Integer> productionDaySet = dayProductionLimitInfo.keySet();
        if (productionDaySet.contains(previousDay)) {
            return previousDay;
        }
        return getPreviousDay(previousDay);
    }

    /**
     * 获取各排产日所处位置
     * 返还排产日及排产位置index
     *
     * @return
     */
    private Map<Integer, Integer> getProductionDayPositionInfo() {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return Collections.emptyMap();
        }
        //获取所有排产日，并按排产日排序
        List<Integer> productionDayList = new ArrayList<>(dayProductionLimitInfo.keySet());
        Collections.sort(productionDayList);
        //获取各排产日所在位置
        Map<Integer, Integer> productionDayPositionMap = new HashMap<>();
        int positionIndex = BigDecimal.ZERO.intValue();
        for (Integer singleProductionDay : productionDayList) {
            productionDayPositionMap.put(singleProductionDay, positionIndex);
            positionIndex = positionIndex + BigDecimal.ONE.intValue();
        }
        return productionDayPositionMap;
    }

    /**
     * 根据有效在产机台分配信息，构建所有可排产日信息
     *
     * @param context                     排产上下文
     * @param continueCxMachineAllocation 分组下在产机台分配信息
     * @return
     */
    private Set<Integer> getAllContinueProductionDaySet(Context context, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return Collections.emptySet();
        }
        Set<Integer> allProductionDaySet = new HashSet<>();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        continueCxMachineAllocation.forEach(singleAllocation -> {
            String cxMachineCode = singleAllocation.getCxMachineCode();
            if (StringUtils.isBlank(cxMachineCode)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            for (Integer productionDay = singleAllocation.getStartDay(); productionDay <= singleAllocation.getEndDay(); productionDay++) {
                if (!cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                    allProductionDaySet.add(productionDay);
                }
            }
        });
        if (CollectionUtils.isEmpty(allProductionDaySet)) {
            return Collections.emptySet();
        }
        return allProductionDaySet;
    }

    /**
     * 获取续作分组在产机台在排产日的配置信息
     *
     * @param context                     排产上下文
     * @param productionDay               排产日
     * @param continueCxMachineAllocation 分组下所有在产机台配置信息
     * @return
     */
    private List<CxMachineAllocationPlanHelper> getContinueProductionConfiguration(Context context, Integer productionDay, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (null == productionDay || CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<CxMachineAllocationPlanHelper> effectiveList = new ArrayList<>();
        continueCxMachineAllocation.forEach(singleAllocation -> {
            String cxMachineCode = singleAllocation.getCxMachineCode();
            if (StringUtils.isBlank(cxMachineCode)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            //停产日跳过
            if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                return;
            }
            if (productionDay >= singleAllocation.getStartDay() && productionDay <= singleAllocation.getEndDay()) {
                effectiveList.add(singleAllocation);
            }
        });
        return effectiveList;
    }

    /**
     * 根据有效的分组下分配信息，构建所有可排产日信息
     *
     * @param context                排产上下文
     * @param groupAllAllocationList 分组下的机台分配信息
     * @return
     */
    private Set<Integer> getAllProductionDaySet(Context context, List<MpStructureAllocation> groupAllAllocationList) {
        if (CollectionUtils.isEmpty(groupAllAllocationList)) {
            return Collections.emptySet();
        }
        Set<Integer> allProductionDaySet = new HashSet<>();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        groupAllAllocationList.forEach(singleAllocation -> {
            String cxMachineCode = singleAllocation.getCxMachineCode();
            if (StringUtils.isBlank(cxMachineCode)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            for (Integer productionDay = singleAllocation.getBeginDay(); productionDay <= singleAllocation.getEndDay(); productionDay++) {
                if (!cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                    allProductionDaySet.add(productionDay);
                }
            }
        });
        if (CollectionUtils.isEmpty(allProductionDaySet)) {
            return Collections.emptySet();
        }
        return allProductionDaySet;
    }

    /**
     * 获取在排产日的配置信息
     *
     * @param context                排产上下文
     * @param productionDay          排产日
     * @param groupAllAllocationList 分组下所有配置信息
     * @return
     */
    private List<MpStructureAllocation> getProductionConfiguration(Context context, Integer productionDay, List<MpStructureAllocation> groupAllAllocationList) {
        if (null == productionDay || CollectionUtils.isEmpty(groupAllAllocationList)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        List<MpStructureAllocation> effectiveList = new ArrayList<>();
        groupAllAllocationList.forEach(singleAllocation -> {
            String cxMachineCode = singleAllocation.getCxMachineCode();
            if (StringUtils.isBlank(cxMachineCode)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            //停产日跳过
            if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                return;
            }
            if (productionDay >= singleAllocation.getBeginDay() && productionDay <= singleAllocation.getEndDay()) {
                effectiveList.add(singleAllocation);
            }
        });
        return effectiveList;
    }

    /**
     * 设置估算的机台数和天数为零
     */
    private void setAllocationZero() {
        needCxCapacityMachineCount = BigDecimal.ZERO;
        theoryDays = BigDecimal.ZERO.intValue();
        leftOverNeedAllocationDays = BigDecimal.ZERO.intValue();
        isAllocationFinish = YesOrNoEnum.YES.getValue();
    }
}
