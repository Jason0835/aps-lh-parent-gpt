package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.engine.strategy.support.RepairPlanResult;

import com.zlt.aps.lh.api.domain.dto.PrecisionMaintenanceJudgeDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanArrangement;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanOverlapResult;
import com.zlt.aps.lh.engine.strategy.support.EquipmentPlanTimeline;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备计划时间适配，保留原始精度区间并衔接维修、上胶囊预热及公共首检。
 */
@Component
public class LhEquipmentPlanTimelineResolver {

    /**
     * 复用无状态清洗组合解析器。
     */
    private final LhCleaningOverlapResolver cleaningOverlapResolver = new LhCleaningOverlapResolver();

    /** @param context 排程上下文 @param decision 有效判定 @param inputVersion 输入指纹 @return 候选时间约束 */
    public EquipmentPlanTimeline resolvePrecision(LhScheduleContext context,
                                                  PrecisionMaintenanceJudgeDTO decision, String inputVersion) {
        Date maintenanceReadyTime = LhScheduleTimeUtil.addMinutes(decision.getPrecisionEndTime(),
                LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
        return new EquipmentPlanTimeline(decision, maintenanceReadyTime, inputVersion);
    }

    /**
     * 为普通精度冻结维修与精度并行后的准备边界，保留精度原始本体起止。
     * 资格及每日额度仍由原精度判定负责，未支持的停机由正式接受门禁拒绝。
     *
     * @param context 已归一实际维修时间的上下文
     * @param decision 有效精度判定
     * @param inputVersion 输入指纹
     * @return 合并准备时间后的只读时间约束
     */
    public EquipmentPlanTimeline resolvePrecisionWithRepair(LhScheduleContext context,
            PrecisionMaintenanceJudgeDTO decision, String inputVersion) {
        EquipmentPlanTimeline precision = this.resolvePrecision(context, decision, inputVersion);
        RepairPlanResult repair = LhRepairOverlapResolver.resolvePrecisionPreparation(context,
                precision.getPhysicalMachineCode(), precision.getMaintenanceStartTime(), precision.getMaintenanceReadyTime());
        return new EquipmentPlanTimeline(decision, repair.getProductionNotBefore(), inputVersion);
    }

    /**
     * 重叠场景统一分派入口。先读取已生效单场景结果，再解析组合，最后由原策略分配公共首检数量。
     * 接入精度、05维修与普通换模、换活字块；其他组合仍须显式校验。
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 候选SKU，试制、量试等复合场景留待后续接入
     * @param startTime 真实切换开始
     * @param endTime 真实切换总时长结束，已包含原首检
     * @param scheduleType 既有排程动作类型
     * @return 无副作用的组合结果
     */
    public EquipmentPlanOverlapResult resolveChangeoverOverlap(LhScheduleContext context, MachineScheduleDTO machine,
            SkuScheduleDTO sku, Date startTime, Date endTime, String scheduleType) {
        if (ScheduleTypeEnum.NEW_SPEC.getCode().equals(scheduleType)) {
            return this.resolveMouldChangeOverlap(context, machine, sku, startTime, endTime);
        }
        if (ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType)) {
            return this.resolveTypeBlockChangeOverlap(context, machine, sku, startTime, endTime);
        }
        boolean overlapsPrecision = Objects.nonNull(machine) && machine.getMaintenanceWindowList().stream()
                .filter(Objects::nonNull).filter(MachineMaintenanceWindowDTO::isEquipmentPlanManaged)
                .anyMatch(window -> this.overlaps(startTime, endTime, window.getMaintenanceStartTime(), window.getProductionResumeTime()));
        return overlapsPrecision ? EquipmentPlanOverlapResult.unsupported("精度与当前业务动作的重叠尚未接入")
                : EquipmentPlanOverlapResult.noOverlap();
    }

    /** @param context 上下文 @param machine 机台 @param sku SKU @param start 切换开始 @param end 含首检结束 @return 精度与换模组合 */
    public EquipmentPlanOverlapResult resolveMouldChangeOverlap(LhScheduleContext context, MachineScheduleDTO machine,
            SkuScheduleDTO sku, Date start, Date end) {
        return this.resolvePrecisionChangeover(context, machine, sku, start, end, ScheduleTypeEnum.NEW_SPEC.getCode());
    }

    /** @param context 上下文 @param machine 机台 @param sku SKU @param start 切换开始 @param end 含首检结束 @return 精度与换活字块组合 */
    public EquipmentPlanOverlapResult resolveTypeBlockChangeOverlap(LhScheduleContext context, MachineScheduleDTO machine,
            SkuScheduleDTO sku, Date start, Date end) {
        return this.resolvePrecisionChangeover(context, machine, sku, start, end, ScheduleTypeEnum.TYPE_BLOCK.getCode());
    }

    /**
     * 合并精度与切换的准备约束，换活字块单独保留可倒推首检的准备下限。
     * 换活字块总时长已包含首检，数量确定前同时返回准备下限和完整切换结束边界。
     * 正规换模继续从全部准备结束后执行公共首检。
     *
     * @param context 排程上下文
     * @param machine 候选机台
     * @param sku 待承接首检的SKU
     * @param start 切换实际开始时间
     * @param end 切换实际完成时间
     * @param scheduleType 切换类型
     * @return 组合准备边界或明确拒绝原因
     */
    private EquipmentPlanOverlapResult resolvePrecisionChangeover(LhScheduleContext context, MachineScheduleDTO machine,
            SkuScheduleDTO sku, Date start, Date end, String scheduleType) {
        if (Objects.isNull(machine) || Objects.isNull(start) || Objects.isNull(end) || !start.before(end)) {
            return EquipmentPlanOverlapResult.noOverlap();
        }
        List<MachineMaintenanceWindowDTO> windows = machine.getMaintenanceWindowList().stream()
                .filter(Objects::nonNull).filter(MachineMaintenanceWindowDTO::isEquipmentPlanManaged)
                .filter(window -> this.overlaps(start, end, window.getMaintenanceStartTime(), window.getProductionResumeTime()))
                .collect(Collectors.toList());
        if (windows.isEmpty()) {
            return EquipmentPlanOverlapResult.noOverlap();
        }
        if (windows.size() != 1) {
            return this.resolveMaintenanceOverlap();
        }
        EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState().getArrangementMap()
                .get(windows.get(0).getPrecisionPlanId());
        if (Objects.isNull(arrangement) || !arrangement.isEffective()) {
            return EquipmentPlanOverlapResult.unsupported("精度重叠缺少有效公共安排，禁止使用候选或已撤销计划");
        }
        if (Objects.isNull(sku) || ConstructionStageEnum.TRIAL.getCode().equals(sku.getConstructionStage())
                || FirstInspectionQtyUtil.isMassTrialQuantityFirstInspection(sku, scheduleType)
                || StructureSwitchSchedulingPolicy.requiresLargeTimeline(context, sku, start)) {
            return EquipmentPlanOverlapResult.unsupported("精度并行与试制、量试或大换英寸组合尚未接入");
        }
        EquipmentPlanTimeline original = arrangement.getTimeline();
        boolean inspectionAllocated = context.getScheduleResultList().stream()
                .filter(result -> ShiftFieldUtil.resolveScheduledQty(result) > 0)
                .map(result -> context.getFirstInspectionResultPlanMap().get(result))
                .filter(Objects::nonNull)
                .anyMatch(plan -> Objects.equals(original.getPrecisionPlanId(), plan.getEquipmentPlanId()));
        if (inspectionAllocated) {
            return EquipmentPlanOverlapResult.unsupported("精度公共首检已被有效结果承接，不得再次分配或移动");
        }
        Date inspectionStart = this.resolvePrecisionInspectionStartTime(original.getMaintenanceReadyTime(), end);
        Date preparationStart = start.before(original.getMaintenanceStartTime())
                ? start : original.getMaintenanceStartTime();
        RepairPlanResult repair = LhRepairOverlapResolver.resolvePrecisionPreparation(context,
                original.getPhysicalMachineCode(), preparationStart, inspectionStart);
        if (repair.getOverlapStatus() == RepairPlanResult.OverlapStatus.UNSUPPORTED) {
            return EquipmentPlanOverlapResult.unsupported(repair.getReason());
        }
        inspectionStart = this.resolveInspectionPreparationTime(
                context, original.getMaintenanceReadyTime(), repair, scheduleType);
        // 数量尚未确定时，换活字块返回首检下限及完整切换边界，实际区间由数量分摊工具冻结。
        EquipmentPlanTimeline parallel = original.withInspection(inspectionStart, repair.getProductionNotBefore());
        String conflict = this.resolveAdditionalOverlapReason(context, parallel, start, true);
        return StringUtils.isNotBlank(conflict)
                ? EquipmentPlanOverlapResult.unsupported(conflict) : EquipmentPlanOverlapResult.supported(parallel);
    }

    /**
     * 合并真实精度及维修预热下限，换活字块的含首检结束时间单独保留。
     *
     * @param context 排程参数上下文
     * @param maintenanceReadyTime 精度及预热完成时间
     * @param repair 覆盖完整切换区间的维修解析结果
     * @param scheduleType 实际切换类型
     * @return 公共首检允许开始的准备下限
     */
    private Date resolveInspectionPreparationTime(LhScheduleContext context, Date maintenanceReadyTime,
            RepairPlanResult repair, String scheduleType) {
        if (!ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType)) {
            return repair.getProductionNotBefore();
        }
        Date inspectionNotBefore = maintenanceReadyTime;
        for (MdmDevicePlanShut source : repair.getSourcePlans()) {
            Date repairReady = LhScheduleTimeUtil.addMinutes(source.getEndDate(),
                    LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
            if (repairReady.after(inspectionNotBefore)) {
                inspectionNotBefore = repairReady;
            }
        }
        return inspectionNotBefore;
    }

    /**
     * 按候选冻结的首检时长复核实际开始边界，换活字块共用倒推计算。
     *
     * @param context 排程上下文
     * @param window 当前有效精度窗口
     * @param changeoverStartTime 真实切换开始时间
     * @param changeoverEndTime 包含首检的切换结束时间，独立精度为空
     * @param inspection 冻结的计件首检计划
     * @param scheduleType 实际切换类型
     * @return 当前组合应采用的首检开始时间
     */
    public Date resolvePrecisionInspectionStartTime(LhScheduleContext context, MachineMaintenanceWindowDTO window,
            Date changeoverStartTime, Date changeoverEndTime, FirstInspectionAllocationPlan inspection,
            String scheduleType) {
        Date readyTime = this.resolvePrecisionInspectionStartTime(window.getMaintenanceReadyTime(), changeoverEndTime);
        if (!ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType) || Objects.isNull(changeoverEndTime)) {
            return readyTime;
        }
        Date preparationStart = changeoverStartTime.before(window.getMaintenanceStartTime())
                ? changeoverStartTime : window.getMaintenanceStartTime();
        RepairPlanResult repair = LhRepairOverlapResolver.resolvePrecisionPreparation(
                context, window.getMachineCode(), preparationStart, readyTime);
        Date inspectionNotBefore = this.resolveInspectionPreparationTime(
                context, window.getMaintenanceReadyTime(), repair, scheduleType);
        return FirstInspectionAllocationUtil.resolvePrecisionTypeBlockInspectionStartTime(
                inspectionNotBefore, changeoverEndTime, inspection.getInspectionDurationSeconds(),
                inspection.getCountingShift());
    }

    /**
     * 准备预演、正式组合及提交校验共用首检起点；首检不得占用上胶囊及预热时间。
     * @param maintenanceReadyTime 已冻结的精度上胶囊及预热完成时间
     * @param changeoverEndTime 切换结束；独立精度传空
     * @return 两项准备均结束、可以开始计件首检的时刻
     */
    public Date resolvePrecisionInspectionStartTime(Date maintenanceReadyTime, Date changeoverEndTime) {
        Date inspectionStart = Objects.nonNull(changeoverEndTime) && changeoverEndTime.after(maintenanceReadyTime)
                ? changeoverEndTime : maintenanceReadyTime;
        return new Date(inspectionStart.getTime());
    }

    /**
     * 首检数量确定后复核真实首检结束前的组合占用，不能只检查尚未分配数量时的准备边界。
     *
     * @param context 上下文
     * @param inspection 参数计件首检
     * @param changeStart 切换开始，无切换时为空
     * @return 尚未覆盖组合原因，无冲突为空
     */
    public String resolveInspectionOverlapReason(LhScheduleContext context, FirstInspectionAllocationPlan inspection,
            Date changeStart) {
        if (!inspection.isValid() || Objects.isNull(inspection.getEquipmentPlanId())) {
            return inspection.getInvalidReason();
        }
        EquipmentPlanArrangement arrangement = context.getEquipmentPlanRuntimeState().getArrangementMap().get(inspection.getEquipmentPlanId());
        if (Objects.isNull(arrangement) || !arrangement.isEffective()) {
            return "精度计件首检缺少有效设备安排";
        }
        EquipmentPlanTimeline actual = arrangement.getTimeline().withInspection(
                inspection.getInspectionStartTime(), inspection.getInspectionEndTime());
        return this.resolveAdditionalOverlapReason(context, actual,
                Objects.nonNull(changeStart) ? changeStart : actual.getMaintenanceStartTime(), true);
    }

    /**
     * 专用清洗组合沿用严格重叠校验，使用完整区间复核新增等待和首检占用。
     * 普通维修精度链路由内部带场景参数的入口处理。
     *
     * @param context 上下文
     * @param timeline 组合时间轴
     * @param changeStart 切换开始
     * @return 未覆盖组合原因，无其他占用时为空
     */
    public String resolveAdditionalOverlapReason(LhScheduleContext context, EquipmentPlanTimeline timeline,
            Date changeStart) {
        return this.resolveAdditionalOverlapReason(context, timeline, changeStart, false);
    }

    /**
     * 复核组合实际区间，仅普通精度链路允许已完成准备的05维修并行。
     *
     * @param context 排程上下文
     * @param timeline 已冻结准备和首检的时间轴
     * @param changeStart 切换或精度开始
     * @param allowPlannedRepair 是否属于本次已接入的普通维修精度组合
     * @return 未覆盖或准备未完成的原因，无冲突时为空
     */
    private String resolveAdditionalOverlapReason(LhScheduleContext context, EquipmentPlanTimeline timeline,
            Date changeStart, boolean allowPlannedRepair) {
        Date start = changeStart.before(timeline.getMaintenanceStartTime()) ? changeStart : timeline.getMaintenanceStartTime();
        Date end = timeline.getProductionResumeTime();
        String physical = timeline.getPhysicalMachineCode();
        boolean capsuleOverlap = context.getCapsuleReplacementTimeWindowMap().values().stream()
                .filter(Objects::nonNull).anyMatch(window -> this.samePhysicalMachine(physical, window.getPhysicalMachineCode())
                        && this.overlaps(start, end, window.getReplacementStartTime(), window.getReplacementEndTime()));
        if (capsuleOverlap) {
            return "精度并行与换胶囊组合尚未接入";
        }
        RepairPlanResult repairOverlap = allowPlannedRepair
                ? LhRepairOverlapResolver.resolvePrecisionPreparation(context, physical, start, end)
                : LhRepairOverlapResolver.resolveWithPrecision(context.getDevicePlanShutList(), physical, start, end,
                        "精度并行与停机/维修组合尚未接入，停机类型=");
        if (repairOverlap.getOverlapStatus() == RepairPlanResult.OverlapStatus.UNSUPPORTED) {
            return repairOverlap.getReason();
        }
        if (allowPlannedRepair && repairOverlap.getSourcePlans().stream().anyMatch(plan ->
                LhScheduleTimeUtil.addMinutes(plan.getEndDate(), LhScheduleTimeUtil.getCapsulePreheatMinutes(context))
                        .after(timeline.getInspectionStartTime()))) {
            return "计划性维修及预热尚未完成，不能在公共首检期间开始或继续维修";
        }
        for (MachineScheduleDTO side : context.getMachineScheduleMap().values()) {
            if (Objects.isNull(side) || !this.samePhysicalMachine(physical, side.getMachineCode())) {
                continue;
            }
            if (!cleaningOverlapResolver.resolveWithPrecisionForSide(side, start, end).isEmpty()) {
                return "精度并行与清洗组合尚未接入";
            }
            for (MachineMaintenanceWindowDTO window : side.getMaintenanceWindowList()) {
                if (Objects.nonNull(window) && !Objects.equals(timeline.getPrecisionPlanId(), window.getPrecisionPlanId())
                        && this.overlaps(start, end, window.getMaintenanceStartTime(),
                                Objects.nonNull(window.getProductionResumeTime()) ? window.getProductionResumeTime() : window.getMaintenanceEndTime())) {
                    return this.resolveMaintenanceOverlap().getReason();
                }
            }
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (this.samePhysicalMachine(physical, result.getLhMachineCode())
                    && context.getScheduleWindowShifts().stream().anyMatch(shift -> {
                        Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                        return Objects.nonNull(quantity) && quantity > 0 && this.overlaps(start, end,
                                ShiftFieldUtil.getShiftStartTime(result, shift.getShiftIndex()),
                                ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex()));
                    })) {
                return "精度并行区间与已提交生产重叠，不能改写已扣账结果";
            }
        }
        return null;
    }

    /** @param physical 物理机台 @param machineCode 运行态编码 @return 是否同一物理机台 */
    private boolean samePhysicalMachine(String physical, String machineCode) {
        return StringUtils.equals(physical, LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }

    /** 半开区间相交；边界相等不视为重叠。 */
    private boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return Objects.nonNull(start) && Objects.nonNull(end) && Objects.nonNull(otherStart) && Objects.nonNull(otherEnd)
                && start.before(end) && otherStart.before(otherEnd) && start.before(otherEnd) && end.after(otherStart);
    }

    // 扩展顺序：单场景结果 -> 重叠计算 -> 公共首检 -> 时间计划提交。

    /** 预留维修单场景判定，现有维修流程继续执行。 */
    public void resolveRepairMaintenanceList() {
    }

    /** 预留清洗单场景判定，现有清洗流程继续执行。 */
    public void resolveCleaningMaintenanceList() {
    }

    /** 预留维护间重叠计算。 */
    public EquipmentPlanOverlapResult resolveMaintenanceOverlap() {
        return EquipmentPlanOverlapResult.unsupported("维护之间的重叠尚未接入");
    }
}
