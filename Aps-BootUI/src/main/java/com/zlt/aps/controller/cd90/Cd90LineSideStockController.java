package com.zlt.aps.controller.cd90;

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
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.api.service.ICd90LineSideStockService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 90°裁断线边库存信息Controller
 *
 * @author hak
 */
@Controller
@RequestMapping("/cd90/lineSideStock")
@Api(tags = { "90°裁断线边库存信息维护接口" })
public class Cd90LineSideStockController extends BaseController {
	private String prefix = "cd90/lineSideStock";

	@Autowired
	private ICd90LineSideStockService stockService;

	@Autowired
	private IExportLogService iExportLogService;

	/**
	 * 跳转至90°裁断线边库存列表页面
	 */
	@RequiresPermissions("cd90:lineSideStock:view")
	@GetMapping()
	public String operlog() {
		return prefix + "/stock";
	}

	/**
	 * 90°裁断线边库存信息列表
	 */
	@ApiOperation("查询90°裁断线边库存信息列表")
	@RequiresPermissions("cd90:lineSideStock:list")
	@PostMapping("/list")
	@ResponseBody
	public TableDataInfo list(Cd90LineSideStock stock) {
		return stockService.list(stock);
	}

	/**
	 * 到MES同步90°裁断线边库存信息
	 */
	@ApiOperation("到MES同步90°裁断线边库存信息")
	@RequiresPermissions("cd90:lineSideStock:syncStock")
	@PostMapping("/syncStock")
	@ResponseBody
	public AjaxResult syncStock() {
		return stockService.syncStock();
	}

	@ApiOperation("导出90°裁断线边库存信息")
	@RequiresPermissions("cd90:lineSideStock:export")
	@GetMapping("/export")
	@ResponseBody
	public void export(HttpServletResponse response, Cd90LineSideStock stock) throws IOException {
		List<Cd90LineSideStock> list = stockService.exportList(stock);
		ExcelUtil<Cd90LineSideStock> util = new ExcelUtil<>(Cd90LineSideStock.class);
		String fileName = I18nUtil.getMessage("ui.cd90.lineside.stock.export.fileName");
		Workbook workbook = util.exportExcel2(response, list, fileName);
		ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, stock.toString(),
				ApsConstant.PROCEDURE_CODE_CD90);
		iExportLogService.add(exportLog);
	}
}
