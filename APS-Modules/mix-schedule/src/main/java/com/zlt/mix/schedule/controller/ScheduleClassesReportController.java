package com.zlt.mix.schedule.controller;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.schedule.api.domain.dto.ScheduleClassesReportDto;
import com.zlt.mix.schedule.service.ScheduleClassesReportService;

import io.swagger.annotations.ApiOperation;

/**
 * 日计划班次报表统计Controller
 *
 */
@RestController
@RequestMapping("/scheduleClassesReport")
@SuppressWarnings("rawtypes")
public class ScheduleClassesReportController extends BaseController {
	@Resource
	private ScheduleClassesReportService scheduleReportService;

	/**
	 * 查询日计划每日报表统计列表
	 */
	@ApiOperation("查询日计划每日报表统计列表")
	@PostMapping("/selectScheduleReportList")
	public TableDataInfo listGlueCollectPlan(@RequestBody ScheduleClassesReportDto scheduleReportDto) {
		List<ScheduleClassesReportDto> list = scheduleReportService.selectScheduleReportList(scheduleReportDto);
		return getDataTable(list);
	}

	/**
	 * 获取报表的表头信息（拼接好的字符串）
	 * 
	 * @param scheduleReportDto
	 * @return
	 */
	@ApiOperation("获取报表的表头信息")
	@PostMapping("/getBaseTitle")
	public String getBaseTitle(@RequestBody ScheduleClassesReportDto scheduleReportDto) {
		String baseTitleTemplate = I18nUtil.getMessage("schedule.scheduleClassesReport.baseTitle"); // 统计行标记
		ScheduleClassesReportDto summary = scheduleReportService.getScheduleReportSummary(scheduleReportDto);
		if (summary != null) {
			return StringUtils.format(baseTitleTemplate, summary.getMidPlanSpec(), summary.getMidPlanQty(),
					summary.getMidFinishSpec(), summary.getMidFinishQty());
		} else {
			return StringUtils.format(baseTitleTemplate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
					BigDecimal.ZERO);
		}
	}

	/**
	 * 导出日计划每日报表统计列表
	 */
	@ApiOperation("导出日计划每日报表统计列表")
	@PostMapping("/exportScheduleReportList")
	public List<ScheduleClassesReportDto> export(@RequestBody ScheduleClassesReportDto scheduleReportDto) {
		List<ScheduleClassesReportDto> resultList = scheduleReportService.selectScheduleReportList(scheduleReportDto);
		if (CollectionUtils.isNotEmpty(resultList)) {
			String summaryStr = I18nUtil.getMessage("schedule.scheduleReport.summary"); // 统计行标记
			for (ScheduleClassesReportDto result : resultList) {
				if (ZltConstant.STATUS_DISABLE.equals(result.getIsSummary())) {
					result.setProcedure(summaryStr);
				}
			}
		}
		return resultList;
	}
}
