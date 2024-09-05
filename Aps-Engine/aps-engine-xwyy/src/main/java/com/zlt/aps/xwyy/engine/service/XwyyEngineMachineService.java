package com.zlt.aps.xwyy.engine.service;

import java.util.List;

import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;

/**
 * 纤维压延生产线服务接口
 * @Description 
 * @Author hakimrayn
 * @Date 2021-7-22 11:10:34
 * @Version 1.0
 */
public interface XwyyEngineMachineService {
	
	/**
	 * 纤维压延排程安排生产线
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:21:43
	 * @Param
	 * @Return
	 */
	void scheduleMachine(List<XwyyScheduleResultVo> scheduleList);
}
