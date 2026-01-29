package com.zlt.aps.monthplan.common.utils;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * 销售订单过滤
 * @author Yelq
 */
import java.util.*;

public class SalesOrderFilterUtils {
  public static List<SalesOrderPool> filterSalesOrdersOptimized(
      List<SalesOrderPool> salesOrders,
      Map<String, MdmMaterialInfo> materialInfoMap) {

    // 防御性编程
    if (salesOrders == null || salesOrders.isEmpty()) {
      return Collections.emptyList();
    }
    if (materialInfoMap == null || materialInfoMap.isEmpty()) {
      // 如果物料映射为空，则所有订单都不符合条件
      return Collections.emptyList();
    }
    // JDK 1.8 使用普通HashSet，而非不可变集合
    Set<String> validMaterialCodes = preprocessMaterialCodes(materialInfoMap.keySet());
    // 预分配结果列表大小（避免动态扩容）
    List<SalesOrderPool> filteredOrders = new ArrayList<>(salesOrders.size());
    for (SalesOrderPool order : salesOrders) {
      if (order == null) {
        continue;
      }
      String materialCode = order.getOriMaterialCode();
      if (materialCode == null || materialCode.trim().isEmpty()) {
        continue;
      }
      String normalizedCode = materialCode.trim();
      if (validMaterialCodes.contains(normalizedCode)) {
        filteredOrders.add(order);
      }
    }
    return filteredOrders;
  }

  /**
   * 预处理物料编码：去重、去除空格
   * JDK 1.8 兼容版本
   */
  private static Set<String> preprocessMaterialCodes(Collection<String> rawMaterialCodes) {
    if (rawMaterialCodes == null || rawMaterialCodes.isEmpty()) {
      return Collections.emptySet();
    }
    // 预分配大小避免扩容
    Set<String> processedCodes = new HashSet<>(rawMaterialCodes.size() * 2);
    for (String code : rawMaterialCodes) {
      if (StringUtils.isNotBlank(code)) {
        String trimmed = code.trim();
        if (!trimmed.isEmpty()) {
          processedCodes.add(trimmed);
        }
      }
    }
    return processedCodes;
  }

}
