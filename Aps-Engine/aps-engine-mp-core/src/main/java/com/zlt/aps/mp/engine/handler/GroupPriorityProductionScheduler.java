package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxCapacityAllocationHandler;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxMouldProductionHandler;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.GroupTimeExtensionHandler;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SpecialMaterialScheduleHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 结构优先排产调度器
 * 以结构优先级为维度
 * 1、先获取当前结构优先级的Top3列表
 * 2、按同规格
 * 如A 同规格 12天
 * B 同规格
 *
 * @author ZLT
 * @date 20260426
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPriorityProductionScheduler {

    private final CxMouldProductionHandler cxMouldProductionHandler;

    private final CxCapacityAllocationHandler cxCapacityAllocationHandler;

    private final SpecialMaterialScheduleHandler specialMaterialScheduleHandler;

    private final GroupTimeExtensionHandler groupTimeExtensionHandler;

    /**
     * 以交付优先的排产方式模拟
     * 1、获取当前剩余待排产分组中，按优先级从高~低获取top列表
     * 优先级挑选：结构优先优先->高优先级有量的Sku个数多的优先->模具受限的Sku个数多的优先->结构需求量大的优先
     * 2、从Top列表中的每个分组，分别获取各自最合适匹配的成型机台
     * 匹配逻辑：
     * 完全覆盖：0 <= 需求天数-成型剩余产能 <= 1
     * 超出覆盖：1 < 需求天数-成型剩余产能
     * 不覆盖：0 > 需求天数 - 成型剩余产能
     * 第一优先级：与机台前分组同规格，需求能完全覆盖成型剩余产能
     * 第二优先级：与机台前分组同英寸，需求能完全覆盖成型剩余产能
     * 第三优先级：与机台前分组同规格，需求超出覆盖成型剩余产能
     * 第四优先级：与机台前分组同英寸，需求超出覆盖成型剩余产能
     * 第五优先级：与机台前分组同规格，需求不能覆盖成型剩余产能
     * 第六优先级：与机台前分组同英寸，需求不能覆盖成型剩余产能
     * 第七优先级：与机台前分组断面宽，需求能完全覆盖成型剩余产能
     * 第八优先级：与机台前分组断面宽，需求超出覆盖成型剩余产能
     * 第九优先级：与机台前分组断面宽，需求不能覆盖成型剩余产能
     * 第十优先级：与机台近生产过，需求完全覆盖成型剩余产能
     * 第十一优先级：与机台近生产过，需求超出覆盖成型剩余产能
     * 第十二优先级：与机台近生产过，需求不能覆盖成型剩余产能
     * 3、top各自选出匹配机台后，在进行Top列表的选择，选出分组、机台合适排产组
     * 优先级按Top中的优先级规则
     * 4、依次类推，迭代轮询，最终得到预排的分组
     *
     * @param context             排产上下文
     * @param excludeGroupPlan    需要剔除的分组(中间过程中找不到机台等情形)-初始空集合
     * @param preSelectedGroupMap 计划可分配产能的分组-初始空集合
     */
    public void allocationCxMachine(Context context, Set<String> excludeGroupPlan, Set<String> preSelectedGroupMap) {
        //1、获取还需排产分组的当前Top列表
        List<ProductionPlanGroupInfo> topList = getTopList(context, excludeGroupPlan);
        if (CollectionUtils.isEmpty(topList)) {
            return;
        }
        //2、获取各分组对应匹配的合适机台
        List<GroupPrioritySchedulerResultHelper> topSelectedCxMachineList = getGroupSelectedCxMachine(context, topList, false);
        if (CollectionUtils.isEmpty(topSelectedCxMachineList)) {
            excludeGroupPlan.addAll(topList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            allocationCxMachine(context, excludeGroupPlan, preSelectedGroupMap);
            return;
        }
        Map<ProductionPlanGroupInfo, GroupPrioritySchedulerResultHelper> selectedCxMachineMap = topSelectedCxMachineList.stream().collect(Collectors.toMap(GroupPrioritySchedulerResultHelper::getSelectedGroup, Function.identity()));
        topList.forEach(preSelectedGroup -> {
            if (!selectedCxMachineMap.containsKey(preSelectedGroup)) {
                excludeGroupPlan.add(preSelectedGroup.getGroupName());
            }
        });
        //3、获取综合最匹配的分组+机台的分组
        GroupPrioritySchedulerResultHelper finalSelected = getAppointGroupPlanProduction(context, topSelectedCxMachineList);
        if (null == finalSelected) {
            return;
        }
        //4、对挑选出来的分组，进行机台产能分配
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo selectedCxMachine = finalSelected.getSelectedCxMachine();
        ProductionPlanGroupInfo addNewGroupPlan = finalSelected.getSelectedGroup();
        String groupName = addNewGroupPlan.getGroupName();
        Integer originNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        Integer originLeftOverByCxMachine = selectedCxMachine.getRemainingDays();
        CxMachineAllocationPlanHelper addHelper = buildAllocationDetailInfo(productionContext, finalSelected);
        if (null == addHelper) {
            excludeGroupPlan.add(groupName);
            allocationCxMachine(context, excludeGroupPlan, preSelectedGroupMap);
            return;
        }
        //5、对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper, new HashSet<>());
        //重新获取剩余天数：可能因提前收尾变化，导致计划实际没有排，下轮直接排除,不能设置分配完成
        Integer newNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        if (newNeedAllocationDaysByGroupPlan.equals(originNeedAllocationDaysByGroupPlan)) {
            excludeGroupPlan.add(groupName);
        } else {
            preSelectedGroupMap.add(groupName);
            excludeGroupPlan.clear();
        }
        //下一批
        allocationCxMachine(context, excludeGroupPlan, preSelectedGroupMap);
    }

    /**
     * 对预期排产分组中的固定机台分组进行排产选机台
     *
     * @param context                排产上下文
     * @param priorityFixedGroupList Top之后固定机台优先排产
     * @return
     */
    public void productionGroupFixedCxMachine(Context context, Set<String> excludeGroupPlan, List<ProductionPlanGroupInfo> priorityFixedGroupList) {
        if (CollectionUtils.isEmpty(priorityFixedGroupList)) {
            return;
        }
        //1、获取还需排产分组的当前Top列表
        List<ProductionPlanGroupInfo> topFixedCxMachineList = getTopListByRange(context, priorityFixedGroupList, excludeGroupPlan);
        if (CollectionUtils.isEmpty(topFixedCxMachineList)) {
            return;
        }
        //2、获取对应的指定机台
        List<GroupPrioritySchedulerResultHelper> topSelectedCxMachineList = getGroupSelectedCxMachine(context, topFixedCxMachineList, true);
        if (CollectionUtils.isEmpty(topSelectedCxMachineList)) {
            excludeGroupPlan.addAll(topFixedCxMachineList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            productionGroupFixedCxMachine(context, excludeGroupPlan, priorityFixedGroupList);
            return;
        }
        Map<ProductionPlanGroupInfo, GroupPrioritySchedulerResultHelper> selectedCxMachineMap = topSelectedCxMachineList.stream().collect(Collectors.toMap(GroupPrioritySchedulerResultHelper::getSelectedGroup, Function.identity()));
        topFixedCxMachineList.forEach(preSelectedGroup -> {
            if (!selectedCxMachineMap.containsKey(preSelectedGroup)) {
                excludeGroupPlan.add(preSelectedGroup.getGroupName());
            }
        });
        //3、获取综合最匹配的分组+机台的分组
        GroupPrioritySchedulerResultHelper finalSelected = getAppointGroupPlanProduction(context, topSelectedCxMachineList);
        if (null == finalSelected) {
            return;
        }
        //4、对挑选出来的分组，进行机台产能分配
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo selectedCxMachine = finalSelected.getSelectedCxMachine();
        ProductionPlanGroupInfo addNewGroupPlan = finalSelected.getSelectedGroup();
        String groupName = addNewGroupPlan.getGroupName();
        Integer originNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        Integer originLeftOverByCxMachine = selectedCxMachine.getRemainingDays();
        CxMachineAllocationPlanHelper addHelper = buildAllocationDetailInfo(productionContext, finalSelected);
        if (null == addHelper) {
            excludeGroupPlan.add(groupName);
            productionGroupFixedCxMachine(context, excludeGroupPlan, priorityFixedGroupList);
            return;
        }
        //5、对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper, new HashSet<>());
        //重新获取剩余天数：可能因提前收尾变化，导致计划实际没有排，下轮直接排除,不能设置分配完成
        Integer newNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        if (newNeedAllocationDaysByGroupPlan.equals(originNeedAllocationDaysByGroupPlan)) {
            excludeGroupPlan.add(groupName);
        } else {
            excludeGroupPlan.clear();
        }
        //下一批
        productionGroupFixedCxMachine(context, excludeGroupPlan, priorityFixedGroupList);
    }

    /**
     * 获取结构优先级最高Top列表
     * 1、小于最短上机天数：跳过
     * 2、选择最高优先级的top个数列表
     * 优先级挑选：结构优先优先->高优先级有量的Sku个数多的优先->模具受限的Sku个数多的优先->结构需求量大的优先
     *
     * @param context          排产上下文
     * @param excludeGroupPlan 需要排产的分组
     * @return
     */
    private List<ProductionPlanGroupInfo> getTopList(Context context, Set<String> excludeGroupPlan) {
        TbrProductionGroupLogRecorder.addStartGroupSelectedCxMachineLog(context);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanMap = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return Collections.emptyList();
        }
        Set<String> needExcludeGroupMap = Optional.ofNullable(excludeGroupPlan).orElse(Collections.emptySet());
        Map<String, ProductionPlanGroupInfo> effectiveMap = Maps.newHashMap();
        allGroupPlanMap.forEach((groupName, groupInfo) -> {
            if (needExcludeGroupMap.contains(groupName)) {
                return;
            }
            effectiveMap.put(groupName, groupInfo);
        });
        if (CollectionUtils.isEmpty(effectiveMap)) {
            return Collections.emptyList();
        }
        //获取分组优先级最高的Top列表
        Integer topCount = productionContext.getBaseDataContainer().getParamConfiguration().getStructureBillPreCount();
        if (null == topCount) {
            topCount = BigDecimal.ONE.intValue();
        }
        List<ProductionPlanGroupInfo> topList = Lists.newArrayList();
        Set<String> excludeSelectTopMap = Sets.newHashSet();
        excludeSelectTopMap.addAll(needExcludeGroupMap);
        for (int index = BigDecimal.ONE.intValue(); index <= topCount; ) {
            if (!CollectionUtils.isEmpty(topList)) {
                excludeSelectTopMap.addAll(topList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            }
            ProductionPlanGroupInfo selectedGroup = cxCapacityAllocationHandler.getInsertNewGroupPlan(productionContext, effectiveMap, excludeSelectTopMap);
            if (null == selectedGroup) {
                break;
            }
            String groupName = selectedGroup.getGroupName();
            //最小分配天数
            Integer minAllocationDays = selectedGroup.getMinAllocationDays(productionContext);
            Integer leftOverDays = selectedGroup.getLeftOverNeedAllocationDays();
            //20260206 小于最短上机天数，则不进行分配
            if (!selectedGroup.isNextAllocation(leftOverDays, productionContext)) {
                if (leftOverDays > BigDecimal.ZERO.intValue()) {
                    log.info(TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, true, leftOverDays, minAllocationDays));
                }
                selectedGroup.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
                excludeSelectTopMap.add(groupName);
                continue;
            }
            topList.add(selectedGroup);
            index = index + BigDecimal.ONE.intValue();
        }
        return topList;
    }

    /**
     * 从指定分组集合中，获取结构优先级最高Top列表
     * 1、小于最短上机天数：跳过
     * 2、选择最高优先级的top个数列表
     * 优先级挑选：结构优先优先->高优先级有量的Sku个数多的优先->模具受限的Sku个数多的优先->结构需求量大的优先
     *
     * @param context          排产上下文
     * @param excludeGroupPlan 需要排产的分组
     * @return
     */
    private List<ProductionPlanGroupInfo> getTopListByRange(Context context, List<ProductionPlanGroupInfo> groupRange, Set<String> excludeGroupPlan) {
        if (CollectionUtils.isEmpty(groupRange)) {
            return Collections.emptyList();
        }
        TbrProductionGroupLogRecorder.addStartGroupSelectedCxMachineLog(context);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> needExcludeGroupMap = Optional.ofNullable(excludeGroupPlan).orElse(Collections.emptySet());
        List<ProductionPlanGroupInfo> effectiveList = Lists.newArrayList();
        groupRange.forEach(groupInfo -> {
            String groupName = groupInfo.getGroupName();
            if (needExcludeGroupMap.contains(groupName)) {
                return;
            }
            effectiveList.add(groupInfo);
        });
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        //获取分组优先级最高的Top列表
        Integer topCount = productionContext.getBaseDataContainer().getParamConfiguration().getStructureBillPreCount();
        if (null == topCount) {
            topCount = BigDecimal.ONE.intValue();
        }
        List<ProductionPlanGroupInfo> topList = Lists.newArrayList();
        Set<String> excludeSelectTopMap = Sets.newHashSet();
        excludeSelectTopMap.addAll(needExcludeGroupMap);
        for (int index = BigDecimal.ONE.intValue(); index <= topCount; ) {
            if (!CollectionUtils.isEmpty(topList)) {
                excludeSelectTopMap.addAll(topList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            }
            ProductionPlanGroupInfo selectedGroup = cxCapacityAllocationHandler.getHighestGroupPlan(productionContext, effectiveList, excludeSelectTopMap);
            if (null == selectedGroup) {
                break;
            }
            String groupName = selectedGroup.getGroupName();
            //最小分配天数
            Integer minAllocationDays = selectedGroup.getMinAllocationDays(productionContext);
            Integer leftOverDays = selectedGroup.getLeftOverNeedAllocationDays();
            //20260206 小于最短上机天数，则不进行分配
            if (!selectedGroup.isNextAllocation(leftOverDays, productionContext)) {
                if (leftOverDays > BigDecimal.ZERO.intValue()) {
                    log.info(TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, true, leftOverDays, minAllocationDays));
                }
                selectedGroup.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
                excludeSelectTopMap.add(groupName);
                continue;
            }
            topList.add(selectedGroup);
            index = index + BigDecimal.ONE.intValue();
        }
        return topList;
    }

    /**
     * 按GroupCxMachinePriorityEnum中的顺序获取Top3对应的选中的排产机台
     *
     * @param context 排产上下文
     * @param topList 预排分组信息
     * @param isFixed 是否固定
     * @return
     */
    private List<GroupPrioritySchedulerResultHelper> getGroupSelectedCxMachine(Context context, List<ProductionPlanGroupInfo> topList, boolean isFixed) {
        if (CollectionUtils.isEmpty(topList)) {
            return Collections.emptyList();
        }
        List<GroupPrioritySchedulerResultHelper> resultList = Lists.newArrayList();
        topList.forEach(topGroup -> {
            CxMachineBaseInfoVo preSelectedCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlanAppoint(context, topGroup, null, isFixed);
            if (null != preSelectedCxMachine) {
                GroupPrioritySchedulerResultHelper singleResult = new GroupPrioritySchedulerResultHelper(topGroup, preSelectedCxMachine);
                resultList.add(singleResult);
            }
        });
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        return resultList;
    }

    /**
     * 从合适的预排分组中，挑选最合适的一个分组进行排产分配
     *
     * @param context              排产上下文
     * @param topPreProductionList top预排列表
     * @return
     */
    private GroupPrioritySchedulerResultHelper getAppointGroupPlanProduction(Context context, List<GroupPrioritySchedulerResultHelper> topPreProductionList) {
        if (CollectionUtils.isEmpty(topPreProductionList)) {
            return null;
        }
        if (topPreProductionList.size() == BigDecimal.ONE.intValue()) {
            return topPreProductionList.get(BigDecimal.ZERO.intValue());
        }
        //排序：优先级级别(值越低越在前)->差值小的
        Comparator sortComparator = Comparator.comparing(GroupPrioritySchedulerResultHelper::getSelectedPriorityValue)
                .thenComparing(GroupPrioritySchedulerResultHelper::getSelectedPriorityDiffValue);
        topPreProductionList.sort(sortComparator);
        return topPreProductionList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 从finalSelected的匹配组中
     * 将selectedGroup在selectedCxMachine进行产能分配
     * 并返回分配结果
     *
     * @param productionContext 排产上下文
     * @param finalSelected     分组+机台匹配组信息
     * @return
     */
    private CxMachineAllocationPlanHelper buildAllocationDetailInfo(TbrProductionContext productionContext, GroupPrioritySchedulerResultHelper finalSelected) {
        CxMachineBaseInfoVo selectedCxMachine = finalSelected.getSelectedCxMachine();
        ProductionPlanGroupInfo addNewGroupPlan = finalSelected.getSelectedGroup();
        String groupName = addNewGroupPlan.getGroupName();
        Integer leftOverDays = addNewGroupPlan.getLeftOverNeedAllocationDays();
        Set<Integer> hasProductionDaySet = finalSelected.getSelectedProductionDaySet();
        Integer startDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
        //20260121 切换结构控制
        DayCapacityLimitVo dayCapacityLimitVo = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Integer realChangeDay = dayCapacityLimitVo.confirmStartDayByChangeGroup(productionContext, startDay, groupName, selectedCxMachine, hasProductionDaySet);
        if (null == realChangeDay) {
            return null;
        }
        ProductGroupCxCapacityInfo lhRatioInfo = addNewGroupPlan.getLhRatioByCxMachine(selectedCxMachine);
        startDay = realChangeDay;
        Set<Integer> realProductionDaySet = hasProductionDaySet.stream().filter(singleDay -> singleDay >= realChangeDay).collect(Collectors.toSet());
        Integer remainingDays = realProductionDaySet.size();
        //分配产能
        Integer needAllocationDays = addNewGroupPlan.getRemainingNeedAllocationDays();
        Integer monthMaxDays = productionContext.getMonthDays();
        //20260209 特殊材料是否需要拉量或是舍弃
        CxMachineAllocationPlanHelper calculationAllocation = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, leftOverDays, startDay, monthMaxDays);
        Integer confirmNeedAllocationDays = specialMaterialScheduleHandler.calculateConfirmAllocationDaysBySpecialMaterial(calculationAllocation, productionContext, addNewGroupPlan);
        if (null == confirmNeedAllocationDays || confirmNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            log.info(TbrProductionGroupLogRecorder.addSpecialMaterialStockLimitLog(productionContext, groupName, true));
            return null;
        }
        needAllocationDays = Math.max(needAllocationDays, confirmNeedAllocationDays);
        Integer realAllocationDays = Math.min(remainingDays, needAllocationDays);
        //更新剩余天数：分组的剩余天数、成型机台剩余可分配天数
        addNewGroupPlan.updateLeftOverNeedAllocationDays(realAllocationDays);
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, realAllocationDays, startDay, monthMaxDays);
        CxMachineAllocationPlanHelper beforeAllocation = selectedCxMachine.addAllocationPlanInfo(productionContext, addHelper);
        //20260429+ 前分组因每日切换限制需要延长收尾
        groupTimeExtensionHandler.handlerTimeExtensionDayConclusion(productionContext, beforeAllocation);
        return addHelper;
    }

}
