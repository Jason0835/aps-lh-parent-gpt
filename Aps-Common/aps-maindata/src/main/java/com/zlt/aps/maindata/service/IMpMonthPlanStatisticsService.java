package com.zlt.aps.maindata.service;


import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanStatisticsService.java
 * 描    述：IMpMonthPlanStatisticsServiceS2-0612.最终排产计划统计后端接口
 *@author zlt
 *@date 2026-02-05
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpMonthPlanStatisticsService  extends IDocService<MpMonthPlanStatistics>{

    /**
     * 删除月计划统计结果
     * @param factoryCode 工厂
     * @param year 年份
     * @param month 月份
     * @param productionVersion 排程版本
     */
    void deleteMonthPlanStatisticsByCondition(String factoryCode, String year, String month, String productionVersion, List<String> structureList);

}
