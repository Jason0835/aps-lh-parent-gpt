package com.tlt.aps.utils;

import com.tlt.aps.constant.FactoryConstant;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 年月工具类
 *
 * @author ZLT
 * @date 20250618
 */
public class YearMonthUtils {

    /**
     * 是否采用自然月进行排产
     * 周期起始日在2~28之间则认为为非自然月
     * 否则为自然月
     * true 表示自然月 false表示否自然月
     *
     * @return
     */
    public static boolean isNaturalMonth(Integer cycleStartDay) {
        if (null == cycleStartDay) {
            return true;
        }

        if (cycleStartDay > FactoryConstant.NO_NATURAL_MONTH_MAX_VALUE) {
            return true;
        }
        if (cycleStartDay <= FactoryConstant.MONTH_START_DAY) {
            return true;
        }
        return false;
    }

    /**
     * 获得当前年月的前一个月的起始时间
     *
     * @return
     */
    public static LocalDate getPreviousMonth(Integer year, Integer month) {
        LocalDate currentProductionMonth = LocalDate.of(year, month, FactoryConstant.MONTH_START_DAY);
        LocalDate previousMonth = currentProductionMonth.minusMonths(BigDecimal.ONE.intValue());
        return previousMonth;
    }

    /**
     * 非自然月方式
     * 根据工厂排产月份停开工日历
     * 计算获取停车列表
     *
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDays(List<ProductionCalendarHelper> productionCalendarList, Date cycleStartDate, Date cycleEndDate) {
        if (CollectionUtils.isEmpty(productionCalendarList)) {
            return Collections.emptySet();
        }
        Integer monthMaxDays = getDifferenceDays(cycleStartDate, cycleEndDate);
        Set<Integer> stopDays = productionCalendarList.stream().flatMap(productionCalendar -> {
            Date stopBeginDate = productionCalendar.getBeginDate();
            Date stopEndDate = productionCalendar.getEndDate();
            Integer beginDay = getDifferenceDays(cycleStartDate, stopBeginDate);
            Integer endDay = getDifferenceDays(cycleStartDate, stopEndDate);
            if (endDay > monthMaxDays) {
                endDay = monthMaxDays;
            }
            Set<Integer> holidays = new HashSet<>();
            for (int day = beginDay; day <= endDay; day++) {
                holidays.add(day);
            }
            return holidays.stream();
        }).collect(Collectors.toSet());
        return stopDays;
    }

    /**
     * 得到年、月、日的日期值
     *
     * @param date
     * @return
     */
    public static Date getDate(LocalDate date) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate localDate = LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        Instant instantTime = localDate.atStartOfDay(zoneId).toInstant();
        return new Date(instantTime.toEpochMilli());
    }

    /**
     * 两个日期之前相差的天数 + 1，
     * 主要用以某天在某个周期上的天数
     * <p>
     * 如2025-06-07 与 2025-06-07
     *
     * @param before 起始日
     * @param after  结束日
     * @return
     */
    public static Integer getDifferenceDays(Date before, Date after) {
        if (null == before || null == after) {
            return BigDecimal.ZERO.intValue();
        }
        return Long.valueOf(Math.abs(Duration.between(before.toInstant(), after.toInstant()).toDays())).intValue() + BigDecimal.ONE.intValue();
    }

    private YearMonthUtils() {

    }
}
