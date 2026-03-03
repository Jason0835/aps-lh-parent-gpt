package com.zlt.aps.mp.demand.service;


import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.DpStockVersion;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpStockVersionService.java
 * 描    述：IDpStockVersionService需求计划_版本库存后端接口
 *@author yelq
 *@date 2025-12-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpStockVersionService  extends IDocService<DpStockVersion>{
  /**
   * 将分配时的成品库存记录到库存版本表中(以需求版本号的维度)；
   * @param demandPlan 需求计划
   * @param finishedProductStockMap 成品库存记录
   */
  void insertBatchData(DpDemandPlan demandPlan, Map<String, List<MdmProductStock>> finishedProductStockMap);

  /**
   *  获取需求计划版本号列表
   * @param queryCondition 查询条件
   * @return 需求计划版本号列表
   */
  List<String> findMonthPlanVersion(DpStockVersion queryCondition);
}
