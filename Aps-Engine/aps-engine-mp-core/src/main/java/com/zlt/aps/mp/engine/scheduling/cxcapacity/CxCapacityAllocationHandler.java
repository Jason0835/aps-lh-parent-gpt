package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.basedata.assemble.history.ProductionHistoryHandler;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.daylimit.GroupCapacityProductionLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.handler.CxMachinePrioritySelector;
import com.zlt.aps.mp.engine.handler.GroupPlanCxMachineSelector;
import com.zlt.aps.mp.engine.handler.GroupPlanPrioritySelector;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSpecialMaterialProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型产能分配处理业务类--相当于工具类
 *
 * @author ZLT
 * @date 20251215
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CxCapacityAllocationHandler {

    private final ProductionHistoryHandler productionHistoryHandler;

    private final CxMouldProductionHandler cxMouldProductionHandler;

    private final GroupPlanPrioritySelector groupPlanPrioritySelector;

    private final CxMachinePrioritySelector cxMachinePrioritySelector;

    private final SpecialMaterialScheduleHandler specialMaterialScheduleHandler;

    /**
     * 对成型机台创建分配集合对象-按最小硫化配比分配
     *
     * @param cxMachineBaseInfo 成型机台信息
     * @param lhRatio           硫化配比信息
     * @param groupPlanInfo     分配的分组计划
     * @param continueSkuMap    续作规格信息
     * @param allocationDay     分配天数
     * @param startDay          起始天数
     * @param monthDays         月份最大天数
     * @return
     */
    public static CxMachineAllocationPlanHelper createAllocationPlanHelper(CxMachineBaseInfoVo cxMachineBaseInfo, ProductGroupCxCapacityInfo lhRatio, ProductionPlanGroupInfo groupPlanInfo, Map<String, CxContinueSkuInfoHelper> continueSkuMap, Integer allocationDay, Integer startDay, Integer monthDays) {
        Integer startAllocationDay = monthDays;
        Integer endAllocationDay = BigDecimal.ZERO.intValue();
        Set<Integer> stopDayInfo = cxMachineBaseInfo.getStopDayInfo();
        if (null == stopDayInfo) {
            stopDayInfo = new HashSet<>();
        }
        //分配的天数
        int index = BigDecimal.ZERO.intValue();
        Integer day = startDay + index;
        for (; index < allocationDay && day <= monthDays; ) {
            //停产日
            if (stopDayInfo.contains(day)) {
                day = day + BigDecimal.ONE.intValue();
                continue;
            }
            //超出月份周期
            if (day > monthDays) {
                break;
            }
            if (startAllocationDay > day) {
                startAllocationDay = day;
            }
            if (day > endAllocationDay) {
                endAllocationDay = day;
            }
            index = index + BigDecimal.ONE.intValue();
            day = day + BigDecimal.ONE.intValue();
        }
        if (null == continueSkuMap) {
            continueSkuMap = new HashMap<>();
        }
        //如果分配结束点 + 停产 = 周期天数，则分配结束点调整到最末
        if (endAllocationDay + stopDayInfo.size() == monthDays) {
            endAllocationDay = monthDays;
        }
        return new CxMachineAllocationPlanHelper(cxMachineBaseInfo.getCxMachineCode(), groupPlanInfo, lhRatio, continueSkuMap, allocationDay, startAllocationDay, endAllocationDay);
    }

    /**
     * 对结构收尾的成型机台反向挑选合适的结构上机
     * 收尾成型机台的剩余产能能覆盖挑选的结构剩余排产净需求
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组结构需求
     */
    public void reverseMachineAllocation(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        if (CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            //todo 记录日志
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取收尾机台信息
        Set<String> reverseFindSet = productionContext.getReverseFindSet();
        if (CollectionUtils.isEmpty(reverseFindSet)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addNoContinueGroupReverseProductionLog(context));
            return;
        }
        List<CxMachineBaseInfoVo> reverseCxMachineList = new ArrayList<>();
        reverseFindSet.forEach(cxMachineCode -> reverseCxMachineList.add(productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode)));
        if (CollectionUtils.isEmpty(reverseCxMachineList)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoExistBaseInfoLog(context));
            return;
        }
        //收尾机台-剔除空出来的机台
        List<CxMachineBaseInfoVo> endingCxMachineList = reverseCxMachineList.stream().filter(cxMachineInfo -> !CollectionUtils.isEmpty(cxMachineInfo.getAllocationList())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(endingCxMachineList)) {
            //todo 记录日志
            return;
        }
        String reverseAllCxMachineInfo = endingCxMachineList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.joining(StringConstant.COMMA));
        log.info(TbrProductionGroupLogRecorder.addInProductionMachinesInfoLog(productionContext, reverseAllCxMachineInfo));
        // 对收尾机台排序
        cxMachinePrioritySelector.sortReverseCxMachineList(productionContext, endingCxMachineList);
        //一台一台反向挑选合适的结构分组计划
        endingCxMachineList.forEach(reverseCxMachineInfo -> selectedGroupPlanByCxMachine(productionContext, estimateGroupCxAllocationMap, reverseCxMachineInfo, new HashSet<>()));
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

    /**
     * 成型产能机台反向挑选合适的结构
     * 剩余产能要能覆盖计划排产净需求
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划
     * @param cxMachineInfo                成型产能信息
     * @param excludeGroupPlan             不再参与的分组
     */
    public void selectedGroupPlanByCxMachine(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo, Set<String> excludeGroupPlan) {
        //获取合适优先级的一个结构
        ProductionPlanGroupInfo allocationGroupPlan = getSelectedGroup(context, estimateGroupCxAllocationMap, cxMachineInfo, excludeGroupPlan);
        if (null == allocationGroupPlan) {
            //记录日志
            TbrProductionGroupLogRecorder.addReverseCxMachineNoFindMatchPlanLog(context, cxMachineInfo);
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = allocationGroupPlan.getGroupName();
        Integer startDay = cxMachineInfo.getNextStartDay();
        //20260329 机台反选不再要求产能覆盖，故而不能直接取需求剩余天数allocationGroupPlan.getLeftOverNeedAllocationDays()
        Integer leftOverDays = allocationGroupPlan.getMachineReverseAllocationDays();
        ProductGroupCxCapacityInfo lhRatioInfo = allocationGroupPlan.getLhRatioByCxMachine(cxMachineInfo);
        //20260209 特殊材料是否需要拉量或是舍弃
        CxMachineAllocationPlanHelper calculationAllocation = createAllocationPlanHelper(cxMachineInfo, lhRatioInfo, allocationGroupPlan, null, leftOverDays, startDay, context.getMonthDays());
        Integer confirmNeedAllocationDays = specialMaterialScheduleHandler.calculateConfirmAllocationDaysBySpecialMaterial(calculationAllocation, productionContext, allocationGroupPlan);
        if (null == confirmNeedAllocationDays || confirmNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            TbrProductionGroupLogRecorder.addSpecialMaterialStockLimitLog(productionContext, groupName, false);
            allocationGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo, excludeGroupPlan);
            return;
        }
        leftOverDays = confirmNeedAllocationDays;
        //20260206 结构剩余需分配天数小于最短上机天数，则标记分配完成，查找下一个
        Integer minAllocationDays = allocationGroupPlan.getMinAllocationDays(productionContext);
        if (!allocationGroupPlan.isNextAllocation(leftOverDays, productionContext)) {
            TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, false, leftOverDays, minAllocationDays);
            allocationGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo, excludeGroupPlan);
            return;
        }
        TbrProductionGroupLogRecorder.addReverseCxMachineSelectedGroupPlanLog(context, cxMachineInfo, allocationGroupPlan);
        //重新计算分配的起始时间
        Set<Integer> hasProductionDaySet = cxMachineInfo.confirmProductionRange(context, allocationGroupPlan);
        Integer realStartDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
        startDay = Math.max(startDay, realStartDay);
        //20260121 切换结构的控制
        DayCapacityLimitVo dayCapacityLimitVo = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Integer realChangeDay = dayCapacityLimitVo.confirmStartDayByChangeGroup(productionContext, startDay, groupName, cxMachineInfo, hasProductionDaySet);
        if (null == realChangeDay) {
            //记录日志
            Integer maxChangeLimit = productionContext.getBaseDataContainer().getParamConfiguration().getDayChangeGroupCount();
            TbrProductionGroupLogRecorder.addChangeGroupLimitCxMachineLog(context, cxMachineInfo.getCxMachineCode(), maxChangeLimit);
            return;
        }
        startDay = realChangeDay;
        //20260209 采用新的分配天数
        Integer needAllocationDays = confirmNeedAllocationDays;
        CxMachineAllocationPlanHelper addHelper = createAllocationPlanHelper(cxMachineInfo, lhRatioInfo, allocationGroupPlan, null, needAllocationDays, startDay, context.getMonthDays());
        cxMachineInfo.addAllocationPlanInfo(context, addHelper);
        allocationGroupPlan.updateLeftOverNeedAllocationDays(needAllocationDays);
        //20260109 标记分配完成--不能标记分配完成，有可能因提前收尾导致需要在其它机台进行分配 对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, cxMachineInfo.getCxMachineCode(), addHelper, new HashSet<>());
        //20260322 剩余时间-有可能因提前收尾，导致时间变化
        Integer leftOver = cxMachineInfo.getRemainCapacity();
        //还有剩余产能，继续挑选下一个分组结构
        if (leftOver > BigDecimal.ZERO.intValue()) {
            excludeGroupPlan.add(groupName);
            TbrProductionGroupLogRecorder.addReverseCxMachineFindNextGroupPlanLog(context, cxMachineInfo);
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo, excludeGroupPlan);
        }
    }

    /**
     * 获取新增分组计划上机 --新增结构
     * 1、高优先级SKU个数多的优先
     * 2、2副模具共用受限，则结构总净需求小的优先
     * 3、特殊种类SKU个数多的优先
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划集合
     * @param excludeGroupPlan             需要排除的分组计划
     * @return
     */
    public ProductionPlanGroupInfo getInsertNewGroupPlan(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, Set<String> excludeGroupPlan) {
        if (CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> allGroupPlanList = new ArrayList<>(estimateGroupCxAllocationMap.values());
        if (CollectionUtils.isEmpty(allGroupPlanList)) {
            return null;
        }
        List<ProductionPlanGroupInfo> leftOverGroupList = allGroupPlanList.stream().filter(singleGroup -> !excludeGroupPlan.contains(singleGroup.getGroupName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverGroupList)) {
            return null;
        }
        String leftOverGroupInfo = leftOverGroupList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.joining(StringConstant.COMMA));
        TbrProductionGroupLogRecorder.addNeedSelectedCxMachineGroupLog(context, leftOverGroupInfo);
        List<ProductionPlanGroupInfo> needProductionGroupList = leftOverGroupList.stream().filter(groupPlan -> groupPlan.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needProductionGroupList)) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        // 如果有在机的特殊结构，则优先取出特殊结构
        boolean onLineCxMachineProductionSpecialStructure = productionContext.getBaseDataContainer().getCxMachineBaseInfo().values().stream().anyMatch(machine -> this.hasSpecialStructure(machine));
        ProductionPlanGroupInfo selected = null;
        if (onLineCxMachineProductionSpecialStructure) {
            TbrSpecialMaterialProductionLogRecorder.addProductionSpecialMaterialInfoLog(productionContext, "在产机台中有排产");
            selected = groupPlanPrioritySelector.getHeightPriorityGroupBySpecialMaterial(productionContext, excludeGroupPlan);
        }
        if (null != selected) {
            return selected;
        }
        TbrProductionGroupLogRecorder.addSelectedHighestNoSpecialMaterialGroupLog(context);
        return groupPlanPrioritySelector.getHeightPriorityGroup(productionContext, excludeGroupPlan);
    }

    /**
     * 对分组(结构)计划，挑选合适成型机台
     * 需要考虑成型工装的匹配
     *
     * @param context         排产上下文
     * @param addNewGroupPlan 排产分组计划
     * @return
     */
    public CxMachineBaseInfoVo selectedCxMachineForGroupPlan(Context context, ProductionPlanGroupInfo addNewGroupPlan) {
        if (null == addNewGroupPlan) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String structureName = addNewGroupPlan.getGroupName();
        TbrProductionGroupLogRecorder.addHighestGroupSelectedCxMachineLog(productionContext, structureName);
        //获取分组及零度零度供料架
        String isZeroRack = addNewGroupPlan.getIsZero();
        //挑选机台
        List<CxMachineBaseInfoVo> enableCxMachineList = GroupPlanCxMachineSelector.getEnableBaseCxMachineList(context, addNewGroupPlan);
        if (CollectionUtils.isEmpty(enableCxMachineList)) {
            TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, structureName);
            return null;
        }
        //20260120 挑选排产日有交集的，结合成型工装数量-成型鼓，日产能上限
        List<CxMachineBaseInfoVo> hasProductionDayList = enableCxMachineList.stream().filter(singleMachine -> selectEnableMachineAndSetInfo(productionContext, addNewGroupPlan, singleMachine)
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionDayList)) {
            return null;
        }
        List<CxMachineBaseInfoVo> capacityCoverageList = hasProductionDayList.stream().filter(singleMachine -> singleMachine.getCapacityDiffValue() >= BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        List<CxMachineBaseInfoVo> selectedCapacityList;
        if (!CollectionUtils.isEmpty(capacityCoverageList) && !addNewGroupPlan.isSpecialMaterial()) {
            //产能能覆盖且非特殊结构，取差值最小
            Integer minProductionDays = capacityCoverageList.stream().mapToInt(CxMachineBaseInfoVo::getCapacityDiffValue).min().getAsInt();
            selectedCapacityList = capacityCoverageList.stream().filter(cxMachineInfo -> minProductionDays.equals(cxMachineInfo.getCapacityDiffValue())).collect(Collectors.toList());
        } else {
            //产能不能覆盖，取差值最大
            Integer maxProductionDays = hasProductionDayList.stream().mapToInt(CxMachineBaseInfoVo::getCapacityDiffValue).max().getAsInt();
            selectedCapacityList = hasProductionDayList.stream().filter(cxMachineInfo -> maxProductionDays.equals(cxMachineInfo.getCapacityDiffValue())).collect(Collectors.toList());
        }
        if (selectedCapacityList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo minProductionSelected = selectedCapacityList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addSelectedFinalByMaxCapacityMachineLog(context, structureName, isZeroRack, minProductionSelected.getCxMachineCode(), minProductionSelected.getCxMachineTypeCode());
            return minProductionSelected;
        }
        return cxMachinePrioritySelector.selectOptimalOneCxMachine(context, selectedCapacityList, addNewGroupPlan);
    }

    /**
     * 从canSelectedList机台中获取机台
     *
     * @param productionContext   排产上下文
     * @param canSelectedList     可选机台
     * @param productionGroupInfo 排产分组计划
     * @return
     */
    public CxMachineBaseInfoVo selectedCxMachineForGroupPlanByAppoint(TbrProductionContext productionContext, List<CxMachineBaseInfoVo> canSelectedList, ProductionPlanGroupInfo productionGroupInfo) {
        if (CollectionUtils.isEmpty(canSelectedList) || null == productionGroupInfo) {
            return null;
        }
        String structureName = productionGroupInfo.getGroupName();
        String isZeroRack = productionGroupInfo.getIsZero();
        //挑选排产日有交集的，结合成型工装数量-成型鼓，日产能上限
        List<CxMachineBaseInfoVo> hasProductionDayList = canSelectedList.stream().filter(singleMachine -> selectEnableMachineAndSetInfo(productionContext, productionGroupInfo, singleMachine)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionDayList)) {
            return null;
        }
        Integer needDays = productionGroupInfo.getLeftOverNeedAllocationDays();
        canSelectedList.forEach(single -> single.setLeftOverDays(productionContext));
        List<CxMachineBaseInfoVo> preSelectedList;
        List<CxMachineBaseInfoVo> passCapacityList = canSelectedList.stream().filter(single -> single.getLastCanProductionDays() >= needDays).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(passCapacityList)) {
            Integer minDays = passCapacityList.stream().mapToInt(CxMachineBaseInfoVo::getLastCanProductionDays).min().getAsInt();
            preSelectedList = passCapacityList.stream().filter(single -> minDays.equals(single.getLastCanProductionDays())).collect(Collectors.toList());
        } else {
            Integer maxDays = canSelectedList.stream().mapToInt(CxMachineBaseInfoVo::getLastCanProductionDays).max().getAsInt();
            preSelectedList = canSelectedList.stream().filter(single -> maxDays.equals(single.getLastCanProductionDays())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(preSelectedList)) {
            TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(productionContext, structureName);
            return null;
        }
        if (preSelectedList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo minProductionSelected = preSelectedList.get(BigDecimal.ZERO.intValue());
            TbrProductionGroupLogRecorder.addSelectedFinalByMaxCapacityMachineLog(productionContext, structureName, isZeroRack, minProductionSelected.getCxMachineCode(), minProductionSelected.getCxMachineTypeCode());
            return preSelectedList.get(BigDecimal.ZERO.intValue());
        }
        return cxMachinePrioritySelector.selectOptimalOneCxMachine(productionContext, preSelectedList, productionGroupInfo);
    }

    /**
     * 获取符合条件的成型机台，且设置对应的信息
     * 匹配结构+机台类型的成型工装、日产能限制
     * 1、历史生产信息
     * 2、产能
     *
     * @param productionContext 排产上下文
     * @param addNewGroupPlan   结构
     * @param singleMachine     预选机台
     * @return
     */
    public boolean selectEnableMachineAndSetInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo addNewGroupPlan, CxMachineBaseInfoVo singleMachine) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        //最小分配天数 20260209 特殊材料结构，将最小分配天数置为1
        Integer minAllocationDays = addNewGroupPlan.getMinAllocationDays(productionContext);
        Integer needDays = addNewGroupPlan.getLeftOverNeedAllocationDays();
        String groupName = addNewGroupPlan.getGroupName();
        /**
         * 20260120 判断成型鼓是否符合条件
         * 20260125 分配产能限制控制 1、成型工装数量 2、日产能上限
         */
        GroupCapacityProductionLimitHelper limitResult = baseDataContainer.getLeftOverProductionDayInfo(productionContext, addNewGroupPlan, singleMachine);
        //获取成型工装的排产日集合
        Set<Integer> productionDayInfo = limitResult.getProductionDaySet();
        if (!isReachMinAllocationDays(addNewGroupPlan, productionDayInfo, minAllocationDays)) {
            if (productionDayInfo.size() > BigDecimal.ZERO.intValue()) {
                TbrProductionGroupLogRecorder.addGroupNoReachMinAllocationDayLog(productionContext, groupName, productionDayInfo.size(), minAllocationDays);
            }
            return false;
        }
        Set<Integer> hasProductionDaySet = singleMachine.confirmProductionRange(productionContext, productionDayInfo);
        if (CollectionUtils.isEmpty(hasProductionDaySet)) {
            return false;
        }
        Integer capacityDays = hasProductionDaySet.size();
        if (capacityDays < minAllocationDays) {
            return false;
        }
        //设置历史信息
        singleMachine.setLastBoardingDate(BigDecimal.ZERO.intValue());
        singleMachine.setProductionCount(BigDecimal.ZERO.intValue());
        productionHistoryHandler.setCxMachineProductionGroupPlanHistory(productionContext, addNewGroupPlan, singleMachine);
        //设置产能
        singleMachine.setSelectedProductionDaySet(hasProductionDaySet);
        singleMachine.setSelectedProductionDys(capacityDays);
        Integer diffValue = capacityDays - needDays;
        singleMachine.setCapacityDiffValue(diffValue);
        return true;
    }

    /**
     * 获取产能可覆盖，机台可匹配的分组计划
     * 通过机台反向匹配计划
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 所有分组计划
     * @param cxMachineInfo                成型机台
     * @param excludeGroupPlan             需要排产的结构
     * @return
     */
    private ProductionPlanGroupInfo getSelectedGroup(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo, Set<String> excludeGroupPlan) {
        if (null == cxMachineInfo || CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            //todo 记录日志
            return null;
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //记录日志 空机台不是收尾
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoExistBaseInfoLog(context, cxMachineInfo));
            return null;
        }
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        if (remainingDays <= BigDecimal.ZERO.intValue()) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoRemainingCapacityLog(context, cxMachineInfo));
            return null;
        }
        //获取能够在该成型上生产的结构清单
        Map<String, ProductionPlanGroupInfo> canProductionGroupMap = getCanProductionGroup(context, estimateGroupCxAllocationMap, cxMachineInfo, excludeGroupPlan);
        if (CollectionUtils.isEmpty(canProductionGroupMap)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoFindCapacityPlanLog(context, cxMachineInfo));
            return null;
        }
        //剔除不可匹配的结构信息（不可作业的结构或是SKU需要剔除,零度供料架）
        Map<String, ProductionPlanGroupInfo> enableGroupPlanMap = excludeDisable(context, canProductionGroupMap, cxMachineInfo);
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoFindMatchPlanLog(context, cxMachineInfo));
            return null;
        }
        //获取合适优先级的一个结构
        return groupPlanPrioritySelector.selectOptimalOneGroup(context, enableGroupPlanMap, cxMachineInfo);
    }

    /**
     * 得到成型机台能够排产的清单
     * 此时会结合成型工装的数量
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组结构计划集合
     * @param cxMachineInfo                成型机信息
     * @param excludeGroupPlan             需要排产的分组计划
     * @return
     */
    private Map<String, ProductionPlanGroupInfo> getCanProductionGroup(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo, Set<String> excludeGroupPlan) {
        if (null == cxMachineInfo || CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return Collections.emptyMap();
        }
        //成型机剩余产能能覆盖剩余排产净需求
        Map<String, ProductionPlanGroupInfo> capacityCoverageMap = new HashMap<>(estimateGroupCxAllocationMap.size());
        estimateGroupCxAllocationMap.forEach((structureName, groupPlan) -> {
            if (CollectionUtils.isEmpty(excludeGroupPlan)) {
                calcDiffCapacityAndAddMap(context, capacityCoverageMap, structureName, groupPlan, cxMachineInfo);
                return;
            }
            if (excludeGroupPlan.contains(structureName)) {
                return;
            }
            calcDiffCapacityAndAddMap(context, capacityCoverageMap, structureName, groupPlan, cxMachineInfo);
        });
        return capacityCoverageMap;
    }

    /**
     * 剔除不匹配的结构
     * 不可作业结构/SKU,零度不匹配
     *
     * @param context             排产上下文
     * @param capacityCoverageMap 产能覆盖的分组计划
     * @param cxMachineInfo       收尾机台
     * @return
     */
    private Map<String, ProductionPlanGroupInfo> excludeDisable(Context context, Map<String, ProductionPlanGroupInfo> capacityCoverageMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (CollectionUtils.isEmpty(capacityCoverageMap) || null == cxMachineInfo) {
            return Collections.emptyMap();
        }
        Map<String, ProductionPlanGroupInfo> enableProductionMap = new HashMap<>(capacityCoverageMap.size());
        capacityCoverageMap.forEach((structureName, groupPlan) -> {
            boolean isBaseSelected = GroupPlanCxMachineSelector.isMatch(context, groupPlan, cxMachineInfo);
            if (!isBaseSelected) {
                return;
            }
            enableProductionMap.put(structureName, groupPlan);
        });
        return enableProductionMap;
    }

    /**
     * 机台反向挑选结构，将成型工装也满足的结构加入到capacityCoverageMap集合中
     * 同时计算结构需求与成型产能的差异
     *
     * @param context             排产上下文
     * @param capacityCoverageMap 需要加入的集合
     * @param structureName       结构名
     * @param groupPlan           分组计划信息
     * @param cxMachineInfo       成型机台
     */
    private void calcDiffCapacityAndAddMap(Context context, Map<String, ProductionPlanGroupInfo> capacityCoverageMap, String structureName, ProductionPlanGroupInfo groupPlan, CxMachineBaseInfoVo cxMachineInfo) {
        Integer minLhDayCapacityQty = groupPlan.getMinLhDayCapacityQty();
        if (null == minLhDayCapacityQty || minLhDayCapacityQty <= BigDecimal.ZERO.longValue()) {
            //todo 记录日志
            return;
        }
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        Set<String> fixedCxMachineSet = Optional.ofNullable(groupPlan.getFixedCxMachineSet()).orElse(Collections.emptySet());
        if (!CollectionUtils.isEmpty(fixedCxMachineSet) && !fixedCxMachineSet.contains(cxMachineCode)) {
            String fixedMachineInfo = String.join(StringConstant.COMMA, fixedCxMachineSet);
            TbrProductionGroupLogRecorder.addGroupNoSelectedForFixedCxMachineLog(context, structureName, cxMachineCode, fixedMachineInfo);
            return;
        }
        Map<String, MonthPlanStructureLhRatioVo> lhRatioMap = groupPlan.getCxMachineLhRationMap();
        if (CollectionUtils.isEmpty(lhRatioMap)) {
            //todo 记录日志
            return;
        }
        MonthPlanStructureLhRatioVo lhRatio = groupPlan.getLhRatio(cxMachineInfo);
        if (null == lhRatio) {
            //todo 记录日志
            return;
        }
        Integer ratio = lhRatio.getLhMachineMaxQty();
        if (null == ratio || ratio <= BigDecimal.ZERO.intValue()) {
            //todo 记录日志
            return;
        }
        //记录配比-需要传递
        cxMachineInfo.setRatio(ratio);
        //20260109--先采用天数来判断，因剩余未排产量存在模具受限的干扰
        Integer remainingNeedDays = groupPlan.getRemainingNeedAllocationDays();
        if (remainingNeedDays <= BigDecimal.ZERO.intValue()) {
            //todo 记录日志
            return;
        }
        //20260120 真实可排产日，成型工装-成型鼓 日产能上限控制
        Set<Integer> hasProductionSet = cxMachineInfo.confirmProductionRange(context, groupPlan);
        if (CollectionUtils.isEmpty(hasProductionSet)) {
            return;
        }
        //成型剩余产能
        Integer realRemainingDays = hasProductionSet.size();
        TbrProductionGroupLogRecorder.addReverseCxMachineMatchCapacityLog(context, cxMachineInfo, realRemainingDays, structureName, remainingNeedDays);
        /*if (realRemainingDays < remainingNeedDays) {
            return;
        }*/
        //结构需求与机台产能差异天数 sandy+ 2026.3.29
        groupPlan.setDiffStructureAndMachineDays(remainingNeedDays - realRemainingDays);
        groupPlan.setMachineReverseAllocationDays(Math.min(remainingNeedDays, realRemainingDays));
        capacityCoverageMap.put(structureName, groupPlan);
    }

    /**
     * 可满足的排产天信息
     *
     * @param groupPlanInfo     分组计划信息
     * @param productionDayInfo 工装排产天数
     * @param minAllocationDays 最小排产天数
     * @return
     */
    private boolean isReachMinAllocationDays(ProductionPlanGroupInfo groupPlanInfo, Set<Integer> productionDayInfo, Integer minAllocationDays) {
        if (CollectionUtils.isEmpty(productionDayInfo)) {
            return false;
        }
        if (groupPlanInfo.isSpecialMaterial()) {
            return true;
        }
        return productionDayInfo.size() >= minAllocationDays;
    }

}
