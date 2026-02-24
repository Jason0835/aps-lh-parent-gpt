package com.zlt.aps.mp.demand.service;


import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpPredictionDetail;
import com.zlt.bill.common.service.IDocService;

import java.time.YearMonth;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpPredictionDetailService.java
 * 描    述：IMpPredictionDetailService预测明细后端接口
 *@author yelq
 *@date 2026-01-16
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpPredictionDetailService  extends IDocService<MpPredictionDetail>{
  /**
   *  批量插入预测明细
   * @param tMonthDemandPlan
   * @param productionVersions
   */
  void batchInsert(DpDemandPlan tMonthDemandPlan,Map<YearMonth, MpFactoryProductionVersion> productionVersions);
  /**
   * 获取版本信息
   * @param batchNumbers
   * @return
   */
  Map<String, Map<String, MpPredictionDetail>> fetchVersion(Set<String> batchNumbers);
}
