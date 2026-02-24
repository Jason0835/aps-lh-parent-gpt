package com.zlt.aps.mp.engine.utils;


import com.zlt.aps.mp.engine.domain.vo.MonthPlanProductMouldInfoVo;
import com.zlt.aps.mp.engine.scheduling.TbrProductionContext;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SKU与模具关系去重
 * @author Yelq
 */
public class MouldRelationDeduplicator {

  /**
   * 方案1：使用Collectors.toMap去重并优先选取productMouldInfoList中的元素
   * 优点：代码简洁，性能优秀，单次遍历完成
   */
  public static List<MonthPlanProductMouldInfoVo> deduplicateAndMerge(
      List<MonthPlanProductMouldInfoVo> productMouldInfoList,
      List<MonthPlanProductMouldInfoVo> mouldDeliveryList, TbrProductionContext productionContext) {

    if (isEmpty(productMouldInfoList) && isEmpty(mouldDeliveryList)) {
      return Collections.emptyList();
    }
    List<MonthPlanProductMouldInfoVo> processMouldDeliveryList = MouldDateProcessor.processBoardingDateBatch(mouldDeliveryList,productionContext.getProductionStartDate(),productionContext.getProductionEndDate());
    // 使用LinkedHashMap保持插入顺序
    Map<String, MonthPlanProductMouldInfoVo> deduplicatedMap = Stream.concat(
            safeStream(productMouldInfoList),
            safeStream(processMouldDeliveryList)
        )
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            MonthPlanProductMouldInfoVo::getGroupKey,
            Function.identity(),
            (v1, v2) -> isFromFirstList(v1, productMouldInfoList) ? v1 : v2,
            LinkedHashMap::new
        ));

    return new ArrayList<>(deduplicatedMap.values());
  }

  private static <T> Stream<T> safeStream(List<T> list) {
    return CollectionUtils.isEmpty(list) ? Stream.empty() : list.stream();
  }

  private static boolean isEmpty(List<?> list) {
    return CollectionUtils.isEmpty(list);
  }

  private static boolean isFromFirstList(MonthPlanProductMouldInfoVo vo,
                                  List<MonthPlanProductMouldInfoVo> firstList) {
    // 通过对象引用判断是否来自第一个列表
    return firstList != null && firstList.contains(vo);
  }
}
