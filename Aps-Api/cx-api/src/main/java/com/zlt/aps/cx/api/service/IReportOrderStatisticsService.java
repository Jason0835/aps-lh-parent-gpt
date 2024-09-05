package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 报表统计列表Service接口
 * @author: Chen
 * @since: 2022/8/10 10:53
 */
@FeignClient(contextId = "IReportOrderStatisticsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IReportOrderStatisticsService {

    /**
     * 根据条件查询报表统计列表（统计方式：每天）
     */
    @ApiOperation("根据条件查询报表统计列表（统计方式：每天）")
    @PostMapping("/reportOrderStatistics/selectReportStatisticsList")
    public TableDataInfo selectReportStatisticsList(@RequestBody ReportOrderStatisticsDto dto);

    /**
     * 根据条件查询报表统计列表（统计方式：汇总）
     */
    @ApiOperation("根据条件查询报表统计列表（统计方式：汇总）")
    @PostMapping("/reportOrderStatistics/selectReportSummaryList")
    public TableDataInfo selectReportSummaryList(@RequestBody ReportOrderStatisticsDto dto);

    /**
     * 导出班次完成统计报表列表
     */
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/reportOrderStatistics/export")
    public byte[] export(@RequestBody ReportOrderStatisticsDto dto);
}
