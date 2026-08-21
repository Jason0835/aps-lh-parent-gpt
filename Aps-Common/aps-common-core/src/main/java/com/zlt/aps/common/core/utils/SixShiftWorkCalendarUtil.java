package com.zlt.aps.common.core.utils;

import cn.hutool.core.date.DateUtil;

import java.util.Date;

/**
 * 六班排程与三班工作日历映射工具。
 *
 * <p>排程六班从排程日前一天中班开始，依次映射为：前一天中班、当天夜班、当天早班、
 * 当天中班、后一天夜班、后一天早班。</p>
 */
public final class SixShiftWorkCalendarUtil {

    private static final int MIN_SHIFT_ORDER = 1;
    private static final int MAX_SHIFT_ORDER = 6;
    private static final int MAX_FORMING_LOGICAL_SHIFT_ORDER = 8;

    private SixShiftWorkCalendarUtil() {
    }

    /**
     * 解析结果班次对应的工作日历生产日期。
     *
     * @param scheduleDate 排程日期
     * @param shiftOrder   结果班次顺序，取值1到6
     * @return 对应工作日历生产日期的零点时间
     * @throws IllegalArgumentException 排程日期为空或班次顺序越界时抛出
     */
    public static Date resolveProductionDate(Date scheduleDate, int shiftOrder) {
        validate(scheduleDate, shiftOrder);
        int dayOffset = shiftOrder == 1 ? -1 : (shiftOrder >= 5 ? 1 : 0);
        return DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, dayOffset));
    }

    /**
     * 解析结果班次对应的工作日历班次序号。
     *
     * @param shiftOrder 结果班次顺序，取值1到6
     * @return 1表示夜班字段、2表示早班字段、3表示中班字段
     * @throws IllegalArgumentException 班次顺序越界时抛出
     */
    public static int resolveCalendarShiftOrder(int shiftOrder) {
        validateShiftOrder(shiftOrder);
        int[] mapping = {3, 1, 2, 3, 1, 2};
        return mapping[shiftOrder - 1];
    }

    /**
     * 解析结果班次对应的工作日历字段名，用于规则证据和日志。
     *
     * @param shiftOrder 结果班次顺序，取值1到6
     * @return 工作日历班次标志字段名
     * @throws IllegalArgumentException 班次顺序越界时抛出
     */
    public static String resolveCalendarShiftField(int shiftOrder) {
        int calendarShiftOrder = resolveCalendarShiftOrder(shiftOrder);
        if (calendarShiftOrder == 1) {
            return "ONE_SHIFT_FLAG";
        }
        if (calendarShiftOrder == 2) {
            return "TWO_SHIFT_FLAG";
        }
        return "THREE_SHIFT_FLAG";
    }

    /**
     * 解析成型 CLASS1 至 CLASS8 对应的工作日历生产日期。
     *
     * <p>成型逻辑班从排程日前一天中班开始，随后按夜班、早班、中班逐日推进。</p>
     *
     * @param scheduleDate             排程日期
     * @param formingLogicalShiftOrder 成型逻辑班次，取值1到8
     * @return 对应工作日历生产日期的零点时间
     * @throws IllegalArgumentException 排程日期为空或成型逻辑班次越界时抛出
     */
    public static Date resolveFormingProductionDate(Date scheduleDate, int formingLogicalShiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate must not be null");
        }
        validateFormingLogicalShiftOrder(formingLogicalShiftOrder);
        int dayOffset = formingLogicalShiftOrder == 1
                ? -1 : (formingLogicalShiftOrder - 2) / 3;
        return DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, dayOffset));
    }

    /**
     * 解析成型 CLASS1 至 CLASS8 对应的工作日历班次序号。
     *
     * @param formingLogicalShiftOrder 成型逻辑班次，取值1到8
     * @return 1表示夜班、2表示早班、3表示中班
     * @throws IllegalArgumentException 成型逻辑班次越界时抛出
     */
    public static int resolveFormingCalendarShiftOrder(int formingLogicalShiftOrder) {
        validateFormingLogicalShiftOrder(formingLogicalShiftOrder);
        if (formingLogicalShiftOrder == 1) {
            return 3;
        }
        return (formingLogicalShiftOrder - 2) % 3 + 1;
    }

    private static void validate(Date scheduleDate, int shiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate must not be null");
        }
        validateShiftOrder(shiftOrder);
    }

    private static void validateShiftOrder(int shiftOrder) {
        if (shiftOrder < MIN_SHIFT_ORDER || shiftOrder > MAX_SHIFT_ORDER) {
            throw new IllegalArgumentException("shiftOrder must be between 1 and 6");
        }
    }

    private static void validateFormingLogicalShiftOrder(int formingLogicalShiftOrder) {
        if (formingLogicalShiftOrder < MIN_SHIFT_ORDER
                || formingLogicalShiftOrder > MAX_FORMING_LOGICAL_SHIFT_ORDER) {
            throw new IllegalArgumentException("formingLogicalShiftOrder must be between 1 and 8");
        }
    }
}
