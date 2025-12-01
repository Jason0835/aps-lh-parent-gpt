package com.zlt.mix.schedule.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zlt.mix.schedule.api.domain.dto.ScheduleOrderReportDto;
import com.zlt.mix.schedule.mapper.ScheduleOrderReportMapper;
import com.zlt.mix.schedule.service.ScheduleOrderReportService;

/**
 * 各工序工单完成统计报表
 * 
 * @author hakimryan
 *
 */
@Service
public class ScheduleOrderReportServiceImpl implements ScheduleOrderReportService {
	@Resource
	private ScheduleOrderReportMapper scheduleOrderReportMapper;

	/**
	 * 获取排产日报表
	 *
	 * @param scheduleReportDto
	 * @return
	 */
	@Override
	public List<ScheduleOrderReportDto> selectScheduleReportList(ScheduleOrderReportDto scheduleReportDto) {
		if (scheduleReportDto == null || scheduleReportDto.getQueryStartDate() == null
				|| scheduleReportDto.getQueryEndDate() == null) {
			return new ArrayList<>(); // 没有排产日期直接返回空列表
		}
		List<ScheduleOrderReportDto> result = scheduleOrderReportMapper.selectScheduleReportList(scheduleReportDto);
		return result;
	}
}
