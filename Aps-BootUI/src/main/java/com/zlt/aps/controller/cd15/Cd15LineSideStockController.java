package com.zlt.aps.controller.cd15;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;
import com.zlt.aps.cd15.api.service.ICd15LineSideStockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 15°裁断线边库存信息Controller
 *
 * @author hak
 */
@Controller
@RequestMapping("/cd15/lineSideStock")
@Api(tags = { "15°裁断线边库存信息维护接口" })
public class Cd15LineSideStockController extends BaseController {
	private String prefix = "cd15/lineSideStock";

	@Autowired
	private ICd15LineSideStockService stockService;

	@Autowired
	private IExportLogService iExportLogService;

	/**
	 * 跳转至15°裁断线边库存列表页面
	 */
	@RequiresPermissions("cd15:lineSideStock:view")
	@GetMapping()
	public String operlog() {
		return prefix + "/stock";
	}

	/**
	 * 15°裁断线边库存信息列表
	 */
	@ApiOperation("查询15°裁断线边库存信息列表")
	@RequiresPermissions("cd15:lineSideStock:list")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo list(Cd15LineSideStock stock) {
		return stockService.list(stock);
	}

	/**
	 * 到MES同步15°裁断线边库存信息
	 */
	@ApiOperation("到MES同步15°裁断线边库存信息")
	@RequiresPermissions("cd15:lineSideStock:syncStock")
	@PostMapping("/syncStock")
	@ResponseBody
	public AjaxResult syncStock() {
		return stockService.syncStock();
	}

	@ApiOperation("导出15°裁断线边库存信息")
	@RequiresPermissions("cd15:lineSideStock:export")
	@GetMapping("/export")
	@ResponseBody
	public void export(HttpServletResponse response, Cd15LineSideStock stock) throws IOException {
		List<Cd15LineSideStock> list = stockService.exportList(stock);
		ExcelUtil<Cd15LineSideStock> util = new ExcelUtil<>(Cd15LineSideStock.class);
		String fileName = I18nUtil.getMessage("ui.cd15.lineside.stock.export.fileName");
		Workbook workbook = util.exportExcel2(response, list, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(),
				ApsConstant.PROCEDURE_CODE_CD15);
		iExportLogService.add(exportLog);
	}
}
