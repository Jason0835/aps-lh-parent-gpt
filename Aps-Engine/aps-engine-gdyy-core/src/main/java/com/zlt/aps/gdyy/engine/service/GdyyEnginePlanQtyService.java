package com.zlt.aps.gdyy.engine.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.gdyy.engine.vo.GdyyScheduleResultVo;

/**
 * 钢带压延库存信息处理服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-19 11:30:42
 * @Version 1.0
 */
public interface GdyyEnginePlanQtyService {
	/**
	 * 计算排产计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-19 11:31:01
	 * @Param scheduleList 排产记录
	 * @param scheduleDate    排产日期
	 * @param stockRatio      预留库存系数
	 * @param defaultLossRate 默认损耗率
	 * @Param stockLossRate 库存损耗率
	 * @param standardSize      标准长度
	 * @param isRollStock       是否按大卷计算库存
	 * @param breadth           幅宽
	 * @param isProductionStage 仅对投产阶段规格排产
	 * @Return
	 */
	void calculateSchedulePlanQty(Date scheduleDate, List<GdyyScheduleResultVo> scheduleList, String stockRatio,
			String defaultLossRate, BigDecimal stockLossRate, Double standardSize, boolean isRollStock, Double breadth,
			boolean isProductionStage);

	/**
	 * 获取排产日的16点半部件库存量
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param standardSize 默认标准长度
	 * @param isRollStock  是否按大卷计算库存
	 * @return key：钢带编号，value：库存量
	 */
	Map<String, Double> getStockQtyMap(Date scheduleDate, BigDecimal stockLossRate, Double standardSize,
			boolean isRollStock);
}
