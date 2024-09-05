package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;

import java.util.List;

/**
 * 每日各工序工单完成情况统计报表Service
 * @author: Chen
 * @since: 2022/8/10 10:27
 */
public interface ReportOrderStatisticsService {

    /**
     * 根据条件查询报表统计列表（统计方式：每天）
     */
    List<ReportOrderStatisticsDto> selectReportStatisticsList(ReportOrderStatisticsDto dto);

    /**
     * 根据条件查询报表统计列表（统计方式：汇总）
     */
    List<ReportOrderStatisticsDto> selectReportSummaryList(ReportOrderStatisticsDto dto);

    /**
     * 导出报表统计列表
     */
    public byte[] export(ReportOrderStatisticsDto dto);
}
