package com.zlt.aps.mp.demand.mapper;


import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.SupplyOrderPool;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SupplyOrderPoolMapper.java
 * 描    述：供应链订单池Mapper接口
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Mapper
public interface SupplyOrderPoolEntityMapper extends CommBaseMapper<SupplyOrderPool> {
  /**
   *  获取调整供应链订单
   * @param createCondition 调整参数
   * @return 供应链订单
   */
  List<SupplyOrderPool> findAdjustSupplyOrderPool(DpDemandPlan createCondition);
}
