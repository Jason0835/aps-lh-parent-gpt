package com.zlt.mix.schedule.engine.util.event;

import java.math.BigDecimal;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;

/**
 * 胶料库存更新事件，用于将胶料补充到库存中
 * 
 * @author hakimryan
 *
 */
public class GlueStockUpdateEvent implements ScheduleEvent {
	/**
	 * 物料大类
	 */
	private String majorType;
	/**
	 * 胶料
	 */
	private String glueCode;
	/**
	 * 库存量
	 */
	private BigDecimal stockQty;
	/**
	 * 库存重量
	 */
	private BigDecimal stockWeightQty;

	public GlueStockUpdateEvent(String glueCode, String majorType, BigDecimal stockQty, BigDecimal stockWeightQty) {
		this.glueCode = glueCode;
		this.majorType = majorType;
		this.stockQty = stockQty;
		this.stockWeightQty = stockWeightQty;
	}

	/**
	 * 执行库存更新事件
	 */
	@Override
	public void excute(ScheduleEventQueue queue) {
		GlueScheduleStockPool glueStock = queue.getGlueStock();
		if (this.stockQty != null && this.stockQty.compareTo(BigDecimal.ZERO) > 0) {
			glueStock.addStock(this.glueCode, this.majorType, this.stockQty); // 更新库存车数
		}
		if (this.stockWeightQty != null && this.stockWeightQty.compareTo(BigDecimal.ZERO) > 0) {
			glueStock.addStockWeight(this.glueCode, this.majorType, this.stockWeightQty); // 更新库存重量
		}
		queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|库存更新" + glueCode);
	}
}
