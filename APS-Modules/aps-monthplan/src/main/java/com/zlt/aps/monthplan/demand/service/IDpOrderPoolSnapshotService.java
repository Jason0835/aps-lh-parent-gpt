package com.zlt.aps.monthplan.demand.service;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderPoolSnapshot;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.IDocService;

import java.time.YearMonth;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpOrderPoolSnapshotService.java
 * 描    述：IDpOrderPoolSnapshotServiceS1-0206.订单池快照后端接口
 *@author yelq
 *@date 2025-12-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpOrderPoolSnapshotService  extends IDocService<DpOrderPoolSnapshot>{
  /**
   * 保存订单池快照数据
   * @param createCondition
   * @param salesOrders
   * @param supplyOrderPools
   */
  void saveOrderPoolSnapshot(DpDemandPlan createCondition, List<SalesOrderPool> salesOrders, List<SupplyOrderPool> supplyOrderPools);
  /**
   * 保存订单池快照数据
   * @param predictionVersion
   * @param yearMonth
   * @param allStockUpOrders
   */
  void saveOrderPoolSnapshot(String predictionVersion, YearMonth yearMonth,  List<SupplyOrderPool> allStockUpOrders);
  /**
   *  获取供应链订单
   * @param finalVersion
   * @return
   */
  List<SupplyOrderPool> fetchCycleStockOrder(MpFactoryProductionVersion finalVersion);
}
