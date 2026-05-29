package com.zlt.aps.mp.engine.domain.dto;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.capacity.MpMonthPlanDailyCapacityLimit;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.*;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.FormalRoundEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.ConclusionLhMachineHandler;
import com.zlt.aps.mp.engine.handler.ContinuousProductionDayHandler;
import com.zlt.aps.mp.engine.handler.SkuProductionSnapshot;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.utils.NoProductionReasonUtils;
import com.zlt.aps.utils.ProductSpecificationsUtils;
import com.zlt.common.utils.PubUtil;
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
     * 最小高优先级硫化机台数（参数配置）
     */
    private Integer minHeightPriorityLhMachineCount;
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
     * 20260427+ 分组指定的固定1~固定3集合
     */
    private Set<String> priorityFixedCxMachineSet;
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
     * 最大周期排产量的值
     */
    private Integer maxCycleQty;

    /**
     * 结构需求与机台产能差异天数，用于最优匹配
     * sandy+ 2026.3.28
     */
    private Integer diffStructureAndMachineDays;
    /**
     * 机台反选结构，实际可分配天数
     * 20260329
     * 只在机台反选场景使用，因不再产能覆盖，导致不能使用需求剩余天数
     */
    private Integer machineReverseAllocationDays;
    /**
     * 排产-成型硫化产能限制
     * 包含 最大胎胚数
     * 最大硫化机台数
     * 实单最低硫化机台数
     * key=day : value=日成型硫化产能限制实例
     */
    private Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo;

    /**
     * 日产能限制Map
     * 用于统计每日已排硫化机台数、胎胚种类数等信息 sandy+2026.3.21
     */
    private Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap;

    /**
     * 分配的成型机台
     */
    private Set<String> allocationCxMachineCodeSet;
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
     * OEM是否参与结构优先级竞争
     */
    private Boolean oemJoinStructurePriority;

    /**
     * OEM品牌集合
     */
    private Set<String> oemBrandSet;

    /**
     * 是否需要提前收尾处理
     * 特殊材料的结构进行拉量时，不能进行提前收尾处理
     */
    private boolean hasBeforeConclusionHandler;
    /**
     * 根据模具数计算的硫化机台数（用于特殊材料粗算天数）
     */
    private Integer minLhMachineCountByMould;
    /**
     * 高优先级需求量总值
     */
    private Integer sumHeightRequireQty;
    /**
     * 20260523+ 新成型机分配排产前Sku排产量备份
     */
    private Map<String, SkuProductionSnapshot> beforeProductionSnapshotMap;
    /**
     * 构建初始化分组信息对象
     * TBR 结构 PCR 英寸
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
        groupInfo.setPriorityFixedCxMachineSet(new HashSet<>());
        groupInfo.setIsLatestSpecialMaterial(false);
        groupInfo.setHasBeforeConclusionHandler(true);

        //默认不含特殊材料信息
        groupInfo.setEmbryoSpecialMaterialInfoMap(Collections.emptyMap());
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return groupInfo;
        }
        //是否零度结构的处理
        boolean isHasZeroRack = groupPlanData.stream().anyMatch(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getIsZeroRack()));
        if (isHasZeroRack) {
            groupInfo.setIsZero(YesOrNoEnum.YES.getCode());
        }
        // 判断如果是特殊结构，需要判断是否最后一个结构
        TbrProductionContext productionContext = (TbrProductionContext) context;
        // OEM是否参与结构优先级竞争 sandy+ 2026.4.3
        groupInfo.setOemJoinStructurePriority(productionContext.getBaseDataContainer().getParamConfiguration().getOemJoinStructurePriority());
        // OEM品牌集合
        groupInfo.setOemBrandSet(productionContext.getBaseDataContainer().getParamConfiguration().getOemBrandConfig());
        // 胎胚与特殊材料对应关系清单
        Map<String, Map<String, BigDecimal>> embryoSpecialMaterialInfoMap = productionContext.getBaseDataContainer().getEmbryoSpecialMaterialInfoMap();
        // 本结构涉及的特殊材料清单
        Map<String, BigDecimal> materialMap = new HashMap<>();
        // 遍历需求计划，取有特殊材料清单的记录作为本结构的特殊材料清单（正常都一样，防止数据有异常）
        for (MonthPlanProductionRequirePlanVo planVo : groupPlanData) {
            Map<String, BigDecimal> skuMaterialMap = embryoSpecialMaterialInfoMap.get(planVo.getEmbryoCode());
            if (!CollectionUtils.isEmpty(skuMaterialMap)) {
                materialMap.putAll(skuMaterialMap);
                break;
            }
        }
        // 高优先级最小排产机台
        Integer minHeightPriorityLhMachineCount = productionContext.getBaseDataContainer().getParamConfiguration().getMinHeightPriorityLhMachineCount();
        groupInfo.setMinHeightPriorityLhMachineCount(minHeightPriorityLhMachineCount);
        if (CollectionUtils.isEmpty(materialMap)) {
            return groupInfo;
        }
        groupInfo.setEmbryoSpecialMaterialInfoMap(materialMap);
        // 刷新上下文的特殊材料结构关系表
        productionContext.updateSpecialMaterialStructureRelationMap(groupInfo);

        List<String> materialCodeList = groupPlanData.stream()
                .map(MonthPlanProductionRequirePlanVo::getMaterialDesc).distinct().collect(Collectors.toList());
        Set<String> mouldCodeSet = new HashSet<>();
        for (String materialCode : materialCodeList) {
            List<MonthPlanProductMouldInfoVo> mouldList = productionContext.getBaseDataContainer()
                    .getSkuMouldRelationMap().get(materialCode);
            if (CollectionUtils.isEmpty(mouldList)) {
                continue;
            }

            Set<String> newMouldCodeSet = mouldList.stream().map(MonthPlanProductMouldInfoVo::getMouldCode)
                    .filter(StringUtils::isNotEmpty).distinct().collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(newMouldCodeSet)) {
                continue;
            }
            mouldCodeSet.addAll(newMouldCodeSet);
        }
        // 模具数换算成硫化机台数
        groupInfo.minLhMachineCountByMould = mouldCodeSet.size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        return groupInfo;
    }

    /**
     * 检测特殊材料数据是否正常
     * 同组下各个Sku对应的胎胚使用的特殊材料要一致
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
     * 获取结构最短上机天数
     * 如果不是特殊材料的结构则为参数SYS0204010
     * 特殊材料结构的上机天数为1
     *
     * @param productionContext 排产上下文
     * @return
     */
    public Integer getMinAllocationDays(TbrProductionContext productionContext) {
        if (isSpecialMaterial()) {
            return BigDecimal.ONE.intValue();
        }
        return productionContext.getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
    }

    /**
     * 获取一天的浮动余量
     * = 1 * Sku日硫化量(min(所有可排产Sku日硫化量)) * 硫化机台数(min(分组所有成型硫化配比的最大硫化机台数))
     *
     * @return
     */
    public Integer getThreshold() {
        return Math.min(minLhMachineCount, minLhMachineCountByMould) * getDayCapacityBySingleLh();
    }

    /**
     * 获取高优先级需求量的占比
     *
     * @return
     */
    public BigDecimal getHeightRequireRatio() {
        if (null == sumHeightRequireQty || null == sumPlanQty) {
            return BigDecimal.ZERO;
        }
        if (sumPlanQty <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(sumHeightRequireQty).divide(BigDecimal.valueOf(sumPlanQty), 3, RoundingMode.HALF_UP);
    }

    /**
     * 获取一天的最大浮动余量
     * = 1 * Sku日硫化量(min(所有可排产Sku日硫化量)) * 硫化机台数(min(分组所有成型硫化配比的最大硫化机台数))
     *
     * @return
     */
    public Integer getMaxThreshold() {
        return Math.max(minLhMachineCount, minLhMachineCountByMould) * getDayCapacityBySingleLh();
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
        String noReachMinProductionDaysReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_MIN_CX_CAPACITY_WHOLE_STRUCTURE_NAME, theoryDays, minProductionDays);
        theoryDays = BigDecimal.ZERO.intValue();
        leftOverNeedAllocationDays = BigDecimal.ZERO.intValue();
        needCxCapacityMachineCount = BigDecimal.ZERO;
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
     *
     * @param minLhMachineCount                 最低硫化机台数
     * @param realAllocationDayBeforeConclusion 高于最低硫化机台数的天数
     * @param minProductionDays                 最低上机天数
     */
    public void setNoProductionLowMinLhMachineNoReachMinProductionDays(Integer minLhMachineCount, Integer realAllocationDayBeforeConclusion, Integer minProductionDays) {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        String lowMinLhMachineNoReachMinProductionDaysReason = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_LOW_MIN_LH_MACHINE_COUNT_WHOLE_STRUCTURE_NAME, realAllocationDayBeforeConclusion, minProductionDays);
        groupPlanData.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(lowMinLhMachineNoReachMinProductionDaysReason));
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
        //20260516+ 防止中间断开导致续作时间不对
        Set<Integer> dayProductionLimitSet = dayProductionLimitInfo.keySet();
        Set<Integer> stopDayInfo = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Set<Integer> effectiveContinueProductionDay = ContinuousProductionDayHandler.getEarliestContinuousRangeResultExcludeStop(dayProductionLimitSet, stopDayInfo);
        if (CollectionUtils.isEmpty(effectiveContinueProductionDay)) {
            return null;
        }
        List<Integer> productionDayList = new ArrayList<>(effectiveContinueProductionDay);
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
     * 结构延长一天收尾的处理
     * 将剩余分配天数 -1
     */
    public void timeExtensionOneDayConclusion() {
        if (null == leftOverNeedAllocationDays) {
            return;
        }
        leftOverNeedAllocationDays = leftOverNeedAllocationDays - BigDecimal.ONE.intValue();
    }

    /**
     * 20260323 结构收尾需要将以分配天数扣减
     *
     * @param deductionDays 收尾的天数
     */
    public void deductionAllocationDays(Integer deductionDays) {
        if (null == deductionDays || deductionDays <= BigDecimal.ZERO.intValue()) {
            return;
        }
        if (null == leftOverNeedAllocationDays) {
            return;
        }
        leftOverNeedAllocationDays = leftOverNeedAllocationDays + deductionDays;
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
     * 获取结构下，最早续作收尾的硫化组信息
     *
     * @param context
     * @param continueSkuMap
     * @param excludeDaySet
     * @return
     */
    public EarliestConclusionLhGroupHelper getEarliestConclusionLhInfoByContinueSku(Context context, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Set<Integer> excludeDaySet) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(continueSkuMap)) {
            return null;
        }
        //重算使用硫化组机台数
        reCalcMpDailyCapacityLimit(context);
        //得到续作最大硫化组可使用的模具数
        Integer sumMouldNumber = continueSkuMap.values().stream().mapToInt(CxContinueSkuInfoHelper::getMouldNumber).sum();
        Integer maxLhMachineCount = sumMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        List<GroupPlanCxLhCapacityLimitHelper> hasAddContinueSkuList = dayLimitList.stream().filter(dayLimit -> {
            Integer day = dayLimit.getDay();
            if (excludeDaySet.contains(day)) {
                return false;
            }
            MpDailyCapacityLimitVo dayUsedDetail = dailyCapacityLimitVoMap.get(day);
            if (null == dayUsedDetail) {
                return true;
            }
            Integer realUsedLhMachines = Optional.ofNullable(dayUsedDetail.getUsedLhMachines()).orElse(BigDecimal.ZERO.intValue());
            return realUsedLhMachines < maxLhMachineCount;
        }).collect(Collectors.toList());
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
        Integer canAddCount = previousLimit.getReleaseLhMachineCount(context, selectedDayLimit);
        if (canAddCount <= BigDecimal.ZERO.intValue()) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        SkuDayProductionInfoHelper previousSku = selectedDayLimit.getEarliestConclusionSkuInfo(context, previousLimit, canAddCount);
        if (null == previousSku) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        return EarliestConclusionLhGroupHelper.createEarliestConclusionLhGroup(conclusionDay, endDay, previousSku, true);
    }

    /**
     * 获取结构下，最早收尾的硫化信息
     *
     * @param context     排产上下文
     * @param round       轮次
     * @param excludeDays 排除的收尾时间点
     * @return
     */
    public EarliestConclusionLhGroupHelper getEarliestConclusionLhInfo(Context context, FormalRoundEnum round, Set<Integer> excludeDays) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return null;
        }
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取可释放机台的排产日信息
        List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList = dayLimitList.stream().filter(dayLimit -> {
            //前一日排产情况
            GroupPlanCxLhCapacityLimitHelper previousDayLimit = getPreviousDayInfo(dayLimit);
            //后一日排产情况
            GroupPlanCxLhCapacityLimitHelper nexDayLimit = getNextDayInfo(dayLimit);
            return !dayLimit.isReachLimitByMouldNumber(context, round, previousDayLimit, nexDayLimit);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        //得到最早的收尾点
        SelectRangeLhMachineInfo selectedRange = getRangeInfo(context, hasAddSkuList, excludeDays);
        if (null == selectedRange) {
            return null;
        }
        GroupPlanCxLhCapacityLimitHelper selectedDayLimit = selectedRange.getStartDayLimit();
        GroupPlanCxLhCapacityLimitHelper endDayLimit = selectedRange.getEndDayLimit();
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
        //可释放的机台
        Integer canAddCount = previousLimit.getReleaseLhMachineCount(context, selectedDayLimit);
        if (canAddCount <= BigDecimal.ZERO.intValue()) {
            return EarliestConclusionLhGroupHelper.createEmptyEarliestConclusionLhGroup(conclusionDay, endDay);
        }
        SkuDayProductionInfoHelper previousSku = selectedDayLimit.getEarliestConclusionSkuInfo(context, previousLimit, canAddCount);
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
        String mouldSetCode = selectedMould.get(BigDecimal.ZERO.intValue()).getMouldSetCode();
        //得到有效排产范围：考虑模壳、胶囊卡盘、模具分配比例等
        MouldProductionDayLimitHelper limitHelper = getProductionDayLimitInfo(context, addSkuInfo, preSelected, selectedMould);
        Set<Integer> effectiveRangeSet = limitHelper.getProductionDaySet();
        if (CollectionUtils.isEmpty(effectiveRangeSet)) {
            TbrMouldProductionLogRecorder.addLhGroupSkuLimitLog(context, groupName, onLineMachineInfo, addSkuInfo, mouldSetCode, limitHelper.getLimitType());
            preSelected.updateProductionDateRange(null, null);
            return;
        }
        List<Integer> sortList = new ArrayList<>(effectiveRangeSet);
        Collections.sort(sortList);
        int size = sortList.size();
        Integer newClosingDay = sortList.get(BigDecimal.ZERO.intValue());
        Integer newEndDay = sortList.get(size - BigDecimal.ONE.intValue());
        preSelected.updateProductionDateRange(newClosingDay, newEndDay);
    }

    /**
     * 获取排产限制信息
     * 得到有效排产日范围集合
     *
     * @param context       排产上下文
     * @param addSkuInfo    排产的Sku信息
     * @param preSelected   收尾硫化组信息
     * @param selectedMould 选中的模具
     * @return
     */
    public Set<Integer> getMouldProductionLimitInfo(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, EarliestConclusionLhGroupHelper preSelected, List<ProductionMouldInfoVo> selectedMould) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || null == preSelected) {
            return Collections.emptySet();
        }
        MouldProductionDayLimitHelper limitHelper = getProductionDayLimitInfo(context, addSkuInfo, preSelected, selectedMould);
        Set<Integer> effectiveRangeSet = limitHelper.getProductionDaySet();
        if (CollectionUtils.isEmpty(effectiveRangeSet)) {
            return Collections.emptySet();
        }
        return effectiveRangeSet;
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
     * 判断在productionDay是否可排产productionPlan
     * 是否达到胎胚种类数限制
     *
     * @param productionPlan 排产的计划
     * @param productionDay  排产日
     * @return
     */
    public boolean isLimitEmbryoCodeCount(MonthPlanProductionRequirePlanVo productionPlan, Integer productionDay) {
        GroupPlanCxLhCapacityLimitHelper limit = dayProductionLimitInfo.get(productionDay);
        if (null == limit) {
            return true;
        }
        //20260324 因已经将硫化配比提前计算，故而只需判断胎胚即可
        Set<String> plannedEmbryoCodeSet = Optional.ofNullable(limit.getProductionEmbryoCodeSet()).orElse(Collections.emptySet());
        if (plannedEmbryoCodeSet.contains(productionPlan.getEmbryoCode())) {
            return false;
        }
        return plannedEmbryoCodeSet.size() >= limit.getMaxEmbryoCodeCount();
    }

    /**
     * 一轮排产完毕后，将还有计划的Sku重新标记参与排产
     */
    public void afterProductionResetThisRound() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(single -> single.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return;
        }
        hasProductionList.forEach(singlePlan -> singlePlan.setIsThisRound(YesOrNoEnum.YES.getValue()));
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
        return getBoostReplenishmentQuota();
    }

    /**
     * 获取还需要进行成型机台小于最短上机天数场景
     * 还有实单需求的结构
     *
     * @return
     */
    public Integer getBoostReplenishmentQuota() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.intValue();
        }
        if (null == leftOverNeedAllocationDays) {
            return BigDecimal.ZERO.intValue();
        }
        return leftOverNeedAllocationDays;
    }

    /**
     * 根据续作类型，获取续作可排产信息
     * 1、同规格同花纹
     * 2、同生胎同模具
     *
     * @param productionStage           排产阶段
     * @param continueType              续作类型
     * @param materialDesc              续作Sku
     * @param shareMouldMaterialDescSet 共用模具的物料集合
     * @param continueProductInfoHelper 续作Sku详细信息
     * @return
     */
    public List<MonthPlanProductionRequirePlanVo> getContinueListByType(ProductionStageEnum productionStage, ContinueTypeEnum continueType, String materialDesc, Set<String> shareMouldMaterialDescSet, CxContinueSkuInfoHelper continueProductInfoHelper) {
        if (ContinueTypeEnum.SAME_SPECIFICATIONS_PATTERN == continueType) {
            return getSameSpecificationsAndPatternPlan(materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
        }
        if (ContinueTypeEnum.SAME_EMBRYO_CODE_SHARE_MOULD == continueType) {
            return getSameEmbryoCodeAndMouldPlan(productionStage, materialDesc, shareMouldMaterialDescSet, continueProductInfoHelper);
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
     * @param productionStage           排产阶段
     * @param materialDesc              前规格
     * @param shareMouldMaterialDescSet 共用模具的sku集合
     * @param continueProductInfoHelper 前规格详情信息
     */
    public List<MonthPlanProductionRequirePlanVo> getSameEmbryoCodeAndMouldPlan(ProductionStageEnum productionStage, String materialDesc, Set<String> shareMouldMaterialDescSet, CxContinueSkuInfoHelper continueProductInfoHelper) {
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
            //20260414+ 共用模具都算续作，不再区分排产阶段
            sameEmbryoCodeAndMouldList.add(groupPlan);
//            //20260326 不是测算阶段，共用模具都算续作
//            if (ProductionStageEnum.CALCULATION_STAGE != productionStage) {
//                sameEmbryoCodeAndMouldList.add(groupPlan);
//                return;
//            }
//            //测算阶段-同生胎
//            if (groupPlan.isSameEmbryoCode(continueProductInfoHelper)) {
//                sameEmbryoCodeAndMouldList.add(groupPlan);
//            }
        });
        return sameEmbryoCodeAndMouldList;
    }

    /**
     * 结构需求与机台产能差异天数的绝对值
     *
     * @return
     */
    public Integer getAbsDiffStructureAndMachineDays() {
        return Math.abs(diffStructureAndMachineDays);
    }

    /**
     * 是否结构优先
     * 只要有1个结构优先就是结构优先
     * 1 结构优先 0 不是结构优先
     *
     * @return
     */
    public Integer isStructurePriority() {
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return YesOrNoEnum.NO.getValue();
        }
        boolean isStructurePriority = groupPlanData.stream().anyMatch(singlePlan -> YesOrNoEnum.YES.getCode().equals(singlePlan.getStructurePriority()));
        if (isStructurePriority) {
            return YesOrNoEnum.YES.getValue();
        }
        return YesOrNoEnum.NO.getValue();
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
        if (!oemJoinStructurePriority) {
            // OEM不参与结构优先级竞争
            hasHeightProductionList = hasHeightProductionList.stream().filter(heightProductionPlan -> !oemBrandSet.contains(heightProductionPlan.getBrand())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasHeightProductionList)) {
                return BigDecimal.ZERO.intValue();
            }
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
     * 使用的特殊原材料的种类数
     *
     * @return
     */
    public Integer getUsedSpecialMaterialCount() {
        if (CollectionUtils.isEmpty(embryoSpecialMaterialInfoMap)) {
            return BigDecimal.ZERO.intValue();
        }
        return embryoSpecialMaterialInfoMap.keySet().size();
    }

    /**
     * 判断是否可进行下一次分配
     * 特殊材料结构不进行最小分配天数控制即最小排产天数 = 1
     * 非特殊材料结构，需要判断剩余可分配天数要 >= 参数SYS0204010
     *
     * @param productionContext 排产上下文
     * @return
     */
    public boolean isNextAllocation(Integer leftOverDays, TbrProductionContext productionContext) {
        if (leftOverDays <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        return leftOverDays >= getMinAllocationDays(productionContext);
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
        dayProductionLimitInfo = dayLimitInfo;
        //20260422+ 处理结构需要额外增加硫化机台数
        extraHandlerDayLimit(context);
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
        //20260422+ 处理结构需要额外增加硫化机台数
        extraHandlerDayLimit(context);
    }

    /**
     * 获取在productionDay是否排产了需要排产Sku的胎胚
     *
     * @param productionDay     排产日
     * @param productionSkuInfo 排产Sku信息
     * @return
     */
    public boolean hasProductionEmbryo(Integer productionDay, MonthPlanProductionRequirePlanVo productionSkuInfo) {
        if (null == productionDay || null == productionSkuInfo) {
            return false;
        }
        String embryoCode = productionSkuInfo.getEmbryoCode();
        if (StringUtils.isBlank(embryoCode)) {
            return false;
        }
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return false;
        }
        GroupPlanCxLhCapacityLimitHelper dayLimitInfo = dayProductionLimitInfo.get(productionDay);
        if (null == dayLimitInfo) {
            return false;
        }
        Set<String> productionEmbryoCodeSet = dayLimitInfo.getProductionEmbryoCodeSet();
        if (CollectionUtils.isEmpty(productionEmbryoCodeSet)) {
            return false;
        }
        return productionEmbryoCodeSet.contains(embryoCode);
    }

    /**
     * 增加结构的Sku日排产信息
     *
     * @param productionContext    排产上下文
     * @param skuDayProductionInfo 排产SKu信息
     */
    public void addDayProductionInfo(TbrProductionContext productionContext, SkuDayProductionInfoHelper skuDayProductionInfo) {
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
        if (checkCanAddEmbryoCode(productionContext, skuDayProductionInfo)) {
            //增加日排产量<日换模数量时，不计算胎胚种类数 sandy+ 2026.3.24
            limitInfo.getProductionEmbryoCodeSet().add(skuDayProductionInfo.getEmbryoCode());
        }
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
            addSkuProductionDetailInfo(limitInfo, skuDayProductionInfo);
            return;
        }
        //更新使用模具和排产数量
        planned.addDayProductionInfo(skuDayProductionInfo);
        //加入Sku排产明细信息
        addSkuProductionDetailInfo(limitInfo, skuDayProductionInfo);
    }

    /**
     * 检查能否加胎胚种类数
     *
     * @param productionContext
     * @param skuDayProductionInfo
     * @return
     */
    private boolean checkCanAddEmbryoCode(TbrProductionContext productionContext, SkuDayProductionInfoHelper skuDayProductionInfo) {
        Integer changeMouldFirstQty = productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty();
        return skuDayProductionInfo.getSumProductionQty() >= changeMouldFirstQty;
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
     * 根据硫化配比，计算成型单日产能量
     * = 最小日硫化量(单模) * 2 * lhRatio
     *
     * @param lhRatio
     * @return
     */
    private BigDecimal getDayCapacityByLhRatio(Integer lhRatio) {
        return BigDecimal.valueOf(getDayCapacityBySingleLh()).multiply(BigDecimal.valueOf(lhRatio));
    }

    /**
     * 获取单日产能--Sku最小日硫化量
     *
     * @return
     */
    private Integer getDayCapacityBySingleLh() {
        if (null == minLhDayCapacityQty) {
            return BigDecimal.ZERO.intValue();
        }
        return minLhDayCapacityQty * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
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
    public GroupPlanCxLhCapacityLimitHelper getNextDayInfo(GroupPlanCxLhCapacityLimitHelper currentLimit) {
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
        //20260324 因已经将硫化配比提前计算，故而只需判断胎胚即可
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
            return null;
        }
        Set<Integer> productionDaySet = dayProductionLimitInfo.keySet();
        List<Integer> previousDayList = productionDaySet.stream().filter(day -> day < currentDay).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(previousDayList)) {
            return null;
        }
        //日期从大到小排序
        previousDayList.sort(Comparator.comparing(Integer::intValue, Comparator.reverseOrder()));
        return previousDayList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 加入Sku排产明细，以Sku为维度，同一模具合并
     *
     * @param limitInfo            日排产信息
     * @param skuDayProductionInfo sku日排产信息
     */
    private void addSkuProductionDetailInfo(GroupPlanCxLhCapacityLimitHelper limitInfo, SkuDayProductionInfoHelper skuDayProductionInfo) {
        if (null == limitInfo || null == skuDayProductionInfo || StringUtils.isBlank(skuDayProductionInfo.getMaterialDesc())) {
            return;
        }
        SkuDayProductionInfoHelper skuDayInfo = SkuDayProductionInfoHelper.createClone(skuDayProductionInfo);
        String materialDesc = skuDayInfo.getMaterialDesc();
        List<SkuDayProductionInfoHelper> detailList = limitInfo.getSkuProductionDetailInfo().get(materialDesc);
        if (null == detailList) {
            detailList = new ArrayList<>();
            detailList.add(skuDayInfo);
            limitInfo.getSkuProductionDetailInfo().put(materialDesc, detailList);
            return;
        }
        boolean isHandler = false;
        for (SkuDayProductionInfoHelper alreadyInfo : detailList) {
            if (alreadyInfo.mergeNewProductionInfo(skuDayInfo)) {
                isHandler = true;
                break;
            }
        }
        if (!isHandler) {
            detailList.add(skuDayInfo);
        }
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

    /**
     * 初始化日产能限制信息
     *
     * @param context
     * @return
     */
    public void initMpDailyCapacityLimit(Context context) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        if (dailyCapacityLimitVoMap == null) {
            dailyCapacityLimitVoMap = new HashMap<>();
        }
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        Integer endDay = productionContext.getMonthDays();
        GroupPlanCxLhCapacityLimitHelper capacityLimitHelper;
        for (int i = ProductionConstant.MONTH_START_DAY; i <= endDay; i++) {
            dailyCapacityLimitVo = dailyCapacityLimitVoMap.get(i);
            if (dailyCapacityLimitVo == null) {
                dailyCapacityLimitVo = new MpDailyCapacityLimitVo();
            }
            dailyCapacityLimitVo.setDailyDate(i);
            // 1、设置每日硫化机台总限制数、每日胎胚种类总限制数
            capacityLimitHelper = dayProductionLimitInfo.get(i);
            if (capacityLimitHelper == null) {
                continue;
            }
            dailyCapacityLimitVo.setMaxLhMachines(capacityLimitHelper.getMaxLhMachineCount());
            dailyCapacityLimitVo.setMaxEmbryoTypes(capacityLimitHelper.getMaxEmbryoCodeCount());

            dailyCapacityLimitVoMap.put(i, dailyCapacityLimitVo);
        }
        //初始化日产信息（包括日最大排产量、开停产标识、日产比例）
        initDayProductionInfo(productionContext, dailyCapacityLimitVoMap);
    }

    /**
     * 初始化日产信息（包括日最大排产量、开停产标识、日产比例）
     *
     * @param context
     * @param dailyCapacityLimitVoMap
     */
    private void initDayProductionInfo(Context context, Map<Integer, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<Integer, MdmWorkCalendar> workCalendarMap = productionContext.getBaseDataContainer().getWorkCalendarMap();
        if (PubUtil.isEmpty(dailyCapacityLimitVoMap) || PubUtil.isEmpty(workCalendarMap)) {
            return;
        }

        Integer dayMaxCapacity = productionContext.getBaseDataContainer().getParamConfiguration().getDayMaxCapacity();
        MpDailyCapacityLimitVo dailyCapacityLimitVo;
        MdmWorkCalendar workCalendar;
        for (Map.Entry<Integer, MpDailyCapacityLimitVo> entry : dailyCapacityLimitVoMap.entrySet()) {
            dailyCapacityLimitVo = entry.getValue();
            workCalendar = workCalendarMap.get(entry.getKey());
            if (workCalendar == null) {
                continue;
            }
            dailyCapacityLimitVo.setDayOpenCloseFlag(workCalendar.getDayFlag());
            dailyCapacityLimitVo.setDayProductionRate(workCalendar.getRate());
            //日最大排产量 = 日最大产能*比率/100
            dailyCapacityLimitVo.setMaxDayProductionQty(dayMaxCapacity * workCalendar.getRate() / 100);
            //若前日是停产，今日第1天开产，即开产首日，则仍按正常产能分配
            dailyCapacityLimitVo.setOpenProductionFirstDay(isOpenProductionFirstDay(workCalendarMap, entry.getKey()));
            if (dailyCapacityLimitVo.isOpenProductionFirstDay()) {
                dailyCapacityLimitVo.setMaxDayProductionQty(dayMaxCapacity);
            }
        }
    }

    /**
     * 检查是否开产首日
     *
     * @param workCalendarMap 日历Map
     * @param checkDay        检查日
     * @return true--开产首日，false--不是开产首日
     */
    private boolean isOpenProductionFirstDay(Map<Integer, MdmWorkCalendar> workCalendarMap, int checkDay) {
        int preDay = checkDay - 1;
        preDay = preDay < FactoryConstant.MONTH_START_DAY ? FactoryConstant.MONTH_START_DAY : preDay;
        return !YesOrNoEnum.YES.getCode().equals(workCalendarMap.get(preDay).getDayFlag()) &&
                YesOrNoEnum.YES.getCode().equals(workCalendarMap.get(checkDay).getDayFlag());
    }

    /**
     * 计划日硫化机台数、胎胚种类数、换模次数
     * 统计使用
     *
     * @param context
     */
    public void reCalcMpDailyCapacityLimit(Context context) {
        MpMonthPlanDailyCapacityLimit dailyCapacityLimitObj = new MpMonthPlanDailyCapacityLimit();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer endDay = productionContext.getMonthDays();
        //1. 转换模具排产结果
        List<FactoryMonthPlanMouldDayResult> mouldDayResultList = convertMouldDayResult(endDay);
        //2. 组装参数Map
        Map<String, Object> paramMap = composeDailyCapacityParamMap(productionContext);
        //3. 循环计算日产能
        for (int i = ProductionConstant.MONTH_START_DAY; i <= endDay; i++) {
            if (dailyCapacityLimitVoMap.get(i) == null) {
                continue;
            }
            dailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mouldDayResultList, i, dailyCapacityLimitVoMap.get(i), paramMap, null, null);
        }
    }

    /**
     * 计划日硫化机台数、胎胚种类数、换模次数
     *
     * @param context
     */
    public void reCalcMpDailyCapacityLimitByDay(Context context, Integer iDay, String mainPattern, String embryoCode) {
        if (dailyCapacityLimitVoMap.get(iDay) == null) {
            return;
        }
        MpMonthPlanDailyCapacityLimit dailyCapacityLimitObj = new MpMonthPlanDailyCapacityLimit();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer endDay = productionContext.getMonthDays();
        //1. 转换模具排产结果
        List<FactoryMonthPlanMouldDayResult> mouldDayResultList = convertMouldDayResult(endDay);
        //2. 组装参数Map
        Map<String, Object> paramMap = composeDailyCapacityParamMap(productionContext);
        //3. 计算日产能
        dailyCapacityLimitObj.calcLhMachinesWithEmbryoTypes(mouldDayResultList, iDay, dailyCapacityLimitVoMap.get(iDay), paramMap, mainPattern, embryoCode);
    }

    /**
     * 组装日产能参数Map
     *
     * @param productionContext
     * @return
     */
    public Map<String, Object> composeDailyCapacityParamMap(TbrProductionContext productionContext) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(MonthPlanEnums.CHANGE_MOULD_FIRST_QTY.getCode(), productionContext.getBaseDataContainer().getParamConfiguration().getChangeMouldFirstQty());
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY.getCode(), productionContext.getBaseDataContainer().getParamConfiguration().getChangeTypeBlockQty());
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_MAX_QTY.getCode(), productionContext.getBaseDataContainer().getParamConfiguration().getChangeTypeBlockMaxQty());
        paramMap.put(MonthPlanEnums.CHANGE_TYPE_BLOCK_QTY_DIFF.getCode(), productionContext.getBaseDataContainer().getParamConfiguration().getChangeTypeBlockQtyDiff());
        return paramMap;
    }

    /**
     * 转换模具排产结果
     *
     * @param endDay 结束日
     * @return 模具排产结果列表
     */
    public List<FactoryMonthPlanMouldDayResult> convertMouldDayResult(Integer endDay) {
        GroupPlanCxLhCapacityLimitHelper capacityLimitHelper;
        Map<String, FactoryMonthPlanMouldDayResult> mpProdFinalMap = new HashMap<>();
        for (int i = ProductionConstant.MONTH_START_DAY; i <= endDay; i++) {
            String dayField = FactoryConstant.DAY_FIELD + i;
            capacityLimitHelper = dayProductionLimitInfo.get(i);
            if (capacityLimitHelper == null) {
                continue;
            }
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyMap = capacityLimitHelper.getProductionSkuQtyInfo();
            if (PubUtil.isEmpty(productionSkuQtyMap)) {
                continue;
            }

            // 组装模具日排产结果
            productionSkuQtyMap.forEach((materialDesc, skuProductionInfo) -> {
                FactoryMonthPlanMouldDayResult mpMouldDayResult = mpProdFinalMap.get(skuProductionInfo.getMaterialCode());
                if (mpMouldDayResult == null) {
                    mpMouldDayResult = new FactoryMonthPlanMouldDayResult();
                    mpMouldDayResult.setStructureName(groupName);
                    mpMouldDayResult.setMaterialCode(skuProductionInfo.getMaterialCode());
                    mpMouldDayResult.setMaterialDesc(skuProductionInfo.getMaterialDesc());
                    mpMouldDayResult.setEmbryoCode(skuProductionInfo.getEmbryoCode());
                    mpMouldDayResult.setMainMaterialDesc(skuProductionInfo.getMainMaterialDesc());
                    mpMouldDayResult.setMainPattern(skuProductionInfo.getMainPattern());
                    mpMouldDayResult.setDayVulcanizationQty(skuProductionInfo.getDayVulcanizationQty());
                }
                mpMouldDayResult.setFieldValueByFieldName(dayField, skuProductionInfo.getSumProductionQty());
                mpProdFinalMap.put(skuProductionInfo.getMaterialCode(), mpMouldDayResult);
            });
        }
        return mpProdFinalMap.values().stream().collect(Collectors.toList());
    }

    /**
     * 获取可排产的连续时间点
     *
     * @param context       排产上下文
     * @param hasAddSkuList 可排产日信息
     * @param excludeDays   需要剔除的收尾时间点
     * @return
     */
    private SelectRangeLhMachineInfo getRangeInfo(Context context, List<GroupPlanCxLhCapacityLimitHelper> hasAddSkuList, Set<Integer> excludeDays) {
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayMap = hasAddSkuList.stream().collect(Collectors.toMap(GroupPlanCxLhCapacityLimitHelper::getDay, Function.identity()));
        //20260113 剔除需要排除的收尾时间点
        if (!CollectionUtils.isEmpty(excludeDays)) {
            hasAddSkuList = hasAddSkuList.stream().filter(singleGroup -> !excludeDays.contains(singleGroup.getDay())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(hasAddSkuList)) {
            return null;
        }
        Set<Integer> canProductionDaySet = hasAddSkuList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        Set<Integer> rangeSet = getEffectiveRange(context, canProductionDaySet);
        if (CollectionUtils.isEmpty(rangeSet)) {
            return null;
        }
        List<Integer> rangeList = new ArrayList<>(rangeSet);
        rangeList.sort(Comparator.comparing(Integer::intValue));
        Integer conclusionDay = rangeList.get(BigDecimal.ZERO.intValue());
        Integer endDay = rangeList.get(rangeList.size() - BigDecimal.ONE.intValue());
        GroupPlanCxLhCapacityLimitHelper selectedDayLimit = dayMap.get(conclusionDay);
        GroupPlanCxLhCapacityLimitHelper endDayLimit = dayMap.get(endDay);
        return new SelectRangeLhMachineInfo(selectedDayLimit, endDayLimit);
    }

    /**
     * 获取可排产的时间范围
     * 与全局最大硫化机台数限制取得交集
     *
     * @param context  排产上下文
     * @param rangeSet 初始的可排产日
     * @return
     */
    private Set<Integer> getEffectiveRange(Context context, Set<Integer> rangeSet) {
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        //获取当前排产范围本身存在的间断日集合
        Set<Integer> disContinuitySet = getDisContinuityDayByRange(rangeSet, stopDaySet);
        //获取与全局硫化机台数可排产日集合
        Set<Integer> effectiveDaySet = getEffectiveDayByIntersection(context, rangeSet);
        if (CollectionUtils.isEmpty(effectiveDaySet)) {
            return Collections.emptySet();
        }
        //得到间断日
        Set<Integer> diffSet = rangeSet.stream().filter(single -> !effectiveDaySet.contains(single)).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(diffSet)) {
            disContinuitySet.addAll(diffSet);
        }
        if (CollectionUtils.isEmpty(disContinuitySet)) {
            return rangeSet;
        }
        List<Integer> diffList = new ArrayList<>(disContinuitySet);
        //从早到晚
        diffList.sort(Comparator.comparing(Integer::intValue));
        for (Integer diffDay : diffList) {
            //看前面是否有连续时间段
            Set<Integer> beforeSet = rangeSet.stream().filter(single -> single < diffDay && effectiveDaySet.contains(single)).collect(Collectors.toSet());
            Set<Integer> findRangeSet = ContinuousProductionDayHandler.getEarliestContinuousRange(context, 2, beforeSet, stopDaySet);
            if (!CollectionUtils.isEmpty(findRangeSet)) {
                return findRangeSet;
            }
            //看后面是否有连续时间段
            Set<Integer> afterSet = rangeSet.stream().filter(single -> single >= diffDay && effectiveDaySet.contains(single)).collect(Collectors.toSet());
            findRangeSet = ContinuousProductionDayHandler.getEarliestContinuousRange(context, 2, afterSet, stopDaySet);
            if (!CollectionUtils.isEmpty(findRangeSet)) {
                return findRangeSet;
            }
        }
        return Collections.emptySet();
    }

    /**
     * 获取本身间断的集合
     *
     * @param rangeSet   本身的排产日集合
     * @param stopDaySet 停产日集合
     * @return
     */
    private Set<Integer> getDisContinuityDayByRange(Set<Integer> rangeSet, Set<Integer> stopDaySet) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(rangeSet)) {
            return Collections.emptySet();
        }
        Set<Integer> disContinuitySet = new HashSet<>();
        Set<Integer> realStopDaySet = Optional.ofNullable(stopDaySet).orElse(Collections.emptySet());
        List<Integer> allProductionDayList = dayProductionLimitInfo.keySet().stream().collect(Collectors.toList());
        allProductionDayList.sort(Comparator.comparing(Integer::intValue));
        allProductionDayList.forEach(productionDay -> {
            if (realStopDaySet.contains(productionDay)) {
                return;
            }
            if (rangeSet.contains(productionDay)) {
                return;
            }
            disContinuitySet.add(productionDay);
        });
        return disContinuitySet;
    }

    /**
     * 获取与实际使用机台数与全局限制机台数
     * 不超全局限制机台数的排产日
     *
     * @param context  排产上下文
     * @param rangeSet 分组内可排产日集合
     * @return
     */
    private Set<Integer> getEffectiveDayByIntersection(Context context, Set<Integer> rangeSet) {
        //计算每日的使用硫化机台数
        reCalcMpDailyCapacityLimit(context);
        Set<Integer> effectiveDaySet = new HashSet<>();
        rangeSet.forEach(productionDay -> {
            //1、取得使用硫化组数
            MpDailyCapacityLimitVo dayUsedDetail = dailyCapacityLimitVoMap.get(productionDay);
            if (null == dayUsedDetail) {
                effectiveDaySet.add(productionDay);
                return;
            }
            //2、获取全局最大可剩余的硫化组数
            GroupPlanCxLhCapacityLimitHelper dayLimit = dayProductionLimitInfo.get(productionDay);
            if (null == dayLimit) {
                return;
            }
            Integer realUsedLhMachines = Optional.ofNullable(dayUsedDetail.getUsedLhMachines()).orElse(BigDecimal.ZERO.intValue());
            Integer maxLhMachines = dayLimit.getMaxLimitLhMachine();
            //3、不能超
            if (realUsedLhMachines < maxLhMachines) {
                effectiveDaySet.add(productionDay);
            }
        });
        if (CollectionUtils.isEmpty(effectiveDaySet)) {
            return Collections.emptySet();
        }
        return effectiveDaySet;
    }

    /**
     * 获取排产限制信息
     * 得到有效排产日范围集合
     *
     * @param context       排产上下文
     * @param addSkuInfo    排产的Sku信息
     * @param preSelected   收尾硫化组信息
     * @param selectedMould 选中的模具
     * @return
     */
    private MouldProductionDayLimitHelper getProductionDayLimitInfo(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, EarliestConclusionLhGroupHelper preSelected, List<ProductionMouldInfoVo> selectedMould) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<Integer> stopDayInfo = productionContext.getStopDays();
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        Integer preClosingDay = preSelected.getClosingDay();
        Integer preEndDay = preSelected.getEndDay();
        //20260327 修正根据materialDesc重新构建前Sku信息
        BeforeSkuProductionInfo lhBeforeSkuInfo = ConclusionLhMachineHandler.findBeforeSkuProductionInfoByAddSku(context, addSkuInfo, this, preClosingDay);
        TbrMouldProductionLogRecorder.addFindBeforeSkuInfo(context, groupName, addSkuInfo.getMaterialDesc(), lhBeforeSkuInfo);
        preSelected.updateBeforeSkuInfo(lhBeforeSkuInfo);
        BeforeSkuProductionInfo mouldSkuInfo = ChangeMouldInfo.buildBeforeSkuProductionInfoByMould(productionContext, preClosingDay, selectedMould);
        ChangeMouldInfo changeMouldInfo = ChangeMouldInfo.buildChangeMouldInfo(context, addSkuInfo, preSelected.getBeforeSkuInfo(), mouldSkuInfo);
        boolean isChangeMould = changeMouldInfo.isChangeMould();
        //隔天换模
        if (isChangeMould && changeMouldInfo.isProductionNextDay()) {
            preClosingDay = context.getNextHasProductionDay(preClosingDay, stopDayInfo);
        }
        return LhGroupProductionRangeCalculator.confirmProductionRange(productionContext, addSkuInfo, preClosingDay, preEndDay, selectedMould, dayLimitList, stopDayInfo, isChangeMould);
    }

    /**
     * 20260422+ 处理结构需要额外增加硫化机台数
     *
     * @param context 排产上下文
     */
    private void extraHandlerDayLimit(Context context) {
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, Integer> extraMap = productionContext.getBaseDataContainer().getParamConfiguration().getExtraMap();
        if (CollectionUtils.isEmpty(extraMap) || !extraMap.containsKey(groupName)) {
            return;
        }
        Integer lhMachines = extraMap.get(groupName);
        if (BigDecimal.ZERO.intValue() == lhMachines) {
            return;
        }
        dayProductionLimitInfo.forEach((day, dayLimitInfo) -> {
            Set<String> cxMachineCodeSet = dayLimitInfo.getCxMachineCodeSet();
            if (CollectionUtils.isEmpty(cxMachineCodeSet) || cxMachineCodeSet.size() <= BigDecimal.ONE.intValue()) {
                return;
            }
            dayLimitInfo.addMaxLhMachines(lhMachines);
        });
    }

    /**
     * 是否达到二次排产条件
     * 1、首次排产，直接跳过
     * 2、二次排产时，满足最短上机天数
     *
     * @param context             排产上下文
     * @param addSkuInfo          排产计划
     * @param canProductionDaySet 排产天数
     * @param maxNeedDays         需求量最少排产天数
     * @return
     */
    private boolean isReachAgainProduction(Context context, MonthPlanProductionRequirePlanVo addSkuInfo, Set<Integer> canProductionDaySet, Integer maxNeedDays) {
        if (null == addSkuInfo || CollectionUtils.isEmpty(canProductionDaySet)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> skuPlanList = groupPlanData.stream().filter(singlePlan -> addSkuInfo.getMaterialDesc().equals(singlePlan.getMaterialDesc())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuPlanList)) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> effectiveList = skuPlanList.stream().filter(singleSkuPlan -> singleSkuPlan.getOriginProductionQty() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return false;
        }
        boolean hasProduction = effectiveList.stream().anyMatch(MonthPlanProductionRequirePlanVo::hasPlannedProduction);
        if (!hasProduction) {
            return true;
        }
        Integer daySize = Math.min(canProductionDaySet.size(), maxNeedDays);
        // 非首次上模最短在机天数
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer skuShortestProductionDays = productionContext.getBaseDataContainer().getParamConfiguration().getSkuShortestProductionDays();
        return daySize >= skuShortestProductionDays;
    }
}
