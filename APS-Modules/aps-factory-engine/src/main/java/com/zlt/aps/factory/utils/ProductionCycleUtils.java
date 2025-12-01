package com.zlt.aps.factory.utils;

import com.tlt.aps.constant.StringConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排产周期工具类型
 * 纯计算类
 *
 * @author ZLT
 * @date 20250927
 */
@Slf4j
public class ProductionCycleUtils {

    /**
     * 解析排产周期内，特殊天限定
     * 天,产能数;天,产能数
     *
     * @param specialDayLimitValue
     * @return
     */
    public static Map<Integer, Long> analysisSpecialDayLimit(String specialDayLimitValue) {
        if (StringUtils.isBlank(specialDayLimitValue)) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> limitMap = new HashMap<>();
        String[] specialDayLimitList = specialDayLimitValue.split(StringConstant.SPLIT_SEMICOLON);
        for (int index = 0; index < specialDayLimitList.length; index++) {
            String limitConfiguration = specialDayLimitList[index];
            if (StringUtils.isBlank(limitConfiguration)) {
                continue;
            }
            String[] dayLimitConfiguration = limitConfiguration.split(StringConstant.COMMA);
            if (dayLimitConfiguration.length != 2) {
                continue;
            }
            Integer day = Integer.valueOf(dayLimitConfiguration[BigDecimal.ZERO.intValue()]);
            Long capacity = Long.valueOf(dayLimitConfiguration[BigDecimal.ONE.intValue()]);
            limitMap.put(day, capacity);
        }
        return limitMap;
    }

    /**
     * 根据周期，获取日期在周期所处范围
     *
     * @param startDate 周期起始日
     * @param endDate   周期结束日
     * @return
     */
    public static Map<Integer, Integer> getDayByCycleNumber(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return Collections.emptyMap();
        }
        if (startDate.after(endDate)) {
            return Collections.emptyMap();
        }
        List<Integer> daySortList = getCycleDayList(startDate, endDate);
        Map<Integer, Integer> dayNumberMap = new HashMap<>(48);
        Integer dayNumber = BigDecimal.ONE.intValue();
        for (Integer day : daySortList) {
            dayNumberMap.put(day, dayNumber);
            dayNumber = dayNumber + BigDecimal.ONE.intValue();
        }
        return dayNumberMap;
    }

    /**
     * 构建排产周期的日顺序信息
     *
     * @param startDate 周期起始日
     * @param endDate   周期结束日
     * @return
     */
    private static List<Integer> getCycleDayList(Date startDate, Date endDate) {
        List<Integer> dayList = new ArrayList<>();
        Integer startDay = DateUtils.getDaysByMonth(startDate);
        Integer endDay = DateUtils.getMaxDaysByMonth(startDate);
        for (Integer day = startDay; day <= endDay; day++) {
            dayList.add(day);
        }
        Integer cycleEndDate = DateUtils.getDaysByMonth(endDate);
        for (Integer day = BigDecimal.ONE.intValue(); day <= cycleEndDate; day++) {
            dayList.add(day);
        }
        return dayList;
    }


    private ProductionCycleUtils() {

    }
}
