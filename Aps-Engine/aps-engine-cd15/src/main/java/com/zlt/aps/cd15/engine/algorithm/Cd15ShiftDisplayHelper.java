package com.zlt.aps.cd15.engine.algorithm;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * CD15 班次展示与业务日期解析工具。
 */
public final class Cd15ShiftDisplayHelper {

    private static final String SHIFT_NAME_MIDDLE = "中班";
    private static final String SHIFT_NAME_NIGHT = "夜班";
    private static final String SHIFT_NAME_DAY = "早班";

    private Cd15ShiftDisplayHelper() {
    }

    /**
     * 生成面向页面和任务进度的班次展示名，夜班按跨日后的归属日期展示。
     *
     * @param scheduleDate 排程日期
     * @param classIndex CLASS 序号
     * @return 例如：中班07/08、夜班07/09、早班07/09
     */
    public static String shiftDisplayName(Date scheduleDate, int classIndex) {
        return shiftDisplayName(toLocalDate(scheduleDate), classIndex);
    }

    /**
     * 生成面向页面和任务进度的班次展示名，夜班按跨日后的归属日期展示。
     *
     * @param scheduleDate 排程日期
     * @param classIndex CLASS 序号
     * @return 例如：中班07/08、夜班07/09、早班07/09
     */
    public static String shiftDisplayName(LocalDate scheduleDate, int classIndex) {
        LocalDate displayDate = displayDate(scheduleDate, classIndex);
        String shiftName = shiftNameForDisplay(classIndexToShiftCode(classIndex));
        if (!StringUtils.hasText(shiftName) || displayDate == null) {
            return "CLASS" + Math.max(classIndex, 1);
        }
        return shiftName + String.format("%02d/%02d", displayDate.getMonthValue(), displayDate.getDayOfMonth());
    }

    /**
     * 获取排程结果归属日期。CLASS1 为排程日前一天中班，CLASS2~4 为排程日，后续每3班顺延一天。
     *
     * @param scheduleDate 排程日期
     * @param classIndex CLASS 序号
     * @return 排程结果归属日期
     */
    public static LocalDate displayDate(LocalDate scheduleDate, int classIndex) {
        if (scheduleDate == null) {
            return null;
        }
        if (classIndex <= 1) {
            return scheduleDate.minusDays(1);
        }
        return scheduleDate.plusDays((classIndex - 2L) / 3L);
    }

    /**
     * 获取班次开始时间。夜班展示归属次日，但资源计算开始时点仍为前一日22点。
     *
     * @param scheduleDate 排程日期
     * @param classIndex CLASS 序号
     * @return 班次开始时间
     */
    public static LocalDateTime shiftStartTime(Date scheduleDate, int classIndex) {
        LocalDate displayDate = displayDate(toLocalDate(scheduleDate), classIndex);
        String shiftCode = classIndexToShiftCode(classIndex);
        if (displayDate == null) {
            displayDate = LocalDate.now();
        }
        if ("02".equals(shiftCode)) {
            return LocalDateTime.of(displayDate.minusDays(1), LocalTime.of(22, 0));
        }
        if ("03".equals(shiftCode)) {
            return LocalDateTime.of(displayDate, LocalTime.of(6, 0));
        }
        return LocalDateTime.of(displayDate, LocalTime.of(14, 0));
    }

    /**
     * 班次中文集中在此方法，后续多语言替换时只需要调整映射来源。
     *
     * @param shiftCode 班次编码
     * @return 班次名称
     */
    public static String shiftNameForDisplay(String shiftCode) {
        if (!StringUtils.hasText(shiftCode)) {
            return null;
        }
        switch (shiftCode) {
            case "01":
                return SHIFT_NAME_MIDDLE;
            case "02":
                return SHIFT_NAME_NIGHT;
            case "03":
                return SHIFT_NAME_DAY;
            default:
                return null;
        }
    }

    /**
     * 将 CLASS 序号映射为 CD15 班次编码。
     *
     * @param classIndex CLASS 序号
     * @return 班次编码
     */
    public static String classIndexToShiftCode(int classIndex) {
        int normalized = ((Math.max(classIndex, 1) - 1) % 3) + 1;
        return String.format("%02d", normalized);
    }

    /**
     * Date 转 LocalDate。
     *
     * @param value 日期
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDate 转 Date。
     *
     * @param value 日期
     * @return Date
     */
    public static Date toDate(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}