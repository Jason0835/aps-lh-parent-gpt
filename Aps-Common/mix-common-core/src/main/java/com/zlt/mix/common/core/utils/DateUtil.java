package com.zlt.mix.common.core.utils;

import com.ruoyi.common.utils.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Gim
 */
public class DateUtil {

    /*
     * private static final SimpleDateFormat DEFAULT = new
     * SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); private static final
     * SimpleDateFormat ONLY_DATE = new SimpleDateFormat("yyyy-MM-dd"); private
     * static final SimpleDateFormat ONLY_DATE_INT = new
     * SimpleDateFormat("yyyyMMdd");
     */

    public static String formatDatetime(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public static String formatDatetimeMinute(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    public static String formatDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    public static String formatDateYmd(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyyMMdd").format(date);
    }

    public static String formatMonth(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM").format(date);
    }

    public static String formatMDDate(Date date) {
        return date == null ? "" : new SimpleDateFormat("MM-dd").format(date);
    }

    /**
     * @return 当前时间
     */
    public static Date now() {
        return new Date();
    }

    /**
     * @return 当前日期
     */
    public static String nowDate() {
        SimpleDateFormat ONLY_DATE = new SimpleDateFormat("yyyy-MM-dd");
        return ONLY_DATE.format(new Date());
    }

    /**
     *
     * @return 当前年份
     */
    public static Integer nowYear() {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 注意：获取的周是跨7天并且是以星期一为一周的第一天
     * @return 当前年份所在的周
     */
    public static Integer nowWeekOfYear() {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(7);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 返回当天零点的时间
     *
     * @param date 日期
     * @return 当天零点
     */
    public static Date thatDay(Date date) {
        if (date == null)
            return null;

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 返回次日零点的时间
     *
     * @param date 日期
     * @return 当天零点
     */
    public static Date nextDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);

        return thatDay(cal.getTime());
    }

    /**
     * 返回指定的日期
     *
     * @param year  年
     * @param month 月，从1开始
     * @param day   日
     * @return 日期
     */
    public static Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, (month - 1));
        cal.set(Calendar.DATE, day);

        return thatDay(cal.getTime());
    }

    /**
     * @return 10年后的时间
     */
    public static Date tenYearsLater() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 10);
        return cal.getTime();
    }

    public static Date after1Hour() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        return cal.getTime();
    }

    public static Date after24Hour() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        return cal.getTime();
    }

    public static Date after1Week() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 7);
        return cal.getTime();
    }

    /**
     * 几分钟后
     * @param minutes
     * @return
     */
    public static Date minuteLater(Integer minutes) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.add(Calendar.MINUTE, minutes);

        return calendar.getTime();
    }

    /**
     * 几分钟前
     * @param minutes
     * @return
     */
    public static Date minuteBefore(Integer minutes) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.add(Calendar.MINUTE, -minutes);

        return calendar.getTime();
    }

    /**
     * 几秒钟后
     * @param seconds
     * @return
     */
    public static Date secondLater(Integer seconds) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.add(Calendar.SECOND, seconds);

        return calendar.getTime();
    }

    /**
     * 几小时后
     * @param hours
     * @return
     */
    public static Date hourLater(Integer hours) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.add(Calendar.HOUR, hours);

        return calendar.getTime();
    }

    public static Date from(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }

        try {
            if (source.length() == "yyyy-MM".length()) {
                return new SimpleDateFormat("yyyy-MM").parse(source);
            }
            String[] timeParts = source.split(":");
            if (timeParts.length == 1) {
                return new SimpleDateFormat("yyyy-MM-dd").parse(source);
            } else if (timeParts.length == 2) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(source);
            } else if (timeParts.length == 3) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(source);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    /*
     * @param dt
     *
     * @return 当前日期是星期几
     */
    public static String getWeekOfDate(Date dt) {
        String[] weekDays = {"7", "1", "2", "3", "4", "5", "6"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return weekDays[w];
    }

    public static Integer todayInt() {
        SimpleDateFormat ONLY_DATE_INT = new SimpleDateFormat("yyyyMMdd");
        String todayText = ONLY_DATE_INT.format(now());
        Integer today = new Integer(todayText);
        return today;
    }

    /**
     * @param date10 : “yyyy-MM-dd”格式的日期字符串
     * @return 值为yyyyMMdd的整型代表日期，若是格式错误返回null
     */
    public static Integer parseDateInt(String date10) {
        Integer result = null;

        try {
            SimpleDateFormat DEFAULT = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat DEFAULT_INT = new SimpleDateFormat("yyyyMMdd");

            Date date = DEFAULT.parse(date10);
            result = Integer.parseInt(DEFAULT_INT.format(date));
        } catch (Exception ignore) {
        }

        return result;
    }

    /**
     * @param date8 : “yyyyMMdd”格式的日期字符串
     * @return 值为yyyy-MM-dd的字符串代表日期，若是格式错误返回空字符串
     */
    public static String formatDate(Integer date8) {
        String result = "";

        try {
            String dateTime = date8.toString();
            result = dateTime.substring(0, 4) + "-" + dateTime.substring(4, 6) + "-" + dateTime.substring(6);
        } catch (Exception ignore) {
        }

        return result;
    }


    public static int getDateInt(Date date) {
        if (date == null) {
            return getDateInt(new Date());
        }

        Integer result = 0;
        try {
            SimpleDateFormat DEFAULT_INT = new SimpleDateFormat("yyyyMMdd");
            result = Integer.parseInt(DEFAULT_INT.format(date));
        } catch (Exception ignore) {
        }

        return result;
    }

    public static Date from(Integer date8) {
        if (null == date8) {
            return now();
        }

        SimpleDateFormat DEFAULT_INT = new SimpleDateFormat("yyyyMMdd");

        Date date = now();
        try {
            date = DEFAULT_INT.parse(date8.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    public static long getTenSecondsSpanOneDay(Date date) {
        if (date == null)
            return 0L;

        long timestamp = date.getTime();

        Date today = thatDay(date);
        long timestampToday = today.getTime();

        long result = (timestamp - timestampToday) / 10000L;
        return result;
    }

    public static Date getEndDate(Date endDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }


    /**
     * @return 本周一的日期
     */
    public static Integer firstDateOfThisWeek() {
        return firstDateOfThisWeek(now());
    }


    /**
     * @return 下周一的日期
     */
    public static Integer firstDateOfNextWeek() {
        return firstDateOfThisWeek(after1Week());
    }


    /**
     * @param date 日期
     * @return 指定日期所在周的周一
     */
    public static Integer firstDateOfThisWeek(Date date) {
        Calendar monday = Calendar.getInstance();
        monday.setTime(date);

        int offset = 0;
        switch (monday.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                offset = 0;
                break;
            case Calendar.TUESDAY:
                offset = -1;
                break;
            case Calendar.WEDNESDAY:
                offset = -2;
                break;
            case Calendar.THURSDAY:
                offset = -3;
                break;
            case Calendar.FRIDAY:
                offset = -4;
                break;
            case Calendar.SATURDAY:
                offset = -5;
                break;
            case Calendar.SUNDAY:
                offset = -6;
                break;
        }
        monday.add(Calendar.DATE, offset);

        Integer result = DateUtil.getDateInt(monday.getTime());
        return result;
    }


    /**
     * @return 本月第一天的日期
     */
    public static Integer firstDateOfThisMonth() {
        return firstDateOfThisMonth(DateUtil.now());
    }

    /**
     * @return 下一个月第一天的日期
     */
    public static Integer firstDateOfNextMonth() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        c.set(Calendar.DATE, 1);
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DATE);
    }

    /**
     * 获取指定日期这个月的第一天
     * @param dateStr
     * @param format
     * @return
     */
    public static String getFirstDayOfThisMonth(String dateStr,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_MONTH,1);
            return sdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 获取指定日期这个月的第一天
     * @param date
     * @param format
     * @return
     */
    public static String getFirstDayOfThisMonth(Date date,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH,1);
        return sdf.format(calendar.getTime());
    }

    /**
     * 获取指定日期下个月的第一天
     * @param dateStr
     * @param format
     * @return
     */
    public static String getFirstDayOfNextMonth(String dateStr,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_MONTH,1);
            calendar.add(Calendar.MONTH, 1);
            return sdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取指定日期下个月的第一天
     * @param date
     * @param format
     * @return
     */
    public static String getFirstDayOfNextMonth(Date date,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH,1);
        calendar.add(Calendar.MONTH, 1);
        return sdf.format(calendar.getTime());
    }

    /**
     * @param date 日期
     * @return 指定日期所在月第一天的日期
     */
    public static Integer firstDateOfThisMonth(Date date) {
        Calendar monday = Calendar.getInstance();
        monday.setTime(date);

        int year = monday.get(Calendar.YEAR);
        int month = monday.get(Calendar.MONTH) + 1;

        Integer result = (year * 100 + month) * 100 + 1;
        return result;
    }

    /**
     * @param date 日期
     * @return 指定日期7天后的日期
     */
    public static Date getAfter1Week(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 7);
        return cal.getTime();
    }


    /**
     * @param date 日期
     * @return 指定日期的下个月第一天的日期
     */
    public static Date getAfter1Month(Date date) {
        Calendar monday = Calendar.getInstance();
        monday.setTime(date);
        monday.add(Calendar.MONTH, 1);
        monday.set(Calendar.DAY_OF_MONTH, 1);

        return monday.getTime();
    }

    /**
     * @param date 日期
     * @return 指定日期的上个月第一天的日期
     */
    public static Date getLast1Month(Date date) {
        Calendar monday = Calendar.getInstance();
        monday.setTime(date);
        monday.add(Calendar.MONTH, -1);
        monday.set(Calendar.DAY_OF_MONTH, 1);

        return monday.getTime();
    }
    /**
     * @return 指定月份的上个月
     */
    public static String getLast1MonthString(String monthDate) {
        return formatMonth(getLast1Month(from(monthDate)));

    }

    /**
     * 指定年份的周数
     * @param year 年份
     * @return 周数
     */
    public static int getYearWeekNumber(int year) {
        Date date = getYearWeekFirstDate(year, 53);
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.setTime(date);

        return calendar.get(Calendar.YEAR) == year ? 53 : 52;
    }

    /**
     * 指定年份的周的第一天
     * @param year 年份
     * @param weekOfYear 从1开始算周
     * @return 第一天
     */
    public static Date getYearWeekFirstDate(int year, int weekOfYear) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(7);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        return calendar.getTime();
    }

    /**
     * 指定年份的周的最后一天
     * @param year 年份
     * @param weekOfYear 从1开始算周
     * @return 最后一天
     */
    public static Date getYearWeekEndDate(int year, int weekOfYear) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(7);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.WEEK_OF_YEAR, weekOfYear);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        return calendar.getTime();
    }

    /**
     * 判断单双周：0-双周，1-单周
     * @return
     */
    public static int onceCycleOrBiweekly(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.SIMPLIFIED_CHINESE);
        calendar.setTime(date);

        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        return week % 2;
    }

    public static List<Integer> parseTimeSpan(Date dateLarger, Date dateSmall) {
        ArrayList<Integer> ret = new ArrayList<>();
        long timespan = Math.abs(dateLarger.getTime() - dateSmall.getTime());
        TimeUnit[] timeunits = {TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS};
        for (TimeUnit tu : timeunits) {
            long num = 0;
            num = tu.convert(timespan, TimeUnit.MILLISECONDS);
            ret.add(Integer.valueOf((int) num));
            timespan -= tu.toMillis(num);
        }
        Collections.reverse(ret);
        return ret;
    }

    /**
      * 根据传入的日期获取实际对应的工作日期
      * @ClassName DateUtil
      * @Description TODO
      * @Author Joran.Zhang
      * @Date 2021/8/7 13:40
      * @Version 1.0
    **/
    public static String  getLLWorkDate(Date realDate){
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(realDate);
        calendar.add(Calendar.HOUR_OF_DAY,8);
        return formatDate(calendar.getTime());
    }

    /*** 获取日期年份*/
    public static int getYear(Date date){
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);

    }

    /*** 获取日期月份*/
    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        return (calendar.get(Calendar.MONTH) + 1);
    }

    /*** 获取日期号*/
    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 转换英文日期 输出例：Jan 02
     * @param date 格式年月 例如一月二号，则输入 0102
     * @return
     */
    public static String getEngMonthDay(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        try {
            Date parse = sdf.parse(date);
            sdf = new SimpleDateFormat("MMM dd",Locale.ENGLISH);
            return sdf.format(parse);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
