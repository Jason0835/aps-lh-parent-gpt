package com.zlt.aps.itf.scm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * SCM接口
 *
 * @author zlt
 * @since 2025/12/10
 */
@Api(tags = "SCM接口")
@RestController
@RequestMapping("/scm")
public class ScmItfController {
	@Autowired
	private ScmItfService scmItfService;

	/**
	 * 同步已计划未发货数据
	 */
	@ApiOperation("同步已计划未发货数据")
	@PostMapping("/syncPlanedNotShipList")
	public AjaxResult syncPlanedNotShipList(@RequestBody SyncPlanedNotShipParamVo planedNotShipParamVo) {
		return scmItfService.syncPlanedNotShipList(planedNotShipParamVo);
	}
}
