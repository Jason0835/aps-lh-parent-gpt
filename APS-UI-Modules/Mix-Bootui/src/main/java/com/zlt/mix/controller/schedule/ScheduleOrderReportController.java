package com.zlt.mix.controller.schedule;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;
import com.zlt.mix.schedule.api.service.IScheduleOrderReportService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 各工序工单完成统计报表Controller
 *
 */
@Api(tags = "各工序工单完成统计报表")
@Controller
@RequestMapping("/schedule/scheduleOrderReport")
public class ScheduleOrderReportController extends BaseController {

	@Resource
	private IScheduleOrderReportService iScheduleOrderReportService;
	@Resource
	private IExportLogService iExportLogService;

	private final String prefix = "schedule/scheduleOrderReport";

	/**
	 * 跳转至主页面
	 */
	@RequiresPermissions("schedule:scheduleOrderReport:view")
	@GetMapping()
	public String toIndex(ModelMap modelMap) {
		modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));
		return prefix + "/scheduleOrderReport";
	}

	/**
	 * 查询日计划每日统计报表数据
	 * 
	 * @param scheduleReportDto
	 * @return
	 */
	@ApiOperation("查询各工序工单完成统计报表数据")
	@RequiresPermissions("schedule:scheduleOrderReport:view")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo listScheduleReport(ScheduleOrderReportDto scheduleReportDto) {
		return iScheduleOrderReportService.selectScheduleReportList(scheduleReportDto);
	}

	/**
	 * 导出工单完成统计报表
	 */
	@ApiOperation("导出各工序工单完成统计报表数据")
	@RequiresPermissions("schedule:scheduleOrderReport:export")
	@GetMapping("/export")
	@ResponseBody
	public void export(HttpServletResponse response, ScheduleOrderReportDto scheduleReportDto) throws IOException {
		String fileName = I18nUtil.getMessage("schedule.scheduleOrderReport.modelName");
		// 获取字节流数据
		List<ScheduleOrderReportDto> list = iScheduleOrderReportService.export(scheduleReportDto);
		ExcelUtil<ScheduleOrderReportDto> util = new ExcelUtil<>(ScheduleOrderReportDto.class);
		Workbook workbook = util.exportExcel2(response, list, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName,
				scheduleReportDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
		iExportLogService.add(exportLog);
	}
}
