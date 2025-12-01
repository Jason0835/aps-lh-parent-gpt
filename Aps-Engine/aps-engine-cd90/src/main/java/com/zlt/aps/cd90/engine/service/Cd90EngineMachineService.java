package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;

import java.util.List;

/**
 * 90度裁断生产线服务接口
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-14 11:24:00
 * @Version 1.0
 */
public interface Cd90EngineMachineService {
	
	/**
	 * 为90度裁断排程安排生产线
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:21:43
	 * @Param
	 * @Return
	 */
	void scheduleMachine(List<Cd90ScheduleResultVo> scheduleList);

	/**
	 * 根据机台产能选择机台
	 *
	 * @param scheduleList 排程结果列表
	 */
	void chooseMachineByCapacity(List<Cd90ScheduleResultVo> scheduleList);
}
