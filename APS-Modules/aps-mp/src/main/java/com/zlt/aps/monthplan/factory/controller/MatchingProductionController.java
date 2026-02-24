package com.zlt.aps.monthplan.factory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.engine.scheduling.matching.MatchingProductionHandler;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 搭配排产服务 后台业务服务入口
 *
 * @author ZLT
 * @date 20260109
 */
@RestController
@RequestMapping("/matchingProduction")
@Api(value = "搭配排产服务", tags = "搭配排产服务")
public class MatchingProductionController extends BaseController {
	@Autowired
	private MatchingProductionHandler matchingProductionHandler;

	/**
	 * 搭配排产测试入口
	 *
	 * @param productionVersion 待搭配生产版本
	 */
	@ApiOperation("搭配排产测试入口")
	@PostMapping("/matchingProduction/{productionVersion}")
	public AjaxResult matchingProduction(@PathVariable("productionVersion") String productionVersion) {
		matchingProductionHandler.matchingProduction(productionVersion);
		return AjaxResult.success();
	}

    /**
     * 周程滚动搭配排产测试入口
     *
     * @param productionVersion 待搭配生产版本
     */
    @ApiOperation("周程滚动搭配排产测试入口")
    @PostMapping("/matchingProductionAdjust/{productionVersion}")
    public AjaxResult matchingProductionAdjust(@PathVariable("productionVersion") String productionVersion) {
        matchingProductionHandler.matchingProductionAdjust(productionVersion);
        return AjaxResult.success();
    }
}
