package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMonthSurplusService.java
 * 描    述：IMdmMonthSurplusService0140基础数据_月底计划余量后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
public interface IMdmMonthSurplusService extends IDocService<MdmMonthSurplus> {
  /**
   *
   * @param createCondition 参数
   * @param requireVersionNumber 需求版本号
   * @param finishedProductStockMap 成品库存
   */
  void calculateMonthSurplus(MpDemandPlan createCondition, String requireVersionNumber,Map<String, List<MpFinishedProductStock>> finishedProductStockMap);
}
