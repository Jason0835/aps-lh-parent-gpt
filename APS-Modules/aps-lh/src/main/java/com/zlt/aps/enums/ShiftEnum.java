package com.zlt.aps.enums;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 16799
 */

@Getter
public enum ShiftEnum {

    /**
     * 两班制
     */
    TWO_SHIFT(2, 12, Arrays.asList(1, 2, 4, 5)),

    /**
     * 三班制
     */
    THREE_SHIFT(3, 8, Arrays.asList(1, 2, 3, 4, 5, 6));


    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 班制数
     */
    private final int shiftCount;

    /**
     * 班制时长
     */
    private final int shiftDuration;

    /**
     * 班次开始时间
     */
    private final Date startTime = new Date();

    /**
     * 有效的班制数组
     */
    private final List<Integer> validClasses;

    ShiftEnum(int shiftCount, int shiftDuration, List<Integer> validClasses) {
        this.shiftCount = shiftCount;
        this.shiftDuration = shiftDuration;
        this.validClasses = validClasses;
    }

    /**
     * title：设置班次配置类的开始时间
     * desc ： 用于后续获取每个班次的开始时间/结束时间的依据
     * @param scheduleDate 日期
     * @param cxShiftSystemStartHour  小时
     */
    public void setStartTime(Date scheduleDate, Integer cxShiftSystemStartHour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(scheduleDate);
        calendar.set(Calendar.HOUR_OF_DAY, cxShiftSystemStartHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date shiftStartTime = calendar.getTime();
        this.startTime.setTime(shiftStartTime.getTime());
    }

    /**
     * title:获取今天的班制字段
     * desc : 传入一个后缀就能返回一依据班制的集合，class1后缀 ..  .  最大到class3后缀
     * @param suffix 后缀  [Sort/PlanQty/StartTime/EndTime] 等
     * @return // → [class1PlanQty, class2PlanQty]
     */
    public List<String> getTodayClasses(String prefix,String suffix) {
        return validClasses.stream()
                .filter(c -> c <= shiftCount)
                .map(c -> prefix + c + suffix)
                .collect(Collectors.toList());
    }

    /**
     * title:获取明天的班制字段
     * desc : 传入一个后缀就能返回一依据班制的集合，class4后缀 ..  .  最大到class6后缀
     * @param suffix 后缀  [Sort/PlanQty/StartTime/EndTime] 等
     * @return // → [class4PlanQty, class5PlanQty]
     */
    public List<String> getTomorrowClasses(String prefix,String suffix) {
        return validClasses.stream()
                .filter(c -> c > shiftCount)
                .map(c -> prefix + c + suffix)
                .collect(Collectors.toList());
    }

    /**
     * 根据传入的包含数字的英文串，识别出里面的数字，然后依据班制数推算数字所属于的班次的开始时间和结束时间
     * @param input 包含数字的英文串
     * @return 包含 startTime 和 endTime 的 HashMap，格式为 yyyy-MM-dd HH:mm:ss
     */
    public Map<String, String> getShiftTimeByString(String input) {
        // 使用正则表达式提取数字
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);

        if (!matcher.find()) {
            throw new IllegalArgumentException("输入字符串中未找到数字");
        }

        int classNum = Integer.parseInt(matcher.group());

        for (int i = 0 ; i < validClasses.size() ; i++) {
            if (validClasses.get(i) == classNum){
                classNum = i + 1;
            }
        }

        // 计算班次的开始时间和结束时间
        int startHourOffset = (classNum - 1) * shiftDuration;
        int endHourOffset = classNum * shiftDuration;

        // 创建 Calendar 实例并设置为 startTime
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);

        // 设置开始时间
        calendar.add(Calendar.HOUR_OF_DAY, startHourOffset);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date shiftStartTime = calendar.getTime();

        // 设置结束时间
        calendar.setTime(startTime);
        calendar.add(Calendar.HOUR_OF_DAY, endHourOffset);
        Date shiftEndTime = calendar.getTime();

        // 格式化日期和时间
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 创建返回的 HashMap
        Map<String, String> result = new HashMap<>();
        result.put("startTime", dateTimeFormat.format(shiftStartTime));
        result.put("endTime", dateTimeFormat.format(shiftEndTime));

        return result;
    }

    /**
     * 根据班次类型获取配置
     * @param shiftCount 传入班制数
     * @return 班制配置对象
     */
    public static ShiftEnum of(int shiftCount) {
        return Arrays.stream(values())
                .filter(config -> config.shiftCount == shiftCount)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的班制类型"));
    }


    /**
     * 根据传入时间判断所属班次编号
     * @param time 传入的时间
     * @return 返回所在班次
     */
    public int getShiftNumber(LocalDateTime time) {
        // 将班次开始时间转为 LocalDateTime 对象
        LocalDateTime startDateTime = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // 计算时间差（小时）
        long diffHours = ChronoUnit.HOURS.between(startDateTime, time);

        // 处理负时间差（时间在班次开始时间之前）
        int totalPeriodHours = shiftCount * shiftDuration;
        if (diffHours < 0) {
            diffHours += ((-diffHours / totalPeriodHours) + 1) * totalPeriodHours;
        }

        // 计算周期内的小时偏移量
        long modHours = diffHours % totalPeriodHours;
        int shiftIndex = (int) (modHours / shiftDuration);

        // 计算天数差（基于班次周期）
        long daysDiff = diffHours / totalPeriodHours;

        // 验证天数范围（最多支持2天）
        if (daysDiff < 0 || daysDiff > 1) {
            throw new IllegalArgumentException("时间超出支持的班次范围");
        }

        // 计算在 validClasses 中的位置
        int position = shiftIndex + (int) daysDiff * shiftCount;
        if (position >= validClasses.size()) {
            throw new IllegalArgumentException("时间超出配置的班次范围");
        }

        return validClasses.get(position);
    }


    /**
     * 解析今日末班结束时间
     */
    public LocalDateTime parseToDayLastShiftEndTime() {
        int lastShiftNumber = this.getShiftCount();
        Map<String, String> timeMap = this.getShiftTimeByString(String.valueOf(lastShiftNumber));
        return LocalDateTime.parse(timeMap.get("endTime"), formatter);
    }

    /**
     * 解析今日开始排程时间
     */
    public LocalDateTime parseToDayFirstShiftStartTime() {
        int firstShiftNumber = 1;
        Map<String, String> timeMap = this.getShiftTimeByString(String.valueOf(firstShiftNumber));
        return LocalDateTime.parse(timeMap.get("startTime"), formatter);
    }

    /**
     * 解析明日末班结束时间
     */
    public LocalDateTime parseTomorrowDayLastShiftEndTime() {
        int lastShiftNumber = this.getShiftCount() * 2;
        lastShiftNumber = validClasses.get(lastShiftNumber - 1);
        Map<String, String> timeMap = this.getShiftTimeByString(String.valueOf(lastShiftNumber));
        return LocalDateTime.parse(timeMap.get("endTime"), formatter);
    }




}
