package com.zlt.aps.factory.scheduling.cxcapacity;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.daylimit.DayCapacityLimitVo;
import com.zlt.aps.factory.daylimit.GroupCapacityProductionLimitHelper;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.factory.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.factory.domain.dto.ProductGroupCxCapacityInfo;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class SimulateProductionHandler {
    /**
     * 模拟排产计划
     *
     * @param productionContext      排产上下文
     * @param allGroupPlanMap        所有排产分组计划
     * @param continueAllocationList 在产机构在产机台的分配情况
     * @param allContinueMap         所有续作Sku
     */
    public void productionGroupPlan(TbrProductionContext productionContext, Map<String, ProductionPlanGroupInfo> allGroupPlanMap, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, CxContinueInfoHelper> allContinueMap) {
        //设置收尾机台信息-空
        productionContext.setReverseFindSet(new HashSet<>());
        //1、在机结构对在产成型机台进行模拟模具排产
        mouldProductionByContinueGroup(productionContext, allGroupPlanMap, continueAllocationList, allContinueMap);
        //2、对在产机台-收尾成型机台，反向匹配待排结构
        CxCapacityAllocationHandler.reverseMachineAllocation(productionContext, allGroupPlanMap);
        //3、对还需排产结构，获取优先级最高的结构--结构新增
        addNewGroupPlanHandler(productionContext, allGroupPlanMap);
    }

    /**
     * 1、对在机结构进行新增Sku的模具排产
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
        Map<ProductionPlanGroupInfo, List<CxMachineAllocationPlanHelper>> groupPlanMap = continueAllocationList.stream().collect(Collectors.groupingBy(CxMachineAllocationPlanHelper::getProductionPlanInfo));
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        allContinueMap.forEach((structureName, cxContinueInfo) -> {
            ProductionPlanGroupInfo groupPlanInfo = allGroupPlanMap.get(structureName);
            List<CxMachineAllocationPlanHelper> continueCxMachineAllocation = groupPlanMap.get(groupPlanInfo);
            if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
                log.warn(TbrBeforeProductionGroupLogRecorder.addContinueGroupNoOnLineMachineLog(productionContext, structureName, null));
                return;
            }
            groupPlanInfo.buildDayProductionLimitInfoByContinue(context, continueCxMachineAllocation);
            //1、todo 对测算成型产能分配的续作部分进行重排(先清空再重排-续作Sku一起排产)

            //2、在机结构-在产机台新增Sku排产 首先设置可排产的计划在本轮次可进行排产
            groupPlanInfo.setThisRoundCanProduction();
            //在机结构-新增Sku模拟排产
            CxAddSkuProductionHandler.productionAddSkuByContinueCxMachine(context, groupPlanInfo, new HashSet<>());
            //再次设置可排产的计划在本轮次可进行排产
            groupPlanInfo.setThisRoundCanProduction();
            //处理需要提前收尾(需要调整到成型机台下的收尾点，包含成型机台最后一个配置的分配信息和成型机台剩余时间调整)
            GroupPlanBeforeConclusionHandler.handlerBeforeConclusion(context, groupPlanInfo);
            //设置收尾机台
            continueCxMachineAllocation.forEach(cxMachineAllocation -> {
                String cxMachineCode = cxMachineAllocation.getCxMachineCode();
                CxMachineBaseInfoVo machineInfo = allCxMachineInfo.get(cxMachineCode);
                Integer newRemainingDays = machineInfo.getRemainingDays();
                //加入收尾匹配
                if (newRemainingDays > BigDecimal.ZERO.intValue()) {
                    productionContext.addReverseMachine(machineInfo.getCxMachineCode());
                }
            });
        });
    }

    /**
     * 2、对还需排产的结构，获取优先级最高的结构进行机台匹配排产
     *
     * @param context                      排产上下文
     * @param estimateGroupCxAllocationMap 分组计划需求量
     */
    private void addNewGroupPlanHandler(Context context, Map<String, ProductionPlanGroupInfo> estimateGroupCxAllocationMap) {
        ProductionPlanGroupInfo addNewGroupPlan = CxCapacityAllocationHandler.getInsertNewGroupPlan(context, estimateGroupCxAllocationMap);
        if (null == addNewGroupPlan) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addNoGetAddGroupPlanLog(context));
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String groupName = addNewGroupPlan.getGroupName();
        /**
         * 20260120 判断成型鼓是否符合条件
         * 20260125 分配产能限制控制 1、成型工装数量 2、日产能上限
         */
        GroupCapacityProductionLimitHelper limitResult = productionContext.getBaseDataContainer().getLeftOverProductionDayInfo(context, addNewGroupPlan, null);
        //获取成型工装的排产日集合
        Set<Integer> productionDayInfo = limitResult.getProductionDaySet();
        if (CollectionUtils.isEmpty(limitResult.getProductionDaySet())) {
            //20260120 标记分配完成--没有成型工装,没有日产能，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        //对挑选出的结构，匹配还有排产量的成型机台
        CxMachineBaseInfoVo selectedCxMachine = CxCapacityAllocationHandler.selectedCxMachineForGroupPlan(context, addNewGroupPlan, productionDayInfo);
        if (null == selectedCxMachine) {
            //记录日志
            log.info(TbrProductionGroupLogRecorder.addGroupNoSelectedCxMachineLog(context, groupName));
            //20260109 标记分配完成--没有找到合适，说明后面也找不到
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
            //下一新增结构
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
            return;
        }
        Set<Integer> hasProductionDaySet = selectedCxMachine.getSelectedProductionDaySet();
        Integer startDay = hasProductionDaySet.stream().mapToInt(Integer::intValue).min().getAsInt();
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
            addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
        }
        startDay = realChangeDay;
        Set<Integer> realProductionDaySet = hasProductionDaySet.stream().filter(singleDay -> singleDay >= realChangeDay).collect(Collectors.toSet());
        Integer remainingDays = realProductionDaySet.size();
        //分配产能
        ProductGroupCxCapacityInfo lhRatioInfo = addNewGroupPlan.getLhRatioByCxMachine(selectedCxMachine);
        Integer needAllocationDays = addNewGroupPlan.getRemainingNeedAllocationDays();
        Integer realAllocationDays = Math.min(remainingDays, needAllocationDays);
        //更新剩余天数
        Integer leftOver1 = remainingDays - realAllocationDays;
        addNewGroupPlan.updateLeftOverNeedAllocationDays(realAllocationDays);
        CxMachineAllocationPlanHelper addHelper = CxCapacityAllocationHandler.createAllocationPlanHelper(selectedCxMachine, lhRatioInfo, addNewGroupPlan, null, realAllocationDays, startDay, context.getMonthDays());
        selectedCxMachine.addAllocationPlanInfo(context, addHelper);
        //对成型机台进行模拟模具排产
        CxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, selectedCxMachine.getCxMachineCode(), addHelper);
        if (needAllocationDays <= remainingDays) {
            //20260108 标记分配完成
            addNewGroupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
        }
        //重新获取机台的剩余日 提前收尾
        Integer leftOver = selectedCxMachine.getRemainingDays();
        //反向机台匹配结构计划
        if (leftOver > BigDecimal.ZERO.intValue()) {
            CxCapacityAllocationHandler.selectedGroupPlanByCxMachine(context, estimateGroupCxAllocationMap, selectedCxMachine);
        }
        //下一新增结构
        addNewGroupPlanHandler(context, estimateGroupCxAllocationMap);
    }
}
