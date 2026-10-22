package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.RepairPlanRuntimeState;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 计划性维修单场景解析，集中来源时间、合并区间、预热和早班固定量边界。
 * 不读取数据库，不调用排产策略，也不持有候选资源。
 *
 * @author APS
 * @date 2026/10/21
 */
@Slf4j
public final class LhPlannedRepairResolver {

    /**
     * 仅供容量内核识别的维修投影类型，不代表精度维护。
     */
    public static final String PLANNED_REPAIR_CAPACITY_WINDOW_TYPE = "05_CAPACITY";
    /**
     * 独立维修时刻严格校验，不用隐式默认值替代非法配置。
     */
    private static final DateTimeFormatter REPAIR_START_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    /**
     * 禁止创建无状态解析器实例。
     */
    private LhPlannedRepairResolver() {
    }

    /**
     * 读取按物理机台归一化的维修容量投影，返回独立副本。
     * 来源列表替换由上下文失效缓存；参数或班次变化通过输入指纹失效。
     * 显式传入其他列表的兼容调用即时解析，不复用当前批次缓存。
     *
     * @param context 当前排程上下文，可为空
     * @param plans 实际维修和其他停机计划
     * @param machineCode 当前运行侧机台
     * @return 多段维修及各来源早班边界的独立容量窗口
     */
    public static List<MachineMaintenanceWindowDTO> resolveCapacityWindows(
            LhScheduleContext context, List<MdmDevicePlanShut> plans, String machineCode) {
        if (context == null || plans != context.getDevicePlanShutList()) {
            return buildCapacityWindows(context, plans, machineCode);
        }
        if (CollectionUtils.isEmpty(plans) && context.getTemporaryFaultWindowMap().isEmpty()) {
            // 无固定设备约束时直接返回，避免在每次容量计算中重复拼接班次指纹。
            return new ArrayList<>(0);
        }
        RepairPlanRuntimeState state = context.getRepairPlanRuntimeState();
        String inputKey = capacityInputKey(context);
        if (!Objects.equals(inputKey, state.getCapacityInputKey())) {
            // 索引只保存固定来源；预热或真实班次变化后整批失效，不缓存SKU组合结果。
            state.useCapacityInput(inputKey);
            Map<String, List<MachineFaultWindowDTO>> physicalFaults = state.getFaultWindowMap().values().stream()
                    .collect(Collectors.groupingBy(fault -> LhSingleControlMachineUtil
                            .resolvePhysicalMachineCode(fault.getMachineCode()),
                            LinkedHashMap::new, Collectors.toList()));
            state.getPhysicalFaultWindowMap().putAll(physicalFaults);
            if (!CollectionUtils.isEmpty(plans)) {
                Map<String, List<MdmDevicePlanShut>> physicalRepairs = plans.stream().filter(Objects::nonNull)
                        .filter(plan -> StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                                plan.getMachineStopType()))
                        .filter(plan -> StringUtils.isNotEmpty(plan.getMachineCode()))
                        .collect(Collectors.groupingBy(plan -> LhSingleControlMachineUtil
                                .resolvePhysicalMachineCode(plan.getMachineCode()),
                                LinkedHashMap::new, Collectors.toList()));
                state.getPhysicalRepairPlanMap().putAll(physicalRepairs);
            }
        }
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        List<MachineMaintenanceWindowDTO> cached = state.getCapacityWindowMap().computeIfAbsent(
                physicalCode, physical -> buildCapacityWindows(context,
                        state.getPhysicalRepairPlanMap().getOrDefault(physical, Collections.emptyList()), physical));
        return cached.stream().map(MachineMaintenanceWindowDTO::copy).map(window -> {
            window.setMachineCode(machineCode);
            return window;
        }).collect(Collectors.toList());
    }

    /**
     * 获取归一化缓存所需的预热配置及真实班次边界指纹，不缓存候选组合时间。
     *
     * @param context 当前上下文
     * @return 配置、日期及班次一致时稳定的输入键
     */
    private static String capacityInputKey(LhScheduleContext context) {
        String shifts = Objects.isNull(context.getScheduleDate()) ? ""
                : LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate()).stream()
                        .map(shift -> shift.getShiftIndex() + ":" + shift.getWorkDate().getTime() + ":"
                                + shift.getShiftStartDateTime().getTime() + ":"
                                + shift.getShiftEndDateTime().getTime() + ":" + shift.resolveShiftTypeEnum())
                        .collect(Collectors.joining(";"));
        Long scheduleTime = context.getScheduleDate() == null ? null : context.getScheduleDate().getTime();
        return LhScheduleTimeUtil.getCapsulePreheatMinutes(context) + "|" + scheduleTime + "|" + shifts;
    }

    /**
     * 按维修独立开始时刻平移原始时长；不覆盖设备计划原始起止字段。
     *
     * @param context 排程上下文
     * @param source 原始05计划
     * @return 仅用于本轮计算的实际维修计划
     */
    public static MdmDevicePlanShut buildActualRepairPlan(LhScheduleContext context, MdmDevicePlanShut source) {
        String configuredTime = context.getParamValue(
                LhScheduleParamConstant.PLANNED_REPAIR_START_TIME, LhScheduleConstant.PLANNED_REPAIR_START_TIME);
        LocalTime startTime;
        if (StringUtils.isEmpty(configuredTime)) {
            log.warn("计划性维修开始时刻为空，本条不登记，不阻断排程, 计划ID: {}", source.getId());
            return null;
        }
        try {
            startTime = LocalTime.parse(configuredTime, REPAIR_START_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            log.warn("计划性维修开始时刻配置非法，本条不登记，不阻断排程, 计划ID: {}, 参数: {}, 配置值: {}",
                    source.getId(), LhScheduleParamConstant.PLANNED_REPAIR_START_TIME, configuredTime);
            return null;
        }
        Date actualStart = Date.from(source.getBeginDate().toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate().atTime(startTime).atZone(ZoneId.systemDefault()).toInstant());
        long durationMillis = source.getEndDate().getTime() - source.getBeginDate().getTime();
        MdmDevicePlanShut actual = RepairPlanRuntimeState.copySource(source);
        actual.setBeginDate(actualStart);
        actual.setEndDate(new Date(actualStart.getTime() + durationMillis));
        return actual;
    }

    /**
     * 构造指定机台的计划性维修容量窗口。
     * <p>窗口开始时间为真实维修开始，结束时间为真实维修结束，最早恢复时间为维修结束加
     * SYS0307009。调用方可将该列表用于班次产能扣减和最终结果时间对齐，但不得挂入机台
     * maintenanceWindowList，避免误生成精度保养摘要、额度与回填记录。</p>
     *
     * @param context 排程上下文，用于读取 SYS0307009 预热配置
     * @param devicePlanShutList 设备停机计划
     * @param machineCode 机台编号
     * @return 已按真实维修区间合并的容量计算窗口
     */
    private static List<MachineMaintenanceWindowDTO> buildCapacityWindows(
            LhScheduleContext context,
            List<MdmDevicePlanShut> devicePlanShutList,
            String machineCode) {
        List<Date[]> repairIntervals = collectMergedPlannedRepairIntervals(
                devicePlanShutList, machineCode);
        List<MachineMaintenanceWindowDTO> repairWindowList = new ArrayList<>(repairIntervals.size());
        if (CollectionUtils.isEmpty(repairIntervals)) {
            return repairWindowList;
        }
        int preheatMinutes = LhScheduleTimeUtil.getCapsulePreheatMinutes(context);
        for (Date[] repairInterval : repairIntervals) {
            MachineMaintenanceWindowDTO repairWindow = new MachineMaintenanceWindowDTO();
            repairWindow.setMachineCode(machineCode);
            repairWindow.setMaintenanceType(PLANNED_REPAIR_CAPACITY_WINDOW_TYPE);
            repairWindow.setMaintenanceStartTime(RepairPlanRuntimeState.copyDate(repairInterval[0]));
            repairWindow.setMaintenanceEndTime(RepairPlanRuntimeState.copyDate(repairInterval[1]));
            repairWindow.setProductionResumeTime(
                    LhScheduleTimeUtil.addMinutes(repairInterval[1], preheatMinutes));
            repairWindow.setTriggerReason("计划性维修结束后按SYS0307009执行预热");
            if (Objects.nonNull(context) && Objects.nonNull(context.getScheduleDate())) {
                for (LhShiftConfigVO shift : LhScheduleTimeUtil.getScheduleShifts(
                        context, context.getScheduleDate())) {
                    if (ShiftEnum.MORNING_SHIFT == shift.resolveShiftTypeEnum()
                            && hasRepairStartingOnWorkDate(
                            devicePlanShutList, machineCode, repairInterval, shift.getWorkDate())) {
                        repairWindow.getRepairFixedQtyShiftWindowMap().put(
                                RepairPlanRuntimeState.copyDate(shift.getShiftStartDateTime()),
                                RepairPlanRuntimeState.copyDate(shift.getShiftEndDateTime()));
                    }
                }
            }
            repairWindowList.add(repairWindow);
        }
        return repairWindowList;
    }

    /**
     * 跨日合并维修仍按每条来源计划日期识别固定量早班，不能只保留合并区间的第一天。
     *
     * @param stops 实际维修列表
     * @param machineCode 机台
     * @param mergedInterval 已合并维修区间
     * @param workDate 当前业务日期
     * @return 该日期是否有归属于本合并区间的维修开始
     */
    private static boolean hasRepairStartingOnWorkDate(List<MdmDevicePlanShut> stops, String machineCode,
            Date[] mergedInterval, Date workDate) {
        return stops.stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                        plan.getMachineStopType()))
                .filter(plan -> StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getMachineCode())))
                .filter(plan -> Objects.nonNull(plan.getBeginDate()))
                .anyMatch(plan -> !plan.getBeginDate().before(mergedInterval[0])
                        && plan.getBeginDate().before(mergedInterval[1])
                        && Objects.equals(LhScheduleTimeUtil.clearTime(plan.getBeginDate()), workDate));
    }

    /**
     * 收集并合并指定机台全部有效计划性维修区间。
     * <p>合并口径包含首尾相接，确保连续维修只在最后一次真实结束后执行一次预热。</p>
     *
     * @param devicePlanShutList 设备停机计划
     * @param machineCode 机台编号
     * @return 按开始时间升序排列、互不重叠的维修区间
     */
    private static List<Date[]> collectMergedPlannedRepairIntervals(
            List<MdmDevicePlanShut> devicePlanShutList,
            String machineCode) {
        List<Date[]> repairIntervals = new ArrayList<>();
        if (CollectionUtils.isEmpty(devicePlanShutList) || StringUtils.isEmpty(machineCode)) {
            return repairIntervals;
        }
        for (MdmDevicePlanShut planShut : devicePlanShutList) {
            if (Objects.isNull(planShut)
                    || !StringUtils.equals(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode),
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(planShut.getMachineCode()))
                    || !StringUtils.equals(MachineStopTypeEnum.PLANNED_REPAIR.getCode(),
                    planShut.getMachineStopType())
                    || Objects.isNull(planShut.getBeginDate())
                    || Objects.isNull(planShut.getEndDate())
                    || !planShut.getBeginDate().before(planShut.getEndDate())) {
                continue;
            }
            repairIntervals.add(new Date[]{planShut.getBeginDate(), planShut.getEndDate()});
        }
        return mergeIntervals(repairIntervals);
    }

    /**
     * 解析维修当天早班的维修起点。现代容量窗口携带完整早班边界，残班也不能再次按普通班产排满。
     *
     * @param stops 普通设备停机
     * @param windows 公共容量窗口
     * @param machineCode 当前机台
     * @param start 计算起点
     * @param end 计算终点
     * @return 当前窗口受05固定量限制时的维修开始，否则为空
     */
    public static Date resolveFixedQtyRepairStart(List<MdmDevicePlanShut> stops,
            List<MachineMaintenanceWindowDTO> windows, String machineCode, Date start, Date end) {
        boolean hasRepairWindow = false;
        Date repairStart = null;
        if (!CollectionUtils.isEmpty(windows)) {
            for (MachineMaintenanceWindowDTO window : windows) {
                if (Objects.isNull(window) || !StringUtils.equals(PLANNED_REPAIR_CAPACITY_WINDOW_TYPE,
                        window.getMaintenanceType())) {
                    continue;
                }
                hasRepairWindow |= !window.getRepairFixedQtyShiftWindowMap().isEmpty();
                for (Map.Entry<Date, Date> fixedShift : window.getRepairFixedQtyShiftWindowMap().entrySet()) {
                    if (start.before(fixedShift.getValue()) && end.after(fixedShift.getKey())) {
                        repairStart = earlier(repairStart, window.getMaintenanceStartTime());
                    }
                }
            }
        }
        return hasRepairWindow ? repairStart : resolvePlannedRepairStartTime(stops, machineCode, start, end);
    }

    /**
     * 解析当前班次窗口内最早的计划性维修开始时间。
     *
     * @param devicePlanShutList 设备停机计划
     * @param machineCode 机台编号
     * @param windowStartTime 班次实际开始时间
     * @param windowEndTime 班次结束时间
     * @return 维修开始时间；当前班次未命中时返回 null
     */
    private static Date resolvePlannedRepairStartTime(List<MdmDevicePlanShut> devicePlanShutList,
                                                      String machineCode,
                                                      Date windowStartTime,
                                                      Date windowEndTime) {
        Date earliestRepairStartTime = null;
        for (Date[] repairInterval : collectMergedPlannedRepairIntervals(devicePlanShutList, machineCode)) {
            Date repairStartTime = repairInterval[0];
            if (!repairStartTime.before(windowStartTime) && repairStartTime.before(windowEndTime)
                    && (Objects.isNull(earliestRepairStartTime)
                    || repairStartTime.before(earliestRepairStartTime))) {
                earliestRepairStartTime = repairStartTime;
            }
        }
        return earliestRepairStartTime;
    }

    /**
     * 合并有交叉或首尾相接的区间，统一输出不重叠区间列表。
     *
     * @param intervals 已校验起止时间的维修区间
     * @return 按起点排序并合并后的真实维修区间
     */
    private static List<Date[]> mergeIntervals(List<Date[]> intervals) {
        List<Date[]> mergedIntervals = new ArrayList<>();
        if (CollectionUtils.isEmpty(intervals)) {
            return mergedIntervals;
        }
        intervals.sort((left, right) -> {
            int startCmp = left[0].compareTo(right[0]);
            if (startCmp != 0) {
                return startCmp;
            }
            return left[1].compareTo(right[1]);
        });
        Date mergedStartTime = intervals.get(0)[0];
        Date mergedEndTime = intervals.get(0)[1];
        for (int index = 1; index < intervals.size(); index++) {
            Date currentStartTime = intervals.get(index)[0];
            Date currentEndTime = intervals.get(index)[1];
            if (!currentStartTime.after(mergedEndTime)) {
                if (currentEndTime.after(mergedEndTime)) {
                    mergedEndTime = currentEndTime;
                }
                continue;
            }
            mergedIntervals.add(new Date[]{mergedStartTime, mergedEndTime});
            mergedStartTime = currentStartTime;
            mergedEndTime = currentEndTime;
        }
        mergedIntervals.add(new Date[]{mergedStartTime, mergedEndTime});
        return mergedIntervals;
    }

    /**
     * 取两个时间中的较早值。
     *
     * @param left 第一日期，可为空
     * @param right 第二日期，可为空
     * @return 非空日期中的较早值，两者皆空时为空
     */
    private static Date earlier(Date left, Date right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.before(right) ? left : right;
    }
}
