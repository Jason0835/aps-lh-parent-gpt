package com.zlt.aps.tc.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.domain.vo.TcRollingWindow;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧自动滚动物理班次窗口解析服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TcRollingWindowService {

    /** 自动滚动固定在班次开始前半小时触发。 */
    public static final int ROLLING_LEAD_MINUTES = 30;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final TcShiftConfigMapper shiftConfigMapper;

    /**
     * 查找当前分钟严格命中的胎侧自动滚动窗口。
     *
     * @param factoryCode 可选工厂编码，为空时检查全部工厂
     * @param triggerTime job触发时间
     * @return 命中的班次窗口
     */
    public List<TcRollingWindow> resolveDueWindows(String factoryCode, Date triggerTime) {
        LocalDateTime triggerMinute = this.toLocalDateTime(
                triggerTime == null ? new Date() : triggerTime).truncatedTo(ChronoUnit.MINUTES);
        Map<String, List<TcShiftConfig>> factoryConfigMap = this.loadShiftConfigs(factoryCode);
        Map<String, TcRollingWindow> windowMap = new LinkedHashMap<>();
        factoryConfigMap.forEach((currentFactoryCode, configList) -> {
            for (int offset = -1; offset <= 1; offset++) {
                LocalDate scheduleDate = triggerMinute.toLocalDate().plusDays(offset);
                this.resolveShiftStarts(scheduleDate, configList).forEach((shiftOrder, shiftStart) -> {
                    TcShiftConfig config = this.findConfig(configList, shiftOrder);
                    if (config == null || !shiftStart.minusMinutes(ROLLING_LEAD_MINUTES)
                            .truncatedTo(ChronoUnit.MINUTES).equals(triggerMinute)) {
                        return;
                    }
                    TcRollingWindow window = this.buildWindow(
                            currentFactoryCode, scheduleDate, shiftOrder, shiftStart);
                    windowMap.put(this.buildWindowKey(window), window);
                });
            }
        });
        return new ArrayList<>(windowMap.values());
    }

    /**
     * 加载一至六班配置；关闭班次也参与物理日期回绕计算。
     *
     * @param factoryCode 可选工厂编码
     * @return 按工厂分组的班次配置
     */
    private Map<String, List<TcShiftConfig>> loadShiftConfigs(String factoryCode) {
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<TcShiftConfig>()
                .between(TcShiftConfig::getShiftOrder, 1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .orderByAsc(TcShiftConfig::getFactoryCode)
                .orderByAsc(TcShiftConfig::getShiftOrder);
        wrapper.eq(StrUtil.isNotBlank(factoryCode), TcShiftConfig::getFactoryCode, StrUtil.trim(factoryCode));
        List<TcShiftConfig> configList = this.shiftConfigMapper.selectList(wrapper);
        return (configList == null ? new ArrayList<TcShiftConfig>() : configList).stream()
                .filter(config -> StrUtil.isNotBlank(config.getFactoryCode()))
                .collect(Collectors.groupingBy(config -> StrUtil.trim(config.getFactoryCode()),
                        LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 从业务日前一天开始，遇到开始时刻回绕时递增物理日期。
     *
     * @param scheduleDate 六班结果归属排程日期
     * @param configList 班次配置
     * @return 班序对应的物理开始时间
     */
    Map<Integer, LocalDateTime> resolveShiftStarts(LocalDate scheduleDate, List<TcShiftConfig> configList) {
        Map<Integer, LocalDateTime> resultMap = new LinkedHashMap<>();
        if (scheduleDate == null || configList == null) {
            return resultMap;
        }
        LocalDate physicalDate = scheduleDate.minusDays(1);
        LocalTime previousStartTime = null;
        List<TcShiftConfig> sortedList = configList.stream()
                .filter(Objects::nonNull)
                .filter(config -> config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TcShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
        for (TcShiftConfig config : sortedList) {
            LocalTime startTime = this.parseStartTime(config);
            if (startTime == null) {
                continue;
            }
            if (previousStartTime != null && !startTime.isAfter(previousStartTime)) {
                physicalDate = physicalDate.plusDays(1);
            }
            resultMap.put(config.getShiftOrder(), physicalDate.atTime(startTime));
            previousStartTime = startTime;
        }
        return resultMap;
    }

    /**
     * 解析班次开始时刻。
     *
     * @param config 班次配置
     * @return 开始时刻，配置非法时返回null
     */
    private LocalTime parseStartTime(TcShiftConfig config) {
        if (config == null || StrUtil.isBlank(config.getPlanStartTime())) {
            return null;
        }
        try {
            return LocalTime.parse(StrUtil.trim(config.getPlanStartTime()), TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            log.warn("胎侧自动滚动班次开始时间无效，factoryCode={}，shiftOrder={}，planStartTime={}",
                    config.getFactoryCode(), config.getShiftOrder(), config.getPlanStartTime());
            return null;
        }
    }

    /**
     * 查找指定班序配置。
     *
     * @param configList 班次配置
     * @param shiftOrder 班次顺序
     * @return 匹配配置
     */
    private TcShiftConfig findConfig(List<TcShiftConfig> configList, Integer shiftOrder) {
        return configList.stream().filter(config -> Objects.equals(shiftOrder, config.getShiftOrder()))
                .findFirst().orElse(null);
    }

    /**
     * 构造胎侧自动滚动窗口。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 六班结果归属排程日期
     * @param shiftOrder 班次顺序
     * @param shiftStart 物理开始时间
     * @return 自动滚动窗口
     */
    private TcRollingWindow buildWindow(String factoryCode, LocalDate scheduleDate,
                                        Integer shiftOrder, LocalDateTime shiftStart) {
        TcRollingWindow window = new TcRollingWindow();
        window.setFactoryCode(factoryCode);
        window.setScheduleDate(this.toDate(scheduleDate.atStartOfDay()));
        window.setStockDate(this.toDate(shiftStart.toLocalDate().atStartOfDay()));
        window.setTargetShiftOrder(shiftOrder);
        window.setShiftStartTime(this.toDate(shiftStart));
        return window;
    }

    /**
     * 构造窗口去重键。
     *
     * @param window 自动滚动窗口
     * @return 去重键
     */
    private String buildWindowKey(TcRollingWindow window) {
        return window.getFactoryCode() + "|" + window.getScheduleDate().getTime()
                + "|" + window.getTargetShiftOrder();
    }

    /**
     * Date转换为本地时间。
     *
     * @param date 时间
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 本地时间转换为Date。
     *
     * @param localDateTime 本地时间
     * @return Date时间
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
