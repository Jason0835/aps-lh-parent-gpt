package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 分组排产分配辅助类
 * 业务工具类
 *
 * @author ZLT
 * @date 20260722
 */
public class GroupProductionAllocationHelper {

    /**
     * @param productionContext 排产上下文
     * @param preSelectGroup    预分配的分组信息对象(TBR 结构)
     * @return
     */
    public static Set<CxMachineAllocationPlanHelper> getAllAssignedInfoByGroup(TbrProductionContext productionContext, ProductionPlanGroupInfo preSelectGroup) {
        if (null == preSelectGroup) {
            return Collections.emptySet();
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return Collections.emptySet();
        }
        Set<CxMachineAllocationPlanHelper> assignedInfo = Sets.newHashSet();
        allCxMachineInfo.forEach((cxMachineCode, cxMachineInfo) -> {
            List<CxMachineAllocationPlanHelper> assignedList = cxMachineInfo.getAllocationList();
            if (CollectionUtils.isEmpty(assignedList)) {
                return;
            }
            assignedList.forEach(assignedAllocationInfo -> {
                if (assignedAllocationInfo.getProductionPlanInfo().equals(preSelectGroup)) {
                    assignedInfo.add(assignedAllocationInfo);
                }
            });
        });
        if (CollectionUtils.isEmpty(assignedInfo)) {
            return Collections.emptySet();
        }
        return assignedInfo;
    }

    /**
     * 判断是否间断排产
     *
     * @param productionContext    排产上下文
     * @param preSelectGroup       预排分组信息对象
     * @param allPreAllocationInfo 所有分配信息(已分配+预分配)
     * @return
     */
    public static boolean isDiscontinueProduction(TbrProductionContext productionContext, ProductionPlanGroupInfo preSelectGroup, Set<CxMachineAllocationPlanHelper> allPreAllocationInfo) {
        if (null == preSelectGroup || CollectionUtils.isEmpty(allPreAllocationInfo)) {
            return false;
        }
        Set<CxMachineAllocationPlanHelper> noWholeCxMachineAllocationInfo = extractWholeCxMachine(productionContext, allPreAllocationInfo);
        if (CollectionUtils.isEmpty(noWholeCxMachineAllocationInfo)) {
            return false;
        }
        if (isMultipleRange(productionContext, noWholeCxMachineAllocationInfo)) {
            return true;
        }
        Set<Integer> preProductionDaySet = Sets.newHashSet();
        Set<Integer> stopDaySet = Sets.newHashSet();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        setProductionAndStopDayInfo(productionContext, preProductionDaySet, stopDaySet, noWholeCxMachineAllocationInfo, allCxMachineInfo);
        if (CollectionUtils.isEmpty(preProductionDaySet)) {
            return false;
        }
        String groupName = preSelectGroup.getGroupName();
        GroupPreAllocationInfoHelper discontinueInfo = new GroupPreAllocationInfoHelper(groupName, preSelectGroup, preProductionDaySet, stopDaySet);
        return discontinueInfo.hasDiscontinueProduction();
    }

    /**
     * 获取分组的所有可排产日以及各机台的停产日
     * 将排产日信息加入到preProductionDaySet集合中
     * 将机台的停产日加入到stopDaySet集合中
     *
     * @param productionContext   排产上下文
     * @param preProductionDaySet 需要加入的排产日集合，初始为空集合
     * @param stopDaySet          需要加入的停产日集合，初始为空集合
     * @param preAllocationInfo   预分配信息
     * @param allCxMachineInfo    所有机台信息
     */
    public static void setProductionAndStopDayInfo(TbrProductionContext productionContext, Set<Integer> preProductionDaySet, Set<Integer> stopDaySet, Set<CxMachineAllocationPlanHelper> preAllocationInfo, Map<String, CxMachineBaseInfoVo> allCxMachineInfo) {
        if (CollectionUtils.isEmpty(allCxMachineInfo) || null == preProductionDaySet || null == stopDaySet) {
            return;
        }
        if (CollectionUtils.isEmpty(preAllocationInfo) || preAllocationInfo.size() <= BigDecimal.ONE.intValue()) {
            return;
        }
        //设置全局停产日
        if (!CollectionUtils.isEmpty(productionContext.getStopDays())) {
            stopDaySet.addAll(productionContext.getStopDays());
        }
        //迭代 每个分配信息
        preAllocationInfo.forEach(singlePreAllocation -> {
            //20260728+ 剔除整台分配的
            if (isWholeCxMachineProduction(singlePreAllocation, allCxMachineInfo)) {
                return;
            }
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(singlePreAllocation.getCxMachineCode());
            Set<Integer> singleStopDaySet = Optional.ofNullable(cxMachineInfo.getStopDayInfo()).orElse(Collections.emptySet());
            Integer startDay = singlePreAllocation.getStartDay();
            Integer endDay = singlePreAllocation.getEndDay();
            for (int index = startDay; index <= endDay; index++) {
                if (singleStopDaySet.contains(index)) {
                    stopDaySet.add(index);
                    continue;
                }
                preProductionDaySet.add(index);
            }
        });
    }

    /**
     * 剔除掉整台排产成型机台的分配信息
     *
     * @param productionContext    排产上下文
     * @param allPreAllocationInfo 所有预分配信息
     * @return
     */
    private static Set<CxMachineAllocationPlanHelper> extractWholeCxMachine(TbrProductionContext productionContext, Set<CxMachineAllocationPlanHelper> allPreAllocationInfo) {
        if (CollectionUtils.isEmpty(allPreAllocationInfo)) {
            return Collections.emptySet();
        }
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        if (CollectionUtils.isEmpty(allCxMachineInfo)) {
            return Collections.emptySet();
        }
        Set<CxMachineAllocationPlanHelper> noWholeCxMachineSet = Sets.newHashSet();
        allPreAllocationInfo.forEach(singleAllocation -> {
            if (isWholeCxMachineProduction(singleAllocation, allCxMachineInfo)) {
                return;
            }
            noWholeCxMachineSet.add(singleAllocation);
        });
        if (CollectionUtils.isEmpty(noWholeCxMachineSet)) {
            return Collections.emptySet();
        }
        return noWholeCxMachineSet;
    }

    /**
     * 是否多段排产
     *
     * @param productionContext              排产上下文
     * @param noWholeCxMachineAllocationInfo 剔除整台分配的分配段信息
     * @return
     */
    private static boolean isMultipleRange(TbrProductionContext productionContext, Set<CxMachineAllocationPlanHelper> noWholeCxMachineAllocationInfo) {
        if (CollectionUtils.isEmpty(noWholeCxMachineAllocationInfo)) {
            return false;
        }
        List<CxMachineAllocationPlanHelper> noWholeCxMachineAllocationList = Lists.newLinkedList(noWholeCxMachineAllocationInfo);
        List<CxMachineAllocationPlanHelper> noEndAllocationList = getAllocationByEarliestEndMonth(productionContext, noWholeCxMachineAllocationList);
        if (noEndAllocationList.size() == BigDecimal.ONE.intValue()) {
            return false;
        }
        return true;
    }

    /**
     * 是否整台分配
     * 分配天数与机台理论生产天数一样
     *
     * @param allocationRange  分配段
     * @param allCxMachineInfo 所有成型机台信息
     * @return
     */
    private static boolean isWholeCxMachineProduction(CxMachineAllocationPlanHelper allocationRange, Map<String, CxMachineBaseInfoVo> allCxMachineInfo) {
        if (CollectionUtils.isEmpty(allCxMachineInfo) || null == allocationRange) {
            return true;
        }
        String cxMachineCode = allocationRange.getCxMachineCode();
        if (StringUtils.isBlank(cxMachineCode)) {
            return true;
        }
        CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(cxMachineCode);
        if (null == cxMachineInfo) {
            return true;
        }
        Integer allocationDays = allocationRange.getAllocationDay();
        if (allocationDays <= BigDecimal.ZERO.intValue()) {
            return true;
        }
        Integer maxProductionDays = BigDecimal.ZERO.intValue();
        Set<Integer> theoryProductionDays = cxMachineInfo.getTheoryProductionDaySet();
        if (!CollectionUtils.isEmpty(theoryProductionDays)) {
            maxProductionDays = theoryProductionDays.size();
        }
        if (allocationDays >= maxProductionDays) {
            return true;
        }
        return false;
    }

    /**
     * 获取剩余分配信息：剔除最早排产日的分配段结束日 = 月份最后一天
     *
     * @param productionContext              排产上下文
     * @param noWholeCxMachineAllocationList 非整台分配段信息
     * @return
     */
    private static List<CxMachineAllocationPlanHelper> getAllocationByEarliestEndMonth(TbrProductionContext productionContext, List<CxMachineAllocationPlanHelper> noWholeCxMachineAllocationList) {
        if (CollectionUtils.isEmpty(noWholeCxMachineAllocationList)) {
            return Collections.emptyList();
        }
        int size = noWholeCxMachineAllocationList.size();
        if (size == BigDecimal.ONE.intValue()) {
            return noWholeCxMachineAllocationList;
        }
        //从最早的分配日开始
        noWholeCxMachineAllocationList.sort(Comparator.comparing(CxMachineAllocationPlanHelper::getStartDay));
        CxMachineAllocationPlanHelper earliest = noWholeCxMachineAllocationList.get(BigDecimal.ZERO.intValue());
        Integer earliestEndDay = earliest.getEndDay();
        if (productionContext.isProductionEndDay(earliestEndDay)) {
            List<CxMachineAllocationPlanHelper> newList = noWholeCxMachineAllocationList.subList(BigDecimal.ONE.intValue(), size);
            return getAllocationByEarliestEndMonth(productionContext, newList);
        }
        return noWholeCxMachineAllocationList;
    }


    private GroupProductionAllocationHelper() {
    }
}
