package com.zlt.mix.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.api.domain.dto.ScheduleClassesReportDto;

/**
 * 日计划班次报表统计
 * 
 * @author hakimryan
 *
 */
public interface ScheduleClassesReportMapper {

	/**
	 * 查询日计划每日报表
	 * 
	 * @param dto
	 * @return
	 */
	List<ScheduleClassesReportDto> selectScheduleReportList(@Param("dto") ScheduleClassesReportDto dto);

	/**
	 * 查询日计划每日报表统计信息
	 * 
	 * @param dto
	 * @return
	 */
	ScheduleClassesReportDto getScheduleReportSummary(@Param("dto") ScheduleClassesReportDto scheduleReportDto);
}
