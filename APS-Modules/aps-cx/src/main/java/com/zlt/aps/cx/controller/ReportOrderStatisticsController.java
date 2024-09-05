package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.ReportOrderStatisticsDto;
import com.zlt.aps.cx.service.ReportOrderStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单完成统计报表Controller
 *
 * @author: Chen
 * @since: 2022/8/10 10:48
 */
@Api(tags = "工单完成统计报表")
@RestController
@RequestMapping("/reportOrderStatistics")
public class ReportOrderStatisticsController extends BaseController {

    @Autowired
    private ReportOrderStatisticsService reportOrderStatisticsService;

    /**
     * 根据条件查询报表统计列表（统计方式：每天）
     */
    @ApiOperation("根据条件查询报表统计列表（统计方式：每天）")
    @PostMapping("/selectReportStatisticsList")
    public TableDataInfo selectReportStatisticsList(@RequestBody ReportOrderStatisticsDto dto) {
        List<ReportOrderStatisticsDto> list = reportOrderStatisticsService.selectReportStatisticsList(dto);
        return getDataTable(list);
    }

    /**
     * 根据条件查询报表统计列表（统计方式：汇总）
     */
    @ApiOperation("根据条件查询报表统计列表（统计方式：汇总）")
    @PostMapping("/selectReportSummaryList")
    public TableDataInfo selectReportSummaryList(@RequestBody ReportOrderStatisticsDto dto) {
        List<ReportOrderStatisticsDto> list = reportOrderStatisticsService.selectReportSummaryList(dto);
        return getDataTable(list);
    }

    /**
     * 导出班次完成统计报表列表
     */
    @Log(title = "ui.data.column.reportOrderStatistics.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody ReportOrderStatisticsDto dto) {
        return reportOrderStatisticsService.export(dto);
    }
}
