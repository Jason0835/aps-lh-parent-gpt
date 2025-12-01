package com.zlt.aps.cd15.engine.service;

import java.util.List;
import java.util.Map;

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
	 * @Param paramsMap 系统参数
	 * @Return
	 */
	void scheduleEquilibrium(List<Cd15ScheduleResultVo> scheduleList, Map<String, String> paramsMap);
}
