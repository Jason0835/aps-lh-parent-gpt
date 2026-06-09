package com.zlt.aps.mp.factory.service;


import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthPlanStatisticsService.java
 * 描    述：IMpMonthPlanStatisticsServiceS2-0612.最终排产计划统计后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2026-02-05
 */
public interface IMpMonthPlanStatisticsService extends IDocService<MpMonthPlanStatistics> {

    /**
     * 删除月计划统计结果
     *
     * @param factoryCode       工厂
     * @param year              年份
     * @param month             月份
     * @param productionVersion 排程版本
     * @param tempFlag
     * @param structureList     结构列表
     */
    void deleteMonthPlanStatisticsByCondition(String factoryCode, String year, String month, String productionVersion, String tempFlag, List<String> structureList);

    /**
     * 根据排产版本号，获取排产统计信息
     * 如果isFinalAdjust = true，则从本身表t_mp_month_plan_statistics中获取
     * 否则，先从定稿备份表中t_mp_final_statistics_log获取，有则直接返回
     * 否则，再从t_mp_month_plan_statistics本身表中获取
     *
     * @param factoryMonthPlanMouldDayResult 其它查询条件
     * @param productionVersion              排产版本号
     * @param isFinalAdjust                  是否月计划调整入口
     * @return
     */
    List<MpMonthPlanStatistics> getStatisticsInfo(FactoryMonthPlanMouldDayResult factoryMonthPlanMouldDayResult,
                                                  String productionVersion,
                                                  boolean isFinalAdjust);

    /**
     * 获取排产统计信息
     * 如果isFinalAdjust = true，则从本身表t_mp_month_plan_statistics中获取
     * 否则，先从定稿备份表中t_mp_final_statistics_log获取，有则直接返回
     * 否则，再从t_mp_month_plan_statistics本身表中获取
     *
     * @param factoryCode       工厂
     * @param productionVersion 排产版本
     * @param isFinalAdjust     是否月计划调整入口
     * @return
     */
    Map<String, MpMonthPlanStatistics> getStatisticsInfo(String factoryCode, String productionVersion, boolean isFinalAdjust);
}
