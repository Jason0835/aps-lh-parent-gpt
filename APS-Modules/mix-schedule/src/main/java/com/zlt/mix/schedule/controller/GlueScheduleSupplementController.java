package com.zlt.mix.schedule.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.schedule.service.GlueScheduleSupplementService;

import io.swagger.annotations.ApiOperation;

/**
 * 终炼/母炼日计划排程Controller
 *
 */
@RestController
@RequestMapping("/glueScheduleSupplement")
public class GlueScheduleSupplementController extends BaseController {
	@Autowired
	private GlueScheduleSupplementService glueScheduleSupplementService;

	/**
	 * 计算终炼/母炼日计划补量列表
	 */
	@ApiOperation("查询胶料计划补量列表")
	@PostMapping("/pageGlueScheduleSupplement")
	public TableDataInfo pageGlueScheduleSupplement(@RequestBody GlueScheduleSupplement glueScheduleSupplement) {
		List<GlueScheduleSupplement> list = glueScheduleSupplementService
				.listGlueScheduleSupplement(glueScheduleSupplement);
		return getDataTable(list);
	}

	@ApiOperation("导出胶料计划补量列表")
	@PostMapping("/exportGlueScheduleSupplement")
	public List<GlueScheduleSupplement> saveSupplement(@RequestBody GlueScheduleSupplement glueScheduleSupplement) {
		return glueScheduleSupplementService.listGlueScheduleSupplement(glueScheduleSupplement);
	}
}
