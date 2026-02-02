package com.zlt.aps.factory.basedataassemble.history;

import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.factory.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.factory.scheduling.BaseDataContainer;
import com.zlt.aps.factory.scheduling.TbrProductionContext;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
     * 以成型机台维度，构建机台最近生产的分组计划信息
     * 设置groupInfo的近1个月的生产日及近n个月的生产次数
     *
     * @param context       排产上下文
     * @param groupInfo     即将要排产的分组
     * @param cxMachineInfo 排产的机台
     */
    public void setCxMachineProductionGroupPlanHistory(Context context, ProductionPlanGroupInfo groupInfo, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == context || null == cxMachineInfo || null == groupInfo) {
            return;
        }
        String groupName = groupInfo.getGroupName();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        CxMachineProductionHistoryInfo machineProductionHistoryInfo = baseDataContainer.getCxMachineProductionHistoryInfo().get(cxMachineInfo.getCxMachineCode());
        if (null == machineProductionHistoryInfo) {
            return;
        }
        CxMachineLatestProductionInfo latestGroupProductionInfo = machineProductionHistoryInfo.getProductionGroupPlanLatestHistory().get(groupName);
        if (null != latestGroupProductionInfo) {
            groupInfo.setLastBoardingDate(latestGroupProductionInfo.getProductionDay());
        }
        CxMachineProductionGroupInfo groupProductionInfo = machineProductionHistoryInfo.getProductionGroupPlanHistory().get(groupName);
        if (null != groupProductionInfo) {
            groupInfo.setProductionCount(groupProductionInfo.getProductionCount());
        }
    }

    /**
     * 以分组计划为维度，构建分组计划最近生产的机台信息
     * 设置cxMachineInfo的近1个月的生产日及近n个月的生产次数
     *
     * @param context       排产上下文
     * @param groupInfo     排产的分组
     * @param cxMachineInfo 即将要排产的机台
     */
    public void setGroupPlanProductionCxMachineHistory(Context context, ProductionPlanGroupInfo groupInfo, CxMachineBaseInfoVo cxMachineInfo) {
        if (null == context || null == cxMachineInfo || null == groupInfo) {
            return;
        }
        String groupName = groupInfo.getGroupName();
        String cxMachineCode = cxMachineInfo.getCxMachineCode();
        if (StringUtils.isBlank(groupName) || StringUtils.isBlank(cxMachineCode)) {
            return;
        }
        TbrProductionContext productionContext = (TbrProductionContext) context;
        BaseDataContainer baseDataContainer = productionContext.getBaseDataContainer();
        GroupPlanProductionHistoryInfo groupPlanProductionHistoryInfo = baseDataContainer.getGroupPlanHistoryInfoMap().get(cxMachineCode);
        if (null == groupPlanProductionHistoryInfo) {
            return;
        }
        CxMachineLatestProductionInfo latestGroupProductionInfo = groupPlanProductionHistoryInfo.getProductionCxMachineLatestHistory().get(cxMachineCode);
        if (null != latestGroupProductionInfo) {
            cxMachineInfo.setLastBoardingDate(latestGroupProductionInfo.getProductionDay());
        }
        CxMachineProductionGroupInfo groupProductionInfo = groupPlanProductionHistoryInfo.getProductionCxMachineHistory().get(cxMachineCode);
        if (null != groupProductionInfo) {
            cxMachineInfo.setProductionCount(groupProductionInfo.getProductionCount());
        }
    }

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
