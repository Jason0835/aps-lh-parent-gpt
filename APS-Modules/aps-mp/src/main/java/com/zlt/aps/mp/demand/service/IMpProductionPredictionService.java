package com.zlt.aps.mp.demand.service;


import com.zlt.aps.mp.api.domain.entity.MpProductionPrediction;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpProductionPredictionService.java
 * 描    述：IMpProductionPredictionServiceS2-1002.未来产量预测后端接口
 *@author yelq
 *@date 2025-12-28
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpProductionPredictionService  extends IDocService<MpProductionPrediction>{
  /**
   *  生成订单预测
   * @param createCondition 参数
   * @return 结果
   */
  void createMonthPrediction(MpProductionPrediction createCondition) throws InterruptedException;
  /**
   *  获取预测版本号列表
   * @param queryCondition 查询条件
   * @return 预测版本号列表
   */
  List<String> findPredictionVersion(MpProductionPrediction queryCondition);
}
