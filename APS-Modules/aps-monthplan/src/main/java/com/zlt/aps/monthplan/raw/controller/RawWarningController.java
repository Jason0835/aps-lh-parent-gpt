package com.zlt.aps.monthplan.raw.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;

import com.zlt.aps.monthplan.raw.service.IRawWarningService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Nick
 */
@RestController
@RequestMapping("/raw-warning")
@Api(tags = "原材料预警管理")
public class RawWarningController {

    @Autowired
    private IRawWarningService rawWarningService;

    @PostMapping("/execute-usage-warning")
    @ApiOperation("执行用量偏差预警")
    public AjaxResult executeUsageWarning(@RequestParam String factoryCode,
                                          @RequestParam Integer year,
                                          @RequestParam Integer week) {
        return rawWarningService.executeUsageDeviationWarning(factoryCode, year, week);
    }

    @PostMapping("/execute-new-material-warning")
    @ApiOperation("执行新材料预警")
    public AjaxResult executeNewMaterialWarning(@RequestParam String factoryCode,
                                                @RequestParam Integer year,
                                                @RequestParam Integer month) {
        return rawWarningService.executeNewMaterialWarning(factoryCode, year, month);
    }

    @PostMapping("/sync-actual-usage")
    @ApiOperation("同步实际用量数据")
    public AjaxResult syncActualUsage(@RequestParam String factoryCode,
                                      @RequestParam Integer year,
                                      @RequestParam Integer week) {
        return rawWarningService.syncWeekActualUsage(factoryCode, year, week);
    }

    @GetMapping("/query-warnings")
    @ApiOperation("查询预警记录")
    public AjaxResult queryWarnings(@RequestParam(required = false) String factoryCode,
                                    @RequestParam(required = false) String warningType,
                                    @RequestParam(required = false) Date startDate,
                                    @RequestParam(required = false) Date endDate,
                                    @RequestParam(required = false) String status) {
        List<RawWarningRecord> warnings = rawWarningService.queryWarningRecords(
                factoryCode, warningType, startDate, endDate, status);
        return AjaxResult.success(warnings);
    }

    @PostMapping("/handle-warning")
    @ApiOperation("处理预警记录")
    public AjaxResult handleWarning(@RequestParam Long id,
                                    @RequestParam String handler,
                                    @RequestParam String opinion) {
        return rawWarningService.handleWarning(id, handler, opinion);
    }

    @GetMapping("/statistics")
    @ApiOperation("获取预警统计")
    public AjaxResult getStatistics(@RequestParam String factoryCode,
                                    @RequestParam(required = false) String warningType,
                                    @RequestParam(required = false) Integer days) {
        Map<String, Object> statistics = rawWarningService.getWarningStatistics(
                factoryCode, warningType, days);
        return AjaxResult.success(statistics);
    }

    @GetMapping("/config-test")
    @ApiOperation("测试预警配置")
    public AjaxResult testWarningConfig() {
        // 这里可以添加测试预警配置的接口
        return AjaxResult.success("预警功能已就绪");
    }
}