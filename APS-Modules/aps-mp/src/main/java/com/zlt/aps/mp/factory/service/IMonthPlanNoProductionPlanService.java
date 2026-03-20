package com.zlt.aps.mp.factory.service;


import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.mp.api.domain.vo.MonthPlanStatisticsVo;
import com.zlt.bill.common.service.IDocService;

import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanNoProductionPlanService.java
 * 描    述：IMonthPlanNoProductionPlanServiceS2-0606.排产结果-未排产计划后端接口
 *@author yelq
 *@date 2026-01-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMonthPlanNoProductionPlanService  extends IDocService<MonthPlanNoProductionPlan>{
  /**
   * 列表查询
   */
  List<MonthPlanNoProductionPlan> selectList(MonthPlanNoProductionPlan query);

  /**
   * 统计未排SAP总量
   */
  void statistics(MonthPlanStatisticsVo statisticsVo, MonthPlanNoProductionPlan noProductionPlan);

  /**
   * 导出未排产数据
   * @param queryVO 查询条件
   * @return 导出的字节数组
   * @throws IOException IO异常
   */
  byte[] exportMonthPlanNoProductionPlan(MonthPlanNoProductionPlan queryVO) throws IOException;
}
