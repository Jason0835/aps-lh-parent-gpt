package com.zlt.aps.cd90.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;

/**
 * 90度裁断生产顺序mapper
 */
public interface Cd90EngineProductOrderMapper {

	/**
	 * 根据参数获取90度裁断的排程信息
	 * 
	 * @Param params 需要重算的排程结果过滤条件
	 * @Return
	 */
	List<Cd90ScheduleResultVo> selectCd90ScheduleList(@Param("params") Cd90ScheduleResult params);

	/**
	 * 批量更新排程结果的生产顺序
	 * 
	 * @param scheduleResultList
	 */
	int updatCd90ScheduleResultOrder(@Param("scheduleResultList") List<Cd90ScheduleResultVo> scheduleResultList);
}
