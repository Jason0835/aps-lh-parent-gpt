package com.zlt.mix.schedule.controller;

import java.text.ParseException;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleReportDto;
import com.zlt.mix.schedule.service.ScheduleReportService;

import io.swagger.annotations.ApiOperation;

/**
 * 日计划每日报表统计Controller
 *
 */
@RestController
@RequestMapping("/scheduleReport")
public class ScheduleReportController extends BaseController {
	@Resource
	private ScheduleReportService scheduleReportService;

	/**
	 * 查询日计划每日报表统计列表
	 */
	@ApiOperation("查询日计划每日报表统计列表")
	@PostMapping("/selectScheduleReportList")
	public TableDataInfo listGlueCollectPlan(@RequestBody ScheduleReportDto scheduleReportDto) throws ParseException {
		List<ScheduleReportDto> list = scheduleReportService.selectScheduleReportList(scheduleReportDto);
		return getDataTable(list);
	}

	/**
	 * 导出日计划每日报表统计列表
	 */
	@ApiOperation("导出日计划每日报表统计列表")
	@PostMapping("/exportScheduleReportList")
	public List<ScheduleReportDto> export(@RequestBody ScheduleReportDto scheduleReportDto) {
		return scheduleReportService.selectScheduleReportList(scheduleReportDto);
	}
}
