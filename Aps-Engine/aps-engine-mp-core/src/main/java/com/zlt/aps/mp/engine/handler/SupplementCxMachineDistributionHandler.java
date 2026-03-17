package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.logrecorder.SupplementCxMachineDistributionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.CxCapacityAllocationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final CxCapacityAllocationHandler cxCapacityAllocationHandler;

    /**
     * 对成型机台因剩余产能不满足最短上机天数的机台，进行尾量再分配
     *
     * @param productionContext 排产上下文
     * @param allGroupPlanMap   所有分组计划
     */
    public void handlerTailCapacity(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap) {
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
        return getLeftOverCxMachineInfo(allCxMachineInfoMap.values().stream().collect(Collectors.toList()));
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
        return getLeftOverNeedDaysGroupInfo(allGroupPlanMap.values().stream().collect(Collectors.toList()));
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
        List<CxMachineBaseInfoVo> realLeftOverCxMachineList = getLeftOverCxMachineInfo(hasLeftOverCxMachineList);
        if (CollectionUtils.isEmpty(realLeftOverCxMachineList)) {
            return;
        }
        realLeftOverCxMachineList.forEach(singleCxMachine -> singleCxMachine.addLastAllocationToFull(productionContext));
    }

    /**
     * 是否有处理过
     *
     * @param productionContext
     * @param hasLeftOverCxMachineList
     * @param hasLeftOverGroupList
     * @return
     */
    private boolean productionTailCapacity(TbrProductionContext productionContext, List<CxMachineBaseInfoVo> hasLeftOverCxMachineList, List<ProductionPlanGroupInfo> hasLeftOverGroupList) {
        List<CxMachineBaseInfoVo> realLeftOverCxMachineList = getLeftOverCxMachineInfo(hasLeftOverCxMachineList);
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
        selectGroupList.forEach(singleGroupPlan ->{
            List<CxMachineBaseInfoVo> enableCxMachineList = GroupPlanCxMachineSelector.getEnableCxMachineListByAppoint(productionContext, singleGroupPlan, realLeftOverCxMachineList);
            if(CollectionUtils.isEmpty(enableCxMachineList)){
                return ;
            }
            //20260120 挑选排产日有交集的，结合成型工装数量-成型鼓，日产能上限
            List<CxMachineBaseInfoVo> hasProductionDayList = enableCxMachineList.stream().filter(singleMachine -> cxCapacityAllocationHandler.selectEnableMachineAndSetInfo(productionContext, singleGroupPlan, singleMachine)
            ).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(hasProductionDayList)) {
                return ;
            }


        });
        if(CollectionUtils.isEmpty(handlerResult)){
            return false;
        }
        return productionTailCapacity(productionContext, realLeftOverCxMachineList, realLeftOverGroupList);
    }


    /**
     * 从theoryLeftOverCxMachineList获取还有剩余产能的成型机台
     *
     * @param theoryLeftOverCxMachineList
     * @return
     */
    private List<CxMachineBaseInfoVo> getLeftOverCxMachineInfo(List<CxMachineBaseInfoVo> theoryLeftOverCxMachineList) {
        if (CollectionUtils.isEmpty(theoryLeftOverCxMachineList)) {
            return Collections.emptyList();
        }
        List<CxMachineBaseInfoVo> leftOverCxMachineInfoList = theoryLeftOverCxMachineList.stream().filter(single -> single.getRemainingDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
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
        sort.thenComparing(Comparator.comparing(ProductionPlanGroupInfo::getSpecialMaterialsCount, Comparator.reverseOrder())
                .thenComparing(new Comparator() {
                    @Override
                    public int compare(Object obj1, Object obj2) {
                        ProductionPlanGroupInfo groupInfo1 = (ProductionPlanGroupInfo) obj1;
                        ProductionPlanGroupInfo groupInfo2 = (ProductionPlanGroupInfo) obj2;
                        // 判断如果都是特殊材料，同时包含专用与共用特殊材料的结构优先
                        Boolean hasDedicatedSpecialMaterials1 = groupInfo1.hasDedicatedSpecialMaterials(productionContext);
                        Boolean hasDedicatedSpecialMaterials2 = groupInfo2.hasDedicatedSpecialMaterials(productionContext);
                        int result = hasDedicatedSpecialMaterials2.compareTo(hasDedicatedSpecialMaterials1); // 倒序
                        if (result != 0) {
                            return result;
                        }
                        Integer remainingNeedAllocationDays1 = Optional.ofNullable(groupInfo1.getRemainingNeedAllocationDays()).orElse(0);
                        Integer remainingNeedAllocationDays2 = Optional.ofNullable(groupInfo2.getRemainingNeedAllocationDays()).orElse(0);
                        result = remainingNeedAllocationDays1.compareTo(remainingNeedAllocationDays2);
                        return result;
                    }
                }));

        hasLeftOverGroupList.sort(sort);
    }

}
