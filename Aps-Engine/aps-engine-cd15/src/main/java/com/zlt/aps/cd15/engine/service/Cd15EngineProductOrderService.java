package com.zlt.aps.cd15.engine.service;

import java.util.List;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;

/**
 * 15度裁断排产顺序服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-10 16:12:32
 * @Version 1.0
 */
public interface Cd15EngineProductOrderService {

	/**
	 * 根据钢压大卷、裁断角度排序，计算排产结果的生产顺序
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-10 16:13:28
	 * @Param
	 * @Return
	 */
	void calculateProduceOrder(List<Cd15ScheduleResultVo> scheduleList);

	/**
	 * 重算排产结果的生产顺序
	 * 
	 * @Param params 需要重算的排程结果过滤条件
	 * @Return
	 */
	void recalculateProduceOrder(Cd15ScheduleResult params);
}
