package com.zlt.aps.itf.scm.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.scm.service.ScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
import com.zlt.aps.itf.vo.GoodsBoxVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
	@ApiOperation("已计划未发货订单接口（含排发货）")
	@PostMapping("/syncPlanedNotShipList")
	public AjaxResult syncPlanedNotShipList(@RequestBody SyncPlanedNotShipParamVo planedNotShipParamVo) {
		return scmItfService.syncPlanedNotShipList(planedNotShipParamVo);
	}

	/**
	 * 锁定订单池
	 */
	@ApiOperation("锁定订单池")
	@PostMapping("/lockSalesOrderPool")
	public AjaxResult lockSalesOrderPool(@RequestBody SyncPlanedNotShipParamVo planedNotShipParamVo) {
		return scmItfService.lockSalesOrderPool(planedNotShipParamVo);
	}

	/**
	 * 解锁订单池
	 *
	 * @param planedNotShipParamVo 解锁参数
	 * @return 结果集合
	 */
	@ApiOperation("解锁订单池")
	@PostMapping("/unlockSalesOrderPool")
	public AjaxResult unlockSalesOrderPool(@RequestBody SyncPlanedNotShipParamVo planedNotShipParamVo) {
		return scmItfService.unlockSalesOrderPool(planedNotShipParamVo);
	}


	/**
	 * 发货明细表同步接口
	 */
	@ApiOperation("发货明细表同步接口")
	@PostMapping("/syncOutShipDmdOrdList")
	public AjaxResult syncOutShipDmdOrdList(@RequestBody SyncPlanedNotShipParamVo syncOutShipDmdOrdVo) {
		return scmItfService.syncOutShipDmdOrdList(syncOutShipDmdOrdVo);
	}


	/**
	 * 月计划排程结果推送
	 */
	@ApiOperation("月计划排程结果推送")
	@PostMapping("/publicFacScheduleVersion")
	public AjaxResult publicFacScheduleVersion(@RequestBody List<SyncOutFacScheduleVersionVo> outFacScheduleVersionList) {
		return scmItfService.publicFacScheduleVersion(outFacScheduleVersionList);
	}

    /**
     * 同步区域/国家数据
     *
     * @return 结果
     */
    @ApiOperation("同步区域/国家数据")
    @PostMapping("/scm/syncArea")
    public AjaxResult syncArea() {
    	return scmItfService.syncArea();
    }

	/**
	 * 查询最新两个月的版本锁定情况
	 * @return 结果集合
	 */
	@ApiOperation("查询最新两个月的版本锁定情况")
	@PostMapping("/getMonthLock")
	public AjaxResult getMonthLock() {
		return scmItfService.getMonthLock();
	}

	/**
	 * 查询工厂装箱信息
	 * @return 结果
	 */
	@ApiOperation("查询工厂装箱信息")
	@PostMapping("/selectGoodsBox")
	public AjaxResult selectGoodsBox(@RequestBody GoodsBoxVo goodsBox) {
		return scmItfService.selectGoodsBox(goodsBox);
	}
}
