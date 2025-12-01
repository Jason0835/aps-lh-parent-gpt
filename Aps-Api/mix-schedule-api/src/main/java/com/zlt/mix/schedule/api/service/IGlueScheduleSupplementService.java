package com.zlt.mix.schedule.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;

/**
 * 胶料补量统计报表
 * @author hakimryan
 *
 */
@FeignClient(contextId = "IGlueScheduleSupplementService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueScheduleSupplementService {

	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/glueScheduleSupplement/pageGlueScheduleSupplement")
	TableDataInfo pageGlueScheduleSupplement(@RequestBody GlueScheduleSupplement glueScheduleSupplement);

	/**
	 * 导出排产日报表
	 * 
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/glueScheduleSupplement/exportGlueScheduleSupplement")
	List<GlueScheduleSupplement> export(@RequestBody GlueScheduleSupplement glueScheduleSupplement);
}
