package com.zlt.aps.mp.engine.scheduling.cxcapacity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
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
            if (handledDayInfo.size() > BigDecimal.ONE.intValue()) {
                handlerTimeExtensionDay(context, groupPlan, earliestConclusion, handledDayInfo, nextDay);
            }
            //20260425+ 标记不再进行分配？
            groupPlan.setIsAllocationFinish(YesOrNoEnum.YES.getValue());
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
            if (handledDayInfo.size() > BigDecimal.ONE.intValue()) {
                handlerTimeExtensionDay(context, groupPlan, allocationRange, handledDayInfo, nextDay);
            }
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
     * 前分组，因衔接分组日切换次数限制，导致需要自动延长收尾
     * 场景：后分组衔接时，因每日切换次数限制，后分组需往后起始排产
     * 则前结构自动延长
     *
     * @param context          排产上下文
     * @param beforeAllocation 前分组配置
     */
    public void handlerTimeExtensionDayConclusion(Context context, CxMachineAllocationPlanHelper beforeAllocation) {
        if (null == beforeAllocation) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        String cxMachineCode = beforeAllocation.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            return;
        }
        //取得需要延长的排产日集合
        List<Integer> effectiveDayList = getTimeExtensionDayList(productionContext, cxMachineInfo, beforeAllocation);
        if (CollectionUtils.isEmpty(effectiveDayList)) {
            return;
        }
        effectiveDayList.forEach(timeExtensionDay -> forceTimeExtensionOneDayConclusion(context, beforeAllocation, timeExtensionDay));
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
     * 当nextDay已经处理过时，则取在此之前的最后一个日为其收尾日
     *
     * @param context            排产上下文
     * @param groupPlan          分组对象
     * @param earliestConclusion 收尾信息
     * @param handledDayInfo     处理的排产日信息
     * @param nextDay            下一个日期
     */
    private void handlerTimeExtensionDay(Context context, ProductionPlanGroupInfo groupPlan, CxMachineAllocationPlanHelper earliestConclusion, Set<String> handledDayInfo, Integer nextDay) {
        String handlerKey = earliestConclusion.getTimeExtensionDayInfo(nextDay);
        if (!handledDayInfo.contains(handlerKey)) {
            return;
        }
        //20260511+ 因加入高优先级强制收尾业务，导致延长探测会出现跳跃，故而获取截止nextDay连续有效探测日
        Set<String> effectiveDetectKey = getEffectiveDetectDayInfo(context, earliestConclusion, handledDayInfo, nextDay);
        if (CollectionUtils.isEmpty(effectiveDetectKey)) {
            return;
        }
        String prefix = earliestConclusion.getTimeExtensionPrefix();
        Integer startIndex = prefix.length();
        Integer earliestConclusionDay = earliestConclusion.getEndDay();
        Set<Integer> effectiveDaySet = Sets.newHashSet();
        effectiveDetectKey.forEach(singleDay -> {
            if (!singleDay.startsWith(prefix)) {
                return;
            }
            Integer endIndex = singleDay.length();
            String dayValue = singleDay.substring(startIndex, endIndex);
            Integer day = Integer.valueOf(dayValue);
            if (day > earliestConclusionDay) {
                effectiveDaySet.add(day);
            }
        });
        if (CollectionUtils.isEmpty(effectiveDaySet)) {
            return;
        }
        List<Integer> timeExtensionDayList = new ArrayList<>(effectiveDaySet);
        timeExtensionDayList.sort(Comparator.comparing(Integer::intValue));
        List<Integer> effectiveDayList = timeExtensionDayList.subList(BigDecimal.ZERO.intValue(), timeExtensionDayList.size() - BigDecimal.ONE.intValue());
        effectiveDayList.forEach(timeExtensionDay -> timeExtensionOneDayConclusion(context, groupPlan, earliestConclusion, timeExtensionDay));
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
     * 对分组计划在earliestConclusion分配基础上强制延长收尾一天
     *
     * @param context            排产上下文
     * @param earliestConclusion 收尾的分配信息
     * @param newEndDay          新的收尾日
     */
    private void forceTimeExtensionOneDayConclusion(Context context, CxMachineAllocationPlanHelper earliestConclusion, Integer newEndDay) {
        TbrProductionContext productionContext = (TbrProductionContext) context;
        ProductionPlanGroupInfo groupPlan = earliestConclusion.getProductionPlanInfo();
        if (!checkBaseInfo(context, groupPlan, earliestConclusion, newEndDay)) {
            return;
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        //存在分配才延长
        if (!cxMachineInfo.hasAllocation(earliestConclusion)) {
            return;
        }
        //分配延长一天
        earliestConclusion.timeExtensionOneDay(newEndDay);
        //机台延长一天
        cxMachineInfo.timeExtensionOneDayConclusion(productionContext, newEndDay, earliestConclusion);
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
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        if (null == cxMachineInfo) {
            return false;
        }
        if (newEndDay.equals(earliestConclusion.getEndDay()) && !isMonthCycleContinueTimeExtension(productionContext, earliestConclusion)) {
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
            //20260626+ 非正式阶段不在月周期清单中标记不排
            groupAllPlanList.forEach(singlePlan -> singlePlan.resetProductionDataInfo(singlePlan.isFlagFalse(context)));
        }
        groupPlan.setDayProductionLimitInfo(Maps.newHashMap());
        groupPlan.buildDayProductionLimitInfoByContinue(context, newAllocationInfoList);
        groupPlan.setDailyCapacityLimitVoMap(Maps.newHashMap());
    }

    /**
     * 20260511+ 获取有效延长收尾探测日信息
     * 1、nextDay已经存在
     * 2、从nextDay开始，往后延续日且大于nextDay的日期，剔除最后一个日期
     *
     * @param context            排产上下文
     * @param earliestConclusion 分配信息
     * @param handledDayInfo     已经探测的所有日信息
     * @param nextDay            最后一次尝试探测日
     * @return
     */
    private Set<String> getEffectiveDetectDayInfo(Context context, CxMachineAllocationPlanHelper earliestConclusion, Set<String> handledDayInfo, Integer nextDay) {
        if (null == earliestConclusion || CollectionUtils.isEmpty(handledDayInfo) || null == nextDay) {
            return Collections.emptySet();
        }
        String handlerKey = earliestConclusion.getTimeExtensionDayInfo(nextDay);
        if (!handledDayInfo.contains(handlerKey)) {
            return Collections.emptySet();
        }
        String selectedCxMachineCode = earliestConclusion.getCxMachineCode();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(selectedCxMachineCode);
        if (null == cxMachineInfo) {
            return Collections.emptySet();
        }
        //获取截止nextDay连续的探测日，大于nextDay，且与nextDay是连续的
        Set<Integer> matchDetectDay = Sets.newHashSet();
        Set<Integer> stopDays = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        Integer endDay = context.getProductionEndDay();
        Integer startDay = nextDay;
        for (; startDay <= endDay; ) {
            String effectiveKey = earliestConclusion.getTimeExtensionDayInfo(startDay);
            if (stopDays.contains(startDay)) {
                startDay = startDay + BigDecimal.ONE.intValue();
                continue;
            }
            if (handledDayInfo.contains(effectiveKey)) {
                matchDetectDay.add(startDay);
                startDay = startDay + BigDecimal.ONE.intValue();
                continue;
            }
            break;
        }
        if (CollectionUtils.isEmpty(matchDetectDay) || matchDetectDay.size() <= BigDecimal.ONE.intValue()) {
            return Collections.emptySet();
        }
        Set<String> effectiveKeySet = Sets.newHashSet();
        matchDetectDay.forEach(day -> effectiveKeySet.add(earliestConclusion.getTimeExtensionDayInfo(day)));
        return effectiveKeySet;
    }

    /**
     * 获取前结构需要延长的排产日集合
     * 1、前结构延长日不可小于原有收尾日
     * 2、新的收尾日不可小于周期排产起始日
     * 3、新的收尾日 = 原有收尾日时，看是否是续作结构，是则表示周期续作延长
     *
     * @param productionContext 排产上下文
     * @param cxMachineInfo     成型产能对象
     * @param beforeAllocation  前结构延长信息对象
     * @return
     */
    private List<Integer> getTimeExtensionDayList(TbrProductionContext productionContext, CxMachineBaseInfoVo cxMachineInfo, CxMachineAllocationPlanHelper beforeAllocation) {
        Integer newEndDay = beforeAllocation.getTimeExtensionDay();
        if (null == newEndDay) {
            return Collections.emptyList();
        }
        Integer cycleFirstProductionDay = productionContext.getCycleFirstProductionDay();
        if (newEndDay < cycleFirstProductionDay) {
            return Collections.emptyList();
        }
        Integer originEndDay = beforeAllocation.getEndDay();
        if (originEndDay > newEndDay) {
            return Collections.emptyList();
        }
        List<Integer> effectiveDayList = Lists.newArrayList();
        Set<Integer> stopDays = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        //周期结构不在月周期名单的续作延长
        int startTimeExtensionDay;
        if (originEndDay.equals(newEndDay) && isMonthCycleContinueTimeExtension(productionContext, beforeAllocation)) {
            startTimeExtensionDay = cycleFirstProductionDay;
        } else {
            startTimeExtensionDay = originEndDay + BigDecimal.ONE.intValue();
        }
        for (; startTimeExtensionDay <= newEndDay; startTimeExtensionDay++) {
            if (stopDays.contains(startTimeExtensionDay)) {
                continue;
            }
            effectiveDayList.add(startTimeExtensionDay);
        }
        return effectiveDayList;
    }

    /**
     * 判断是否周期结构且不在月周期清单中的续作结构延长
     *
     * @param productionContext 排产上下文
     * @param beforeAllocation  结构延长信息对象
     * @return
     */
    private boolean isMonthCycleContinueTimeExtension(TbrProductionContext productionContext, CxMachineAllocationPlanHelper beforeAllocation) {
        if (null == beforeAllocation) {
            return false;
        }
        String cxMachineCode = beforeAllocation.getCxMachineCode();
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(cxMachineCode);
        if (null == cxMachineInfo) {
            return false;
        }
        ProductionPlanGroupInfo groupInfo = beforeAllocation.getProductionPlanInfo();
        if (null == groupInfo) {
            return false;
        }
        if (!groupInfo.isCycleType()) {
            return false;
        }
        String continueGroupName = productionContext.getContinueStructureMap().get(cxMachineInfo.getCxMachineCode());
        if (!groupInfo.getGroupName().equals(continueGroupName)) {
            return false;
        }
        return true;
    }

    @Override
    public void handlerByMoldAllocationAdjust(Context context, ProductionStageEnum productionStage, Map<String, CxContinueInfoHelper> allContinueInfo, List<CxMachineAllocationPlanHelper> continueAllocationList, Map<String, ProductionPlanGroupInfo> allGroupPlanInfo, List<MpStructureAllocation> allAllocationList) {

    }
}
