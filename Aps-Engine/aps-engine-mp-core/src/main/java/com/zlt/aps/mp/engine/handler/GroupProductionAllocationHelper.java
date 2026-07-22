package com.zlt.aps.mp.engine.handler;

import com.google.common.collect.Sets;
import com.zlt.aps.mp.engine.domain.dto.CxMachineAllocationPlanHelper;
import com.zlt.aps.mp.engine.domain.dto.ProductionPlanGroupInfo;
import com.zlt.aps.mp.engine.domain.vo.CxMachineBaseInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
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
        Set<Integer> preProductionDaySet = Sets.newHashSet();
        Set<Integer> stopDaySet = Sets.newHashSet();
        Map<String, CxMachineBaseInfoVo> allCxMachineInfo = productionContext.getBaseDataContainer().getCxMachineBaseInfo();
        setProductionAndStopDayInfo(productionContext, preProductionDaySet, stopDaySet, allPreAllocationInfo, allCxMachineInfo);
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
            CxMachineBaseInfoVo cxMachineInfo = allCxMachineInfo.get(singlePreAllocation.getCxMachineCode());
            if (null == cxMachineInfo) {
                return;
            }
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

    private GroupProductionAllocationHelper() {
    }
}
