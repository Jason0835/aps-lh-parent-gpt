package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanTimeline;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanArrangement;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanRuntimeState;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 仅组合已落实的同物料喷砂置换与当日精度，保持离机清洗资源及唯一置换首检所有权。
 * 所有状态保存在可回滚上下文中，不以物料尚未收尾为由扩展普通精度准入。
 *
 * @author APS
 * @since 2026/10/23
 */
@Component
public class LhSandBlastPrecisionOverlapPolicy {
    /**
     * 复用精度计划资格、参数、每日物理额度及日历校验。
     */
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService = new LhMaintenanceScheduleService();
    /**
     * 复用精度正式接受、撤销及回填生命周期。
     */
    @Resource
    private LhEquipmentPlanScheduleService equipmentPlanScheduleService = new LhEquipmentPlanScheduleService();

    /**
     * 在续作数量计算前按精度优先级评估真实置换机会，组合后仍由续作主链分配班量。
     *
     * @param context 已冻结模具和换模额度的排程上下文
     */
    public void prepare(LhScheduleContext context) {
        if (context.getContinuationSandBlastWindowMap().isEmpty()) {
            return;
        }
        for (MachineScheduleDTO machine : this.resolvePriorityMachines(context)) {
            this.prepareForMachine(context, machine);
        }
    }

    /**
     * 读取喷砂各类精度机会共用的优先级，避免不同组合分别占用每日额度导致顺序倒置。
     *
     * @param context 已加载精度计划的上下文
     * @return 按距到期天数、计划日期及机台编码排序的候选
     */
    public List<MachineScheduleDTO> resolvePriorityMachines(LhScheduleContext context) {
        return context.getMachineScheduleMap().values().stream()
                .filter(Objects::nonNull)
                .filter(machine -> Objects.nonNull(maintenanceScheduleService.resolveMaintenancePlan(
                        context, machine.getMachineCode())))
                .sorted(Comparator.comparing((MachineScheduleDTO machine) -> maintenanceScheduleService
                                .resolveMaintenancePlan(context, machine.getMachineCode()).getDaysToDue(),
                        Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(machine -> maintenanceScheduleService.resolveMaintenancePlan(
                                context, machine.getMachineCode()).getPlanDate(), Comparator.nullsLast(Date::compareTo))
                        .thenComparing(MachineScheduleDTO::getMachineCode))
                .collect(Collectors.toList());
    }

    /**
     * 对当前优先级位置的单机评估同物料置换精度，不改变原置换资格或时间算法。
     *
     * @param context 已冻结置换事实的上下文
     * @param machine 当前精度优先级机台
     */
    public void prepareForMachine(LhScheduleContext context, MachineScheduleDTO machine) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
        if (context.getEquipmentPlanRuntimeState().getReplacementPrecisionPlanMap().containsKey(physical)) {
            return;
        }
        List<MachineCleaningWindowDTO> events = this.resolveWholeReplacement(context, physical);
        if (events.isEmpty()) {
            return;
        }
        MachineCleaningWindowDTO source = events.get(0);
        PrecisionMaintenanceJudgeDTO decision = maintenanceScheduleService.resolveReplacementPrecision(
                context, machine, source.getCleanStartTime());
        if (decision.getDurationHours() <= 0
                || !source.getReplacementReadyTime().after(decision.getPrecisionStartTime())) {
            return;
        }
        Date maintenanceReady = LhScheduleTimeUtil.addMinutes(decision.getPrecisionEndTime(),
                LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
        Date inspectionStart = source.getReplacementReadyTime().after(maintenanceReady)
                ? source.getReplacementReadyTime() : maintenanceReady;
        long inspectionMillis = source.getReplacementReadyTime().getTime()
                - source.getReplacementInspectionStartTime().getTime();
        Date inspectionEnd = new Date(inspectionStart.getTime() + inspectionMillis);
        LhShiftConfigVO countingShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), new Date(inspectionEnd.getTime() - 1L));
        if (Objects.isNull(countingShift) || !equipmentPlanScheduleService.acceptReplacementPrecision(
                context, decision, source.getCleanStartTime(), inspectionStart, inspectionEnd)) {
            return;
        }
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        state.getReplacementPrecisionPlanMap().put(physical, decision.getPrecisionPlanId());
        for (MachineCleaningWindowDTO event : events) {
            state.getReplacementOriginalTimelineMap().put(event.getLhCode(), new CleaningPlanTimeline(event));
            // 原置换总时长已含首检。组合只保留一次首检，从两项准备较晚结束时正向计量。
            event.setReplacementInspectionStartTime(new Date(inspectionStart.getTime()));
            event.setReplacementReadyTime(new Date(inspectionEnd.getTime()));
            event.setReplacementInspectionShiftStartTime(countingShift.getShiftStartDateTime());
        }
        PriorityTraceLogHelper.appendProcessLog(context, "喷砂置换与精度组合",
                "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                        + "，物理机台=" + physical + "，计划主键=" + decision.getPrecisionPlanId()
                        + "，物料=" + source.getContinuationMaterialCode()
                        + "，状态=" + source.getContinuationProductStatus()
                        + "，置换开始=" + LhScheduleTimeUtil.formatDateTime(source.getCleanStartTime())
                        + "，精度开始=" + LhScheduleTimeUtil.formatDateTime(decision.getPrecisionStartTime())
                        + "，精度结束=" + LhScheduleTimeUtil.formatDateTime(decision.getPrecisionEndTime())
                        + "，唯一首检开始=" + LhScheduleTimeUtil.formatDateTime(inspectionStart)
                        + "，正常生产恢复=" + LhScheduleTimeUtil.formatDateTime(inspectionEnd));
    }

    /**
     * 核验全部运行侧的同源同物料置换，禁止用单侧交接证明整机可以停止原生产。
     *
     * @param context 当前批次上下文
     * @param physical 物理机台编码
     * @return 同步置换事件，任一侧缺失或边界不一致时为空集合
     */
    private List<MachineCleaningWindowDTO> resolveWholeReplacement(LhScheduleContext context, String physical) {
        List<MachineScheduleDTO> sides = context.getMachineScheduleMap().values().stream()
                .filter(Objects::nonNull)
                .filter(machine -> StringUtils.equals(physical,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode())))
                .collect(Collectors.toList());
        List<MachineCleaningWindowDTO> events = new ArrayList<>();
        for (MachineScheduleDTO side : sides) {
            MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(side.getMachineCode());
            if (Objects.isNull(event) || !event.isOffMachineCleaning()
                    || StringUtils.isBlank(event.getReplacementMouldCode())
                    || Objects.isNull(event.getCleanStartTime())
                    || Objects.isNull(event.getReplacementInspectionStartTime())
                    || Objects.isNull(event.getReplacementReadyTime())
                    || !event.getReplacementInspectionStartTime().before(event.getReplacementReadyTime())) {
                return Collections.emptyList();
            }
            if (!events.isEmpty()) {
                MachineCleaningWindowDTO source = events.get(0);
                if (!Objects.equals(source.getSourcePlanId(), event.getSourcePlanId())
                        || !Objects.equals(source.getCleanStartTime(), event.getCleanStartTime())
                        || !Objects.equals(source.getReplacementInspectionStartTime(),
                                event.getReplacementInspectionStartTime())
                        || !Objects.equals(source.getReplacementReadyTime(), event.getReplacementReadyTime())
                        || !StringUtils.equals(source.getContinuationMaterialCode(),
                                event.getContinuationMaterialCode())
                        || !StringUtils.equals(source.getContinuationProductStatus(),
                                event.getContinuationProductStatus())) {
                    return Collections.emptyList();
                }
            }
            events.add(event);
        }
        return events;
    }

    /**
     * 置换被最终裁量取消或任一运行侧失效时撤销依赖该机会的精度并释放其每日额度。
     *
     * @param context 已完成清洗处置复核的排程上下文
     */
    public void reconcile(LhScheduleContext context) {
        for (Map.Entry<String, Long> entry : new ArrayList<>(context.getEquipmentPlanRuntimeState()
                .getReplacementPrecisionPlanMap().entrySet())) {
            EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState()
                    .getArrangementMap().get(entry.getValue());
            if (Objects.isNull(arrangement) || !arrangement.isEffective()) {
                LhSandBlastPrecisionOverlapPolicy.restoreReplacementProjection(context, entry.getValue());
            } else if (this.resolveWholeReplacement(context, entry.getKey()).isEmpty()) {
                equipmentPlanScheduleService.revokeArrangement(context, entry.getValue(),
                        "喷砂置换机会已取消或物理配对侧不再成立");
            }
        }
    }

    /**
     * 恢复组合前冻结的置换时间，已取消事件不重建，原模离机清洗资源边界不作改写。
     *
     * @param context 当前可回滚上下文
     * @param planId 被撤销的精度安排主键
     */
    public static void restoreReplacementProjection(LhScheduleContext context, Long planId) {
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        List<String> physicalMachines = state.getReplacementPrecisionPlanMap().entrySet().stream()
                .filter(entry -> Objects.equals(planId, entry.getValue())).map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (String physical : physicalMachines) {
            List<String> sideCodes = state.getReplacementOriginalTimelineMap().keySet().stream()
                    .filter(code -> StringUtils.equals(physical,
                            LhSingleControlMachineUtil.resolvePhysicalMachineCode(code)))
                    .collect(Collectors.toList());
            for (String sideCode : sideCodes) {
                CleaningPlanTimeline original = state.getReplacementOriginalTimelineMap().remove(sideCode);
                MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(sideCode);
                if (Objects.isNull(event) || StringUtils.isBlank(event.getReplacementMouldCode())) {
                    continue;
                }
                event.setReplacementInspectionStartTime(original.getReplacementInspectionStartTime());
                event.setReplacementReadyTime(original.getReplacementReadyTime());
                LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(context.getScheduleWindowShifts(),
                        new Date(original.getReplacementReadyTime().getTime() - 1L));
                event.setReplacementInspectionShiftStartTime(
                        Objects.isNull(shift) ? null : shift.getShiftStartDateTime());
            }
            state.getReplacementPrecisionPlanMap().remove(physical);
        }
    }
}
