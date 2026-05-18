package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSimulateProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxCapacityAllocationHandler;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxMouldProductionHandler;
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
     * @param context                       排产上下文
     * @param excludeGroupPlan              需要剔除的分组(中间过程中找不到机台等情形)-初始空集合
     * @param preSelectedGroupSet           计划可分配产能的分组-初始空集合
     * @param preSelectedGroupAllocationMap 计划可分配产能的分组分配情况-初始空集合
     * @param discontinueGroupSet           有间断的分组对象集合
     */
    public void allocationCxMachine(Context context, Set<String> excludeGroupPlan, Set<String> preSelectedGroupSet, Map<String, Set<CxMachineAllocationPlanHelper>> preSelectedGroupAllocationMap, Set<String> discontinueGroupSet) {
        //1、获取还需排产分组的当前Top列表
        List<ProductionPlanGroupInfo> topList = getTopList(context, excludeGroupPlan);
        if (CollectionUtils.isEmpty(topList)) {
            return;
        }
        //2、获取各分组对应匹配的合适机台
        List<GroupPrioritySchedulerResultHelper> topSelectedCxMachineList = getGroupSelectedCxMachine(context, topList, false, discontinueGroupSet);
        if (CollectionUtils.isEmpty(topSelectedCxMachineList)) {
            excludeGroupPlan.addAll(topList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            allocationCxMachine(context, excludeGroupPlan, preSelectedGroupSet, preSelectedGroupAllocationMap, discontinueGroupSet);
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
        CxMachineAllocationPlanHelper addHelper = buildAllocationDetailInfo(productionContext, finalSelected);
        if (null == addHelper) {
            excludeGroupPlan.add(groupName);
            allocationCxMachine(context, excludeGroupPlan, preSelectedGroupSet, preSelectedGroupAllocationMap, discontinueGroupSet);
            return;
        }
        //5、对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper, new HashSet<>());
        //重新获取剩余天数：可能因提前收尾变化，导致计划实际没有排，下轮直接排除,不能设置分配完成
        Integer newNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        if (newNeedAllocationDaysByGroupPlan.equals(originNeedAllocationDaysByGroupPlan)) {
            excludeGroupPlan.add(groupName);
        } else {
            preSelectedGroupSet.add(groupName);
            Set<CxMachineAllocationPlanHelper> preAllocationSet = preSelectedGroupAllocationMap.get(groupName);
            if (null == preAllocationSet) {
                preAllocationSet = Sets.newHashSet();
                preSelectedGroupAllocationMap.put(groupName, preAllocationSet);
            }
            preAllocationSet.add(addHelper);
            excludeGroupPlan.clear();
        }
        //20260429+ 前分组分配是否需要延长处理
        cxMouldProductionHandler.handlerTimeExtensionDayConclusionByBeforeGroup(productionContext, addHelper);
        //下一批
        allocationCxMachine(context, excludeGroupPlan, preSelectedGroupSet, preSelectedGroupAllocationMap, discontinueGroupSet);
    }

    /**
     * 对预期排产分组中的固定机台分组进行排产选机台
     *
     * @param context                  排产上下文
     * @param excludeGroupPlan         需要剔除的分组
     * @param appointPriorityGroupList 指定的优先级分组对象集合
     * @param discontinueGroupSet      有间断排产的分组对象集合
     * @param isFixed                  是否固定选机台
     * @return
     */
    public void productionAppointGroupCxMachine(Context context, Set<String> excludeGroupPlan, List<ProductionPlanGroupInfo> appointPriorityGroupList, Set<String> discontinueGroupSet, boolean isFixed) {
        if (CollectionUtils.isEmpty(appointPriorityGroupList)) {
            return;
        }
        //1、获取还需排产分组的当前Top列表
        List<ProductionPlanGroupInfo> topFixedCxMachineList = getTopListByRange(context, appointPriorityGroupList, excludeGroupPlan);
        if (CollectionUtils.isEmpty(topFixedCxMachineList)) {
            return;
        }
        //2、获取对应的指定机台
        List<GroupPrioritySchedulerResultHelper> topSelectedCxMachineList = getGroupSelectedCxMachine(context, topFixedCxMachineList, isFixed, discontinueGroupSet);
        if (CollectionUtils.isEmpty(topSelectedCxMachineList)) {
            excludeGroupPlan.addAll(topFixedCxMachineList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet()));
            productionAppointGroupCxMachine(context, excludeGroupPlan, appointPriorityGroupList, discontinueGroupSet, isFixed);
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
        CxMachineAllocationPlanHelper addHelper = buildAllocationDetailInfo(productionContext, finalSelected);
        if (null == addHelper) {
            excludeGroupPlan.add(groupName);
            productionAppointGroupCxMachine(context, excludeGroupPlan, appointPriorityGroupList, discontinueGroupSet, isFixed);
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
        //20260429+ 前分组分配是否需要延长处理
        cxMouldProductionHandler.handlerTimeExtensionDayConclusionByBeforeGroup(productionContext, addHelper);
        //下一批
        productionAppointGroupCxMachine(context, excludeGroupPlan, appointPriorityGroupList, discontinueGroupSet, isFixed);
    }

    /**
     * 对有设置固定机台的分组，当预分配到同一机台时，预分配起始时间在前的分组先排
     * 1、对有设置固定的分组appointPriorityGroupList，获取其预分配的机台
     * 2、当预分配的机台有多个分组时，判断其各预分配的时间
     * 3、预分配时间在前的分组先进行排产
     *
     * @param context                  排产上下文
     * @param excludeGroupPlan         需要剔除的分组信息
     * @param appointPriorityGroupList 指定的固定分组信息
     * @param discontinueGroupSet      有间断的分组
     * @return
     */
    public void allocationFixedGroupSameCxMachineEarlyGroup(Context context, Set<String> excludeGroupPlan, List<ProductionPlanGroupInfo> appointPriorityGroupList, Set<String> discontinueGroupSet) {
        if (CollectionUtils.isEmpty(appointPriorityGroupList)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> excludeGroupSet = Sets.newHashSet();
        if (!CollectionUtils.isEmpty(excludeGroupPlan)) {
            excludeGroupSet.addAll(excludeGroupPlan);
        }
        List<ProductionPlanGroupInfo> effectiveList = appointPriorityGroupList.stream().filter(singleGroup -> !excludeGroupSet.contains(singleGroup.getGroupName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return;
        }
        //获取预选中为同一台的固定结构中，预分配起始时间最早的分组信息
        List<GroupPrioritySchedulerResultHelper> earlyList = getFixedGroupSameCxMachineEarlyGroupInfo(context, effectiveList, discontinueGroupSet);
        if (CollectionUtils.isEmpty(earlyList)) {
            return;
        }
        String groupInfo = earlyList.stream().map(GroupPrioritySchedulerResultHelper::getPreSelectedGroupName).collect(Collectors.joining(StringConstant.COMMA));
        TbrSimulateProductionLogRecorder.addDeliveryPriorityTypeLog(context, groupInfo, "指定分组分配同机台时，时间在前优先排产");
        earlyList.forEach(preSelectedInfo -> {
            //对挑选出来的分组，进行机台产能分配
            CxMachineBaseInfoVo selectedCxMachine = preSelectedInfo.getSelectedCxMachine();
            ProductionPlanGroupInfo addNewGroupPlan = preSelectedInfo.getSelectedGroup();
            String groupName = addNewGroupPlan.getGroupName();
            Integer originNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
            CxMachineAllocationPlanHelper addHelper = buildAllocationDetailInfo(productionContext, preSelectedInfo);
            if (null == addHelper) {
                excludeGroupPlan.add(groupName);
                return;
            }
            //对成型机台进行模拟模具排产
            cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper, new HashSet<>());
            //重新获取剩余天数：可能因提前收尾变化，导致计划实际没有排，下轮直接排除,不能设置分配完成
            Integer newNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
            if (newNeedAllocationDaysByGroupPlan.equals(originNeedAllocationDaysByGroupPlan)) {
                excludeGroupPlan.add(groupName);
            } else {
                excludeGroupPlan.remove(groupName);
            }
        });
        //重新分配
        allocationFixedGroupSameCxMachineEarlyGroup(context, excludeGroupPlan, appointPriorityGroupList, discontinueGroupSet);
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
     * @param groupRange       指定的排产分组
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
     * 获取固定结构分配到同一成型机台，超过多个固定结构时，预分配时间在前的固定结构信息
     * 因时间在前的先排(防止因工装限制导致的结构二次分配)
     * 1、先获取有指定固定机台的分组，预分配的机台信息
     * 2、提取预分配机台对应的分组，超过1个时(即同机台有多个分组竞争)
     * 3、对超过1个的分组的机台进行预分配，提取起始时间最早的分组信息
     *
     * @param context             排产上下文
     * @param effectiveList       有效结构(有指定固定机台结构)
     * @param discontinueGroupSet 有间断结构信息
     * @return
     */
    private List<GroupPrioritySchedulerResultHelper> getFixedGroupSameCxMachineEarlyGroupInfo(Context context, List<ProductionPlanGroupInfo> effectiveList, Set<String> discontinueGroupSet) {
        Set<String> realDiscontinueGroupSet;
        if (CollectionUtils.isEmpty(discontinueGroupSet)) {
            realDiscontinueGroupSet = Collections.emptySet();
        } else {
            realDiscontinueGroupSet = Sets.newHashSet();
            realDiscontinueGroupSet.addAll(discontinueGroupSet);
        }
        List<GroupPrioritySchedulerResultHelper> resultList = Lists.newArrayList();
        effectiveList.forEach(singleGroup -> {
            //如果是间断，则选时间最长的机台
            boolean isMoreProductionDay = realDiscontinueGroupSet.contains(singleGroup.getGroupName());
            CxMachineBaseInfoVo preSelectedCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlanAppoint(context, singleGroup, isMoreProductionDay, true);
            if (null != preSelectedCxMachine) {
                GroupPrioritySchedulerResultHelper singleResult = new GroupPrioritySchedulerResultHelper(singleGroup, preSelectedCxMachine);
                resultList.add(singleResult);
            }
        });
        if (CollectionUtils.isEmpty(resultList)) {
            return Collections.emptyList();
        }
        //如果选中的是同一台，则时间在前的先排
        Map<String, List<GroupPrioritySchedulerResultHelper>> cxMachinePreMap = resultList.stream().collect(Collectors.groupingBy(GroupPrioritySchedulerResultHelper::getSelectedCxMachineCode));
        Map<String, List<GroupPrioritySchedulerResultHelper>> multipleGroupMap = Maps.newHashMap();
        cxMachinePreMap.forEach((cxMachineCode, preGroupList) -> {
            if (CollectionUtils.isEmpty(preGroupList) || preGroupList.size() <= BigDecimal.ONE.intValue()) {
                return;
            }
            multipleGroupMap.put(cxMachineCode, preGroupList);
        });
        if (CollectionUtils.isEmpty(multipleGroupMap)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<GroupPrioritySchedulerResultHelper> earlyList = new ArrayList<>();
        multipleGroupMap.forEach((cxMachineCode, preGroupList) -> {
            List<CxMachineAllocationPlanHelper> addAllocationHelperList = Lists.newArrayList();
            Map<ProductionPlanGroupInfo, GroupPrioritySchedulerResultHelper> groupMap = Maps.newHashMap();
            preGroupList.forEach(preSingleGroup -> {
                CxMachineAllocationPlanHelper addPreHelper = getAllocationDetailInfo(productionContext, preSingleGroup);
                if (null == addPreHelper) {
                    return;
                }
                addAllocationHelperList.add(addPreHelper);
                groupMap.put(preSingleGroup.getSelectedGroup(), preSingleGroup);
            });
            if (CollectionUtils.isEmpty(addAllocationHelperList)) {
                return;
            }
            //20260516+ 起始时间不是多个时，则按指定Top3规则挑选
            Set<Integer> startDaySet = addAllocationHelperList.stream().map(CxMachineAllocationPlanHelper::getStartDay).collect(Collectors.toSet());
            if (startDaySet.size() <= BigDecimal.ONE.intValue()) {
                return;
            }
            addAllocationHelperList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getStartDay));
            earlyList.add(groupMap.get(addAllocationHelperList.get(BigDecimal.ZERO.intValue()).getProductionPlanInfo()));
        });
        if (CollectionUtils.isEmpty(earlyList)) {
            return Collections.emptyList();
        }
        return earlyList;
    }

    /**
     * 按GroupCxMachinePriorityEnum中的顺序获取Top3对应的选中的排产机台
     *
     * @param context             排产上下文
     * @param topList             预排分组信息
     * @param isFixed             是否固定
     * @param discontinueGroupSet 有间断的分组对象集合
     * @return
     * @pa
     */
    private List<GroupPrioritySchedulerResultHelper> getGroupSelectedCxMachine(Context context, List<ProductionPlanGroupInfo> topList, boolean isFixed, Set<String> discontinueGroupSet) {
        if (CollectionUtils.isEmpty(topList)) {
            return Collections.emptyList();
        }
        Set<String> realDiscontinueGroupSet = null == discontinueGroupSet ? Collections.emptySet() : discontinueGroupSet;
        List<GroupPrioritySchedulerResultHelper> resultList = Lists.newArrayList();
        topList.forEach(topGroup -> {
            //如果是间断，则选时间最长的机台
            boolean isMoreProductionDay = realDiscontinueGroupSet.contains(topGroup.getGroupName());
            CxMachineBaseInfoVo preSelectedCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlanAppoint(context, topGroup, isMoreProductionDay, isFixed);
            if (null != preSelectedCxMachine) {
                TbrSimulateProductionLogRecorder.addHeightPriorityMatchLog(context, topGroup.getGroupName(), preSelectedCxMachine);
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
        CxMachineAllocationPlanHelper addHelper = getAllocationDetailInfo(productionContext, finalSelected);
        if (null == addHelper) {
            return null;
        }
        CxMachineBaseInfoVo selectedCxMachine = finalSelected.getSelectedCxMachine();
        ProductionPlanGroupInfo addNewGroupPlan = finalSelected.getSelectedGroup();
        Integer realAllocationDays = addHelper.getAllocationDay();
        //更新剩余天数：分组的剩余天数、成型机台剩余可分配天数
        addNewGroupPlan.updateLeftOverNeedAllocationDays(realAllocationDays);
        //20260429+ 前分组分配信息传递，用于当需要前分组强制延长收尾时需要
        CxMachineAllocationPlanHelper beforeAllocation = selectedCxMachine.addAllocationPlanInfo(productionContext, addHelper);
        addHelper.setBeforeAllocationByChangeLimit(beforeAllocation);
        return addHelper;
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
    private CxMachineAllocationPlanHelper getAllocationDetailInfo(TbrProductionContext productionContext, GroupPrioritySchedulerResultHelper finalSelected) {
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
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, realAllocationDays, startDay, monthMaxDays);
        return addHelper;
    }

}
