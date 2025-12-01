package com.zlt.mix.sync.controller;

import javax.annotation.Resource;

import com.zlt.mix.common.core.constant.ZltConstant;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.sync.service.KettleService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "向MES发送请求同步数据")
@RestController
@RequestMapping("/request/mes/sync")
public class RequestMesController {
	@Resource
	private KettleService kettleService;
	@Resource
	private RedisTemplate redisTemplate;
	
	@ApiOperation("密炼配方同步接口")
	@PostMapping("/syncMesPmtRecipe")
	public AjaxResult syncMesPmtRecipe() {
		kettleService.excuteKettle("PmtRecipe.ktr");
		// 清除配方机台缓存 start
		redisTemplate.delete(ZltConstant.CACHE_RECIPE_MACHINE);
		// 清除配方机台缓存 end
		return AjaxResult.success();
	}

	@ApiOperation("物料信息同步接口")
	@PostMapping("/syncBasMaterial")
	public AjaxResult syncBasMaterial() {
		kettleService.excuteKettle("BasMaterial.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("不合格胶库存同步接口")
	@PostMapping("/syncBghPpmRubberStorage")
	public AjaxResult syncBghPpmRubberStorage() {
		kettleService.excuteKettle("PpmRubberStorage_bgh.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("返回胶库存同步接口")
	@PostMapping("/syncFhPpmRubberStorage")
	public AjaxResult syncFhPpmRubberStorage() {
		kettleService.excuteKettle("PpmRubberStorage_fh.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("终炼胶库存同步接口")
	@PostMapping("/syncZlPpmRubberStorage")
	public AjaxResult syncZlPpmRubberStorage() {
		kettleService.excuteKettle("PpmRubberStorage_zl.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("母炼胶库存同步接口")
	@PostMapping("/syncMlPpmRubberStorage")
	public AjaxResult syncMlPpmRubberStorage() {
		kettleService.excuteKettle("PpmRubberStorage_ml.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("硫磺辅料库存同步接口")
	@PostMapping("/syncLhflPpmRubberStorage")
	public AjaxResult syncLhflPpmRubberStorage() {
		kettleService.excuteKettle("PpmRubberStorage_lhfl.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("胶料计划完成量同步接口")
	@PostMapping("/syncGlueFinish")
	public AjaxResult syncGlueFinish() {
		kettleService.excuteKettle("PptPlan_finish.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("硫磺辅料计划完成量同步接口")
	@PostMapping("/syncLhflFinish")
	public AjaxResult syncLhflFinish() {
		kettleService.excuteKettle("PptPlan_finish_lhfl.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("8点到12点完成量同步接口")
	@PostMapping("/syncGluePartFinish")
	public AjaxResult syncGluePartFinish() {
		kettleService.excuteKettle("PptPlan_part_finish.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("配方类型同步接口")
	@PostMapping("/syncRecipeType")
	public AjaxResult syncRecipeType() {
		kettleService.excuteKettle("PmtRecipeType.ktr");
		return AjaxResult.success();
	}

	@ApiOperation("胶料支领量同步转换")
	@PostMapping("/syncRubberStoreout")
	public AjaxResult syncRubberStoreout() {
		kettleService.excuteKettle("PpmRubberStoreout.ktr");
		return AjaxResult.success();
	}
	
}
