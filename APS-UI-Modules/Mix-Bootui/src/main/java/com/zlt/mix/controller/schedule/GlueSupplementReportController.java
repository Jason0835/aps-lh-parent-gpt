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
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.schedule.api.service.IGlueScheduleSupplementService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 胶料补量历史记录查询Controller
 *
 */
@Api(tags = "胶料补量历史记录查询")
@Controller
@RequestMapping("/schedule/glueSupplementReport")
public class GlueSupplementReportController extends BaseController {

	@Resource
	private IGlueScheduleSupplementService glueScheduleSupplementService;
	@Resource
	private IExportLogService iExportLogService;

	private final String prefix = "schedule/supplementReport";

	/**
	 * 跳转至主页面
	 */
	@RequiresPermissions("schedule:glueSupplementReport:view")
	@GetMapping()
	public String toIndex(ModelMap modelMap) {
		modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", new Date()));
		return prefix + "/glueSupplement";
	}

	/**
	 * 查询日计划每日统计报表数据
	 * 
	 * @param glueScheduleSupplement
	 * @return
	 */
	@ApiOperation("查询日计划每日统计报表数据")
	@RequiresPermissions("schedule:glueSupplementReport:view")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo listScheduleReport(GlueScheduleSupplement glueScheduleSupplement) {
		return glueScheduleSupplementService.pageGlueScheduleSupplement(glueScheduleSupplement);
	}

	/**
	 * 导出工单完成统计报表
	 */
	@ApiOperation("导出日计划每日统计报表数据")
	@RequiresPermissions("schedule:glueSupplementReport:export")
	@GetMapping("/export")
	@ResponseBody
	public void export(HttpServletResponse response, GlueScheduleSupplement glueScheduleSupplement) throws IOException {
		String fileName = I18nUtil.getMessage("schedule.glueScheduleResult.supplement.glueReport");
		// 获取字节流数据
		List<GlueScheduleSupplement> list = glueScheduleSupplementService.export(glueScheduleSupplement);
		ExcelUtil<GlueScheduleSupplement> util = new ExcelUtil<>(GlueScheduleSupplement.class);
		Workbook workbook = util.exportExcel2(response, list, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName,
				glueScheduleSupplement.toString(), ZltConstant.PROCEDURE_CODE_MIX);
		iExportLogService.add(exportLog);
	}
}
