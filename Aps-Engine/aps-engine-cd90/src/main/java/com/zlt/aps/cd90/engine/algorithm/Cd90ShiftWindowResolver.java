package com.zlt.aps.cd90.engine.algorithm;

import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
}
