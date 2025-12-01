package com.zlt.aps.factory.utils;

import com.zlt.aps.factory.domain.vo.ProductionCalendarVO;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalVersionInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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
     * 20250519 ZLT 非自然月方式
     * 根据工厂月份停开工日历及月份最大天数
     * 计算获取停车列表
     *
     * @param productionContext      排产上下文
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDaysByNoNaturalMonth(ProductionContext productionContext, List<ProductionCalendarVO> productionCalendarList) {
        if (CollectionUtils.isEmpty(productionCalendarList)) {
            return Collections.emptySet();
        }
        ZoneId zoneId = ZoneId.systemDefault();
        Set<Integer> stopDays = productionCalendarList.stream().flatMap(productionCalendar -> {
            LocalDate beginDate = productionCalendar.getBeginDate().toInstant().atZone(zoneId).toLocalDate();
            LocalDate endDate = productionCalendar.getEndDate().toInstant().atZone(zoneId).toLocalDate();
            Map<String, Integer> daysMap = calculateDaysByMonth(productionContext, beginDate, endDate);
            Integer beginDay = daysMap.get(START_DAY);
            Integer endDay = daysMap.get(END_DAY);
            Set<Integer> holidays = new HashSet<>();
            for (int day = beginDay; day <= endDay; day++) {
                holidays.add(day);
            }
            return holidays.stream();
        }).collect(Collectors.toSet());
        return stopDays;
    }

    /**
     * 自然月方式
     * 根据工厂月份停开工日历及月份最大天数
     * 计算获取停车列表
     *
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDays(List<ProductionCalendarVO> productionCalendarList) {
        if (CollectionUtils.isEmpty(productionCalendarList)) {
            return Collections.emptySet();
        }
        ZoneId zoneId = ZoneId.systemDefault();
        Set<Integer> stopDays = productionCalendarList.stream().flatMap(productionCalendar -> {
            LocalDate beginDate = productionCalendar.getBeginDate().toInstant().atZone(zoneId).toLocalDate();
            LocalDate endDate = productionCalendar.getEndDate().toInstant().atZone(zoneId).toLocalDate();
            Integer beginDay = beginDate.getDayOfMonth();
            Integer endDay = endDate.getDayOfMonth();
            Set<Integer> holidays = new HashSet<>();
            for (int day = beginDay; day <= endDay; day++) {
                holidays.add(day);
            }
            return holidays.stream();
        }).collect(Collectors.toSet());
        return stopDays;
    }

    /**
     * 非自然月方式
     * 根据工厂排产月份停开工日历
     * 计算获取停车列表
     *
     * @param productionCalendarList 停开工日历
     * @return
     */
    public static Set<Integer> calculateStopDays(List<ProductionCalendarVO> productionCalendarList, FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        if (CollectionUtils.isEmpty(productionCalendarList)) {
            return Collections.emptySet();
        }
        Integer productionYear = finalVersion.getYear();
        Integer productionMonth = finalVersion.getMonth();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate localStartDate = finalVersion.getProductionStartDate().toInstant().atZone(zoneId).toLocalDate();
        LocalDate localEndDate = finalVersion.getProductionEndDate().toInstant().atZone(zoneId).toLocalDate();
        Integer startDay = localEndDate.getDayOfMonth() + BigDecimal.ONE.intValue();
        //前一个月的最大天数
        Integer previousMonthDays = localStartDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        //下个月起始天数值
        Integer nextMonthStartDays = previousMonthDays - startDay + BigDecimal.ONE.intValue();
        Set<Integer> stopDays = productionCalendarList.stream().flatMap(productionCalendar -> {
            LocalDate beginDate = productionCalendar.getBeginDate().toInstant().atZone(zoneId).toLocalDate();
            Integer beginDay = beginDate.getDayOfMonth();
            if (DateUtils.isProductionMonth(productionYear, productionMonth, beginDate)) {
                beginDay = nextMonthStartDays + beginDay;
            } else {
                beginDay = beginDay - startDay + BigDecimal.ONE.intValue();
            }
            LocalDate endDate = productionCalendar.getEndDate().toInstant().atZone(zoneId).toLocalDate();
            Integer endDay = endDate.getDayOfMonth();
            if (DateUtils.isProductionMonth(productionYear, productionMonth, endDate)) {
                endDay = nextMonthStartDays + endDay;
            } else {
                endDay = endDay - startDay + BigDecimal.ONE.intValue();
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
     * 20250519 ZLT 根据日期，计算其在排产月份中的天数
     * 1、自然月，则直接为其日期在月份天数
     * 2 非自然月，重新计算天数值
     * 2.1、月份与排产月份一致，则 月份天数 = 原月份天数 + 上月最大天数 - 起始天数 + 1
     * 2.2、月份与排产月份不一致，则月份天数 = 上月最大天数 - 起始天数 + 1
     *
     * @param context   排产上下文
     * @param beginDate 开始日
     * @param endDate   结束日
     * @return
     */
    public static Map<String, Integer> calculateDaysByMonth(ProductionContext context, LocalDate beginDate, LocalDate endDate) {
        Map<String, Integer> daysMap = new HashMap<>();
        Integer beginDay = beginDate.getDayOfMonth();
        Integer endDay = endDate.getDayOfMonth();
        daysMap.put(DateUtils.START_DAY, beginDay);
        daysMap.put(DateUtils.END_DAY, endDay);
        //自然月排产
        if (context.isNaturalMonth()) {
            return daysMap;
        }
        //非自然月
        Integer startDay = context.getProductionParam().getMonthCycleStartDay();
        //前一个月的最大天数
        Integer previousMonthDays = context.getPreviousMonth().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
        //下个月起始天数值
        Integer nextMonthStartDays = previousMonthDays - startDay + BigDecimal.ONE.intValue();
        if (DateUtils.isProductionMonth(context, beginDate)) {
            beginDay = nextMonthStartDays + beginDay;
        } else {
            beginDay = beginDay - startDay + BigDecimal.ONE.intValue();
        }
        if (DateUtils.isProductionMonth(context, endDate)) {
            endDay = nextMonthStartDays + endDay;
        } else {
            endDay = endDay - startDay + BigDecimal.ONE.intValue();
        }
        daysMap.put(DateUtils.START_DAY, beginDay);
        daysMap.put(DateUtils.END_DAY, endDay);
        return daysMap;
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
