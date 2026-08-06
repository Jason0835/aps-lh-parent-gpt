package com.zlt.aps.tq.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tq.api.constant.TqScheduleConstants;
import com.zlt.aps.tq.api.domain.entity.TqShiftConfig;
import com.zlt.aps.tq.domain.vo.TqRollingWindow;
import com.zlt.aps.tq.mapper.TqShiftConfigMapper;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 胎圈自动滚动物理班次窗口解析服务。
 *
 * <p>对齐胎面 TmRollingWindowService，扫描 T_TQ_SHIFT_CONFIG 配置，
 * 在班次开始前 30 分钟（参数化）触发窗口命中，构造 TqRollingWindow 供
 * TqAutoRollingApplicationService 执行库存同步、校验和滚动调量。</p>
 *
 * @author APS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TqRollingWindowService {

    /** 自动滚动固定在班次开始前半小时触发（与胎面对齐默认值）。 */
    public static final int ROLLING_LEAD_MINUTES = 30;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final TqShiftConfigMapper shiftConfigMapper;

    /**
     * 查找当前分钟严格命中的胎圈自动滚动窗口。
     *
     * @param factoryCode 可选工厂编码，为空时检查全部工厂
     * @param triggerTime job触发时间
     * @return 命中的班次窗口
     */
    public List<TqRollingWindow> resolveDueWindows(String factoryCode, java.util.Date triggerTime) {
        LocalDateTime triggerMinute = this.toLocalDateTime(
                triggerTime == null ? new java.util.Date() : triggerTime).truncatedTo(ChronoUnit.MINUTES);
        Map<String, List<TqShiftConfig>> factoryConfigMap = this.loadShiftConfigs(factoryCode);
        Map<String, TqRollingWindow> windowMap = new LinkedHashMap<>();
        factoryConfigMap.forEach((currentFactoryCode, configList) -> {
            for (int offset = -1; offset <= 1; offset++) {
                LocalDate scheduleDate = triggerMinute.toLocalDate().plusDays(offset);
                this.resolveShiftStarts(scheduleDate, configList).forEach((shiftOrder, shiftStart) -> {
                    TqShiftConfig config = this.findConfig(configList, shiftOrder);
                    if (config == null || !"1".equals(config.getOpenFlag())
                            || !shiftStart.minusMinutes(ROLLING_LEAD_MINUTES)
                            .truncatedTo(ChronoUnit.MINUTES).equals(triggerMinute)) {
                        return;
                    }
                    TqRollingWindow window = this.buildWindow(
                            currentFactoryCode, scheduleDate, shiftOrder, shiftStart);
                    windowMap.put(this.buildWindowKey(window), window);
                });
            }
        });
        return new ArrayList<>(windowMap.values());
    }

    /**
     * 按业务排程日和班序解析单个胎圈窗口，供人工滚动补齐物理库存日。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 六班结果归属排程日期
     * @param shiftOrder 班次顺序
     * @return 对应窗口，配置不存在或未开班时返回null
     */
    public TqRollingWindow resolveWindow(String factoryCode, java.util.Date scheduleDate, Integer shiftOrder) {
        if (StrUtil.isBlank(factoryCode) || scheduleDate == null || shiftOrder == null) {
            return null;
        }
        List<TqShiftConfig> configList = this.loadShiftConfigs(factoryCode)
                .getOrDefault(StrUtil.trim(factoryCode), new ArrayList<>());
        TqShiftConfig targetConfig = this.findConfig(configList, shiftOrder);
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
    private Map<String, List<TqShiftConfig>> loadShiftConfigs(String factoryCode) {
        LambdaQueryWrapper<TqShiftConfig> wrapper = new LambdaQueryWrapper<TqShiftConfig>()
                .between(TqShiftConfig::getShiftOrder, 1, TqScheduleConstants.TQ_MAX_SHIFT_ORDER)
                .orderByAsc(TqShiftConfig::getFactoryCode)
                .orderByAsc(TqShiftConfig::getShiftOrder);
        wrapper.eq(StrUtil.isNotBlank(factoryCode), TqShiftConfig::getFactoryCode, StrUtil.trim(factoryCode));
        List<TqShiftConfig> configList = this.shiftConfigMapper.selectList(wrapper);
        return (configList == null ? new ArrayList<TqShiftConfig>() : configList).stream()
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
    Map<Integer, LocalDateTime> resolveShiftStarts(LocalDate scheduleDate, List<TqShiftConfig> configList) {
        Map<Integer, LocalDateTime> resultMap = new LinkedHashMap<>();
        if (scheduleDate == null || configList == null) {
            return resultMap;
        }
        LocalDate physicalDate = scheduleDate.minusDays(1);
        LocalTime previousStartTime = null;
        List<TqShiftConfig> sortedList = configList.stream()
                .filter(Objects::nonNull)
                .filter(config -> config.getShiftOrder() != null)
                .sorted(Comparator.comparing(TqShiftConfig::getShiftOrder))
                .collect(Collectors.toList());
        for (TqShiftConfig config : sortedList) {
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
    private LocalTime parseStartTime(TqShiftConfig config) {
        if (config == null || StrUtil.isBlank(config.getPlanStartTime())) {
            return null;
        }
        try {
            return LocalTime.parse(StrUtil.trim(config.getPlanStartTime()), TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            log.warn("胎圈自动滚动班次开始时间无效，factoryCode={}，shiftOrder={}，planStartTime={}",
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
    private TqShiftConfig findConfig(List<TqShiftConfig> configList, Integer shiftOrder) {
        return configList.stream().filter(config -> Objects.equals(shiftOrder, config.getShiftOrder()))
                .findFirst().orElse(null);
    }

    /**
     * 构造胎圈自动滚动窗口。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 六班结果归属排程日期
     * @param shiftOrder 班次顺序
     * @param shiftStart 物理开始时间
     * @return 自动滚动窗口
     */
    private TqRollingWindow buildWindow(String factoryCode, LocalDate scheduleDate,
                                         Integer shiftOrder, LocalDateTime shiftStart) {
        TqRollingWindow window = new TqRollingWindow();
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
    private String buildWindowKey(TqRollingWindow window) {
        return window.getFactoryCode() + "|" + window.getScheduleDate().getTime()
                + "|" + window.getTargetShiftOrder();
    }

    /**
     * Date转换为本地时间。
     *
     * @param date 时间
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 本地时间转换为Date。
     *
     * @param localDateTime 本地时间
     * @return Date时间
     */
    private java.util.Date toDate(LocalDateTime localDateTime) {
        return java.util.Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
