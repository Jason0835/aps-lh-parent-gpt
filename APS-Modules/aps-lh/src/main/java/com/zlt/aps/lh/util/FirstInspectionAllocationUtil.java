package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionAllocationPlan;
import com.zlt.aps.lh.engine.strategy.support.FirstInspectionShiftAllocation;
import org.springframework.util.CollectionUtils;

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
     * @param changeoverEndTime 换模或换活字块结束时间，也是首检区间结束时间
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
        LhShiftConfigVO countingShift = FirstInspectionQtyUtil.resolveFirstInspectionAttributionShift(
                context, sku, orderedShifts, changeoverEndTime, scheduleType);
        if (Objects.isNull(countingShift)) {
            return FirstInspectionAllocationPlan.invalid(
                    "切换结束时间未命中排程班次", null, changeoverEndTime);
        }

        int sequence = FirstInspectionQtyUtil.resolveNextFirstInspectionSequence(context, countingShift);
        if (FirstInspectionQtyUtil.isTrialTimeBasedFirstInspection(sku, countingShift, scheduleType)) {
            // 试制继续沿用项目既有固定2小时/中班75%规则，不生成首检条数。
            return FirstInspectionAllocationPlan.valid(
                    sequence, 0, 0, 0L, changeoverEndTime, changeoverEndTime,
                    countingShift, new ArrayList<FirstInspectionShiftAllocation>(0));
        }
        int configuredQty = FirstInspectionQtyUtil.resolveAdjustedFirstInspectionQty(
                context, sequence, machineCode, false);
        int inspectionQty = Math.min(Math.max(0, configuredQty), Math.max(0, remainingQty));
        if (inspectionQty <= 0) {
            return FirstInspectionAllocationPlan.valid(
                    sequence, 0, 0, 0L, changeoverEndTime, changeoverEndTime,
                    countingShift, new ArrayList<FirstInspectionShiftAllocation>(0));
        }

        long effectiveSeconds = ShiftCapacityResolverUtil.resolveShiftDurationSeconds(countingShift);
        if (shiftCapacity <= 0 || effectiveSeconds <= 0L) {
            return FirstInspectionAllocationPlan.invalid(
                    "班产或班次有效生产时长配置非法", countingShift, changeoverEndTime);
        }
        int hourlyOutput = (int) (((long) shiftCapacity * SECONDS_PER_HOUR) / effectiveSeconds);
        if (hourlyOutput <= 0) {
            return FirstInspectionAllocationPlan.invalid(
                    "班产与有效生产时长折算后的小时产量为0", countingShift, changeoverEndTime);
        }
        long durationSeconds = ceilDivide(
                (long) inspectionQty * SECONDS_PER_HOUR, hourlyOutput);
        long durationMillis = durationSeconds * MILLIS_PER_SECOND;
        Date inspectionStartTime = new Date(changeoverEndTime.getTime() - durationMillis);

        List<FirstInspectionShiftAllocation> allocations = new ArrayList<FirstInspectionShiftAllocation>(4);
        long coveredMillis = 0L;
        long endMillis = changeoverEndTime.getTime();
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
                    "首检时间区间未被排程班次完整覆盖", countingShift, changeoverEndTime);
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
                    "首检取整尾差无法按真实时间顺序完成补偿", countingShift, changeoverEndTime);
        }
        boolean capacityExceeded = allocations.stream()
                .anyMatch(allocation -> allocation.getQuantity() > allocation.getCapacityLimit());
        if (capacityExceeded) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检跨班分摊超过班次实际可用产能", countingShift, changeoverEndTime);
        }

        List<FirstInspectionShiftAllocation> positiveAllocations = allocations.stream()
                .filter(allocation -> allocation.getQuantity() > 0)
                .collect(Collectors.toList());
        int finalQty = positiveAllocations.stream()
                .mapToInt(FirstInspectionShiftAllocation::getQuantity)
                .sum();
        if (finalQty != inspectionQty) {
            return FirstInspectionAllocationPlan.invalid(
                    "首检跨班分摊总量不守恒", countingShift, changeoverEndTime);
        }
        return FirstInspectionAllocationPlan.valid(
                sequence, inspectionQty, hourlyOutput, durationSeconds,
                inspectionStartTime, changeoverEndTime, countingShift, positiveAllocations);
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
