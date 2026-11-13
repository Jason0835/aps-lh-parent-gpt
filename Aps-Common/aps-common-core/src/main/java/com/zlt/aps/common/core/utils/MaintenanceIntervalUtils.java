package com.zlt.aps.common.core.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备停机时间区间计算工具。
 */
public final class MaintenanceIntervalUtils {

    private MaintenanceIntervalUtils() {
    }

    /**
     * 合并有效的重叠或首尾相接停机区间。
     *
     * @param intervalList 停机区间列表，每项依次为开始时间和结束时间
     * @return 按开始时间排序且互不重叠的区间列表
     */
    public static List<Date[]> mergeIntervals(List<Date[]> intervalList) {
        if (intervalList == null || intervalList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Date[]> sortedIntervals = intervalList.stream()
                .filter(interval -> interval != null && interval.length >= 2
                        && interval[0] != null && interval[1] != null
                        && interval[1].after(interval[0]))
                .map(interval -> new Date[]{interval[0], interval[1]})
                .sorted(Comparator.comparing(interval -> interval[0]))
                .collect(Collectors.toList());
        List<Date[]> mergedIntervals = new ArrayList<>();
        for (Date[] interval : sortedIntervals) {
            if (mergedIntervals.isEmpty()) {
                mergedIntervals.add(interval);
                continue;
            }
            Date[] previousInterval = mergedIntervals.get(mergedIntervals.size() - 1);
            if (!interval[0].after(previousInterval[1])) {
                if (interval[1].after(previousInterval[1])) {
                    previousInterval[1] = interval[1];
                }
                continue;
            }
            mergedIntervals.add(interval);
        }
        return mergedIntervals;
    }
}
