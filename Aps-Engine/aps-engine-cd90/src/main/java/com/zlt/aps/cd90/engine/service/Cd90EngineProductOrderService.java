package com.zlt.aps.cd90.engine.service;

import java.util.List;

import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;

/**
 * 90度裁断排产顺序服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 16:12:32
 * @Version 1.0
 */
public interface Cd90EngineProductOrderService {

	/**
	 * 根据钢压大卷、可供时仓排序，计算排产结果的生产顺序
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 16:13:28
	 * @Param
	 * @Return
	 */
	void calculateProduceOrder(List<Cd90ScheduleResultVo> scheduleList);

	/**
	 * 重算排产结果的生产顺序
	 * 
	 * @Param params 需要重算的排程结果过滤条件
	 * @Return
	 */
	void recalculateProduceOrder(Cd90ScheduleResult params);
}
