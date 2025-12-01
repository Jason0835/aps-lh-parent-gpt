package com.zlt.aps.xwyy.engine.service;

import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;

import java.math.BigDecimal;
import java.util.List;

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

	/**
	 * 根据产能选机台
	 *
	 * @param scheduleList 排程数据
     * @param machineQuataHour 机台产能时长
	 */
	void chooseMachineByCapacity(List<XwyyScheduleResultVo> scheduleList, BigDecimal machineQuataHour);
}
