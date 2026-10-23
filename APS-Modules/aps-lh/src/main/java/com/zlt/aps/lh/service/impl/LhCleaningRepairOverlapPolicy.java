package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.CleaningPlanRuntimeState;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.LhMouldCodeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 清洗、计划性维修及真实切换的公共组合规则。
 * 干冰沿用既有组合；喷砂仅保留已确认完整原模具组的换活字块来源，并共用一次切换首检。
 *
 * @author APS
 * @date 2026/10/23
 */
public final class LhCleaningRepairOverlapPolicy {

    /**
     * 禁止创建无状态策略实例。
     */
    private LhCleaningRepairOverlapPolicy() {
    }

    /**
     * 判断清洗来源是否与计划性维修本体相交，故障、预热和端点相接均不取得组合资格。
     *
     * @param context 当前批次固定维修来源
     * @param window 已通过清洗来源校验的窗口
     * @return 清洗与05维修本体严格相交时返回true
     */
    public static boolean hasRepairOverlap(LhScheduleContext context, MachineCleaningWindowDTO window) {
        return context != null && window != null
                && (MachineCleaningOverlapUtil.isDryIceCleaning(window)
                || MachineCleaningOverlapUtil.isSandBlastCleaning(window))
                && LhPlannedRepairResolver.resolveCapacityWindows(context, context.getDevicePlanShutList(),
                window.getLhCode()).stream().anyMatch(repair -> MachineCleaningOverlapUtil.isOverlap(
                window, repair.getMaintenanceStartTime(), repair.getMaintenanceEndTime()));
    }

    /**
     * 识别已加载的同原模具换活字块指令，初始化只保留该来源和喷砂序号，正式结果仍须重验。
     *
     * @param context 固定历史指令及模具关系
     * @param window 喷砂来源及完整原模具组
     * @return 存在精确同机同前料并支持原完整模具组的换活字块指令时返回true
     */
    public static boolean hasPlannedSandBlastHandoff(LhScheduleContext context, MachineCleaningWindowDTO window) {
        if (!MachineCleaningOverlapUtil.isSandBlastCleaning(window) || !hasRepairOverlap(context, window)) {
            return false;
        }
        return context.getHistoricalReverseMouldChangePlanList().stream().filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals("02", plan.getChangeMouldType()))
                .filter(plan -> StringUtils.equals(
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(plan.getLhMachineCode()),
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(window.getLhCode())))
                .filter(plan -> StringUtils.equals(window.getSourceMaterialCode(), plan.getBeforeMaterialCode()))
                .filter(plan -> plan.getPlanDate() != null && Objects.equals(
                        LhScheduleTimeUtil.clearTime(plan.getPlanDate()),
                        LhScheduleTimeUtil.clearTime(window.getCleanStartTime())))
                .anyMatch(plan -> supportsOriginalMoulds(context, window, plan.getAfterMaterialCode()));
    }

    /**
     * 校验后料支持当前来源的每一套原模具，不能借用同SKU其他模具冒充本次喷砂。
     *
     * @param context 已加载物料模具关系
     * @param source 喷砂来源
     * @param afterMaterialCode 后料
     * @return 原完整模具组非空且全部支持时返回true
     */
    private static boolean supportsOriginalMoulds(LhScheduleContext context, MachineCleaningWindowDTO source,
            String afterMaterialCode) {
        Set<String> original = new java.util.LinkedHashSet<>(LhMouldCodeUtil.splitMouldCode(source.getMouldCode()));
        Set<String> supported = context.getSkuMouldRelMap().getOrDefault(afterMaterialCode, Collections.emptyList())
                .stream().map(relation -> relation.getMouldCode()).filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        return !original.isEmpty() && supported.containsAll(original);
    }

    /**
     * 将来源窗口投影到真实切换起点，保留清洗原时长及原班次名额。
     * 清洗不得提前覆盖前料生产，也不得通过投影跨日或转移清洗班次额度。
     * 喷砂维持两档固定开始时刻，切换晚于喷砂起点时不能平移喷砂取得本组合资格。
     *
     * @param context 当前批次
     * @param source 已冻结的原始清洗来源
     * @param switchStart 真实切换起点
     * @param switchEnd 真实切换自身完成时间
     * @param typeBlock 是否保留独立清洗的换活字块动作
     * @return 满足三项真实重叠时的独立清洗窗口，否则返回空
     */
    public static MachineCleaningWindowDTO projectWindow(LhScheduleContext context,
            MachineCleaningWindowDTO source, Date switchStart, Date switchEnd, boolean typeBlock) {
        if (source == null || source.getSourcePlanId() == null || source.isOffMachineCleaning()
                || source.getCleanEndTime() == null || source.getCleanStartTime() == null
                || !source.getCleanStartTime().before(source.getCleanEndTime())
                || context == null
                || (!MachineCleaningOverlapUtil.isDryIceCleaning(source)
                && !(typeBlock && hasPlannedSandBlastHandoff(context, source)))
                || !MachineCleaningOverlapUtil.isOverlap(source, switchStart, switchEnd)) {
            return null;
        }
        if (MachineCleaningOverlapUtil.isSandBlastCleaning(source)
                && switchStart.after(source.getCleanStartTime())) {
            return null;
        }
        boolean repairOverlapsSwitch = LhPlannedRepairResolver.resolveCapacityWindows(
                context, context.getDevicePlanShutList(), source.getLhCode()).stream()
                .anyMatch(repair -> MachineCleaningOverlapUtil.isOverlap(source,
                        repair.getMaintenanceStartTime(), repair.getMaintenanceEndTime())
                        && overlaps(switchStart, switchEnd,
                        repair.getMaintenanceStartTime(), repair.getMaintenanceEndTime()));
        if (!repairOverlapsSwitch) {
            return null;
        }
        if (context.getMachineScheduleMap().containsKey(source.getLhCode())
                && context.getMachineScheduleMap().get(source.getLhCode()).getMaintenanceWindowList().stream()
                .filter(Objects::nonNull).filter(MachineMaintenanceWindowDTO::isEquipmentPlanManaged)
                .anyMatch(maintenance -> overlaps(switchStart, switchEnd,
                        maintenance.getMaintenanceStartTime(), maintenance.getProductionResumeTime()))) {
            // 公共精度首检有独立冻结契约，本次三项组合不能覆盖它的时间轴。
            return null;
        }
        Date cleanStart = source.getCleanStartTime().before(switchStart) ? switchStart : source.getCleanStartTime();
        LhShiftConfigVO sourceShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), source.getCleanStartTime());
        LhShiftConfigVO targetShift = LhScheduleTimeUtil.resolveShiftByTime(
                context.getScheduleWindowShifts(), cleanStart);
        if (sourceShift == null || targetShift == null
                || !Objects.equals(sourceShift.getShiftStartDateTime(), targetShift.getShiftStartDateTime())) {
            return null;
        }
        if (typeBlock) {
            Date reservedStart = context.getCleaningPlanRuntimeState().getIndependentQuotaStartMap()
                    .get(sourceKey(source));
            LhShiftConfigVO reservedShift = reservedStart == null ? null : LhScheduleTimeUtil.resolveShiftByTime(
                    context.getScheduleWindowShifts(), reservedStart);
            if (reservedShift == null || !Objects.equals(
                    reservedShift.getShiftStartDateTime(), targetShift.getShiftStartDateTime())) {
                return null;
            }
        }
        MachineCleaningWindowDTO projected = CleaningPlanRuntimeState.copyWindow(source);
        long duration = source.getCleanEndTime().getTime() - source.getCleanStartTime().getTime();
        projected.setCleanStartTime((Date) cleanStart.clone());
        projected.setCleanEndTime(new Date(cleanStart.getTime() + duration));
        projected.setReadyTime((Date) projected.getCleanEndTime().clone());
        if (MachineCleaningOverlapUtil.isSandBlastCleaning(source)) {
            // 三项组合的唯一首检由换活字块主链分摊，不能再领取独立喷砂首检量。
            projected.setSandBlastFirstInspectionQty(null);
            projected.setSandBlastInspectionShiftStartTime(null);
        }
        return projected;
    }

    /**
     * 读取本运行侧的归一三项组合来源，换活字块另校验独立清洗名额。
     * 候选不修改固定事实或资源，正规换模覆盖不额外消费独立清洗名额。
     *
     * @param context 当前批次
     * @param machineCode 精确运行侧机台
     * @param switchStart 真实切换开始
     * @param switchEnd 真实切换自身完成
     * @param typeBlock 是否需要独立清洗名额
     * @return 当前候选的独立组合清洗窗口
     */
    public static List<MachineCleaningWindowDTO> resolveWindows(LhScheduleContext context, String machineCode,
            Date switchStart, Date switchEnd, boolean typeBlock) {
        if (context == null || StringUtils.isEmpty(machineCode) || switchStart == null || switchEnd == null) {
            return Collections.emptyList();
        }
        List<MachineCleaningWindowDTO> sources = context.getCleaningPlanRuntimeState().getSourceWindowMap()
                .get(machineCode);
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }
        return sources.stream()
                .map(source -> projectWindow(context, source, switchStart, switchEnd, typeBlock))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 计算组合准备完成点，清洗、维修及一次预热、切换并行取最晚结束时间。
     *
     * @param context 当前批次
     * @param machineCode 当前运行侧
     * @param switchStart 真实切换开始
     * @param switchEnd 真实切换自身完成
     * @param typeBlock 是否需要保留独立清洗
     * @return 未命中三项组合时返回空，命中时返回实际准备下限
     */
    public static Date resolvePreparationEnd(LhScheduleContext context, String machineCode,
            Date switchStart, Date switchEnd, boolean typeBlock) {
        List<MachineCleaningWindowDTO> windows = resolveWindows(
                context, machineCode, switchStart, switchEnd, typeBlock);
        if (windows.isEmpty()) {
            return null;
        }
        Date readyTime = windows.stream().map(MachineCleaningWindowDTO::getCleanEndTime)
                .max(Date::compareTo).orElse(switchEnd);
        if (switchEnd.after(readyTime)) {
            readyTime = switchEnd;
        }
        for (MachineMaintenanceWindowDTO repair : LhPlannedRepairResolver.resolveCapacityWindows(
                context, context.getDevicePlanShutList(), machineCode)) {
            if (overlaps(switchStart, switchEnd, repair.getMaintenanceStartTime(), repair.getMaintenanceEndTime())
                    && repair.getProductionResumeTime().after(readyTime)) {
                readyTime = repair.getProductionResumeTime();
            }
        }
        return (Date) readyTime.clone();
    }

    /**
     * 解析三项组合需要正向首检的准备下限，供候选、正式排量及冻结时间轴共同使用。
     *
     * @param context 当前批次
     * @param machineCode 当前运行侧
     * @param scheduleType 实际切换场景
     * @param switchStart 真实切换开始
     * @param switchEnd 真实切换自身完成
     * @return 正规换模组合或准备超出换活字块总时长时返回下限，其他场景返回空
     */
    public static Date resolveForwardInspectionReady(LhScheduleContext context, String machineCode,
            String scheduleType, Date switchStart, Date switchEnd) {
        if (!ScheduleTypeEnum.NEW_SPEC.getCode().equals(scheduleType)
                && !ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType)) {
            return null;
        }
        Date readyTime = resolvePreparationEnd(context, machineCode, switchStart, switchEnd,
                ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType));
        return readyTime != null && (!ScheduleTypeEnum.TYPE_BLOCK.getCode().equals(scheduleType)
                || readyTime.after(switchEnd)) ? readyTime : null;
    }

    /**
     * 查找已正式提交且有正量的真实组合动作，候选和零量结果不能覆盖收尾处置。
     *
     * @param context 已提交结果及固定来源
     * @param machineCode 清洗运行侧
     * @param source 原始或投影后的清洗来源
     * @return 最早兑现本来源的切换结果，没有兑现时返回空
     */
    public static LhScheduleResult resolveCommittedSwitch(LhScheduleContext context, String machineCode,
            MachineCleaningWindowDTO source) {
        if (context == null || source == null) {
            return null;
        }
        // 必须从初始化来源还原候选；不能用已经平移的清洗窗口再次扩大匹配范围。
        MachineCleaningWindowDTO original = context.getCleaningPlanRuntimeState().getSourceWindowMap()
                .getOrDefault(machineCode, Collections.emptyList()).stream()
                .filter(window -> Objects.equals(source.getSourcePlanId(), window.getSourcePlanId()))
                .findFirst().orElse(null);
        if (!hasRepairOverlap(context, original)) {
            return null;
        }
        return context.getScheduleResultList().stream().filter(Objects::nonNull)
                .filter(result -> CleaningDispositionResolver.coversMachineSide(result.getLhMachineCode(), machineCode))
                .filter(result -> "1".equals(result.getIsChangeMould()) || "1".equals(result.getIsTypeBlock()))
                .filter(result -> result.getDailyPlanQty() != null && result.getDailyPlanQty() > 0)
                .filter(result -> result.getMouldChangeStartTime() != null)
                .filter(result -> !MachineCleaningOverlapUtil.isSandBlastCleaning(original)
                        || ("1".equals(result.getIsTypeBlock())
                        && supportsOriginalMoulds(context, original, result.getMaterialCode())
                        && new java.util.LinkedHashSet<>(LhMouldCodeUtil.splitMouldCode(result.getMouldCode()))
                        .equals(new java.util.LinkedHashSet<>(LhMouldCodeUtil.splitMouldCode(original.getMouldCode())))))
                .filter(result -> projectWindow(context, original, result.getMouldChangeStartTime(),
                        resolveSwitchEnd(context, result), "1".equals(result.getIsTypeBlock())) != null)
                .min(Comparator.comparing(LhScheduleResult::getMouldChangeStartTime)).orElse(null);
    }

    /**
     * 按实际动作身份计算切换自身完成时间，两个标记同时存在时优先采用换活字块。
     *
     * @param context 本批时长参数
     * @param result 有真实切换起点的结果
     * @return 真实切换完成时间
     */
    public static Date resolveSwitchEnd(LhScheduleContext context, LhScheduleResult result) {
        int duration = "1".equals(result.getIsTypeBlock()) ? LhScheduleTimeUtil.getTypeBlockChangeTotalHours(context)
                : LhScheduleTimeUtil.getMouldChangeTotalHours(context);
        return LhScheduleTimeUtil.addHours(result.getMouldChangeStartTime(), duration);
    }

    /**
     * 统一独立清洗名额与来源运行侧的关联键，不按物料或预计完成日串用来源。
     *
     * @param source 清洗来源及运行侧
     * @return 来源和运行侧的稳定组合键
     */
    public static String sourceKey(MachineCleaningWindowDTO source) {
        return source.getSourcePlanId() + "|" + source.getLhCode();
    }

    /**
     * 校验两个有效半开区间是否严格相交。
     *
     * @param start 第一段开始
     * @param end 第一段结束
     * @param otherStart 第二段开始
     * @param otherEnd 第二段结束
     * @return 两段都有正时长且存在交集时返回true
     */
    private static boolean overlaps(Date start, Date end, Date otherStart, Date otherEnd) {
        return start != null && end != null && otherStart != null && otherEnd != null
                && start.before(end) && otherStart.before(otherEnd)
                && start.before(otherEnd) && end.after(otherStart);
    }
}
