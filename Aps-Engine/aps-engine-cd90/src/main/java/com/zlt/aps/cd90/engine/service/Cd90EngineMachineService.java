package com.zlt.aps.cd90.engine.service;

import java.util.List;

import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;

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
}
