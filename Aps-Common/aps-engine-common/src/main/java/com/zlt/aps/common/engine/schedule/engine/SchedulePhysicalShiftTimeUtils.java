package com.zlt.aps.common.engine.schedule.engine;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * TM 与 TC 共用的六班物理日期和时间窗口工具。
 *
 * <p>排程日期是业务排程日期，物理生产基准日由调用方传入的参数偏移决定。
 * 六班相对基准日的映射固定为 1班 D、2至4班 D+1、5至6班 D+2，
 * 不根据班次配置列表的时间先后推断日期。</p>
 */
public final class SchedulePhysicalShiftTimeUtils {

    /** 六个业务班次相对物理基准日的日期偏移。 */
    private static final int[] SHIFT_DAY_OFFSETS = {0, 1, 1, 1, 2, 2};

    /** 最大业务班次。 */
    private static final int MAX_SHIFT_ORDER = SHIFT_DAY_OFFSETS.length;

    private SchedulePhysicalShiftTimeUtils() {
    }

    /**
     * 解析模块传入的班次日期偏移参数。
     *
     * @param configuredValue 模块参数有效值
     * @param defaultValue 参数缺失或非法时的默认值
     * @return 解析后的日期偏移天数
     */
    public static int parseDateStartOffset(String configuredValue, int defaultValue) {
        if (StrUtil.isBlank(configuredValue)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(configuredValue.trim());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 按排程日期、参数偏移和班次顺序解析物理生产日期。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @return 班次物理生产日期；参数不完整或班次超出1至6时返回 null
     */
    public static Date resolveShiftPhysicalDate(Date scheduleDate, int dateStartOffset, Integer shiftOrder) {
        if (scheduleDate == null || shiftOrder == null || shiftOrder < 1 || shiftOrder > MAX_SHIFT_ORDER) {
            return null;
        }
        Date baseDate = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, dateStartOffset));
        return DateUtil.offsetDay(baseDate, SHIFT_DAY_OFFSETS[shiftOrder - 1]);
    }

    /**
     * 按历史公共六班物理日期规则解析班次开始时间。
     *
     * <p>该方法不接收跨天标识，仅为兼容既有调用保留；新的排程、人工和滚动链路必须使用完整窗口方法。</p>
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param planStartTime 班次计划开始时间
     * @return 班次物理开始时间；参数缺失或时间格式非法时返回 null
     */
    public static Date resolveShiftStartTime(Date scheduleDate, int dateStartOffset,
                                              Integer shiftOrder, String planStartTime) {
        Date shiftDate = resolveShiftPhysicalDate(scheduleDate, dateStartOffset, shiftOrder);
        if (shiftDate == null || StrUtil.isBlank(planStartTime)) {
            return null;
        }
        try {
            return parseDateTime(shiftDate, planStartTime);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 按公共六班物理日期规则解析班次实际开始时间。
     * 跨天班次的展示日期是班次结束所属日期，因此开始时间需要回退一天；
     * 非跨天班次仍使用展示日期。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param planStartTime 班次计划开始时间
     * @param crossDayFlag 是否跨天，1表示跨天
     * @return 班次实际开始时间；参数缺失或时间格式非法时返回 null
     */
    public static Date resolveShiftStartTime(Date scheduleDate, int dateStartOffset,
                                              Integer shiftOrder, String planStartTime,
                                              String crossDayFlag) {
        if (StrUtil.isBlank(planStartTime)) {
            return null;
        }
        Date actualStartDate = resolveActualShiftStartDate(scheduleDate, dateStartOffset,
                shiftOrder, crossDayFlag);
        if (actualStartDate == null) {
            return null;
        }
        try {
            return parseDateTime(actualStartDate, planStartTime);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 按公共六班物理日期规则解析班次完整实际时间窗口。
     * 跨天班次的展示日期作为结束日期，开始日期回退一天；非跨天班次使用展示日期。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param planStartTime 班次计划开始时间
     * @param planEndTime 班次计划结束时间
     * @param crossDayFlag 是否跨天，1表示跨天
     * @return 起止时间数组，下标0为开始时间、下标1为结束时间；参数无效时返回 null
     */
    public static Date[] resolveShiftWindow(Date scheduleDate, int dateStartOffset,
                                             Integer shiftOrder, String planStartTime,
                                             String planEndTime, String crossDayFlag) {
        try {
            return resolveShiftWindowInternal(scheduleDate, dateStartOffset, shiftOrder,
                    planStartTime, planEndTime, crossDayFlag);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 根据班次实际开始日期反推业务排程日期，供自动滚动触发器定位窗口。
     * 反推使用与正向解析相同的六班日期偏移和跨天展示日期规则。
     *
     * @param actualStartTime 班次实际开始时间
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param crossDayFlag 是否跨天，1表示跨天
     * @return 对应业务排程日期；参数不完整或班次超出1至6时返回 null
     */
    public static Date resolveScheduleDateByActualStart(Date actualStartTime, int dateStartOffset,
                                                         Integer shiftOrder, String crossDayFlag) {
        if (actualStartTime == null || shiftOrder == null || shiftOrder < 1
                || shiftOrder > MAX_SHIFT_ORDER) {
            return null;
        }
        Date displayDate = DateUtil.beginOfDay(actualStartTime);
        if ("1".equals(crossDayFlag)) {
            displayDate = DateUtil.offsetDay(displayDate, 1);
        }
        Date baseDate = DateUtil.offsetDay(displayDate, -SHIFT_DAY_OFFSETS[shiftOrder - 1]);
        return DateUtil.beginOfDay(DateUtil.offsetDay(baseDate, -dateStartOffset));
    }

    /**
     * 根据已加载的公共班次窗口模型构建六班物理时间窗口。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftTimeWindowMap 班次窗口配置
     * @param parseFailureConsumer 时间解析失败回调，可为空
     * @return 班次顺序到开始、结束时间的映射；无有效配置时返回空映射
     */
    public static Map<Integer, Date[]> buildShiftWindowMap(
            Date scheduleDate,
            int dateStartOffset,
            Map<Integer, ScheduleShiftTimeWindowModel> shiftTimeWindowMap,
            BiConsumer<ScheduleShiftTimeWindowModel, Exception> parseFailureConsumer) {
        Map<Integer, Date[]> result = new LinkedHashMap<>();
        if (scheduleDate == null || shiftTimeWindowMap == null || shiftTimeWindowMap.isEmpty()) {
            return result;
        }
        shiftTimeWindowMap.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> buildShiftWindow(scheduleDate, dateStartOffset, entry.getValue(),
                        result, parseFailureConsumer));
        return result;
    }

    /**
     * 格式化单个班次物理时间窗口，供排程过程日志记录映射证据。
     *
     * @param shiftWindow 班次开始、结束时间数组
     * @return 格式化后的窗口文本；窗口为空或不完整时返回“未解析”
     */
    public static String formatShiftWindow(Date[] shiftWindow) {
        if (shiftWindow == null || shiftWindow.length < 2 || shiftWindow[0] == null || shiftWindow[1] == null) {
            return "未解析";
        }
        return DateUtil.formatDateTime(shiftWindow[0]) + "~" + DateUtil.formatDateTime(shiftWindow[1]);
    }

    /**
     * 解析单个班次的起止时间，并按照跨天标识修正结束日期。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param window 班次窗口模型
     * @param result 结果映射
     * @param parseFailureConsumer 时间解析失败回调
     */
    private static void buildShiftWindow(Date scheduleDate, int dateStartOffset,
                                         ScheduleShiftTimeWindowModel window,
                                         Map<Integer, Date[]> result,
                                         BiConsumer<ScheduleShiftTimeWindowModel, Exception> parseFailureConsumer) {
        if (window.getShiftOrder() == null || window.getShiftOrder() < 1
                || window.getShiftOrder() > MAX_SHIFT_ORDER
                || StrUtil.isBlank(window.getPlanStartTime()) || StrUtil.isBlank(window.getPlanEndTime())) {
            return;
        }
        try {
            Date[] shiftWindow = resolveShiftWindowInternal(scheduleDate, dateStartOffset,
                    window.getShiftOrder(), window.getPlanStartTime(), window.getPlanEndTime(),
                    window.getCrossDayFlag());
            result.put(window.getShiftOrder(), shiftWindow);
        } catch (Exception exception) {
            if (parseFailureConsumer != null) {
                parseFailureConsumer.accept(window, exception);
            }
        }
    }

    /**
     * 解析单个班次窗口的内部实现，统一维护实际开始日期和结束日期规则。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param planStartTime 班次计划开始时间
     * @param planEndTime 班次计划结束时间
     * @param crossDayFlag 是否跨天，1表示跨天
     * @return 起止时间数组
     */
    private static Date[] resolveShiftWindowInternal(Date scheduleDate, int dateStartOffset,
                                                      Integer shiftOrder, String planStartTime,
                                                      String planEndTime, String crossDayFlag) {
        if (StrUtil.isBlank(planStartTime) || StrUtil.isBlank(planEndTime)) {
            return null;
        }
        Date actualStartDate = resolveActualShiftStartDate(scheduleDate, dateStartOffset,
                shiftOrder, crossDayFlag);
        if (actualStartDate == null) {
            return null;
        }
        Date startTime = parseDateTime(actualStartDate, planStartTime);
        Date endTime = parseDateTime(actualStartDate, planEndTime);
        if ("1".equals(crossDayFlag) || !endTime.after(startTime)) {
            endTime = DateUtil.offsetDay(endTime, 1);
        }
        return new Date[]{startTime, endTime};
    }

    /**
     * 解析班次实际开始日期，跨天班次回退到展示日期前一天。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param crossDayFlag 是否跨天，1表示跨天
     * @return 班次实际开始日期；参数不完整或班次无效时返回 null
     */
    private static Date resolveActualShiftStartDate(Date scheduleDate, int dateStartOffset,
                                                     Integer shiftOrder, String crossDayFlag) {
        Date shiftDate = resolveShiftPhysicalDate(scheduleDate, dateStartOffset, shiftOrder);
        if (shiftDate == null) {
            return null;
        }
        return "1".equals(crossDayFlag) ? DateUtil.offsetDay(shiftDate, -1) : shiftDate;
    }

    /**
     * 将班次日期和时间文本组合为日期时间。
     *
     * @param shiftDate 班次物理日期
     * @param timeText 时间文本，支持 HH:mm 或 HH:mm:ss
     * @return 解析后的日期时间
     */
    private static Date parseDateTime(Date shiftDate, String timeText) {
        String normalizedTime = timeText.trim();
        if (normalizedTime.length() == 5) {
            normalizedTime = normalizedTime + ":00";
        }
        return DateUtil.parse(DateUtil.formatDate(shiftDate) + " " + normalizedTime);
    }
}
