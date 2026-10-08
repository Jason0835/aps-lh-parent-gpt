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
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionTimelinePlan;
import com.zlt.aps.lh.engine.strategy.support.StructureSwitchSchedulingPolicy;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 设备计划时间适配；不重新判断精度日期、时长、预热或生产优先级。 */
@Component
public class LhEquipmentPlanTimelineResolver {

    /** 精度保养后的公共首检固定2小时，与SKU产速无关。 */
    public static final long PRECISION_INSPECTION_SECONDS = 2L * 3600L;

    /** @param context 排程上下文 @param decision 有效判定 @param inputVersion 输入指纹 @return 候选时间约束 */
    public EquipmentPlanTimeline resolvePrecision(LhScheduleContext context,
                                                  PrecisionMaintenanceJudgeDTO decision, String inputVersion) {
        FirstInspectionTimelinePlan inspection = FirstInspectionAllocationUtil.buildFixedDurationTimelinePlan(
                context.getScheduleWindowShifts(), decision.getPrecisionEndTime(), PRECISION_INSPECTION_SECONDS);
        return new EquipmentPlanTimeline(decision, inspection, inputVersion);
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
     * 两项完整耗时并行取最大完成点，共用最后2小时首检，不再向切换总时长重复追加首检。
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
        long inspectionMillis = PRECISION_INSPECTION_SECONDS * 1000L;
        Date resume = new Date(Math.max(original.getMaintenanceEndTime().getTime() + inspectionMillis, end.getTime()));
        EquipmentPlanTimeline parallel = original.withInspection(new Date(resume.getTime() - inspectionMillis), resume);
        String conflict = this.resolveAdditionalOverlapReason(context, parallel, start);
        return StringUtils.isNotBlank(conflict)
                ? EquipmentPlanOverlapResult.unsupported(conflict) : EquipmentPlanOverlapResult.supported(parallel);
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
