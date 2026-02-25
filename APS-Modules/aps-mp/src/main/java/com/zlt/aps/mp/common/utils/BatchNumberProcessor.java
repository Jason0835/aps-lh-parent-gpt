package com.zlt.aps.mp.common.utils;

import com.zlt.aps.mp.api.domain.entity.MpPredictionDetail;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * 获取最新批次号
 * @author Yelq
 */
public class BatchNumberProcessor {

  public static  List<String> getLatestBatchNumbers(List<MpPredictionDetail> result) {
    if (CollectionUtils.isEmpty(result)) {
      return Collections.emptyList();
    }
    // 1. 按批次号分组，并对每个分组按创建时间排序
    Map<String, List<MpPredictionDetail>> groupedMap = result.stream()
        .collect(Collectors.groupingBy(
            MpPredictionDetail::getBatchNumber,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> list.stream()
                    .sorted(Comparator.comparing(MpPredictionDetail::getYear).thenComparing(MpPredictionDetail::getMonth))
                    .collect(Collectors.toList())
            )
        ));

    // 2. 按工厂、年份、月份分组，找出每个组中创建时间最新的批次
    Map<String, BatchGroupKey> groupKeyToLatestBatch = new HashMap<>();
    groupedMap.forEach((batchNumber, details) -> {
      if (!details.isEmpty()) {
        // 取第一个（createTime最早的）记录
        MpPredictionDetail firstDetail = details.get(0);
        // 创建组合键
        String groupKey = createGroupKey(
            firstDetail.getFactoryCode(),
            firstDetail.getYear(),
            firstDetail.getMonth()
        );
        // 当前批次的第一个记录的创建时间
        Date currentCreateTime = firstDetail.getCreateTime();
        // 获取当前分组已有的最新批次
        BatchGroupKey existing = groupKeyToLatestBatch.get(groupKey);
        if (existing == null ||
            existing.getCreateTime().before(currentCreateTime)) {
          // 更新为创建时间更晚的批次
          groupKeyToLatestBatch.put(groupKey,
              new BatchGroupKey(batchNumber, currentCreateTime));
        }
      }
    });
    // 3. 提取批次号列表
    return groupKeyToLatestBatch.values().stream().sorted(Comparator.comparing(BatchGroupKey::getCreateTime).reversed())
        .map(BatchGroupKey::getBatchNumber)
        .collect(Collectors.toList());
  }

  private static String createGroupKey(String factoryCode, Integer year, Integer month) {
    return factoryCode + "|" + year + "|" + month;
  }

  // 辅助类，用于存储批次号和创建时间
  @Getter
  private static class BatchGroupKey {
    private final String batchNumber;
    private final Date createTime;

    public BatchGroupKey(String batchNumber, Date createTime) {
      this.batchNumber = batchNumber;
      this.createTime = createTime;
    }

  }
}
