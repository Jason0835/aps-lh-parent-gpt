package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Maps;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxContinueInfoHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductionRequirePlanVo;
import com.zlt.aps.mp.engine.enums.DeductionDayProductionTypeEnum;
import com.zlt.aps.mp.engine.enums.ProductionStageEnum;
import com.zlt.aps.mp.engine.handler.GroupPlanDeductionDayHandler;
import com.zlt.aps.mp.engine.logrecorder.GroupTimeExtensionConclusionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分组计划-分配时间延长处理器
 *
 * @author ZLT
 * @date 20260328
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupTimeExtensionHandler extends OnLineGroupOnLineMachineHandler {

    private final GroupPlanDeductionDayHandler groupPlanDeductionDayHandler;

    /**
     * 在机结构对在产机台排产后，分组计划是否需要在估算天数分配后进行结构延长处理
     *
     * @param cxAddSkuProductionHandler   新增Sku排产处理器(因循环依赖，采用参数传递)
     * @param context                     排产上下文
     * @param groupName                   分组名
     * @param cxContinueInfo              续作信息
     * @param continueCxMachineAllocation 在产机台分配情况
     */
    public void handlerTimeExtension(CxAddSkuProductionHandler cxAddSkuProductionHandler, Context context, String groupName, CxContinueInfoHelper cxContinueInfo, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
        }
        Set<String> allocationCxMachineSet = continueCxMachineAllocation.stream().map(CxMachineAllocationPlanHelper::getCxMachineCode).collect(Collectors.toSet());
        String cxMachineCodeInfo = allocationCxMachineSet.stream().collect(Collectors.joining(StringConstant.COMMA));
        log.info(GroupTimeExtensionConclusionLogRecorder.addGroupStartTimeExtensionConclusionLog(context, groupName, cxMachineCodeInfo));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanInfo = productionContext.getGroupProductionInfo();
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        continueCxMachineAllocation.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getEndDay));
        CxMachineAllocationPlanHelper earliestConclusion = continueCxMachineAllocation.get(BigDecimal.ZERO.intValue());
        Integer earliestConclusionDay = earliestConclusion.getEndDay();
        // 判断是否可延长收尾
        if (!hasTimeExtension(productionContext, groupPlan, earliestConclusionDay)) {
            log.info(GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo));
            return;
        }
        //1、收尾时间延长一天 取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(earliestConclusionDay, stopDaySet);
        if (nextDay > context.getProductionEndDay()) {
            log.info(GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerByEndLog(context, groupName, cxMachineCodeInfo));
            return;
        }
        //清除排产信息
        clearSimulateProductionDataByTimeExtension(context, groupName, groupPlan, continueCxMachineAllocation);
        //创建新的收尾天数信息
        timeExtensionOneDayConclusion(context, groupPlan, earliestConclusion, nextDay);
        //重新设置信息
        resetProductionLimitInfo(context, groupPlan, continueCxMachineAllocation);
        //2、重新排产模拟-先续作Sku->同规格同花纹->同模具->新增Sku
        productionContinueBySingleGroup(cxAddSkuProductionHandler, ProductionStageEnum.SIMULATE_STAGE, productionContext, groupName, cxContinueInfo, allGroupPlanInfo);
        cxAddSkuProductionHandler.productionAddSkuBySingleGroup(context, groupPlan, groupName, cxContinueInfo, continueCxMachineAllocation);
        //3、检测是否还需要延长

    }

    /**
     * 判断分组是否可进行延长
     * 满足以下两个条件
     * 1、分组是否还有未排产的实单量Sku
     * 2、未排产实单的Sku有模具产能
     *
     * @param context   排产上下文
     * @param groupPlan 结构
     * @param endDay    当前收尾日
     * @return
     */
    public boolean hasTimeExtension(Context context, ProductionPlanGroupInfo groupPlan, Integer endDay) {
        //最后一天，不能延长
        if (null == groupPlan || null == endDay || context.getProductionEndDay().equals(endDay)) {
            return false;
        }
        //没有分配完成：
        if (YesOrNoEnum.NO.getValue().equals(groupPlan.getIsAllocationFinish()) || groupPlan.getLeftOverNeedAllocationDays() > BigDecimal.ZERO.intValue()) {
            return false;
        }
        List<MonthPlanProductionRequirePlanVo> groupAllPlanList = groupPlan.getGroupPlanData();
        if (CollectionUtils.isEmpty(groupAllPlanList)) {
            return false;
        }
        //实单还有剩余排产量
        List<MonthPlanProductionRequirePlanVo> hasActualNeedProductionList = groupAllPlanList.stream().filter(singlePlan -> singlePlan.hasActualProductionQuantity()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hasActualNeedProductionList)) {
            return false;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer conclusionLhMachine = productionContext.getBaseDataContainer().getMinConclusionLhMachineCount(groupPlan.getGroupName());
        if (conclusionLhMachine <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(endDay, stopDaySet);
        //判断模具是否还有剩余产能
        Set<String> materialDescSet = hasActualNeedProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> hasMouldCapacitySet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, materialDescSet, nextDay, nextDay);
        if (CollectionUtils.isEmpty(hasMouldCapacitySet)) {
            return false;
        }
        return true;
    }

    /**
     * 清除groupPlan的所有排产信息
     *
     * @param context                     排产上下文
     * @param groupName                   分组名
     * @param groupPlan                   分组计划
     * @param continueCxMachineAllocation 所有排产分配
     */
    private void clearSimulateProductionDataByTimeExtension(Context context, String groupName, ProductionPlanGroupInfo groupPlan, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (null == groupPlan || StringUtils.isBlank(groupName) || CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlan.getDayProductionLimitInfo();
        continueCxMachineAllocation.forEach(singleAllocationInfo -> {
            if (!groupName.equals(singleAllocationInfo.getAllocationGroup())) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(singleAllocationInfo.getCxMachineCode());
            if (null == cxMachineInfo) {
                return;
            }
            log.info(GroupTimeExtensionConclusionLogRecorder.addTimeExtensionConclusionHandlerStartClearDataLog(context, groupName, cxMachineInfo.getCxMachineCode()));
            Set<Integer> deductionDaySet = cxMachineInfo.getAllocationDaySet(singleAllocationInfo);
            groupPlanDeductionDayHandler.deductionDayInfo(context, DeductionDayProductionTypeEnum.TIME_EXTENSION_REST, cxMachineInfo, groupPlan, singleAllocationInfo, dayProductionLimitInfo, deductionDaySet);
        });
    }

    /**
     * 对分组计划在earliestConclusion分配基础上延长收尾一天
     *
     * @param context            排产上下文
     * @param groupPlan          分组计划
     * @param earliestConclusion 收尾的分配信息
     * @param newEndDay          新的收尾日
     */
    private void timeExtensionOneDayConclusion(Context context, ProductionPlanGroupInfo groupPlan, CxMachineAllocationPlanHelper earliestConclusion, Integer newEndDay) {
        if (null == groupPlan || null == earliestConclusion || null == newEndDay) {
            return;
        }
        if (!groupPlan.getGroupName().equals(earliestConclusion.getAllocationGroup())) {
            return;
        }
        if (newEndDay.equals(earliestConclusion.getEndDay())) {
            return;
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        CxMachineAllocationPlanHelper lastAllocationInfo = cxMachineInfo.getLastAllocationInfo();
        if (lastAllocationInfo != earliestConclusion) {
            return;
        }
        //分配延长一天
        lastAllocationInfo.timeExtensionOneDay(newEndDay);
        //机台延长一天
        cxMachineInfo.timeExtensionOneDayConclusion(newEndDay);
        //分组计划增加分配一天
        groupPlan.timeExtensionOneDayConclusion();
    }

    /**
     * 重新设置分组的排产限制信息
     *
     * @param context               排产上下文
     * @param groupPlan             分组计划
     * @param newAllocationInfoList 新的分配信息
     */
    private void resetProductionLimitInfo(Context context, ProductionPlanGroupInfo groupPlan, List<CxMachineAllocationPlanHelper> newAllocationInfoList) {
        if (null == groupPlan || CollectionUtils.isEmpty(newAllocationInfoList)) {
            return;
        }
        groupPlan.setDayProductionLimitInfo(Maps.newHashMap());
        groupPlan.buildDayProductionLimitInfoByContinue(context, newAllocationInfoList);
        groupPlan.setDailyCapacityLimitVoMap(Maps.newHashMap());
    }

}
