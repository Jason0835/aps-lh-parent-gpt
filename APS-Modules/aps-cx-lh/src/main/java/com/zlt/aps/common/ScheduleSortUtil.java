package com.zlt.aps.common;

import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 16799
 */
public class ScheduleSortUtil {

    /**
     * 对 List<LhAlgorithmScheduleResultDto> 分别按 class1/2/3StartTime 排序并填充 Sort
     */
    public static void fillSortByStartTimes(List<LhAlgorithmScheduleResultDto> scheduleList) {
        if (scheduleList == null || scheduleList.isEmpty()) {
            return;
        }

        // 1. 填充 class1Sort
        fillSortForClass(scheduleList, 
                LhAlgorithmScheduleResultDto::getClass1StartTime,
                LhAlgorithmScheduleResultDto::setClass1Sort);

        // 2. 填充 class2Sort
        fillSortForClass(scheduleList,
                LhAlgorithmScheduleResultDto::getClass2StartTime,
                LhAlgorithmScheduleResultDto::setClass2Sort);

        // 3. 填充 class3Sort
        fillSortForClass(scheduleList,
                LhAlgorithmScheduleResultDto::getClass3StartTime,
                LhAlgorithmScheduleResultDto::setClass3Sort);
    }

    /**
     * 通用方法：对某个 classXStartTime 排序并填充 classXSort
     * @param scheduleList 待排序的列表
     * @param getter 获取 StartTime 的方法引用
     * @param setter 设置 Sort 的方法引用
     */
    private static void fillSortForClass(
            List<LhAlgorithmScheduleResultDto> scheduleList,
            TimeGetter getter,
            SortSetter setter) {

        // 过滤出非空 StartTime 的 DTO，并按时间升序排序
        List<LhAlgorithmScheduleResultDto> sortedList = scheduleList.stream()
                .filter(dto -> getter.getTime(dto) != null)
                .sorted(Comparator.comparing(getter::getTime))
                .collect(Collectors.toList());

        // 填充 Sort（从 1 开始）
        int sort = 1;
        for (LhAlgorithmScheduleResultDto dto : sortedList) {
            setter.setSort(dto, sort);
            sort++;
        }
    }

    // 函数式接口：获取 StartTime
    @FunctionalInterface
    private interface TimeGetter {
        LocalDateTime getTime(LhAlgorithmScheduleResultDto dto);
    }

    // 函数式接口：设置 Sort
    @FunctionalInterface
    private interface SortSetter {
        void setSort(LhAlgorithmScheduleResultDto dto, int sort);
    }
}
