package com.zlt.aps.lh.util;

import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionShiftAllocation;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 换模和换活字块共用的首检时间分摊工具。
 *
 * <p>本工具只做无副作用计算：先按项目参数取得一次首检总量，再根据班产和班次有效时长
 * 计算真实首检区间，最后按各班次的真实重叠时长分摊数量。调用方只有在候选机台正式
 * 命中后才提交计数和结果，避免候选预演污染首检账本。</p>
 *
 * @author APS
 */
public final class FirstInspectionAllocationUtil {

    /** 小时换算秒数。 */
    private static final long SECONDS_PER_HOUR = 3600L;

    /** 秒换算毫秒数。 */
    private static final long MILLIS_PER_SECOND = 1000L;

    private FirstInspectionAllocationUtil() {
    }

    /**
     * 构建一次首检的跨班时间分摊计划。
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param shifts 完整排程窗口班次
     * @param changeoverEndTime 换模或换活字块结束时间
     * @param shiftCapacity 当前 SKU 在目标机台的运行态班产
     * @param remainingQty 当前机台最多允许消费的目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码，用于单控首检折半
     * @param availableCapacityMap 各班次实际可供首检占用的容量；为空时使用班产上限
     * @return 无副作用首检分摊计划
     */
    public static FirstInspectionAllocationPlan buildPlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            Date changeoverEndTime,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode,
            Map<Integer, Integer> availableCapacityMap) {
        return buildPlan(
                context, sku, shifts, changeoverEndTime, null, shiftCapacity,
                remainingQty, scheduleType, machineCode, availableCapacityMap);
    }

    /**
     * 按统一时间与实际产能口径构建首检分摊计划，并支持量试首检生产门禁。
     *
     * <p>普通 SKU 继续按换模/换活字块结束时间向前倒推首检区间；量试 SKU 的首检属于
     * 开产，当统一门禁不早于准备完成时间时，首检必须从门禁开始并按所需时长向后分摊。
     * 试制 SKU 仍执行固定2小时产能扣减，不生成首检条数。</p>
     *
     * @param context 排程上下文
     * @param sku 当前 SKU
     * @param shifts 排程班次
     * @param changeoverEndTime 换模或换活字块结束时间
     * @param inspectionNotBeforeTime 首检不得早于的统一生产门禁；无门禁时传 null
     * @param shiftCapacity 当前 SKU 在目标机台的运行态班产
     * @param remainingQty 当前机台最多允许消费的目标量
     * @param scheduleType 排程类型
     * @param machineCode 机台编码，用于单控首检折半
     * @param availableCapacityMap 各班次实际可供首检占用的容量；为空时使用班产上限
     * @return 无副作用首检分摊计划
     */
    public static FirstInspectionAllocationPlan buildPlan(
            LhScheduleContext context,
            SkuScheduleDTO sku,
            List<LhShiftConfigVO> shifts,
            Date changeoverEndTime,
            Date inspectionNotBeforeTime,
            int shiftCapacity,
            int remainingQty,
            String scheduleType,
            String machineCode,
            Map<Integer, Integer> availableCapacityMap) {
        if (CollectionUtils.isEmpty(shifts) || Objects.isNull(changeoverEndTime)) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检缺少完整班次或切换结束时间", null, changeoverEndTime);
        }
        List<LhShiftConfigVO> orderedShifts = shifts.stream()
                .filter(Objects::nonNull)
                .filter(shift -> Objects.nonNull(shift.getShiftIndex()))
                .filter(shift -> Objects.nonNull(shift.getShiftStartDateTime()))
                .filter(shift -> Objects.nonNull(shift.getShiftEndDateTime()))
                .sorted(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .collect(Collectors.toList());
        boolean forwardMassTrialInspection = FirstInspectionQtyUtil
                .isMassTrialQuantityFirstInspection(sku, scheduleType)
                && Objects.nonNull(inspectionNotBeforeTime);
        Date inspectionReferenceTime = forwardMassTrialInspection
                ? (inspectionNotBeforeTime.after(changeoverEndTime)
                ? inspectionNotBeforeTime : changeoverEndTime)
                : changeoverEndTime;
        LhShiftConfigVO countingShift = FirstInspectionQtyUtil.resolveFirstInspectionAttributionShift(
                context, sku, orderedShifts, inspectionReferenceTime, scheduleType);
        if (Objects.isNull(countingShift)) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检时间未命中排程班次", null, inspectionReferenceTime);
        }

        int sequence = FirstInspectionQtyUtil.resolveNextFirstInspectionSequence(context, countingShift);
        if (FirstInspectionQtyUtil.isTrialTimeBasedFirstInspection(sku, countingShift, scheduleType)) {
            // 试制继续沿用项目既有固定2小时/中班75%规则，不生成首检条数。
            return FirstInspectionAllocationPlan.valid(
                    sequence, 0, BigDecimal.ZERO, 0L, inspectionReferenceTime, inspectionReferenceTime,
                    countingShift, new ArrayList<FirstInspectionShiftAllocation>(0));
        }
        int configuredQty = FirstInspectionQtyUtil.resolveAdjustedFirstInspectionQty(
                context, sequence, machineCode, false);
        int inspectionQty = Math.min(Math.max(0, configuredQty), Math.max(0, remainingQty));
        if (inspectionQty <= 0) {
            return FirstInspectionAllocationPlan.valid(
                    sequence, 0, BigDecimal.ZERO, 0L, inspectionReferenceTime, inspectionReferenceTime,
                    countingShift, new ArrayList<FirstInspectionShiftAllocation>(0));
        }

        long effectiveSeconds = ShiftCapacityResolverUtil.resolveShiftDurationSeconds(countingShift);
        if (shiftCapacity <= 0 || effectiveSeconds <= 0L) {
            return FirstInspectionAllocationPlan.invalid(
                    "班产或班次有效生产时长配置非法", countingShift, inspectionReferenceTime);
        }
        BigDecimal hourlyOutput = BigDecimalUtils.div(
                (long) shiftCapacity * SECONDS_PER_HOUR, effectiveSeconds, 4).stripTrailingZeros();
        if (hourlyOutput.compareTo(BigDecimal.ZERO) <= 0) {
            return FirstInspectionAllocationPlan.invalid(
                    "班产与有效生产时长折算后的小时产量为0", countingShift, inspectionReferenceTime);
        }
        /*
         * 首检时长直接按“首检量×班次有效秒数÷班产”计算，最后只在秒级向上取整。
         * 禁止先把小时产量截断为整数，否则班产14条/8小时会从1.75条/小时被降为1条/小时，
         * 将本应落在后续班次的首检区间错误拉长到前一班次。
         */
        long durationSeconds = ceilDivide(
                (long) inspectionQty * effectiveSeconds, shiftCapacity);
        long durationMillis = durationSeconds * MILLIS_PER_SECOND;
        Date inspectionStartTime = forwardMassTrialInspection
                ? inspectionReferenceTime
                : new Date(inspectionReferenceTime.getTime() - durationMillis);
        Date inspectionEndTime = forwardMassTrialInspection
                ? new Date(inspectionStartTime.getTime() + durationMillis)
                : inspectionReferenceTime;

        List<FirstInspectionShiftAllocation> allocations = new ArrayList<FirstInspectionShiftAllocation>(4);
        long coveredMillis = 0L;
        long endMillis = inspectionEndTime.getTime();
        long startMillis = inspectionStartTime.getTime();
        String configPlusShiftType = ShiftCapacityResolverUtil.resolveOddShiftCapacityPlusShiftType(context);
        for (LhShiftConfigVO shift : orderedShifts) {
            long overlapStartMillis = Math.max(startMillis, shift.getShiftStartDateTime().getTime());
            long overlapEndMillis = Math.min(endMillis, shift.getShiftEndDateTime().getTime());
            if (overlapStartMillis >= overlapEndMillis) {
                continue;
            }
            long overlapMillis = overlapEndMillis - overlapStartMillis;
            coveredMillis += overlapMillis;
            long numerator = (long) inspectionQty * overlapMillis;
            int baseQty = (int) (numerator / durationMillis);
            long fractionalRemainder = numerator % durationMillis;
            int shiftCapacityLimit = Math.max(0, ShiftCapacityResolverUtil.resolveActualShiftPlanQty(
                    shiftCapacity, shift, configPlusShiftType, scheduleType));
            if (!CollectionUtils.isEmpty(availableCapacityMap)
                    && availableCapacityMap.containsKey(shift.getShiftIndex())) {
                shiftCapacityLimit = Math.min(shiftCapacityLimit,
                        Math.max(0, availableCapacityMap.get(shift.getShiftIndex())));
            }
            allocations.add(new FirstInspectionShiftAllocation(
                    shift, new Date(overlapStartMillis), new Date(overlapEndMillis), overlapMillis,
                    fractionalRemainder, shiftCapacityLimit, baseQty));
        }
        if (coveredMillis != durationMillis) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检时间区间未被排程班次完整覆盖", countingShift, inspectionEndTime);
        }

        int allocatedQty = allocations.stream()
                .mapToInt(FirstInspectionShiftAllocation::getQuantity)
                .sum();
        int remainingTailQty = inspectionQty - allocatedQty;
        List<FirstInspectionShiftAllocation> compensationOrder = allocations.stream()
                .sorted(Comparator.comparingLong(FirstInspectionShiftAllocation::getFractionalRemainder)
                        .reversed()
                        .thenComparing(allocation -> allocation.getShift().getShiftStartDateTime()))
                .collect(Collectors.toList());
        for (FirstInspectionShiftAllocation allocation : compensationOrder) {
            if (remainingTailQty <= 0) {
                break;
            }
            allocation.increaseQuantity();
            remainingTailQty--;
        }
        if (remainingTailQty > 0) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检取整尾差无法按真实时间顺序完成补偿", countingShift, inspectionEndTime);
        }
        boolean capacityExceeded = allocations.stream()
                .anyMatch(allocation -> allocation.getQuantity() > allocation.getCapacityLimit());
        if (capacityExceeded) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检跨班分摊超过班次实际可用产能", countingShift, inspectionEndTime);
        }

        List<FirstInspectionShiftAllocation> positiveAllocations = allocations.stream()
                .filter(allocation -> allocation.getQuantity() > 0)
                .collect(Collectors.toList());
        int finalQty = positiveAllocations.stream()
                .mapToInt(FirstInspectionShiftAllocation::getQuantity)
                .sum();
        if (finalQty != inspectionQty) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检跨班分摊总量不守恒", countingShift, inspectionEndTime);
        }
        return FirstInspectionAllocationPlan.valid(
                sequence, inspectionQty, hourlyOutput, durationSeconds,
                inspectionStartTime, inspectionEndTime, countingShift, positiveAllocations);
    }

    /**
     * 将分摊计划转换为班次首检占用量映射。
     *
     * @param plan 首检分摊计划
     * @return 班次索引到首检数量的有序映射
     */
    public static Map<Integer, Integer> toShiftQtyMap(FirstInspectionAllocationPlan plan) {
        Map<Integer, Integer> shiftQtyMap = new LinkedHashMap<Integer, Integer>(4);
        if (Objects.isNull(plan) || !plan.isValid()) {
            return shiftQtyMap;
        }
        for (FirstInspectionShiftAllocation allocation : plan.getShiftAllocations()) {
            shiftQtyMap.put(allocation.getShift().getShiftIndex(), allocation.getQuantity());
        }
        return shiftQtyMap;
    }

    private static long ceilDivide(long dividend, long divisor) {
        return dividend / divisor + (dividend % divisor == 0L ? 0L : 1L);
    }
}
