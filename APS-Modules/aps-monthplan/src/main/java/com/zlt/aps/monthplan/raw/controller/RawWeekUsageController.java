package com.zlt.aps.monthplan.raw.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.monthplan.raw.service.impl.RawWeekUsageGenerateServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Nick
 */
@RestController
@RequestMapping("/raw-week-usage")
@Api(tags = "周维度原材料用量管理")
public class RawWeekUsageController {

    @Autowired
    private RawWeekUsageGenerateServiceImpl rawWeekUsageGenerateService;

    @PostMapping("/generate")
    @ApiOperation("生成周维度原材料用量记录")
    public AjaxResult generate(@RequestParam String factoryCode,
                               @RequestParam Integer year,
                               @RequestParam Integer month) {
        return rawWeekUsageGenerateService.generateWeekUsage(factoryCode, year, month);
    }

    @PostMapping("/recalculate")
    @ApiOperation("重新计算周用量记录")
    public AjaxResult recalculate(@RequestParam String factoryCode,
                                  @RequestParam Integer year,
                                  @RequestParam Integer month,
                                  @RequestParam Integer week) {
        return rawWeekUsageGenerateService.recalculateWeekUsage(factoryCode, year, month, week);
    }

    @GetMapping("/statistics")
    @ApiOperation("获取周用量统计数据")
    public AjaxResult getStatistics(@RequestParam String factoryCode,
                                    @RequestParam Integer year,
                                    @RequestParam(required = false) Integer month,
                                    @RequestParam(required = false) Integer week) {
        Map<String, Object> statistics = rawWeekUsageGenerateService
                .getWeekUsageStatistics(factoryCode, year, month, week);
        return AjaxResult.success(statistics);
    }

    @PostMapping("/generate-for-month")
    @ApiOperation("为月度计划生成周用量记录")
    public AjaxResult generateForMonth(@RequestParam String factoryCode,
                                       @RequestParam Integer year,
                                       @RequestParam Integer month) {
        return rawWeekUsageGenerateService.generateWeekUsageForMonth(factoryCode, year, month);
    }
}