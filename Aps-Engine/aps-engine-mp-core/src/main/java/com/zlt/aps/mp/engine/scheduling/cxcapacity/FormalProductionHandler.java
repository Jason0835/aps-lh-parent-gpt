package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.enums.MonthPlanNoProductionReasonEnum;
import com.zlt.aps.enums.ProductionGroupTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.engine.daylimit.MouldProductionLimitTypeEnum;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.ContinueTypeEnum;
import com.zlt.aps.mp.engine.enums.FormalRoundEnum;
import com.zlt.aps.mp.engine.enums.LogRecorderStageEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.*;
import com.zlt.aps.mp.engine.handler.statistics.DayProductionStatisticsHandler;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldFormalProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import com.zlt.aps.mp.engine.utils.NoProductionReasonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 开始正式排产，按结构进行排产
 * 此时已经确定了各个结构的机台分配情况
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormalProductionHandler extends OnLineGroupOnLineMachineHandler {

    private final CxAddSkuProductionHandler cxAddSkuProductionHandler;

    private final GroupPlanPrioritySelector groupPlanPrioritySelector;

    private final ClearProductionInfoHandler clearProductionInfoHandler;

    private final DayProductionStatisticsHandler dayProductionStatisticsHandler;

    private final DifferentGroupMoldAllocationAdjustHandler differentGroupMoldAllocationAdjustHandler;

    /**
     * 正式排产，对结构按已经分配好的机台产能进行排产
     * 先在机结构，其次新增结构
     * 1、在机结构先排产
     * 1.1、在机结构的续作Sku使用续作模具排产
     * 1.2、在机结构的续作Sku的同规格同花纹排产(还是续作模具)
     * 1.3、在机结构的续作Sku的同生胎同模具排产(还是续作模具)
     * 1.4、在机结构的新增Sku排产
     * 2、新增结构排产
     *
     * @param context           排产上下文
     * @param allGroupPlanInfo  所有结构信息
     * @param allAllocationList 结构排产分配信息
     * @param allContinueInfo   在机结构信息
     */
    public void productionContinueGroup(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allGroupPlanInfo) && CollectionUtils.isEmpty(allContinueInfo)) {
            //记录日志
            log.info(TbrMouldFormalProductionLogRecorder.addDataEmptyLog(context));
            return;
        }
        //20260626+ 续作排产计划量重新计算(因周期结构不在月周期结构可能因切换结构限制导致的延长)
        resetPlanQty(context, allGroupPlanInfo, allContinueInfo);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //没有分配到产能的结构，直接设置未排原因
        setNoConfigurationCapacityReasonByGroup(productionContext, allGroupPlanInfo);
        //初始化排产计数器
        productionContext.setProductionCounter(SkuProductionCounter.buildInit());
        allGroupPlanInfo.forEach((structureName, groupPlanInfo) -> groupPlanInfo.setThisRoundCanProduction());
        productionContext.addStageLogBuilder(LogRecorderStageEnum.FORMAL_PRODUCTION_CONTINUE);
        log.info(TbrMouldFormalProductionLogRecorder.addProductionContinueGroupLog(productionContext));
        //续作部分排产 1、续作Sku 2、续作Sku同规格同花纹高优先级量 3、续作Sku同生胎共模具高优先级量
        productionContinue(cxAddSkuProductionHandler, ProductionStageEnum.FORMAL_STAGE, productionContext, allContinueInfo, Collections.emptyList(), allGroupPlanInfo, allAllocationList);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
//        //4、一次性排产完毕
//        reachGroupLhMachines(productionContext, FormalRoundEnum.DISPOSABLE_LH_MACHINE, allGroupPlanInfo, allContinueInfo);
        productionContext.addStageLogBuilder(LogRecorderStageEnum.FORMAL_PRODUCTION_MIN_LH_MACHINE);
        //4、满足实单最低硫化机台数排产
        reachGroupLhMachines(productionContext, FormalRoundEnum.FIRST_ACTUAL_MIN_LH_MACHINE, allGroupPlanInfo, allContinueInfo);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        reachGroupLhMachines(productionContext, FormalRoundEnum.SECOND_ACTUAL_MIN_LH_MACHINE, allGroupPlanInfo, allContinueInfo);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        reachGroupLhMachines(productionContext, FormalRoundEnum.THIRD_ACTUAL_MIN_LH_MACHINE, allGroupPlanInfo, allContinueInfo);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        //5、按结构优先级，前段排产
        List<ProductionPlanGroupInfo> groupSortList = groupPlanPrioritySelector.sortGroupByFormalProduction(allGroupPlanInfo);
        if (!CollectionUtils.isEmpty(groupSortList)) {
            productionContext.addStageLogBuilder(LogRecorderStageEnum.FORMAL_PRODUCTION_BEFORE_LH_MACHINE);
            groupSortList.forEach(groupPlan -> productionGroupAddSku(productionContext, allGroupPlanInfo, groupPlan, FormalRoundEnum.FIRST_HALF_PRIORITY, ""));
            dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        }
        //6、按结构优先级、后段排产--新的排序
        List<ProductionPlanGroupInfo> newGroupSortList = groupPlanPrioritySelector.sortGroupByFormalProduction(allGroupPlanInfo);
        if (!CollectionUtils.isEmpty(newGroupSortList)) {
            productionContext.addStageLogBuilder(LogRecorderStageEnum.FORMAL_PRODUCTION_FINAL_LH_MACHINE);
            newGroupSortList.forEach(groupPlan -> productionGroupAddSku(productionContext, allGroupPlanInfo, groupPlan, FormalRoundEnum.LATTER_HALF_PRIORITY, ""));
        }
    }

    @Override
    public void handlerByMoldAllocationAdjust(Context context, ProductionStageEnum productionStage, Map<String, CxContinueInfoHelper> allContinueInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //说明在模拟阶段已经处理
        if (Boolean.TRUE.equals(productionContext.getHandlerMoldRatioDeductFlag())) {
            return;
        }
        //1、获取不同结构模具分配比例释放调整信息
        differentGroupMoldAllocationAdjustHandler.checkMoldRatioAllocation(context);
        TbrMouldFormalProductionLogRecorder.addFinishedByGroupMoldRatioLog(context);
        //2、清除续作排产数据
        clearProductionInfoHandler.resetBeforeFormalProduction(context, allGroupPlanInfo, allAllocationList);
        TbrMouldFormalProductionLogRecorder.addResetProductionContinueLog(context);
        //3、重排续作部分
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            productionContinueByType(cxAddSkuProductionHandler, productionStage, context, allGroupPlanInfo, structureName, cxContinueInfo, ContinueTypeEnum.SAME_SKU);
        });
    }

    /**
     * 排产结果后，设置未排原因
     *
     * @param productionContext 排产上下文
     * @param allGroupPlanInfo  所有分组计划
     * @param sumProductionMap  计划排产量
     */
    public void setNoProductionReasonAfterResult(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<Long, Integer> sumProductionMap) {
        //Sku排产限制情况
        Map<String, List<MouldProductionLimitTypeEnum>> skuProductionLimitInfo = productionContext.getSkuProductionLimitInfo();
        allGroupPlanInfo.forEach((structureName, groupPlan) -> {
            if (CollectionUtils.isEmpty(groupPlan.getAllocationCxMachineCodeSet())) {
                return;
            }
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlan.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                return;
            }
            groupPlanData.forEach(singlePlan -> {
                if (singlePlan.getOriginProductionQty() <= BigDecimal.ZERO.intValue()) {
                    return;
                }
                if (YesOrNoEnum.NO.getCode().equals(singlePlan.getIsProduction())) {
                    return;
                }
                Integer realProductionQty = sumProductionMap.getOrDefault(singlePlan.getMonthPlanId(), BigDecimal.ZERO.intValue());
                Integer diffValue = singlePlan.getFactProdReqQty() - realProductionQty;
                if (diffValue <= BigDecimal.ZERO.intValue()) {
                    return;
                }
                boolean hasMouldCapacity = SkuMouldSelector.hasMouldCapacity(productionContext, singlePlan.getMaterialDesc());
                MonthPlanNoProductionReasonEnum defaultReason;
                if (hasMouldCapacity) {
                    defaultReason = MonthPlanNoProductionReasonEnum.NO_ENOUGH_CX_MACHINE_CAPACITY;
                } else {
                    defaultReason = MonthPlanNoProductionReasonEnum.NO_ENOUGH_MOULD_CAPACITY;
                }
                List<MouldProductionLimitTypeEnum> limitInfoList = skuProductionLimitInfo.get(singlePlan.getMaterialDesc());
                //20260208 部分未排及不排判断
                MonthPlanNoProductionReasonEnum generalNoProductionReason = MonthPlanNoProductionReasonEnum.GENERAL_NO_PRODUCTION_REASON;
                if (realProductionQty > BigDecimal.ZERO.intValue()) {
                    generalNoProductionReason = MonthPlanNoProductionReasonEnum.GENERAL_PART_NO_PRODUCTION_REASON;
                }
                String noProductionReason = NoProductionReasonUtils.getNoProductionReasonByLimit(generalNoProductionReason, limitInfoList, defaultReason);
                singlePlan.singleAddNoProductionReason(noProductionReason);
            });
        });
    }

    /**
     * 20260626+ 续作结构，不在月周期结构清单，又因切换结构限制导致的延长
     * 需要重新调整续作Sku的计划量信息
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有结构
     * @param allContinueInfo  续作Sku信息
     */
    private void resetPlanQty(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        if (CollectionUtils.isEmpty(allContinueInfo) || CollectionUtils.isEmpty(allGroupPlanInfo)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Set<String> monthProductionCycleSet = productionContext.getBaseDataContainer().getMonthProductionCycleList();
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo continueGroup = allGroupPlanInfo.get(structureName);
            if (null == continueGroup) {
                return;
            }
            if (!ProductionGroupTypeEnum.CYCLE.getGroupType().equals(continueGroup.getStructureType())) {
                //不是周期结构
                return;
            }
            if (monthProductionCycleSet.contains(structureName)) {
                //在月周期清单中
                return;
            }
            Set<String> realAllocationCxMachineInfo = continueGroup.getAllocationCxMachineCodeSet();
            if (CollectionUtils.isEmpty(realAllocationCxMachineInfo)) {
                //没有实际分配机台，跳过
                return;
            }
            ContinueSkuCalculator.setContinueSkuPlanDemandQty(context, continueGroup, cxContinueInfo);
        });
    }

    /**
     * @param context          排产上下文
     * @param round            轮次
     * @param allGroupPlanInfo 所有分组计划
     * @param allContinueInfo  在机分组计划
     */
    private void reachGroupLhMachines(Context context, FormalRoundEnum round, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, Map<String, CxContinueInfoHelper> allContinueInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //1、在机机构新增Sku排产
        allContinueInfo.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(structureName);
            if (null == groupPlan) {
                return;
            }
            productionGroupAddSku(productionContext, allGroupPlanInfo, groupPlan, round, "在机");
        });
        //2、非在机结构，新增规格排产
        allGroupPlanInfo.forEach((structureName, groupPlan) -> {
            if (allContinueInfo.containsKey(structureName)) {
                return;
            }
            productionGroupAddSku(productionContext, allGroupPlanInfo, groupPlan, round, "新增");
        });
    }

    /**
     * 设置没有分配产能导致整个结构不排的未排原因
     *
     * @param productionContext
     * @param allGroupPlanInfo
     */
    private void setNoConfigurationCapacityReasonByGroup(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo) {
        String noAllocationCxMachineCapacity = NoProductionReasonUtils.getNoProductionReason(MonthPlanNoProductionReasonEnum.NO_PRODUCTION_CX_MACHINE, "");
        allGroupPlanInfo.forEach((structureName, groupPlan) -> {
            if (!CollectionUtils.isEmpty(groupPlan.getAllocationCxMachineCodeSet())) {
                return;
            }
            //没有分配到成型产能
            List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlan.getGroupPlanData();
            if (CollectionUtils.isEmpty(groupPlanData)) {
                return;
            }
            List<MonthPlanProductionRequirePlanVo> effectivePlanList = groupPlanData.stream().filter(single -> single.hasProduction()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(effectivePlanList)) {
                return;
            }
            effectivePlanList.forEach(singlePlan -> singlePlan.setNoProductionAndAddReason(noAllocationCxMachineCapacity));
        });
    }

    /**
     * 对分组计划进行新增Sku排产
     *
     * @param context          排产上下文
     * @param allGroupPlanInfo 所有分组计划
     * @param groupPlan        当前排产的分组计划
     * @param round            轮次
     * @param desc             说明
     */
    private void productionGroupAddSku(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, ProductionPlanGroupInfo groupPlan, FormalRoundEnum round, String desc) {
        String structureName = groupPlan.getGroupName();
        TbrMouldFormalProductionLogRecorder.addProductionSingleGroupAddSkuLog(context, structureName, round, desc);
        // 设置当前结构 剩余的每日硫化机台数 sandy+ 2026.3.22
        cxAddSkuProductionHandler.setRemainLhMachineCount(context, allGroupPlanInfo, structureName);
        //4.1 初始日产能限制信息，用于统计使用
        groupPlan.initMpDailyCapacityLimit(context);
        //4.2 SKU排产
        cxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(context, ProductionStageEnum.FORMAL_STAGE, round, groupPlan, new HashSet<>());
        //4.3 重新计算统计产能
        groupPlan.reCalcMpDailyCapacityLimit(context);
    }

}
