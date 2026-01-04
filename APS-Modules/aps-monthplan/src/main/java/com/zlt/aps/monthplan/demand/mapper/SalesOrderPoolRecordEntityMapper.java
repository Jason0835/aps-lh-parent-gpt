package com.zlt.aps.monthplan.demand.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPoolRecord;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：SalesOrderPoolRecordEntityMapper.java
 * 描    述：销售订单池Mapper接口
 *@author zlt
 *@date 2025-12-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface SalesOrderPoolRecordEntityMapper extends CommBaseMapper<SalesOrderPoolRecord> {
	
	/**
	 * 批量保存
	 * @param salesOrderPoolRecordList
	 * @return
	 */
	int batchInsert(List<SalesOrderPoolRecord> salesOrderPoolRecordList);
	
}
