package com.zlt.aps.cx.service;

import com.zlt.aps.cx.api.domain.dto.ReportStatisticsDto;

import java.util.List;

/**
 * @author: Chen
 * @since: 2022/4/25 13:39
 */
public interface ReportStatisticsService {

    /**
     * 根据条件查询报表统计列表
     */
    public List<ReportStatisticsDto> selectReportStatisticsList(ReportStatisticsDto dto);

    /**
     * 导出报表统计数据
     */
    public byte[] export(ReportStatisticsDto dto);
}
