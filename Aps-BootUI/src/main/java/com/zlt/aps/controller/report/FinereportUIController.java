package com.zlt.aps.controller.report;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.finereport.IFinereportService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = { "报表控制器" })
@Controller
@RequestMapping("/report")
public class FinereportUIController extends BaseController {
	@Autowired
	private IFinereportService iFinereportService;

	@ApiOperation("库存库龄分析报表")
	@RequiresPermissions("report:inventoryAgeAnalysis")
	@PostMapping("/inventoryAgeAnalysis")
	@ResponseBody
	public AjaxResult inventoryAgeAnalysis() {
		return iFinereportService.inventoryAgeAnalysis();
	}
}
