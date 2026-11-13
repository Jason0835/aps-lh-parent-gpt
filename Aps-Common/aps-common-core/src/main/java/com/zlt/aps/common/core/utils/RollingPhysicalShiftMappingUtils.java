package com.zlt.aps.common.core.utils;

import com.zlt.aps.common.core.domain.vo.RollingPhysicalShiftMapping;
import com.zlt.aps.common.core.domain.vo.RollingPhysicalShiftRepresentation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 依据班次实际起止时间定位自动滚动目标物理班次及双日期表示。
 */
public final class RollingPhysicalShiftMappingUtils {

    private RollingPhysicalShiftMappingUtils() {
    }

    /**
     * 解析触发边界开始的当班，并验证主侧、同步侧以及紧接的下一物理班。
     *
     * @param representations 按工厂时区和班次配置解析出的候选物理窗口
     * @param triggerBoundaryTime 定时触发所对应的班次开始边界
     * @return 双日期映射；目标班、双侧表示或下一班缺失、歧义时 valid 为 false
     */
    public static RollingPhysicalShiftMapping resolveCurrentShift(
            List<RollingPhysicalShiftRepresentation> representations,
            java.util.Date triggerBoundaryTime) {
        RollingPhysicalShiftMapping mapping = new RollingPhysicalShiftMapping();
        if (representations == null || triggerBoundaryTime == null) {
            mapping.setInvalidReason("MISSING_TRIGGER_OR_SHIFT_CONFIGURATION");
            return mapping;
        }
        List<RollingPhysicalShiftRepresentation> validRepresentations = representations.stream()
                .filter(Objects::nonNull)
                .filter(RollingPhysicalShiftMappingUtils::hasValidPhysicalWindow)
                .collect(Collectors.toList());
        Map<String, RollingPhysicalShiftRepresentation> targetWindows = distinctWindows(validRepresentations.stream()
                .filter(item -> Objects.equals(item.getStartTime(), triggerBoundaryTime))
                .collect(Collectors.toList()));
        if (targetWindows.size() != 1) {
            mapping.setInvalidReason(targetWindows.isEmpty()
                    ? "TARGET_SHIFT_NOT_FOUND" : "TARGET_SHIFT_WINDOW_AMBIGUOUS");
            return mapping;
        }
        RollingPhysicalShiftRepresentation targetWindow = targetWindows.values().iterator().next();
        mapping.setTargetStartTime(targetWindow.getStartTime());
        mapping.setTargetEndTime(targetWindow.getEndTime());
        boolean targetOverlapsAnotherWindow = distinctWindows(validRepresentations).values().stream()
                .anyMatch(item -> !samePhysicalWindow(item, targetWindow)
                        && item.getStartTime().before(targetWindow.getEndTime())
                        && item.getEndTime().after(targetWindow.getStartTime()));
        if (targetOverlapsAnotherWindow) {
            mapping.setInvalidReason("TARGET_SHIFT_OVERLAPS_ANOTHER_WINDOW");
            return mapping;
        }

        List<RollingPhysicalShiftRepresentation> targetRepresentations = validRepresentations.stream()
                .filter(item -> samePhysicalWindow(item, targetWindow))
                .collect(Collectors.toList());
        List<RollingPhysicalShiftRepresentation> primaryCandidates = targetRepresentations.stream()
                .filter(item -> item.getShiftOrder() != null && item.getShiftOrder() >= 1
                        && item.getShiftOrder() <= 3)
                .collect(Collectors.toList());
        List<RollingPhysicalShiftRepresentation> secondaryCandidates = targetRepresentations.stream()
                .filter(item -> item.getShiftOrder() != null && item.getShiftOrder() >= 4
                        && item.getShiftOrder() <= 6)
                .collect(Collectors.toList());
        if (!hasValidPairedRepresentations(primaryCandidates, secondaryCandidates)) {
            mapping.setInvalidReason("DUAL_DATE_SHIFT_MAPPING_INVALID");
            return mapping;
        }

        mapping.setPrimary(primaryCandidates.get(0));
        mapping.setSecondary(secondaryCandidates.get(0));

        Map<String, RollingPhysicalShiftRepresentation> nextWindows = distinctWindows(validRepresentations.stream()
                .filter(item -> startsAtNextPhysicalBoundary(item.getStartTime(), targetWindow.getEndTime()))
                .collect(Collectors.toList()));
        if (nextWindows.size() != 1) {
            mapping.setInvalidReason(nextWindows.isEmpty()
                    ? "NEXT_SHIFT_NOT_FOUND" : "NEXT_SHIFT_WINDOW_AMBIGUOUS");
            return mapping;
        }
        RollingPhysicalShiftRepresentation nextWindow = nextWindows.values().iterator().next();
        boolean nextOverlapsAnotherWindow = distinctWindows(validRepresentations).values().stream()
                .anyMatch(item -> !samePhysicalWindow(item, nextWindow)
                        && item.getStartTime().before(nextWindow.getEndTime())
                        && item.getEndTime().after(nextWindow.getStartTime()));
        if (nextOverlapsAnotherWindow) {
            mapping.setInvalidReason("NEXT_SHIFT_OVERLAPS_ANOTHER_WINDOW");
            return mapping;
        }

        mapping.setDemandWindowStartTime(nextWindow.getStartTime());
        mapping.setValid(true);
        return mapping;
    }

    /**
     * 兼容解析触发边界前结束的最近物理班次；新版定时任务应调用 {@link #resolveCurrentShift(List, java.util.Date)}。
     *
     * @param representations     按工厂时区和班次配置解析出的候选物理窗口
     * @param triggerBoundaryTime 定时触发所对应的班次开始边界
     * @return 双日期映射；候选缺失、歧义或物理窗口不一致时 valid 为 false
     */
    public static RollingPhysicalShiftMapping resolvePreviousShift(
            List<RollingPhysicalShiftRepresentation> representations,
            java.util.Date triggerBoundaryTime) {
        RollingPhysicalShiftMapping mapping = new RollingPhysicalShiftMapping();
        if (representations == null || triggerBoundaryTime == null) {
            return mapping;
        }
        List<RollingPhysicalShiftRepresentation> previousWindows = representations.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStartTime() != null && item.getEndTime() != null
                        && item.getStartTime().before(item.getEndTime())
                        && !item.getEndTime().after(triggerBoundaryTime))
                .sorted(Comparator.comparing(RollingPhysicalShiftRepresentation::getEndTime).reversed()
                        .thenComparing(RollingPhysicalShiftRepresentation::getStartTime,
                                Comparator.reverseOrder()))
                .collect(Collectors.toList());
        if (previousWindows.isEmpty()) {
            return mapping;
        }
        java.util.Date latestEndTime = previousWindows.get(0).getEndTime();
        List<RollingPhysicalShiftRepresentation> nearestWindows = previousWindows.stream()
                .filter(item -> Objects.equals(latestEndTime, item.getEndTime()))
                .collect(Collectors.toList());
        List<RollingPhysicalShiftRepresentation> distinctNearestWindows = nearestWindows.stream()
                .collect(Collectors.collectingAndThen(Collectors.toMap(
                                item -> item.getStartTime().getTime() + ":" + item.getEndTime().getTime(),
                                item -> item, (first, ignored) -> first, java.util.LinkedHashMap::new),
                        windows -> windows.values().stream().collect(Collectors.toList())));
        if (distinctNearestWindows.size() != 1) {
            return mapping;
        }
        RollingPhysicalShiftRepresentation previousWindow = distinctNearestWindows.get(0);
        List<RollingPhysicalShiftRepresentation> physicalRepresentations = representations.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(previousWindow.getStartTime(), item.getStartTime())
                        && Objects.equals(previousWindow.getEndTime(), item.getEndTime()))
                .collect(Collectors.toList());
        List<RollingPhysicalShiftRepresentation> primaryCandidates = physicalRepresentations.stream()
                .filter(item -> item.getShiftOrder() != null && item.getShiftOrder() >= 1
                        && item.getShiftOrder() <= 3).collect(Collectors.toList());
        List<RollingPhysicalShiftRepresentation> secondaryCandidates = physicalRepresentations.stream()
                .filter(item -> item.getShiftOrder() != null && item.getShiftOrder() >= 4
                        && item.getShiftOrder() <= 6).collect(Collectors.toList());
        if (primaryCandidates.size() != 1 || secondaryCandidates.size() != 1
                || !Objects.equals(primaryCandidates.get(0).getShiftOrder() + 3,
                secondaryCandidates.get(0).getShiftOrder())
                || primaryCandidates.get(0).getScheduleDate() == null
                || secondaryCandidates.get(0).getScheduleDate() == null
                || !Objects.equals(primaryCandidates.get(0).getScheduleDate().minusDays(1),
                secondaryCandidates.get(0).getScheduleDate())) {
            return mapping;
        }
        mapping.setTargetStartTime(previousWindow.getStartTime());
        mapping.setTargetEndTime(previousWindow.getEndTime());
        mapping.setPrimary(primaryCandidates.get(0));
        mapping.setSecondary(secondaryCandidates.get(0));
        mapping.setValid(true);
        return mapping;
    }

    /**
     * 判断班次映射是否具有有效且正向的实际时间区间。
     *
     * @param representation 班次物理时间表示
     * @return 起止时间完整且开始早于结束时返回 true
     */
    private static boolean hasValidPhysicalWindow(RollingPhysicalShiftRepresentation representation) {
        return representation.getStartTime() != null && representation.getEndTime() != null
                && representation.getStartTime().before(representation.getEndTime());
    }

    /**
     * 判断下一物理班是否与当前班按秒级闭区间配置连续。
     *
     * @param nextStartTime  下一物理班实际开始时间
     * @param currentEndTime 当前物理班原始结束时间
     * @return 两个时刻相等或下一班恰好晚一秒时返回true
     */
    private static boolean startsAtNextPhysicalBoundary(java.util.Date nextStartTime,
                                                        java.util.Date currentEndTime) {
        if (nextStartTime == null || currentEndTime == null) {
            return false;
        }
        long currentEndMillis = currentEndTime.getTime();
        return nextStartTime.getTime() == currentEndMillis
                || (currentEndMillis <= Long.MAX_VALUE - 1000L
                && nextStartTime.getTime() == currentEndMillis + 1000L);
    }

    /**
     * 按起止时间对重复物理窗口去重。
     *
     * @param windows 待去重的班次表示
     * @return 每个物理时间区间对应的首个表示
     */
    private static Map<String, RollingPhysicalShiftRepresentation> distinctWindows(
            List<RollingPhysicalShiftRepresentation> windows) {
        Map<String, RollingPhysicalShiftRepresentation> distinct = new LinkedHashMap<>();
        windows.stream().filter(Objects::nonNull).forEach(item -> distinct.putIfAbsent(
                item.getStartTime().getTime() + ":" + item.getEndTime().getTime(), item));
        return distinct;
    }

    /**
     * 比较两个班次表示是否对应完全相同的物理时间区间。
     *
     * @param first  第一个班次表示
     * @param second 第二个班次表示
     * @return 起止时刻均相同时返回 true
     */
    private static boolean samePhysicalWindow(RollingPhysicalShiftRepresentation first,
                                              RollingPhysicalShiftRepresentation second) {
        return Objects.equals(first.getStartTime(), second.getStartTime())
                && Objects.equals(first.getEndTime(), second.getEndTime());
    }

    /**
     * 校验目标物理班恰好具有一组有效的主侧与同步侧结果表示。
     *
     * @param primaryCandidates   主侧前三班候选
     * @param secondaryCandidates 同步侧后三班候选
     * @return 班序和排程日期符合双日期关系时返回 true
     */
    private static boolean hasValidPairedRepresentations(
            List<RollingPhysicalShiftRepresentation> primaryCandidates,
            List<RollingPhysicalShiftRepresentation> secondaryCandidates) {
        if (primaryCandidates.size() != 1 || secondaryCandidates.size() != 1) {
            return false;
        }
        RollingPhysicalShiftRepresentation primary = primaryCandidates.get(0);
        RollingPhysicalShiftRepresentation secondary = secondaryCandidates.get(0);
        return Objects.equals(primary.getShiftOrder() + 3, secondary.getShiftOrder())
                && primary.getScheduleDate() != null && secondary.getScheduleDate() != null
                && Objects.equals(primary.getScheduleDate().minusDays(1), secondary.getScheduleDate());
    }
}
