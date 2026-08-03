package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxContinueSkuInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.domain.vo.ProductionMouldInfoVo;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 周期结构特殊业务处理器
 * TBR 为结构
 * PCR 为寸口
 *
 * @author ZLT
 * @date 20260731
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CycleGroupProductionHandler {
    /**
     * 对不在月周期清单中的周期结构因结构切换限制导致自动延长1天的结构
     * 需要判断是否满足最低实单，
     * 若不满足最低实单，则不能自动延长，反而是后上机结构提前一天上机
     *
     * @param context        排产上下文
     * @param allContinueMap 续作信息
     */
    public void handlerNoMonthRangeCycleGroupByTimeExtension(Context context, Map<String, CxContinueInfoHelper> allContinueMap) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        //获取只分配1天的在机周期结构且不在月周期排产清单中的周期结构
        List<CxMachineBaseInfoVo> onlyOneDayCycleGroupList = findCycleGroupTimeExtensionByNoMonthRange(productionContext);
        if (CollectionUtils.isEmpty(onlyOneDayCycleGroupList)) {
            return;
        }
        onlyOneDayCycleGroupList.forEach(cxMachineInfo -> {
            handlerOneDayByCycleGroup(productionContext, cxMachineInfo, allContinueMap);
        });
    }

    /**
     * 获取上月在机周期分组(结构)不在本月周期分组(结构)清单中
     * 因日结构切换限制导致其延长的非本月周期结构在机结构延长排产
     *
     * @param productionContext 排产上下文
     * @return
     */
    private List<CxMachineBaseInfoVo> findCycleGroupTimeExtensionByNoMonthRange(TbrProductionContext productionContext) {
        Integer cycleStartDay = productionContext.getCycleFirstProductionDay();
        if (null == cycleStartDay) {
            return Collections.emptyList();
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineMap = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineMap)) {
            return Collections.emptyList();
        }
        List<CxMachineBaseInfoVo> findResult = Lists.newArrayList();
        allCxMachineMap.forEach((cxMachineCode, cxMachineInfo) -> {
            List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            //上机日从小到大
            allocationList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getStartDay));
            CxMachineAllocationPlanHelper firstProductionGroup = allocationList.get(BigDecimal.ZERO.intValue());
            if (!isCycleGroupTimeExtensionByNoMonthRange(productionContext, firstProductionGroup)) {
                return;
            }
            findResult.add(cxMachineInfo);
        });
        if (CollectionUtils.isEmpty(findResult)) {
            return Collections.emptyList();
        }
        return findResult;
    }

    /**
     * 处理在机周期结构只分配到1天且为不在月周期结构清单中的周期排产信息(因日结构切换限制导致的续作延长)
     * 1、需要判断是续作延长(满足最低实单)
     * 2、续作不可延长，后结构提前
     *
     * @param productionContext 排产上下文
     * @param cxMachineInfo     成型机台
     * @param allContinueMap    续作信息
     */
    private void handlerOneDayByCycleGroup(TbrProductionContext productionContext, CxMachineBaseInfoVo cxMachineInfo, Map<String, CxContinueInfoHelper> allContinueMap) {
        if (null == cxMachineInfo) {
            return;
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList) || allocationList.size() <= BigDecimal.ONE.intValue()) {
            return;
        }
        //上机日从小到大
        allocationList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getStartDay));
        CxMachineAllocationPlanHelper continueGroupInfo = allocationList.get(BigDecimal.ZERO.intValue());
        CxMachineAllocationPlanHelper afterGroupInfo = allocationList.get(BigDecimal.ONE.intValue());
        String groupName = continueGroupInfo.getAllocationGroup();
        CxContinueInfoHelper continueSkuInfo = allContinueMap.get(groupName);
        boolean isReachMinLhMachine = isReachMinLhMachineByContinueSku(productionContext, continueGroupInfo.getProductionPlanInfo(), continueGroupInfo.getMinRatio(), continueSkuInfo);
        if (isReachMinLhMachine) {
            return;
        }
        //后结构提前
        allocationList.remove(continueGroupInfo);
        addAfterGroupAllocationDay(productionContext, afterGroupInfo, continueGroupInfo.getStartDay(), continueGroupInfo.getAllocationDay());
    }

    /**
     * 判断：true表示是 false 表示不是
     * 1、是否为在机周期结构
     * 2、因不在月周期排产内，且因后结构切换日限制导致的前结构延长
     * 3、且延长1日
     *
     * @param productionContext 排产上下文
     * @param allocationInfo    最早排产信息
     * @return
     */
    private boolean isCycleGroupTimeExtensionByNoMonthRange(TbrProductionContext productionContext, CxMachineAllocationPlanHelper allocationInfo) {
        if (null == allocationInfo) {
            return false;
        }
        ProductionPlanGroupInfo groupInfo = allocationInfo.getProductionPlanInfo();
        if (null == groupInfo) {
            return false;
        }
        if (!groupInfo.isCycleType()) {
            //不是周期结构
            return false;
        }
        String cxMachineCode = allocationInfo.getCxMachineCode();
        String groupName = groupInfo.getGroupName();
        String continueGroup = productionContext.getContinueStructureMap().get(cxMachineCode);
        if (!groupName.equals(continueGroup)) {
            return false;
        }
        Integer cycleStartDay = productionContext.getCycleFirstProductionDay();
        if (allocationInfo.getAllocationDay() > BigDecimal.ONE.intValue() || !cycleStartDay.equals(allocationInfo.getStartDay())) {
            //排产天数超过1天或是起始排产日不为周期第一天
            return false;
        }
        Set<String> monthProductionCycleSet = productionContext.getBaseDataContainer().getMonthProductionCycleList();
        if (monthProductionCycleSet.contains(groupName)) {
            //在月周期排产分组清单中
            return false;
        }
        return true;
    }

    /**
     * 续作计划能否达到最低实单量，可则不用调整
     * 否则续作结构收尾，后结构提前
     * 就1天的排产量
     *
     * @param productionContext 排产上下文
     * @param groupPlanInfo     分析计划信息对象
     * @param minLhMachine      最低实单
     * @param continueSkuInfo   续作计划信息
     */
    private boolean isReachMinLhMachineByContinueSku(TbrProductionContext productionContext, ProductionPlanGroupInfo groupPlanInfo, Integer minLhMachine, CxContinueInfoHelper continueSkuInfo) {
        if (null == minLhMachine || minLhMachine <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        if (null == continueSkuInfo) {
            return false;
        }
        Map<String, CxContinueSkuInfoHelper> continueSkuInfoMap = continueSkuInfo.getContinueSkuMouldNumberMap();
        if (CollectionUtils.isEmpty(continueSkuInfoMap)) {
            return false;
        }
        List<Integer> productionLhMachineList = Lists.newArrayList();
        continueSkuInfoMap.forEach((materialDesc, cxContinueSkuInfo) -> {
            Integer planDemandQty = CycleGroupCalculateHandler.getContinueSkuPlannedQty(productionContext, cxContinueSkuInfo, groupPlanInfo);
            if (null == planDemandQty || planDemandQty <= BigDecimal.ZERO.intValue()) {
                return;
            }
            List<MonthPlanProductionRequirePlanVo> planList = productionContext.getAllSkuProductionPlan().get(materialDesc);
            if(CollectionUtils.isEmpty(planList)){
                return;
            }
            Integer maxDayQty = planList.get(BigDecimal.ZERO.intValue()).getMaxDaySingleLhMachineQty();
            Integer theoryMaxMouldNumber = cxContinueSkuInfo.getMouldNumber();
            List<ProductionMouldInfoVo> selectMouldList = SkuMouldSelector.getContinueSkuMouldNumberInit(productionContext, ProductionStageEnum.SIMULATE_STAGE, materialDesc, theoryMaxMouldNumber);
            if (CollectionUtils.isEmpty(selectMouldList)) {
                return;
            }
            theoryMaxMouldNumber = selectMouldList.size();
            //向上取整
            int usedLhMachine = BigDecimal.valueOf(planDemandQty).divide(BigDecimal.valueOf(maxDayQty), BigDecimal.ZERO.intValue(), RoundingMode.UP).intValue();
            int maxLhMachine = theoryMaxMouldNumber / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
            if (usedLhMachine < maxLhMachine) {
                productionLhMachineList.add(usedLhMachine);
                return;
            }
            productionLhMachineList.add(maxLhMachine);
        });
        if (CollectionUtils.isEmpty(productionLhMachineList)) {
            return false;
        }
        Integer sumUsedLhMachine = productionLhMachineList.stream().mapToInt(Integer::intValue).sum();
        return sumUsedLhMachine >= minLhMachine;
    }

    /**
     * 后结构自动延长
     *
     * @param productionContext 排产上下文
     * @param afterGroup        后结构分配信息
     * @param newStartDay       前结构起始日
     * @param addAllocationDays 增加的分配天数
     */
    private void addAfterGroupAllocationDay(TbrProductionContext productionContext, CxMachineAllocationPlanHelper afterGroup, Integer newStartDay, Integer addAllocationDays) {
        if (null == afterGroup) {
            return;
        }
        ProductionPlanGroupInfo groupInfo = afterGroup.getProductionPlanInfo();
        if (null == groupInfo) {
            return;
        }
        //特殊材料
        productionContext.updateSpecialMaterialInfoByTimeExtension(groupInfo);
        afterGroup.autoAdvanceProduction(newStartDay, addAllocationDays);
    }


}
