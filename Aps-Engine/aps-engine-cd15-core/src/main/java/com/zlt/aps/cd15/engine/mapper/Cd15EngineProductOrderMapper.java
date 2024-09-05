package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

/**
 * 15度裁断生产顺序mapper
 */
public interface Cd15EngineProductOrderMapper {

	/**
	 * 根据参数获取15度裁断的排程信息
	 * 
	 * @Param params 需要重算的排程结果过滤条件
	 * @Return
	 */
	List<Cd15ScheduleResultVo> selectCd15ScheduleList(@Param("params") Cd15ScheduleResult params);

	/**
	 * 批量更新排程结果的生产顺序
	 * 
	 * @param scheduleResultList
	 */
	int updatCd15ScheduleResultOrder(@Param("scheduleResultList") List<Cd15ScheduleResultVo> scheduleResultList);
}
