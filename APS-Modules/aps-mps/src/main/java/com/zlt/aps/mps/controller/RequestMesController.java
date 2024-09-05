package com.zlt.aps.mps.controller;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.engine.service.FactoryService;
import com.zlt.aps.common.engine.utils.DateUtil;
import com.zlt.aps.mps.common.MpsSyncHandle;
import com.zlt.aps.mps.common.SyncKeyEnum;
import com.zlt.sync.handle.SyncDataHandle;
import com.zlt.sync.povo.SyncParamsVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = "向MES主动发送请求同步数据")
@RestController
@RequestMapping("/request/mes/sync")
public class RequestMesController extends SyncDataHandle {
	@Resource
	private MpsSyncHandle mpsSyncHandle;
	@Autowired
	private FactoryService factoryService;

	@ApiOperation("胎胚月结库存同步接口")
	@PostMapping("/cxSyncMonthStock/{queryDate}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "queryDate", dataType = "string", value = "查询日期，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult cxSyncMonthStock(@PathVariable("queryDate") String queryDate) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.EMBRYO_MONTH_SYNC.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		json.put("queryDate", queryDate);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		this.syncRequest(paramsVO); // 发送请求
		return AjaxResult.success();
	}

	@ApiOperation("胎胚不良数同步接口")
	@PostMapping("/cxTireBadNum/{queryDate}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "queryDate", dataType = "string", value = "查询日期，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult cxTireBadNum(@PathVariable("queryDate") String queryDate) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.EMBRYO_BAD_QUANTITY.getDescription());
		JSONObject json = new JSONObject();
		json.put("queryDate", queryDate);
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		this.syncRequest(paramsVO); // 发送请求
		return AjaxResult.success();
	}

	@ApiOperation("成型8-12点的完成量接口")
	@PostMapping("/cxFinish/{statDate}/{endDate}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "statDate", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endDate", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult cxFinish(@PathVariable("statDate") String statDate, @PathVariable("endDate") String endDate) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.FORMING8_12_COMPLETE, statDate, endDate);
		this.syncRequest(paramsVO); // 发送请求
		return AjaxResult.success();
	}

	@ApiOperation("半部件代号与SAP物料品号对应关系同步接口")
	@PostMapping("/syncSapMaterial")
	public AjaxResult syncSapMaterial() {
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.HALF_PART_SAP.getDescription());
		JSONObject json = new JSONObject();
		paramsVO.setParams(json);
		this.syncRequest(paramsVO); // 发送请求
		return AjaxResult.success();
	}

	@ApiOperation("成品(硫化)库存同步")
	@PostMapping("/lhSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult lhSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.FINISHED_STOCK_SYNC.getDescription(),
				startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎胚(成型)库存同步")
	@PostMapping("/cxSyncStock/{queryDate}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "queryDate", dataType = "string", value = "查询日期，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult cxSyncStock(@PathVariable("queryDate") String queryDate) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.EMBRYO_STOCK_SYNC.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		json.put("queryDate", queryDate);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);

		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎面库存同步")
	@PostMapping("/tmSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query") })
	public AjaxResult tmSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.TREAD_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎侧库存同步")
	@PostMapping("/tcSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult tcSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.SIDEWALL_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("内衬库存同步")
	@PostMapping("/ncSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult ncSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.LINING_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎圈库存同步")
	@PostMapping("/tqSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult tqSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.BEAD_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("钢丝圈库存同步")
	@PostMapping("/gsqSyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult gsqSyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.STEEL_WIRE_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("15度裁断库存同步")
	@PostMapping("/cd15SyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult cd15SyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.ADJUDI15_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("90度裁断库存同步")
	@PostMapping("/cd90SyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult cd90SyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.ADJUDI90_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("钢带压延库存同步")
	@PostMapping("/gdyySyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult gdyySyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.GDYY_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("纤维压延库存同步")
	@PostMapping("/xwyySyncStock/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd", paramType = "query"), })
	public AjaxResult xwyySyncStock(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createStockSyncParamsVO(SyncKeyEnum.XWYY_STOCK.getDescription(), startTime,
				endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("成型日完成量")
	@PostMapping("/cxDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult cxDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.CX_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("硫化日完成量")
	@PostMapping("/lhDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult lhDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.LH_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎面日完成量")
	@PostMapping("/tmDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult tmDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.TM_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎侧日完成量")
	@PostMapping("/tcDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult tcDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.TC_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("胎圈日完成量")
	@PostMapping("/tqDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult tqDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.TQ_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("内衬日完成量")
	@PostMapping("/ncDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult ncDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.NC_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("钢丝圈日完成量")
	@PostMapping("/gsqDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult gsqDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.GSQ_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("15度裁断日完成量")
	@PostMapping("/cd15DayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult cd15DayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.CD15_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("90度裁断日完成量")
	@PostMapping("/cd90DayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult cd90DayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.CD90_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("钢带压延日完成量")
	@PostMapping("/gdyyDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult gdyyDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.GDYY_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("纤维压延日完成量")
	@PostMapping("/xwyyDayFinish/{startTime}/{endTime}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"), })
	public AjaxResult xwyyDayFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime) {
		SyncParamsVO paramsVO = this.createFinishSyncParamsVO(SyncKeyEnum.XWYY_DAY_COMPLETE, startTime, endTime);
		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	@ApiOperation("成型机台当前生产规格接口")
	@PostMapping("/cxProductionSpec")
	@ApiImplicitParams({})
	public AjaxResult cxProductionSpec() {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.CX_PRODUCTION_SPEC.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		this.syncRequest(paramsVO); // 发送请求，进行生产规格同步
		return AjaxResult.success();
	}

	@ApiOperation("硫化机台当前生产规格接口")
	@PostMapping("/lhInProductionSpec")
	@ApiImplicitParams({})
	public AjaxResult lhInProductionSpec() {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.LH_IN_PRODUCTION_SPEC.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		this.syncRequest(paramsVO); // 发送请求，进行生产规格同步
		return AjaxResult.success();
	}

	@ApiOperation("成型中夜班完成量接口")
	@PostMapping("/cxMidNightFinish/{startTime}/{endTime}/{queryCode}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "queryCode", dataType = "string", value = "查询代号", paramType = "query"), })
	public AjaxResult cxMidNightFinish(@PathVariable("startTime") String startTime,
			@PathVariable("endTime") String endTime, @PathVariable("queryCode") String queryCode) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.CX_MID_NIGHT_FINISH.getDescription());
		JSONObject json = new JSONObject();
		json.put("startDate", startTime);
		json.put("endDate", endTime);
		json.put("queryCode", queryCode);
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);

		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}
	
	@ApiOperation("各工序班次完成量接口")
	@PostMapping("/classFinishQty/{procedureCode}/{startTime}/{endTime}/{queryCode}")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "startTime", dataType = "string", value = "开始时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "endTime", dataType = "string", value = "结束时间，格式：yyyy-MM-dd hh24:mi:ss", paramType = "query"),
			@ApiImplicitParam(name = "queryCode", dataType = "string", value = "查询代号", paramType = "query"),
			@ApiImplicitParam(name = "procedureCode", dataType = "string", value = "工序编号", paramType = "query") })
	public AjaxResult classFinishQty(@PathVariable("procedureCode") String procedureCode,
			@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime,
			@PathVariable("queryCode") String queryCode) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.CLASS_FINISH_QTY.getDescription());
		JSONObject json = new JSONObject();
		json.put("startDate", startTime);
		json.put("endDate", endTime);
		json.put("queryCode", queryCode);
		json.put("procedureCode", procedureCode);
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);

		this.syncRequest(paramsVO); // 发送请求，进行库存同步
		return AjaxResult.success();
	}

	/**
	 * 构建库存同步的 SyncParamsVO
	 * 
	 * @param syncKey   同步key
	 * @param startTime 开始时间
	 * @param endTime   结束时间
	 * @return
	 */
	private SyncParamsVO createStockSyncParamsVO(String syncKey, String startTime, String endTime) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(syncKey);
		JSONObject json = new JSONObject();
		json.put("startDate", startTime);
		json.put("endDate", endTime);
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		return paramsVO;
	}

	/**
	 * 构建完成量同步接口参数
	 * 
	 * @param statDate 开始时间
	 * @param endDate  结束时间
	 * @param syncKey
	 * @return
	 */
	private SyncParamsVO createFinishSyncParamsVO(SyncKeyEnum syncKey, String statDate, String endDate) {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(syncKey.getDescription());
		JSONObject json = new JSONObject();
		json.put("startDate", statDate);
		json.put("endDate", endDate);
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		return paramsVO;
	}
	
	@ApiOperation("同步15度裁断线边库库存")
	@PostMapping("/syncCd15LineSideStock")
	public AjaxResult syncCd15LineSideStock() {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.ADJUDI15_LINESIDE_STOCK.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		json.put("endDate", DateUtil.nowDate());
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		return this.syncRequest(paramsVO);
	}
	
	@ApiOperation("同步90度裁断线边库库存")
	@PostMapping("/syncCd90LineSideStock")
	public AjaxResult syncCd90LineSideStock() {
		String factoryCode = factoryService.getFactoryCode();
		String companyCode = factoryService.getCompanyCode();
		SyncParamsVO paramsVO = new SyncParamsVO();
		paramsVO.setSyncKey(SyncKeyEnum.ADJUDI90_LINESIDE_STOCK.getDescription());
		JSONObject json = new JSONObject();
		json.put("factoryCode", factoryCode);
		json.put("companyCode", companyCode);
		json.put("endDate", DateUtil.nowDate());
		paramsVO.setParams(json);
		paramsVO.setFactoryCode(factoryCode);
		paramsVO.setCompanyCode(companyCode);
		return this.syncRequest(paramsVO);
	}

	@Override
	public void asyncResult(AjaxResult ajaxResult) {
		mpsSyncHandle.asyncResult(ajaxResult, this);
	}
}
