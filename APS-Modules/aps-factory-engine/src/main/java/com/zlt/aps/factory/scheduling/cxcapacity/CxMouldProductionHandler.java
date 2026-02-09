package com.zlt.aps.factory.scheduling.cxcapacity;

import com.zlt.aps.factory.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxLhProductionHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.logrecorder.TbrMouldProductionLogRecorder;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
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

    private final GroupPlanBeforeConclusionHandler groupPlanBeforeConclusionHandler;

    /**
     * 非在机结构，模具排产
     *
     * @param context
     * @param cxMachineCode
     * @param productionPlan
     */
    public void noContinueGroupPlanMouldProduction(Context context, String cxMachineCode, CxMachineAllocationPlanHelper productionPlan) {
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
        cxAddSkuProductionHandler.productionAddSku(context, cxMachineCode, hasProductionPlanList, productionPlan, productionContext.getBaseDataContainer().getMouldShellMap());
        //处理结构提前收尾
        groupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, productionPlanInfo, cxMachineInfo, cxLhRatio);
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
