package com.zlt.aps.mp.demand.mapper;

import java.util.List;

import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import org.apache.ibatis.annotations.Mapper;

import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPoolMapper.java
 * 描    述：销售订单池Mapper接口
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface SalesOrderPoolEntityMapper extends CommBaseMapper<SalesOrderPool> {
	
	/**
	 * 批量保存
	 * @param salesOrderPoolList
	 * @return
	 */
	int batchInsert(List<SalesOrderPool> salesOrderPoolList);
	/**
	 * 获取调整的销售订单
	 * @param createCondition 调整参数
	 * @return 销售订单
	 */
  List<SalesOrderPool> findAdjustSalesOrderPool(DpDemandPlan createCondition);
}
