package com.zlt.mix.schedule.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;

/**
 * 各工序工单完成统计报表
 * @author hakimryan
 *
 */
@FeignClient(contextId = "IScheduleOrderReportService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IScheduleOrderReportService {

	/**
	 * 获取排产日报表
	 *
	 * @param scheduleOrderReportDto
	 * @return
	 */
	@PostMapping("/scheduleOrderReport/selectScheduleReportList")
	TableDataInfo selectScheduleReportList(@RequestBody ScheduleOrderReportDto scheduleOrderReportDto);

	/**
	 * 导出排产日报表
	 * 
	 * @param scheduleOrderReportDto
	 * @return
	 */
	@PostMapping("/scheduleOrderReport/exportScheduleReportList")
	List<ScheduleOrderReportDto> export(@RequestBody ScheduleOrderReportDto scheduleOrderReportDto);
}
