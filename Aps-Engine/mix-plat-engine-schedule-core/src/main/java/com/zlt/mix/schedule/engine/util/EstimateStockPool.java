package com.zlt.mix.schedule.engine.util;

import java.math.BigDecimal;

/**
 * 预计库存池
 * @author hakimryan
 *
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.vo.EstimateStockVo;
import com.zlt.mix.schedule.engine.vo.GlueStockVo;

/**
 * 预库存池
 * 
 * @author hakimryan
 *
 */
public class EstimateStockPool {
	/**
	 * 库存列表，每个胶料的库存分开存放
	 */
	private Map<String, List<EstimateStockVo>> glueStock;

	private EstimateStockPool() {
		glueStock = new HashMap<>();
	}

	/**
	 * 初始化
	 * 
	 * @return
	 */
	public static EstimateStockPool init(Date scheduleDate, List<GlueStockVo> stockList) {
		EstimateStockPool instance = new EstimateStockPool();
		// 初始化的库存时间默认是中班开始时间
		Date currentTime = ShiftClassUtil.getShiftClassStartTime(scheduleDate, GlueEngineConstants.SHIFT_CLASS_MID);
		for (GlueStockVo stock : stockList) {
			instance.updateEstimateStock(stock.getGlue(), stock.getStockNum(), currentTime);
		}
		return instance;
	}

	/**
	 * 更新库存
	 * 
	 * @param glueCode    胶料号
	 * @param stockNum    库存数，可以是负数
	 * @param currentTime 当前时间
	 */
	public void updateEstimateStock(String glueCode, BigDecimal stockNum, Date currentTime) {
		if (glueCode == null || stockNum == null || currentTime == null) {
			return;
		}
		EstimateStockVo stock = new EstimateStockVo();
		stock.setGlueCode(glueCode);
		stock.setStockNum(stockNum);
		stock.setUpdateTime(currentTime);
		List<EstimateStockVo> estimateStock = this.getEstimateStock(glueCode);
		int index = 0;
		for (int size = estimateStock.size(); index < size; index++) {
			EstimateStockVo tempStock = estimateStock.get(index);
			if (tempStock.getUpdateTime().compareTo(currentTime) > 0) {
				break;
			}
		}
		estimateStock.add(index, stock);
	}

	/**
	 * 获取指定胶料的预库存列表
	 * 
	 * @param glueCode 胶料号
	 * @return
	 */
	public List<EstimateStockVo> getEstimateStock(String glueCode) {
		if (glueCode == null) {
			return new ArrayList<>(0);
		}
		List<EstimateStockVo> estimateStock = glueStock.get(glueCode);
		if (estimateStock == null) {
			estimateStock = new ArrayList<>();
			glueStock.put(glueCode, estimateStock);
		}
		return estimateStock;
	}

	/**
	 * 获取胶料指定时间的预库存列表<br/>
	 * 仅统计指定时间点之前的库存加值 以及 所有的减值
	 * 
	 * @param glueCode    胶料号
	 * @param currentTime 当前时间
	 * @return
	 */
	public List<EstimateStockVo> getEstimateStock(String glueCode, Date currentTime) {
		if (glueCode == null || currentTime == null) {
			return new ArrayList<>(0);
		}
		List<EstimateStockVo> estimateStock = this.getEstimateStock(glueCode);
		return estimateStock.stream().filter(
				s -> s.getUpdateTime().compareTo(currentTime) <= 0 || s.getStockNum().compareTo(BigDecimal.ZERO) < 0)
				.collect(Collectors.toList());
	}

	/**
	 * 获取胶料指定时间的预库存量<br/>
	 * 仅统计指定时间点之前的库存加值之和 以及 所有的减值之和
	 * 
	 * @param glueCode    胶料号
	 * @param currentTime 当前时间
	 * @return
	 */
	public BigDecimal getEstimateStockNum(String glueCode, Date currentTime) {
		if (glueCode == null || currentTime == null) {
			return BigDecimal.ZERO;
		}
		List<EstimateStockVo> estimateStock = this.getEstimateStock(glueCode, currentTime);
		return estimateStock.stream().map(EstimateStockVo::getStockNum).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
