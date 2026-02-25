package com.zlt.aps.mp.common.utils;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;

import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

/**
 * @author Yelq
 */
public class GroupedMapWithOrder {

  /**
   * 方法1：保持原始列表排序，使用LinkedHashMap
   */
  public static Map<String, List<FactoryMonthPlanMouldDayResult>> groupWithOrder(
      List<FactoryMonthPlanMouldDayResult> notFinalMouldDayResultList) {

    // 1. 先按 YearMonth 排序
    notFinalMouldDayResultList.sort(
        Comparator.comparing(FactoryMonthPlanMouldDayResult::getYearMonth)
    );

    // 2. 使用LinkedHashMap保持插入顺序
    // 关键：使用LinkedHashMap保持顺序

    return notFinalMouldDayResultList.stream()
        .collect(Collectors.groupingBy(
            FactoryMonthPlanMouldDayResult::getExportGroupKey,
            // 关键：使用LinkedHashMap保持顺序
            LinkedHashMap::new,
            Collectors.toList()
        ));
  }
}
