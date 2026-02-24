package com.zlt.aps.mp.engine.handler;

import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 对使用preDay1、preDay2、preDay3、day1、day2、day3.....等属性，
 * 进行汇总属性值处理
 *
 * @author ZLT
 * @date 20260101
 */
@Slf4j
public class DayProductionHandler {

    /**
     * 汇总需要进行天的数量汇总的统计，对对象按周期
     * 对使用preDay1、preDay2、preDay3、day1、day2、day3.....等属性，进行汇总属性值处理
     *
     * @param dayQtyInfo
     * @return
     */
    public static Integer summaryDayQty(BaseEntity dayQtyInfo, Integer[] dayList) {
        Integer totalValue = BigDecimal.ZERO.intValue();
        if (null == dayQtyInfo || null == dayList || dayList.length <= BigDecimal.ZERO.intValue()) {
            return totalValue;
        }
        for (Integer day : dayList) {
            String fieldName = "";
            if (day > BigDecimal.ZERO.intValue()) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Integer dayValue;
            Object value = dayQtyInfo.getFieldValueByFieldName(fieldName);
            if (null == value) {
                dayValue = BigDecimal.ZERO.intValue();
            } else {
                dayValue = Integer.valueOf("" + value);
            }
            totalValue = totalValue + dayValue;
        }
        return totalValue;
    }

    /**
     * 汇总需要进行天的数量汇总的统计，对对象按周期
     * 对使用preDay1、preDay2、preDay3、day1、day2、day3.....等属性，进行属性值
     * 将source的值增加到target中
     *
     * @param target  目标数据
     * @param source  源数据
     * @param dayList 周期属性
     * @return
     */
    public static void addDayQty(BaseEntity target, BaseEntity source, Integer[] dayList) {
        if (null == target || null == source || null == dayList || dayList.length <= BigDecimal.ZERO.intValue()) {
            return;
        }
        //日期合计汇总
        String fieldName;
        for (Integer day : dayList) {
            if (day > BigDecimal.ZERO.intValue()) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Object value = source.getFieldValueByFieldName(fieldName);
            if (null == value) {
                continue;
            }
            Integer productionValue = Integer.valueOf("" + value);
            Object previousValue = target.getFieldValueByFieldName(fieldName);
            Integer sumValue;
            if (null == previousValue) {
                sumValue = BigDecimal.ZERO.intValue();
            } else {
                sumValue = Integer.valueOf("" + previousValue);
            }
            sumValue = sumValue + productionValue;
            target.setFieldValueByFieldName(fieldName, sumValue);
        }
    }

    /**
     * 提取对应的日排产量
     *
     * @param source  元数据对象
     * @param dayList 周期
     * @return
     */
    public static Map<Integer, Integer> getDayQty(BaseEntity source, Integer[] dayList) {
        if (null == source || null == dayList || dayList.length <= BigDecimal.ZERO.intValue()) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> dayQtyMap = new HashMap<>();
        //日期量
        String fieldName;
        for (Integer day : dayList) {
            if (day > BigDecimal.ZERO.intValue()) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(day);
            Object value = source.getFieldValueByFieldName(fieldName);
            if (null == value) {
                continue;
            }
            Integer productionValue = Integer.valueOf("" + value);
            if (productionValue <= BigDecimal.ZERO.intValue()) {
                continue;
            }
            dayQtyMap.put(day, productionValue);
        }
        return dayQtyMap;
    }
}
