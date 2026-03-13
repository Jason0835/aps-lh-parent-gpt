package com.zlt.aps.mp.demand.service;


import com.zlt.aps.mp.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.mp.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanSumService.java
 * 描    述：IDpDemandPlanSumService需求计划汇总后端接口
 *@author yelq
 *@date 2026-01-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpDemandPlanSumService  extends IDocService<DpDemandPlanSum>{
  /**
   * 批量更新需求计划
   * @param billVO
   */
  void batchUpdateForDemand(DpDemandPlanSum billVO);
  /**
   *  获取需求计划版本号列表
   * @param queryCondition 查询条件
   * @return 需求计划版本号列表
   */
  List<String> findMonthPlanVersion(DpDemandPlanSum queryCondition);
  /**
   * 获取计划类型
   * @param selectedRequireVersion 选择的需求版本
   * @return
   */
  String  getPlanType(FactoryProductionPlanVo selectedRequireVersion);
}
