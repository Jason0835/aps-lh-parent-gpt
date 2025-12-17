package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpHistorySaleRecordService.java
 * 描    述：IMpHistorySaleRecordService历史销售记录后端接口
 *@author zlt
 *@date 2025-12-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpHistorySaleRecordService extends IDocService<MpHistorySaleRecord> {
  /**
   *  查询近12个月的月均销量大于零的月份数 > 8 的“SKU列表2”
   * @return
   */
  Set<String> findSkuInLastTwelveMonth();
}
