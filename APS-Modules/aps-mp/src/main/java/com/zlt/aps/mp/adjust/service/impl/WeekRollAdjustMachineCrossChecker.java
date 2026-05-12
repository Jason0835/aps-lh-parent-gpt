package com.zlt.aps.mp.adjust.service.impl;

import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.common.utils.PubUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 周滚动调整单台机台结构时间交叉校验工具。
 */
class WeekRollAdjustMachineCrossChecker {

    private static final String SPLIT_COMMA = ",";
    private static final String SPLIT_KEY = "|";

    private WeekRollAdjustMachineCrossChecker() {
    }

    /**
     * 过滤包含指定单台机台的调整结果。
     *
     * @param resultList 调整结果集合
     * @param scheduledMachine 单台机台编码
     * @return 单台机台调整结果集合
     */
    static List<MpAdjustResult> filterAdjustResultByMachine(List<MpAdjustResult> resultList, String scheduledMachine) {
        List<MpAdjustResult> filteredList = new ArrayList<>();
        if (PubUtil.isEmpty(resultList) || StringUtils.isBlank(scheduledMachine)) {
            return filteredList;
        }
        for (MpAdjustResult result : resultList) {
            if (containsMachine(result.getCxMachineCode(), scheduledMachine)) {
                filteredList.add(result);
            }
        }
        return filteredList;
    }

    /**
     * 判断指定单台机台是否存在不同结构时间交叉。
     *
     * @param allocationList 结构转产集合
     * @param scheduledMachine 单台机台编码
     * @return true 存在交叉，false 不存在交叉
     */
    static boolean hasDifferentStructureCross(List<MpStructureAllocation> allocationList, String scheduledMachine) {
        List<MpStructureAllocation> machineAllocationList = filterAllocationByMachine(allocationList, scheduledMachine);
        List<MpStructureAllocation> distinctResultList = distinctByStructureAndDate(machineAllocationList);
        for (int i = 0; i < distinctResultList.size(); i++) {
            MpStructureAllocation current = distinctResultList.get(i);
            for (int j = i + 1; j < distinctResultList.size(); j++) {
                MpStructureAllocation next = distinctResultList.get(j);
                if (!StringUtils.equals(current.getStructureName(), next.getStructureName()) && isTimeCrossed(current, next)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断目标结构转产记录是否与同机台其它结构时间交叉。
     *
     * @param targetAllocation 目标结构转产记录
     * @param allocationList 已有结构转产记录
     * @return true 存在交叉，false 不存在交叉
     */
    static boolean hasTargetDifferentStructureCross(MpStructureAllocation targetAllocation, List<MpStructureAllocation> allocationList) {
        if (targetAllocation == null || PubUtil.isEmpty(allocationList)
                || StringUtils.isBlank(targetAllocation.getCxMachineCode())
                || StringUtils.isBlank(targetAllocation.getStructureName())
                || targetAllocation.getBeginDay() == null || targetAllocation.getEndDay() == null
                || targetAllocation.getBeginDay() > targetAllocation.getEndDay()) {
            return false;
        }
        for (MpStructureAllocation allocation : allocationList) {
            if (allocation == null || Objects.equals(targetAllocation.getId(), allocation.getId())
                    || StringUtils.equals(targetAllocation.getStructureName(), allocation.getStructureName())
                    || StringUtils.isBlank(allocation.getStructureName())
                    || allocation.getBeginDay() == null || allocation.getEndDay() == null
                    || allocation.getBeginDay() > allocation.getEndDay()) {
                continue;
            }
            if (containsMachine(allocation.getCxMachineCode(), targetAllocation.getCxMachineCode())
                    && isTimeCrossed(targetAllocation, allocation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找同机台同结构的结构转产记录。
     *
     * @param allocationList 已有结构转产记录
     * @param targetAllocation 目标结构转产记录
     * @return 已有结构转产记录
     */
    static MpStructureAllocation findSameMachineStructure(List<MpStructureAllocation> allocationList, MpStructureAllocation targetAllocation) {
        if (targetAllocation == null || PubUtil.isEmpty(allocationList)
                || StringUtils.isBlank(targetAllocation.getCxMachineCode())
                || StringUtils.isBlank(targetAllocation.getStructureName())) {
            return null;
        }
        for (MpStructureAllocation allocation : allocationList) {
            if (allocation != null
                    && StringUtils.equals(targetAllocation.getStructureName(), allocation.getStructureName())
                    && containsMachine(allocation.getCxMachineCode(), targetAllocation.getCxMachineCode())) {
                return allocation;
            }
        }
        return null;
    }

    /**
     * 判断新调整的日期是否与该单台机台下的其他结构日期交叉。
     *
     * @param allocationList 结构转产集合
     * @param scheduledMachine 单台机台编码
     * @param currentStructureName 当前正在调整的结构名称
     * @param newBeginDay 调整后的新开始日期
     * @param newEndDay 调整后的新结束日期
     * @return true 存在交叉，false 不存在交叉
     */
    static boolean hasCrossWithNewDate(List<MpStructureAllocation> allocationList, String scheduledMachine,
                                        String currentStructureName, Integer newBeginDay, Integer newEndDay) {
        if (PubUtil.isEmpty(allocationList) || StringUtils.isBlank(scheduledMachine)
                || newBeginDay == null || newEndDay == null || newBeginDay > newEndDay) {
            return false;
        }
        List<MpStructureAllocation> machineAllocationList = filterAllocationByMachine(allocationList, scheduledMachine);
        for (MpStructureAllocation allocation : machineAllocationList) {
            // 排除当前正在调整的结构
            if (StringUtils.equals(allocation.getStructureName(), currentStructureName)) {
                continue;
            }
            // 检查日期是否交叉
            if (allocation.getBeginDay() != null && allocation.getEndDay() != null) {
                boolean isCross = !(newEndDay < allocation.getBeginDay() || allocation.getEndDay() < newBeginDay);
                if (isCross) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 过滤包含指定单台机台的结构转产记录。
     *
     * @param allocationList 结构转产集合
     * @param scheduledMachine 单台机台编码
     * @return 单台机台结构转产集合
     */
    private static List<MpStructureAllocation> filterAllocationByMachine(List<MpStructureAllocation> allocationList, String scheduledMachine) {
        List<MpStructureAllocation> filteredList = new ArrayList<>();
        if (PubUtil.isEmpty(allocationList) || StringUtils.isBlank(scheduledMachine)) {
            return filteredList;
        }
        for (MpStructureAllocation allocation : allocationList) {
            if (containsMachine(allocation.getCxMachineCode(), scheduledMachine)) {
                filteredList.add(allocation);
            }
        }
        return filteredList;
    }

    /**
     * 判断逗号分隔机台中是否包含指定单台机台。
     *
     * @param machineCodes 调整结果机台编码，可能为多个机台逗号分隔
     * @param scheduledMachine 单台机台编码
     * @return true 包含，false 不包含
     */
    private static boolean containsMachine(String machineCodes, String scheduledMachine) {
        if (StringUtils.isBlank(machineCodes) || StringUtils.isBlank(scheduledMachine)) {
            return false;
        }
        String targetMachine = scheduledMachine.trim();
        String[] machineCodeArray = machineCodes.split(SPLIT_COMMA);
        for (String machineCode : machineCodeArray) {
            if (targetMachine.equals(machineCode.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按结构、开始日、结束日去重，避免同结构多 SKU 重复记录互相误判。
     *
     * @param resultList 调整结果集合
     * @return 去重后的调整结果集合
     */
    private static List<MpStructureAllocation> distinctByStructureAndDate(List<MpStructureAllocation> allocationList) {
        Map<String, MpStructureAllocation> distinctMap = new LinkedHashMap<>();
        if (PubUtil.isEmpty(allocationList)) {
            return new ArrayList<>();
        }
        for (MpStructureAllocation allocation : allocationList) {
            if (StringUtils.isBlank(allocation.getStructureName()) || allocation.getBeginDay() == null || allocation.getEndDay() == null
                    || allocation.getBeginDay() > allocation.getEndDay()) {
                continue;
            }
            String key = allocation.getStructureName() + SPLIT_KEY + allocation.getBeginDay() + SPLIT_KEY + allocation.getEndDay();
            distinctMap.putIfAbsent(key, allocation);
        }
        return new ArrayList<>(distinctMap.values());
    }

    /**
     * 判断两个调整结果的开始日、结束日是否交叉。
     *
     * @param current 当前调整结果
     * @param next 下一个调整结果
     * @return true 交叉，false 不交叉
     */
    private static boolean isTimeCrossed(MpStructureAllocation current, MpStructureAllocation next) {
        int currentBegin = current.getBeginDay();
        int currentEnd = current.getEndDay();
        int nextBegin = next.getBeginDay();
        int nextEnd = next.getEndDay();
        return !(currentEnd < nextBegin || nextEnd < currentBegin);
    }
}
