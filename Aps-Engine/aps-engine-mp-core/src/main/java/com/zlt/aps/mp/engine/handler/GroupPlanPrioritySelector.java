package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.aps.mp.engine.basedata.assemble.history.ProductionHistoryHandler;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.GroupCxMachineSelectedTypeEnum;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSpecialMaterialProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结构优先级选择器
 *
 * @author ZLT
 * @date 20260320
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPlanPrioritySelector {

    private final ProductionHistoryHandler productionHistoryHandler;

    /**
     * 从所有还需排产计划分组中，获取最高优先级的结构
     * 1、获取还有需要分配的结构
     * 2、高优先级需求量Sku个数多的优先
     * 3、模具受限下，分组需求量小的优先(还需分配天数)
     * 4、使用特殊原材料共用性差的分组优先
     * 6、使用特殊原材料Sku个数多的优先
     * 5、分组需求量大的优先
     * 7、当选上的是含有特殊原材料的结构时：从所有的含有特殊原材料的结构重新选择
     * 7.1、特殊原材料共用性差的原材料结构优先
     * 7.2、使用SKu个数多的优先
     * 7.3、需求量大的优先
     *
     * @param context          排产上下文
     * @param excludeGroupPlan 需要排除的分组
     * @return
     */
    public ProductionPlanGroupInfo getHeightPriorityGroup(Context context, Set<String> excludeGroupPlan) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlan = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupPlan)) {
            return null;
        }
        List<ProductionPlanGroupInfo> needProductionGroupList = allGroupPlan.values().stream().filter(singleGroup -> {
            if (excludeGroupPlan.contains(singleGroup.getGroupName())) {
                return false;
            }
            return singleGroup.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue();
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needProductionGroupList)) {
            return null;
        }
        ProductionPlanGroupInfo selected;
        //结构优先列表 sandy+ 2026.3.26
        List<ProductionPlanGroupInfo> structurePriorityList = needProductionGroupList.stream().filter(x -> {
            return x.getGroupPlanData().stream().filter(y -> YesOrNoEnum.YES.getCode().equals(y.getStructurePriority())).count() > 0;
        }).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(structurePriorityList)){
            needProductionGroupList = structurePriorityList;
        }

        //高优先级需求SKU个数多的优先
        Integer maxHeightPriority = needProductionGroupList.stream().mapToInt(ProductionPlanGroupInfo::getHeightPriorityCount).max().getAsInt();
        List<ProductionPlanGroupInfo> heightList = needProductionGroupList.stream().filter(groupPlan -> maxHeightPriority.equals(groupPlan.getHeightPriorityCount())).collect(Collectors.toList());
        if (heightList.size() == BigDecimal.ONE.intValue()) {
            selected = heightList.get(BigDecimal.ZERO.intValue());
        } else {
            selected = getHighestOneGroup(productionContext, heightList);
        }
        //如果选上的是特殊原材料结构，则从所有特殊原材料结构中重新获取
        if (selected.isSpecialMaterial()) {
            TbrSpecialMaterialProductionLogRecorder.addProductionSpecialMaterialInfoLog(productionContext, "新结构挑选到");
            return getHeightPriorityGroupBySpecialMaterial(productionContext, excludeGroupPlan);
        }
        return selected;
    }

    /**
     * 机台反向结构，在已经产能可覆盖需求的分组列表中，获取最合适的一组计划(结构)
     * 此时机台已经有前结构信息
     * 1、固定优先
     * 2、成型的前结构同规格(SKU的规格属性)优先
     * 3、成型的前结构同英寸(SKU的英寸属性)优先
     * 4、成型的前结构断面宽±10
     * 5、近1个月结构上机日期近的优先
     * 6、近3个月结构生产次数多的优先
     *
     * @param context            排产上下文
     * @param enableGroupPlanMap 产能可覆盖的分组计划
     * @param cxMachineInfo      当前需要选择结构的机台
     * @return
     */
    public ProductionPlanGroupInfo selectOptimalOneGroup(Context context, Map<String, ProductionPlanGroupInfo> enableGroupPlanMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> groupPlanList = new ArrayList<>(enableGroupPlanMap.size());
        //设置固定信息--固定优先级
        enableGroupPlanMap.forEach((structureName, groupPlan) -> {
            groupPlan.setFixedPriority(cxMachineInfo.getFixedPriorityValue(groupPlan));
            groupPlanList.add(groupPlan);
        });
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return null;
        }
        List<ProductionPlanGroupInfo> sectionWidthList;
        ProductionPlanGroupInfo selected;
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String cxMachineTypeCode = cxMachineInfo.getCxMachineTypeCode();
        // 1、如果在机结构有特殊材料，需要优先选择包含特殊材料的结构
        List<ProductionPlanGroupInfo> specialMaterialList = getNeedGroupList(context, cxMachineInfo, groupPlanList);
        //1、取固定的
        Integer minFixedPriority = specialMaterialList.stream().mapToInt(ProductionPlanGroupInfo::getFixedPriority).min().getAsInt();
        List<ProductionPlanGroupInfo> fixedGroupPlanList = specialMaterialList.stream().filter(groupPlan -> minFixedPriority.equals(groupPlan.getFixedPriority())).collect(Collectors.toList());
        if (fixedGroupPlanList.size() == BigDecimal.ONE.intValue()) {
            selected = fixedGroupPlanList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.FIXED_PRIORITY);
            return selected;
        }
        CxMachineAllocationPlanHelper lastHelper = cxMachineInfo.getLastAllocationInfo();
        //取前规格排产计划-所有
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getProductionPlanInfo().getGroupPlanData();
        //2、与前结构含有同规格的优先
        List<ProductionPlanGroupInfo> sameSpecificationList = fixedGroupPlanList.stream().filter(fixedPlan -> fixedPlan.hasSameSpecifications(realProductionPlanList)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(sameSpecificationList)) {
            if (sameSpecificationList.size() == BigDecimal.ONE.intValue()) {
                selected = sameSpecificationList.get(BigDecimal.ZERO.intValue());
                TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SAME_SPECIFICATIONS_PRIORITY);
                return selected;
            }else{
                //与前结构含有同规格 有多条，则含有结构优先的-> 结构需求与产能接近 sandy+ 2026.3.26
                return getScmProductionPlanGroupInfo(context, cxMachineCode, cxMachineTypeCode, sameSpecificationList);
            }
        }

        Integer diffValue = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getSectionWidthDiffValue();
        //3、与前结构含有同英寸的优先
        List<ProductionPlanGroupInfo> sameProSizeList = fixedGroupPlanList.stream().filter(fixedPlan -> fixedPlan.hasSameSpecifications(realProductionPlanList) || fixedPlan.hasSameProSize(realProductionPlanList)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(sameProSizeList)) {
            if (sameProSizeList.size() == BigDecimal.ONE.intValue()) {
                selected = sameProSizeList.get(BigDecimal.ZERO.intValue());
                TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SAME_PRO_SIZE_PRIORITY);
                return selected;
            }else{
                //3.1、同英寸下 断面宽差值±10 参数
                sectionWidthList = sameProSizeList.stream().filter(sectionWidthPlan -> sectionWidthPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(sectionWidthList)) {
                    if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
                        selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
                        TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SECTION_WIDTH_PRIORITY);
                        return selected;
                    }else{
                        //与前结构含有同规格 有多条，则含有结构优先的-> 结构需求与产能接近 sandy+ 2026.3.26
                        return getScmProductionPlanGroupInfo(context, cxMachineCode, cxMachineTypeCode, sectionWidthList);
                    }
                }
                //3.2、同英寸下，没有断面宽差值±10
                //与前结构含有同规格 有多条，则含有结构优先的-> 结构需求与产能接近 sandy+ 2026.3.26
                return getScmProductionPlanGroupInfo(context, cxMachineCode, cxMachineTypeCode, sameProSizeList);
            }
        }

        //4、断面宽差值±10 参数
        sectionWidthList = fixedGroupPlanList.stream().filter(sectionWidthPlan -> sectionWidthPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(sectionWidthList)) {
            if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
                selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
                TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SECTION_WIDTH_PRIORITY);
                return selected;
            }else{
                //与前结构含有同规格 有多条，则含有结构优先的-> 结构需求与产能接近 sandy+ 2026.3.26
                return getScmProductionPlanGroupInfo(context, cxMachineCode, cxMachineTypeCode, sectionWidthList);
            }
        }

        //5、设置该成型机近1个月的排产分组和排产次数
        sectionWidthList.forEach(groupPlan -> {
            groupPlan.setLastBoardingDate(BigDecimal.ZERO.intValue());
            groupPlan.setProductionCount(BigDecimal.ZERO.intValue());
            productionHistoryHandler.setCxMachineProductionGroupPlanHistory(context, groupPlan, cxMachineInfo);
        });
        sectionWidthList.sort(Comparator.comparing(ProductionPlanGroupInfo::getLastBoardingDate, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ProductionPlanGroupInfo::getProductionCount, Comparator.nullsLast(Comparator.reverseOrder())));
        selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
        log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.HISTORY_QUALITY_PRIORITY));
        return selected;
    }

    /**
     * 在同等层级中，含有多个备选时，先 含有结构优先的 -> 结构需求与产能接近
     * @param context 排产上下文
     * @param cxMachineCode 当前需要选择的机台
     * @param cxMachineTypeCode 当前需要选择的机台机型
     * @param sameLevelGroupInfoList 同等层级结构清单
     * @return
     */
    private ProductionPlanGroupInfo getScmProductionPlanGroupInfo(Context context, String cxMachineCode, String cxMachineTypeCode, List<ProductionPlanGroupInfo> sameLevelGroupInfoList) {
        ProductionPlanGroupInfo selected;
        //1. 含有结构优先的，优先选择
        List<ProductionPlanGroupInfo> structurePriorityList = sameLevelGroupInfoList.stream().filter(x -> {
            return x.getGroupPlanData().stream().filter(y -> YesOrNoEnum.YES.getCode().equals(y.getStructurePriority())).count() > 0;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(structurePriorityList)) {
            structurePriorityList = sameLevelGroupInfoList;
        }
        if (structurePriorityList.size() == BigDecimal.ONE.intValue()) {
            selected = structurePriorityList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SAME_STRUCTURE_PRIORITY);
            return selected;
        }
        //2. 在供应链高优先级个数多的前4结构清单中，降序，获取结构需求与产能接近的结构 sandy+2026.3.29
        structurePriorityList.sort( Comparator.comparingInt(ProductionPlanGroupInfo::getHeightPriorityCount).reversed());
        Integer structureBillPreCount = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getStructureBillPreCount();
        Integer subSize = structurePriorityList.size() > structureBillPreCount ? structureBillPreCount : structurePriorityList.size();
        structurePriorityList = structurePriorityList.subList(0,subSize);
        //结构需求与产能接近 -> 结构需求更大的
        structurePriorityList.sort( Comparator.comparingInt(ProductionPlanGroupInfo::getAbsDiffStructureAndMachineDays)
                .thenComparingInt(ProductionPlanGroupInfo::getRemainingNeedAllocationDays).reversed());
        selected = structurePriorityList.get(BigDecimal.ZERO.intValue());
        TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.NEAR_CAPACITY_PRIORITY);
        return selected;
    }

    /**
     * 使用特殊原材料的结构间，挑选还有需求量，且优先级最高含特殊原材料的结构
     * 1、只在使用特殊原材料的结构、还有需求量的结构进行挑选
     * 2、特殊原材料共用性差的结构优先
     * 3、使用特殊原材料Sku个数多的优先
     * 4、剩余可分配天数多的优先
     *
     * @param context          排产上下文
     * @param excludeGroupPlan 需要排除的分组
     * @return
     */
    public ProductionPlanGroupInfo getHeightPriorityGroupBySpecialMaterial(Context context, Set<String> excludeGroupPlan) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlan = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupPlan)) {
            return null;
        }
        List<ProductionPlanGroupInfo> hasLeftOverAndSpecialMaterialList = allGroupPlan.values().stream().filter(singleGroup -> {
            //不含特殊原材料的结构过滤
            if (!singleGroup.isSpecialMaterial()) {
                return false;
            }
            if (excludeGroupPlan.contains(singleGroup.getGroupName())) {
                return false;
            }
            return singleGroup.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue();
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasLeftOverAndSpecialMaterialList)) {
            return null;
        }
        if (hasLeftOverAndSpecialMaterialList.size() == BigDecimal.ONE.intValue()) {
            return hasLeftOverAndSpecialMaterialList.get(BigDecimal.ZERO.intValue());
        }
        groupCommonSort(productionContext, hasLeftOverAndSpecialMaterialList);
        return hasLeftOverAndSpecialMaterialList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 在机结构优先级比较
     * 1、优先使用特殊原材料的结构
     * 2、参见特殊原材料结构间的比较
     *
     * @param before 前结构
     * @param after  后结构
     * @return
     */
    public int compareContinueGroup(ProductionPlanGroupInfo before, ProductionPlanGroupInfo after) {
        Boolean beforeIsSpecial = null == before ? false : before.isSpecialMaterial();
        Boolean afterIsSpecial = null == after ? false : after.isSpecialMaterial();
        // Boolean的true比false大，因此需要倒序，优先处理true的
        int result = afterIsSpecial.compareTo(beforeIsSpecial);
        if (result != BigDecimal.ZERO.intValue()) {
            return result;
        }
        return compareSpecialMaterial(before, after);
    }

    /**
     * 比较两个特殊优先级
     * 1、使用特殊原材料种类多的优先
     * 2、含SKu个数多的优先
     * 3、需求量大优先
     *
     * @param before 前一个
     * @param after  后一个
     * @return
     */
    public int compareSpecialMaterial(ProductionPlanGroupInfo before, ProductionPlanGroupInfo after) {
        //使用特殊原材料种类多的优先
        Integer beforeTypeCount = before.getUsedSpecialMaterialCount();
        Integer afterTypeCount = after.getUsedSpecialMaterialCount();
        //倒序
        int result = afterTypeCount.compareTo(beforeTypeCount);
        if (result != BigDecimal.ZERO.intValue()) {
            return result;
        }
        //Sku个数多的先
        Integer beforeSkuCount = Optional.ofNullable(before.getSpecialMaterialsCount()).orElse(BigDecimal.ZERO.intValue());
        Integer afterSkuCount = Optional.ofNullable(after.getSpecialMaterialsCount()).orElse(BigDecimal.ZERO.intValue());
        result = afterSkuCount.compareTo(beforeSkuCount);
        if (result != BigDecimal.ZERO.intValue()) {
            return result;
        }
        Integer beforeNeedDays = Optional.ofNullable(before.getRemainingNeedAllocationDays()).orElse(BigDecimal.ZERO.intValue());
        Integer afterNeedDays = Optional.ofNullable(after.getRemainingNeedAllocationDays()).orElse(BigDecimal.ZERO.intValue());
        result = beforeNeedDays.compareTo(afterNeedDays);
        return result;
    }

    /**
     * 1、高优先级需求Sku个数多的优先
     * 2、模具受限下，需求量小的优先
     * 3、共用性材料差的优先
     * 4、需求量多的优先
     *
     * @param needProductionGroupList
     * @return
     */
    private ProductionPlanGroupInfo getHighestOneGroup(TbrProductionContext productionContext, List<ProductionPlanGroupInfo> needProductionGroupList) {
        if (CollectionUtils.isEmpty(needProductionGroupList)) {
            return null;
        }
        //高优先级需求SKU个数多的优先
        Integer maxHeightPriority = needProductionGroupList.stream().mapToInt(ProductionPlanGroupInfo::getHeightPriorityCount).max().getAsInt();
        List<ProductionPlanGroupInfo> heightList = needProductionGroupList.stream().filter(groupPlan -> maxHeightPriority.equals(groupPlan.getHeightPriorityCount())).collect(Collectors.toList());
        if (heightList.size() == BigDecimal.ONE.intValue()) {
            return heightList.get(BigDecimal.ZERO.intValue());
        }
        groupCommonSort(productionContext, heightList);
        return heightList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 结构通用排产
     * 1、特殊材料共用性差的优先
     * 2、使用特殊材料的Sku个数多的优先
     * 4、需求量的要优先
     *
     * @param productionContext       排产上下文
     * @param needProductionGroupList 有需求量的计划
     */
    private void groupCommonSort(TbrProductionContext productionContext, List<ProductionPlanGroupInfo> needProductionGroupList) {
        needProductionGroupList.sort((before, after) -> compareSpecialMaterial(before, after));
    }

    /**
     * 如果在机有排产过含有特殊原材料分组计划，则优先排产特殊原材料的分组计划
     * 否则为原有的分组计划groupPlanList
     *
     * @param context
     * @param currentCxMachine
     * @param groupPlanList
     */
    private List<ProductionPlanGroupInfo> getNeedGroupList(Context context, CxMachineBaseInfoVo currentCxMachine, List<ProductionPlanGroupInfo> groupPlanList) {
        boolean isSpecial = isProductionSpecialMaterialGroup(context, currentCxMachine);
        if (!isSpecial) {
            return groupPlanList;
        }
        // 特殊材料校验为true，则需要优先排特殊结构
        List<ProductionPlanGroupInfo> tempSpecialMaterialList = groupPlanList.stream()
                .filter(ProductionPlanGroupInfo::isSpecialMaterial).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tempSpecialMaterialList)) {
            return groupPlanList;
        }
        return tempSpecialMaterialList;
    }

    /**
     * 是否排产过含有特殊原材料的分组计划
     *
     * @param context          排产上下文
     * @param currentCxMachine 当前机台
     * @return
     */
    private boolean isProductionSpecialMaterialGroup(Context context, CxMachineBaseInfoVo currentCxMachine) {
        //当前在机有排产含有特殊材料的结构
        boolean isSpecial = this.hasSpecialStructure(currentCxMachine);
        if (isSpecial) {
            return true;
        }
        // 如果在机结构没有特殊材料，但是其他机台的在机结构有特殊材料，则同样需要优先选择包含特殊材料的结构
        TbrProductionContext productionContext = (TbrProductionContext) context;
        return productionContext.getBaseDataContainer().getCxMachineBaseInfo().values().stream()
                .anyMatch(machine -> this.hasSpecialStructure(machine));
    }

    /**
     * 判断机台是否包含特殊结构
     *
     * @param machine
     * @return
     */
    private Boolean hasSpecialStructure(CxMachineBaseInfoVo machine) {
        return machine.getAllocationList().stream()
                .anyMatch(allocation -> allocation.getProductionPlanInfo().isSpecialMaterial());
    }
}
