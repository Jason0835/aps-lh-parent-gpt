package com.ruoyi.job.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 月计划汇总对外暴露接口
 * 
 * @author Gim
 */
@FeignClient(contextId = "IRequestMesService", value = ServiceNameConstants.APS_MPS_SERVICE)
public interface IRequestMesService {

	String prefix = "/request/mes/sync";

	/**
	 * 成型8-12点的完成量接口
	 * 
	 * @param statDate 开始时间，格式：yyyy-MM-dd HH:mm:ss
	 * @param endDate  结束时间，格式：yyyy-MM-dd HH:mm:ss
	 */
	@PostMapping(value = prefix + "/cxFinish/{statDate}/{endDate}")
	AjaxResult sendCxFinish(@PathVariable("statDate") String statDate, @PathVariable("endDate") String endDate);

	/**
	 * 半部件代号与SAP物料品号对应关系同步接口
	 */
	@PostMapping(value = prefix + "/syncSapMaterial")
	AjaxResult syncSapMaterial();

	/**
	 * 胎胚月结库存同步接口
	 * 
	 * @param queryDate 查询日期
	 */
	@PostMapping(value = prefix + "/cxSyncMonthStock/{queryDate}")
	AjaxResult cxSyncMonthStock(@PathVariable("queryDate") String queryDate);

	/**
	 * 胎胚不良数同步接口
	 * 
	 * @param queryDate 查询日期
	 */
	@PostMapping(value = prefix + "/cxTireBadNum/{queryDate}")
	AjaxResult cxTireBadNum(@PathVariable("queryDate") String queryDate);

	/**
	 * 成品(硫化)库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/lhSyncStock/{startTime}/{endTime}")
	AjaxResult lhSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎胚(成型)库存同步
	 * 
	 * @param queryDate 开始时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/cxSyncStock/{queryDate}")
	AjaxResult cxSyncStock(@PathVariable("queryDate") String queryDate);

	/**
	 * 胎面库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/tmSyncStock/{startTime}/{endTime}")
	AjaxResult tmSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎侧库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/tcSyncStock/{startTime}/{endTime}")
	AjaxResult tcSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 内衬库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/ncSyncStock/{startTime}/{endTime}")
	AjaxResult ncSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎圈库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/tqSyncStock/{startTime}/{endTime}")
	AjaxResult tqSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 钢丝圈库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/gsqSyncStock/{startTime}/{endTime}")
	AjaxResult gsqSyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 15度裁断库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/cd15SyncStock/{startTime}/{endTime}")
	AjaxResult cd15SyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 90度裁断库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/cd90SyncStock/{startTime}/{endTime}")
	AjaxResult cd90SyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 钢带压延库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/gdyySyncStock/{startTime}/{endTime}")
	AjaxResult gdyySyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 纤维压延延库存同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd
	 * @param endTime   结束时间，格式：yyyy-MM-dd
	 */
	@PostMapping(value = prefix + "/xwyySyncStock/{startTime}/{endTime}")
	AjaxResult xwyySyncStock(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 成型日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/cxDayFinish/{startTime}/{endTime}")
	AjaxResult cxDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 硫化日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/lhDayFinish/{startTime}/{endTime}")
	AjaxResult lhDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎面日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/tmDayFinish/{startTime}/{endTime}")
	AjaxResult tmDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎侧日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/tcDayFinish/{startTime}/{endTime}")
	AjaxResult tcDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 内衬日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/ncDayFinish/{startTime}/{endTime}")
	AjaxResult ncDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 钢丝圈日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/gsqDayFinish/{startTime}/{endTime}")
	AjaxResult gsqDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 胎圈日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/tqDayFinish/{startTime}/{endTime}")
	AjaxResult tqDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 15度裁断日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/cd15DayFinish/{startTime}/{endTime}")
	AjaxResult cd15DayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 90d度裁断日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/cd90DayFinish/{startTime}/{endTime}")
	AjaxResult cd90DayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 钢带压延日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/gdyyDayFinish/{startTime}/{endTime}")
	AjaxResult gdyyDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 纤维压延日完成量同步
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 */
	@PostMapping(value = prefix + "/xwyyDayFinish/{startTime}/{endTime}")
	AjaxResult xwyyDayFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime);

	/**
	 * 成型机台当前生产规格接口
	 * 
	 */
	@PostMapping(value = prefix + "/cxProductionSpec")
	AjaxResult cxProductionSpec();

	/**
	 * 成型中夜班完成量接口
	 * 
	 * @param startTime 开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime   结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param queryCode 查询代号
	 */
	@PostMapping(value = prefix + "/cxMidNightFinish/{startTime}/{endTime}/{queryCode}")
	AjaxResult cxMidNightFinish(@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime,
			@PathVariable("queryCode") String queryCode);

	/**
	 * 硫化机台当前生产规格接口
	 * 
	 */
	@PostMapping(value = prefix + "/lhInProductionSpec")
	AjaxResult lhInProductionSpec();

	/**
	 * 各工序班次完成量接口
	 * 
	 * @param procedureCode 工序编号
	 * @param startTime     开始时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param endTime       结束时间，格式：yyyy-MM-dd hh24:mi:ss
	 * @param queryCode     查询代号
	 */
	@PostMapping(value = prefix + "/classFinishQty/{procedureCode}/{startTime}/{endTime}/{queryCode}")
	AjaxResult classFinishQty(@PathVariable("procedureCode") String procedureCode,
			@PathVariable("startTime") String startTime, @PathVariable("endTime") String endTime,
			@PathVariable("queryCode") String queryCode);

	/**
	 * 同步15度裁断线边库库存
	 * @return
	 */
	@PostMapping(value = prefix + "/syncCd15LineSideStock")
	AjaxResult syncCd15LineSideStock();
	
	/**
	 * 同步90度裁断线边库库存
	 * @return
	 */
	@PostMapping(value = prefix + "/syncCd90LineSideStock")
	AjaxResult syncCd90LineSideStock();
}
