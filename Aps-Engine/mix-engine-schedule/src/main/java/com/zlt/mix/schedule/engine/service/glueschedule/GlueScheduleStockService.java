package com.zlt.mix.schedule.engine.service.glueschedule;

import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;

import java.util.Date;
import java.util.Map;

/**
 * 胶料排程库存服务
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleStockService {

	/**
	 * 初始话库存池
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @return
	 */
	GlueScheduleStockPool buildStockPool(Date scheduleDate, String mixArea, Map<String, String> reserveGlueRecipeMap);

	/**
	 * 只加载指定类型物料的库存池
	 * 
	 * @param scheduleDate 排产日
	 * @param mixArea      密炼区
	 * @param majorType    物料类型
	 * @return
	 */
	GlueScheduleStockPool buildStockPool(Date scheduleDate, String mixArea, Map<String, String> reserveGlueRecipeMap, String... majorType);
}
