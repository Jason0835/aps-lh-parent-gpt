package com.zlt.aps.mp.engine.handler.appoint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.handler.appoint.GroupAppointBusinessHandler;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 在机分组(结构)进行模拟排产后，如果与特定指定机台排产范围有冲突
 * 则需进行调整，重新进行模拟排产
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260715
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContinueGroupAdjustHandler {

    private final GroupAppointBusinessHandler groupAppointHandler;

    /**
     * 业务场景：续作在机分组(结构)进行模拟排产后，
     * 如果在机机台分配到的排产范围与特定指定的机台后结构有冲突，
     * 则对应的在机结构需要调整重排
     *
     * @param context                排产上下文
     * @param allGroupPlanMap        所有分组(结构)信息对象集合
     * @param continueAllocationList 在机结构已分配信息
     * @param cxContinueInfoMap      在机结构续作信息
     */
    public void continueGroupAdjustByAppoint(Context context, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> cxContinueInfoMap) {
        if (CollectionUtils.isEmpty(allGroupPlanMap) || CollectionUtils.isEmpty(continueAllocationList)) {
            return;
        }
        //检测在机结构在产机台是否需要进行调整
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<ProductionPlanGroupInfo, CxMachineAllocationPlanHelper> singleMachineGroupAdjustMap = Maps.newHashMap();
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> moreMachineGroupAdjustMap = Maps.newHashMap();
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupOnLineMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        continueAllocationList.forEach(continueAllocation -> addGroupAdjustInfo(productionContext, groupOnLineMap, continueAllocation, singleMachineGroupAdjustMap, moreMachineGroupAdjustMap));
        //需要调整后，则进行机台分配调整
        if (CollectionUtils.isEmpty(singleMachineGroupAdjustMap) && CollectionUtils.isEmpty(moreMachineGroupAdjustMap)) {
            return;
        }
        //对重新调整的在机结构，重新模拟排产


    }

    /**
     * 对需要进行调整下机日的单机台在机结构进行调整其下机日
     * 并重新进行模拟排产
     *
     * @param productionContext
     * @param singleMachineGroupAdjustInfo
     */
    private void adjustSingleCxMachineGroupByAppoint(TbrProductionContext productionContext, Map<ProductionPlanGroupInfo, CxMachineAllocationPlanHelper> singleMachineGroupAdjustInfo, List<CxMachineAllocationPlanHelper> continueAllocationList) {
        if (CollectionUtils.isEmpty(singleMachineGroupAdjustInfo)) {
            return;
        }
        singleMachineGroupAdjustInfo.forEach((groupInfo, onLineAllocation) -> adjustOnLineGroupOffDayByAppoint(productionContext, groupInfo, onLineAllocation, continueAllocationList));
    }

    /**
     * 加入在机结构是否需要进行调整(因成型指定排产时间范围导致的调整)
     * 1、如果在机结构只有一台在机且机台有指定，则强制下机，需要调整。
     * 加入到singleMachineGroupAdjustMap集合中
     * 2、如果在机结构有多台，则指定机台强制下机，需要调整
     * 加入到moreMachineGroupAdjustMap集合中
     * 同时，此时需要考虑续作Sku的下机先后顺序以达到减机台后的胎胚种类数和硫化机台数
     *
     * @param productionContext           排产上下文
     * @param groupOnLineMap              在机结构机台分配信息
     * @param onLineGroupAllocationInfo   当前在机结构分配机台信息
     * @param singleMachineGroupAdjustMap 在机结构单机台需调整信息
     * @param moreMachineGroupAdjustMap   在机结构多机台需调整信息
     */
    private void addGroupAdjustInfo(TbrProductionContext productionContext,
                                    Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupOnLineMap,
                                    CxMachineAllocationPlanHelper onLineGroupAllocationInfo,
                                    Map<ProductionPlanGroupInfo, CxMachineAllocationPlanHelper> singleMachineGroupAdjustMap,
                                    Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> moreMachineGroupAdjustMap) {
        if (null == onLineGroupAllocationInfo || null == singleMachineGroupAdjustMap || null == moreMachineGroupAdjustMap) {
            return;
        }
        String cxMachineCode = onLineGroupAllocationInfo.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        Integer theoryOffDay = onLineGroupAllocationInfo.getEndDay();
        //是否有指定排产范围限制
        boolean hasAdjust = groupAppointHandler.hasAdvanceOffByContinueCxMachine(productionContext, cxMachineInfo, theoryOffDay);
        if (!hasAdjust) {
            return;
        }
        //有指定
        ProductionPlanGroupInfo groupInfo = onLineGroupAllocationInfo.getProductionPlanInfo();
        List<CxMachineAllocationPlanHelper> onLineList = groupOnLineMap.get(groupInfo);
        if (CollectionUtils.isEmpty(onLineList)) {
            return;
        }
        if (onLineList.size() == BigDecimal.ONE.intValue()) {
            singleMachineGroupAdjustMap.put(groupInfo, onLineGroupAllocationInfo);
            return;
        }
        List<CxMachineAllocationPlanHelper> canAdjustList = moreMachineGroupAdjustMap.get(groupInfo);
        if (null == canAdjustList) {
            canAdjustList = Lists.newArrayList();
            moreMachineGroupAdjustMap.put(groupInfo, canAdjustList);
        }
        canAdjustList.add(onLineGroupAllocationInfo);
    }

    /**
     * 单机台在机结构，
     * 因机台指定结构上机排产范围导致的在机结构需提前下机的调整
     * 先确定新下机日，在根据新收尾日重新进行模拟排产
     *
     * @param productionContext      排产上下文
     * @param groupInfo              分组信息对象--结构
     * @param onLineAllocation       在机机台分配情况
     * @param continueAllocationList 在机结构在产机台的分配情况
     */
    private void adjustOnLineGroupOffDayByAppoint(TbrProductionContext productionContext, ProductionPlanGroupInfo groupInfo, CxMachineAllocationPlanHelper onLineAllocation, List<CxMachineAllocationPlanHelper> continueAllocationList) {
        if (null == groupInfo || null == onLineAllocation) {
            return;
        }
        String cxMachineCode = onLineAllocation.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineInfoByCode(cxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        Integer theoryOffDay = onLineAllocation.getEndDay();
        //是否有指定排产范围限制
        boolean hasAdjust = groupAppointHandler.hasAdvanceOffByContinueCxMachine(productionContext, cxMachineInfo, theoryOffDay);
        if (!hasAdjust) {
            return;
        }
        Integer newOffDay = groupAppointHandler.getContinueCxMachineEndDayByAppoint(productionContext, cxMachineInfo);
        if (null == newOffDay) {
            //表示月初一开始就下机

            return;
        }
        //先清除
    }

}
