package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.GroupPlanPrioritySelector;
import com.zlt.aps.mp.engine.handler.SupplementCxMachineDistributionHandler;
import com.zlt.aps.mp.engine.logrecorder.TbrBeforeProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSimulateProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模拟排产
 * 此时在机结构已经对在产机台确定了各个机台的收尾时间点
 * continueAllocationList中已经含有
 * 1、先对在机结构在产机台的续作部分进行模拟排产
 * 2、再对在机结构在产机台进行新增Sku的模拟排产
 * 3、在在产机台中的收尾机台进行反向查找匹配分组计划的模拟排产
 * 4、对还需排产的分组(新增和在机结构新增机台)的计划进行模拟排产
 *
 * @author ZLT
 * @date 20260127
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulateProductionHandler extends OnLineGroupOnLineMachineHandler {

    private final CxMouldProductionHandler cxMouldProductionHandler;

    private final GroupPlanPrioritySelector groupPlanPrioritySelector;

    private final CxAddSkuProductionHandler cxAddSkuProductionHandler;

    private final ClearProductionInfoHandler clearProductionInfoHandler;

    private final CxCapacityAllocationHandler cxCapacityAllocationHandler;

    private final SpecialMaterialScheduleHandler specialMaterialScheduleHandler;

    private final GroupPlanBeforeConclusionHandler groupPlanBeforeConclusionHandler;

    private final SupplementCxMachineDistributionHandler supplementCxMachineDistributionHandler;

    /**
     * 模拟排产计划
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有排产分组计划
     * @param continueAllocationList 在产机构在产机台的分配情况
     * @param allContinueMap         所有续作Sku
     */
    public void productionGroupPlan(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //设置收尾机台信息-空
        productionContext.setReverseFindSet(new HashSet<>());
        //1、模拟排产前的数据处理
        beforeSimulateProductionHandler(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        log.info(TbrSimulateProductionLogRecorder.addResetDataFinishLog(productionContext));
        //1、在机结构对在产成型机台进行模拟模具排产
        mouldProductionByContinueGroup(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        //2、对在产机台-收尾成型机台，反向匹配待排结构
        cxCapacityAllocationHandler.reverseMachineAllocation(productionContext, allGroupPlanMap);
        //3、对还需排产结构，获取优先级最高的结构--结构新增
        addNewGroupPlanHandler(productionContext, allGroupPlanMap);
        //4、对成型剩余不满足最短上机天数的机台进行分配结构处理
        supplementCxMachineDistributionHandler.handlerTailCapacity(productionContext, allGroupPlanMap);
    }

    /**
     * 在模拟排产前的处理
     * 1、对测算在产机台收尾点的续作部分排产清除
     * 2、根据在产机台分配结果，构建分组计划的在产机台排产限制信息
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组计划
     * @param continueAllocationList 在产机台分配情况
     * @param allContinueMap         所有续作信息
     */
    private void beforeSimulateProductionHandler(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(allContinueMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //对测算成型产能分配的续作部分进行重排-先清空已排信息
        clearProductionInfoHandler.clearProductionData(productionContext);
        //在机结构对在产机台构建硫化组限制
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        allContinueMap.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
            List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
            if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(productionContext, structureName, null, null));
                return;
            }
            groupPlanInfo.buildDayProductionLimitInfoByContinue(context, continueCxMachineAllocation);
        });
    }

    /**
     * 1、对在机结构进行Sku的模具排产
     * 1.1、先对在机结构的续作部分进行模拟模具排产
     * 1.1.1、续作Sku模拟模具排产
     * 1.1.2、与续作Sku同规格同花纹的其它Sku模拟模具排产
     * 1.1.3、与续作Sku同生胎共用模具的其它Sku模拟模具排产
     * 1.2、再对在机结构的新增Sku，按Sku的优先级进行模拟模具排产
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组排产计划
     * @param continueAllocationList 在机机台产能分配
     * @param allContinueMap         续作信息
     */
    private void mouldProductionByContinueGroup(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (CollectionUtils.isEmpty(allContinueMap)) {
            log.info(TbrProductionGroupLogRecorder.addContinueSkuNoContinueGroupProductionLog(context));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、在机结构-在产机台-续作Sku排产
        productionContinue(ProductionStageEnum.SIMULATE_STAGE, productionContext, allContinueMap, allGroupPlanMap);
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        //2、在机结构-新增Sku排产 优先给特殊结构所在机台选择
        allContinueMap.entrySet().stream().sorted((entry1, entry2) -> {
                    // 判断结构是否包含特殊结构，优先给特殊结构所在机台选择
                    ProductionPlanGroupInfo before = allGroupPlanMap.get(entry1.getKey());
                    ProductionPlanGroupInfo after = allGroupPlanMap.get(entry2.getKey());
                    return groupPlanPrioritySelector.compareContinueGroup(before, after);
                })
                .forEach(entry -> {
                    String structureName = entry.getKey();
                    ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
                    if (null == groupPlanInfo) {
                        return;
                    }
                    List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
                    if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                        log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(productionContext, structureName, null, null));
                        return;
                    }
                    //在机结构-在产机台新增Sku排产 首先设置可排产的计划在本轮次可进行排产
                    groupPlanInfo.setThisRoundCanProduction();
                    //在机结构-新增Sku模拟排产
                    cxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(context, groupPlanInfo, new HashSet<>());
                    //再次设置可排产的计划在本轮次可进行排产
                    groupPlanInfo.setThisRoundCanProduction();
                    //处理需要提前收尾(需要调整到成型机台下的收尾点，包含成型机台最后一个配置的分配信息和成型机台剩余时间调整)
                    groupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, groupPlanInfo);
                    //设置收尾机台
                    continueCxMachineAllocation.forEach(cxMachineAllocation -> {
                        String cxMachineCode = cxMachineAllocation.getCxMachineCode();
                        CxMachineBaseInfoVo machineInfo = allCxMachineInfo.get(cxMachineCode);
                        Integer newRemainingDays = machineInfo.getRemainingDays();
                        //加入收尾匹配
                        if (newRemainingDays > BigDecimal.ZERO.intValue()) {
                            productionContext.addReverseMachine(machineInfo.getCxMachineCode());
                        }
                    });
                });
    }

    /**
     * 2、对还需排产的结构，获取优先级最高的结构进行机台匹配排产
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划需求量
     */
    private void addNewGroupPlanHandler(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        ProductionPlanGroupInfo addNewGroupPlan = cxCapacityAllocationHandler.getInsertNewGroupPlan(context, estimateGroupCxAllocationMap);
        if (null == addNewGroupPlan) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addNoGetAddGroupPlanLog(context));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = addNewGroupPlan.getGroupName();
        //最小分配天数
        Integer minAllocationDays = addNewGroupPlan.getMinAllocationDays(productionContext);
        Integer leftOverDays = addNewGroupPlan.getLeftOverNeedAllocationDays();
        //20260206 小于最短上机天数，则不进行分配
        if (!addNewGroupPlan.isNextAllocation(leftOverDays, productionContext)) {
            if (leftOverDays > BigDecimal.ZERO.intValue()) {
                log.info(TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, true, leftOverDays, minAllocationDays));
            }
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        //对挑选出的结构，匹配还有排产量的成型机台
        CxMachineBaseInfoVo selectedCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlan(context, addNewGroupPlan);
        if (null == selectedCxMachine) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, groupName));
            //20260109 标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        Set<Integer> hasProductionDaySet = selectedCxMachine.getSelectedProductionDaySet();
        Integer startDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
        //20260121 切换结构控制
        DayCapacityLimitVo dayCapacityLimitVo = productionContext.getBaseDataContainer().getDayCapacityLimit();
        Integer realChangeDay = dayCapacityLimitVo.confirmStartDayByChangeGroup(productionContext, startDay, groupName, selectedCxMachine, hasProductionDaySet);
        if (null == realChangeDay) {
            //记录日志
            Integer maxChangeLimit = productionContext.getBaseDataContainer().getParamConfiguration().getDayChangeGroupCount();
            log.info(TbrProductionGroupLogRecorder.addChangeGroupLimitCxMachineLog(context, selectedCxMachine.getCxMachineCode(), maxChangeLimit));
            //20260109 标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        ProductGroupCxCapacityInfo lhRatioInfo = addNewGroupPlan.getLhRatioByCxMachine(selectedCxMachine);
        startDay = realChangeDay;
        Set<Integer> realProductionDaySet = hasProductionDaySet.stream().filter(singleDay -> singleDay >= realChangeDay).collect(Collectors.toSet());
        Integer remainingDays = realProductionDaySet.size();
        //分配产能
        Integer needAllocationDays = addNewGroupPlan.getRemainingNeedAllocationDays();
        //20260209 特殊材料是否需要拉量或是舍弃
        CxMachineAllocationPlanHelper calculationAllocation = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, leftOverDays, startDay, context.getMonthDays());
        Integer confirmNeedAllocationDays = specialMaterialScheduleHandler.calculateConfirmAllocationDaysBySpecialMaterial(calculationAllocation, productionContext, addNewGroupPlan);
        if (null == confirmNeedAllocationDays || confirmNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            log.info(TbrProductionGroupLogRecorder.addSpecialMaterialStockLimitLog(context, groupName, true));
            //标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        needAllocationDays = Math.max(needAllocationDays, confirmNeedAllocationDays);
        Integer realAllocationDays = Math.min(remainingDays, needAllocationDays);
        //更新剩余天数
        addNewGroupPlan.updateLeftOverNeedAllocationDays(realAllocationDays);
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, realAllocationDays, startDay, context.getMonthDays());
        selectedCxMachine.addAllocationPlanInfo(context, addHelper);
        //对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper);
        if (needAllocationDays <= remainingDays) {
            //20260108 标记分配完成
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
        }
        //重新获取机台的剩余日 提前收尾
        Integer leftOver = selectedCxMachine.getRemainingDays();
        //反向机台匹配结构计划
        if (leftOver > BigDecimal.ZERO.intValue()) {
            cxCapacityAllocationHandler.selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, selectedCxMachine, new HashSet<>());
        }
        //下一新增结构
        addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
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
