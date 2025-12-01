package com.zlt.mix.schedule.service;

import java.util.List;

import com.zlt.mix.schedule.api.domain.dto.ScheduleClassesReportDto;

/**
 * 日计划每日报表统计
 * 
 * @author hakimryan
 *
 */
public interface ScheduleClassesReportService {
	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	List<ScheduleClassesReportDto> selectScheduleReportList(ScheduleClassesReportDto scheduleReportDto);
	
	/**
	 * 获取排产日报表统计信息
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	ScheduleClassesReportDto getScheduleReportSummary(ScheduleClassesReportDto scheduleReportDto);
}
