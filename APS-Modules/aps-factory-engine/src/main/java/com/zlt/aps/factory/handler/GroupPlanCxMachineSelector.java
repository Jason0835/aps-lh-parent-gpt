package com.zlt.aps.factory.handler;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.scheduling.cxcapacity.TbrProductionGroupLogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分组计划-成型机台选择器
 *
 * @author ZLT
 * @date 20260106
 */
@Slf4j
public class GroupPlanCxMachineSelector {

    /**
     * 根据分组计划，判断当前成型机台是否符合匹配条件
     * 符合，则将cxMachineInfo放入enableCxMachineList集合中
     * 同时设置此时的成型硫化配比
     * 并记录相应的匹配日志详情
     * 1、零度供料架的匹配
     * 2、是否限制作业(限制结构，限制规格)
     * 3、是否存在对应的硫化配比
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组计划
     * @param cxMachineInfo 成型机台
     */
    public static boolean isSelectMatchCxMachineForGroupPlan(Context context, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo) {
        //分组信息：TBR 结构名、是否要求零度供料架、排产计划集合
        String structureName = groupPlanInfo.getGroupName();
        String isZeroRack = groupPlanInfo.getIsZero();
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        //成型机台信息
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String machineIsZeroRack = cxMachineInfo.getIsZeroRack();
        if (!isZeroRack.equals(machineIsZeroRack)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedZeroMatchLog(context, structureName, isZeroRack, cxMachineCode, machineIsZeroRack));
            return false;
        }
        if (cxMachineInfo.isNoProductionStructure(structureName)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedLimitLog(context, structureName, isZeroRack, cxMachineCode));
            return false;
        }
        if (CollectionUtils.isEmpty(groupPlanData)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedGroupNoProductionLog(context, structureName, isZeroRack, cxMachineCode));
            return false;
        }
        Set<String> materialCodeSet = groupPlanData.stream().map(MonthPlanProductionRequirePlanVo::getMaterialCode).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(materialCodeSet)) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedGroupMaterialDescExceptionLog(context, structureName, isZeroRack, cxMachineCode));
            return false;
        }
        boolean isProduction = true;
        for (String materialCode : materialCodeSet) {
            if (cxMachineInfo.isNoProductionMaterial(materialCode)) {
                isProduction = false;
                break;
            }
        }
        if (!isProduction) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedLimitLog(context, structureName, isZeroRack, cxMachineCode));
            return false;
        }
        String brandCode = cxMachineInfo.getCxMachineBrandCode();
        MonthPlanStructureLhRatioVo lhRatioInfo = groupPlanInfo.getLhRatio(cxMachineInfo);
        if (null == lhRatioInfo) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedNoRatioLog(context, structureName, isZeroRack, cxMachineCode, brandCode));
            return false;
        }
        cxMachineInfo.setRatio(lhRatioInfo.getLhMachineMaxQty());
        log.info(TbrProductionGroupLogRecorder.addGroupSelectedCxMachineCodeLog(context, structureName, isZeroRack, cxMachineCode, brandCode));
        return true;
    }
}
