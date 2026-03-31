package com.zlt.aps.mp.common.utils;

import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.aps.mp.demand.service.IDpOrderOffsetDetailService;
import com.zlt.aps.mp.demand.service.IDpShippedNotScanVersionService;
import com.zlt.aps.mp.demand.service.IDpStockVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 保存分配结果
 * @author Yelq
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SaveAllocationResultService {
  private final IDpOrderOffsetDetailService dpOrderOffsetDetailService;
  // 版本库存
  private final IDpStockVersionService dpStockVersionService;
  
  private final IDpShippedNotScanVersionService dpShippedNotScanVersion;

  @Async("batchInsertExecutor")
  public void saveAllocationResults(
      DpDemandPlan demandPlan,
      PredictionContext.OrderAllocationResult allocationResult) {
    // 批量插入分配结果
    if (!org.springframework.util.CollectionUtils.isEmpty(allocationResult.getAllocations())) {
      this.dpOrderOffsetDetailService.batchInsert(allocationResult.getAllocations());
    }
    if(!org.springframework.util.CollectionUtils.isEmpty(allocationResult.getStockMap())) {
      // 批量插入库存版本
      dpStockVersionService.insertBatchData(demandPlan, allocationResult.getStockMap());
    }
    if(!org.springframework.util.CollectionUtils.isEmpty(allocationResult.getNotScanMap())) {
        // 批量插入未扫描订单版本
        DpShippedNotScanVersion queryCondition = new DpShippedNotScanVersion();
        queryCondition.setFactoryCode(demandPlan.getFactoryCode());
        queryCondition.setYear(demandPlan.getYear());
        queryCondition.setMonth(demandPlan.getMonth());
        queryCondition.setRequireVersion(demandPlan.getMonthPlanVersion());
        dpShippedNotScanVersion.generateShippedNotScanVersion(queryCondition);
    }
  }
}
