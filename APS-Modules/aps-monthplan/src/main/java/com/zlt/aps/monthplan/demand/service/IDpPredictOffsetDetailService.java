package com.zlt.aps.monthplan.demand.service;


import com.zlt.aps.monthplan.api.domain.entity.DpPredictOffsetDetail;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpPredictOffsetDetailService.java
 * 描    述：IDpPredictOffsetDetailService预测冲减分配后端接口
 *@author yelq
 *@date 2026-01-20
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpPredictOffsetDetailService  extends IDocService<DpPredictOffsetDetail>{
  /**
   *  获取预测冲减详情
   * @param monthPlanVersions
   * @return
   */
  List<DpPredictOffsetDetail> findPredictOffsetDetail(Set<String> monthPlanVersions);
}
