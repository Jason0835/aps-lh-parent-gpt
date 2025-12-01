package com.zlt.mix.schedule.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleReportDto;

/**
 * 日计划每日报表统计
 * @author hakimryan
 *
 */
@FeignClient(contextId = "IScheduleReportService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IScheduleReportService {

	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/scheduleReport/selectScheduleReportList")
	TableDataInfo selectScheduleReportList(@RequestBody ScheduleReportDto scheduleReportDto);

	/**
	 * 导出排产日报表
	 * 
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/scheduleReport/exportScheduleReportList")
	List<ScheduleReportDto> export(@RequestBody ScheduleReportDto scheduleReportDto);
}
