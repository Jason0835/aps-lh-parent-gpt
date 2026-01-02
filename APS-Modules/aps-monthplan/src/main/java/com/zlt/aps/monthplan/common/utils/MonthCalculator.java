package com.zlt.aps.monthplan.common.utils;

import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 月份计算器 - 计算T月、T+1月、T+2月
 * 规则：
 * 1. T月 = 当前操作日所在年月(当月) + 1个月
 * 2. T+1月 = 在T月的基础上 + 1个月
 * 3. T+2月 = 在T月的基础上 + 2个月
 * <p>
 * 示例：
 * 操作日期：2025-11-07 → 当月：2025-11
 * T月：2025-12
 * T+1月：2026-01
 * T+2月：2026-02
 *
 * @author Yelq
 */
public class MonthCalculator {

  /**
   * 计算月份范围
   */
  public static MonthRangeResult calculateMonthRanges() {
    // 获取操作日所在月份
    YearMonth currentMonth = YearMonth.from(LocalDate.now());

    // T月 = 当月 + 1个月
    YearMonth tMonth = currentMonth.plusMonths(1);

    // T+1月 = T月 + 1个月
    YearMonth tPlus1Month = tMonth.plusMonths(1);

    // T+2月 = T月 + 2个月
    YearMonth tPlus2Month = tMonth.plusMonths(2);

    YearMonth tPlus3Month = tMonth.plusMonths(3);

    YearMonth tPlus4Month = tMonth.plusMonths(4);

    YearMonth tPlus5Month = tMonth.plusMonths(5);

    YearMonth tPlus6Month = tMonth.plusMonths(6);

    YearMonth tPlus7Month = tMonth.plusMonths(7);

    YearMonth tPlus8Month = tMonth.plusMonths(8);

    YearMonth tPlus9Month = tMonth.plusMonths(9);

    YearMonth tPlus10Month = tMonth.plusMonths(10);

    YearMonth tPlus11Month = tMonth.plusMonths(11);

    YearMonth tPlus12Month = tMonth.plusMonths(12);

    YearMonth tPlus13Month = tMonth.plusMonths(13);

    YearMonth tPlus14Month = tMonth.plusMonths(14);

    YearMonth tPlus15Month = tMonth.plusMonths(15);

    YearMonth tPlus16Month = tMonth.plusMonths(16);

    YearMonth tPlus17Month = tMonth.plusMonths(17);

    YearMonth tPlus18Month = tMonth.plusMonths(18);

    YearMonth tPlus19Month = tMonth.plusMonths(19);

    YearMonth tPlus20Month = tMonth.plusMonths(20);

    YearMonth tPlus21Month = tMonth.plusMonths(21);

    YearMonth tPlus22Month = tMonth.plusMonths(22);

    YearMonth tPlus23Month = tMonth.plusMonths(23);

    return new MonthRangeResult(
        tMonth,
        tPlus1Month,
        tPlus2Month,
        tPlus3Month,
        tPlus4Month,
        tPlus5Month,
        tPlus6Month,
        tPlus7Month,
        tPlus8Month,
        tPlus9Month,
        tPlus10Month,
        tPlus11Month,
        tPlus12Month,
        tPlus13Month,
        tPlus14Month,
        tPlus15Month,
        tPlus16Month,
        tPlus17Month,
        tPlus18Month,
        tPlus19Month,
        tPlus20Month,
        tPlus21Month,
        tPlus22Month,
        tPlus23Month
    );
  }

  /**
   * 结果封装类
   */
  @Getter
  public static class MonthRangeResult {
    // Getters
    private final YearMonth tMonth;
    private final YearMonth tPlus1Month;
    private final YearMonth tPlus2Month;
    private final YearMonth tPlus3Month;
    private final YearMonth tPlus4Month;
    private final YearMonth tPlus5Month;
    private final YearMonth tPlus6Month;
    private final YearMonth tPlus7Month;
    private final YearMonth tPlus8Month;
    private final YearMonth tPlus9Month;
    private final YearMonth tPlus10Month;
    private final YearMonth tPlus11Month;
    private final YearMonth tPlus12Month;
    private final YearMonth tPlus13Month;
    private final YearMonth tPlus14Month;
    private final YearMonth tPlus15Month;
    private final YearMonth tPlus16Month;
    private final YearMonth tPlus17Month;
    private final YearMonth tPlus18Month;
    private final YearMonth tPlus19Month;
    private final YearMonth tPlus20Month;
    private final YearMonth tPlus21Month;
    private final YearMonth tPlus22Month;
    private final YearMonth tPlus23Month;


    public MonthRangeResult(YearMonth tMonth, YearMonth tPlus1Month, YearMonth tPlus2Month, YearMonth tPlus3Month, YearMonth tPlus4Month, YearMonth tPlus5Month, YearMonth tPlus6Month, YearMonth tPlus7Month, YearMonth tPlus8Month, YearMonth tPlus9Month, YearMonth tPlus10Month, YearMonth tPlus11Month, YearMonth tPlus12Month, YearMonth tPlus13Month, YearMonth tPlus14Month, YearMonth tPlus15Month, YearMonth tPlus16Month, YearMonth tPlus17Month, YearMonth tPlus18Month, YearMonth tPlus19Month, YearMonth tPlus20Month, YearMonth tPlus21Month, YearMonth tPlus22Month, YearMonth tPlus23Month) {
      this.tMonth = tMonth;
      this.tPlus1Month = tPlus1Month;
      this.tPlus2Month = tPlus2Month;
      this.tPlus3Month = tPlus3Month;
      this.tPlus4Month = tPlus4Month;
      this.tPlus5Month = tPlus5Month;
      this.tPlus6Month = tPlus6Month;
      this.tPlus7Month = tPlus7Month;
      this.tPlus8Month = tPlus8Month;
      this.tPlus9Month = tPlus9Month;
      this.tPlus10Month = tPlus10Month;
      this.tPlus11Month = tPlus11Month;
      this.tPlus12Month = tPlus12Month;
      this.tPlus13Month = tPlus13Month;
      this.tPlus14Month = tPlus14Month;
      this.tPlus15Month = tPlus15Month;
      this.tPlus16Month = tPlus16Month;
      this.tPlus17Month = tPlus17Month;
      this.tPlus18Month = tPlus18Month;
      this.tPlus19Month = tPlus19Month;
      this.tPlus20Month = tPlus20Month;
      this.tPlus21Month = tPlus21Month;
      this.tPlus22Month = tPlus22Month;
      this.tPlus23Month = tPlus23Month;
    }
  }

}
