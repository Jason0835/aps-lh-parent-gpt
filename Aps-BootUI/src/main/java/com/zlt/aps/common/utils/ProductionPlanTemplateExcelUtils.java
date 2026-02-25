package com.zlt.aps.common.utils;

import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 排产计划模板工具类
 *
 * @author ZLT
 * @date 20250708
 */
@Slf4j
public class ProductionPlanTemplateExcelUtils {

    /**
     * 构建周期日顺序excel头列表信息
     * 调整列表的日期展示
     *
     * @param version
     * @return
     */
    public static List<Integer> getCycleDayList(MpFactoryProductionVersion version) {
        Date startDate = version.getProductionStartDate();
        Date endDate = version.getProductionEndDate();
        List<Integer> dayList = new ArrayList<>();
        Integer startDay = getDaysByMonth(startDate);
        Integer endDay = getMaxDaysByMonth(startDate);
        for (Integer day = startDay; day <= endDay; day++) {
            dayList.add(day);
        }
        Integer cycleEndDate = getDaysByMonth(endDate);
        for (Integer day = BigDecimal.ONE.intValue(); day <= cycleEndDate; day++) {
            dayList.add(day);
        }
        return dayList;
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

    private ProductionPlanTemplateExcelUtils() {

    }
}
