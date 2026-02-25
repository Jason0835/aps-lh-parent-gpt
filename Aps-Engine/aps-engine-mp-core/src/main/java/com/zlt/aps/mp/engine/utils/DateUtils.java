package com.zlt.aps.mp.engine.utils;

import com.zlt.aps.mp.engine.domain.vo.ProductionDayInfoVo;
import com.zlt.aps.mp.engine.scheduling.ProductionContext;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalVersionInfoVo;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 日期工具类
 *
 * @author ZLT
 * @date 20250220
 */
@Slf4j
public class DateUtils {
    /**
     * 开始日标记-startDay
     */
    public static final String START_DAY = "startDay";
    /**
     * 结束日标记-endDay
     */
    public static final String END_DAY = "endDay";

    /**
     * 根据年 月 获取对应的月份的最大天数
     *
     * @param year  年份
     * @param month 月份
     * @return
     */
    public static Integer getDaysByYearMonth(int year, int month) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);
        a.roll(Calendar.DATE, -1);
        int maxDate = a.get(Calendar.DATE);
        return maxDate;
    }

    /**
     * 根据日期，返回该日在当月的那天
     * 值为1~31
     *
     * @param day
     * @return
     */
    public static Integer getDaysByMonth(Date day) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate monthDay = day.toInstant().atZone(zoneId).toLocalDate();
        return monthDay.getDayOfMonth();
    }

    /**
     * 根据某日，获取该日所在月份最大天数
     *
     * @param day
     * @return
     */
    public static Integer getMaxDaysByMonth(Date day) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate monthDay = day.toInstant().atZone(zoneId).toLocalDate();
        YearMonth yearMonth = YearMonth.of(monthDay.getYear(), monthDay.getMonthValue());
        return yearMonth.lengthOfMonth();
    }

    /**
     * 根据年，月，在月的天数，得到日期
     *
     * @param year  年
     * @param month 月
     * @param day   天
     * @return
     */
    public static Date getDate(int year, int month, int day) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate localDate = LocalDate.of(year, month, day);
        Instant instantTime = localDate.atStartOfDay(zoneId).toInstant();
        return new Date(instantTime.toEpochMilli());
    }

    /**
     * 获取两个日期之间相差的天数
     * 如2025-12-01与2025-12-31之间天数为31
     *
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return
     */
    public static Integer getIntervalDays(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return BigDecimal.ZERO.intValue();
        }
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate startLocal = startDate.toInstant().atZone(zoneId).toLocalDate();
        LocalDate endLocal = endDate.toInstant().atZone(zoneId).toLocalDate();
        return Long.valueOf(Math.abs(ChronoUnit.DAYS.between(startLocal, endLocal))).intValue() + BigDecimal.ONE.intValue();
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
     * 根据日期，返回日期在当年的月份值
     * 值为1-12
     *
     * @param day
     * @return
     */
    public static Integer getMonthsByYear(Date day) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate monthDay = day.toInstant().atZone(zoneId).toLocalDate();
        return monthDay.getMonthValue();
    }

    /**
     * 根据日期，返回日期年值
     * 值为2024等
     *
     * @param day
     * @return
     */
    public static Integer getYear(Date day) {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate monthDay = day.toInstant().atZone(zoneId).toLocalDate();
        return monthDay.getYear();
    }

    /**
     * 获取当前日期的下一个月
     *
     * @return
     */
    public static LocalDate getNextMonth() {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        // 获取当前年当前月的下一个月
        LocalDate nextMonth = currentDate.plusMonths(1);
        return nextMonth;
    }

    /**
     * 自然月方式
     * 根据工厂月份停开工日历及月份最大天数
     * 计算获取停车列表
     *
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDays(List<ProductionDayInfoVo> productionCalendarList) {
        return Collections.emptySet();
    }

    /**
     * 非自然月方式
     * 根据工厂排产月份停开工日历
     * 计算获取停车列表
     *
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDays(List<ProductionDayInfoVo> productionCalendarList, FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        return Collections.emptySet();
    }

    /**
     * 判断日期是否与排产月一直
     *
     * @param productionContext 排产上下文
     * @param date              日期
     * @return true 表示一直， false表示不一致
     */
    private static boolean isProductionMonth(ProductionContext productionContext, LocalDate date) {
        Integer year = productionContext.getYear();
        Integer month = productionContext.getMonth();
        Integer dateYear = date.getYear();
        Integer dateMonth = date.getMonthValue();
        return year.equals(dateYear) && month.equals(dateMonth);
    }

    /**
     * 判断日期是否与排产月一致
     *
     * @param productionYear  排产年份
     * @param productionMonth 排产月份
     * @param date            日期
     * @return true 表示一直， false表示不一致
     */
    private static boolean isProductionMonth(Integer productionYear, Integer productionMonth, LocalDate date) {
        Integer dateYear = date.getYear();
        Integer dateMonth = date.getMonthValue();
        return productionYear.equals(dateYear) && productionMonth.equals(dateMonth);
    }

}
