package com.zlt.aps.monthplan.demand.service;


import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IDpDemandPlanService.java
 * 描    述：IDpDemandPlanService需求计划后端接口
 *@author yelq
 *@date 2025-12-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IDpDemandPlanService  extends IDocService<DpDemandPlan>{
  /**
   *  生成需求计划
   * @param createCondition 参数
   */
  void createMonthRequire(DpDemandPlan createCondition);

}
