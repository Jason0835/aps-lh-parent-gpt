package com.zlt.aps.common.engine.schedule.engine;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * TM 与 TC 共用的六班物理日期和时间窗口工具。
 *
 * <p>业务排程日期使用 {@link LocalDate}，班次实际时间点使用 {@link Date}。
 * 所有日期时间组合必须显式使用工厂 ZoneId，避免受到 JVM 默认时区影响。</p>
 */
public final class SchedulePhysicalShiftTimeUtils {

    /** 缺省工厂时区，兼容未配置工厂参数的生产环境。 */
    public static final String DEFAULT_FACTORY_ZONE_ID = "Asia/Ho_Chi_Minh";

    /** 六个业务班次相对物理基准日的日期偏移。 */
    private static final int[] SHIFT_DAY_OFFSETS = {0, 1, 1, 1, 2, 2};

    private static final int MAX_SHIFT_ORDER = SHIFT_DAY_OFFSETS.length;

    /**
     * 前一排程日已生产链尾只允许从早班结束边界 CLASS3 向前选择。
     */
    private static final int PREVIOUS_DAY_PREDECESSOR_MAX_SHIFT_ORDER = 3;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
     * 解析工厂时区参数，非法或为空时使用越南时区。
     *
     * @param configuredValue 工厂 ZoneId 参数
     * @return 有效工厂时区
     */
    public static ZoneId resolveFactoryZoneId(String configuredValue) {
        if (StrUtil.isBlank(configuredValue)) {
            return ZoneId.of(DEFAULT_FACTORY_ZONE_ID);
        }
        try {
            return ZoneId.of(configuredValue.trim());
        } catch (RuntimeException exception) {
            return ZoneId.of(DEFAULT_FACTORY_ZONE_ID);
        }
    }

    /**
     * 解析前一排程日已经完成生产范围内的最后有效班次。
     *
     * <p>当前一班启动时，上一排程日的 CLASS4～CLASS6 仍属于未来计划，不能作为实际生产链尾。
     * 因此本方法固定从 CLASS3 向 CLASS1 查找计划量大于0且顺序为有效整数的最后任务。</p>
     *
     * @param planQtyValueProvider        按班次顺序读取计划量的函数，可为空
     * @param sequenceValueProvider       按班次顺序读取顺序值的函数，可为空
     * @param acceptNumericStringSequence 是否兼容数字字符串形式的历史顺序值
     * @return CLASS1～CLASS3 范围内最后有效班次；无有效任务时返回 null
     */
    public static Integer resolvePreviousDayPredecessorShiftOrder(
            IntFunction<Object> planQtyValueProvider,
            IntFunction<Object> sequenceValueProvider,
            boolean acceptNumericStringSequence) {
        if (planQtyValueProvider == null || sequenceValueProvider == null) {
            return null;
        }
        for (int shiftOrder = PREVIOUS_DAY_PREDECESSOR_MAX_SHIFT_ORDER; shiftOrder >= 1; shiftOrder--) {
            try {
                Object planQtyValue = planQtyValueProvider.apply(shiftOrder);
                Object sequenceValue = sequenceValueProvider.apply(shiftOrder);
                if (parseDecimal(planQtyValue).compareTo(BigDecimal.ZERO) > 0
                        && parseSequence(sequenceValue, acceptNumericStringSequence) != null) {
                    return shiftOrder;
                }
            } catch (RuntimeException exception) {
                // 单个动态字段读取失败时按该班次无有效链尾处理，继续检查更早班次。
            }
        }
        return null;
    }

    /**
     * 将业务日期按工厂时区转换为当天零点时间点。
     *
     * @param date 业务日期
     * @param zoneId 工厂时区
     * @return 当天零点时间点
     */
    public static Date toStartOfDay(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.of(DEFAULT_FACTORY_ZONE_ID) : zoneId;
        return Date.from(date.atStartOfDay(effectiveZoneId).toInstant());
    }

    /**
     * 将业务日期按工厂时区转换为当天结束时间点。
     *
     * @param date 业务日期
     * @param zoneId 工厂时区
     * @return 当天结束时间点
     */
    public static Date toEndOfDay(LocalDate date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.of(DEFAULT_FACTORY_ZONE_ID) : zoneId;
        return Date.from(date.plusDays(1).atStartOfDay(effectiveZoneId).toInstant().minusNanos(1));
    }

    /**
     * 按排程日期、参数偏移和班次顺序解析物理生产日期。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @return 班次物理生产日期；参数无效时返回 null
     */
    public static LocalDate resolveShiftPhysicalDate(LocalDate scheduleDate, int dateStartOffset,
                                                      Integer shiftOrder) {
        if (scheduleDate == null || shiftOrder == null || shiftOrder < 1 || shiftOrder > MAX_SHIFT_ORDER) {
            return null;
        }
        return scheduleDate.plusDays(dateStartOffset + SHIFT_DAY_OFFSETS[shiftOrder - 1]);
    }

    /**
     * 根据实际开始时间反推业务排程日期。
     *
     * @param actualStartTime 班次实际开始时间
     * @param dateStartOffset 一班相对排程日期的偏移天数
     * @param shiftOrder 班次顺序
     * @param crossDayFlag 是否跨天，1表示跨天
     * @param zoneId 工厂时区
     * @return 对应业务排程日期
     */
    public static LocalDate resolveScheduleDateByActualStart(Date actualStartTime, int dateStartOffset,
                                                               Integer shiftOrder, String crossDayFlag,
                                                               ZoneId zoneId) {
        if (actualStartTime == null || shiftOrder == null || shiftOrder < 1
                || shiftOrder > MAX_SHIFT_ORDER) {
            return null;
        }
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.of(DEFAULT_FACTORY_ZONE_ID) : zoneId;
        LocalDate displayDate = actualStartTime.toInstant().atZone(effectiveZoneId).toLocalDate();
        if ("1".equals(crossDayFlag)) {
            displayDate = displayDate.plusDays(1);
        }
        return displayDate.minusDays(SHIFT_DAY_OFFSETS[shiftOrder - 1] + dateStartOffset);
    }

    /**
     * 兼容旧调用的越南时区反推入口。
     *
     * @param actualStartTime 班次实际开始时间
     * @param dateStartOffset 日期偏移
     * @param shiftOrder 班次顺序
     * @param crossDayFlag 跨天标识
     * @return 业务排程日期
     */
    public static LocalDate resolveScheduleDateByActualStart(Date actualStartTime, int dateStartOffset,
                                                               Integer shiftOrder, String crossDayFlag) {
        return resolveScheduleDateByActualStart(actualStartTime, dateStartOffset, shiftOrder,
                crossDayFlag, ZoneId.of(DEFAULT_FACTORY_ZONE_ID));
    }

    /**
     * 解析班次完整实际时间窗口。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 日期偏移
     * @param shiftOrder 班次顺序
     * @param planStartTime 班次计划开始时间
     * @param planEndTime 班次计划结束时间
     * @param crossDayFlag 跨天标识
     * @param zoneId 工厂时区
     * @return 起止时间数组，下标0为开始时间、下标1为结束时间
     */
    public static Date[] resolveShiftWindow(LocalDate scheduleDate, int dateStartOffset,
                                             Integer shiftOrder, String planStartTime,
                                             String planEndTime, String crossDayFlag, ZoneId zoneId) {
        try {
            if (scheduleDate == null || StrUtil.isBlank(planStartTime) || StrUtil.isBlank(planEndTime)) {
                return null;
            }
            LocalDate physicalDate = resolveShiftPhysicalDate(scheduleDate, dateStartOffset, shiftOrder);
            if (physicalDate == null) {
                return null;
            }
            LocalDate actualStartDate = "1".equals(crossDayFlag) ? physicalDate.minusDays(1) : physicalDate;
            ZoneId effectiveZoneId = zoneId == null ? ZoneId.of(DEFAULT_FACTORY_ZONE_ID) : zoneId;
            LocalDateTime start = LocalDateTime.of(actualStartDate, parseTime(planStartTime));
            LocalDateTime end = LocalDateTime.of(actualStartDate, parseTime(planEndTime));
            if ("1".equals(crossDayFlag) || !end.isAfter(start)) {
                end = end.plusDays(1);
            }
            return new Date[]{Date.from(start.atZone(effectiveZoneId).toInstant()),
                    Date.from(end.atZone(effectiveZoneId).toInstant())};
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 兼容未传工厂时区的窗口解析入口。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 日期偏移
     * @param shiftOrder 班次顺序
     * @param planStartTime 开始时间
     * @param planEndTime 结束时间
     * @param crossDayFlag 跨天标识
     * @return 时间窗口
     */
    public static Date[] resolveShiftWindow(LocalDate scheduleDate, int dateStartOffset,
                                             Integer shiftOrder, String planStartTime,
                                             String planEndTime, String crossDayFlag) {
        return resolveShiftWindow(scheduleDate, dateStartOffset, shiftOrder, planStartTime,
                planEndTime, crossDayFlag, ZoneId.of(DEFAULT_FACTORY_ZONE_ID));
    }

    /**
     * 根据班次配置构建全部班次时间窗口。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 日期偏移
     * @param shiftTimeWindowMap 班次窗口配置
     * @param parseFailureConsumer 时间解析失败回调
     * @param zoneId 工厂时区
     * @return 班次顺序到时间窗口的映射
     */
    public static Map<Integer, Date[]> buildShiftWindowMap(
            LocalDate scheduleDate,
            int dateStartOffset,
            Map<Integer, ScheduleShiftTimeWindowModel> shiftTimeWindowMap,
            BiConsumer<ScheduleShiftTimeWindowModel, Exception> parseFailureConsumer,
            ZoneId zoneId) {
        Map<Integer, Date[]> result = new LinkedHashMap<>();
        if (scheduleDate == null || shiftTimeWindowMap == null || shiftTimeWindowMap.isEmpty()) {
            return result;
        }
        shiftTimeWindowMap.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ScheduleShiftTimeWindowModel window = entry.getValue();
                    try {
                        Date[] shiftWindow = resolveShiftWindow(scheduleDate, dateStartOffset,
                                window.getShiftOrder(), window.getPlanStartTime(), window.getPlanEndTime(),
                                window.getCrossDayFlag(), zoneId);
                        if (shiftWindow != null) {
                            result.put(window.getShiftOrder(), shiftWindow);
                        }
                    } catch (RuntimeException exception) {
                        if (parseFailureConsumer != null) {
                            parseFailureConsumer.accept(window, exception);
                        }
                    }
                });
        return result;
    }

    /**
     * 兼容未传工厂时区的窗口构建入口。
     *
     * @param scheduleDate 排程日期
     * @param dateStartOffset 日期偏移
     * @param shiftTimeWindowMap 班次窗口配置
     * @param parseFailureConsumer 时间解析失败回调
     * @return 班次窗口映射
     */
    public static Map<Integer, Date[]> buildShiftWindowMap(
            LocalDate scheduleDate,
            int dateStartOffset,
            Map<Integer, ScheduleShiftTimeWindowModel> shiftTimeWindowMap,
            BiConsumer<ScheduleShiftTimeWindowModel, Exception> parseFailureConsumer) {
        return buildShiftWindowMap(scheduleDate, dateStartOffset, shiftTimeWindowMap,
                parseFailureConsumer, ZoneId.of(DEFAULT_FACTORY_ZONE_ID));
    }

    /**
     * 格式化班次物理时间窗口供过程日志使用。
     *
     * @param shiftWindow 班次开始、结束时间数组
     * @param zoneId 工厂时区
     * @return 格式化窗口文本
     */
    public static String formatShiftWindow(Date[] shiftWindow, ZoneId zoneId) {
        if (shiftWindow == null || shiftWindow.length < 2 || shiftWindow[0] == null || shiftWindow[1] == null) {
            return "未解析";
        }
        ZoneId effectiveZoneId = zoneId == null ? ZoneId.of(DEFAULT_FACTORY_ZONE_ID) : zoneId;
        return DATE_TIME_FORMATTER.format(shiftWindow[0].toInstant().atZone(effectiveZoneId)) + "~"
                + DATE_TIME_FORMATTER.format(shiftWindow[1].toInstant().atZone(effectiveZoneId));
    }

    /**
     * 兼容未传工厂时区的日志格式化入口。
     *
     * @param shiftWindow 班次时间窗口
     * @return 格式化窗口文本
     */
    public static String formatShiftWindow(Date[] shiftWindow) {
        return formatShiftWindow(shiftWindow, ZoneId.of(DEFAULT_FACTORY_ZONE_ID));
    }

    /**
     * 将计划量动态字段转换为数值。
     *
     * @param value 动态字段原始值
     * @return 有效数值；空值或非法值返回0
     */
    private static BigDecimal parseDecimal(Object value) {
        if (value == null || (value instanceof String && StrUtil.isBlank((String) value))) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 按调用方兼容策略解析有效整数顺序。
     *
     * @param value                       动态顺序字段原始值
     * @param acceptNumericStringSequence 是否兼容数字字符串
     * @return 有效整数顺序；类型或数值非法时返回 null
     */
    private static Integer parseSequence(Object value, boolean acceptNumericStringSequence) {
        if (value instanceof Number) {
            try {
                return new BigDecimal(String.valueOf(value)).intValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                return null;
            }
        }
        if (!acceptNumericStringSequence || !(value instanceof String)
                || StrUtil.isBlank((String) value)) {
            return null;
        }
        try {
            return new BigDecimal(((String) value).trim()).intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            return null;
        }
    }

    private static LocalTime parseTime(String timeText) {
        String normalizedTime = timeText.trim();
        String[] timeParts = normalizedTime.split(":");
        if (timeParts.length == 2) {
            normalizedTime = normalizedTime + ":00";
        }
        if (!timeParts[0].isEmpty() && timeParts[0].length() == 1) {
            normalizedTime = "0" + normalizedTime;
        }
        return LocalTime.parse(normalizedTime);
    }
}
