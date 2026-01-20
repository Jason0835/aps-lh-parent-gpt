package com.zlt.aps.controller.report;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.finereport.IFinereportService;
import com.zlt.aps.itf.finereport.vo.FinereportParams;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Api(tags = { "报表控制器" })
@Controller
@RequestMapping("/report")
public class FinereportUIController extends BaseController {
	@Autowired
	private IFinereportService iFinereportService;

    @ApiOperation("帆软报表公共预览页面")
    @PostMapping("/reportView")
    @ResponseBody
    public AjaxResult reportView(@RequestBody FinereportParams params) {
        if (params == null || StringUtils.isEmpty(params.getReportCode())) {
            return AjaxResult.error("报表参数配置有误");
        }
        return iFinereportService.reportView(params.getReportCode());
    }

	@ApiOperation("库存库龄分析报表")
	@RequiresPermissions("report:inventoryAgeAnalysis")
	@PostMapping("/inventoryAgeAnalysis")
	@ResponseBody
	public AjaxResult inventoryAgeAnalysis() {
		return iFinereportService.inventoryAgeAnalysis();
	}

    /**
     * 单胎总重报表
     *
     * @return 结果
     */
    @ApiOperation("单胎总重报表")
    @RequiresPermissions("report:singleTireTotalWeight")
    @PostMapping("/singleTireTotalWeight")
    @ResponseBody
    public AjaxResult singleTireTotalWeight() {
        return iFinereportService.singleTireTotalWeight();
    }

    /**
     * 越南工厂成型机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂成型机数据报表")
    @RequiresPermissions("report:factoryMoldingMachine")
    @PostMapping("/factoryMoldingMachine")
    @ResponseBody
    public AjaxResult factoryMoldingMachine() {
        return iFinereportService.factoryMoldingMachine();
    }

    /**
     * 越南工厂硫化机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂硫化机数据报表")
    @RequiresPermissions("report:factoryVulcanizingMachine")
    @PostMapping("/factoryVulcanizingMachine")
    @ResponseBody
    public AjaxResult factoryVulcanizingMachine() {
        return iFinereportService.factoryVulcanizingMachine();
    }

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂结构在机数据报表")
    @RequiresPermissions("report:productionStructure")
    @PostMapping("/productionStructure")
    @ResponseBody
    public AjaxResult productionStructure() {
        return iFinereportService.productionStructure();
    }

    /**
     * 越南工厂年度产量
     */
    @ApiOperation("越南工厂年度产量")
    @RequiresPermissions("report:productionYear")
    @PostMapping("/productionYear")
    @ResponseBody
    public AjaxResult productionYear() {
        return iFinereportService.productionYear();
    }

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果
     */
    @ApiOperation("订单冲减库存报表")
    @RequiresPermissions("report:orderOffset")
    @PostMapping("/orderOffset")
    @ResponseBody
    public AjaxResult orderOffset() {
        return iFinereportService.orderOffset();
    }
}
