package com.zlt.aps.mps.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mps.service.MonthPlanStatisticsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "数据汇总统计服务")
@RestController
@RequestMapping("/mps/statistics")
@Slf4j
public class StatisticsController {
	@Autowired
	private MonthPlanStatisticsService monthPlanStatisticsService;

	@ApiOperation("月度计划实际超欠产统计接口")
	@PostMapping("/monthPlan/actualOverProduction")
	public AjaxResult monthPlanActualOverProduction() {
		log.debug("test--------------------------------statistics actualOverProduction---------------------");
		return monthPlanStatisticsService.actualOverProduction();
	}
}
