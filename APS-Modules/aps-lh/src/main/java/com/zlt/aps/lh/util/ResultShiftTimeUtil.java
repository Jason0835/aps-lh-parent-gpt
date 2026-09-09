package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.constant.LhScheduleParamConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.ShiftProductionControlDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.component.CapsuleReplacementRuleService;
import com.zlt.aps.lh.component.StructureMinMachineRetentionService;
import com.zlt.aps.lh.context.LhScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 数量修改后的班次时间收口工具。
 * <p>复用正式排量的产能、停机和取整规则，不修改数量、首检区间、候选排序或后料安排。</p>
 */
@Slf4j
public final class ResultShiftTimeUtil {

    /** 无批次状态的胶囊规则，仅查询已经登记的容量上限，禁止再次登记或扣量。 */
    private static final CapsuleReplacementRuleService CAPSULE_RULE = new CapsuleReplacementRuleService();
    /** 仅使用内存结果判定在机状态，不调用配置加载或数据库查询方法。 */
    private static final StructureMinMachineRetentionService RETENTION_RULE = new StructureMinMachineRetentionService();

    private ResultShiftTimeUtil() {
    }

    /**
     * 按实际班产及已生效损失反算当前数量的完成时间。
     * @param context 排程上下文
     * @param result 当前结果，班产已经采用当前机台口径
     * @param shift 真实班次配置
     * @param startTime 本班实际开始时间
     * @param planQty 最终分配数量
     * @param cleaningWindows 已按本结果过滤的清洗窗口
     * @param maintenanceWindows 已含维修预热及换胶囊的产能窗口
     * @return 当前数量的完成时间；无有效时间或容量时返回空，不伪造班末完成
     */
    public static Date resolveEndTime(LhScheduleContext context, LhScheduleResult result,
                                      LhShiftConfigVO shift, Date startTime, int planQty,
                                      List<MachineCleaningWindowDTO> cleaningWindows,
                                      List<MachineMaintenanceWindowDTO> maintenanceWindows) {
        if (Objects.isNull(context) || Objects.isNull(result) || Objects.isNull(shift)
                || Objects.isNull(startTime) || planQty <= 0) {
            return null;
        }
        ShiftProductionControlDTO control = ShiftProductionControlUtil.resolveEffectiveControl(context, shift, startTime);
        if (Objects.isNull(control) || !control.isCanSchedule()) {
            return null;
        }
        int mouldQty = ShiftCapacityResolverUtil.resolveMachineMouldQty(
                Objects.isNull(result.getMouldQty()) ? 0 : result.getMouldQty());
        int capacity = resolveCapacity(context, result, shift, control, cleaningWindows, maintenanceWindows, mouldQty);
        if (capacity <= 0) {
            log.warn("班次数量时间核对无有效产能, batchNo: {}, materialCode: {}, machineCode: {}, shiftIndex: {}, planQty: {}",
                    context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(), shift.getShiftIndex(), planQty);
            return null;
        }
        // 一模内的尾数使用同一完成点；完整奇数班产仍保留既有班别修正上限。
        boolean hasSandBlastInspection = !CleaningScheduleRuleUtil.resolveSandBlastInspectionWindows(
                cleaningWindows, control.getEffectiveStartTime(), control.getEffectiveEndTime()).isEmpty();
        int timeQty = hasSandBlastInspection ? planQty
                : ShiftCapacityResolverUtil.roundUpQtyToMouldMultiple(planQty, mouldQty);
        if (planQty <= capacity) {
            timeQty = Math.min(timeQty, capacity);
        }
        Date endTime = ShiftCapacityResolverUtil.resolveShiftPlanEndTime(context.getDevicePlanShutList(),
                cleaningWindows, maintenanceWindows, result.getLhMachineCode(),
                control.getEffectiveStartTime(), control.getEffectiveEndTime(), timeQty, capacity, context, result, shift);
        if (planQty > capacity || (Objects.nonNull(endTime) && endTime.after(shift.getShiftEndDateTime()))) {
            log.warn("班次数量时间超限, batchNo: {}, materialCode: {}, machineCode: {}, shiftIndex: {}, "
                            + "planQty: {}, capacity: {}, endTime: {}, shiftEndTime: {}，保留真实计算时间，不截断掩盖超量",
                    context.getBatchNo(), result.getMaterialCode(), result.getLhMachineCode(), shift.getShiftIndex(),
                    planQty, capacity, endTime, shift.getShiftEndDateTime());
        }
        return endTime;
    }

    /**
     * 复用正向排量入口，胶囊固定扣量仅收紧已登记上限，时间模式已包含在维护窗口中。
     * @param context 排程上下文
     * @param result 当前结果
     * @param shift 班次配置
     * @param control 生效班次窗口
     * @param cleaningWindows 有效清洗窗口
     * @param maintenanceWindows 有效维护窗口
     * @param mouldQty 当前机台模数
     * @return 相同输入下的实际最大可排量
     */
    private static int resolveCapacity(LhScheduleContext context, LhScheduleResult result, LhShiftConfigVO shift,
                                        ShiftProductionControlDTO control, List<MachineCleaningWindowDTO> cleaningWindows,
                                        List<MachineMaintenanceWindowDTO> maintenanceWindows, int mouldQty) {
        int capacity = ShiftCapacityResolverUtil.resolveShiftCapacityWithDowntime(context.getDevicePlanShutList(),
                cleaningWindows, maintenanceWindows, result.getLhMachineCode(),
                control.getEffectiveStartTime(), control.getEffectiveEndTime(),
                Objects.isNull(result.getSingleMouldShiftQty()) ? 0 : result.getSingleMouldShiftQty(),
                Objects.isNull(result.getLhTime()) ? 0 : result.getLhTime(), mouldQty,
                ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift),
                context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_LOSS_QTY, LhScheduleConstant.DRY_ICE_LOSS_QTY),
                context.getParamIntValue(LhScheduleParamConstant.DRY_ICE_DURATION_HOURS, LhScheduleConstant.DRY_ICE_DURATION_HOURS),
                shift, ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context), result.getScheduleType(),
                context.getParamIntValue(LhScheduleParamConstant.PLANNED_REPAIR_FIXED_QTY, LhScheduleConstant.PLANNED_REPAIR_FIXED_QTY));
        capacity = ShiftProductionControlUtil.deductCapacityByControl(control, capacity, mouldQty);
        return CAPSULE_RULE.resolveReplacementShiftCapacityUpperLimit(context, result, shift, capacity);
    }

    /**
     * 汇总最终班次完成时间，停产保机仍保持窗口占用语义。
     * @param context 排程上下文
     * @param result 已完成数量及班次时间回写的结果
     */
    public static void refreshSummary(LhScheduleContext context, LhScheduleResult result) {
        ShiftFieldUtil.syncDailyPlanQty(result);
        if (context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
            return;
        }
        Date endTime = null;
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            Date shiftEnd = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
            if (Objects.nonNull(qty) && qty > 0 && Objects.nonNull(shiftEnd)
                    && (Objects.isNull(endTime) || shiftEnd.after(endTime))) {
                endTime = shiftEnd;
            }
        }
        result.setSpecEndTime(endTime);
        result.setTdaySpecEndTime(endTime);
    }

    /**
     * 刷新已提交机台的最终占用边界及班次缓存，前料回写不得覆盖后料占用。
     * <p>模具在新增阶段按机台绑定和结果时间判定可用性，此处不清空已分配模具集合。</p>
     * @param context 排程上下文
     * @param result 本次变化的结果
     */
    public static void refreshMachine(LhScheduleContext context, LhScheduleResult result) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        if (Objects.isNull(machine)) {
            return;
        }
        Date endTime = result.getSpecEndTime();
        for (LhScheduleResult scheduled : context.getScheduleResultList()) {
            if (StringUtils.equals(result.getLhMachineCode(), scheduled.getLhMachineCode())
                    && Objects.nonNull(scheduled.getSpecEndTime())
                    && (Objects.isNull(endTime) || scheduled.getSpecEndTime().after(endTime))) {
                endTime = scheduled.getSpecEndTime();
            }
        }
        if (!context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
            machine.setEstimatedEndTime(endTime);
        }
        if (Objects.nonNull(context.getStructureShiftInMachineIndex())) {
            context.getStructureShiftInMachineIndex().refreshMachine(context, RETENTION_RULE, result.getLhMachineCode());
        }
    }

    /**
     * 保存前归整使用与主链一致的机台清洗过滤和维修、胶囊窗口。
     * @param context 排程上下文
     * @param result 当前结果
     * @param shift 变化的班次
     * @param planQty 归整后的数量
     * @return 归整后的结束时间
     */
    public static Date resolveNormalizedEndTime(LhScheduleContext context, LhScheduleResult result,
                                                LhShiftConfigVO shift, int planQty) {
        MachineScheduleDTO machine = context.getMachineScheduleMap().get(result.getLhMachineCode());
        List<MachineCleaningWindowDTO> cleaningWindows = Objects.isNull(machine)
                ? Collections.emptyList() : machine.getCleaningWindowList();
        if (CleaningScheduleRuleUtil.shouldSkipCleaningByResultEnding(result)) {
            cleaningWindows = Collections.emptyList();
        }
        Date switchEnd = Objects.isNull(result.getMouldChangeStartTime()) ? null
                : LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(),
                "1".equals(result.getIsTypeBlock()) ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                        : LhScheduleTimeUtil.getMouldChangeTotalHours(context));
        cleaningWindows = MachineCleaningOverlapUtil.excludeOverlapWindows(
                cleaningWindows, result.getMouldChangeStartTime(), switchEnd);
        List<MachineMaintenanceWindowDTO> maintenanceWindows = ShiftCapacityResolverUtil.resolveCapacityMaintenanceWindowList(
                context, context.getDevicePlanShutList(), result.getLhMachineCode(),
                Objects.isNull(machine) ? Collections.emptyList() : machine.getMaintenanceWindowList());
        return resolveEndTime(context, result, shift, ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex()),
                planQty, cleaningWindows, maintenanceWindows);
    }
}
