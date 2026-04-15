package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Maps;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupCapacityProductionLimitHelper;
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
import com.zlt.aps.mp.engine.scheduling.BaseDataContainer;
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
     * @param handledDayInfo              已经延长过的日期信息(分组名|*|成型机编号|*|排产日)
     */
    public void handlerTimeExtension(CxAddSkuProductionHandler cxAddSkuProductionHandler, Context context, String groupName, CxContinueInfoHelper cxContinueInfo, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation, Set<String> handledDayInfo) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return;
        }
        Set<String> allocationCxMachineSet = continueCxMachineAllocation.stream().map(CxMachineAllocationPlanHelper::getCxMachineCode).collect(Collectors.toSet());
        String cxMachineCodeInfo = allocationCxMachineSet.stream().collect(Collectors.joining(StringConstant.COMMA));
        GroupTimeExtensionConclusionLogRecorder.addGroupStartTimeExtensionConclusionLog(context, groupName, cxMachineCodeInfo, false);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Map<String, ProductionPlanGroupInfo> allGroupPlanInfo = productionContext.getGroupProductionInfo();
        ProductionPlanGroupInfo groupPlan = allGroupPlanInfo.get(groupName);
        CxMachineAllocationPlanHelper earliestConclusion = getTimeExtensionCxMachineAllocation(context, continueCxMachineAllocation);
        if (null == earliestConclusion) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(earliestConclusion.getCxMachineCode());
        Integer earliestConclusionDay = earliestConclusion.getEndDay();
        // 判断是否可延长收尾
        if (!hasTimeExtension(productionContext, groupPlan, cxMachineInfo, earliestConclusionDay)) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        //1、收尾时间延长一天 取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(earliestConclusionDay, stopDaySet);
        if (nextDay > context.getProductionEndDay()) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerByEndLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        String handlerKey = earliestConclusion.getTimeExtensionDayInfo(nextDay);
        if (handledDayInfo.contains(handlerKey)) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        handledDayInfo.add(handlerKey);
        //清除排产信息
        clearSimulateProductionDataByTimeExtension(context, groupName, groupPlan, continueCxMachineAllocation);
        //创建新的收尾天数信息
        timeExtensionOneDayConclusion(context, groupPlan, earliestConclusion, nextDay);
        //重新设置信息
        resetProductionLimitInfo(context, groupPlan, continueCxMachineAllocation);
        /**
         * 重新排产模拟
         * 1、先续作Sku->同规格同花纹->同模具
         * 2、再新增Sku
         */
        productionContinueBySingleGroup(cxAddSkuProductionHandler, ProductionStageEnum.SIMULATE_STAGE, productionContext, groupName, cxContinueInfo, allGroupPlanInfo);
        //新增Sku
        cxAddSkuProductionHandler.productionAddSkuBySingleGroup(context, ProductionStageEnum.SIMULATE_STAGE, groupPlan, groupName, cxContinueInfo, continueCxMachineAllocation, handledDayInfo);
    }

    /**
     * 单机台分配的延长收尾处理
     *
     * @param cxMouldProductionHandler 新增Sku排产处理器(因循环依赖，采用参数传递)
     * @param context                  排产上下文
     * @param allocationRange          当前分配段
     * @param handledDayInfo           已经延长过的日期信息(分组名|*|成型机编号|*|排产日)
     */
    public void handlerTimeExtension(CxMouldProductionHandler cxMouldProductionHandler, Context context, CxMachineAllocationPlanHelper allocationRange, Set<String> handledDayInfo) {
        if (null == allocationRange) {
            return;
        }
        String cxMachineCodeInfo = allocationRange.getCxMachineCode();
        ProductionPlanGroupInfo groupPlan = allocationRange.getProductionPlanInfo();
        String groupName = groupPlan.getGroupName();
        if (allocationRange.getAllocationDay() <= BigDecimal.ZERO.intValue()) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        GroupTimeExtensionConclusionLogRecorder.addGroupStartTimeExtensionConclusionLog(context, groupName, cxMachineCodeInfo, true);
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCodeInfo);
        Integer earliestConclusionDay = allocationRange.getEndDay();
        // 判断是否可延长收尾
        if (!hasTimeExtension(productionContext, groupPlan, cxMachineInfo, earliestConclusionDay)) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        //1、收尾时间延长一天 取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(earliestConclusionDay, stopDaySet);
        if (nextDay > context.getProductionEndDay()) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerByEndLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        String handlerKey = allocationRange.getTimeExtensionDayInfo(nextDay);
        if (handledDayInfo.contains(handlerKey)) {
            GroupTimeExtensionConclusionLogRecorder.addNoTimeExtensionConclusionHandlerLog(context, groupName, cxMachineCodeInfo);
            return;
        }
        handledDayInfo.add(handlerKey);
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = cxMachineInfo.getDayProductionLimitInfo();
        //清除排产信息--模具排产信息
        GroupTimeExtensionConclusionLogRecorder.addTimeExtensionConclusionHandlerStartClearDataLog(context, groupName, cxMachineCodeInfo);
        Set<Integer> deductionDaySet = cxMachineInfo.getAllocationDaySet(allocationRange);
        groupPlanDeductionDayHandler.deductionMouldDayInfo(context, DeductionDayProductionTypeEnum.TIME_EXTENSION_REST, cxMachineInfo, groupPlan, allocationRange, dayProductionLimitInfo, deductionDaySet);
        //创建新的收尾天数信息
        timeExtensionOneDayConclusionByNoOnLine(context, groupPlan, allocationRange, nextDay);
        //重新模拟排产
        cxMouldProductionHandler.noContinueGroupPlanMouldProduction(context, cxMachineCodeInfo, allocationRange, handledDayInfo);
    }

    /**
     * 判断分组是否可进行延长
     * 满足以下两个条件
     * 1、分组是否还有未排产的实单量Sku
     * 2、未排产实单的Sku有模具产能
     * 3、是否有对应的产能及成型工装
     *
     * @param context       排产上下文
     * @param groupPlan     结构
     * @param cxMachineInfo 成型机台
     * @param endDay        当前收尾日
     * @return
     */
    public boolean hasTimeExtension(Context context, ProductionPlanGroupInfo groupPlan, CxMachineBaseInfoVo cxMachineInfo, Integer endDay) {
        //最后一天，不能延长
        if (null == groupPlan || null == endDay || context.getProductionEndDay().equals(endDay)) {
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
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        Integer conclusionLhMachine = baseDataContainer.getMinConclusionLhMachineCount(groupPlan.getGroupName());
        if (conclusionLhMachine <= BigDecimal.ZERO.intValue()) {
            return false;
        }
        //取得下一天
        Set<Integer> stopDaySet = Optional.ofNullable(context.getStopDays()).orElse(Collections.emptySet());
        Integer nextDay = context.getNextHasProductionDay(endDay, stopDaySet);
        if (cxMachineInfo.getAllocationDaySet().contains(nextDay)) {
            return false;
        }
        //成型工装可用
        GroupCapacityProductionLimitHelper limitResult = baseDataContainer.getLeftOverProductionDayInfo(context, groupPlan, cxMachineInfo);
        Set<Integer> productionDayInfo = limitResult.getProductionDaySet();
        if (!productionDayInfo.contains(endDay)) {
            return false;
        }
        //判断模具是否还有剩余产能
        Set<String> materialDescSet = hasActualNeedProductionList.stream().map(MonthPlanProductionRequirePlanVo::getMaterialDesc).collect(Collectors.toSet());
        Set<String> hasMouldCapacitySet = productionContext.getHasMouldCapacity(ProductionConstant.DOUBLE_MOULD_PRODUCTION, materialDescSet, nextDay, nextDay);
        if (CollectionUtils.isEmpty(hasMouldCapacitySet)) {
            return false;
        }
        return true;
    }

    /**
     * 获取需要延长收尾的排产配置
     *
     * @param context                     排产上下文
     * @param continueCxMachineAllocation 在机结构在产机台分配信息
     */
    private CxMachineAllocationPlanHelper getTimeExtensionCxMachineAllocation(Context context, List<CxMachineAllocationPlanHelper> continueCxMachineAllocation) {
        if (CollectionUtils.isEmpty(continueCxMachineAllocation)) {
            return null;
        }
        //&& single.isTimeExtensionFlag()
        List<CxMachineAllocationPlanHelper> effectiveList = continueCxMachineAllocation.stream().filter(single -> single.getEndDay() < context.getProductionEndDay()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return null;
        }
        //延长时间最长的，表明最合适，优先保留 此时只会有一条
        effectiveList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getEndDay, Comparator.nullsLast(Comparator.reverseOrder())));
        return effectiveList.get(BigDecimal.ZERO.intValue());
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
            GroupTimeExtensionConclusionLogRecorder.addTimeExtensionConclusionHandlerStartClearDataLog(context, groupName, cxMachineInfo.getCxMachineCode());
            Set<Integer> deductionDaySet = cxMachineInfo.getAllocationDaySet(singleAllocationInfo);
            groupPlanDeductionDayHandler.deductionMouldDayInfo(context, DeductionDayProductionTypeEnum.TIME_EXTENSION_REST, cxMachineInfo, groupPlan, singleAllocationInfo, dayProductionLimitInfo, deductionDaySet);
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
        if (!checkBaseInfo(context, groupPlan, earliestConclusion, newEndDay)) {
            return;
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        CxMachineAllocationPlanHelper lastAllocationInfo = cxMachineInfo.getLastAllocationInfo();
        if (lastAllocationInfo != earliestConclusion) {
            return;
        }
        //分配延长一天
        lastAllocationInfo.timeExtensionOneDay(newEndDay);
        //机台延长一天
        cxMachineInfo.timeExtensionOneDayConclusion(productionContext, newEndDay, lastAllocationInfo);
        //分组计划增加分配一天
        groupPlan.timeExtensionOneDayConclusion();
    }

    /**
     * 对分组计划在earliestConclusion分配基础上延长收尾一天
     * 非在机机构在产机台阶段
     *
     * @param context            排产上下文
     * @param groupPlan          分组计划
     * @param earliestConclusion 收尾的分配信息
     * @param newEndDay          新的收尾日
     */
    private void timeExtensionOneDayConclusionByNoOnLine(Context context, ProductionPlanGroupInfo groupPlan, CxMachineAllocationPlanHelper earliestConclusion, Integer newEndDay) {
        if (!checkBaseInfo(context, groupPlan, earliestConclusion, newEndDay)) {
            return;
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        //分配延长一天
        earliestConclusion.timeExtensionOneDay(newEndDay);
        //机台延长一天
        cxMachineInfo.timeExtensionOneDayConclusion(productionContext, newEndDay, earliestConclusion);
        //分组计划增加分配一天
        groupPlan.timeExtensionOneDayConclusion();
    }

    /**
     * 校验基础数据是否正确
     * false 不正确
     * true 正确
     *
     * @param context            排产上下文
     * @param groupPlan          分组计划
     * @param earliestConclusion 收尾分配段
     * @param newEndDay          新的收尾日
     * @return
     */
    private boolean checkBaseInfo(Context context, ProductionPlanGroupInfo groupPlan, CxMachineAllocationPlanHelper earliestConclusion, Integer newEndDay) {
        if (null == groupPlan || null == earliestConclusion || null == newEndDay) {
            return false;
        }
        if (!groupPlan.getGroupName().equals(earliestConclusion.getAllocationGroup())) {
            return false;
        }
        if (newEndDay.equals(earliestConclusion.getEndDay())) {
            return false;
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        if (null == cxMachineInfo) {
            return false;
        }
        return true;
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
        //处理计划的待排产量及排产标记重置
        List<MonthPlanProductionRequirePlanVo> groupAllPlanList = groupPlan.getGroupPlanData();
        if (!CollectionUtils.isEmpty(groupAllPlanList)) {
            groupAllPlanList.forEach(singlePlan -> singlePlan.resetProductionDataInfo());
        }
        groupPlan.setDayProductionLimitInfo(Maps.newHashMap());
        groupPlan.buildDayProductionLimitInfoByContinue(context, newAllocationInfoList);
        groupPlan.setDailyCapacityLimitVoMap(Maps.newHashMap());
    }

}
