package com.zlt.aps.cd15.engine.service;

import java.util.List;

import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

/**
 * 15度裁断排产均衡服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 15:26:28
 * @Version 1.0
 */
public interface Cd15EngineEquilibriumService {
	/**
	 * 均衡处理
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 15:27:55
	 * @Param scheduleList 15度裁断排产结果
	 * @Param planDifferenceRate 系统参数：中班总量和夜班总量差额百分比
	 * @Param supplyTimePass 系统参数：库存供应时长小时数
	 * @Param equalShareThreshold 系统参数：各班计划量均分阈值
	 * @Return
	 */
	void scheduleEquilibrium(List<Cd15ScheduleResultVo> scheduleList, String planDifferenceRate, String supplyTimePass,
			String equalShareThreshold);
}
