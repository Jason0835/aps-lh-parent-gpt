package com.zlt.aps.mp.engine.handler;

import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.aps.mp.engine.daylimit.GroupPlanCxLhCapacityLimitHelper;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.domain.dto.CxLhProductionHelper;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineUsedLhInfo;
import com.zlt.aps.mp.engine.domain.vo.GroupConclusionInfoVo;
import com.zlt.aps.mp.engine.domain.vo.MonthPlanStructureLhRatioVo;
import com.zlt.aps.mp.engine.logrecorder.GroupPlanConclusionLogRecorder;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
     * @param context
     * @param groupPlanInfo
     * @return conclusionDay ：不再排产，即在此前一日为分组收尾日
     * deductionDaySet 需要清除的排产日信息
     */
    public GroupConclusionInfoVo getConclusionInfoByProductionInfo(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return null;
        }
        String groupName = groupPlanInfo.getGroupName();
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        String cxMachineCodeInfo = "";
        if (!CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            cxMachineCodeInfo = allCxMachineCodeSet.stream().collect(Collectors.joining(StringConstant.COMMA));
        }
        GroupPlanConclusionLogRecorder.addGroupStartConclusionLog(context, groupName, cxMachineCodeInfo);
        //获取当前模拟排产的数据
        Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo = groupPlanInfo.getDayProductionLimitInfo();
        if (CollectionUtils.isEmpty(dayProductionLimitInfo) || CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoAllocationInfoLog(context, groupName);
            return null;
        }
        Integer minLhMachineCount = groupPlanInfo.getClosureMinLhRatio();
        if (null == minLhMachineCount) {
            GroupPlanConclusionLogRecorder.addNoLhRatioInfoLog(context, groupName);
            return null;
        }
        //获取使用硫化机台数低于实单要求的最低硫化机台数天数集合
        List<GroupPlanCxLhCapacityLimitHelper> lowMinMouldNumberList = getForcedConclusionDayInfo(groupPlanInfo, dayProductionLimitInfo);
        if (CollectionUtils.isEmpty(lowMinMouldNumberList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return GroupConclusionInfoVo.buildNoConclusionInfo(minLhMachineCount);
        }
        //按日期排序
        lowMinMouldNumberList.sort(Comparator.comparing(GroupPlanCxLhCapacityLimitHelper::getDay));
        //取得最小和最大日期
        Integer conclusionDay = lowMinMouldNumberList.get(BigDecimal.ZERO.intValue()).getDay();
        Set<Integer> deductionDaySet = lowMinMouldNumberList.stream().map(GroupPlanCxLhCapacityLimitHelper::getDay).collect(Collectors.toSet());
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
        List<CxMachineUsedLhInfo> lowMinLhMachineCountList = productionUsedLhInfoList.stream().filter(single -> single.getUsedLhMachineCount() < minLhMachineCount).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinLhMachineCountList)) {
            //记录日志
            GroupPlanConclusionLogRecorder.addNoConclusionInfoLog(context, groupName, minLhMachineCount);
            return GroupConclusionInfoVo.buildNoConclusionInfo(minLhMachineCount);
        }
        //按日期排序
        lowMinLhMachineCountList.sort(Comparator.comparing(CxMachineUsedLhInfo::getProductionDay));
        //取得最小日期
        Integer conclusionDay = lowMinLhMachineCountList.get(BigDecimal.ZERO.intValue()).getProductionDay();
        Integer deductionDay = lowMinLhMachineCountList.size();
        Set<Integer> deductionDaySet = lowMinLhMachineCountList.stream().map(CxMachineUsedLhInfo::getProductionDay).collect(Collectors.toSet());
        GroupConclusionInfoVo groupConclusionInfo = new GroupConclusionInfoVo(minLhMachineCount, conclusionDay, deductionDaySet);
        groupConclusionInfo.addSelectedConclusionCxMachine(cxMachineInfo);
        groupConclusionInfo.updateDeductionDaysByCxMachine(deductionDay);
        return groupConclusionInfo;
    }

    /**
     * 从分组计划分配的成型机台中，获取提前收尾，配比大的机台
     * 场景：在机机构在产机台的排产，故而此时机台都是只有一个结构分配
     *
     * @param context       排产上下文
     * @param groupPlanInfo 分组
     * @return
     */
    public CxMachineBaseInfoVo getConclusionCxMachine(Context context, ProductionPlanGroupInfo groupPlanInfo) {
        if (null == groupPlanInfo) {
            return null;
        }
        Set<String> allCxMachineCodeSet = groupPlanInfo.getAllocationCxMachineCodeSet();
        if (CollectionUtils.isEmpty(allCxMachineCodeSet)) {
            return null;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        List<MonthPlanStructureLhRatioVo> effectiveRatioList = new ArrayList<>();
        Map<MonthPlanStructureLhRatioVo, Set<String>> effectiveRationMap = new HashMap<>();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        allCxMachineCodeSet.forEach(cxMachineCode -> {
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
            if (null == cxMachineInfo) {
                return;
            }
            MonthPlanStructureLhRatioVo findLhRatio = groupPlanInfo.getLhRatio(cxMachineInfo);
            if (null == findLhRatio) {
                return;
            }
            Set<String> cxMachineCodeSet = effectiveRationMap.get(findLhRatio);
            if (null == cxMachineCodeSet) {
                cxMachineCodeSet = new HashSet<>();
                effectiveRationMap.put(findLhRatio, cxMachineCodeSet);
            }
            cxMachineCodeSet.add(cxMachineCode);
            effectiveRatioList.add(findLhRatio);
        });
        if (CollectionUtils.isEmpty(effectiveRatioList)) {
            return null;
        }
        effectiveRatioList.sort(Comparator.comparing(MonthPlanStructureLhRatioVo::getLhMachineMaxQty));
        MonthPlanStructureLhRatioVo selectedLhRatio = effectiveRatioList.get(BigDecimal.ZERO.intValue());
        List<String> selectedCxMachineList = new ArrayList<>(effectiveRationMap.get(selectedLhRatio));
        Collections.sort(selectedCxMachineList);
        String selectedCxMachineCode = selectedCxMachineList.get(BigDecimal.ZERO.intValue());
        return allCxMachineInfo.get(selectedCxMachineCode);
    }

    /**
     * 从分组计划分配的成型机台中，获取提前收尾，配比大的机台
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
     * 获取需要强制收尾的日排产信息
     * 实单排产硫化机台数低于要求的minLhMachineCount最低硫化机台数
     *
     * @param minLhMachineCount      最低硫化配比
     * @param dayProductionLimitInfo 日排产信息集合
     * @return
     */
    private List<GroupPlanCxLhCapacityLimitHelper> getForcedConclusionDayInfo(Integer minLhMachineCount, Map<Integer, GroupPlanCxLhCapacityLimitHelper> dayProductionLimitInfo) {
        //转化成模具数
        Integer minMouldNumber = minLhMachineCount * ProductionConstant.DOUBLE_MOULD_PRODUCTION;
        List<GroupPlanCxLhCapacityLimitHelper> dayLimitList = dayProductionLimitInfo.values().stream().collect(Collectors.toList());
        //获取使用模具数低于minMouldNumber的天数数据
        List<GroupPlanCxLhCapacityLimitHelper> lowMinMouldNumberList = dayLimitList.stream().filter(singleDay -> singleDay.isLowMinMouldNumber(minMouldNumber)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(lowMinMouldNumberList)) {
            return Collections.emptyList();
        }
        return lowMinMouldNumberList;
    }
}
