package com.zlt.aps.monthplan.demand.service;



import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ISupplyOrderPoolService.java
 * 描    述：ISupplyOrderPoolService供应链订单池后端接口
 *@author yelq
 *@date 2025-12-22
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface ISupplyOrderPoolService  extends IDocService<SupplyOrderPool>{
  /**
   * 生成周期排产储备
   * @param supplyOrderPool
   */
  @Transactional
  void createCycleStockUp(SupplyOrderPool supplyOrderPool);
  /**
   *  生产常规储备
   * @param supplyOrderPool
   */
  @Transactional
  void createPrecedentStockUp(SupplyOrderPool supplyOrderPool);

  /**
   * 新增周期排产储备时候，输入储备数量的时候，需要加一个提示用户无订单库存有多少，月底计划余量有多少
   * @param supplyOrderPool 入参
   * @return AjaxResult
   */
  AjaxResult calculateStockMsg(SupplyOrderPool supplyOrderPool);
  /**
   *  输入物料编码，带出对应信息
   * @param supplyOrderPool  物料编码
   * @return 对应信息
   */
  SupplyOrderPool queryRelationByMaterialCode(SupplyOrderPool supplyOrderPool);
  /**
   *  查询当前年月供应链订单
   * @return 当前年月供应链订单
   */
  List<SupplyOrderPool> findCurrentSupplyOrderPool();
  /**
   * 生成周期排产储备
   */
  List<SupplyOrderPool> createCycleStockUp(DpDemandPlan createCondition, YearMonth yearMonth);
  /**
   * 生成周期排产储备
   */
  List<SupplyOrderPool> createPrecedentStockUp(DpDemandPlan createCondition,YearMonth yearMonth);
  /**
   *  超期校验
   * @param supplyOrderPool
   * @return
   */
  AjaxResult checkOverdue(SupplyOrderPool supplyOrderPool);
}
