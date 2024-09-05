package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.ReportClassAccuracyDto;
import com.zlt.aps.cx.service.ReportClassAccuracyService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班次完成统计报表Controller
 *
 * @author chen
 * @date 2022-05-23
 */
@RestController
@RequestMapping("/reportClassAccuracy")
public class ReportClassAccuracyController extends BaseController {
    @Autowired
    private ReportClassAccuracyService reportClassAccuracyService;

    /**
     * 查询班次完成统计报表列表
     */
    @ApiOperation("查询班次完成统计报表列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ReportClassAccuracyDto reportClassAccuracy) {
        List<ReportClassAccuracyDto> list = reportClassAccuracyService.selectReportClassAccuracyList(reportClassAccuracy);
        return getDataTable(list);
    }

    /**
     * 导出班次完成统计报表列表
     */
    @Log(title = "ui.data.column.reportClassAccuracy.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/getList")
    public List<ReportClassAccuracyDto> getList(@RequestBody ReportClassAccuracyDto reportClassAccuracy) {
        startPage();
        return reportClassAccuracyService.selectReportClassAccuracyList(reportClassAccuracy);
    }

    /**
     * 导出班次完成统计报表列表
     */
    @Log(title = "ui.data.column.reportClassAccuracy.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出班次完成统计报表列表")
    @PostMapping("/export")
    public byte[] export(@RequestBody ReportClassAccuracyDto reportClassAccuracy) {
        return reportClassAccuracyService.export(reportClassAccuracy);
    }
}
