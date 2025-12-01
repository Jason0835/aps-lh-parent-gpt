package com.zlt.mix.schedule.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.schedule.api.domain.dto.ScheduleClassesReportDto;

/**
 * 日计划班次完成报表统计
 * @author hakimryan
 *
 */
@FeignClient(contextId = "IScheduleReportClassesService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IScheduleClassesReportService {

	/**
	 * 获取排产班次完成报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/scheduleClassesReport/selectScheduleReportList")
	TableDataInfo selectScheduleReportList(@RequestBody ScheduleClassesReportDto scheduleReportDto);

	/**
	 * 获取报表表头标题
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/scheduleClassesReport/getBaseTitle")
	String getBaseTitle(@RequestBody ScheduleClassesReportDto scheduleReportDto);

	/**
	 * 导出排产班次完成报表
	 * 
	 * @param scheduleReportDto
	 * @return
	 */
	@PostMapping("/scheduleClassesReport/exportScheduleReportList")
	List<ScheduleClassesReportDto> export(@RequestBody ScheduleClassesReportDto scheduleReportDto);
}
