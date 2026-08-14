package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.daylimit.DayCapacityLimitVo;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.LogRecorderStageEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.*;
import com.zlt.aps.mp.engine.handler.appoint.GroupAppointBusinessHandler;
import com.zlt.aps.mp.engine.logrecorder.KeyInformationLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrProductionGroupLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSimulateProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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

    private final CycleGroupProductionHandler cycleGroupProductionHandler;

    private final CxCapacityAllocationHandler cxCapacityAllocationHandler;

    private final GroupAppointBusinessHandler groupAppointBusinessHandler;

    private final DayProductionStatisticsHandler dayProductionStatisticsHandler;

    private final SpecialMaterialScheduleHandler specialMaterialScheduleHandler;

    private final GroupPriorityProductionScheduler groupPriorityProductionScheduler;

    private final SupplementCxMachineDistributionHandler supplementCxMachineDistributionHandler;

    private final DifferentGroupMoldAllocationAdjustHandler differentGroupMoldAllocationAdjustHandler;

    /**
     * 模拟排产计划
     * 1、在机结构对在产成型机台进行模拟模具排产
     * //clearSimulateDataAndResetProductionContinue(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有排产分组计划
     * @param continueAllocationList 在产机构在产机台的分配情况
     * @param allContinueMap         所有续作Sku
     */
    public void productionGroupPlan(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //设置收尾机台信息-空
        productionContext.setReverseFindSet(new HashSet<>());
        Integer productionMode = productionContext.getBaseDataContainer().getParamConfiguration().getProductionMode();
        //1、模拟排产前的数据处理
        clearProductionInfoHandler.beforeSimulateProductionHandler(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        log.info(TbrSimulateProductionLogRecorder.addResetDataFinishLog(productionContext));
        //2、在机结构对在产成型机台进行模拟模具排产
        mouldProductionByContinueGroup(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        KeyInformationLogRecorder.recorderContinueCxMachineProductionLog(productionContext, allGroupPlanMap, allContinueMap);
        TbrSimulateProductionLogRecorder.addProductionModeLog(productionContext, productionMode);
        //打印在产-日产和换膜信息
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        //20280811+ 指定业务处理
        continueAdjustByAppoint(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        if (YesOrNoEnum.YES.getValue().equals(productionMode)) {
            //交付优先，在机分组之后，按分组的高优先级排序，优先级高的分组先进行排产
            deliveryPriorityProduction(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        } else {
            //效率优先，在机分组之后，按收尾机台(排除空机台)的切换匹配分组待需分配排产
            efficiencyPriorityProduction(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        }
        KeyInformationLogRecorder.recorderAllAllocationGroupInfoLog(productionContext);
        //6、对成型剩余不满足最短上机天数的机台进行分配结构处理
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_SUPPLEMENT_PRODUCTION);
        supplementCxMachineDistributionHandler.handlerTailCapacity(productionContext, allGroupPlanMap);
        //7、20260731+ 对不在月周期结构清单的在产周期结构延长进行调整处理
        cycleGroupProductionHandler.handlerNoMonthRangeCycleGroupByTimeExtension(productionContext, allContinueMap);
    }

    @Override
    public void handlerByMoldAllocationAdjust(Context context, ProductionStageEnum productionStage, Map<String, CxContinueInfoHelper> allContinueInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、获取不同结构模具分配比例释放调整信息
        differentGroupMoldAllocationAdjustHandler.checkMoldRatioAllocation(context);
        productionContext.setHandlerMoldRatioDeductFlag(Boolean.TRUE);
        TbrSimulateProductionLogRecorder.addFinishedByGroupMoldRatioLog(context);
        //2、清除续作排产数据
        clearProductionInfoHandler.beforeSimulateProductionHandler(context, allGroupPlanInfo, continueAllocationList, allContinueInfo);
        TbrSimulateProductionLogRecorder.addResetProductionContinueLog(context);
        //3、重排续作部分
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            productionContinueByType(cxAddSkuProductionHandler, productionStage, context, allGroupPlanInfo, structureName, cxContinueInfo, ContinueTypeEnum.SAME_SKU);
        });
    }

    /**
     * 在机分组调整(TBR-结构),因指定业务处理
     *
     * @param productionContext
     * @param allGroupPlanMap
     * @param continueAllocationList
     * @param allContinueMap
     */
    private void continueAdjustByAppoint(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_APPOINT_ADJUST_PRODUCTION);
        boolean isContinueAdjust = groupAppointBusinessHandler.hasContinueGroupAdjust(productionContext, continueAllocationList, allContinueMap);
        if (!isContinueAdjust) {
            //指定优先排产
            groupAppointBusinessHandler.appointPriority(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
            return;
        }
        //在机分组(TBR-结构)重排
        resetProduction(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
    }

    /**
     * 以订单交付性角度：即高优先级订单结构优先
     * 交付优先排产模式
     *
     * @param productionContext
     */
    private void deliveryPriorityProduction(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //1、按高优先级，进行不指定预排：得到预期排产的分组信息
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_DELIVERY_PRIORITY_PRODUCTION);
        Set<String> preSelectedGroupSet = Sets.newHashSet();
        Map<String, Set<CxMachineAllocationPlanHelper>> preSelectedGroupAllocationMap = Maps.newHashMap();
        TbrSimulateProductionLogRecorder.addStartDeliveryPriorityLog(productionContext);
        groupPriorityProductionScheduler.allocationCxMachine(productionContext, Sets.newHashSet(), preSelectedGroupSet, preSelectedGroupAllocationMap, Sets.newHashSet());
        TbrSimulateProductionLogRecorder.addEndDeliveryPriorityLog(productionContext);
        KeyInformationLogRecorder.recorderInsertAllocationGroupInfoLog(productionContext, preSelectedGroupAllocationMap);
        //2、判断预期排产分组中是否有设置固定1~3的分组和排产间断的二次上机
        List<ProductionPlanGroupInfo> discontinueGroupList = getDiscontinuePreSelectedGroup(productionContext, preSelectedGroupAllocationMap);
        List<ProductionPlanGroupInfo> hasFixedPriorityCxMachineList = getGroupFixedCxMachine(productionContext, preSelectedGroupSet);
        Set<String> multipleRangeGroupSet = getMultipleRangeGroup(preSelectedGroupAllocationMap);
        if (CollectionUtils.isEmpty(hasFixedPriorityCxMachineList) && CollectionUtils.isEmpty(discontinueGroupList) && CollectionUtils.isEmpty(multipleRangeGroupSet)) {
            return;
        }
        Set<String> discontinueGroupSet = discontinueGroupList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet());
        //3、开始重排在产分组在产机台续作
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_RESET_CONTINUE_PRODUCTION);
        resetProduction(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        //4、对固定、多段、间断分组进行优先排产
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_FIXED_PRODUCTION);
        TbrSimulateProductionLogRecorder.addDeliveryPriorityFixedCxMachineGroupLog(productionContext);
        Map<String, Set<CxMachineAllocationPlanHelper>> specialPriorityResultMap = Maps.newHashMap();
        //4.1、多固定同一成型机台，时间在前的先排
        groupPriorityProductionScheduler.allocationFixedGroupSameCxMachineEarlyGroup(productionContext, Sets.newHashSet(), hasFixedPriorityCxMachineList, discontinueGroupSet, specialPriorityResultMap);
        //4.2、对有多段的固定最先排
        List<ProductionPlanGroupInfo> multipleRangeFixedPriorityCxMachineList = hasFixedPriorityCxMachineList.stream().filter(singleGroup -> multipleRangeGroupSet.contains(singleGroup.getGroupName())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(multipleRangeFixedPriorityCxMachineList)) {
            String groupInfo = multipleRangeFixedPriorityCxMachineList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.joining(StringConstant.COMMA));
            String typeText = "多段分配指定分组优先排产";
            TbrSimulateProductionLogRecorder.addDeliveryPriorityTypeLog(productionContext, groupInfo, typeText);
            Set<String> multipleDiscontinueGroupSet = Sets.newHashSet();
            multipleRangeFixedPriorityCxMachineList.forEach((singleGroup -> multipleDiscontinueGroupSet.add(singleGroup.getGroupName())));
            groupPriorityProductionScheduler.productionAppointGroupCxMachine(productionContext, Sets.newHashSet(), multipleRangeFixedPriorityCxMachineList, multipleDiscontinueGroupSet, true, specialPriorityResultMap);
        }
        //4.3 对有多段的非固定优先排产
        List<ProductionPlanGroupInfo> multipleRangeNoFixedList = getMultipleNoFixedGroup(productionContext, hasFixedPriorityCxMachineList, multipleRangeGroupSet);
        if (!CollectionUtils.isEmpty(multipleRangeNoFixedList)) {
            String groupInfo = multipleRangeNoFixedList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.joining(StringConstant.COMMA));
            String typeText = "多段分配非指定分组优先排产";
            TbrSimulateProductionLogRecorder.addDeliveryPriorityTypeLog(productionContext, groupInfo, typeText);
            Set<String> multipleDiscontinueGroupSet = Sets.newHashSet();
            multipleRangeNoFixedList.forEach((singleGroup -> multipleDiscontinueGroupSet.add(singleGroup.getGroupName())));
            groupPriorityProductionScheduler.productionAppointGroupCxMachine(productionContext, Sets.newHashSet(), multipleRangeNoFixedList, multipleDiscontinueGroupSet, false, specialPriorityResultMap);
        }
        //4.4、对其它固定先排
        List<ProductionPlanGroupInfo> otherFixedPriorityCxMachineList = hasFixedPriorityCxMachineList.stream().filter(singleGroup -> !multipleRangeGroupSet.contains(singleGroup.getGroupName())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(otherFixedPriorityCxMachineList)) {
            String groupInfo = otherFixedPriorityCxMachineList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.joining(StringConstant.COMMA));
            String typeText = "其它指定分组优先排产";
            TbrSimulateProductionLogRecorder.addDeliveryPriorityTypeLog(productionContext, groupInfo, typeText);
            groupPriorityProductionScheduler.productionAppointGroupCxMachine(productionContext, Sets.newHashSet(), otherFixedPriorityCxMachineList, discontinueGroupSet, true, specialPriorityResultMap);
        }
        KeyInformationLogRecorder.recorderInsertAllocationGroupInfoLog(productionContext, specialPriorityResultMap);
        //5、在对剩余的进行Top3排产
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_LAST_PRODUCTION);
        TbrSimulateProductionLogRecorder.addDeliveryPriorityLeftOverGroupLog(productionContext);
        //5.2 剩余排产
        Map<String, Set<CxMachineAllocationPlanHelper>> finalAddResultMap = Maps.newHashMap();
        groupPriorityProductionScheduler.allocationCxMachine(productionContext, Sets.newHashSet(), Sets.newHashSet(), finalAddResultMap, discontinueGroupSet);
        KeyInformationLogRecorder.recorderInsertAllocationGroupInfoLog(productionContext, finalAddResultMap);
    }

    /**
     * 以生产角度：生产切换角度
     * 效率优先排产
     * 1、对收尾机台(空机台剔除),从机台角度选择分组计划
     * 2、对剩余还需排产分组，以分组角度，按优先级获取最高优先级分组，挑选可排产机台(此时空机台参与排产)
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有分组对象集合
     * @param continueAllocationList 所有在产分组-对在产机台的分配信息集合
     * @param allContinueMap         所有在产分组续作Sku信息集合
     */
    private void efficiencyPriorityProduction(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //1、对在产机台-收尾成型机台，反向匹配待排结构
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_EFFICIENCY_PRIORITY_PRODUCTION);
        cxCapacityAllocationHandler.reverseMachineAllocation(productionContext, allGroupPlanMap);
        //2、对结构重新标记分配完成情况--还需分配量>最小上机时间的结构，重新标记没有分配完成
        resetFlagAllocationFinish(productionContext, allGroupPlanMap);
        //3、对还需排产结构，获取优先级最高的结构--结构新增
        productionContext.addStageLogBuilder(LogRecorderStageEnum.SIMULATE_EFFICIENCY_ADD_PRODUCTION);
        addNewGroupPlanHandler(productionContext, allGroupPlanMap, new HashSet<>());
    }

    /**
     * 重新开始模拟排产：
     * 1、清空收尾机台设置
     * 2、将成型产能分配还原到在产分组对在产机台的初始分配(测算分配)
     * 3、在产分组对在产机台已分配情况进行模拟排产
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有分组对象集合
     * @param continueAllocationList 所有在产机台对在产分组的分配信息集合
     * @param allContinueMap         所有续作Sku集合
     */
    private void resetProduction(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //1、清空机台收尾设置
        productionContext.setReverseFindSet(new HashSet<>());
        //2、还原设置(包含在产机台对在产分配的续作分配)
        clearProductionInfoHandler.resetProductionBySimulateProductionHandler(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        TbrSimulateProductionLogRecorder.addDeliveryPriorityResetContinueLog(productionContext);
        KeyInformationLogRecorder.recorderContinueAllocationGroupInfoLog(productionContext, allGroupPlanMap, allContinueMap, continueAllocationList);
        //3、在机结构对在产成型机台进行模拟模具排产
        mouldProductionByContinueGroup(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        KeyInformationLogRecorder.recorderContinueCxMachineProductionLog(productionContext, allGroupPlanMap, allContinueMap);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        //4、指定优先排产
        groupAppointBusinessHandler.appointPriority(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);

    }

    /**
     * 2、对在机结构进行Sku的模具排产
     * 2.1、先对在机结构的续作部分进行模拟模具排产
     * 2.1.1、续作Sku模拟模具排产
     * 2.1.2、与续作Sku同规格同花纹的其它Sku模拟模具排产
     * 2.1.3、与续作Sku同生胎共用模具的其它Sku模拟模具排产
     * 2.2、再对在机结构的新增Sku，按Sku的优先级进行模拟模具排产
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
        productionContinue(cxAddSkuProductionHandler, ProductionStageEnum.SIMULATE_STAGE, productionContext, allContinueMap, continueAllocationList, allGroupPlanMap, Collections.emptyList());
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        TbrSimulateProductionLogRecorder.addEndContinueSkuProductionLog(productionContext);
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
                    cxAddSkuProductionHandler.productionAddSkuBySingleGroup(context, ProductionStageEnum.SIMULATE_STAGE, groupPlanInfo, structureName, entry.getValue(), continueCxMachineAllocation, new HashSet<>());
                });
    }

    /**
     * 4、重新标记分配完成标记
     * 剩余需求量>最小上机时间的结构，置为分配没完成
     *
     * @param context     排产上下文
     * @param allGroupMap 所有分组计划信息
     */
    private void resetFlagAllocationFinish(Context context, Map<String, ProductionPlanGroupInfo> allGroupMap) {
        if (CollectionUtils.isEmpty(allGroupMap)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        allGroupMap.forEach((groupName, groupPlanInfo) -> {
            Integer realLeftOverDays = groupPlanInfo.getBoostReplenishmentQuota();
            if (realLeftOverDays <= BigDecimal.ZERO.intValue()) {
                return;
            }
            if (!groupPlanInfo.isNextAllocation(realLeftOverDays, productionContext, false)) {
                return;
            }
            groupPlanInfo.setIsAllocationFinish(YesOrNoEnum.NO.getValue());
        });
    }

    /**
     * 5、对还需排产的结构，获取优先级最高的结构进行机台匹配排产
     *
     * @param context          排产上下文
     * @param allGroupPlanMap  分组计划需求量
     * @param excludeGroupPlan 不再参与的分组
     */
    private void addNewGroupPlanHandler(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, Set<String> excludeGroupPlan) {
        TbrProductionGroupLogRecorder.addStartGroupSelectedCxMachineLog(context);
        ProductionPlanGroupInfo addNewGroupPlan = cxCapacityAllocationHandler.getInsertNewGroupPlan(context, allGroupPlanMap, excludeGroupPlan);
        if (null == addNewGroupPlan) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addNoGetAddGroupPlanLog(context));
            return;
        }
        String groupName = addNewGroupPlan.getGroupName();
        //对挑选出的结构，匹配还有排产量的成型机台
        CxMachineBaseInfoVo selectedCxMachine = cxCapacityAllocationHandler.selectedCxMachineForGroupPlan(context, addNewGroupPlan);
        if (null == selectedCxMachine) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, groupName));
            //20260109 标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, allGroupPlanMap, new HashSet<>());
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        boolean isChangeProSize = selectedCxMachine.isChangeProSize(productionContext, addNewGroupPlan);
        Set<Integer> hasProductionDaySet = selectedCxMachine.getSelectedProductionDaySet();
        Integer startDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
        Integer leftOverDays = addNewGroupPlan.getLeftOverNeedAllocationDays();
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
            addNewGroupPlanHandler(context, allGroupPlanMap, new HashSet<>());
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
        calculationAllocation.setChangeProSize(isChangeProSize);
        Integer confirmNeedAllocationDays = specialMaterialScheduleHandler.calculateConfirmAllocationDaysBySpecialMaterial(calculationAllocation, productionContext, addNewGroupPlan);
        if (null == confirmNeedAllocationDays || confirmNeedAllocationDays <= BigDecimal.ZERO.intValue()) {
            log.info(TbrProductionGroupLogRecorder.addSpecialMaterialStockLimitLog(context, groupName, true));
            //标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, allGroupPlanMap, new HashSet<>());
            return;
        }
        needAllocationDays = Math.max(needAllocationDays, confirmNeedAllocationDays);
        Integer realAllocationDays = Math.min(remainingDays, needAllocationDays);
        //20260206 小于最短上机天数，则不进行分配
        if (!addNewGroupPlan.isNextAllocation(realAllocationDays, productionContext, isChangeProSize)) {
            //最小分配天数
            Integer minAllocationDays = addNewGroupPlan.getMinAllocationDays(productionContext, isChangeProSize);
            if (realAllocationDays > BigDecimal.ZERO.intValue()) {
                log.info(TbrProductionGroupLogRecorder.addGroupLeftOverNoReachMinAllocationDayLog(productionContext, groupName, true, realAllocationDays, minAllocationDays));
            }
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            addNewGroupPlanHandler(context, allGroupPlanMap, new HashSet<>());
            return;
        }
        Integer originNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        Integer originLeftOverByCxMachine = selectedCxMachine.getRemainingDays();
        //更新剩余天数：分组的剩余天数、成型机台剩余可分配天数
        addNewGroupPlan.updateLeftOverNeedAllocationDays(realAllocationDays);
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, realAllocationDays, startDay, context.getMonthDays());
        CxMachineAllocationPlanHelper beforeGroupAllocation = selectedCxMachine.addAllocationPlanInfo(context, addHelper);
        //20260429+ 存储前分组分配信息，用以前分组是否需要强制延长
        addHelper.setBeforeAllocationByChangeLimit(beforeGroupAllocation);
        //对成型机台进行模拟模具排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, false, selectedCxMachine.getCxMachineCode(), addHelper, new HashSet<>(), true);
        //20260323 重新获取剩余天数：可能因提前收尾变化，导致计划实际没有排，下轮直接排除,不能比较分配完成
        Integer newNeedAllocationDaysByGroupPlan = addNewGroupPlan.getLeftOverNeedAllocationDays();
        if (newNeedAllocationDaysByGroupPlan.equals(originNeedAllocationDaysByGroupPlan)) {
            excludeGroupPlan.add(groupName);
        } else {
            excludeGroupPlan.remove(groupName);
        }
        //20260429+ 前分组分配强制延长处理
        cxMouldProductionHandler.handlerTimeExtensionDayConclusionByBeforeGroup(productionContext, addHelper);
        //重新获取机台的剩余日：可能因提前收尾变化，导致实际分配天数与初始分配天数不一致
        Integer leftOver = selectedCxMachine.getRemainingDays();
        boolean isProductionByCxMachine = !originLeftOverByCxMachine.equals(leftOver);
        //有排产：机台反向匹配分组计划
        if (isProductionByCxMachine && leftOver > BigDecimal.ZERO.intValue()) {
            cxCapacityAllocationHandler.selectedGroupPlanByCxMachine(context, allGroupPlanMap, selectedCxMachine, new HashSet<>());
        }
        //下一新增结构
        if (isProductionByCxMachine) {
            resetFlagAllocationFinish(context, allGroupPlanMap);
        }
        addNewGroupPlanHandler(context, allGroupPlanMap, excludeGroupPlan);
    }

    /**
     * 对预期排产分组，是否有固定分组，如果有则固定分组优先选择固定机台
     *
     * @param context             排产上下文
     * @param preSelectedGroupSet Top之后的预排分组集合
     * @return 返回有设置固定1~固定3的分组集合
     */
    private List<ProductionPlanGroupInfo> getGroupFixedCxMachine(Context context, Set<String> preSelectedGroupSet) {
        if (CollectionUtils.isEmpty(preSelectedGroupSet)) {
            return Collections.emptyList();
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupMap = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupMap)) {
            return Collections.emptyList();
        }
        List<ProductionPlanGroupInfo> priorityFixedGroupList = Lists.newArrayList();
        preSelectedGroupSet.forEach(groupName -> {
            ProductionPlanGroupInfo groupInfo = allGroupMap.get(groupName);
            if (null == groupInfo) {
                return;
            }
            if (CollectionUtils.isEmpty(groupInfo.getPriorityFixedCxMachineSet())) {
                return;
            }
            priorityFixedGroupList.add(groupInfo);
        });
        if (CollectionUtils.isEmpty(priorityFixedGroupList)) {
            return Collections.emptyList();
        }
        return priorityFixedGroupList;
    }

    /**
     * 获取有多段排产的分组对象集合
     *
     * @param preSelectedGroupAllocationMap 预排分组的预分配信息集合
     * @return
     */
    private Set<String> getMultipleRangeGroup(Map<String, Set<CxMachineAllocationPlanHelper>> preSelectedGroupAllocationMap) {
        if (CollectionUtils.isEmpty(preSelectedGroupAllocationMap)) {
            return Collections.emptySet();
        }
        Set<String> multipleRangeSet = Sets.newHashSet();
        preSelectedGroupAllocationMap.forEach((groupName, preAllocationList) -> {
            if (CollectionUtils.isEmpty(preAllocationList)) {
                return;
            }
            if (preAllocationList.size() <= BigDecimal.ONE.intValue()) {
                return;
            }
            multipleRangeSet.add(groupName);
        });
        return multipleRangeSet;
    }

    /**
     * 获取非固定且多段的分组对象集合
     *
     * @param productionContext     排产上下文
     * @param fixedPriorityList     固定机台分组对象集合
     * @param multipleRangeGroupSet 预排中多段分组对象集合
     * @return
     */
    private List<ProductionPlanGroupInfo> getMultipleNoFixedGroup(TbrProductionContext productionContext, List<ProductionPlanGroupInfo> fixedPriorityList, Set<String> multipleRangeGroupSet) {
        if (CollectionUtils.isEmpty(multipleRangeGroupSet)) {
            return Collections.emptyList();
        }
        Map<String, ProductionPlanGroupInfo> allGroupInfoMap = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupInfoMap)) {
            return Collections.emptyList();
        }
        Set<String> fixedGroupSet = CollectionUtils.isEmpty(fixedPriorityList) ? Collections.emptySet() : fixedPriorityList.stream().map(ProductionPlanGroupInfo::getGroupName).collect(Collectors.toSet());
        List<ProductionPlanGroupInfo> noFixedMultipleList = Lists.newArrayList();
        allGroupInfoMap.forEach((groupName, groupPlanInfo) -> {
            if (!multipleRangeGroupSet.contains(groupName)) {
                return;
            }
            if (fixedGroupSet.contains(groupName)) {
                return;
            }
            noFixedMultipleList.add(groupPlanInfo);
        });
        return noFixedMultipleList;
    }

    /**
     * 获取预排中有间断分配的分组对象集合
     *
     * @param productionContext             排产上下文
     * @param preSelectedGroupAllocationMap 分组预排的预分配信息
     * @return
     */
    private List<ProductionPlanGroupInfo> getDiscontinuePreSelectedGroup(TbrProductionContext productionContext, Map<String, Set<CxMachineAllocationPlanHelper>> preSelectedGroupAllocationMap) {
        if (CollectionUtils.isEmpty(preSelectedGroupAllocationMap)) {
            return Collections.emptyList();
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return Collections.emptyList();
        }
        Map<String, ProductionPlanGroupInfo> allGroupInfo = productionContext.getGroupProductionInfo();
        if (CollectionUtils.isEmpty(allGroupInfo)) {
            return Collections.emptyList();
        }
        List<GroupPreAllocationInfoHelper> preAllocationGroupInfoList = Lists.newArrayList();
        //信息转化，分组排产日及对应机台的停产日信息
        preSelectedGroupAllocationMap.forEach((groupName, preAllocationInfo) -> {
            ProductionPlanGroupInfo groupInfo = allGroupInfo.get(groupName);
            if (CollectionUtils.isEmpty(preAllocationInfo) || null == groupInfo) {
                return;
            }
            Set<Integer> preProductionDaySet = Sets.newHashSet();
            Set<Integer> stopDaySet = Sets.newHashSet();
            GroupProductionAllocationHelper.setProductionAndStopDayInfo(productionContext, preProductionDaySet, stopDaySet, preAllocationInfo, allCxMachineInfo);
            if (CollectionUtils.isEmpty(preProductionDaySet)) {
                return;
            }
            preAllocationGroupInfoList.add(new GroupPreAllocationInfoHelper(groupName, groupInfo, preProductionDaySet, stopDaySet));
        });
        //从预分配信息集合中，挑选排产日有中断的分组对象集合
        if (CollectionUtils.isEmpty(preAllocationGroupInfoList)) {
            return Collections.emptyList();
        }
        List<GroupPreAllocationInfoHelper> discontinueList = preAllocationGroupInfoList.stream().filter(singleGroup -> singleGroup.hasDiscontinueProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(discontinueList)) {
            return Collections.emptyList();
        }
        return discontinueList.stream().map(GroupPreAllocationInfoHelper::getGroupInfo).collect(Collectors.toList());
    }

}
