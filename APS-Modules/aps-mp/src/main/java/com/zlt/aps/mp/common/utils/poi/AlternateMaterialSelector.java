package com.zlt.aps.mp.common.utils.poi;

import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.mp.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 需求计划可替换物料筛选
 * @author Yelq
 */
public class AlternateMaterialSelector {

  /**
   * 设置物料是否可替换标识
   * @param netDemands 净需求明细
   * @param productStockMap 库存数据（key = stockGroupKey）
   */
  public static void setAlternateMaterialFlag(List<DpOrderOffsetDetail> netDemands,
                                        Map<String, List<MdmProductStock>> productStockMap) {
    if (CollectionUtils.isEmpty(netDemands) || CollectionUtils.isEmpty(productStockMap)) {
      return;
    }

    // 1. 预计算：哪些stockGroupKey有正剩余库存
    Set<String> stockKeysWithPositiveLeftover = computeStockKeysWithPositiveLeftover(productStockMap);

    // 2. 预计算：每个替代组（groupKey）是否包含至少一个有库存的stockGroupKey
    Map<String, Boolean> groupHasPositiveStock = computeGroupHasPositiveStock(netDemands, stockKeysWithPositiveLeftover);

    // 3. 遍历每个明细，直接设置标记
    for (DpOrderOffsetDetail detail : netDemands) {
      String stockKey = detail.getStockGroupKey();

      // 情况A：该物料自身有剩余库存 → 不可替换
      if (stockKeysWithPositiveLeftover.contains(stockKey)) {
        detail.setIsAlternateMaterial(YesOrNoEnum.NO.getCode());
        continue;
      }

      // 情况B：自身无库存，且计划产量为0 → 不可替换
      if (detail.getProduceQtyDue() == 0) {
        detail.setIsAlternateMaterial(YesOrNoEnum.NO.getCode());
        continue;
      }

      // 情况C：自身无库存，有生产需求 → 根据替代组内是否有其他物料有库存决定
      boolean hasPositiveInGroup = groupHasPositiveStock.getOrDefault(detail.getGroupKey(), false);
      String result = hasPositiveInGroup ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode();
      detail.setIsAlternateMaterial(result);
    }
  }

  /**
   * 计算所有存在正剩余库存的stockGroupKey
   */
  private static Set<String> computeStockKeysWithPositiveLeftover(Map<String, List<MdmProductStock>> productStockMap) {
    return productStockMap.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .filter(entry -> entry.getValue().stream()
            .filter(Objects::nonNull)
            .mapToInt(MdmProductStock::getLeftOverQty)
            .sum() > 0)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  /**
   * 计算每个替代组（groupKey）是否至少关联一个正库存的stockGroupKey
   */
  private static Map<String, Boolean> computeGroupHasPositiveStock(List<DpOrderOffsetDetail> netDemands,
                                                            Set<String> positiveStockKeys) {
    Map<String, Boolean> groupHasPositiveStock = new HashMap<>();
    for (DpOrderOffsetDetail detail : netDemands) {
      String groupKey = detail.getGroupKey();
      if (positiveStockKeys.contains(detail.getStockGroupKey())) {
        groupHasPositiveStock.put(groupKey, Boolean.TRUE);
      } else {
        // 已经为true则保留，否则可以put false（但默认false不put更省内存）
        groupHasPositiveStock.putIfAbsent(groupKey, Boolean.FALSE);
      }
    }
    return groupHasPositiveStock;
  }
}
