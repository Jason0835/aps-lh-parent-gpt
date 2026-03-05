package com.zlt.aps.mp.demand.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.SalesOrderPool;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

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
	 * 解锁订单池
	 * @return 结果
	 */
	AjaxResult unlockSalesOrderPool(SalesOrderPool billVO);

	/**
	 * 获取销售订单
	 * @param factoryCode
	 * @return
	 */
	List<SalesOrderPool> findCurrentSalesOrderPool(String factoryCode);
	/**
	 * 获取销售订单
	 * @param factoryCode
	 * @param eligibleSkus
	 * @return
	 */
	List<SalesOrderPool> findCurrentSalesOrderPool(String factoryCode, Set<String> eligibleSkus);

	/**
	 * 查询最新两个月的版本锁定情况
	 * @param salesOrderPool
	 * @return
	 */
	AjaxResult getMonthLock(SalesOrderPool salesOrderPool);
}
