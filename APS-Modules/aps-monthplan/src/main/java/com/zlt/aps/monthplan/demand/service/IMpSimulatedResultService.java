package com.zlt.aps.monthplan.demand.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MpSimulatedResult;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpSimulatedResultService.java
 * 描    述：IMpSimulatedResultServiceS2-1004.实单模拟排产后端接口
 *@author yelq
 *@date 2025-12-31
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpSimulatedResultService  extends IDocService<MpSimulatedResult>{
  /**
   *  实单模拟排产
   * @param createCondition
   * @return
   */
  AjaxResult createVmMonthPrediction(MpSimulatedResult createCondition) throws InterruptedException;
}
