package com.zlt.aps.factory.domain.dto;

import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.ProductSpecificationsUtils;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import lombok.Data;
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
    private Long sumPlanQty;
    /**
     * 最小硫化机台数(结构与硫化配比，取最小)
     */
    private Integer minLhMachineCount;
    /**
     * 结构的SKU中最小的日硫化产能
     */
    private Long minLhDayCapacityQty;
    /**
     * 分组的计划信息
     */
    private List<MonthPlanProductionRequirePlanVo> groupPlanData;
    /**
     * 估算需要的机台数
     */
    private BigDecimal needCxCapacityMachineCount;
    /**
     * 估算需要的天数
     */
    private Integer theoryDays;
    /**
     * 成型-硫化配比信息
     */
    private Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap;
    /**
     * 针对成型机的固定优先级
     */
    private Integer fixedPriority;
    /**
     * 近1个月的上机日期
     */
    private Date lastBoardingDate;
    /**
     * 近3个月的排产次数
     */
    private Integer productionCount;

    /**
     * 粗步计算 结构需求量需要的成型产能分配
     * 结构总需求量/(结构下SKU最小日硫化量 * 结构最小硫化配比值 * 月份生产天数
     * 保留1位小数
     * 如果 小数部分 > 0.9，则向上取整
     *
     * @param context              排产上下文
     * @param requirePlanList      需排产的计划
     * @param structureLhRatioList 结构硫化配比信息
     * @return
     */
    public static Map<String, ProductionPlanGroupInfo> statisticsAndEstimateCxAllocationByGroup(Context context, List<MonthPlanProductionRequirePlanVo> requirePlanList, List<MonthPlanStructureLhRatioVo> structureLhRatioList) {
        if (CollectionUtils.isEmpty(requirePlanList)) {
            return Collections.emptyMap();
        }
        //根据结构成型硫化配比信息，提取结构最小的硫化配比
        Map<String, List<MonthPlanStructureLhRatioVo>> structureGroupMap = getStructureGroupInfo(structureLhRatioList);
        Map<String, MonthPlanStructureLhRatioVo> minLhRatioMap = getMinLhRatioMap(structureGroupMap);
        //1、对计划按结构分组，构建结构分组对象ProductionPlanGroupInfo
        Map<String, List<MonthPlanProductionRequirePlanVo>> groupPlanMap = requirePlanList.stream().collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getStructureName));
        Map<String, ProductionPlanGroupInfo> groupInfoMap = new HashMap<>(groupPlanMap.size());
        groupPlanMap.forEach((structureName, planList) -> {
            ProductionPlanGroupInfo groupInfo = new ProductionPlanGroupInfo();
            groupInfo.setGroupName(structureName);
            groupInfo.setProductType(context.getProductType());
            groupInfo.setGroupPlanData(planList);
            List<MonthPlanStructureLhRatioVo> cxLhRatioList = structureGroupMap.get(structureName);
            if (CollectionUtils.isEmpty(cxLhRatioList)) {
                groupInfo.setCxMachineLhRationMap(Collections.emptyMap());
            } else {
                Map<String, MonthPlanStructureLhRatioVo> allCxLhRatioMap = cxLhRatioList.stream().collect(Collectors.toMap(MonthPlanStructureLhRatioVo::getCxMachineBrandCode, Function.identity()));
                groupInfo.setCxMachineLhRationMap(allCxLhRatioMap);
            }
            groupInfoMap.put(structureName, groupInfo);
        });
        //2、提取有效净需求--剔除不可排产的-汇总需求量，并获得分组下最小日硫化产能
        groupInfoMap.forEach((structureName, groupInfo) -> {
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupInfo.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.longValue());
                return;
            }
            //剔除不排产的计划
            List<MonthPlanProductionRequirePlanVo> productionPlanList = groupPlanData.stream().filter(productionPlan -> YesOrNoEnum.YES.getCode().equals(productionPlan.getIsProduction())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(productionPlanList)) {
                groupInfo.setSumPlanQty(BigDecimal.ZERO.longValue());
                return;
            }
            Long sumPlanQty = productionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getCxCapacityRequireQty).sum();
            Long minDayLhCapacity = productionPlanList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getDayVulcanizationQty).min().getAsLong();
            groupInfo.setSumPlanQty(sumPlanQty);
            groupInfo.setMinLhDayCapacityQty(minDayLhCapacity);
        });
        //3、根据结构的硫化配比及最小的硫化机台数 估算需要的成型机台数
        groupInfoMap.forEach((structureName, groupInfo) -> {
            MonthPlanStructureLhRatioVo ratioInfo = minLhRatioMap.get(structureName);
            if (null == ratioInfo) {
                return;
            }
            groupInfo.setMinLhMachineCount(ratioInfo.getLhMachineMaxQty());
            //粗算所需成型机台数
            groupInfo.calculateNeedCxCapacityMachineCount(context.getMaxProductionDays());
        });
        return groupInfoMap;
    }

    /**
     * 计算需要分配的成型产能机台数，保留1位小数
     * 双模方式
     * 总需求量 / (SKU最小日硫化量 * 2 * 结构最小硫化配比 * 月度可排产天数),两位小数
     * 如果 小数部分 >0.9，则向上取整
     * 否则 = 保留1位小数
     *
     * @param monthMaxProductionDays 月度最大可生产天数
     */
    public void calculateNeedCxCapacityMachineCount(Integer monthMaxProductionDays) {
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
            return;
        }
        this.theoryDays = theoryDays;
        needCxCapacityMachineCount = machineCount.setScale(1, RoundingMode.UP);
    }

    /**
     * 根据成型对应硫化配比，得到剩余需求量需要分配的天数
     *
     * @param lhRatio 硫化配比
     * @return
     */
    public Integer calculateNeedDays(Integer lhRatio) {
        if (minLhDayCapacityQty <= BigDecimal.ZERO.intValue() || null == lhRatio || lhRatio <= BigDecimal.ZERO.intValue()) {
            return BigDecimal.ZERO.intValue();
        }
        //剩余需求量
        Long remainingProductionQty = getRemainingProductionQty();
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
    public Long getRemainingProductionQty() {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            return BigDecimal.ZERO.longValue();
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionList = groupPlanData.stream().filter(productionPlan -> productionPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionList)) {
            return BigDecimal.ZERO.longValue();
        }
        return hasProductionList.stream().mapToLong(MonthPlanProductionRequirePlanVo::getProductionQty).sum();
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
        Integer sectionWidth = ProductSpecificationsUtils.parseSectionWidthAndAspectRatio(specification).get(BigDecimal.ZERO.intValue());
        String currentSpecification = new ArrayList<>(currentSpecificationSet).get(BigDecimal.ZERO.intValue());
        Integer currentSectionWidth = ProductSpecificationsUtils.parseSectionWidthAndAspectRatio(currentSpecification).get(BigDecimal.ZERO.intValue());
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
     * 设置估算的机台数和天数为零
     */
    private void setAllocationZero() {
        needCxCapacityMachineCount = BigDecimal.ZERO;
        theoryDays = BigDecimal.ZERO.intValue();
    }
}
