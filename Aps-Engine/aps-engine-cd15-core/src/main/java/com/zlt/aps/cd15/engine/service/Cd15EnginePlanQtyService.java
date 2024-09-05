package com.zlt.aps.cd15.engine.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.cd15.engine.vo.Cd15ScheduleResultVo;
import com.zlt.aps.cd15.engine.vo.Cd15StockVo;

/**
 * 15度裁断计划排产量计算服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-9 11:30:42
 * @Version 1.0
 */
public interface Cd15EnginePlanQtyService {
	/**
	 * 计算排产计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-9 11:31:01
	 * @Param scheduleDate 排产日期
	 * @Param scheduleList 排产记录
	 * @Param defaultLossRate 默认损耗率
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @Param crimpLength 卷曲长度
	 * @Param minRoundRollNum 最小取整卷数
	 */
	void calculateSchedulePlanQty(Date scheduleDate, List<Cd15ScheduleResultVo> scheduleList, String defaultLossRate,
			BigDecimal stockLossRate, boolean isProductionStage, BigDecimal crimpLength, BigDecimal minRoundRollNum);

	/**
	 * 计算成型可供时长
	 * 
	 * @param resultVo 排产结果
	 * @param stockVo  库存信息
	 * @return
	 */
	BigDecimal caculateSuppliyTime(Cd15ScheduleResultVo resultVo, Cd15StockVo stockVo);

	/**
	 * 处理排产计划量，防止出现二次投产
	 * 
	 * @param resultVo 排产信息
	 * @param stockQty 库存量
	 */
	void handleSecondaryProduct(Cd15ScheduleResultVo resultVo, BigDecimal stockQty);

	/**
	 * 获取排产日的16点半部件库存信息（包含成型机台信息相关）
	 * 
	 * @param scheduleDate 排产日期
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @Param stockLossRate 库存损耗率
	 * @return key：帘布编号，value：库存信息
	 */
	Map<String, Cd15StockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate, boolean isProductionStage);
}
