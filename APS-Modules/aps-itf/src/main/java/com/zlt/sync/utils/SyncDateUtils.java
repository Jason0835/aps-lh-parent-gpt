package com.zlt.sync.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SyncDateUtils {

    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YYYYMM = "yyyyMM";
    public static final String DATE_FORMAT_YYYY_MM = "yyyy-MM";

    public static final Integer BETWEEN_SECOND = 1;
    public static final Integer BETWEEN_MINUTE = 2;
    public static final Integer BETWEEN_HOUR = 3;
    public static final Integer BETWEEN_DAY = 4;

    public static String getPrevMonth(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1); // 设置为上一个月
        date = calendar.getTime();
        String accDate = dateFormat.format(date);
        return accDate;
    }

    public static String getNextMonth(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1); // 设置为下一个月
        date = calendar.getTime();
        String accDate = dateFormat.format(date);
        return accDate;
    }

    public static String getDateStringApartDays(int days, String dateFormat) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Date date = getDateApartDays(days, dateFormat);
        String accDate = format.format(date);
        return accDate;
    }

    public static Date getDateApartDays(int days, String dateFormat) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date); // 设置为当前时间
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + days); // 设置为上一个月
        date = calendar.getTime();
        return date;
    }

    public static int getWeekOfYear(String today, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = dateFormat.parse(today);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(date);

        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * 两个时间差
     * @param beginDate
     * @param endDate
     * @param type
     * @return
     */
    public static long between(Date beginDate, Date endDate, Integer type) {

        if (BETWEEN_SECOND.equals(type)) {
            return (endDate.getTime() - beginDate.getTime()) / 1000;
        } else if (BETWEEN_MINUTE.equals(type)) {
            return (endDate.getTime() - beginDate.getTime()) / 1000 / 60;
        } else if (BETWEEN_HOUR.equals(type)) {
            return (endDate.getTime() - beginDate.getTime()) / 1000 / 60 / 60;
        } else if (BETWEEN_DAY.equals(type)) {
            return (endDate.getTime() - beginDate.getTime()) / 1000 / 60 / 60 / 24;
        }

        return 0;
    }
}
