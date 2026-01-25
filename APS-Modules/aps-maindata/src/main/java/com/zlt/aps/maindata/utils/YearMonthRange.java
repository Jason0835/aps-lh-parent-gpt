package com.zlt.aps.maindata.utils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.YearMonth;

/**
 * 扩展的时间范围对象
 * @author Administrator
 */
@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class YearMonthRange {

  // 当前年月（通常是上个月）
  private final YearMonth currentYearMonth;

  // 最近N个月的起始年月
  private final YearMonth recentStartYearMonth;

  // 是否包含所有历史数据
  private final boolean includeAllHistoricalData;

  // 回溯月份数
  private final int monthsBack;

  // 可选：最早的数据限制
  private final YearMonth earliestYearMonth;

}
