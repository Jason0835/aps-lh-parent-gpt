package com.zlt.aps.tc.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcParams;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.api.enums.TcYesNoEnum;
import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 胎侧六班实际开始时间解析服务。
 *
 * <p>班次日期映射与胎侧排程列表表头保持一致，用于前端禁用已开始班次，
 * 同时作为后端人工操作的最终时间校验依据。</p>
 */
@Service
public class TcShiftStartTimeResolver {

    /** 班次开始时间格式。 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 六班相对班次表头基准日的日期偏移。 */
    private static final int[] SHIFT_DAY_OFFSETS = {0, 1, 1, 1, 2, 2};

    private final TcParamsMapper paramsMapper;

    private final TcShiftConfigMapper shiftConfigMapper;

    /**
     * 创建胎侧班次开始时间解析服务。
     *
     * @param paramsMapper 胎侧参数 Mapper
     * @param shiftConfigMapper 胎侧班次配置 Mapper
     */
    public TcShiftStartTimeResolver(TcParamsMapper paramsMapper, TcShiftConfigMapper shiftConfigMapper) {
        this.paramsMapper = paramsMapper;
        this.shiftConfigMapper = shiftConfigMapper;
    }

    /**
     * 按工厂、排程日期解析六个班次的实际开始时间。
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
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(new LambdaQueryWrapper<TcShiftConfig>()
                .eq(TcShiftConfig::getFactoryCode, factoryCode)
                .between(TcShiftConfig::getShiftOrder, 1, TcScheduleConstants.TC_MAX_SHIFT_ORDER)
                .orderByAsc(TcShiftConfig::getShiftOrder));
        Map<Integer, Date> result = new LinkedHashMap<>();
        if (shiftConfigList == null) {
            return result;
        }
        shiftConfigList.stream()
                .filter(config -> config.getShiftOrder() != null)
                .filter(config -> config.getShiftOrder() >= 1
                        && config.getShiftOrder() <= TcScheduleConstants.TC_MAX_SHIFT_ORDER)
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
        TcParams params = this.paramsMapper.selectOne(new LambdaQueryWrapper<TcParams>()
                .eq(TcParams::getFactoryCode, factoryCode)
                .eq(TcParams::getParamCode, TcScheduleConstants.PARAM_SHIFT_DATE_START_OFFSET)
                .eq(TcParams::getEnableStatus, TcYesNoEnum.YES.getCode()));
        if (params == null) {
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        String value = params.getParamValue() == null || params.getParamValue().trim().isEmpty()
                ? params.getDefaultValue() : params.getParamValue();
        if (value == null || value.trim().isEmpty()) {
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return TcScheduleConstants.DEFAULT_SHIFT_DATE_START_OFFSET;
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
        String normalizedTime = startTime.trim();
        if (normalizedTime.length() == 5) {
            normalizedTime = normalizedTime + ":00";
        }
        try {
            return LocalTime.parse(normalizedTime, TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
