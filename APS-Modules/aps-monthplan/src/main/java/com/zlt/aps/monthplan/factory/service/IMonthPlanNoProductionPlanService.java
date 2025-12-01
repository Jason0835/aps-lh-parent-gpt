package com.zlt.aps.monthplan.factory.service;


import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanStatisticsVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanNoProductionPlanService.java
 * 描    述：IMonthPlanNoProductionPlanService分厂月生产计划排产过程-未排产计划后端接口
 *@author zlt
 *@date 2025-03-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMonthPlanNoProductionPlanService{

    /**
     * 列表查询
     */
    List<MonthPlanNoProductionPlan> selectList(MonthPlanNoProductionPlan query);

    /**
     * 统计未排SAP总量
     */
    void statistics(MonthPlanStatisticsVo statisticsVo, MonthPlanNoProductionPlan noProductionPlan);
}
