package com.zlt.mix.schedule.engine.service.glueschedule;

import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 生产补量服务
 *
 */
public interface GlueScheduleEngineSupplementService {
	/**
	 * 剩余计划量补量
	 *
	 * @param scheduleDate
	 * @param mixArea
	 * @param baseScheduleList         排产列表
	 * @param glueStock                库存列表
	 * @param factoryRequireMap        分厂需求列表
	 * @param params                   排产参数
	 * @param mixingTimeMap            胶料间隔时间
	 * @param slPriorityMap            塑胶优先级映射
	 * @param needSlScheduleMap        需要塑胶排产后的优先排产的记录
	 * @param latestScheduleList       查询昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap 炼胶优先配置
	 */
	void surplusQtySuppliment(Date scheduleDate, String mixArea, List<GlueScheduleResultVo> baseScheduleList,
			GlueScheduleStockPool glueStock, Map<String, GlueFactoryRequireVo> factoryRequireMap,
			Map<String, String> params, Map<String, Long> mixingTimeMap, Map<String, List<GlueScheduleResultVo>> slPriorityMap,
							  Map<String, List<GlueScheduleResultVo>> needSlScheduleMap,
							  List<GlueScheduleResultVo> latestScheduleList,
							  Map<String, String> mixingPriorityProductMap,
							  List<MesPmtRecipeVo> mesPmtRecipeList);
}
