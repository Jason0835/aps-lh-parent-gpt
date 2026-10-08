package com.zlt.aps.lh.service.impl;

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

/** 设备计划时间适配；保留原始精度区间，按既有参数衔接上胶囊、预热及公共首检。 */
@Component
public class LhEquipmentPlanTimelineResolver {

    /** @param context 排程上下文 @param decision 有效判定 @param inputVersion 输入指纹 @return 候选时间约束 */
    public EquipmentPlanTimeline resolvePrecision(LhScheduleContext context,
                                                  PrecisionMaintenanceJudgeDTO decision, String inputVersion) {
        Date maintenanceReadyTime = LhScheduleTimeUtil.addMinutes(decision.getPrecisionEndTime(),
                LhScheduleTimeUtil.getCapsulePreheatMinutes(context));
        return new EquipmentPlanTimeline(decision, maintenanceReadyTime, inputVersion);
    }

    /**
     * 重叠场景统一分派入口。先读取已生效单场景结果，再解析组合，最后由原策略分配公共首检数量。
     * 仅接入精度与普通换模、换活字块；新增组合必须显式实现，不能默认放行。
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
     * 保养后上胶囊及预热完成与切换结束取最大边界，再正向执行一次公共首检。
     * 原始保养开始/结束始终保留；不同开始时刻比较绝对完成点，不向前移动已合法的切换窗口。
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
        // 此时尚未选定首检参数及条数，仅返回准备边界；实际首检区间由原数量分摊工具生成。
        EquipmentPlanTimeline parallel = original.withInspection(inspectionStart, inspectionStart);
        String conflict = this.resolveAdditionalOverlapReason(context, parallel, start);
        return StringUtils.isNotBlank(conflict)
                ? EquipmentPlanOverlapResult.unsupported(conflict) : EquipmentPlanOverlapResult.supported(parallel);
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
     * @param context 上下文 @param inspection 参数计件首检 @param changeStart 切换开始，无切换时为空
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
                Objects.nonNull(changeStart) ? changeStart : actual.getMaintenanceStartTime());
    }

    /**
     * 其他场景的扩展入口。当前只报告未覆盖组合，后续接入维修、清洗等时在此明确分派。
     * 使用组合后的完整区间，防止切换较晚完成时把新增的等待和首检时间静默压到其他占用上。
     * @param context 上下文 @param timeline 组合时间轴 @param changeStart 切换开始
     * @return 未覆盖组合原因，无其他占用时为空
     */
    public String resolveAdditionalOverlapReason(LhScheduleContext context, EquipmentPlanTimeline timeline,
            Date changeStart) {
        Date start = changeStart.before(timeline.getMaintenanceStartTime()) ? changeStart : timeline.getMaintenanceStartTime();
        Date end = timeline.getProductionResumeTime();
        String physical = timeline.getPhysicalMachineCode();
        boolean capsuleOverlap = context.getCapsuleReplacementTimeWindowMap().values().stream()
                .filter(Objects::nonNull).anyMatch(window -> this.samePhysicalMachine(physical, window.getPhysicalMachineCode())
                        && this.overlaps(start, end, window.getReplacementStartTime(), window.getReplacementEndTime()));
        if (capsuleOverlap) {
            return "精度并行与换胶囊组合尚未接入";
        }
        for (MdmDevicePlanShut stop : context.getDevicePlanShutList()) {
            if (Objects.nonNull(stop) && this.samePhysicalMachine(physical, stop.getMachineCode())
                    && this.overlaps(start, end, stop.getBeginDate(), stop.getEndDate())) {
                return "精度并行与停机/维修组合尚未接入，停机类型=" + stop.getMachineStopType();
            }
        }
        for (MachineScheduleDTO side : context.getMachineScheduleMap().values()) {
            if (Objects.isNull(side) || !this.samePhysicalMachine(physical, side.getMachineCode())) {
                continue;
            }
            for (MachineCleaningWindowDTO cleaning : side.getCleaningWindowList()) {
                if (Objects.nonNull(cleaning) && this.overlaps(start, end, cleaning.getCleanStartTime(), cleaning.getReadyTime())) {
                    return "精度并行与清洗组合尚未接入";
                }
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
