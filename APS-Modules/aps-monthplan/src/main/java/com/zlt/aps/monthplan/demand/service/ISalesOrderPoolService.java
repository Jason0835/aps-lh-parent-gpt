package com.zlt.aps.monthplan.demand.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISalesOrderPoolService.java
 * 描    述：ISalesOrderPoolService销售订单池后端接口
 *@author zlt
 *@date 2025-12-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ISalesOrderPoolService  extends IDocService<SalesOrderPool>{
	/**
	 * 批量修改同PO号的销售优先级
	 * @param salesOrderPool
	 * @return
	 */
	AjaxResult editBySalCodePo(SalesOrderPool salesOrderPool);
	
	/**
	 * 校验SCM已计划未发货数据
	 * @param salesOrderPool
	 * @return
	 */
	AjaxResult checkSCMData(SalesOrderPool salesOrderPool);
	
	/**
	 * 抓取SCM已计划未发货数据
	 * @param salesOrderPool
	 * @return
	 */
	AjaxResult getSCMData(SalesOrderPool salesOrderPool);
	
	/**
	 * 锁定订单池
	 * @return
	 */
	AjaxResult lockSalesOrderPool(SalesOrderPool billVO);
	
	/**
	 *  查询当前销售订单
	 * @return 当前销售订单
	 */
	List<SalesOrderPool> findCurrentSalesOrderPool();
}
