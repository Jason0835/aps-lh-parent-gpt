package com.zlt.aps.cxlh.cx.api.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 月计划实体
 */
@Data
public class MonthPlan {

    /** ID */
    private Long id;

    /** 计划号 */
    private String productionNo;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 计划年月 */
    private String planYearMonth;

    /** 日计划量数组(1-31日) */
    private BigDecimal day1;
    private BigDecimal day2;
    private BigDecimal day3;
    private BigDecimal day4;
    private BigDecimal day5;
    private BigDecimal day6;
    private BigDecimal day7;
    private BigDecimal day8;
    private BigDecimal day9;
    private BigDecimal day10;
    private BigDecimal day11;
    private BigDecimal day12;
    private BigDecimal day13;
    private BigDecimal day14;
    private BigDecimal day15;
    private BigDecimal day16;
    private BigDecimal day17;
    private BigDecimal day18;
    private BigDecimal day19;
    private BigDecimal day20;
    private BigDecimal day21;
    private BigDecimal day22;
    private BigDecimal day23;
    private BigDecimal day24;
    private BigDecimal day25;
    private BigDecimal day26;
    private BigDecimal day27;
    private BigDecimal day28;
    private BigDecimal day29;
    private BigDecimal day30;
    private BigDecimal day31;

    /**
     * 获取指定日期的计划量
     */
    public BigDecimal getDayQty(Date date) {
        int day = Integer.parseInt(new java.text.SimpleDateFormat("d").format(date));
        switch (day) {
            case 1: return day1;
            case 2: return day2;
            case 3: return day3;
            case 4: return day4;
            case 5: return day5;
            case 6: return day6;
            case 7: return day7;
            case 8: return day8;
            case 9: return day9;
            case 10: return day10;
            case 11: return day11;
            case 12: return day12;
            case 13: return day13;
            case 14: return day14;
            case 15: return day15;
            case 16: return day16;
            case 17: return day17;
            case 18: return day18;
            case 19: return day19;
            case 20: return day20;
            case 21: return day21;
            case 22: return day22;
            case 23: return day23;
            case 24: return day24;
            case 25: return day25;
            case 26: return day26;
            case 27: return day27;
            case 28: return day28;
            case 29: return day29;
            case 30: return day30;
            case 31: return day31;
            default: return BigDecimal.ZERO;
        }
    }
}
