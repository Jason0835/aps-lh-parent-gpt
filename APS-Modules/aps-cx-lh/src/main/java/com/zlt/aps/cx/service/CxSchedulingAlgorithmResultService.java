package com.zlt.aps.cx.service;


import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;

import java.util.Date;
import java.util.List;

/**
 * 成型排程算法引擎后端接口
 *
 * @author tlt Nick
 * time：2025-02-12
 */
public interface CxSchedulingAlgorithmResultService{

    /**
     * title: 成型自动排程入口
     *
     * @param scheduleDate 排程日期
     * @param durationDays 连续排程天数
     * @param factoryCode
     */
    void calculateMoldingPlan(Date scheduleDate, int durationDays, String factoryCode);

    /**
     * 成型单日自动排程【全机台排程】
     *
     * @param lhScheduleResults 硫化工序硫计划列表
     * @param scheduleDate      排程日期
     * @param isTomorrow
     * @param cxBatchNo
     */
    void calculateSingleDayMoldingPlan(List<LhScheduleResult> lhScheduleResults, Date scheduleDate, boolean isTomorrow, String cxBatchNo);

}


