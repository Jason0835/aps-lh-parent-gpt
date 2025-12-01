package com.zlt.mix.schedule.service;

import java.util.List;

import com.zlt.mix.schedule.api.domain.dto.ScheduleReportDto;

/**
 * 日计划每日报表统计
 * 
 * @author hakimryan
 *
 */
public interface ScheduleReportService {
	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	List<ScheduleReportDto> selectScheduleReportList(ScheduleReportDto scheduleReportDto);
}
