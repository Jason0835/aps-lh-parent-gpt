package com.zlt.aps.factory.basedataassemble.history;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 生产历史处理器
 *
 * @author ZLT
 * @date 20260128
 */
@Slf4j
@Component
public class ProductionHistoryHandler {

    /**
     * 创建以机台为维度的生产历史信息
     *
     * @param context        排产上下文
     * @param allocationList 所有排产信息
     * @return
     */
    public Map<String, CxMachineProductionHistoryInfo> buildCxMachineProductionHistory(Context context, List<MpStructureAllocation> allocationList) {
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptyMap();
        }
        Map<String, CxMachineProductionHistoryInfo> historyInfoMap = new HashMap<>(64);
        LocalDate latestOneMonth = context.getPreviousMonth();
        Map<String, List<MpStructureAllocation>> cxMachineGroup = allocationList.stream().collect(Collectors.groupingBy(MpStructureAllocation::getCxMachineCode));
        cxMachineGroup.forEach((cxMachineCode, allProductionInfo) -> {
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            CxMachineProductionHistoryInfo historyInfo = new CxMachineProductionHistoryInfo();
            historyInfo.setCxMachineCode(cxMachineCode);
            historyInfoMap.put(cxMachineCode, historyInfo);
            //近n个月的历史生产次数
            setGroupProductionCountInfo(historyInfo, allProductionInfo);
            List<MpStructureAllocation> latestOneMonthProductionInfo = allProductionInfo.stream().filter(single -> single.getYear().equals(latestOneMonth.getYear()) && single.getMonth().equals(latestOneMonth.getMonthValue())).collect(Collectors.toList());
            //近1个月的最近生产日
            setGroupProductionLatestDayInfo(historyInfo, latestOneMonthProductionInfo);
        });
        return historyInfoMap;
    }

    /**
     * 创建以分组为维度的生产历史
     *
     * @param context        排产上下文
     * @param allocationList 排产信息
     * @return
     */
    public Map<String, GroupPlanProductionHistoryInfo> buildGroupPlanProductionHistory(Context context, List<MpStructureAllocation> allocationList) {
        if (CollectionUtils.isEmpty(allocationList)) {
            return Collections.emptyMap();
        }
        Map<String, GroupPlanProductionHistoryInfo> groupPlanHistoryInfoMap = new HashMap<>(64);
        LocalDate latestOneMonth = context.getPreviousMonth();
        Map<String, List<MpStructureAllocation>> groupPlanGroup = allocationList.stream().collect(Collectors.groupingBy(MpStructureAllocation::getStructureName));
        groupPlanGroup.forEach((structureName, allProductionInfo) -> {
            if (CollectionUtils.isEmpty(allocationList)) {
                return;
            }
            GroupPlanProductionHistoryInfo historyInfo = new GroupPlanProductionHistoryInfo();
            historyInfo.setGroupName(structureName);
            groupPlanHistoryInfoMap.put(structureName, historyInfo);
            //近n个月的历史生产次数
            setGroupProductionCountInfo(historyInfo, allProductionInfo);
            List<MpStructureAllocation> latestOneMonthProductionInfo = allProductionInfo.stream().filter(single -> single.getYear().equals(latestOneMonth.getYear()) && single.getMonth().equals(latestOneMonth.getMonthValue())).collect(Collectors.toList());
            //近1个月的最近生产日
            setGroupProductionLatestDayInfo(historyInfo, latestOneMonthProductionInfo);
        });
        return groupPlanHistoryInfoMap;
    }

    /**
     * 设置各分组近n个月历史生产次数
     *
     * @param historyInfo                机台历史信息
     * @param cxMachineAllProductionInfo 机台近n个月所有生产历史
     */
    private void setGroupProductionCountInfo(CxMachineProductionHistoryInfo historyInfo, List<MpStructureAllocation> cxMachineAllProductionInfo) {
        if (CollectionUtils.isEmpty(cxMachineAllProductionInfo)) {
            historyInfo.setProductionCountInfo(Collections.emptyList());
            return;
        }
        String cxMachineCode = historyInfo.getCxMachineCode();
        Map<String, List<MpStructureAllocation>> groupPlanMap = cxMachineAllProductionInfo.stream().collect(Collectors.groupingBy(MpStructureAllocation::getStructureName));
        List<CxMachineProductionGroupInfo> allProductionGroupPlanList = new ArrayList<>(64);
        groupPlanMap.forEach((groupName, allGroupProductionInfo) -> {
            CxMachineProductionGroupInfo historyProductionCountInfo = buildHistoryProductionCountInfo(allGroupProductionInfo, groupName, cxMachineCode);
            allProductionGroupPlanList.add(historyProductionCountInfo);
        });
        historyInfo.setProductionCountInfo(allProductionGroupPlanList);
    }

    /**
     * 设置各分组近1个月最近生产日
     *
     * @param historyInfo                机台历史信息
     * @param cxMachineAllProductionInfo 机台近1个月所有生产历史
     */
    private void setGroupProductionLatestDayInfo(CxMachineProductionHistoryInfo historyInfo, List<MpStructureAllocation> cxMachineAllProductionInfo) {
        if (CollectionUtils.isEmpty(cxMachineAllProductionInfo)) {
            historyInfo.setLatestProductionInfo(Collections.emptyList());
            return;
        }
        String cxMachineCode = historyInfo.getCxMachineCode();
        Map<String, List<MpStructureAllocation>> groupPlanMap = cxMachineAllProductionInfo.stream().collect(Collectors.groupingBy(MpStructureAllocation::getStructureName));
        List<CxMachineLatestProductionInfo> allProductionGroupPlanList = new ArrayList<>(64);
        groupPlanMap.forEach((groupName, allGroupProductionInfo) -> {
            CxMachineLatestProductionInfo latestHistory = buildMaxProductionDayHistoryInfo(allGroupProductionInfo, groupName, cxMachineCode);
            allProductionGroupPlanList.add(latestHistory);
        });
        historyInfo.setLatestProductionInfo(allProductionGroupPlanList);
    }


    /**
     * 设置各分组近n个月历史生产次数
     *
     * @param historyInfo                机台历史信息
     * @param cxMachineAllProductionInfo 机台近n个月所有生产历史
     */
    private void setGroupProductionCountInfo(GroupPlanProductionHistoryInfo historyInfo, List<MpStructureAllocation> cxMachineAllProductionInfo) {
        if (CollectionUtils.isEmpty(cxMachineAllProductionInfo)) {
            historyInfo.setProductionCountInfo(Collections.emptyList());
            return;
        }
        String groupName = historyInfo.getGroupName();
        Map<String, List<MpStructureAllocation>> groupPlanMap = cxMachineAllProductionInfo.stream().collect(Collectors.groupingBy(MpStructureAllocation::getCxMachineCode));
        List<CxMachineProductionGroupInfo> allProductionGroupPlanList = new ArrayList<>(64);
        groupPlanMap.forEach((cxMachineCode, allGroupProductionInfo) -> {
            CxMachineProductionGroupInfo historyProductionCountInfo = buildHistoryProductionCountInfo(allGroupProductionInfo, groupName, cxMachineCode);
            allProductionGroupPlanList.add(historyProductionCountInfo);
        });
        historyInfo.setProductionCountInfo(allProductionGroupPlanList);
    }


    /**
     * 设置各分组近1个月最近生产日
     *
     * @param historyInfo                机台历史信息
     * @param cxMachineAllProductionInfo 机台近1个月所有生产历史
     */
    private void setGroupProductionLatestDayInfo(GroupPlanProductionHistoryInfo historyInfo, List<MpStructureAllocation> cxMachineAllProductionInfo) {
        if (CollectionUtils.isEmpty(cxMachineAllProductionInfo)) {
            historyInfo.setLatestProductionInfo(Collections.emptyList());
            return;
        }
        String groupName = historyInfo.getGroupName();
        Map<String, List<MpStructureAllocation>> groupPlanMap = cxMachineAllProductionInfo.stream().collect(Collectors.groupingBy(MpStructureAllocation::getCxMachineCode));
        List<CxMachineLatestProductionInfo> allProductionGroupPlanList = new ArrayList<>(64);
        groupPlanMap.forEach((cxMachineCode, allGroupProductionInfo) -> {
            CxMachineLatestProductionInfo latestHistory = buildMaxProductionDayHistoryInfo(allGroupProductionInfo, groupName, cxMachineCode);
            allProductionGroupPlanList.add(latestHistory);
        });
        historyInfo.setLatestProductionInfo(allProductionGroupPlanList);
    }

    /**
     * 创建近n个月的排产次数
     *
     * @param allGroupProductionInfo 近n个月的排产历史信息
     * @param groupName              分组信息
     * @param cxMachineCode          成型机台
     * @return
     */
    private CxMachineProductionGroupInfo buildHistoryProductionCountInfo(List<MpStructureAllocation> allGroupProductionInfo, String groupName, String cxMachineCode) {
        if (CollectionUtils.isEmpty(allGroupProductionInfo)) {
            return new CxMachineProductionGroupInfo(cxMachineCode, groupName, BigDecimal.ZERO.intValue());
        }
        String format = "%s|*|%s";
        Set<String> yearAndMonth = new HashSet<>();
        allGroupProductionInfo.forEach(single -> {
            String productionYearAndMonth = String.format(format, single.getYear(), single.getMonth());
            yearAndMonth.add(productionYearAndMonth);
        });
        return new CxMachineProductionGroupInfo(cxMachineCode, groupName, yearAndMonth.size());
    }

    /**
     * 创建最近1个月各机台排产的最近日期
     *
     * @param allGroupProductionInfo 所有排产信息
     * @param groupName              分组计划
     * @param cxMachineCode          机台
     * @return
     */
    private CxMachineLatestProductionInfo buildMaxProductionDayHistoryInfo(List<MpStructureAllocation> allGroupProductionInfo, String groupName, String cxMachineCode) {
        if (CollectionUtils.isEmpty(allGroupProductionInfo)) {
            return new CxMachineLatestProductionInfo(cxMachineCode, groupName, BigDecimal.ZERO.intValue());
        }
        List<MpStructureAllocation> effectiveList = allGroupProductionInfo.stream().filter(single -> single.getAllotDays() > BigDecimal.ZERO.intValue() && single.getEndDay() > BigDecimal.ZERO.intValue() && single.getEndDay() > single.getBeginDay()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(effectiveList)) {
            return new CxMachineLatestProductionInfo(cxMachineCode, groupName, BigDecimal.ZERO.intValue());
        }
        Set<Integer> productionDaySet = effectiveList.stream().map(MpStructureAllocation::getEndDay).collect(Collectors.toSet());
        List<Integer> productionDayList = new ArrayList<>(productionDaySet);
        productionDayList.sort(Comparator.reverseOrder());
        return new CxMachineLatestProductionInfo(cxMachineCode, groupName, productionDayList.get(BigDecimal.ZERO.intValue()));
    }

}
