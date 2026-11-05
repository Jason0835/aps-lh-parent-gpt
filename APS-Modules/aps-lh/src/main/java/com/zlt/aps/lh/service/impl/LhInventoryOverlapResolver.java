package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhInventoryPlanMatcher;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 盘点与切换并行的独立边界解析；原始盘点继续参与生产容量扣减。
 *
 * @author APS
 * @date 2026/11/03
 */
public final class LhInventoryOverlapResolver {
    /**
     * 禁止实例化无状态解析器。
     */
    private LhInventoryOverlapResolver() {
    }

    /**
     * 判断来源是否为允许切换并行的盘点。
     *
     * @param plan 原始设备停机
     * @return 是否为09盘点
     */
    public static boolean isInventory(MdmDevicePlanShut plan) {
        return plan != null && MachineStopTypeEnum.TAKE_STOCK.getCode().equals(plan.getMachineStopType());
    }

    /**
     * 解析准备阶段的摘要停机结束；盘点只限制生产，不阻塞切换准备。
     *
     * @param machine 运行态机台
     * @return 原停机结束；盘点或空机台返回空
     */
    public static Date resolvePreparationStopEnd(MachineScheduleDTO machine) {
        return machine == null || MachineStopTypeEnum.TAKE_STOCK.getCode().equals(machine.getStopType())
                ? null : machine.getPlanStopEndTime();
    }

    /**
     * 为尚无生产占用的机台分离盘点生产下限和切换准备下限。
     * 只解除窗口起点盘点造成的初始化推迟，不回退真实前料完成及其他门禁。
     *
     * @param context 当前上下文
     * @param machine 当前运行机台
     * @param candidate 原准备起点
     * @return 仅解除初始化盘点占用后的准备起点；其他场景保持原值
     */
    public static Date resolveSwitchReadyTime(LhScheduleContext context, MachineScheduleDTO machine,
                                             Date candidate) {
        if (context == null || machine == null || candidate == null
                || !MachineStopTypeEnum.TAKE_STOCK.getCode().equals(machine.getStopType())
                || context.getScheduleWindowShifts().isEmpty()
                || !Objects.equals(candidate, machine.getEstimatedEndTime())) {
            return candidate;
        }
        boolean hasProduction = context.getScheduleResultList().stream()
                .filter(Objects::nonNull)
                .filter(result -> Objects.equals(machine.getMachineCode(), result.getLhMachineCode()))
                .anyMatch(result -> IntStream.rangeClosed(1, LhScheduleConstant.MAX_SHIFT_SLOT_COUNT).anyMatch(index -> {
                    Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, index);
                    return quantity != null && quantity > 0;
                }));
        if (hasProduction) {
            return candidate;
        }
        Date windowStart = context.getScheduleWindowShifts().get(0).getShiftStartDateTime();
        Date productionReady = MachineCleaningOverlapUtil.resolveEarliestAvailableTime(windowStart,
                machine.getCleaningWindowList(), machine.getPlanStopStartTime(), machine.getPlanStopEndTime());
        if (!candidate.equals(productionReady)) {
            return candidate;
        }
        return MachineCleaningOverlapUtil.resolveEarliestAvailableTime(windowStart,
                machine.getCleaningWindowList(), null, null);
    }

    /**
     * 查找首检实际区间命中的最晚盘点结束，端点相接不视为重叠。
     * 复用普通停机交集合并方法判断，避免产生另一套秒数扣减算法。
     *
     * @param context 排程上下文
     * @param machineCode 来源机台编码，保留左右侧范围
     * @param start 首检开始
     * @param end 首检结束
     * @return 相交盘点最晚结束；未命中返回空
     */
    public static Date resolveInspectionNotBefore(LhScheduleContext context, String machineCode,
                                                 Date start, Date end) {
        List<MdmDevicePlanShut> inventories = context == null ? Collections.emptyList()
                : LhInventoryPlanMatcher.matchInventoryPlans(context.getDevicePlanShutList(), machineCode);
        if (ShiftCapacityResolverUtil.resolvePlannedStopOverlapSeconds(inventories, machineCode, start, end) <= 0) {
            return null;
        }
        return inventories.stream()
                .filter(plan -> plan.getBeginDate().before(end) && plan.getEndDate().after(start))
                .map(MdmDevicePlanShut::getEndDate).max(Date::compareTo).orElse(null);
    }
}
