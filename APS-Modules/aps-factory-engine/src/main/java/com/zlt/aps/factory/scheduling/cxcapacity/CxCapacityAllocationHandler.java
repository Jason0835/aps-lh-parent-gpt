package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.basedataassemble.history.ProductionHistoryHandler;
import com.zlt.aps.factory.daylimit.DayCapacityLimitVo;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.enums.GroupCxMachineSelectedTypeEnum;
import com.zlt.aps.factory.handler.GroupPlanCxMachineSelector;
import com.zlt.aps.factory.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
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
        // 对机台排序
        endingCxMachineList.sort((machine1, machine2) -> {
            // 最先给有特殊结构在机的机台挑选
            Boolean isSpecial1 = this.hasSpecialStructure(machine1);
            Boolean isSpecial2 = this.hasSpecialStructure(machine2);
            // Boolean的true比false大，倒序，优先处理true的
            int result = isSpecial2.compareTo(isSpecial1);
            if (result != 0) {
                return result;
            }
            // 其次最先收尾的先-剩余天数多的，倒序，越大的约优先
            Integer remainingDays1 = machine1.getRemainingDays();
            Integer remainingDays2 = machine2.getRemainingDays();
            result = remainingDays2.compareTo(remainingDays1);
            if (result != 0) {
                return result;
            }
            // 最后按机台编号顺序
            String code1 = machine1.getCxMachineCode();
            String code2 = machine2.getCxMachineCode();
            return code1.compareTo(code2);
        });
        //一台一台反向挑选合适的结构分组计划
        endingCxMachineList.forEach(reverseCxMachineInfo -> selectedGroupPlanByCxMachine(productionContext, estimateGroupCxAllocationMap, reverseCxMachineInfo));
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
     */
    public void selectedGroupPlanByCxMachine(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo) {
        //获取合适优先级的一个结构
        ProductionPlanGroupInfo allocationGroupPlan = getSelectedGroup(context, estimateGroupCxAllocationMap, cxMachineInfo);
        if (null == allocationGroupPlan) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoFindMatchPlanLog(context, cxMachineInfo));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = allocationGroupPlan.getGroupName();
        Integer startDay = cxMachineInfo.getNextStartDay();
        Integer leftOverDays = allocationGroupPlan.getLeftOverNeedAllocationDays();
        Integer remainingDays = cxMachineInfo.getRemainingDays();
        ProductGroupCxCapacityInfo lhRatioInfo = allocationGroupPlan.getLhRatioByCxMachine(cxMachineInfo);
        //20260209 特殊材料是否需要拉量或是舍弃
        CxMachineAllocationPlanHelper calculationAllocation = createAllocationPlanHelper(cxMachineInfo, lhRatioInfo, allocationGroupPlan, null, leftOverDays, startDay, context.getMonthDays());
        Integer confirmNeedAllocationDays = specialMaterialScheduleHandler.calculateConfirmAllocationDaysBySpecialMaterial(calculationAllocation, productionContext, allocationGroupPlan);
        if (null == confirmNeedAllocationDays || confirmNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            log.info(TbrProductionGroupLogRecorder.addSpecialMaterialStockLimitLog(productionContext, groupName, false));
            allocationGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo);
            return;
        }
        leftOverDays = confirmNeedAllocationDays;
        //20260206 结构剩余需分配天数小于最短上机天数，则标记分配完成，查找下一个
        Integer minAllocationDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
        if (!allocationGroupPlan.isNextAllocation(minAllocationDays)) {
            log.info(TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, false, leftOverDays, minAllocationDays));
            allocationGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo);
            return;
        }
        log.info(TbrProductionGroupLogRecorder.addReverseCxMachineSelectedGroupPlanLog(context, cxMachineInfo, allocationGroupPlan));
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
            log.info(TbrProductionGroupLogRecorder.addChangeGroupLimitCxMachineLog(context, cxMachineInfo.getCxMachineCode(), maxChangeLimit));
            return;
        }
        startDay = realChangeDay;
        //20260209 采用新的分配天数 allocationGroupPlan.getRemainingNeedAllocationDays();
        Integer needAllocationDays = confirmNeedAllocationDays;
        //剩余时间
        Integer leftOver = remainingDays - needAllocationDays;
        CxMachineAllocationPlanHelper addHelper = createAllocationPlanHelper(cxMachineInfo, lhRatioInfo, allocationGroupPlan, null, needAllocationDays, startDay, context.getMonthDays());
        cxMachineInfo.addAllocationPlanInfo(context, addHelper);
        //更新特殊材料库存
        ((TbrProductionContext) context).updateSpecialMaterialInfoMap(allocationGroupPlan, needAllocationDays);
        allocationGroupPlan.updateLeftOverNeedAllocationDays(needAllocationDays);
        //20260109 标记分配完成
        allocationGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
        //对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, cxMachineInfo.getCxMachineCode(), addHelper);
        //还有剩余产能，继续挑选下一个分组结构
        if (leftOver >= minAllocationDays) {
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineFindNextGroupPlanLog(context, cxMachineInfo));
            selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, cxMachineInfo);
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
     * @return
     */
    public ProductionPlanGroupInfo getInsertNewGroupPlan(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        if (CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> allGroupPlanList = new ArrayList<>(estimateGroupCxAllocationMap.values());
        if (CollectionUtils.isEmpty(allGroupPlanList)) {
            return null;
        }
        List<ProductionPlanGroupInfo> needProductionGroupList = allGroupPlanList.stream().filter(groupPlan -> groupPlan.getRemainingNeedAllocationDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(needProductionGroupList)) {
            return null;
        }
        // 如果有在机的特殊结构，则优先取出特殊结构
        if (((TbrProductionContext) context).getBaseDataContainer().getCxMachineBaseInfo().values()
                .stream().anyMatch(machine -> this.hasSpecialStructure(machine))) {
            List<ProductionPlanGroupInfo> specialMaterialList = needProductionGroupList.stream().filter(ProductionPlanGroupInfo::isSpecialMaterial).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(specialMaterialList)) {
                needProductionGroupList = specialMaterialList;
            }
        }
        //高优先级需求SKU个数多的优先
        Integer maxHeightPriority = needProductionGroupList.stream().mapToInt(ProductionPlanGroupInfo::getHeightPriorityCount).max().getAsInt();
        List<ProductionPlanGroupInfo> heightList = needProductionGroupList.stream().filter(groupPlan -> maxHeightPriority.equals(groupPlan.getHeightPriorityCount())).collect(Collectors.toList());
        if (heightList.size() == BigDecimal.ONE.intValue()) {
            return heightList.get(BigDecimal.ZERO.intValue());
        }
        Integer maxSpecialMaterial = heightList.stream().mapToInt(ProductionPlanGroupInfo::getSpecialMaterialsCount).max().getAsInt();
        List<ProductionPlanGroupInfo> specialMaterialList = heightList.stream().filter(groupPlan -> maxSpecialMaterial.equals(groupPlan.getSpecialMaterialsCount())).collect(Collectors.toList());
        if (specialMaterialList.size() == BigDecimal.ONE.intValue()) {
            return specialMaterialList.get(BigDecimal.ZERO.intValue());
        }
        specialMaterialList.sort(Comparator.comparing(ProductionPlanGroupInfo::getRemainingNeedAllocationDays));
        return specialMaterialList.get(BigDecimal.ZERO.intValue());
    }

    /**
     * 对分组(结构)计划，挑选合适成型机台
     *
     * @param context                排产上下文
     * @param addNewGroupPlan        排产分组计划
     * @param workWeakProductionInfo 可排产日集合
     * @return
     */
    public CxMachineBaseInfoVo selectedCxMachineForGroupPlan(Context context, ProductionPlanGroupInfo addNewGroupPlan, Set<Integer> workWeakProductionInfo) {
        if (null == addNewGroupPlan) {
            return null;
        }
        //获取分组及零度零度供料架
        String structureName = addNewGroupPlan.getGroupName();
        String isZeroRack = addNewGroupPlan.getIsZero();
        //最小分配天数
        Integer minAllocationDays;
        //20260209 特殊材料结构，将最小分配天数置为零
        if (addNewGroupPlan.isSpecialMaterial()) {
            minAllocationDays = BigDecimal.ZERO.intValue();
        } else {
            minAllocationDays = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getMinAllocationDays();
        }
        //挑选机台
        List<CxMachineBaseInfoVo> enableCxMachineList = GroupPlanCxMachineSelector.getEnableBaseCxMachineList(context, addNewGroupPlan);
        if (CollectionUtils.isEmpty(enableCxMachineList)) {
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, structureName));
            return null;
        }
        Integer needDays = addNewGroupPlan.getLeftOverNeedAllocationDays();
        //20260120 挑选排产日有交集的，结合成型工装数量-成型鼓，日产能上限
        List<CxMachineBaseInfoVo> hasProductionDayList = enableCxMachineList.stream().filter(singleMachine -> {
            Set<Integer> hasProductionDaySet = singleMachine.confirmProductionRange(context, workWeakProductionInfo);
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
            productionHistoryHandler.setCxMachineProductionGroupPlanHistory(context, addNewGroupPlan, singleMachine);
            //设置产能
            singleMachine.setSelectedProductionDaySet(hasProductionDaySet);
            singleMachine.setSelectedProductionDys(capacityDays);
            Integer diffValue = capacityDays - needDays;
            singleMachine.setCapacityDiffValue(diffValue);
            return true;
        }).collect(Collectors.toList());
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
            log.info(TbrProductionGroupLogRecorder.addSelectedFinalByMaxCapacityMachineLog(context, structureName, isZeroRack, minProductionSelected.getCxMachineCode(), minProductionSelected.getCxMachineTypeCode()));
            return minProductionSelected;
        }
        return selectOneCxMachine(context, selectedCapacityList, addNewGroupPlan);
    }

    /**
     * 获取产能可覆盖，机台可匹配的分组计划
     * 通过机台反向匹配计划
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 所有分组计划
     * @param cxMachineInfo                成型机台
     * @return
     */
    private ProductionPlanGroupInfo getSelectedGroup(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo) {
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
        //成型剩余产能能覆盖结构剩余排产净需求量
        Map<String, ProductionPlanGroupInfo> capacityCoverageMap = getProductionCapacityCoverage(context, estimateGroupCxAllocationMap, cxMachineInfo);
        if (CollectionUtils.isEmpty(capacityCoverageMap)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoFindCapacityPlanLog(context, cxMachineInfo));
            return null;
        }
        //剔除不可匹配的结构信息（不可作业的结构或是SKU需要剔除,零度供料架）
        Map<String, ProductionPlanGroupInfo> enableGroupPlanMap = excludeDisable(context, capacityCoverageMap, cxMachineInfo);
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addReverseCxMachineNoFindMatchPlanLog(context, cxMachineInfo));
            return null;
        }
        //获取合适优先级的一个结构
        return selectedOne(context, enableGroupPlanMap, cxMachineInfo);
    }

    /**
     * 得到成型机台剩余产能能覆盖剩余排产净需求的分组结构计划
     * 此时会结合成型工装的数量
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组结构计划集合
     * @param cxMachineInfo                成型机信息
     * @return
     */
    private Map<String, ProductionPlanGroupInfo> getProductionCapacityCoverage(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == cxMachineInfo || CollectionUtils.isEmpty(estimateGroupCxAllocationMap)) {
            return Collections.emptyMap();
        }
        //成型机剩余产能能覆盖剩余排产净需求
        Map<String, ProductionPlanGroupInfo> capacityCoverageMap = new HashMap<>(estimateGroupCxAllocationMap.size());
        estimateGroupCxAllocationMap.forEach((structureName, groupPlan) -> {
            Integer minLhDayCapacityQty = groupPlan.getMinLhDayCapacityQty();
            if (null == minLhDayCapacityQty || minLhDayCapacityQty <= BigDecimal.ZERO.longValue()) {
                //todo 记录日志
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
            //20260109--先采用天数来判断，因剩余未排产量存在模具受限的干扰 groupPlan.getRemainingProductionQty
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
            if (realRemainingDays < remainingNeedDays) {
                return;
            }
            capacityCoverageMap.put(structureName, groupPlan);
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
     * 获取最合适的一个结构
     * 1、固定优先
     * 2、成型的前结构同规格(SKU的规格属性)优先
     * 3、成型的前结构同英寸(SKU的英寸属性)优先
     * 4、成型的前结构断面宽±10
     * 5、近1个月结构上机日期近的优先
     * 6、近3个月结构生产次数多的优先
     *
     * @param enableGroupPlanMap
     * @param cxMachineInfo
     * @return
     */
    private ProductionPlanGroupInfo selectedOne(Context context, Map<String, ProductionPlanGroupInfo> enableGroupPlanMap, CxMachineBaseInfoVo cxMachineInfo) {
        if (CollectionUtils.isEmpty(enableGroupPlanMap)) {
            return null;
        }
        List<ProductionPlanGroupInfo> groupPlanList = new ArrayList<>(enableGroupPlanMap.size());
        enableGroupPlanMap.forEach((structureName, groupPlan) -> {
            groupPlan.setFixedPriority(cxMachineInfo.getFixedPriorityValue(groupPlan));
            groupPlanList.add(groupPlan);
        });
        if (CollectionUtils.isEmpty(groupPlanList)) {
            return null;
        }
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String cxMachineTypeCode = cxMachineInfo.getCxMachineTypeCode();
        // 1、如果在机结构有特殊材料，需要优先选择包含特殊材料的结构
        List<ProductionPlanGroupInfo> specialMaterialList = groupPlanList;
        boolean isSpecial = this.hasSpecialStructure(cxMachineInfo);
        // 如果在机结构没有特殊材料，但是其他机台的在机结构有特殊材料，则同样需要优先选择包含特殊材料的结构
        if (!isSpecial) {
            TbrProductionContext productionContext = (TbrProductionContext) context;
            isSpecial = productionContext.getBaseDataContainer().getCxMachineBaseInfo().values().stream()
                    .anyMatch(machine -> this.hasSpecialStructure(machine));
        }
        // 特殊材料校验为true，则需要优先排特殊结构
        if (isSpecial) {
            List<ProductionPlanGroupInfo> tempSpecialMaterialList = groupPlanList.stream()
                    .filter(ProductionPlanGroupInfo::isSpecialMaterial).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(tempSpecialMaterialList)) {
                specialMaterialList = tempSpecialMaterialList;
            }
        }

        //1、取固定的
        Integer minFixedPriority = specialMaterialList.stream().mapToInt(ProductionPlanGroupInfo::getFixedPriority).min().getAsInt();
        List<ProductionPlanGroupInfo> fixedGroupPlanList = specialMaterialList.stream().filter(groupPlan -> minFixedPriority.equals(groupPlan.getFixedPriority())).collect(Collectors.toList());
        if (fixedGroupPlanList.size() == BigDecimal.ONE.intValue()) {
            ProductionPlanGroupInfo selected = fixedGroupPlanList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.FIXED_PRIORITY));
            return selected;
        }
        CxMachineAllocationPlanHelper lastHelper = cxMachineInfo.getLastAllocationInfo();
        //取前规格排产计划-所有
        List<MonthPlanProductionRequirePlanVo> realProductionPlanList = lastHelper.getProductionPlanInfo().getGroupPlanData();
        //2、与前结构含有同规格的优先
        List<ProductionPlanGroupInfo> sameSpecificationsList = fixedGroupPlanList.stream().filter(fixedPlan -> fixedPlan.hasSameSpecifications(realProductionPlanList)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameSpecificationsList)) {
            sameSpecificationsList = fixedGroupPlanList;
        }
        if (sameSpecificationsList.size() == BigDecimal.ONE.intValue()) {
            ProductionPlanGroupInfo selected = sameSpecificationsList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SAME_SPECIFICATIONS_PRIORITY));
            return selected;
        }
        //3、与前结构含有同英寸的优先
        List<ProductionPlanGroupInfo> sameProSizeList = sameSpecificationsList.stream().filter(sameSpecificationsPlan -> sameSpecificationsPlan.hasSameProSize(realProductionPlanList)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameProSizeList)) {
            sameProSizeList = sameSpecificationsList;
        }
        if (sameProSizeList.size() == BigDecimal.ONE.intValue()) {
            ProductionPlanGroupInfo selected = sameProSizeList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SAME_PRO_SIZE_PRIORITY));
            return selected;
        }
        //4、断面宽差值±10 参数
        Integer diffValue = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getSectionWidthDiffValue();
        List<ProductionPlanGroupInfo> sectionWidthList = sameProSizeList.stream().filter(sectionWidthPlan -> sectionWidthPlan.hasSectionWidthCondition(realProductionPlanList, diffValue)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sectionWidthList)) {
            sectionWidthList = sameProSizeList;
        }
        if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
            ProductionPlanGroupInfo selected = sameProSizeList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.SECTION_WIDTH_PRIORITY));
            return selected;
        }
        //5、设置该成型机近1个月的排产分组和排产次数
        sectionWidthList.forEach(groupPlan -> {
            groupPlan.setLastBoardingDate(BigDecimal.ZERO.intValue());
            groupPlan.setProductionCount(BigDecimal.ZERO.intValue());
            productionHistoryHandler.setCxMachineProductionGroupPlanHistory(context, groupPlan, cxMachineInfo);
        });
        sectionWidthList.sort(Comparator.comparing(ProductionPlanGroupInfo::getLastBoardingDate, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ProductionPlanGroupInfo::getProductionCount, Comparator.nullsLast(Comparator.reverseOrder())));
        ProductionPlanGroupInfo selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
        log.info(TbrProductionGroupLogRecorder.addCxMachineSelectedGroupPlanLog(context, selected.getGroupName(), selected.getIsZero(), cxMachineCode, cxMachineTypeCode, GroupCxMachineSelectedTypeEnum.HISTORY_QUALITY_PRIORITY));
        return selected;
    }

    /**
     * 选择合适的机台
     * 1、固定优先
     * 2、与前分组同规格优先
     * 3、与前分组同英寸优先
     * 4、与前分组断面宽优先
     * 5、近1个月最近生产优先
     * 6、近n个月生产最多优先
     * 7、非零度优先
     * 8、机台编号大优先
     *
     * @param context              排产上下文
     * @param selectedCapacityList 可选择的产能机台
     * @param addNewGroupPlan      新增的计划
     * @return
     */
    private CxMachineBaseInfoVo selectOneCxMachine(Context context, List<CxMachineBaseInfoVo> selectedCapacityList, ProductionPlanGroupInfo addNewGroupPlan) {
        //获取分组及零度零度供料架
        String structureName = addNewGroupPlan.getGroupName();
        String isZeroRack = addNewGroupPlan.getIsZero();
        //设置机台固定信息
        selectedCapacityList.stream().forEach(cxMachineInfo -> cxMachineInfo.setFixedPriority(cxMachineInfo.getFixedPriorityValue(addNewGroupPlan)));
        //固定优先
        Integer minFixedPriority = selectedCapacityList.stream().mapToInt(CxMachineBaseInfoVo::getFixedPriority).min().getAsInt();
        List<CxMachineBaseInfoVo> fixedPriorityList = selectedCapacityList.stream().filter(cxMachineInfo -> minFixedPriority.equals(cxMachineInfo.getFixedPriority())).collect(Collectors.toList());
        if (fixedPriorityList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo fixedSelected = fixedPriorityList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, fixedSelected.getCxMachineCode(), fixedSelected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.FIXED_PRIORITY));
            return fixedSelected;
        }
        //设置是否同规格，同英寸,断面宽
        setSameInfo(context, fixedPriorityList, addNewGroupPlan);
        //同规格优先
        List<CxMachineBaseInfoVo> sameSpecificationsList = fixedPriorityList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSameSpecifications())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameSpecificationsList)) {
            sameSpecificationsList = fixedPriorityList;
        }
        if (sameSpecificationsList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sameSpecificationsList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SAME_SPECIFICATIONS_PRIORITY));
            return selected;
        }
        //同英寸优先
        List<CxMachineBaseInfoVo> sameProSizeList = sameSpecificationsList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSameProSize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sameProSizeList)) {
            sameProSizeList = sameSpecificationsList;
        }
        if (sameProSizeList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sameProSizeList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SAME_PRO_SIZE_PRIORITY));
            return selected;
        }
        //断面宽优先
        List<CxMachineBaseInfoVo> sectionWidthList = sameSpecificationsList.stream().filter(cxMachineInfo -> YesOrNoEnum.YES.getCode().equals(cxMachineInfo.getSameProSize())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(sectionWidthList)) {
            sectionWidthList = sameProSizeList;
        }
        if (sectionWidthList.size() == BigDecimal.ONE.intValue()) {
            CxMachineBaseInfoVo selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
            log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.SECTION_WIDTH_PRIORITY));
            return selected;
        }
        //同规格优先 -> 同英寸优先 -> 断面宽优先 -> 历史最近优先 -> n个月生产最多优先 -> 非零度优先 -> 机台编号
        Comparator sortComparator = Comparator.comparing(CxMachineBaseInfoVo::getSameSpecifications, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getSameProSize, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getSectionWidthCondition, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getLastBoardingDate, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getProductionCount, Comparator.reverseOrder())
                .thenComparing(CxMachineBaseInfoVo::getIsZeroRack)
                .thenComparing(CxMachineBaseInfoVo::getCxMachineCode, Comparator.reverseOrder());
        sectionWidthList.sort(sortComparator);
        CxMachineBaseInfoVo selected = sectionWidthList.get(BigDecimal.ZERO.intValue());
        log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedFinalCxMachineCodeLog(context, structureName, isZeroRack, selected.getCxMachineCode(), selected.getCxMachineTypeCode(), GroupCxMachineSelectedTypeEnum.HISTORY_QUALITY_PRIORITY));
        return selected;
    }

    /**
     * 设置同规格、同英寸，断面宽等信息
     *
     * @param context           排产上下文
     * @param fixedPriorityList 机台集合
     * @param addNewGroupPlan   新增结构
     */
    private void setSameInfo(Context context, List<CxMachineBaseInfoVo> fixedPriorityList, ProductionPlanGroupInfo addNewGroupPlan) {
        //4、断面宽差值±10 断面宽差值范围参数
        Integer diffValue = ((TbrProductionContext) context).getBaseDataContainer().getParamConfiguration().getSectionWidthDiffValue();
        //设置是否同规格，同英寸,断面宽
        fixedPriorityList.forEach(cxMachineInfo -> cxMachineInfo.setSameInfoByCurrentGroupPlan(addNewGroupPlan, diffValue));
    }

}
