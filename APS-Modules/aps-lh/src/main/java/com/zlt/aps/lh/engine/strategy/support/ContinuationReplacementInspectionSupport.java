package com.zlt.aps.lh.engine.strategy.support;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.CleaningDispositionResolver;
import com.zlt.aps.lh.util.FirstInspectionAllocationUtil;
import com.zlt.aps.lh.util.FirstInspectionQtyUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineCleaningOverlapUtil;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同机同料纯喷砂置换的首检计数及换胶囊免扣资格，共用实际置换身份。
 * 精度组合完成后才登记，避免改变组合场景原有数量及时间口径。
 *
 * @author Codex
 * @date 2026/11/11
 */
public final class ContinuationReplacementInspectionSupport {

    /**
     * 秒转换为毫秒的倍率。
     */
    private static final long MILLIS_PER_SECOND = 1000L;

    /**
     * 禁止实例化无状态支持类。
     */
    private ContinuationReplacementInspectionSupport() {
    }

    /**
     * 为纯置换按实际班次登记一次物理首检，完整数量随容量窗口进入续作主链。
     * 调用方必须已完成置换和精度组合判定，且尚未分配续作班次计划量。
     *
     * @param context 已完成设备组合解析的排程上下文
     * @throws IllegalStateException 首检时间不能容纳于置换准备区间时抛出
     */
    public static void prepare(LhScheduleContext context) {
        Map<String, SkuScheduleDTO> sources = context.getContinuousSkuList().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuScheduleDTO::getContinuousMachineCode,
                        sku -> sku, (first, second) -> first, LinkedHashMap::new));
        Map<String, List<MachineCleaningWindowDTO>> groups = new LinkedHashMap<>();
        context.getContinuationSandBlastWindowMap().entrySet().stream()
                .filter(entry -> isIndependentReplacement(context, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((Map.Entry<String, MachineCleaningWindowDTO> entry) ->
                                entry.getValue().getCleanStartTime()).thenComparing(Map.Entry::getKey))
                .forEach(entry -> groups.computeIfAbsent(buildEventKey(entry.getKey(), entry.getValue()),
                        key -> new ArrayList<>()).add(entry.getValue()));
        Map<String, Date> reservations = context.getCleaningPlanRuntimeState()
                .getReplacementInspectionReservationMap();
        for (Map.Entry<String, List<MachineCleaningWindowDTO>> group : groups.entrySet()) {
            List<MachineCleaningWindowDTO> sides = group.getValue();
            MachineCleaningWindowDTO event = sides.get(0);
            String physicalMachine = LhSingleControlMachineUtil.resolvePhysicalMachineCode(event.getLhCode());
            if (reservations.containsKey(group.getKey()) || context.getEquipmentPlanRuntimeState()
                    .getReplacementPrecisionPlanMap().containsKey(physicalMachine)) {
                continue;
            }
            LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(context.getScheduleWindowShifts(),
                    new Date(event.getReplacementReadyTime().getTime() - 1L));
            if (Objects.isNull(shift)) {
                continue;
            }
            int sequence = FirstInspectionQtyUtil.resolveNextFirstInspectionSequence(context, shift);
            int quantity = FirstInspectionQtyUtil.resolveRawFirstInspectionQty(context, sequence);
            int physicalCapacity = sides.stream().mapToInt(side ->
                    ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                            sources.get(side.getLhCode()).getShiftCapacity(), shift,
                            ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context),
                            ScheduleTypeEnum.CONTINUOUS.getCode())).sum();
            long durationSeconds = FirstInspectionAllocationUtil.resolveInspectionDurationSeconds(quantity,
                    ShiftCapacityResolverUtil.resolveShiftDurationSeconds(shift), physicalCapacity);
            Date inspectionStart = new Date(event.getReplacementReadyTime().getTime()
                    - durationSeconds * MILLIS_PER_SECOND);
            if (durationSeconds <= 0L || inspectionStart.before(event.getCleanStartTime())) {
                throw new IllegalStateException(I18nUtil.getMessage(
                        "ui.data.column.lhScheduleResult.errorCode.systemError"));
            }
            // 条数以物理机台计一次，再分给同源L/R；不改变置换总结束时间或旧模清洗时间。
            for (int sideIndex = 0; sideIndex < sides.size(); sideIndex++) {
                MachineCleaningWindowDTO side = sides.get(sideIndex);
                side.setReplacementFirstInspectionQty(quantity / sides.size()
                        + (sideIndex < quantity % sides.size() ? 1 : 0));
                side.setReplacementInspectionStartTime(new Date(inspectionStart.getTime()));
                side.setReplacementInspectionShiftStartTime(new Date(shift.getShiftStartDateTime().getTime()));
            }
            FirstInspectionQtyUtil.recordFirstInspectionSequence(context, shift);
            reservations.put(group.getKey(), new Date(shift.getShiftStartDateTime().getTime()));
            PriorityTraceLogHelper.appendProcessLog(context, "喷砂置换首检登记",
                    "工厂=" + context.getFactoryCode() + "，批次=" + context.getBatchNo()
                            + "，事件=" + group.getKey() + "，班次=class" + shift.getShiftIndex()
                            + "，首检顺序=" + sequence + "，参数="
                            + FirstInspectionQtyUtil.resolveFirstInspectionParamCode(sequence)
                            + "，物理首检量=" + quantity + "，首检开始="
                            + LhScheduleTimeUtil.formatDateTime(inspectionStart)
                            + "，首检结束=" + LhScheduleTimeUtil.formatDateTime(event.getReplacementReadyTime()));
        }
    }

    /**
     * 释放已取消事件的首检计数，同源任一有效侧仍存在时保留物理事件预占。
     *
     * @param context 已移除未到触发点事件或完成清洗处置复核的上下文
     */
    public static void releaseCancelled(LhScheduleContext context) {
        Map<String, Date> reservations = context.getCleaningPlanRuntimeState()
                .getReplacementInspectionReservationMap();
        List<String> activeKeys = context.getContinuationSandBlastWindowMap().entrySet().stream()
                .filter(entry -> isIndependentReplacement(context, entry.getKey(), entry.getValue()))
                .map(entry -> buildEventKey(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        for (String key : new ArrayList<>(reservations.keySet())) {
            if (!activeKeys.contains(key)) {
                LhShiftConfigVO shift = LhScheduleTimeUtil.resolveShiftByTime(
                        context.getScheduleWindowShifts(), reservations.remove(key));
                FirstInspectionQtyUtil.rollbackFirstInspectionSequence(context, shift);
            }
        }
    }

    /**
     * 判断当前结果班次是否承接本轮已登记的纯置换首检，供胶囊首次分配和后处理共用。
     * 不根据备注、机台名称或历史结果推断免扣资格。
     *
     * @param context 当前批次资源与首检预占
     * @param result 当前续作结果
     * @param shift 待计算班次
     * @return 有效同机同料状态置换首检班次返回true
     */
    public static boolean isReplacementInspectionShift(LhScheduleContext context,
            LhScheduleResult result, LhShiftConfigVO shift) {
        MachineCleaningWindowDTO event = context.getContinuationSandBlastWindowMap().get(result.getLhMachineCode());
        if (!StringUtils.equals(ScheduleTypeEnum.CONTINUOUS.getCode(), result.getScheduleType())
                || !isIndependentReplacement(context, result.getLhMachineCode(), event)
                || !StringUtils.equals(event.getContinuationMaterialCode(), result.getMaterialCode())
                || !StringUtils.equals(event.getContinuationProductStatus(), result.getProductStatus())
                || !Objects.equals(event.getReplacementInspectionShiftStartTime(), shift.getShiftStartDateTime())
                || Objects.isNull(event.getReplacementFirstInspectionQty())
                || event.getReplacementFirstInspectionQty() <= 0) {
            return false;
        }
        return Objects.equals(context.getCleaningPlanRuntimeState().getReplacementInspectionReservationMap()
                .get(buildEventKey(result.getLhMachineCode(), event)), shift.getShiftStartDateTime());
    }

    /**
     * 识别仍有效的闲置模具置换事实，独立在机喷砂和已取消事件不能进入首检免扣链。
     *
     * @param context 当前事件处置上下文
     * @param machineCode 运行侧机台
     * @param event 置换事件，允许为空
     * @return 已落实替换模具及时间且仍独立执行时返回true
     */
    private static boolean isIndependentReplacement(LhScheduleContext context, String machineCode,
            MachineCleaningWindowDTO event) {
        return Objects.nonNull(event) && event.isOffMachineCleaning()
                && MachineCleaningOverlapUtil.isSandBlastCleaning(event)
                && StringUtils.isNotBlank(event.getReplacementMouldCode())
                && Objects.nonNull(event.getCleanStartTime()) && Objects.nonNull(event.getReplacementReadyTime())
                && CleaningDispositionResolver.resolve(context, machineCode, event).getDisposition()
                == CleaningDispositionResolver.Disposition.INDEPENDENT;
    }

    /**
     * 构造与置换额度相同的物理事件身份，同源左右侧共享一次登记。
     *
     * @param machineCode 运行侧机台
     * @param event 完整置换事件
     * @return 物理机台、来源计划、开始时刻及物料状态组成的键
     */
    private static String buildEventKey(String machineCode, MachineCleaningWindowDTO event) {
        return LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode) + "|" + event.getSourcePlanId()
                + "|" + event.getCleanStartTime().getTime() + "|" + event.getContinuationMaterialCode()
                + "|" + event.getContinuationProductStatus();
    }
}
