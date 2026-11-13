package com.zlt.aps.common.core.utils;

import cn.hutool.core.date.DateUtil;

import java.time.LocalDate;
import java.util.Date;

/**
 * 六班排程与三班工作日历映射工具。
 *
 * <p>排程六班从排程日前一天中班开始，依次映射为：前一天中班、当天夜班、当天早班、
 * 当天中班、后一天夜班、后一天早班；成型 CLASS1、CLASS2 对应前一天早班和中班，之后每三班
 * 推进一天并按夜班、早班、中班循环，因此 CLASS9、CLASS10 分别映射到排程日后两天夜班和早班。</p>
 */
public final class SixShiftWorkCalendarUtil {

    private static final int MIN_SHIFT_ORDER = 1;
    private static final int MAX_SHIFT_ORDER = 6;

    /** 成型首班（CLASS1）对应的日历班次序号：1表示夜班、2表示早班、3表示中班 */
    private static final int FORMING_FIRST_CALENDAR_SHIFT_ORDER = 2;

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
     * 解析 LocalDate 排程日期对应的工作日历生产日期。
     *
     * @param scheduleDate 排程日期
     * @param shiftOrder 结果班次顺序，取值1到6
     * @return 对应工作日历生产日期
     * @throws IllegalArgumentException 排程日期为空或班次顺序越界时抛出
     */
    public static LocalDate resolveProductionDate(LocalDate scheduleDate, int shiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate must not be null");
        }
        validateShiftOrder(shiftOrder);
        int dayOffset = shiftOrder == 1 ? -1 : (shiftOrder >= 5 ? 1 : 0);
        return scheduleDate.plusDays(dayOffset);
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
     * 解析成型逻辑班对应的工作日历生产日期，CLASS8之后按三班周期继续映射。
     *
     * <p>成型 CLASS1 对应排程日前一天早班，CLASS2 对应前一天中班；此后每三班推进一天，
     * 并按夜班、早班、中班循环。</p>
     *
     * @param scheduleDate             排程日期
     * @param formingLogicalShiftOrder 成型逻辑班次，必须为正数
     * @return 对应工作日历生产日期的零点时间
     * @throws IllegalArgumentException 排程日期为空或成型逻辑班次越界时抛出
     */
    public static Date resolveFormingProductionDate(Date scheduleDate, int formingLogicalShiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate must not be null");
        }
        validateFormingLogicalShiftOrder(formingLogicalShiftOrder);
        int dayOffset = Math.floorDiv(formingLogicalShiftOrder - 3, 3);
        return DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, dayOffset));
    }

    /**
     * 解析 LocalDate 排程日期对应的成型工作日历生产日期。
     *
     * @param scheduleDate 排程日期
     * @param formingLogicalShiftOrder 成型逻辑班次，必须为正数
     * @return 对应工作日历生产日期
     * @throws IllegalArgumentException 排程日期为空或成型逻辑班次越界时抛出
     */
    public static LocalDate resolveFormingProductionDate(LocalDate scheduleDate,
                                                         int formingLogicalShiftOrder) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("scheduleDate must not be null");
        }
        validateFormingLogicalShiftOrder(formingLogicalShiftOrder);
        int dayOffset = Math.floorDiv(formingLogicalShiftOrder - 3, 3);
        return scheduleDate.plusDays(dayOffset);
    }

    /**
     * 解析未来停产来源任务对应的成型逻辑班次。
     *
     * <p>未来停产来源需求按胎侧来源班次顺序直接叠加成型偏移量，
     * 不复用当前班需求为避免与保证窗口重叠而使用的前一班起点。</p>
     *
     * @param shiftOrder          胎侧来源班次顺序，从1开始
     * @param formingShiftOffset 胎侧班次到成型班次的非负偏移量
     * @return 成型逻辑班次顺序；例如班次1且偏移量2返回CLASS3
     */
    public static int resolveFormingLogicalShiftOrder(int shiftOrder, int formingShiftOffset) {
        return Math.max(shiftOrder, MIN_SHIFT_ORDER) + Math.max(formingShiftOffset, 0);
    }

    /**
     * 解析成型逻辑班对应的工作日历班次序号，按夜班、早班、中班循环。
     *
     * @param formingLogicalShiftOrder 成型逻辑班次，必须为正数
     * @return 1表示夜班、2表示早班、3表示中班
     * @throws IllegalArgumentException 成型逻辑班次越界时抛出
     */
    public static int resolveFormingCalendarShiftOrder(int formingLogicalShiftOrder) {
        validateFormingLogicalShiftOrder(formingLogicalShiftOrder);
        return Math.floorMod(formingLogicalShiftOrder, 3) + 1;
    }

    /**
     * 解析上游工序（内衬/垫胶/胎面等六班工序）首班班次编码到「同时段成型班次」的偏移量。
     *
     * <p>上游六班首班为「中班」、成型 CLASS1 为「早班」，成型较上游领先一个班次，
     * 因此「上游班次顺序 + 偏移量 = 同时段成型逻辑班次」：</p>
     * <ul>
     *   <li>首班为中班("03")：偏移量 1（上游 class1 中班 ↔ 成型 CLASS2 中班）</li>
     *   <li>首班为早班("02")：偏移量 0（上游 class1 早班 ↔ 成型 CLASS1 早班）</li>
     *   <li>首班为夜班("01")：偏移量 -1，其同时段成型班次（排产日前一日夜班）不在成型 8 班窗口内</li>
     * </ul>
     *
     * @param firstShiftCode 上游工序首班班次编码（"01"夜班 / "02"早班 / "03"中班）
     * @return 同时段成型班次偏移量
     */
    public static int resolveFormingShiftOffset(String firstShiftCode) {
        return Integer.parseInt(firstShiftCode) - FORMING_FIRST_CALENDAR_SHIFT_ORDER;
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
        if (formingLogicalShiftOrder < MIN_SHIFT_ORDER) {
            throw new IllegalArgumentException("formingLogicalShiftOrder must be positive");
        }
    }
}
