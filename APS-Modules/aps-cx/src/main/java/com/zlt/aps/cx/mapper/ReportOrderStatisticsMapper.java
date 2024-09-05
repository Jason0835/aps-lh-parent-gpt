package com.zlt.aps.cx.mapper;

import java.util.List;

import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;

/**
 * 每日各工序工单完成情况统计报表查询
 * 
 * @author hakimryan
 *
 */
public interface ReportOrderStatisticsMapper {

	/**
	 * 根据条件查询报表统计列表（统计方式：每天）
	 */
	List<ReportOrderStatisticsDto> selectReportStatisticsList(ReportOrderStatisticsDto dto);

	/**
	 * 根据条件查询报表统计列表（统计方式：汇总）
	 */
	List<ReportOrderStatisticsDto> selectReportSummaryList(ReportOrderStatisticsDto dto);
}
