package com.zlt.aps.monthplan.demand.service;


import com.zlt.aps.monthplan.api.domain.entity.MpSkuProductionType;
import com.zlt.bill.common.service.IDocService;

import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpSkuProductionTypeService.java
 * 描    述：IMpSkuProductionTypeServiceSKU排产分类后端接口
 *@author yelq
 *@date 2025-12-26
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpSkuProductionTypeService  extends IDocService<MpSkuProductionType>{
  /**
   *  获取SKU对应的排产分类
   * @return SKU对应的排产分类
   */
  Map<String,String> skuToProductionType();
}
