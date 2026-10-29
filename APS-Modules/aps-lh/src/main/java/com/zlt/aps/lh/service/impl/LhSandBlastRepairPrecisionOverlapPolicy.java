package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanTimeline;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanArrangement;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanRuntimeState;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanTimeline;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.CleaningScheduleRuleUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftFieldUtil;
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
 * 在机喷砂、05维修与当日精度的专用组合策略，三项准备并行后只执行一次喷砂首检。
 * 原始清洗及维修时间保持来源事实，精度资格、物理额度和回填复用公共编排生命周期。
 *
 * @author APS
 * @since 2026/10/23
 */
@Component
public class LhSandBlastRepairPrecisionOverlapPolicy {
    /**
     * 最终承接组合事件的班次备注。
     */
    private static final String COMBINED_ANALYSIS = "喷砂清洗+维修+维保";
    /**
     * 复用精度唯一资格判定入口。
     */
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService = new LhMaintenanceScheduleService();
    /**
     * 复用精度正式安排及撤销生命周期。
     */
    @Resource
    private LhEquipmentPlanScheduleService equipmentPlanScheduleService = new LhEquipmentPlanScheduleService();

    /**
     * 在续作数量计算前，对当前精度优先级的机台评估整机喷砂及维修停机机会。
     *
     * @param context 已加载固定清洗、维修和精度来源的上下文
     * @param machine 与置换精度共用排序后的当前机台
     */
    public void prepareForMachine(LhScheduleContext context, MachineScheduleDTO machine) {
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
        if (context.getEquipmentPlanRuntimeState().getSandBlastRepairPrecisionPlanMap().containsKey(physical)) {
            return;
        }
        List<MachineCleaningWindowDTO> sources = this.resolveWholeSources(context, physical);
        if (sources.isEmpty()) {
            return;
        }
        MachineCleaningWindowDTO source = sources.get(0);
        PrecisionMaintenanceJudgeDTO decision = maintenanceScheduleService.resolveSandBlastRepairPrecision(
                context, machine, source.getCleanStartTime());
        if (decision.getDurationHours() <= 0) {
            return;
        }
        List<MachineMaintenanceWindowDTO> repairs = resolveRepairs(context, source);
        if (repairs.stream().noneMatch(repair -> overlaps(repair.getMaintenanceStartTime(),
                repair.getMaintenanceEndTime(), decision.getPrecisionStartTime(), decision.getPrecisionEndTime()))) {
            return;
        }
        Date inspectionStart = LhScheduleTimeUtil.addMinutes(decision.getPrecisionEndTime(),
                LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
        if (source.getCleanEndTime().after(inspectionStart)) {
            inspectionStart = source.getCleanEndTime();
        }
        for (MachineMaintenanceWindowDTO repair : repairs) {
            if (repair.getProductionResumeTime().after(inspectionStart)) {
                inspectionStart = repair.getProductionResumeTime();
            }
        }
        long inspectionMillis = source.getReadyTime().getTime()
                - MachineCleaningOverlapUtil.resolveSandBlastInspectionStartTime(source).getTime();
        Date inspectionEnd = new Date(inspectionStart.getTime() + inspectionMillis);
        LhShiftConfigVO countingShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), new Date(inspectionEnd.getTime() - 1L));
        if (countingShift == null || !equipmentPlanScheduleService.acceptSandBlastRepairPrecision(
                context, decision, sources, inspectionStart, inspectionEnd)) {
            return;
        }
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        state.getSandBlastRepairPrecisionPlanMap().put(physical, decision.getPrecisionPlanId());
        for (MachineCleaningWindowDTO event : sources) {
            state.getSandBlastRepairOriginalTimelineMap().put(event.getLhCode(), new CleaningPlanTimeline(event));
            event.setSandBlastRepairPrecisionPlanId(decision.getPrecisionPlanId());
            event.setSandBlastInspectionStartTime(new Date(inspectionStart.getTime()));
            event.setReadyTime(new Date(inspectionEnd.getTime()));
            event.setSandBlastInspectionShiftStartTime(countingShift.getShiftStartDateTime());
        }
        PriorityTraceLogHelper.appendProcessLog(context, "喷砂维修精度组合",
                "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                        + "，物理机台=" + physical + "，物料=" + source.getSourceMaterialCode()
                        + "，状态=" + source.getSourceProductStatus() + "，清洗来源=" + source.getSourcePlanId()
                        + "，精度主键=" + decision.getPrecisionPlanId()
                        + "，清洗结束=" + LhScheduleTimeUtil.formatDateTime(source.getCleanEndTime())
                        + "，精度开始=" + LhScheduleTimeUtil.formatDateTime(decision.getPrecisionStartTime())
                        + "，精度结束=" + LhScheduleTimeUtil.formatDateTime(decision.getPrecisionEndTime())
                        + "，唯一喷砂首检开始=" + LhScheduleTimeUtil.formatDateTime(inspectionStart)
                        + "，首检条数=" + sources.stream().mapToInt(event -> event.getSandBlastFirstInspectionQty()).sum()
                        + "，正常生产恢复=" + LhScheduleTimeUtil.formatDateTime(inspectionEnd));
    }

    /**
     * 查找全部运行侧同步且仍在机执行的同源喷砂，不借单侧或历史换模证明整机停机。
     *
     * @param context 当前批次上下文
     * @param physical 物理机台编码
     * @return 完整同源事件组，缺少任何运行侧或维修本体重叠时为空
     */
    private List<MachineCleaningWindowDTO> resolveWholeSources(LhScheduleContext context, String physical) {
        List<MachineScheduleDTO> sides = physicalMachines(context, physical);
        if (sides.isEmpty()) {
            return Collections.emptyList();
        }
        List<MachineCleaningWindowDTO> candidates = sides.get(0).getCleaningWindowList().stream()
                .filter(window -> this.isEligibleSource(context, sides.get(0), window))
                .sorted(Comparator.comparing(MachineCleaningWindowDTO::getCleanStartTime))
                .collect(Collectors.toList());
        for (MachineCleaningWindowDTO source : candidates) {
            List<MachineCleaningWindowDTO> sources = new ArrayList<>(sides.size());
            for (MachineScheduleDTO side : sides) {
                MachineCleaningWindowDTO event = side.getCleaningWindowList().stream()
                        .filter(window -> this.isEligibleSource(context, side, window))
                        .filter(window -> Objects.equals(source.getSourcePlanId(), window.getSourcePlanId())
                                && Objects.equals(source.getCleanStartTime(), window.getCleanStartTime())
                                && Objects.equals(source.getCleanEndTime(), window.getCleanEndTime())
                                && Objects.equals(source.getReadyTime(), window.getReadyTime())
                                && Objects.equals(source.getSourceMaterialCode(), window.getSourceMaterialCode())
                                && Objects.equals(source.getSourceProductStatus(), window.getSourceProductStatus()))
                        .findFirst().orElse(null);
                if (event == null) {
                    break;
                }
                sources.add(event);
            }
            if (sources.size() == sides.size()) {
                return sources;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 校验独立在机喷砂来源和完整首检参数，普通干冰、离机置换及已取消来源不参与组合。
     *
     * @param context 当前批次上下文
     * @param machine 来源所属运行侧
     * @param source 待检查窗口
     * @return 来源、时间、独立处置及05维修相交均成立时返回true
     */
    private boolean isEligibleSource(LhScheduleContext context, MachineScheduleDTO machine,
            MachineCleaningWindowDTO source) {
        return MachineCleaningOverlapUtil.isSandBlastCleaning(source) && !source.isOffMachineCleaning()
                && source.getSourcePlanId() != null && StringUtils.isNotBlank(source.getSourceMaterialCode())
                && source.getCleanStartTime() != null && source.getCleanEndTime() != null
                && source.getReadyTime() != null && source.getCleanStartTime().before(source.getCleanEndTime())
                && source.getCleanEndTime().before(source.getReadyTime())
                && source.getSandBlastFirstInspectionQty() != null && source.getSandBlastFirstInspectionQty() > 0
                && CleaningDispositionResolver.resolve(context, machine.getMachineCode(), source).getDisposition()
                        == CleaningDispositionResolver.Disposition.INDEPENDENT
                && !CleaningDispositionResolver.hasPendingRegularChange(context, machine.getMachineCode(), source)
                && !resolveRepairs(context, source).isEmpty();
    }

    /**
     * 复用实际05维修窗口，只认可维修本体与清洗本体严格相交的来源。
     *
     * @param context 当前固定维修来源
     * @param source 实际喷砂来源
     * @return 命中的维修及一次预热容量窗口
     */
    private static List<MachineMaintenanceWindowDTO> resolveRepairs(LhScheduleContext context,
            MachineCleaningWindowDTO source) {
        return LhPlannedRepairResolver.resolveCapacityWindows(context, context.getDevicePlanShutList(),
                        source.getLhCode()).stream()
                .filter(window -> LhPlannedRepairResolver.PLANNED_REPAIR_CAPACITY_WINDOW_TYPE
                        .equals(window.getMaintenanceType()))
                .filter(window -> overlaps(source.getCleanStartTime(), source.getCleanEndTime(),
                        window.getMaintenanceStartTime(), window.getMaintenanceEndTime()))
                .collect(Collectors.toList());
    }

    /**
     * 对完整组合区间逐一检查其他停机、故障、清洗、维护、胶囊和已提交生产，门禁仅豁免本次来源。
     *
     * @param context 固定来源及当前实际结果
     * @param timeline 含唯一喷砂首检的完整精度时间轴
     * @param sources 已核验的同源整机喷砂窗口
     * @return 未支持冲突的原因，无冲突时为空
     */
    public static String resolveConflict(LhScheduleContext context, EquipmentPlanTimeline timeline,
            List<MachineCleaningWindowDTO> sources) {
        if (sources.isEmpty()) {
            return "三项组合缺少有效喷砂来源";
        }
        MachineCleaningWindowDTO source = sources.get(0);
        Date start = source.getCleanStartTime();
        Date end = timeline.getProductionResumeTime();
        String physical = timeline.getPhysicalMachineCode();
        boolean stopConflict = context.getDevicePlanShutList().stream().filter(Objects::nonNull)
                .filter(plan -> samePhysical(physical, plan.getMachineCode()))
                .filter(plan -> overlaps(start, end, plan.getBeginDate(), plan.getEndDate()))
                .anyMatch(plan -> !MachineStopTypeEnum.PLANNED_REPAIR.getCode().equals(plan.getMachineStopType())
                        || !overlaps(source.getCleanStartTime(), source.getCleanEndTime(),
                                plan.getBeginDate(), plan.getEndDate()));
        if (stopConflict || context.getTemporaryFaultWindowMap().values().stream().filter(Objects::nonNull)
                .anyMatch(fault -> samePhysical(physical, fault.getMachineCode())
                        && overlaps(start, end, fault.getStartTime(), fault.getEndTime()))) {
            return "三项组合与其他停机或临时故障重叠";
        }
        if (context.getCapsuleReplacementTimeWindowMap().values().stream().filter(Objects::nonNull)
                .anyMatch(window -> samePhysical(physical, window.getPhysicalMachineCode())
                        && overlaps(start, end, window.getReplacementStartTime(), window.getReplacementEndTime()))) {
            return "三项组合与换胶囊重叠尚未接入";
        }
        for (MachineScheduleDTO side : physicalMachines(context, physical)) {
            boolean otherCleaning = side.getCleaningWindowList().stream().filter(Objects::nonNull)
                    .filter(window -> sources.stream().noneMatch(event -> Objects.equals(
                            event.getSourcePlanId(), window.getSourcePlanId())
                            && Objects.equals(event.getLhCode(), window.getLhCode())))
                    .anyMatch(window -> overlaps(start, end, window.getCleanStartTime(), window.getReadyTime()));
            boolean otherMaintenance = side.getMaintenanceWindowList().stream().filter(Objects::nonNull)
                    .anyMatch(window -> !Objects.equals(timeline.getPrecisionPlanId(), window.getPrecisionPlanId())
                            && overlaps(start, end, window.getMaintenanceStartTime(),
                                    window.getProductionResumeTime() == null
                                            ? window.getMaintenanceEndTime() : window.getProductionResumeTime()));
            if (otherCleaning || otherMaintenance) {
                return "三项组合与其他清洗或维护重叠尚未接入";
            }
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (!samePhysical(physical, result.getLhMachineCode())) {
                continue;
            }
            if (result.getMouldChangeStartTime() != null && overlaps(start, end, result.getMouldChangeStartTime(),
                    LhCleaningRepairOverlapPolicy.resolveSwitchEnd(context, result))) {
                return "三项组合与已提交换模或换活字块重叠尚未接入";
            }
            if (context.getScheduleWindowShifts().stream().anyMatch(shift ->
                    ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) != null
                            && ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex()) > 0
                            && overlaps(start, end, ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex()),
                                    ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex())))) {
                return "三项组合与已提交生产重叠，不能改写已扣账结果";
            }
        }
        return null;
    }

    /**
     * 来源取消或配对侧失效时撤销依赖它的精度，释放当日额度并恢复组合前首检。
     *
     * @param context 已更新清洗处置的上下文
     */
    public void reconcile(LhScheduleContext context) {
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        for (Map.Entry<String, Long> entry : new ArrayList<>(state.getSandBlastRepairPrecisionPlanMap().entrySet())) {
            EquipmentPlanArrangement arrangement = state.getArrangementMap().get(entry.getValue());
            if (arrangement == null || !arrangement.isEffective()) {
                restoreCleaningTimeline(context, entry.getValue());
                continue;
            }
            List<MachineScheduleDTO> sides = physicalMachines(context, entry.getKey());
            boolean valid = !sides.isEmpty() && sides.stream().allMatch(machine ->
                    machine.getCleaningWindowList().stream().anyMatch(window ->
                            Objects.equals(entry.getValue(), window.getSandBlastRepairPrecisionPlanId())
                                    && this.isEligibleSource(context, machine, window)));
            if (!valid) {
                equipmentPlanScheduleService.revokeArrangement(context, entry.getValue(),
                        "在机喷砂维修机会取消或物理配对侧失效");
            }
        }
    }

    /**
     * 依据最终有效组合及实际首检承接结果合并班次备注，不重排数量或修改其他事件说明。
     *
     * @param context 已完成清洗、维修与精度摘要绑定的上下文
     */
    public void appendFinalAnalysis(LhScheduleContext context) {
        for (Map.Entry<String, Long> entry : context.getEquipmentPlanRuntimeState()
                .getSandBlastRepairPrecisionPlanMap().entrySet()) {
            EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState()
                    .getArrangementMap().get(entry.getValue());
            if (arrangement == null || !arrangement.isEffective()) {
                continue;
            }
            for (MachineScheduleDTO machine : physicalMachines(context, entry.getKey())) {
                machine.getCleaningWindowList().stream()
                        .filter(source -> Objects.equals(entry.getValue(), source.getSandBlastRepairPrecisionPlanId()))
                        .forEach(source -> this.bindFinalAnalysis(context, machine, source, entry.getValue()));
            }
        }
    }

    /**
     * 将单个有效组合绑定到真正承接首检的结果，保留同班其他独立事件的原因。
     *
     * @param context 最终结果上下文
     * @param machine 组合所属运行侧
     * @param source 有效喷砂窗口
     * @param planId 有效精度主键
     */
    private void bindFinalAnalysis(LhScheduleContext context, MachineScheduleDTO machine,
            MachineCleaningWindowDTO source, Long planId) {
        LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(context.getScheduleWindowShifts(),
                new Date(source.getReadyTime().getTime() - 1L));
        if (shift == null) {
            return;
        }
        LhScheduleResult result = context.getScheduleResultList().stream()
                .filter(item -> StringUtils.equals(machine.getMachineCode(), item.getLhMachineCode()))
                .filter(item -> StringUtils.equals(source.getSourceMaterialCode(), item.getMaterialCode()))
                .filter(item -> StringUtils.equals(StringUtils.defaultIfBlank(source.getSourceProductStatus(), "S"),
                        StringUtils.defaultIfBlank(item.getProductStatus(), "S")))
                .filter(item -> ShiftFieldUtil.getShiftPlanQty(item, shift.getShiftIndex()) != null
                        && ShiftFieldUtil.getShiftPlanQty(item, shift.getShiftIndex()) > 0)
                .filter(item -> !CleaningScheduleRuleUtil.resolveSandBlastInspectionWindows(
                        Collections.singletonList(source),
                        ShiftFieldUtil.getShiftStartTime(item, shift.getShiftIndex()),
                        shift.getShiftEndDateTime()).isEmpty())
                .filter(item -> ShiftFieldUtil.getShiftEndTime(item, shift.getShiftIndex()) != null
                        && !ShiftFieldUtil.getShiftEndTime(item, shift.getShiftIndex()).before(source.getReadyTime()))
                .findFirst().orElse(null);
        if (result == null) {
            return;
        }
        boolean otherInspection = machine.getCleaningWindowList().stream()
                .filter(other -> !Objects.equals(source.getSourcePlanId(), other.getSourcePlanId()))
                .filter(other -> CleaningDispositionResolver.resolve(context, machine.getMachineCode(), other)
                        .getDisposition() == CleaningDispositionResolver.Disposition.INDEPENDENT)
                .anyMatch(other -> !CleaningScheduleRuleUtil.resolveSandBlastInspectionWindows(
                        Collections.singletonList(other), shift.getShiftStartDateTime(),
                        shift.getShiftEndDateTime()).isEmpty());
        if (!otherInspection) {
            ShiftFieldUtil.removeShiftAnalysis(result, shift.getShiftIndex(), "喷砂清洗首检");
        }
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machine.getMachineCode());
        boolean otherRepair = context.getDevicePlanShutList().stream().filter(Objects::nonNull)
                .filter(plan -> samePhysical(physical, plan.getMachineCode()))
                .filter(plan -> MachineStopTypeEnum.PLANNED_REPAIR.getCode().equals(plan.getMachineStopType()))
                .filter(plan -> !overlaps(source.getCleanStartTime(), source.getCleanEndTime(),
                        plan.getBeginDate(), plan.getEndDate()))
                .anyMatch(plan -> overlaps(shift.getShiftStartDateTime(), shift.getShiftEndDateTime(),
                        plan.getBeginDate(), plan.getEndDate()));
        if (!otherRepair) {
            ShiftFieldUtil.removeShiftAnalysis(result, shift.getShiftIndex(), "计划性维修+预热");
        }
        ShiftFieldUtil.removeShiftAnalysis(result, shift.getShiftIndex(), "精度计划");
        ShiftFieldUtil.appendShiftAnalysis(result, shift.getShiftIndex(), COMBINED_ANALYSIS);
        PriorityTraceLogHelper.appendProcessLog(context, "喷砂维修精度最终核验",
                "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                        + "，机台=" + machine.getMachineCode() + "，物料=" + result.getMaterialCode()
                        + "，状态=" + result.getProductStatus() + "，精度主键=" + planId
                        + "，首检开始=" + LhScheduleTimeUtil.formatDateTime(
                                MachineCleaningOverlapUtil.resolveSandBlastInspectionStartTime(source))
                        + "，首检结束=" + LhScheduleTimeUtil.formatDateTime(source.getReadyTime())
                        + "，首检条数=" + source.getSandBlastFirstInspectionQty()
                        + "，班次=" + shift.getShiftIndex() + "，最终班次计划量="
                        + ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex())
                        + "，备注=" + COMBINED_ANALYSIS);
    }

    /**
     * 恢复组合前的喷砂首检，已移除的来源不重建，不改写实际清洗结束及模具释放事实。
     *
     * @param context 可回滚的本批上下文
     * @param planId 被撤销的精度主键
     */
    public static void restoreCleaningTimeline(LhScheduleContext context, Long planId) {
        EquipmentPlanRuntimeState state = context.getEquipmentPlanRuntimeState();
        List<String> physicalCodes = state.getSandBlastRepairPrecisionPlanMap().entrySet().stream()
                .filter(entry -> Objects.equals(planId, entry.getValue())).map(Map.Entry::getKey)
                .collect(Collectors.toList());
        for (String physical : physicalCodes) {
            for (MachineScheduleDTO side : physicalMachines(context, physical)) {
                CleaningPlanTimeline original = state.getSandBlastRepairOriginalTimelineMap()
                        .remove(side.getMachineCode());
                if (original == null) {
                    continue;
                }
                for (MachineCleaningWindowDTO window : side.getCleaningWindowList()) {
                    if (!Objects.equals(planId, window.getSandBlastRepairPrecisionPlanId())) {
                        continue;
                    }
                    window.setSandBlastRepairPrecisionPlanId(null);
                    window.setSandBlastInspectionStartTime(original.getInspectionStartTime());
                    window.setReadyTime(original.getInspectionEndTime());
                    LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                            context.getScheduleWindowShifts(), original.getInspectionEndTime());
                    window.setSandBlastInspectionShiftStartTime(shift == null ? null : shift.getShiftStartDateTime());
                }
            }
            state.getSandBlastRepairPrecisionPlanMap().remove(physical);
        }
    }

    /**
     * 取得物理机台的全部已加载运行侧，按编码稳定处理。
     *
     * @param context 当前机台运行态
     * @param physical 物理机台编码
     * @return 全部运行侧
     */
    private static List<MachineScheduleDTO> physicalMachines(LhScheduleContext context, String physical) {
        return context.getMachineScheduleMap().values().stream().filter(Objects::nonNull)
                .filter(machine -> samePhysical(physical, machine.getMachineCode()))
                .sorted(Comparator.comparing(MachineScheduleDTO::getMachineCode)).collect(Collectors.toList());
    }

    /**
     * 判断运行侧是否属于指定物理机台。
     *
     * @param physical 物理机台编码
     * @param machineCode 物理或运行侧编码
     * @return 同一物理机台时返回true
     */
    private static boolean samePhysical(String physical, String machineCode) {
        return StringUtils.equals(physical, LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }

    /**
     * 判断两个有效半开区间是否严格相交。
     *
     * @param start 第一段开始
     * @param end 第一段结束
     * @param otherStart 第二段开始
     * @param otherEnd 第二段结束
     * @return 存在正时长交集时返回true
     */
    private static boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return start != null && end != null && otherStart != null && otherEnd != null
                && start.before(end) && otherStart.before(otherEnd)
                && start.before(otherEnd) && end.after(otherStart);
    }
}
