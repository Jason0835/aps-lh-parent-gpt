package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

import java.util.List;

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

	/**
	 * 处理一出二机台，如果记录的机台支持一出二，则只需要计算1#钢带的计划(清空2#钢带、计划量)，如果不支持，则复制一条记录，清空1#钢带、计划量
	 * @param scheduleList 排程结果列表
	 * @return 处理后结果
	 */
	List<Cd15ScheduleResultVo> handleOneOutTwoMachine(List<Cd15ScheduleResultVo> scheduleList);
	
	   /**
     * 根据机台产能选择机台
     *
     * @param scheduleList 排程结果列表
     */
    void chooseMachineByCapacity(List<Cd15ScheduleResultVo> scheduleList);

    /**
     * 根据机台产能选机台
     * 根据机台选规格
     *
     * @param scheduleList 排程结果
     */
    void chooseMachineByCapacity4Machine(List<Cd15ScheduleResultVo> scheduleList);
}
