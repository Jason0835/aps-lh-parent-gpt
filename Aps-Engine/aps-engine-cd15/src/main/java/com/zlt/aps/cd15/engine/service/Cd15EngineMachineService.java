package com.zlt.aps.cd15.engine.service;

import java.util.List;

import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

/**
 * 15度裁断生产线服务接口
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-9 11:24:00
 * @Version 1.0
 */
public interface Cd15EngineMachineService {
	
	/**
	 * 为15度裁断排程安排生产线
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 11:08:18
	 * @param scheduleList	排产记录
	 */
	void scheduleMachine(List<Cd15ScheduleResultVo> scheduleList);
}
