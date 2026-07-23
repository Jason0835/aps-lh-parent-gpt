package com.zlt.aps.cd15.engine.algorithm;

import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.common.core.enums.ThreeShiftEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 将CD15班次配置解析为自动排程和定时滚动共用的时间窗口。
 */
@Component
public class Cd15ShiftWindowResolver {

    private static final int ACTIVE = 1;
    private static final Pattern CLASS_FIELD_PATTERN = Pattern.compile("CLASS([1-8])");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 解析启用班次并按排程日、当天顺序、全局顺序和结果字段稳定排序。
     *
     * @param scheduleDate 结果表排程日期，配置第一天从该日期前一天开始
     * @param configs 班次配置
     * @return 有序班次描述
     */
    public List<Cd15ShiftDescriptor> resolve(
            LocalDate scheduleDate, List<Cd15ShiftConfig> configs) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("排程日期不能为空");
        }
        if (configs == null) {
            return Collections.emptyList();
        }
        List<Cd15ShiftConfig> enabledConfigs = configs.stream()
                .filter(this::isEnabled)
                .sorted(Comparator.comparingInt(this::scheduleDay)
                        .thenComparingInt(this::dayShiftOrder)
                        .thenComparingInt(this::shiftOrder)
                        .thenComparing(config ->
                                this.normalizeClassField(config.getClassField())))
                .collect(Collectors.toList());
        this.validateDuplicateClassField(enabledConfigs);
        return enabledConfigs.stream()
                .map(config -> this.resolveOne(scheduleDate, config))
                .collect(Collectors.toList());
    }

    /**
     * 根据任务启动时刻解析当前现场资源班次，快照日期沿用班次业务日期口径。
     *
     * @param executionTime 任务启动时刻
     * @param configs 班次配置
     * @return 当前资源班次
     */
    public Cd15ShiftDescriptor resolveCurrentResourceShift(
            LocalDateTime executionTime, List<Cd15ShiftConfig> configs) {
        if (executionTime == null) {
            throw new IllegalArgumentException("任务启动时刻不能为空");
        }
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("未找到启用的CD15班次配置");
        }
        Map<String, Cd15ShiftConfig> configByShiftCode = configs.stream()
                .filter(this::isEnabled)
                .peek(this::validate)
                .collect(Collectors.toMap(
                        config -> config.getShiftCode().trim(),
                        Function.identity(),
                        this::mergeSameResourceShift,
                        LinkedHashMap::new));
        List<Cd15ShiftConfig> matched = configByShiftCode.values().stream()
                .filter(config -> this.contains(
                        executionTime.toLocalTime(), config))
                .collect(Collectors.toList());
        if (matched.size() != 1) {
            throw new IllegalArgumentException("任务启动时刻 " + executionTime
                    + " 必须且只能命中一个启用班次, 当前命中数=" + matched.size());
        }
        Cd15ShiftConfig config = matched.get(0);
        LocalTime startTime = this.parseTime(
                config.getStartTime(), config.getClassField(), "开始时间");
        LocalTime endTime = this.parseTime(
                config.getEndTime(), config.getClassField(), "结束时间");
        boolean crossDay = Integer.valueOf(1).equals(config.getIsCrossDay());
        LocalDate startDate = executionTime.toLocalDate();
        if (crossDay && executionTime.toLocalTime().isBefore(endTime)) {
            startDate = startDate.minusDays(1);
        }
        LocalDate endDate = crossDay ? startDate.plusDays(1) : startDate;
        LocalDate businessDate = crossDay ? endDate : startDate;
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        return Cd15ShiftDescriptor.builder()
                .shiftCode(config.getShiftCode().trim())
                .shiftDisplayName(config.getShiftName())
                .scheduleDate(businessDate)
                .startTime(start)
                .endTime(end)
                .durationSeconds((int) ChronoUnit.SECONDS.between(start, end))
                .build();
    }

    /** 同一班次编码的多日配置必须保持相同的资源班次定义。 */
    private Cd15ShiftConfig mergeSameResourceShift(Cd15ShiftConfig left,
                                                    Cd15ShiftConfig right) {
        if (!this.trim(left.getStartTime()).equals(this.trim(right.getStartTime()))
                || !this.trim(left.getEndTime()).equals(this.trim(right.getEndTime()))
                || !left.getIsCrossDay().equals(right.getIsCrossDay())) {
            throw new IllegalArgumentException("同一班次编码存在不同时间定义: "
                    + left.getShiftCode());
        }
        return left;
    }

    /** 使用左闭右开区间判断任务启动时刻所属班次。 */
    private boolean contains(LocalTime currentTime, Cd15ShiftConfig config) {
        LocalTime startTime = this.parseTime(
                config.getStartTime(), config.getClassField(), "开始时间");
        LocalTime endTime = this.parseTime(
                config.getEndTime(), config.getClassField(), "结束时间");
        if (Integer.valueOf(1).equals(config.getIsCrossDay())) {
            return !currentTime.isBefore(startTime) || currentTime.isBefore(endTime);
        }
        return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
    }
    private Cd15ShiftDescriptor resolveOne(
            LocalDate scheduleDate, Cd15ShiftConfig config) {
        this.validate(config);
        String shiftCode = config.getShiftCode().trim();
        String classField = this.normalizeClassField(config.getClassField());
        LocalTime startTime = this.parseTime(
                config.getStartTime(), classField, "开始时间");
        LocalTime endTime = this.parseTime(
                config.getEndTime(), classField, "结束时间");
        LocalDate startDate = scheduleDate.plusDays(config.getScheduleDay() - 2L);
        LocalDate endDate = Integer.valueOf(1).equals(config.getIsCrossDay())
                ? startDate.plusDays(1) : startDate;
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        long durationSeconds = ChronoUnit.SECONDS.between(start, end);
        if (durationSeconds <= 0 || durationSeconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("班次开始结束时间无效: " + classField);
        }
        LocalDate displayDate = Integer.valueOf(1).equals(config.getIsCrossDay())
                ? endDate : startDate;
        return Cd15ShiftDescriptor.builder()
                .shiftCode(shiftCode)
                .shiftDisplayName(this.shiftDisplayName(
                        config.getShiftName(), classField, displayDate))
                .scheduleDate(displayDate)
                .classField(classField)
                .shiftOrder(config.getShiftOrder())
                .startTime(start)
                .endTime(end)
                .durationSeconds((int) durationSeconds)
                .build();
    }

    /**
     * 校验运行时必需字段、标准三班编码和CLASS字段范围。
     */
    private void validate(Cd15ShiftConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("启用班次配置不能为空");
        }
        String shiftCode = this.trim(config.getShiftCode());
        if (!ThreeShiftEnum.isValidCode(shiftCode)) {
            throw new IllegalArgumentException(
                    "班次编码必须为标准三班编码01、02、03: " + shiftCode);
        }
        String classField = this.normalizeClassField(config.getClassField());
        if (!CLASS_FIELD_PATTERN.matcher(classField).matches()) {
            throw new IllegalArgumentException(
                    "班次结果字段只能取CLASS1至CLASS8: " + config.getClassField());
        }
        if (!StringUtils.hasText(config.getStartTime())
                || !StringUtils.hasText(config.getEndTime())) {
            throw new IllegalArgumentException(
                    "启用班次的起止时间不能为空: " + classField);
        }
        if (config.getScheduleDay() == null || config.getScheduleDay() <= 0
                || config.getDayShiftOrder() == null
                || config.getDayShiftOrder() <= 0
                || config.getShiftOrder() == null
                || config.getShiftOrder() <= 0) {
            throw new IllegalArgumentException(
                    "启用班次的排程天数、当天顺序和班次顺序必须为正整数: "
                            + classField);
        }
        if (!Integer.valueOf(0).equals(config.getIsCrossDay())
                && !Integer.valueOf(1).equals(config.getIsCrossDay())) {
            throw new IllegalArgumentException(
                    "启用班次的跨天标识只能取0或1: " + classField);
        }
        if (config.getShiftHours() != null && config.getShiftHours() <= 0) {
            throw new IllegalArgumentException(
                    "启用班次的班次时长必须为正整数: " + classField);
        }
    }

    /**
     * 同一结果CLASS只能配置一个启用窗口。
     */
    private void validateDuplicateClassField(List<Cd15ShiftConfig> configs) {
        Map<String, Long> counts = configs.stream()
                .map(Cd15ShiftConfig::getClassField)
                .map(this::normalizeClassField)
                .collect(Collectors.groupingBy(
                        Function.identity(), Collectors.counting()));
        Optional<String> duplicate = counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1L)
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst();
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException(
                    "启用班次结果字段重复: " + duplicate.get());
        }
    }

    private LocalTime parseTime(
            String value, String classField, String fieldName) {
        try {
            return LocalTime.parse(value.trim(), TIME_FORMATTER);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    classField + fieldName + "格式必须为HH:mm:ss", exception);
        }
    }

    private String shiftDisplayName(
            String shiftName, String classField, LocalDate displayDate) {
        if (!StringUtils.hasText(shiftName) || displayDate == null) {
            return classField;
        }
        return shiftName.trim() + String.format("%02d/%02d",
                displayDate.getMonthValue(), displayDate.getDayOfMonth());
    }

    private boolean isEnabled(Cd15ShiftConfig config) {
        return config != null
                && Integer.valueOf(ACTIVE).equals(config.getIsActive());
    }

    private int scheduleDay(Cd15ShiftConfig config) {
        return config.getScheduleDay() == null
                ? Integer.MAX_VALUE : config.getScheduleDay();
    }

    private int dayShiftOrder(Cd15ShiftConfig config) {
        return config.getDayShiftOrder() == null
                ? Integer.MAX_VALUE : config.getDayShiftOrder();
    }

    private int shiftOrder(Cd15ShiftConfig config) {
        return config.getShiftOrder() == null
                ? Integer.MAX_VALUE : config.getShiftOrder();
    }

    private String normalizeClassField(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}