package com.zlt.aps.monthplan.factory.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IFactoryMonthPlanProductionFinalResultService.java
 * 描    述：IFactoryMonthPlanProductionFinalResultService工厂月生产计划-最终排产计划定稿后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
public interface IFactoryMonthPlanProductionFinalResultService extends IService<FactoryMonthPlanProductionFinalResult> {

  /**
   * 8、12个月结构上机频次 = 从定稿的月度排产计划，获取近12个月的已排产的月份个数
   * @return 定稿的月度排产计划
   */
  List<FactoryMonthPlanProductionFinalResult> findLastTwelveMonthProdFinalPlan();
  /**
   *  根据物料编号,通过月度生产计划表，获取近12个月有排产的月份个数
   * @param materialCode 物料编号
   * @return 近12个月有排产的月份个数
   */
  int getProductionMonthInLastTwelveMonth(String materialCode);
}
