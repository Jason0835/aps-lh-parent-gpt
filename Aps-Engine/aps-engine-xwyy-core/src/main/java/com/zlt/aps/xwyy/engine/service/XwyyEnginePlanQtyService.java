package com.zlt.aps.xwyy.engine.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.xwyy.engine.vo.XwyyAssistRequirement;
import com.zlt.aps.xwyy.engine.vo.XwyyOriginalLineSpec;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;
import com.zlt.aps.xwyy.engine.vo.XwyyStockVo;

/**
 * 纤维压延计划量信息处理服务接口
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:30:42
 * @Version 1.0
 */
public interface XwyyEnginePlanQtyService {
	/**
	 * 计算计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:31:01
	 * @param scheduleDate 排产日期
	 * @Param scheduleList 排产记录
	 * @param assistMap       外协规格需求
	 * @param originalLineMap 原线配置
	 * @Param stockLossRate 库存损耗率
	 * @param breadth           幅宽
	 * @param isProductionStage 仅对投产阶段规格排产
     * @param isBreak 是否计算破大卷
	 * @Return
	 */
	void calculateSchedulePlanQty(Date scheduleDate, List<XwyyScheduleResultVo> scheduleList,
			Map<String, XwyyAssistRequirement> assistMap, Map<String, XwyyOriginalLineSpec> originalLineMap,
			BigDecimal stockLossRate, Double breadth, boolean isProductionStage, boolean isBreak);

	/**
	 * 获取指定日期的半部件库存量
	 * 
	 * @param scheduleDate  库存日期
	 * @param isAssistStock 是否使用外厂需求的库存
	 * @return key：大卷编号，value：库存量
	 */
	Map<String, Double> getStockQtyMap(Date scheduleDate, BigDecimal stockLossRate, boolean isAssistStock);

	/**
	 * 计算成型可供时长
	 * 
	 * @param resultVo 排产结果
	 * @param stockVo  库存信息
	 * @return
	 */
	public BigDecimal caculateSuppliyTime(XwyyScheduleResultVo resultVo, XwyyStockVo stockVo);

	/**
	 * 获取排产日的16点半部件库存
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param breadth 幅宽
	 * @Param isProductionStage 仅对投产阶段规格排产
	 * @param isAssistStock 是否使用外厂需求的库存
	 * @return key：帘布编号，value：库存量
	 */
	public Map<String, XwyyStockVo> getStockMap(Date scheduleDate, BigDecimal stockLossRate, double breadth,
			boolean isProductionStage, boolean isAssistStock);
}
