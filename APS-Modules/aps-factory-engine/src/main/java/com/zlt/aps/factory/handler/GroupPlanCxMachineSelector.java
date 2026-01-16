package com.zlt.aps.factory.handler;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.factory.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.factory.scheduling.cxcapacity.TbrProductionGroupLogRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
     * 以分组计划为轴心，选择满足初步条件的成型机台集合
     * 1、零度供料架的匹配
     * 2、是否限制作业(限制结构，限制规格)
     * 3、是否存在对应的硫化配比
     * 4、是否指定机台
     *
     * @param context         排产上下文
     * @param addNewGroupPlan 分组计划
     * @return
     */
    public static List<CxMachineBaseInfoVo> getEnableBaseCxMachineList(Context context, ProductionPlanGroupInfo addNewGroupPlan) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String structureName = addNewGroupPlan.getGroupName();
        //获取所有机台信息
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineMap)) {
            return Collections.emptyList();
        }
        //获取有剩余产能的机台信息
        List<CxMachineBaseInfoVo> cxMachineList = new ArrayList<>(allCxMachineMap.values());
        List<CxMachineBaseInfoVo> leftOverCxMachineList = cxMachineList.stream().filter(cxMachine -> cxMachine.getRemainingDays() > BigDecimal.ZERO.intValue()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leftOverCxMachineList)) {
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedLeftOverCapacityLog(context, structureName));
            return Collections.emptyList();
        }
        //成型机台基础条件匹配
        List<CxMachineBaseInfoVo> enableCxMachineList = new ArrayList<>(leftOverCxMachineList.size());
        leftOverCxMachineList.forEach(cxMachineInfo -> {
            //零度匹配，不可作业剔除
            boolean isSelected = isMatch(context, addNewGroupPlan, cxMachineInfo);
            if (isSelected) {
                enableCxMachineList.add(cxMachineInfo);
            }
        });
        if (CollectionUtils.isEmpty(enableCxMachineList)) {
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, structureName));
            return Collections.emptyList();
        }
        return enableCxMachineList;
//        //如果结构有固定机台，则只能从固定中选择
//        Set<String> fixedCxMachineSet = addNewGroupPlan.getFixedCxMachineSet();
//        if (CollectionUtils.isEmpty(fixedCxMachineSet)) {
//            return enableCxMachineList;
//        }
//        String fixedMachineInfo = String.join(StringConstant.COMMA, fixedCxMachineSet);
//        log.info(TbrProductionGroupLogRecorder.addGroupSelectedFixedCxMachineLog(context, structureName, fixedMachineInfo));
//        List<CxMachineBaseInfoVo> finalResult = enableCxMachineList.stream().filter(baseSelectedMachine -> fixedCxMachineSet.contains(baseSelectedMachine.getCxMachineCode())).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(finalResult)) {
//            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedForFixedCxMachineLog(context, structureName, fixedMachineInfo));
//        }
//        return finalResult;
    }

    /**
     * 判断分组计划、成型机台是否符合匹配条件
     * true 符合
     * false 不符合
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
    public static boolean isMatch(Context context, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo) {
        //分组信息：TBR 结构名、是否要求零度供料架、排产计划集合
        String structureName = groupPlanInfo.getGroupName();
        String isZeroRack = groupPlanInfo.getIsZero();
        List<MonthPlanProductionRequirePlanVo> groupPlanData = groupPlanInfo.getGroupPlanData();
        //成型机台信息
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        String machineIsZeroRack = cxMachineInfo.getIsZeroRack();
        if (YesOrNoEnum.YES.getCode().equals(isZeroRack) && !isZeroRack.equals(machineIsZeroRack)) {
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
        String machineTypeCode = cxMachineInfo.getCxMachineTypeCode();
        MonthPlanStructureLhRatioVo lhRatioInfo = groupPlanInfo.getLhRatio(cxMachineInfo);
        if (null == lhRatioInfo) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedNoRatioLog(context, structureName, isZeroRack, cxMachineCode, machineTypeCode));
            return false;
        }
        cxMachineInfo.setRatio(lhRatioInfo.getLhMachineMaxQty());
        log.info(TbrProductionGroupLogRecorder.addGroupSelectedCxMachineCodeLog(context, structureName, isZeroRack, cxMachineCode, machineTypeCode));
        return true;
    }
}
