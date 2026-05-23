package com.zlt.aps.mp.engine.handler;

import cn.hutool.core.convert.Convert;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.dto.SkuDayProductionInfoHelper;
import com.zlt.aps.mp.engine.domain.vo.*;
import com.zlt.aps.mp.engine.logrecorder.GroupPlanConclusionLogRecorder;
import com.zlt.aps.mp.engine.logrecorder.TbrSimulateProductionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分组收尾业务处理器
 * 判断 分组可进行收尾的业务处理
 * 1、按分组计划判断：在机结构在产机台阶段，按分组整体维度判断
 * 2、分组挑选机台、机台挑选分组时，按单机台维度判断
 * 实单排产的硫化机台数低于分组要求的最低配比Lh机台数
 * <p>
 * TBR:结构
 * PCR:英寸
 *
 * @author ZLT
 * @date 20260329
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupPlanConclusionHandler {

    /**
     * 获取分组收尾信息
     * 场景：在机结构对在产机台的收尾处理
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组计划
     * @return conclusionDay ：不再排产，即在此前一日为分组收尾日
     * deductionDaySet 需要清除的排产日信息
     */
    public GroupConclusionInfoVo getConclusionInfoByProductionInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineAllocationPlanHelper> allAllocationInfo) {
        if (null == groupPlanInfo) {
            return null;
        }
        String groupName = groupPlanInfo.getGroupName();
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return null;
        }
        String cxMachineCodeInfo = "";
        if (!CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            cxMachineCodeInfo = allCxMachineCodeSet.stream().collect(Collectors.joining(StringConstant.COMMA));
        }
        GroupPlanConclusionLogRecorder.addGroupStartConclusionLog(context, groupName, cxMachineCodeInfo);
        Integer minLhMachineCount = groupPlanInfo.getClosureMinLhRatio();
        List<GroupPlanCxLhCapacityLimitHelper> realLowMinLhMachineDayList = getLowMinLhMachineDayInfo(context, groupPlanInfo, allAllocationInfo);
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            realLowMinLhMachineDayList = Lists.newArrayList();
        }
        //20260424+ 收尾机台选择 获取在 conclusionDay日前，等于最低实单硫化机台数的日期
        CxMachineBaseInfoVo selectCxMachineInfo = getConclusionCxMachine(context, groupPlanInfo, allAllocationInfo);
        if (null != selectCxMachineInfo) {
            CxMachineAllocationPlanHelper conclusionAllocationRange = selectCxMachineInfo.getLastAllocationInfo();
            handlerContinueDayMinLhMachineDays(context, groupPlanInfo, conclusionAllocationRange, realLowMinLhMachineDayList);
        }
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return GroupConclusionInfoVo.buildNoConclusionInfo(minLhMachineCount);
        }
        //按日期排序
        realLowMinLhMachineDayList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        //取得最小和最大日期
        Integer conclusionDay = realLowMinLhMachineDayList.get(BigDecimal.ZERO.intValue()).getDay();
        Set<Integer> deductionDaySet = realLowMinLhMachineDayList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        return new GroupConclusionInfoVo(minLhMachineCount, conclusionDay, deductionDaySet);
    }

    /**
     * 使用cxMachineInfo分配groupPlanInfo
     * 在conclusionRange分配段进行收尾业务判断
     * 场景：
     * 1、机台反选结构
     * 2、结构(新增需求)挑选机台
     *
     * @param context         排产上下文
     * @param groupPlanInfo   分组计划
     * @param cxMachineInfo   匹配的机台
     * @param cxLhRatio       硫化配比
     * @param conclusionRange 排产分段信息
     * @return
     */
    public GroupConclusionInfoVo getConclusionInfoByProductionInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, CxMachineBaseInfoVo cxMachineInfo, MonthPlanStructureLhRatioVo cxLhRatio, CxMachineAllocationPlanHelper conclusionRange) {
        if (null == groupPlanInfo || null == cxMachineInfo || null == conclusionRange || null == cxLhRatio) {
            return null;
        }
        String groupName = groupPlanInfo.getGroupName();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        Map<Integer, CxLhProductionHelper> cxLhRatioMap = cxMachineInfo.getCxLhRatioMap();
        if (CollectionUtils.isEmpty(cxLhRatioMap)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return null;
        }
        List<CxMachineAllocationPlanHelper> allocationList = cxMachineInfo.getAllocationList();
        if (CollectionUtils.isEmpty(allocationList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addCxMachineNoAllocationInfoLog(context, groupName, cxMachineCode);
            return null;
        }
        List<CxLhProductionHelper> cxLhGroupList = new ArrayList<>(cxLhRatioMap.values());
        List<CxLhProductionHelper> hasProductionList = cxLhGroupList.stream().filter(singleGroup -> !CollectionUtils.isEmpty(singleGroup.getProductionMouldSet())).collect(Collectors.toList());
        Integer minLhMachineCount = cxLhRatio.getLhMachineMinQty();
        //整段没有排产
        if (CollectionUtils.isEmpty(hasProductionList)) {
            Set<Integer> deductionDaySet = cxMachineInfo.getLastProductionDayInfo();
            Integer conclusionDay = conclusionRange.getStartDay();
            Integer deductionDay = conclusionRange.getAllocationDay();
            GroupConclusionInfoVo groupConclusionInfo = new GroupConclusionInfoVo(minLhMachineCount, conclusionDay, deductionDaySet);
            groupConclusionInfo.addSelectedConclusionCxMachine(cxMachineInfo);
            groupConclusionInfo.updateDeductionDaysByCxMachine(deductionDay);
            groupConclusionInfo.setWholeRangeConclusion();
            return groupConclusionInfo;
        }
        Integer startDay = conclusionRange.getStartDay();
        Integer endDay = conclusionRange.getEndDay();
        List<CxMachineUsedLhInfo> productionUsedLhInfoList = new ArrayList<>();
        for (int productionDay = startDay; productionDay <= endDay; productionDay++) {
            if (cxMachineInfo.getStopDayInfo().contains(productionDay)) {
                continue;
            }
            Integer matchDay = productionDay;
            List<CxLhProductionHelper> productionList = hasProductionList.stream().filter(singleGroup -> singleGroup.getProductionDay() >= matchDay).collect(Collectors.toList());
            productionUsedLhInfoList.add(CxMachineUsedLhInfo.build(matchDay, productionList.size()));
        }
        //获取使用硫化机台数低于minLhMachineCount的数据
        List<CxMachineUsedLhInfo> realLowMinLhMachineDayList = getLowMinLhMachineDayInfo(context, groupPlanInfo, productionUsedLhInfoList, conclusionRange, minLhMachineCount);

        //20260507+  获取单个成型机台高优先级占当日排产的硫化机台数<3的天数数据 20260523+ 排产信息取值错误，新增分配从cxMachineInfo对象中获取
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = cxMachineInfo.getDayProductionLimitInfo();
        if (!CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
            Map<String, Integer> heightQtyMap = getSkuHeightNeedProductionQty(groupPlanInfo, true);
            List<GroupPlanCxLhCapacityLimitHelper> lowHeightPriorityLhMachineList = this.getLowHeightPriorityLhMachineList(context, groupPlanInfo, heightQtyMap, dayLimitList);
            Set<Integer> lowHeightPriorityDaySet = lowHeightPriorityLhMachineList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
            List<CxMachineUsedLhInfo> lowHeightPriorityUsedLhInfoList = productionUsedLhInfoList.stream().filter(single -> lowHeightPriorityDaySet.contains(single.getProductionDay())).collect(Collectors.toList());
            // 取并集，同一天的对象引用相同，因此可以直接用set去重
            Set<CxMachineUsedLhInfo> set1 = new HashSet<>(realLowMinLhMachineDayList);
            Set<CxMachineUsedLhInfo> set2 = new HashSet<>(lowHeightPriorityUsedLhInfoList);
            set1.addAll(set2);
            realLowMinLhMachineDayList = new ArrayList<>(set1);
        }

        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            realLowMinLhMachineDayList = Lists.newArrayList();
        }
        //20260424+ 获取在 conclusionDay日前，等于最低实单硫化机台数的日期
        handlerContinueDayMinLhMachineDays(context, conclusionRange, realLowMinLhMachineDayList, productionUsedLhInfoList);
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return GroupConclusionInfoVo.buildNoConclusionInfo(minLhMachineCount);
        }
        //按日期排序
        realLowMinLhMachineDayList.sort(Comparator.comparing(CxMachineUsedLhInfo::getProductionDay));
        //取得最小日期
        Integer conclusionDay = realLowMinLhMachineDayList.get(BigDecimal.ZERO.intValue()).getProductionDay();
        Integer deductionDay = realLowMinLhMachineDayList.size();
        Set<Integer> deductionDaySet = realLowMinLhMachineDayList.stream().map(CxMachineUsedLhInfo::getProductionDay).collect(Collectors.toSet());
        GroupConclusionInfoVo groupConclusionInfo = new GroupConclusionInfoVo(minLhMachineCount, conclusionDay, deductionDaySet);
        groupConclusionInfo.addSelectedConclusionCxMachine(cxMachineInfo);
        groupConclusionInfo.updateDeductionDaysByCxMachine(deductionDay);
        return groupConclusionInfo;
    }

    /**
     * 从分组计划分配的成型机台中，获取提前收尾，优先级值大的机台(优先级值越大，越可提前下机)
     * 场景：在机机构在产机台的排产，故而此时机台都是只有一个结构分配
     *
     * @param context        排产上下文
     * @param groupPlanInfo  分组
     * @param allocationList 机台分配信息
     * @return
     */
    public CxMachineBaseInfoVo getConclusionCxMachine(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineAllocationPlanHelper> allocationList) {
        if (null == groupPlanInfo || CollectionUtils.isEmpty(allocationList)) {
            return null;
        }
        List<CxMachineAllocationPlanHelper> realAllocationList = allocationList.stream().filter(singleAllocation -> groupPlanInfo.equals(singleAllocation.getProductionPlanInfo())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(realAllocationList)) {
            return null;
        }
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        realAllocationList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getReleasePriority, Comparator.reverseOrder()));
        String selectedCxMachineCode = realAllocationList.get(BigDecimal.ZERO.intValue()).getCxMachineCode();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(selectedCxMachineCode);
        if (null == cxMachineInfo) {
            return null;
        }
        MonthPlanStructureLhRatioVo findLhRatio = groupPlanInfo.getLhRatio(cxMachineInfo);
        if (null == findLhRatio) {
            return null;
        }
        return cxMachineInfo;
    }

    /**
     * 在低于硫化机台数的日期中是否包含结构收尾日
     *
     * @param lowMinLhMachineDayList 低于硫化机台数天集合信息
     * @param groupConclusionDaySet  分组收尾日信息
     * @return
     */
    private boolean hasConclusionDayLowLhMachine(List<GroupPlanCxLhCapacityLimitHelper> lowMinLhMachineDayList, Set<Integer> groupConclusionDaySet) {
        if (CollectionUtils.isEmpty(lowMinLhMachineDayList) || CollectionUtils.isEmpty(groupConclusionDaySet)) {
            return false;
        }
        for (GroupPlanCxLhCapacityLimitHelper lowMinLhMachineDay : lowMinLhMachineDayList) {
            Integer day = lowMinLhMachineDay.getDay();
            if (groupConclusionDaySet.contains(day)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取低于实单最低硫化机台数的排产日信息
     * 结构在产机台-处理阶段
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组计划对象
     * @param allAllocationInfo 机台分配信息
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getLowMinLhMachineDayInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineAllocationPlanHelper> allAllocationInfo) {
        String groupName = groupPlanInfo.getGroupName();
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return Collections.emptyList();
        }
        Integer minLhMachineCount = groupPlanInfo.getClosureMinLhRatio();
        if (null == minLhMachineCount) {
            GroupPlanConclusionLogRecorder.addNoLhRatioInfoLog(context, groupName);
            return Collections.emptyList();
        }

        List<GroupPlanCxLhCapacityLimitHelper> lowMinLhMachineDayList = getForcedConclusionDayInfo(groupPlanInfo, dayProductionLimitInfo);

        //20260507+  获取单个成型机台高优先级占当日排产的硫化机台数<3的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        Map<String, Integer> heightQtyMap = getSkuHeightNeedProductionQty(groupPlanInfo, false);
        List<GroupPlanCxLhCapacityLimitHelper> lowHeightPriorityLhMachineList = this.getLowHeightPriorityLhMachineList(context, groupPlanInfo, heightQtyMap, dayLimitList);
        // 取并集，同一天的对象引用相同，因此可以直接用set去重
        Set<GroupPlanCxLhCapacityLimitHelper> set1 = new HashSet<>(lowMinLhMachineDayList);
        Set<GroupPlanCxLhCapacityLimitHelper> set2 = new HashSet<>(lowHeightPriorityLhMachineList);
        set1.addAll(set2);
        lowMinLhMachineDayList = new ArrayList<>(set1);

        if (CollectionUtils.isEmpty(lowMinLhMachineDayList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return Collections.emptyList();
        }
        //20260411+ 不间断的收尾时间:获取分配收尾日集合
        Set<Integer> groupConclusionDaySet = getGroupConclusionDayInfo(context, groupPlanInfo, allAllocationInfo);
        if (CollectionUtils.isEmpty(groupConclusionDaySet)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return Collections.emptyList();
        }
        //不间断的收尾时间: 没有分组收尾日，则表示中间限制原因导致
        if (!hasConclusionDayLowLhMachine(lowMinLhMachineDayList, groupConclusionDaySet)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return Collections.emptyList();
        }
        //不间断的收尾时间: 连续收尾日集合
        return getForcedConclusionDayInfo(context, lowMinLhMachineDayList, allAllocationInfo);
    }

    /**
     * 获取单成型机台分配分组，实单排产硫化机台数低于最低硫化机台数的排产日信息
     *
     * @param context                  排产上下文
     * @param groupPlanInfo            分组计划对象
     * @param productionUsedLhInfoList 日排产信息
     * @param conclusionRange          分配段
     * @param minLhMachineCount        实单最低硫化机台数
     * @return
     */
    private List<CxMachineUsedLhInfo> getLowMinLhMachineDayInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineUsedLhInfo> productionUsedLhInfoList, CxMachineAllocationPlanHelper conclusionRange, Integer minLhMachineCount) {
        String groupName = groupPlanInfo.getGroupName();
        //获取使用硫化机台数低于minLhMachineCount的数据
        List<CxMachineUsedLhInfo> lowMinLhMachineCountList = productionUsedLhInfoList.stream().filter(single -> single.getUsedLhMachineCount() < minLhMachineCount).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinLhMachineCountList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return Collections.emptyList();
        }
        //20260411+ 不间断的收尾时间
        List<CxMachineUsedLhInfo> realLowMinLhMachineDayList = getForcedConclusionDayInfo(context, lowMinLhMachineCountList, conclusionRange);
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return Collections.emptyList();
        }
        return realLowMinLhMachineDayList;
    }

    /**
     * 获取需要强制收尾的日排产信息
     * 实单排产硫化机台数低于要求的最低硫化机台数
     *
     * @param groupPlanInfo          分组信息
     * @param dayProductionLimitInfo 日排产信息集合
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getForcedConclusionDayInfo(ProductionPlanGroupInfo groupPlanInfo, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取使用硫化机台数低于dayMinLhMachineCount的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> lowMinLhMachineList = dayLimitList.stream().filter(singleDay -> {
            Integer day = singleDay.getDay();
            MpDailyCapacityLimitVo dailyCapacityLimit = groupPlanInfo.getDailyCapacityLimitVoMap().get(day);
            Integer usedLhMachineCount = null == dailyCapacityLimit ? BigDecimal.ZERO.intValue() : Optional.ofNullable(dailyCapacityLimit.getUsedLhMachines()).orElse(BigDecimal.ZERO.intValue());
            return singleDay.isLowMinLhMachines(usedLhMachineCount);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinLhMachineList)) {
            return Collections.emptyList();
        }
        return lowMinLhMachineList;
    }

    /**
     * 获取单个成型机台高优先级占当日排产的硫化机台数<3的天数数据
     *
     * @param groupPlanInfo 分组计划对象
     * @param heightQtyMap  分组下所有待排Sku及数量
     * @param dayLimitList  日排产信息
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getLowHeightPriorityLhMachineList(Context context,
                                                                                     ProductionPlanGroupInfo groupPlanInfo,
                                                                                     Map<String, Integer> heightQtyMap,
                                                                                     List<GroupPlanCxLhCapacityLimitHelper> dayLimitList) {
        //20260523+ 设置为结构优先，则跳过高优级量强制收尾业务-高优先级硫化机台数限制数
        Integer minHeightLhMachineLimit = YesOrNoEnum.YES.getValue().equals(groupPlanInfo.isStructurePriority()) ? null : groupPlanInfo.getMinHeightPriorityLhMachineCount();
        TbrSimulateProductionLogRecorder.addStartHeightPriorityLhMachineLog(context, groupPlanInfo.getGroupName(), heightQtyMap, minHeightLhMachineLimit);
        if (null == minHeightLhMachineLimit) {
            return Collections.emptyList();
        }
        //高优先级台数低于阈值机台列表
        List<GroupPlanCxLhCapacityLimitHelper> lowHeightPriorityLhMachineList = new ArrayList<>();
        TreeMap<Integer, List<GroupPlanCxLhCapacityLimitHelper>> dayLimitMap = dayLimitList.stream().collect(
                Collectors.groupingBy(GroupPlanCxLhCapacityLimitHelper::getDay, TreeMap::new, Collectors.toList()));
        Integer latestDay = 0;
        //遍历各排产日，找出高优先级的SKU的机台占用超过阈值的最晚日期（默认2）
        for (Entry<Integer, List<GroupPlanCxLhCapacityLimitHelper>> entry : dayLimitMap.entrySet()) {
            GroupPlanCxLhCapacityLimitHelper singleDay = CollectionUtils.firstElement(entry.getValue());
            Integer day = singleDay.getDay();
            //取出当天的排产量
            Map<String, SkuDayProductionInfoHelper> productionSkuQtyMap = singleDay.getProductionSkuQtyInfo();
            //高优先级排产机台数
            int heightPriorityMachineCount = 0;
            //当天没排产，直接添加到列表中
            if (!CollectionUtils.isEmpty(productionSkuQtyMap)) {
                //内层循环，遍历当天每个SKU排产数据
                for (Entry<String, SkuDayProductionInfoHelper> skuQtyEntry : productionSkuQtyMap.entrySet()) {
                    String materialDesc = skuQtyEntry.getKey();
                    //取出SKU剩余未排的高优先级需求量
                    Integer heightQty = heightQtyMap.getOrDefault(materialDesc, 0);
                    //如果没有高优先级需求量，则看下一个SKU
                    if (heightQty == 0) {
                        continue;
                    }
                    //如果还有高优先级未排量，则高优先级机台数累加
                    SkuDayProductionInfoHelper dayProductionInfo = skuQtyEntry.getValue();
                    Integer dayProductionQty = Convert.toInt(dayProductionInfo.getSumProductionQty(), 0);
                    heightPriorityMachineCount += dayProductionInfo.getUsedMouldSet().size() / ProductionConstant.DOUBLE_MOULD_PRODUCTION;
                    //分配高优先级需求量-计算分配后剩余的高优先级未排量
                    Integer remainHeightQty = heightQty > dayProductionQty ? heightQty - dayProductionQty : 0;
                    heightQtyMap.put(materialDesc, remainHeightQty);
                }
            }
            //判断高优先级排产机台数是否达到阈值，达到了则更新最晚达标天数
            if (heightPriorityMachineCount >= minHeightLhMachineLimit && latestDay < day) {
                latestDay = day;
            }
        }

        if (latestDay > 0) {
            // 检查最晚满足天数后是否还有其他日期，如果有则说明后续天数是不达标的，需要添加到收尾列表中
            Integer currentDay = dayLimitMap.higherKey(latestDay);
            while (currentDay != null) {
                lowHeightPriorityLhMachineList.addAll(dayLimitMap.get(currentDay));
                // 取下一个天
                currentDay = dayLimitMap.higherKey(currentDay);
            }
        }

        return lowHeightPriorityLhMachineList;
    }

    /**
     * 20260523+ 获取Sku-高优先级需求量
     * 1、在【在机】分组对在产机台进行排产阶段(即续作阶段)，则为分组的原始高需求量
     * 2、在进行分组新增机台阶段，则为新机台排产前待排高优先量
     *
     * @param groupPlanInfo  分组计划对象
     * @param isAddCxMachine 是否新增机台分配
     * @return
     */
    private Map<String, Integer> getSkuHeightNeedProductionQty(ProductionPlanGroupInfo groupPlanInfo, boolean isAddCxMachine) {
        if (isAddCxMachine) {
            Map<String, SkuProductionSnapshot> beforeProductionSnapshotMap = groupPlanInfo.getBeforeProductionSnapshotMap();
            if (CollectionUtils.isEmpty(beforeProductionSnapshotMap)) {
                return Collections.emptyMap();
            }
            Map<String, Integer> heightQtyMap = Maps.newHashMap();
            beforeProductionSnapshotMap.forEach((materialDesc, snapshot) -> heightQtyMap.put(materialDesc, snapshot.getHeightProductionQty()));
            return heightQtyMap;
        }
        //累计各sku的高优先级需求量
        Map<String, Integer> heightQtyMap = groupPlanInfo.getGroupPlanData().stream()
                .collect(Collectors.groupingBy(MonthPlanProductionRequirePlanVo::getMaterialDesc,
                        Collectors.collectingAndThen(Collectors.toList(),
                                l -> l.stream().mapToInt(MonthPlanProductionRequirePlanVo::getHeightQty).sum())));
        return heightQtyMap;
    }

    /**
     * 根据分配信息，获取结构的收尾日信息
     *
     * @param context           排产上下文
     * @param groupPlanInfo     分组
     * @param allAllocationInfo 分组分配信息
     * @return
     */
    private Set<Integer> getGroupConclusionDayInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, List<CxMachineAllocationPlanHelper> allAllocationInfo) {
        if (null == groupPlanInfo || CollectionUtils.isEmpty(allAllocationInfo)) {
            return Collections.emptySet();
        }
        List<CxMachineAllocationPlanHelper> realAllocationList = allAllocationInfo.stream().filter(singleAllocation -> groupPlanInfo.equals(singleAllocation.getProductionPlanInfo())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(realAllocationList)) {
            return Collections.emptySet();
        }
        Set<Integer> groupConclusionDaySet = Sets.newHashSet();
        realAllocationList.forEach(singleAllocation -> {
            Integer realConclusionDay = singleAllocation.getRealConclusionDay(context);
            if (null != realConclusionDay) {
                groupConclusionDaySet.add(realConclusionDay);
            }
        });
        if (CollectionUtils.isEmpty(groupConclusionDaySet)) {
            return Collections.emptySet();
        }
        return groupConclusionDaySet;
    }

    /**
     * 获取需要强制收尾的日排产信息
     * 实单排产硫化机台数低于要求的最低硫化机台数
     *
     * @param groupPlanInfo          分组信息
     * @param dayProductionLimitInfo 日排产信息集合
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getMinLhMachineConclusionDayInfo(ProductionPlanGroupInfo groupPlanInfo, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取使用硫化机台数 = dayMinLhMachineCount的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> lowMinLhMachineList = dayLimitList.stream().filter(singleDay -> {
            Integer day = singleDay.getDay();
            MpDailyCapacityLimitVo dailyCapacityLimit = groupPlanInfo.getDailyCapacityLimitVoMap().get(day);
            Integer usedLhMachineCount = null == dailyCapacityLimit ? BigDecimal.ZERO.intValue() : Optional.ofNullable(dailyCapacityLimit.getUsedLhMachines()).orElse(BigDecimal.ZERO.intValue());
            return singleDay.getDayMinLhMachineCount().equals(usedLhMachineCount);
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinLhMachineList)) {
            return Collections.emptyList();
        }
        return lowMinLhMachineList;
    }

    /**
     * 获取有效的收尾日集合信息
     *
     * @param context                排产上下文
     * @param lowMinLhMachineDayList 需要收尾的日集合信息
     * @param allAllocationInfo      所有分配信息集合
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getForcedConclusionDayInfo(Context context, List<GroupPlanCxLhCapacityLimitHelper> lowMinLhMachineDayList, List<CxMachineAllocationPlanHelper> allAllocationInfo) {
        if (CollectionUtils.isEmpty(lowMinLhMachineDayList) || CollectionUtils.isEmpty(allAllocationInfo)) {
            return Collections.emptyList();
        }
        CxMachineAllocationPlanHelper earliest = allAllocationInfo.stream().max(Comparator.comparing(CxMachineAllocationPlanHelper::getReleasePriority)).orElse(null);
        if (null == earliest) {
            return Collections.emptyList();
        }
        Integer realConclusionDay = earliest.getRealConclusionDay(context);
        if (null == realConclusionDay) {
            return Collections.emptyList();
        }
        List<GroupPlanCxLhCapacityLimitHelper> realConclusionDayList = lowMinLhMachineDayList.stream().filter(singleDay -> singleDay.getDay() <= realConclusionDay).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(realConclusionDayList)) {
            return Collections.emptyList();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayMap = realConclusionDayList.stream().collect(Collectors.toMap(GroupPlanCxLhCapacityLimitHelper::getDay, Function.identity()));
        Integer deductionDay = realConclusionDay;
        List<GroupPlanCxLhCapacityLimitHelper> deductionList = Lists.newArrayList();
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(earliest.getCxMachineCode());
        Set<Integer> stopDays = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        for (; deductionDay >= earliest.getStartDay(); ) {
            if (stopDays.contains(deductionDay)) {
                deductionDay = deductionDay - BigDecimal.ONE.intValue();
                continue;
            }
            if (dayMap.containsKey(deductionDay)) {
                deductionList.add(dayMap.get(deductionDay));
                deductionDay = deductionDay - BigDecimal.ONE.intValue();
                continue;
            }
            break;
        }
        if (CollectionUtils.isEmpty(deductionList)) {
            return Collections.emptyList();
        }
        Set<Integer> replenishmentDay = context.getReplenishmentDay();
        Set<Integer> deductionDaySet = deductionList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
        if (deductionDaySet.equals(replenishmentDay)) {
            return Collections.emptyList();
        }
        return deductionList;
    }

    /**
     * 处理实单最低硫化机台数可持续的天数，超出需要在达到连续天数当天强制收尾
     *
     * @param context                    排产上下文
     * @param groupPlan                  分组计划对象
     * @param conclusionAllocationRange  收尾分配段
     * @param realLowMinLhMachineDayList 低于实单硫化机台数日排产集合
     */
    private void handlerContinueDayMinLhMachineDays(Context context, ProductionPlanGroupInfo groupPlan, CxMachineAllocationPlanHelper conclusionAllocationRange, List<GroupPlanCxLhCapacityLimitHelper> realLowMinLhMachineDayList) {
        if (null == conclusionAllocationRange) {
            return;
        }
        //最低硫化配比 >= 最大硫化配比，则跳过
        if (conclusionAllocationRange.getMinRatio() >= conclusionAllocationRange.getMaxRatio()) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer earliestLowMinLhMachineDay = getLowMinLhMachineConclusionDay(realLowMinLhMachineDayList);
        Integer endDay;
        if (null != earliestLowMinLhMachineDay) {
            endDay = context.getPreviousDay(earliestLowMinLhMachineDay);
        } else {
            endDay = conclusionAllocationRange.getEndDay();
        }
        List<GroupPlanCxLhCapacityLimitHelper> minLhMachineDayList = getMinLhMachineDayInfo(productionContext, groupPlan, endDay);
        if (CollectionUtils.isEmpty(minLhMachineDayList)) {
            return;
        }
        Integer continueDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinLhMachineContinueDays();
        Integer continueMaxDays = minLhMachineDayList.size();
        if (continueMaxDays <= continueDays) {
            return;
        }
        minLhMachineDayList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        List<GroupPlanCxLhCapacityLimitHelper> minLhMachineConclusionList = minLhMachineDayList.subList(continueDays, continueMaxDays);
        if (CollectionUtils.isEmpty(minLhMachineConclusionList)) {
            return;
        }
        realLowMinLhMachineDayList.addAll(minLhMachineConclusionList);
        return;
    }

    /**
     * 处理实单最低硫化机台数可持续的天数，超出需要在达到连续天数当天强制收尾
     *
     * @param context                    排产上下文
     * @param conclusionAllocationRange  收尾分配段
     * @param realLowMinLhMachineDayList 低于实单硫化机台数日排产集合
     * @param productionUsedLhInfoList   所有有排产日信息
     */
    private void handlerContinueDayMinLhMachineDays(Context context, CxMachineAllocationPlanHelper conclusionAllocationRange, List<CxMachineUsedLhInfo> realLowMinLhMachineDayList, List<CxMachineUsedLhInfo> productionUsedLhInfoList) {
        if (null == conclusionAllocationRange) {
            return;
        }
        //最低硫化配比 >= 最大硫化配比，则跳过
        Integer minLhMachineCount = conclusionAllocationRange.getMinRatio();
        if (null == minLhMachineCount || minLhMachineCount >= conclusionAllocationRange.getMaxRatio()) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        Integer earliestLowMinLhMachineDay = getLowMinLhMachineConclusionDayBySingleCxMachine(realLowMinLhMachineDayList);
        Integer endDay;
        if (null != earliestLowMinLhMachineDay) {
            endDay = context.getPreviousDay(earliestLowMinLhMachineDay);
        } else {
            endDay = conclusionAllocationRange.getEndDay();
        }
        List<CxMachineUsedLhInfo> minLhMachineDayList = getMinLhMachineDayInfo(productionContext, minLhMachineCount, endDay, productionUsedLhInfoList);
        if (CollectionUtils.isEmpty(minLhMachineDayList)) {
            return;
        }
        Integer continueDays = productionContext.getBaseDataContainer().getParamConfiguration().getMinLhMachineContinueDays();
        Integer continueMaxDays = minLhMachineDayList.size();
        if (continueMaxDays <= continueDays) {
            return;
        }
        minLhMachineDayList.sort(Comparator.comparing(CxMachineUsedLhInfo::getProductionDay));
        List<CxMachineUsedLhInfo> minLhMachineConclusionList = minLhMachineDayList.subList(continueDays, continueMaxDays);
        if (CollectionUtils.isEmpty(minLhMachineConclusionList)) {
            return;
        }
        realLowMinLhMachineDayList.addAll(minLhMachineConclusionList);
        return;
    }

    /**
     * 获取低于实单最低硫化机台数的最早有效日期
     *
     * @param realLowMinLhMachineDayList 一段连续低于最低实单硫化机台数排产日集合信息
     * @return
     */
    private Integer getLowMinLhMachineConclusionDay(List<GroupPlanCxLhCapacityLimitHelper> realLowMinLhMachineDayList) {
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            return null;
        }
        realLowMinLhMachineDayList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        return realLowMinLhMachineDayList.get(BigDecimal.ZERO.intValue()).getDay();
    }

    /**
     * 获取低于实单最低硫化机台数的最早有效日期
     *
     * @param realLowMinLhMachineDayList 一段连续低于最低实单硫化机台数排产日集合信息
     * @return
     */
    private Integer getLowMinLhMachineConclusionDayBySingleCxMachine(List<CxMachineUsedLhInfo> realLowMinLhMachineDayList) {
        if (CollectionUtils.isEmpty(realLowMinLhMachineDayList)) {
            return null;
        }
        realLowMinLhMachineDayList.sort(Comparator.comparing(CxMachineUsedLhInfo::getProductionDay));
        return realLowMinLhMachineDayList.get(BigDecimal.ZERO.intValue()).getProductionDay();
    }

    /**
     * 获取从endDay往前，连续排产都是实单最低硫化机台数的天数信息
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组信息
     * @param endDay        实单硫化机台数起始排产日
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getMinLhMachineDayInfo(Context context, ProductionPlanGroupInfo groupPlanInfo, Integer endDay) {
        if (null == groupPlanInfo || null == endDay) {
            return Collections.emptyList();
        }
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            return Collections.emptyList();
        }
        Integer minLhMachineCount = groupPlanInfo.getClosureMinLhRatio();
        if (null == minLhMachineCount) {
            return Collections.emptyList();
        }
        //获取使用硫化机台数 = 实单要求的最低硫化机台数天数集合
        List<GroupPlanCxLhCapacityLimitHelper> minLhMachineDayList = getMinLhMachineConclusionDayInfo(groupPlanInfo, dayProductionLimitInfo);
        if (CollectionUtils.isEmpty(minLhMachineDayList)) {
            return Collections.emptyList();
        }
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayLimitMap = minLhMachineDayList.stream().collect(Collectors.toMap(GroupPlanCxLhCapacityLimitHelper::getDay, Function.identity()));
        List<GroupPlanCxLhCapacityLimitHelper> effectiveList = new ArrayList<>();
        for (Integer day = endDay; day >= ProductionConstant.MONTH_START_DAY; ) {
            //跳过停工日
            if (context.getStopDays().contains(day)) {
                day = day - BigDecimal.ONE.intValue();
                continue;
            }
            //存在
            if (dayLimitMap.containsKey(day)) {
                effectiveList.add(dayLimitMap.get(day));
                day = day - BigDecimal.ONE.intValue();
                continue;
            }
            break;
        }
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        return effectiveList;
    }

    /**
     * 获取从endDay往前，连续排产都是实单最低硫化机台数的天数信息
     *
     * @param context                  排产上下文
     * @param minLhMachineCount        分组信息
     * @param endDay                   实单硫化机台数起始排产日
     * @param productionUsedLhInfoList 排产日集合
     * @return
     */
    private List<CxMachineUsedLhInfo> getMinLhMachineDayInfo(Context context, Integer minLhMachineCount, Integer endDay, List<CxMachineUsedLhInfo> productionUsedLhInfoList) {
        if (null == minLhMachineCount || CollectionUtils.isEmpty(productionUsedLhInfoList)) {
            return Collections.emptyList();
        }
        //获取使用硫化机台数低于实单要求的最低硫化机台数天数集合
        List<CxMachineUsedLhInfo> minLhMachineDayList = productionUsedLhInfoList.stream().filter(singleDay -> minLhMachineCount.equals(singleDay.getUsedLhMachineCount())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(minLhMachineDayList)) {
            return Collections.emptyList();
        }
        Map<Integer, CxMachineUsedLhInfo> dayLimitMap = minLhMachineDayList.stream().collect(Collectors.toMap(CxMachineUsedLhInfo::getProductionDay, Function.identity()));
        List<CxMachineUsedLhInfo> effectiveList = new ArrayList<>();
        for (Integer day = endDay; day >= ProductionConstant.MONTH_START_DAY; ) {
            //跳过停工日
            if (context.getStopDays().contains(day)) {
                day = day - BigDecimal.ONE.intValue();
                continue;
            }
            //存在
            if (dayLimitMap.containsKey(day)) {
                effectiveList.add(dayLimitMap.get(day));
                day = day - BigDecimal.ONE.intValue();
                continue;
            }
            break;
        }
        if (CollectionUtils.isEmpty(effectiveList)) {
            return Collections.emptyList();
        }
        return effectiveList;
    }

    /**
     * 获取一段连续需要强制收尾的时间
     *
     * @param context                  排产上下文
     * @param lowMinLhMachineCountList 低于硫化机台数的排产日集合
     * @param conclusionRange          分配段范围
     * @return
     */
    private List<CxMachineUsedLhInfo> getForcedConclusionDayInfo(Context context, List<CxMachineUsedLhInfo> lowMinLhMachineCountList, CxMachineAllocationPlanHelper conclusionRange) {
        if (CollectionUtils.isEmpty(lowMinLhMachineCountList) || null == conclusionRange) {
            return Collections.emptyList();
        }
        Integer realConclusionDay = conclusionRange.getRealConclusionDay(context);
        if (null == realConclusionDay) {
            return Collections.emptyList();
        }
        boolean forcedFlag = false;
        for (CxMachineUsedLhInfo lowMinLhMachineDay : lowMinLhMachineCountList) {
            Integer day = lowMinLhMachineDay.getProductionDay();
            if (realConclusionDay.equals(day)) {
                forcedFlag = true;
            }
        }
        if (!forcedFlag) {
            return Collections.emptyList();
        }
        List<CxMachineUsedLhInfo> realConclusionDayList = lowMinLhMachineCountList.stream().filter(singleDay -> singleDay.getProductionDay() <= realConclusionDay).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(realConclusionDayList)) {
            return Collections.emptyList();
        }
        Map<Integer, CxMachineUsedLhInfo> deductionDayMap = realConclusionDayList.stream().collect(Collectors.toMap(CxMachineUsedLhInfo::getProductionDay, Function.identity()));
        TbrProductionContext productionContext = (TbrProductionContext) context;
        CxMachineBaseInfoVo cxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo().get(conclusionRange.getCxMachineCode());
        Set<Integer> stopDays = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
        Integer deductionDay = realConclusionDay;
        List<CxMachineUsedLhInfo> deductionList = Lists.newArrayList();
        for (; deductionDay >= conclusionRange.getStartDay(); ) {
            if (stopDays.contains(deductionDay)) {
                deductionDay = deductionDay - BigDecimal.ONE.intValue();
                continue;
            }
            if (deductionDayMap.containsKey(deductionDay)) {
                deductionList.add(deductionDayMap.get(deductionDay));
                deductionDay = deductionDay - BigDecimal.ONE.intValue();
                continue;
            }
            break;
        }
        if (CollectionUtils.isEmpty(deductionList)) {
            return Collections.emptyList();
        }
        Set<Integer> replenishmentDay = context.getReplenishmentDay();
        Set<Integer> deductionDaySet = deductionList.stream().map(CxMachineUsedLhInfo::getProductionDay).collect(Collectors.toSet());
        if (deductionDaySet.equals(replenishmentDay)) {
            return Collections.emptyList();
        }
        return deductionList;
    }
}
