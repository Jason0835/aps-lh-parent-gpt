package com.zlt.aps.tm.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import com.zlt.aps.tm.mapper.TmShiftConfigMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 胎面六班实际开始时间解析服务。
 *
 * <p>班次日期映射与排程列表表头保持一致：第1班为基准日，
 * 第2至4班为基准日后一天，第5至6班为基准日后两天。</p>
 */
@Service
public class TmShiftStartTimeResolver {

    /** 班次开始时间格式。 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 六班相对基准日的日期偏移。 */
    private static final int[] SHIFT_DAY_OFFSETS = {0, 1, 1, 1, 2, 2};

    private final TmParamsMapper paramsMapper;

    private final TmShiftConfigMapper shiftConfigMapper;

    /**
     * 创建班次开始时间解析服务。
     *
     * @param paramsMapper 胎面参数 Mapper
     * @param shiftConfigMapper 胎面班次配置 Mapper
     */
    public TmShiftStartTimeResolver(TmParamsMapper paramsMapper, TmShiftConfigMapper shiftConfigMapper) {
        this.paramsMapper = paramsMapper;
        this.shiftConfigMapper = shiftConfigMapper;
    }

    /**
     * 按工厂、排程日期解析六个结果班次的实际开始时间。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 班次顺序与实际开始时间的映射，无法解析的班次不放入结果
     */
    public Map<Integer, Date> resolveShiftStartTimes(String factoryCode, Date scheduleDate) {
        if (scheduleDate == null || factoryCode == null || factoryCode.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        int dateStartOffset = this.resolveShiftDateStartOffset(factoryCode);
        Date baseDate = DateUtil.beginOfDay(DateUtil.offsetDay(scheduleDate, dateStartOffset));
        List<TmShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(new LambdaQueryWrapper<TmShiftConfig>()
                .eq(TmShiftConfig::getFactoryCode, factoryCode)
                .between(TmShiftConfig::getShiftOrder, 1, TmScheduleConstants.TM_MAX_SHIFT_ORDER)
                .orderByAsc(TmShiftConfig::getShiftOrder));
        Map<Integer, Date> result = new LinkedHashMap<>();
        if (shiftConfigList == null) {
            return result;
        }
        shiftConfigList.stream()
                .filter(config -> config.getShiftOrder() != null)
                .filter(config -> config.getShiftOrder() >= 1
                        && config.getShiftOrder() <= TmScheduleConstants.TM_MAX_SHIFT_ORDER)
                .forEach(config -> {
                    LocalTime startTime = this.parseStartTime(config.getPlanStartTime());
                    if (startTime == null) {
                        return;
                    }
                    int shiftOrder = config.getShiftOrder();
                    Date shiftDate = DateUtil.offsetDay(baseDate, SHIFT_DAY_OFFSETS[shiftOrder - 1]);
                    String dateTime = DateUtil.format(shiftDate, "yyyy-MM-dd") + " "
                            + startTime.format(TIME_FORMATTER);
                    result.put(shiftOrder, DateUtil.parseDateTime(dateTime));
                });
        return result;
    }

    /**
     * 读取班次表头基准日期的参数偏移。
     *
     * @param factoryCode 工厂编码
     * @return 相对排程日期的偏移天数
     */
    private int resolveShiftDateStartOffset(String factoryCode) {
        if (factoryCode == null || factoryCode.trim().isEmpty()) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        TmParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TmParams>()
                .eq(TmParams::getFactoryCode, factoryCode)
                .eq(TmParams::getParamCode, TmScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET)
                .eq(TmParams::getEnableStatus, TmYesNoEnum.YES.getCode()));
        if (params == null) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        String value = params.getParamValue() == null || params.getParamValue().trim().isEmpty()
                ? params.getDefaultValue() : params.getParamValue();
        if (value == null || value.trim().isEmpty()) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return TmScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
    }

    /**
     * 解析班次开始时间。
     *
     * @param startTime 班次开始时间文本
     * @return 解析后的时间，格式非法时返回 null
     */
    private LocalTime parseStartTime(String startTime) {
        if (startTime == null || startTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(startTime.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
