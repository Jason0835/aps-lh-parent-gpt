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

    return new MonthRangeResult(
        tMonth,
        tPlus1Month,
        tPlus2Month
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

    public MonthRangeResult(
        YearMonth tMonth,
        YearMonth tPlus1Month,
        YearMonth tPlus2Month) {
      this.tMonth = tMonth;
      this.tPlus1Month = tPlus1Month;
      this.tPlus2Month = tPlus2Month;
    }
  }

}
