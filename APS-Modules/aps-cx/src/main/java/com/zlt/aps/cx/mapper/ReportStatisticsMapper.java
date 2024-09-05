package com.zlt.aps.cx.mapper;

import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;

import java.util.List;

/**
 * @author: Chen
 * @since: 2022/4/25 13:42
 */
public interface ReportStatisticsMapper {

    /**
     * 根据条件查询报表统计列表（统计方式：每天）
     */
    List<ReportStatisticsDto> selectReportStatisticsList(ReportStatisticsDto dto);

    /**
     * 根据条件查询报表统计列表（统计方式：汇总）
     */
    List<ReportStatisticsDto> selectReportSummaryList(ReportStatisticsDto dto);
}
