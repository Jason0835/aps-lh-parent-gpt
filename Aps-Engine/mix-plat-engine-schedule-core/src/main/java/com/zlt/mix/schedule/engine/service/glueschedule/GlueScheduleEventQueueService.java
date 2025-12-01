package com.zlt.mix.schedule.engine.service.glueschedule;

import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.vo.GlueFactoryRequireVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.GlueSpanReceiveVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 胶料排程排产事件列表服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleEventQueueService {
	/**
	 * 初始化并执行事件队列
	 *
	 * @param scheduleResult           排程结果列表
	 * @param glueStock                胶料库存
	 * @param startTime                开启时间
	 * @param params                   排程参数
	 * @param factoryRequireMap        分厂需求列表
	 * @param mixingTimeMap            胶料间隔时间
	 * @param mixingMinProductMap      炼胶单规格最小排产数
	 * @param slPriorityMap            塑胶优先级映射
	 * @param needSlScheduleMap        需要塑胶排产后的优先排产的记录
	 * @param latestScheduleList       查询昨日早班的最后一个排程计划
	 * @param mixingPriorityProductMap 炼胶优先配置
	 * @return
	 */
	void excuteEventQueue(List<GlueScheduleResultVo> scheduleResult, List<GlueSpanReceiveVo> glueSpanReceiveList,
			GlueScheduleStockPool glueStock, Date startTime, Map<String, String> params,
			Map<String, GlueFactoryRequireVo> factoryRequireMap, Map<String, Long> mixingTimeMap, Map<String, BigDecimal> mixingMinProductMap,
						  Map<String, List<GlueScheduleResultVo>> slPriorityMap,
						  Map<String, List<GlueScheduleResultVo>> needSlScheduleMap,
						  List<GlueScheduleResultVo> latestScheduleList,
						  Map<String, String> mixingPriorityProductMap,
						  List<MesPmtRecipeVo> mesPmtRecipeList);
}
