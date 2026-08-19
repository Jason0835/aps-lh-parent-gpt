package com.zlt.aps.cd90.engine.algorithm;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 将班次配置解析为自动排程时间窗口。
 */
@Component
public class Cd90ShiftWindowResolver {

    private static final int ACTIVE = 1;
    private static final String SHIFT_NAME_MIDDLE = "中班";
    private static final String SHIFT_NAME_NIGHT = "夜班";
    private static final String SHIFT_NAME_DAY = "早班";
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 解析启用班次并按排程日、当天顺序和结果字段稳定排序。
     *
     * @param scheduleDate 结果表排程日期，窗口第一天为该日期前一天
     * @param configs 班次配置
     * @return 有序班次描述
     */
    public List<Cd90ShiftDescriptor> resolve(LocalDate scheduleDate, List<Cd90ShiftConfig> configs) {
        if (scheduleDate == null) {
            throw new IllegalArgumentException("排程日期不能为空");
        }
        if (configs == null) {
            return Collections.emptyList();
        }
        return configs.stream()
                .filter(this::isEnabled)
                .sorted(Comparator.comparing(this::scheduleDay)
                        .thenComparing(this::dayShiftOrder)
                        .thenComparing(this::shiftOrder)
                        .thenComparing(config -> safe(config.getClassField())))
                .map(config -> resolveOne(scheduleDate, config))
                .collect(Collectors.toList());
    }

    /**
     * 解析页面全量自动排程的资源基线班次。
     * 资源基线固定使用排程窗口首班次，不受任务实际启动日期影响。
     *
     * @param scheduleDate 排程日期
     * @param configs 启用班次配置
     * @return 排程窗口首班次
     */
    public Cd90ShiftDescriptor resolveScheduleBaselineShift(
            LocalDate scheduleDate, List<Cd90ShiftConfig> configs) {
        return this.resolve(scheduleDate, configs).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        I18nUtil.getMessage("ui.cd90.autoSchedule.resourceShiftConfigEmpty")));
    }

    /**
     * 根据任务启动时刻解析当前现场资源班次，快照日期沿用班次业务日期口径。
     *
     * @param executionTime 任务启动时刻
     * @param configs 启用班次配置
     * @return 当前资源班次
     */
    public Cd90ShiftDescriptor resolveCurrentResourceShift(
            LocalDateTime executionTime, List<Cd90ShiftConfig> configs) {
        if (executionTime == null) {
            throw new IllegalArgumentException(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.resourceExecutionTimeEmpty"));
        }
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.resourceShiftConfigEmpty"));
        }
        Map<String, Cd90ShiftConfig> configByShiftCode = configs.stream()
                .filter(this::isEnabled)
                .peek(this::validate)
                .collect(Collectors.toMap(
                        config -> config.getShiftCode().trim(),
                        Function.identity(),
                        this::mergeSameResourceShift,
                        LinkedHashMap::new));
        List<Cd90ShiftConfig> matchedConfigs = configByShiftCode.values().stream()
                .filter(config -> this.contains(executionTime.toLocalTime(), config))
                .collect(Collectors.toList());
        if (matchedConfigs.size() != 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.resourceShiftMatchInvalid"),
                    executionTime, matchedConfigs.size()));
        }
        Cd90ShiftConfig matchedConfig = matchedConfigs.get(0);
        LocalTime startTime = this.parseResourceTime(
                matchedConfig.getStartTime(), matchedConfig.getClassField(), "START_TIME");
        LocalTime endTime = this.parseResourceTime(
                matchedConfig.getEndTime(), matchedConfig.getClassField(), "END_TIME");
        boolean crossDay = Integer.valueOf(1).equals(matchedConfig.getIsCrossDay());
        LocalDate startDate = executionTime.toLocalDate();
        if (crossDay && executionTime.toLocalTime().isBefore(endTime)) {
            startDate = startDate.minusDays(1);
        }
        LocalDate endDate = crossDay ? startDate.plusDays(1) : startDate;
        LocalDate businessDate = crossDay ? endDate : startDate;
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        return Cd90ShiftDescriptor.builder()
                .shiftCode(matchedConfig.getShiftCode().trim())
                .shiftDisplayName(matchedConfig.getShiftName())
                .scheduleDate(businessDate)
                .startTime(start)
                .endTime(end)
                .durationSeconds((int) ChronoUnit.SECONDS.between(start, end))
                .build();
    }

    /** 同一班次编码的多日配置必须保持相同的资源班次定义。 */
    private Cd90ShiftConfig mergeSameResourceShift(Cd90ShiftConfig left,
                                                    Cd90ShiftConfig right) {
        if (!this.trim(left.getStartTime()).equals(this.trim(right.getStartTime()))
                || !this.trim(left.getEndTime()).equals(this.trim(right.getEndTime()))
                || !Objects.equals(left.getIsCrossDay(), right.getIsCrossDay())) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.resourceShiftDefinitionConflict"),
                    left.getShiftCode()));
        }
        return left;
    }

    /** 使用左闭右开区间判断任务启动时刻所属班次。 */
    private boolean contains(LocalTime currentTime, Cd90ShiftConfig config) {
        LocalTime startTime = this.parseResourceTime(
                config.getStartTime(), config.getClassField(), "START_TIME");
        LocalTime endTime = this.parseResourceTime(
                config.getEndTime(), config.getClassField(), "END_TIME");
        if (Integer.valueOf(1).equals(config.getIsCrossDay())) {
            return !currentTime.isBefore(startTime) || currentTime.isBefore(endTime);
        }
        return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
    }

    private LocalTime parseResourceTime(String value, String classField, String fieldName) {
        try {
            return LocalTime.parse(value.trim(), TIME_FORMATTER);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.resourceShiftTimeInvalid"),
                    classField, fieldName), exception);
        }
    }

    private Cd90ShiftDescriptor resolveOne(LocalDate scheduleDate, Cd90ShiftConfig config) {
        validate(config);
        LocalDate shiftDate = scheduleDate.plusDays(scheduleDay(config) - 2L);
        LocalDateTime start = LocalDateTime.of(shiftDate, LocalTime.parse(config.getStartTime()));
        LocalDate endDate = Integer.valueOf(1).equals(config.getIsCrossDay())
                ? shiftDate.plusDays(1) : shiftDate;
        LocalDate displayDate = Integer.valueOf(1).equals(config.getIsCrossDay())
                ? endDate : shiftDate;
        LocalDateTime end = LocalDateTime.of(endDate, LocalTime.parse(config.getEndTime()));
        long durationSeconds = ChronoUnit.SECONDS.between(start, end);
        if (durationSeconds <= 0 || durationSeconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("班次开始结束时间无效: " + config.getClassField());
        }
        return Cd90ShiftDescriptor.builder()
                .shiftCode(config.getShiftCode())
                .shiftDisplayName(shiftDisplayName(config, displayDate))
                .scheduleDate(displayDate)
                .classField(config.getClassField())
                .shiftOrder(config.getShiftOrder()).startTime(start).endTime(end)
                .durationSeconds((int) durationSeconds).build();
    }

    /** 组装面向页面的班次名称，跨夜班按结束日展示。 */
    private String shiftDisplayName(Cd90ShiftConfig config, LocalDate shiftDate) {
        String shiftName = shiftNameForDisplay(config);
        if (!StringUtils.hasText(shiftName) || shiftDate == null) {
            return config.getClassField();
        }
        return shiftName + String.format("%02d/%02d",
                shiftDate.getMonthValue(), shiftDate.getDayOfMonth());
    }

    /** 班次中文集中在此方法，后续多语言只需替换这里的映射来源。 */
    private String shiftNameForDisplay(Cd90ShiftConfig config) {
        if (config == null || !StringUtils.hasText(config.getShiftName())) {
            return null;
        }
        String shiftName = config.getShiftName().trim();
        if (SHIFT_NAME_MIDDLE.equals(shiftName)) {
            return SHIFT_NAME_MIDDLE;
        }
        if (SHIFT_NAME_NIGHT.equals(shiftName)) {
            return SHIFT_NAME_NIGHT;
        }
        if (SHIFT_NAME_DAY.equals(shiftName)) {
            return SHIFT_NAME_DAY;
        }
        return shiftName;
    }

    private boolean isEnabled(Cd90ShiftConfig config) {
        return config != null && Integer.valueOf(ACTIVE).equals(config.getIsActive());
    }

    private void validate(Cd90ShiftConfig config) {
        if (!StringUtils.hasText(config.getShiftCode())
                || !StringUtils.hasText(config.getClassField())
                || !StringUtils.hasText(config.getStartTime())
                || !StringUtils.hasText(config.getEndTime())) {
            throw new IllegalArgumentException("启用班次的编码、结果字段和起止时间不能为空");
        }
    }

    private int scheduleDay(Cd90ShiftConfig config) {
        return config.getScheduleDay() == null ? Integer.MAX_VALUE : config.getScheduleDay();
    }

    private int dayShiftOrder(Cd90ShiftConfig config) {
        return config.getDayShiftOrder() == null ? Integer.MAX_VALUE : config.getDayShiftOrder();
    }

    private int shiftOrder(Cd90ShiftConfig config) {
        return config.getShiftOrder() == null ? Integer.MAX_VALUE : config.getShiftOrder();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
