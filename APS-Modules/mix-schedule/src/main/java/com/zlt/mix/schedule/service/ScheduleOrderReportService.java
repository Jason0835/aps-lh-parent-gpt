package com.zlt.mix.schedule.service;

import java.util.List;

import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;

/**
 * 各工序工单完成统计报表服务接口
 * 
 * @author hakimryan
 *
 */
public interface ScheduleOrderReportService {
	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	List<ScheduleOrderReportDto> selectScheduleReportList(ScheduleOrderReportDto scheduleReportDto);
}
