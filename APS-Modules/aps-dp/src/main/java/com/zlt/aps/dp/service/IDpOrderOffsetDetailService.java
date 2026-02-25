package com.zlt.aps.dp.service;


import com.zlt.aps.mp.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpOrderOffsetDetailService.java
 * 描    述：IDpOrderOffsetDetailServiceS1-0604订单冲减分配后端接口
 *@author yelq
 *@date 2025-12-30
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpOrderOffsetDetailService  extends IDocService<DpOrderOffsetDetail>{
  /**
   *  获取预测冲减详情
   * @param monthPlanVersion
   * @return
   */
  List<DpOrderOffsetDetail> findPredictOffsetDetail(String monthPlanVersion);

  /**
   *   获取订单冲减版本
   * @param dpOrderOffsetDetail 查询条件
   * @return 版本集合
   */
  List<String> getOffsetVersion(DpOrderOffsetDetail dpOrderOffsetDetail);
  /**
   * 批量插入数据
   * @param leftDemands
   */
  void batchInsert(List<DpOrderOffsetDetail> leftDemands);
}
