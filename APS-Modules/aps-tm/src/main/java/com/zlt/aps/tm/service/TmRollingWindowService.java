package com.zlt.aps.tm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.domain.vo.TmRollingWindow;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
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
 * 胎面自动滚动物理班次窗口解析服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmRollingWindowService {

    /** 自动滚动固定在班次开始前半小时触发。 */
    public static final int ROLLING_LEAD_MINUTES = 30;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final TmShiftConfigMapper shiftConfigMapper;

    /**
     * 查找当前分钟严格命中的胎面自动滚动窗口。
     *
     * @param factoryCode 可选工厂编码，为空时检查全部工厂
     * @param triggerTime job触发时间
     * @return 命中的班次窗口
     */
    public List<TmRollingWindow> resolveDueWindows(String factoryCode, Date triggerTime) {
        LocalDateTime triggerMinute = this.toLocalDateTime(
                triggerTime == null ? new Date() : triggerTime).truncatedTo(ChronoUnit.MINUTES);
        Map<String, List<TmShiftConfig>> factoryConfigMap = this.loadShiftConfigs(factoryCode);
        Map<String, TmRollingWindow> windowMap = new LinkedHashMap<>();
        factoryConfigMap.forEach((currentFactoryCode, configList) -> {
            for (int offset = -1; offset <= 1; offset++) {
                LocalDate scheduleDate = triggerMinute.toLocalDate().plusDays(offset);
                this.resolveShiftStarts(scheduleDate, configList).forEach((shiftOrder, shiftStart) -> {
                    TmShiftConfig config = this.findConfig(configList, shiftOrder);
                    if (config == null || !"1".equals(config.getOpenFlag())
                            || !shiftStart.minusMinutes(ROLLING_LEAD_MINUTES)
                            .truncatedTo(ChronoUnit.MINUTES).equals(triggerMinute)) {
                        return;
                    }
                    TmRollingWindow window = this.buildWindow(
                            currentFactoryCode, scheduleDate, shiftOrder, shiftStart);
                    windowMap.put(this.buildWindowKey(window), window);
                });
            }
        });
        return new ArrayList<>(windowMap.values());
    }

    /**
     * 按业务排程日和班序解析单个胎面窗口，供人工滚动补齐物理库存日。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 六班结果归属排程日期
     * @param shiftOrder 班次顺序
     * @return 对应窗口，配置不存在或未开班时返回null
     */
    public TmRollingWindow resolveWindow(String factoryCode, Date scheduleDate, Integer shiftOrder) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null || shiftOrder == null) {
            return null;
        }
        List<TmShiftConfig> configList = this.loadShiftConfigs(factoryCode)
                .getOrDefault(StrUtil.trim(factoryCode), new ArrayList<>());
        TmShiftConfig targetConfig = this.findConfig(configList, shiftOrder);
        if (targetConfig == null || !"1".equals(targetConfig.getOpenFlag())) {
            return null;
        }
        LocalDate localScheduleDate = this.toLocalDateTime(scheduleDate).toLocalDate();
        LocalDateTime shiftStart = this.resolveShiftStarts(localScheduleDate, configList).get(shiftOrder);
        return shiftStart == null ? null
                : this.buildWindow(StrUtil.trim(factoryCode), localScheduleDate, shiftOrder, shiftStart);
    }

    /**
     * 加载一至六班配置；关闭班次也参与物理日期回绕计算。
     *
     * @param factoryCode 可选工厂编码
     * @return 按工厂分组的班次配置
     */
    private Map<String, List<TmShiftConfig>> loadShiftConfigs(String factoryCode) {
        LambdaQueryWrapper<TmShiftConfig> wrapper = new LambdaQueryWrapper<TmShiftConfig>()
                .between(TmShiftConfig::getShiftOrder, 1, TmScheduleConstants.TM_MAX_SHIFT_ORDER)
                .orderByAsc(TmShiftConfig::getFactoryCode)
                .orderByAsc(TmShiftConfig::getShiftOrder);
        wrapper.eq(StrUtil.isNotBlank(factoryCode), TmShiftConfig::getFactoryCode, StrUtil.trim(factoryCode));
        List<TmShiftConfig> configList = this.shiftConfigMapper.selectList(wrapper);
        return (configList == null ? new ArrayList<TmShiftConfig>() : configList).stream()
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
    Map<Integer, LocalDateTime> resolveShiftStarts(LocalDate scheduleDate, List<TmShiftConfig> configList) {
        Map<Integer, LocalDateTime> resultMap = new LinkedHashMap<>();
        if (scheduleDate == null || configList == null) {
            return resultMap;
        }
        LocalDate physicalDate = scheduleDate.minusDays(1);
        LocalTime previousStartTime = null;
        List<TmShiftConfig> sortedList = configList.stream()
                .filter(Objects::nonNull)
                .filter(config -> config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TmShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
        for (TmShiftConfig config : sortedList) {
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
    private LocalTime parseStartTime(TmShiftConfig config) {
        if (config == null || StrUtil.isBlank(config.getPlanStartTime())) {
            return null;
        }
        try {
            return LocalTime.parse(StrUtil.trim(config.getPlanStartTime()), TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            log.warn("胎面自动滚动班次开始时间无效，factoryCode={}，shiftOrder={}，planStartTime={}",
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
    private TmShiftConfig findConfig(List<TmShiftConfig> configList, Integer shiftOrder) {
        return configList.stream().filter(config -> Objects.equals(shiftOrder, config.getShiftOrder()))
                .findFirst().orElse(null);
    }

    /**
     * 构造胎面自动滚动窗口。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 六班结果归属排程日期
     * @param shiftOrder 班次顺序
     * @param shiftStart 物理开始时间
     * @return 自动滚动窗口
     */
    private TmRollingWindow buildWindow(String factoryCode, LocalDate scheduleDate,
                                        Integer shiftOrder, LocalDateTime shiftStart) {
        TmRollingWindow window = new TmRollingWindow();
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
    private String buildWindowKey(TmRollingWindow window) {
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
