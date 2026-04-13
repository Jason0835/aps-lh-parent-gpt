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

    private final CxAddSkuProductionHandler cxAddSkuProductionHandler;
    /**
     * 结构分配延长处理器
     */
    private final GroupTimeExtensionHandler groupTimeExtensionHandler;

    private final GroupPlanBeforeConclusionHandler groupPlanBeforeConclusionHandler;

    /**
     * 非在机结构，模具排产
     *
     * @param context        排产上下文
     * @param cxMachineCode  成型机台
     * @param productionPlan 分配段
     * @param handledDayInfo 已延长处理日期
     */
    public void noContinueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan, Set<String> handledDayInfo) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
        String groupName = productionPlanInfo.getGroupName();
        log.info(TbrMouldProductionLogRecorder.addStartCxMachineMouldProductionPlanLog(context, cxMachineCode, groupName));
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlanInfo.getGroupPlanData();
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addGroupCxMachineMouldNoPlanLog(context, groupName, cxMachineCode));
            return;
        }
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addGroupCxMachineMouldNoFindMachineInfoLog(context, groupName, cxMachineCode));
            return;
        }
        //根据新的分组计划，构建新的硫化配比
        Map<String, MonthPlanStructureLhRatioVo> cxMachineLhRationMap = productionPlanInfo.getCxMachineLhRationMap();
        if (CollectionUtils.isEmpty(cxMachineLhRationMap)) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addGroupCxMachineMouldGroupNoRatioLog(context, groupName, cxMachineCode));
            return;
        }
        String machineTypeCode = cxMachineInfo.getCxMachineTypeCode();
        MonthPlanStructureLhRatioVo cxLhRatio = productionPlanInfo.getLhRatio(cxMachineInfo);
        if (null == cxLhRatio) {
            //记录日志
            log.info(TbrMouldProductionLogRecorder.addGroupCxMachineMouldGroupNoBrandRatioLog(context, groupName, cxMachineCode, machineTypeCode));
            return;
        }
        cxMachineInfo.setRatio(cxLhRatio.getLhMachineMaxQty());
        buildNewLhConclusionInfo(context, cxMachineInfo, cxLhRatio, productionPlan);
        //20260108 开启本轮可排产
        productionPlanInfo.setThisRoundCanProduction();
        cxAddSkuProductionHandler.productionAddSku(context, cxMachineCode, hasProductionPlanList, productionPlan, productionContext.getBaseDataContainer().getMouldShellMap(), new HashSet<>());
        //处理结构提前收尾
        groupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, productionPlanInfo, cxMachineInfo, cxLhRatio, productionPlan);
        //20260330 分组计划标记分配完成，需要验证是否需要进行分组计划分配延长处理
        groupTimeExtensionHandler.handlerTimeExtension(this, context, productionPlan, handledDayInfo);
    }

    /**
     * 重新构建成型对应的收尾信息，从分配的起始天数开始
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台
     * @param ratio          配比信息
     * @param productionPlan 排产计划
     */
    private static void buildNewLhConclusionInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo ratio, CxMachineAllocationPlanHelper productionPlan) {
        //构建硫化组信息--因为时间段更新
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = buildCxLhGroupInfo(context, cxMachineInfo, ratio, productionPlan);

//        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
//        Integer maxLhCount = ratio.getLhMachineMaxQty();
//        Set<Integer> newCxLhGroupNo = new HashSet<>();
//        //切换结构首日需要扣减的硫化机台数
//        TbrProductionContext productionContext = ((TbrProductionContext) context);
//        Integer deductionLhMachineCount = productionContext.getBaseDataContainer().getParamConfiguration().getDeductionLhMachineCount();
//        //重新构建过--因为时间段更新
//        Map<Integer, CxLhProductionHelper> cxLhRatioMap = new HashMap<>(maxLhCount);
//        Integer newStartDay = 0;
//        //若非（1号且续作结构）
//        boolean noContinueStruct = (!(startDay.equals(FactoryConstant.MONTH_START_DAY) &&
//                productionPlanInfo.getGroupName().equals(productionContext.getContinueStructureMap().get(cxMachineInfo.getCxMachineCode()))));
//        for (Integer cxLhGroupNo = BigDecimal.ONE.intValue(); cxLhGroupNo <= maxLhCount; cxLhGroupNo++) {
//            newStartDay = startDay;
//            if (noContinueStruct && cxLhGroupNo <= deductionLhMachineCount) {
//                //模拟排产，成型机只会有1台；将小于扣减的硫化机台数的起始日+1，即首日不上机; sandy+ 2026.3.19
//                newStartDay = startDay + 1;
//            }
//            newCxLhGroupNo.add(cxLhGroupNo);
//            updateProductionInfo(cxLhRatioMap, cxLhGroupNo, productionPlanInfo.getGroupName(), cxMachineInfo.getCxMachineCode(), newStartDay);
//        }
        cxMachineInfo.setCxLhRatioMap(cxLhRatioMap);
        //按日构建限制
        buildDayLimitInfo(context, cxMachineInfo, productionPlan);
//        Set<Integer> needDeletedGroupNo = new HashSet<>();
//        cxLhRatioMap.forEach((cxLhGroupNo, helper) -> {
//            if (!newCxLhGroupNo.contains(cxLhGroupNo)) {
//                needDeletedGroupNo.add(cxLhGroupNo);
//            }
//        });
//        if (CollectionUtils.isEmpty(needDeletedGroupNo)) {
//            return;
//        }
//        needDeletedGroupNo.forEach(deletedGroupNo -> cxLhRatioMap.remove(deletedGroupNo));
    }

    /**
     * 构建成型机台在productionPlan分配段的硫化组信息
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台信息
     * @param ratio          硫化配比信息
     * @param productionPlan 分配信息
     */
    private static Map<Integer, CxLhProductionHelper> buildCxLhGroupInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo ratio, CxMachineAllocationPlanHelper productionPlan) {
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
//        cxMachineInfo.setCxLhRatioMap(cxLhRatioMap);
    }

    /**
     * 构建日排产限制信息
     *
     * @param context        排产上下文
     * @param cxMachineInfo  成型机台
     * @param productionPlan 排产分配信息
     */
    private static void buildDayLimitInfo(Context context, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper productionPlan) {
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
    private static void updateProductionInfo(Map<Integer, CxLhProductionHelper> cxLhRatioMap, Integer cxLhGroupNo, String groupName, String cxMachineCode, Integer startDay) {
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

}
