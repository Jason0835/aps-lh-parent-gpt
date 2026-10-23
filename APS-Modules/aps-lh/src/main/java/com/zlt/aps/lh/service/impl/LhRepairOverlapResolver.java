package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanResult;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 计划性维修组合解析，统一真实切换重叠的容量投影和正式精度拒绝边界。
 * 故障不进入组合编排，清洗及未定义多事项仍沿用原占用并集和调用顺序。
 *
 * @author APS
 * @date 2026/10/21
 */
public final class LhRepairOverlapResolver {

    /**
     * 禁止创建无状态组合解析器实例。
     */
    private LhRepairOverlapResolver() {
    }

    /**
     * 一次计算维修命中及生产下限，零预热时也保留独立命中标志。
     * 真实重叠时切换总时长覆盖预热和首检，后续独立维修仍保留自身恢复时间。
     *
     * @param context 当前批次，可为空
     * @param plans 实际停机列表
     * @param machineCode 当前运行侧
     * @param previousProductionEndTime 前序生产结束时间
     * @param switchStartTime 当前切换开始时间
     * @param switchEndTime 当前切换自身完成时间
     * @return 不修改固定事实或生产资源的独立维修结果
     */
    public static RepairPlanResult resolveSwitch(LhScheduleContext context, List<MdmDevicePlanShut> plans,
            String machineCode, Date previousProductionEndTime, Date switchStartTime, Date switchEndTime) {
        RepairPlanResult result = new RepairPlanResult();
        result.setMachineCode(machineCode);
        result.setPhysicalMachineCode(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
        result.setProductionNotBefore(RepairPlanRuntimeState.copyDate(switchEndTime));
        List<MachineMaintenanceWindowDTO> windows =
                LhPlannedRepairResolver.resolveCapacityWindows(context, plans, machineCode);
        result.setPlannedRepairItems(windows);
        List<MachineMaintenanceWindowDTO> capacityWindows =
                adaptCapacityWindowsForSwitch(windows, switchStartTime, switchEndTime);
        result.getCapacityWindows().addAll(capacityWindows);
        if (context != null) {
            result.setInputVersion(context.getRepairPlanRuntimeState().getConstraintVersion());
        }
        if (StringUtils.isEmpty(machineCode) || switchStartTime == null || switchEndTime == null) {
            return result;
        }
        Date productionReadyTime = switchEndTime;
        boolean repairTimelineStarted = false;
        for (MachineMaintenanceWindowDTO window : capacityWindows) {
            Date repairStartTime = window.getMaintenanceStartTime();
            Date repairResumeTime = window.getProductionResumeTime();
            // 前序生产已越过该恢复点时，不再让已完成维修影响后续切换。
            if (previousProductionEndTime != null && !previousProductionEndTime.before(repairResumeTime)) {
                continue;
            }
            if (!repairTimelineStarted) {
                if (!switchStartTime.before(repairResumeTime)) {
                    continue;
                }
                if (switchEndTime.before(repairStartTime)) {
                    break;
                }
                repairTimelineStarted = true;
            } else if (repairStartTime.after(productionReadyTime)) {
                // 尚未进入当前恢复区间的未来维修，继续交给班次容量处理。
                break;
            }
            // 真实重叠使用维修原始结束与切换完成的最大值；独立维修继续等待自身预热。
            Date candidateReadyTime = repairResumeTime.after(switchEndTime) ? repairResumeTime : switchEndTime;
            if (candidateReadyTime.after(productionReadyTime)) {
                productionReadyTime = candidateReadyTime;
            }
        }
        result.setPlannedRepairAffectingSwitch(repairTimelineStarted);
        result.setProductionNotBefore(RepairPlanRuntimeState.copyDate(productionReadyTime));
        if (repairTimelineStarted) {
            result.setOverlapStatus(RepairPlanResult.OverlapStatus.SUPPORTED);
            result.setReason("真实重叠的维修与切换使用组合准备时间，维修原始区间禁产，首检仍校验实际容量");
        }
        return result;
    }

    /**
     * 将真实相交的05窗口投影为当前切换专用容量，保留原始维修禁产并隔离候选状态。
     * 非相交维修、精度、故障及其他窗口原样复制；不得将该投影写回批次固定事实。
     *
     * @param windows 已解析的容量窗口，未提供窗口时不产生额外约束
     * @param switchStartTime 真实切换开始时间
     * @param switchEndTime 真实切换自身完成时间，不使用后移的生产或首检结束时间
     * @return 当前候选持有的独立容量窗口
     */
    public static List<MachineMaintenanceWindowDTO> adaptCapacityWindowsForSwitch(
            List<MachineMaintenanceWindowDTO> windows, Date switchStartTime, Date switchEndTime) {
        if (CollectionUtils.isEmpty(windows)) {
            return Collections.emptyList();
        }
        return windows.stream().map(MachineMaintenanceWindowDTO::copy).map(window -> {
            if (StringUtils.equals(LhPlannedRepairResolver.PLANNED_REPAIR_CAPACITY_WINDOW_TYPE,
                    window.getMaintenanceType())
                    && overlaps(switchStartTime, switchEndTime,
                    window.getMaintenanceStartTime(), window.getMaintenanceEndTime())) {
                // 仅当前组合覆盖预热；维修本体仍参与不可生产区间并集，不能提前产出首检。
                window.setRepairCombinedWithSwitch(true);
                window.setProductionResumeTime(RepairPlanRuntimeState.copyDate(window.getMaintenanceEndTime()));
            }
            return window;
        }).collect(Collectors.toList());
    }

    /**
     * 保留精度正式接受或组合复核中与设备停机实际相交的原拒绝范围。
     * 不扩大为同日拒绝，不处理独立故障，也不撤销维修来源。
     *
     * @param plans 原实际停机列表，保持原顺序
     * @param physicalMachineCode 精度所属物理机台
     * @param start 精度或完整组合起点
     * @param end 精度或完整组合终点
     * @param reasonPrefix 原调用位置的拒绝说明
     * @return 当前候选的原拒绝结果，无相交时状态为无重叠
     */
    public static RepairPlanResult resolveWithPrecision(List<MdmDevicePlanShut> plans, String physicalMachineCode,
            Date start, Date end, String reasonPrefix) {
        RepairPlanResult result = new RepairPlanResult();
        result.setPhysicalMachineCode(physicalMachineCode);
        for (MdmDevicePlanShut stop : plans) {
            if (stop != null && StringUtils.equals(physicalMachineCode,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(stop.getMachineCode()))
                    && overlaps(start, end, stop.getBeginDate(), stop.getEndDate())) {
                result.setOverlapStatus(RepairPlanResult.OverlapStatus.UNSUPPORTED);
                result.setReason(reasonPrefix + stop.getMachineStopType());
                return result;
            }
        }
        return result;
    }

    /**
     * 清洗组合保留原独立有效窗口及容量并集，不增加取消或拒绝规则。
     *
     * @param result 本次维修候选结果
     * @return 明确沿用原路径的结果，不改变时间和窗口
     */
    public static RepairPlanResult resolveWithCleaning(RepairPlanResult result) {
        result.setOverlapStatus(RepairPlanResult.OverlapStatus.EXISTING_PATH);
        result.setReason("维修与清洗沿用现有有效窗口和容量并集，不增加协同处置");
        return result;
    }

    /**
     * 多事项扩展口保留现有执行顺序，不拼接尚未定义的协同规则。
     *
     * @param result 本次维修候选结果
     * @return 标记原路径且保留全部约束的结果
     */
    public static RepairPlanResult resolveMultipleOverlaps(RepairPlanResult result) {
        result.setOverlapStatus(RepairPlanResult.OverlapStatus.EXISTING_PATH);
        result.setReason("维修多事项协同规则待扩展，继续执行既有各业务约束和容量并集");
        return result;
    }

    /**
     * 按原半开区间判断实际相交，端点相等不算重叠。
     *
     * @param start 当前起点
     * @param end 当前终点
     * @param otherStart 另一实际起点
     * @param otherEnd 另一实际终点
     * @return 是否存在真实交集
     */
    private static boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return start != null && end != null && otherStart != null && otherEnd != null
                && start.before(end) && otherStart.before(otherEnd)
                && start.before(otherEnd) && end.after(otherStart);
    }
}
