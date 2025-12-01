package com.zlt.mix.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.api.domain.dto.ScheduleReportDto;

/**
 * 日计划每日报表统计
 * 
 * @author hakimryan
 *
 */
public interface ScheduleReportMapper {

	/**
	 * 查询日计划每日报表
	 * 
	 * @param dto
	 * @return
	 */
	List<ScheduleReportDto> selectScheduleReportList(@Param("dto") ScheduleReportDto dto);
}
