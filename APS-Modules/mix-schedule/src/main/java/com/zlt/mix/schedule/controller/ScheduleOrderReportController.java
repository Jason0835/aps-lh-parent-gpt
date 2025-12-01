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
import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;
import com.zlt.mix.schedule.service.ScheduleOrderReportService;

import io.swagger.annotations.ApiOperation;

/**
 * 各工序工单完成统计报表Controller
 *
 */
@RestController
@RequestMapping("/scheduleOrderReport")
public class ScheduleOrderReportController extends BaseController {
	@Resource
	private ScheduleOrderReportService scheduleReportService;

	/**
	 * 查询日计划每日报表统计列表
	 */
	@ApiOperation("查询各工序工单完成统计列表")
	@PostMapping("/selectScheduleReportList")
	public TableDataInfo listGlueCollectPlan(@RequestBody ScheduleOrderReportDto scheduleReportDto) throws ParseException {
		List<ScheduleOrderReportDto> list = scheduleReportService.selectScheduleReportList(scheduleReportDto);
		return getDataTable(list);
	}

	/**
	 * 导出日计划每日报表统计列表
	 */
	@ApiOperation("导出各工序工单完成统计列表")
	@PostMapping("/exportScheduleReportList")
	public List<ScheduleOrderReportDto> export(@RequestBody ScheduleOrderReportDto scheduleReportDto) {
		return scheduleReportService.selectScheduleReportList(scheduleReportDto);
	}
}
