package com.zlt.mix.schedule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;

/**
 * 各工序工单完成统计报表
 * 
 * @author hakimryan
 *
 */
public interface ScheduleOrderReportMapper {

	/**
	 * 查询日计划每日报表
	 * 
	 * @param dto
	 * @return
	 */
	List<ScheduleOrderReportDto> selectScheduleReportList(@Param("dto") ScheduleOrderReportDto dto);
}
