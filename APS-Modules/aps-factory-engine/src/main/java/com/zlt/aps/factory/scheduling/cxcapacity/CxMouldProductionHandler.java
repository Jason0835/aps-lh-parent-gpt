package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.*;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型模具排产业务处理
 *
 * @author ZLT
 * @date 20251217
 */
@Slf4j
public class CxMouldProductionHandler {

    /**
     * 在机结构，进行模具排产--按机台
     * 1、先排续作规格
     * 1.1、续作SKU
     * 1.2、同规格同花纹
     * 1.3、换活字块-共生胎同模具
     * 2、收尾新增规格
     *
     * @param context        排产上下文
     * @param cxMachineCode  成型机台
     * @param productionPlan 排产计划信息
     * @param mouldInfoMap   模具关系信息
     * @param mouldShellMap  模壳信息
     */
    @Deprecated
    public static void continueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan, CxContinueInfoHelper cxContinueInfo, Map<String, List<MonthPlanProductMouldInfoVo>> mouldInfoMap, Map<String, MouldShellBaseInfoVo> mouldShellMap) {
        List<MonthPlanProductionRequirePlanVo> groupPlanData = productionPlan.getProductionPlanInfo().getGroupPlanData();
        if (CollectionUtils.isEmpty(groupPlanData)) {
            //todo 记录日志
            return;
        }
        List<MonthPlanProductionRequirePlanVo> hasProductionPlanList = groupPlanData.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasProductionPlanList)) {
            //todo 记录日志
            return;
        }
        //先续作排产： 续作SKU ->同规格同花纹 -> 换活字块共生胎同模具 cxContinueInfo.getCxMachineGroup().get(cxMachineCode);
        Map<String, CxContinueSkuInfoHelper> continueSkuMap = cxContinueInfo.getContinueSkuMouldNumberMap();
        if (!CollectionUtils.isEmpty(continueSkuMap)) {
            CxContinueSkuProductionHandler.productionContinue(context, cxMachineCode, hasProductionPlanList, productionPlan, mouldInfoMap, mouldShellMap);
        }
        //排产收尾新增规格-重新获取需求需排产计划信息
        List<MonthPlanProductionRequirePlanVo> leftOverHasProductionList = hasProductionPlanList.stream().filter(groupPlan -> groupPlan.hasProduction()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverHasProductionList)) {
            //todo 记录日志
            return;
        }
        CxAddSkuProductionHandler.productionAddSku(context, cxMachineCode, leftOverHasProductionList, productionPlan, mouldShellMap);
    }

    /**
     * 非在机结构，模具排产
     *
     * @param context
     * @param cxMachineCode
     * @param productionPlan
     */
    public static void noContinueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan) {
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
        CxAddSkuProductionHandler.productionAddSku(context, cxMachineCode, hasProductionPlanList, productionPlan, productionContext.getBaseDataContainer().getMouldShellMap());
        //处理结构提前收尾
        GroupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, productionPlanInfo, cxMachineInfo, cxLhRatio);
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
        ProductionPlanGroupInfo productionPlanInfo = productionPlan.getProductionPlanInfo();
        Integer startDay = productionPlan.getStartDay();
        Integer maxLhCount = ratio.getLhMachineMaxQty();
        Set<Integer> newCxLhGroupNo = new HashSet<>();
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = cxMachineInfo.getCxLhRatioMap();
        for (Integer cxLhGroupNo = BigDecimal.ONE.intValue(); cxLhGroupNo <= maxLhCount; cxLhGroupNo++) {
            newCxLhGroupNo.add(cxLhGroupNo);
            updateProductionInfo(cxLhRatioMap, cxLhGroupNo, productionPlanInfo.getGroupName(), cxMachineInfo.getCxMachineCode(), startDay);
        }
        Set<Integer> needDeletedGroupNo = new HashSet<>();
        cxLhRatioMap.forEach((cxLhGroupNo, helper) -> {
            if (!newCxLhGroupNo.contains(cxLhGroupNo)) {
                needDeletedGroupNo.add(cxLhGroupNo);
            }
        });
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
        if (CollectionUtils.isEmpty(needDeletedGroupNo)) {
            return;
        }
        needDeletedGroupNo.forEach(deletedGroupNo -> cxLhRatioMap.remove(deletedGroupNo));
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
        newHelper.setProductionQty(BigDecimal.ZERO.intValue());
        cxLhRatioMap.put(cxLhGroupNo, newHelper);
    }

}
