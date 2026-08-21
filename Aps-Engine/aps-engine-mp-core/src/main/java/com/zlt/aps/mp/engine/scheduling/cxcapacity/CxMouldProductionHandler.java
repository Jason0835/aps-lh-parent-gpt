package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mp.engine.daylimit.BeforeSkuProductionInfo;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.handler.statistics.DayProductionStatisticsHandler;
import com.zlt.aps.mp.engine.handler.SimulateProductionSnapshotHandler;
import com.zlt.aps.mp.engine.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 单成型产能-模具排产业务处理
 *
 * @author ZLT
 * @date 20251217
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CxMouldProductionHandler {
    /**
     * 数据备份处理器
     */
    private final SimulateProductionSnapshotHandler simulateProductionSnapshotHandler;
    /**
     * Sku排产处理器
     */
    private final CxAddSkuProductionHandler cxAddSkuProductionHandler;
    /**
     * 结构分配延长处理器
     */
    private final GroupTimeExtensionHandler groupTimeExtensionHandler;
    /**
     * 结构提前收尾业务处理器
     */
    private final GroupPlanBeforeConclusionHandler groupPlanBeforeConclusionHandler;

    private final DayProductionStatisticsHandler dayProductionStatisticsHandler;

    /**
     * 非在机结构，模具排产
     *
     * @param context               排产上下文
     * @param isIgnoreHighPriority  是否忽略高优先级机台数判断
     * @param cxMachineCode         成型机台
     * @param productionPlan        分配段
     * @param handledDayInfo        已延长处理日期
     * @param isForcedTimeExtension 是否需要强制延长探测处理，默认需要
     */
    public void noContinueGroupPlanMouldProduction(Context context,
                                                   boolean isIgnoreHighPriority,
                                                   String cxMachineCode,
                                                   CxMachineAllocationPlanHelper productionPlan,
                                                   Set<String> handledDayInfo,
                                                   boolean isForcedTimeExtension) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
        String groupName = productionPlanInfo.getGroupName();
        Integer originStartDay = productionPlan.getStartDay();
        Integer originEndDay = productionPlan.getEndDay();
        TbrMouldProductionLogRecorder.addStartCxMachineMouldProductionPlanLog(context, cxMachineCode, groupName);
        dayProductionStatisticsHandler.printDayLimitKeyInformationLog(productionContext);
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlanInfo.getGroupPlanData();
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            //记录日志
            TbrMouldProductionLogRecorder.addGroupCxMachineMouldNoPlanLog(context, groupName, cxMachineCode);
            return;
        }
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            //记录日志
            TbrMouldProductionLogRecorder.addGroupCxMachineMouldNoFindMachineInfoLog(context, groupName, cxMachineCode);
            return;
        }
        //根据新的分组计划，构建新的硫化配比
        Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap = productionPlanInfo.getCxMachineLhRationMap();
        if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
            //记录日志
            TbrMouldProductionLogRecorder.addGroupCxMachineMouldGroupNoRatioLog(context, groupName, cxMachineCode);
            return;
        }
        String machineTypeCode = cxMachineInfo.getCxMachineTypeCode();
        MonthPlanStructureLhRatioVo cxLhRatio = productionPlanInfo.getLhRatio(cxMachineInfo);
        if (null == cxLhRatio) {
            //记录日志
            TbrMouldProductionLogRecorder.addGroupCxMachineMouldGroupNoBrandRatioLog(context, groupName, cxMachineCode, machineTypeCode);
            return;
        }
        cxMachineInfo.setRatio(cxLhRatio.getLhMachineMaxQty());
        buildNewLhConclusionInfo(context, cxMachineInfo, cxLhRatio, productionPlan);
        //20260108 开启本轮可排产
        productionPlanInfo.setThisRoundCanProduction();
        cxAddSkuProductionHandler.productionAddSku(context, cxMachineCode, hasProductionPlanList, productionPlan, productionContext.getBaseDataContainer().getMouldShellMap(), new HashSet<>());
        //处理结构提前收尾
        groupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, isIgnoreHighPriority, productionPlanInfo, cxMachineInfo, cxLhRatio, productionPlan);
        //20260330 分组计划标记分配完成，需要验证是否需要进行分组计划分配延长处理
        if (!isTimeExtensionHandlerFlag(isForcedTimeExtension, originStartDay, originEndDay, productionPlan)) {
            return;
        }
        groupTimeExtensionHandler.handlerTimeExtension(this, context, isIgnoreHighPriority, productionPlan, handledDayInfo);
    }

    /**
     * 对因每日结构切换限制或是其它原因导致的间断处理
     * 将前分组的收尾延长到下一分配的起始日前一天
     * 场景：后分组衔接时，因每日切换次数限制，后分组需往后起始排产
     * 则前结构自动延长
     *
     * @param context   排产上下文
     * @param addHelper 当前分组分配信息
     */
    public void handlerTimeExtensionDayConclusionByBeforeGroup(Context context, CxMachineAllocationPlanHelper addHelper) {
        if (null == addHelper) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String cxMachineCode = addHelper.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        if (!cxMachineInfo.hasAllocation(addHelper)) {
            return;
        }
        groupTimeExtensionHandler.handlerTimeExtensionDayConclusion(context, addHelper.getBeforeAllocation());
    }

    /**
     * 重新构建成型对应的收尾信息，从分配的起始天数开始
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台
     * @param ratio          配比信息
     * @param productionPlan 排产计划
     */
    private void buildNewLhConclusionInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo ratio, CxMachineAllocationPlanHelper productionPlan) {
        //构建硫化组信息--因为时间段更新
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = buildCxLhGroupInfo(context, cxMachineInfo, ratio, productionPlan);
        cxMachineInfo.setCxLhRatioMap(cxLhRatioMap);
        //按日构建限制
        buildDayLimitInfo(context, cxMachineInfo, productionPlan);
        //20260523+ 备份当前分组计划各自的当前待排产量
        simulateProductionSnapshotHandler.saveProductionBeforeSnapshotData(context, productionPlan);
    }

    /**
     * 构建成型机台在productionPlan分配段的硫化组信息
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台信息
     * @param ratio          硫化配比信息
     * @param productionPlan 分配信息
     */
    private Map<Integer, CxLhProductionHelper> buildCxLhGroupInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo ratio, CxMachineAllocationPlanHelper productionPlan) {
        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
        //起始日
        Integer startDay = productionPlan.getStartDay();
        //最大硫化组
        Integer maxLhCount = ratio.getLhMachineMaxQty();
        //切换结构首日需要扣减的硫化机台数
        TbrProductionContext productionContext = ((TbrProductionContext) context);
        Integer deductionLhMachineCount = productionContext.getBaseDataContainer().getParamConfiguration().getDeductionLhMachineCount();
        //重新构建硫化组信息--因为时间段更新
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = new HashMap<>(maxLhCount);
        Integer newStartDay;
        //若非（1号且续作结构）
        boolean noContinueStruct = (!(startDay.equals(FactoryConstant.MONTH_START_DAY) &&
                productionPlanInfo.getGroupName().equals(productionContext.getContinueStructureMap().get(cxMachineInfo.getCxMachineCode()))));
        for (Integer cxLhGroupNo = BigDecimal.ONE.intValue(); cxLhGroupNo <= maxLhCount; cxLhGroupNo++) {
            newStartDay = startDay;
            if (noContinueStruct && cxLhGroupNo <= deductionLhMachineCount) {
                //模拟排产，成型机只会有1台；将小于扣减的硫化机台数的起始日+1，即首日不上机; sandy+ 2026.3.19
                newStartDay = startDay + 1;
            }
            updateProductionInfo(cxLhRatioMap, cxLhGroupNo, productionPlanInfo.getGroupName(), cxMachineInfo.getCxMachineCode(), newStartDay);
        }
        return cxLhRatioMap;
    }

    /**
     * 构建日排产限制信息
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台
     * @param productionPlan 排产分配信息
     */
    private void buildDayLimitInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper productionPlan) {
        Integer startDay = productionPlan.getStartDay();
        //按日构建限制
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> newLimit = new HashMap<>();
        for (Integer day = startDay; day <= productionPlan.getEndDay(); day++) {
            //停产日跳过
            if (cxMachineInfo.getStopDayInfo().contains(day)) {
                continue;
            }
            GroupPlanCxLhCapacityLimitHelper dayLimit = GroupPlanCxLhCapacityLimitHelper.buildByCxMachineAllocation(context, cxMachineInfo, day, productionPlan);
            newLimit.put(day, dayLimit);
        }
        cxMachineInfo.setDayProductionLimitInfo(newLimit);
    }

    /**
     * 根据硫化组编号，更新最新的硫化组信息，如果一开始没有则表示新增
     *
     * @param cxLhRatioMap
     * @param cxLhGroupNo
     * @param groupName
     * @param cxMachineCode
     * @param startDay
     */
    private void updateProductionInfo(Map<Integer, CxLhProductionHelper> cxLhRatioMap, Integer cxLhGroupNo, String groupName, String cxMachineCode, Integer startDay) {
        if (cxLhRatioMap.containsKey(cxLhGroupNo)) {
            CxLhProductionHelper helper = cxLhRatioMap.get(cxLhGroupNo);
            helper.resetProductionInfoByNewGroupName(groupName, startDay);
            return;
        }
        Set<String> cxMachineInfo = new HashSet<>();
        cxMachineInfo.add(cxMachineCode);
        CxLhProductionHelper newHelper = CxLhProductionHelper.createEmptyLhGroup(groupName, cxLhGroupNo, cxMachineInfo);
        newHelper.setProductionDay(startDay);
        BeforeSkuProductionInfo beforeSku = BeforeSkuProductionInfo.buildEmpty(startDay);
        newHelper.setBeforeSku(beforeSku);
        cxLhRatioMap.put(cxLhGroupNo, newHelper);
    }

    /**
     * 是否需要进行延长探测处理
     * 1、强制延长探测则结果为true
     * 2、否则如果排产时间范围一直，则不再延长探测
     *
     * @param isForcedTimeExtension 是否需要强制延长探测
     * @param originStartDay        初始排产开始日
     * @param originEndDay          初始排产结束日
     * @param productionPlan        新排产范围
     * @return
     */
    private boolean isTimeExtensionHandlerFlag(boolean isForcedTimeExtension,
                                               Integer originStartDay,
                                               Integer originEndDay,
                                               CxMachineAllocationPlanHelper productionPlan) {
        if (null == originStartDay || null == originEndDay || null == productionPlan) {
            return true;
        }
        if (isForcedTimeExtension) {
            return true;
        }
        return !(originStartDay.equals(productionPlan.getStartDay()) && originEndDay.equals(productionPlan.getEndDay()));
    }

}
