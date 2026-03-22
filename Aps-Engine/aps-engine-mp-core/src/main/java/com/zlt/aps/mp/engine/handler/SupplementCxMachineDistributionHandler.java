package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.logrecorder.SupplementCxMachineDistributionLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxCapacityAllocationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对剩余不满足最短上机天数的机台，进行最后的补充分配
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260315
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupplementCxMachineDistributionHandler {

    private final GroupPlanPrioritySelector groupPlanPrioritySelector;

    private final CxCapacityAllocationHandler cxCapacityAllocationHandler;

    /**
     * 对成型机台因剩余产能不满足最短上机天数的机台，进行尾量再分配
     *
     * @param productionContext 排产上下文
     * @param allGroupPlanMap   所有分组计划
     */
    public void handlerTailCapacity(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap) {
        SupplementCxMachineDistributionLogRecorder.addStartSupplementLog(productionContext);
        //获取还有剩余产能的机台
        List<CxMachineBaseInfoVo> hasLeftOverCxMachineList = getAllLeftOverCxMachineInfo(productionContext);
        if (CollectionUtils.isEmpty(hasLeftOverCxMachineList)) {
            //机台没有剩余产能
            SupplementCxMachineDistributionLogRecorder.addNoLeftOverCxMachineLog(productionContext);
            return;
        }
        //获取还有剩余需求量的结构信息
        List<ProductionPlanGroupInfo> hasLeftOverGroupList = getAllLeftOverNeedDaysInfo(allGroupPlanMap);
        if (CollectionUtils.isEmpty(hasLeftOverGroupList)) {
            SupplementCxMachineDistributionLogRecorder.addNoLeftOverGroupLog(productionContext);
            //剩余机台分配到月底
            addBeforeGroupToFull(productionContext, hasLeftOverCxMachineList);
            return;
        }
        String supplementCxMachineInfo = hasLeftOverCxMachineList.stream().map(CxMachineBaseInfoVo::getCxMachineCode).collect(Collectors.joining(StringConstant.COMMA));
        SupplementCxMachineDistributionLogRecorder.addNeedSupplementAllocationInfoLog(productionContext, supplementCxMachineInfo);
        //按结构优先级排序，进行分配
        productionTailCapacity(productionContext, hasLeftOverCxMachineList, hasLeftOverGroupList);
        //最后还有剩余机台产能的机台，则将前结构顺延到底
        addBeforeGroupToFull(productionContext, hasLeftOverCxMachineList);
    }

    /**
     * 获取还有剩余成型产能的机台列表
     *
     * @param productionContext 排产上下文
     * @return
     */
    private List<CxMachineBaseInfoVo> getAllLeftOverCxMachineInfo(TbrProductionContext productionContext) {
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        if (null == baseDataContainer) {
            return Collections.emptyList();
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfoMap = baseDataContainer.getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfoMap)) {
            return Collections.emptyList();
        }
        return getLeftOverCxMachineInfo(productionContext, allCxMachineInfoMap.values().stream().collect(Collectors.toList()));
    }

    /**
     * 获取还有剩余待分配的分组计划需求
     *
     * @param allGroupPlanMap
     * @return
     */
    private List<ProductionPlanGroupInfo> getAllLeftOverNeedDaysInfo(Map<String, ProductionPlanGroupInfo> allGroupPlanMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap)) {
            return Collections.emptyList();
        }
        //剔除特殊材料的结构
        List<ProductionPlanGroupInfo> rejectSpecialMaterialList = allGroupPlanMap.values().stream().filter(single -> !single.isSpecialMaterial()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(rejectSpecialMaterialList)) {
            return Collections.emptyList();
        }
        return getLeftOverNeedDaysGroupInfo(rejectSpecialMaterialList);
    }

    /**
     * 将剩余产能机台按最后一个结构全部排满
     *
     * @param productionContext        排产上下文
     * @param hasLeftOverCxMachineList 还有剩余产能的机台
     */
    private void addBeforeGroupToFull(TbrProductionContext productionContext, List<CxMachineBaseInfoVo> hasLeftOverCxMachineList) {
        if (CollectionUtils.isEmpty(hasLeftOverCxMachineList)) {
            return;
        }
        List<CxMachineBaseInfoVo> realLeftOverCxMachineList = getLeftOverCxMachineInfo(productionContext, hasLeftOverCxMachineList);
        if (CollectionUtils.isEmpty(realLeftOverCxMachineList)) {
            return;
        }
        realLeftOverCxMachineList.forEach(singleCxMachine -> singleCxMachine.addLastAllocationToFull(productionContext));
    }

    /**
     * 是否有处理过
     *
     * @param productionContext        排产上下文
     * @param hasLeftOverCxMachineList 剩余产能机台集合
     * @param hasLeftOverGroupList     剩余计划集合
     * @return
     */
    private boolean productionTailCapacity(TbrProductionContext productionContext, List<CxMachineBaseInfoVo> hasLeftOverCxMachineList, List<ProductionPlanGroupInfo> hasLeftOverGroupList) {
        List<CxMachineBaseInfoVo> realLeftOverCxMachineList = getLeftOverCxMachineInfo(productionContext, hasLeftOverCxMachineList);
        if (CollectionUtils.isEmpty(realLeftOverCxMachineList)) {
            //机台没有剩余产能
            SupplementCxMachineDistributionLogRecorder.addNoLeftOverCxMachineLog(productionContext);
            return false;
        }
        List<ProductionPlanGroupInfo> realLeftOverGroupList = getLeftOverNeedDaysGroupInfo(hasLeftOverGroupList);
        if (CollectionUtils.isEmpty(realLeftOverGroupList)) {
            SupplementCxMachineDistributionLogRecorder.addNoLeftOverGroupLog(productionContext);
            return false;
        }
        //按结构优先级排序
        groupSort(productionContext, realLeftOverCxMachineList, realLeftOverGroupList);
        //取得成型个数的结构数
        Integer cxMachineSize = realLeftOverCxMachineList.size();
        Integer groupPlanSize = realLeftOverGroupList.size();
        Integer selectSize = Math.min(cxMachineSize, groupPlanSize);
        List<ProductionPlanGroupInfo> selectGroupList = realLeftOverGroupList.subList(BigDecimal.ZERO.intValue(), selectSize);
        List<CxMachineAllocationPlanHelper> handlerResult = new ArrayList<>();
        Set<String> rejectGroupPlan = new HashSet<>();
        selectGroupList.forEach(singleGroupPlan -> {
            String structureName = singleGroupPlan.getGroupName();
            List<CxMachineBaseInfoVo> enableCxMachineList = GroupPlanCxMachineSelector.getEnableCxMachineListByAppoint(productionContext, singleGroupPlan, realLeftOverCxMachineList);
            if (CollectionUtils.isEmpty(enableCxMachineList)) {
                rejectGroupPlan.add(structureName);
                return;
            }
            //挑选机台
            CxMachineBaseInfoVo selectCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlanByAppoint(productionContext, enableCxMachineList, singleGroupPlan);
            if (null == selectCxMachine) {
                rejectGroupPlan.add(structureName);
                return;
            }
            CxMachineAllocationPlanHelper allocationResult = handlerAllocation(productionContext, singleGroupPlan, selectCxMachine);
            if (null == allocationResult) {
                rejectGroupPlan.add(structureName);
            } else {
                handlerResult.add(allocationResult);
            }
        });
        //剔除不能匹配机台的计划
        if (!CollectionUtils.isEmpty(rejectGroupPlan)) {
            realLeftOverGroupList.removeIf(singleGroup -> rejectGroupPlan.contains(singleGroup.getGroupName()));
        }
        if (CollectionUtils.isEmpty(handlerResult)) {
            return false;
        }
        return productionTailCapacity(productionContext, realLeftOverCxMachineList, realLeftOverGroupList);
    }


    /**
     * 从theoryLeftOverCxMachineList获取还有剩余产能的成型机台
     *
     * @param context                     排产上下文
     * @param theoryLeftOverCxMachineList 成型机台
     * @return
     */
    private List<CxMachineBaseInfoVo> getLeftOverCxMachineInfo(Context context, List<CxMachineBaseInfoVo> theoryLeftOverCxMachineList) {
        if (CollectionUtils.isEmpty(theoryLeftOverCxMachineList)) {
            return Collections.emptyList();
        }
        Integer monthEndDay = context.getProductionEndDay();
        List<CxMachineBaseInfoVo> leftOverCxMachineInfoList = theoryLeftOverCxMachineList.stream().filter(single -> single.getCurrentProductionEndDay() < monthEndDay).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverCxMachineInfoList)) {
            return Collections.emptyList();
        }
        return leftOverCxMachineInfoList;
    }

    /**
     * 获取还有剩余待分配的分组计划需求
     *
     * @param theoryLeftOverGroupPlanList 理论还有剩余需求量的分组计划
     * @return
     */
    private List<ProductionPlanGroupInfo> getLeftOverNeedDaysGroupInfo(List<ProductionPlanGroupInfo> theoryLeftOverGroupPlanList) {
        if (CollectionUtils.isEmpty(theoryLeftOverGroupPlanList)) {
            return Collections.emptyList();
        }
        List<ProductionPlanGroupInfo> hasNeedGroupInfoList = theoryLeftOverGroupPlanList.stream().filter(singleGroup -> singleGroup.getBoostReplenishmentQuota() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasNeedGroupInfoList)) {
            return Collections.emptyList();
        }
        return hasNeedGroupInfoList;
    }

    /**
     * 结构优先级
     *
     * @param productionContext        排产上下文
     * @param hasLeftOverCxMachineList 剩余成型机台
     * @param hasLeftOverGroupList     剩余分组计划
     */
    private void groupSort(TbrProductionContext productionContext, List<CxMachineBaseInfoVo> hasLeftOverCxMachineList, List<ProductionPlanGroupInfo> hasLeftOverGroupList) {
        if (CollectionUtils.isEmpty(hasLeftOverGroupList) || CollectionUtils.isEmpty(hasLeftOverCxMachineList)) {
            return;
        }
        // 如果有在机的特殊结构，则优先取出特殊结构
        boolean isSpecialMaterialPriority = false;
        if (hasLeftOverCxMachineList.stream().anyMatch(machine -> machine.lastProductionSpecialStructure())) {
            List<ProductionPlanGroupInfo> specialMaterialList = hasLeftOverGroupList.stream().filter(ProductionPlanGroupInfo::isSpecialMaterial).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(specialMaterialList)) {
                isSpecialMaterialPriority = true;
            }
        }
        Comparator sort;
        if (isSpecialMaterialPriority) {
            sort = Comparator.comparing(ProductionPlanGroupInfo::isSpecialMaterial, Comparator.reverseOrder())
                    .thenComparing(ProductionPlanGroupInfo::getHeightPriorityCount, Comparator.reverseOrder());
        } else {
            sort = Comparator.comparing(ProductionPlanGroupInfo::getHeightPriorityCount, Comparator.reverseOrder());
        }
        sort.thenComparing(Comparator.comparing(ProductionPlanGroupInfo::getUsedSpecialMaterialCount, Comparator.reverseOrder())
                .thenComparing(new Comparator() {
                    @Override
                    public int compare(Object obj1, Object obj2) {
                        ProductionPlanGroupInfo before = (ProductionPlanGroupInfo) obj1;
                        ProductionPlanGroupInfo after = (ProductionPlanGroupInfo) obj2;
                        return groupPlanPrioritySelector.compareSpecialMaterial(before, after);
                    }
                }));

        hasLeftOverGroupList.sort(sort);
    }

    /**
     * 对成型机台分配selectCxMachine结构。
     *
     * @param productionContext 排产上下文
     * @param addPlanGroup      分组计划
     * @param selectCxMachine   成型机台
     * @return
     */
    private CxMachineAllocationPlanHelper handlerAllocation(TbrProductionContext productionContext, ProductionPlanGroupInfo addPlanGroup, CxMachineBaseInfoVo selectCxMachine) {
        //判断切换结构的点
        CxMachineAllocationPlanHelper lastGroup = selectCxMachine.getLastAllocationInfo();
        if (null == lastGroup) {
//            handlerEmptyCxMachine(productionContext, addPlanGroup, selectCxMachine);
            return null;
        }
        boolean isChange = !lastGroup.getProductionPlanInfo().getGroupName().equals(addPlanGroup.getGroupName());
        if (isChange) {
            return changeHandler(productionContext, addPlanGroup, selectCxMachine);
        }
        return noChangeHandler(productionContext, addPlanGroup, selectCxMachine);
    }

    /**
     * 选中的为空机台
     *
     * @param productionContext 排产上下文
     * @param addPlanGroup      新增结构
     * @param selectCxMachine   机台
     * @return
     */
    private CxMachineAllocationPlanHelper handlerEmptyCxMachine(TbrProductionContext productionContext, ProductionPlanGroupInfo addPlanGroup, CxMachineBaseInfoVo selectCxMachine) {
        String groupName = addPlanGroup.getGroupName();
        Set<Integer> hasProductionDaySet = selectCxMachine.getTheoryProductionDaySet();
        Integer startDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
        //20260121 切换结构控制
        DayCapacityLimitVo dayCapacityLimitVo = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Integer realChangeDay = dayCapacityLimitVo.confirmStartDayByChangeGroup(productionContext, startDay, groupName, selectCxMachine, hasProductionDaySet);
        if (null == realChangeDay) {
            //记录日志
            Integer maxChangeLimit = productionContext.getBaseDataContainer().getParamConfiguration().getDayChangeGroupCount();
            log.info(TbrProductionGroupLogRecorder.addChangeGroupLimitCxMachineLog(productionContext, selectCxMachine.getCxMachineCode(), maxChangeLimit));
            return null;
        }
        ProductGroupCxCapacityInfo lhRatioInfo = addPlanGroup.getLhRatioByCxMachine(selectCxMachine);
        startDay = realChangeDay;
        Set<Integer> realProductionDaySet = hasProductionDaySet.stream().filter(singleDay -> singleDay >= realChangeDay).collect(Collectors.toSet());
        Integer remainingDays = realProductionDaySet.size();
        //分配产能
        Integer needDays = addPlanGroup.getLeftOverNeedAllocationDays();
        Integer realAllocationDays = Math.min(remainingDays, needDays);
        //更新剩余天数
        addPlanGroup.updateLeftOverNeedAllocationDays(realAllocationDays);
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectCxMachine, lhRatioInfo, addPlanGroup, null, realAllocationDays, startDay, productionContext.getMonthDays());
        selectCxMachine.addAllocationPlanInfo(productionContext, addHelper);
        return addHelper;
    }

    /**
     * 需要切换结构的处理
     *
     * @param productionContext 排产上下文
     * @param addPlanGroup      分配的分组计划
     * @param selectCxMachine   成型
     * @return
     */
    private CxMachineAllocationPlanHelper changeHandler(TbrProductionContext productionContext, ProductionPlanGroupInfo addPlanGroup, CxMachineBaseInfoVo selectCxMachine) {
        Integer needDays = addPlanGroup.getLeftOverNeedAllocationDays();
        Integer remainDays = selectCxMachine.getLeftOverDaysByLastAllocation(productionContext);
        Integer realAllocationDays = Math.min(needDays, remainDays);
        CxMachineAllocationPlanHelper lastAllocationInfo = selectCxMachine.getLastAllocationInfo();
        Integer startDay = lastAllocationInfo.getEndDay() + BigDecimal.ONE.intValue();
        Integer endDay = productionContext.getMonthDays();
        Integer refundDay = BigDecimal.ZERO.intValue();
        Integer realStartDay = startDay;
        Set<Integer> hasChangeGroupSet = productionContext.getBaseDataContainer().getDayCapacityLimit().getHasChangeGroupProductionDay(productionContext);
        for (; realStartDay <= endDay; realStartDay++) {
            //退的天数达到，或是已经找到可切换结构的天数
            if (refundDay.equals(realAllocationDays) || hasChangeGroupSet.contains(realStartDay)) {
                break;
            }
            if (productionContext.getStopDays().contains(realStartDay)) {
                continue;
            }
            refundDay = refundDay + BigDecimal.ONE.intValue();
        }
        if (refundDay >= realAllocationDays) {
            return null;
        }
        //前结构延长天数
        if (refundDay > BigDecimal.ZERO.intValue()) {
            ProductGroupCxCapacityInfo lhRatioInfo = addPlanGroup.getLhRatioByCxMachine(selectCxMachine);
            CxMachineAllocationPlanHelper refundAllocation = CxCapacityAllocationHandler.createAllocationPlanHelper(selectCxMachine, lhRatioInfo, addPlanGroup, null, refundDay, startDay, productionContext.getMonthDays());
            selectCxMachine.updateLastAllocationPlanInfo(productionContext, refundAllocation);
        }
        realAllocationDays = realAllocationDays - refundDay;
        ProductGroupCxCapacityInfo lhRatioInfo = addPlanGroup.getLhRatioByCxMachine(selectCxMachine);
        CxMachineAllocationPlanHelper addAllocation = CxCapacityAllocationHandler.createAllocationPlanHelper(selectCxMachine, lhRatioInfo, addPlanGroup, null, realAllocationDays, realStartDay, productionContext.getMonthDays());
        //计划更新剩余天数
        addPlanGroup.updateLeftOverNeedAllocationDays(realAllocationDays);
        //更新成型分配信息
        selectCxMachine.addAllocationPlanInfo(productionContext, addAllocation);
        return addAllocation;
    }

    /**
     * 无需结构切换
     *
     * @param productionContext 排产上下文
     * @param addPlanGroup      分配分组计划
     * @param selectCxMachine   分配成型
     * @return
     */
    private CxMachineAllocationPlanHelper noChangeHandler(TbrProductionContext productionContext, ProductionPlanGroupInfo addPlanGroup, CxMachineBaseInfoVo selectCxMachine) {
        Integer needDays = addPlanGroup.getLeftOverNeedAllocationDays();
        Integer remainDays = selectCxMachine.getLeftOverDaysByLastAllocation(productionContext);
        Integer realAllocationDays = Math.min(needDays, remainDays);
        CxMachineAllocationPlanHelper lastAllocationInfo = selectCxMachine.getLastAllocationInfo();
        CxMachineAllocationPlanHelper addNewAllocation = createAddAllocationInfo(productionContext, addPlanGroup, selectCxMachine);
        //计划更新剩余天数
        addPlanGroup.updateLeftOverNeedAllocationDays(realAllocationDays);
        //更新成型分配信息
        selectCxMachine.updateLastAllocationPlanInfo(productionContext, addNewAllocation);
        return lastAllocationInfo;
    }

    /**
     * 创建分配信息
     *
     * @param productionContext 排产上下文
     * @param addPlanGroup      分配的分组计划
     * @param selectCxMachine   分配的成型机台
     * @return
     */
    private CxMachineAllocationPlanHelper createAddAllocationInfo(TbrProductionContext productionContext, ProductionPlanGroupInfo addPlanGroup, CxMachineBaseInfoVo selectCxMachine) {
        Integer needDays = addPlanGroup.getLeftOverNeedAllocationDays();
        Integer remainDays = selectCxMachine.getLeftOverDaysByLastAllocation(productionContext);
        Integer realAllocationDays = Math.min(needDays, remainDays);
        CxMachineAllocationPlanHelper lastAllocationInfo = selectCxMachine.getLastAllocationInfo();
        Integer startDay = lastAllocationInfo.getEndDay() + BigDecimal.ONE.intValue();
        ProductGroupCxCapacityInfo lhRatioInfo = addPlanGroup.getLhRatioByCxMachine(selectCxMachine);
        return CxCapacityAllocationHandler.createAllocationPlanHelper(selectCxMachine, lhRatioInfo, addPlanGroup, null, realAllocationDays, startDay, productionContext.getMonthDays());
    }

}
