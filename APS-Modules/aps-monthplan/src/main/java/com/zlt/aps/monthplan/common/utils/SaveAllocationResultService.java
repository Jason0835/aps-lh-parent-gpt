package com.zlt.aps.monthplan.common.utils;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
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

  @Async("batchInsertExecutor")
  public void saveAllocationResults(
      DpDemandPlan createCondition,
      String monthPlanVersion,
      PredictionContext.OrderAllocationResult allocationResult) {
    // 批量插入分配结果
    if (!org.springframework.util.CollectionUtils.isEmpty(allocationResult.getAllocations())) {
      this.dpOrderOffsetDetailService.batchInsert(allocationResult.getAllocations());
    }
    if(!org.springframework.util.CollectionUtils.isEmpty(allocationResult.getStockMap())) {
      // 批量插入库存版本
      dpStockVersionService.insertBatchData(createCondition, monthPlanVersion, allocationResult.getStockMap());
    }
  }
}
