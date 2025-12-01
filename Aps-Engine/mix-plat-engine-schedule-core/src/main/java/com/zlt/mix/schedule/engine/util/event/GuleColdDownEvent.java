package com.zlt.mix.schedule.engine.util.event;

import java.math.BigDecimal;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.engine.util.GlueScheduleStockPool;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;

/**
 * 胶料冷却事件，用于将完成冷却的胶料补充到库存中
 * 
 * @author hakimryan
 *
 */
public class GuleColdDownEvent implements ScheduleEvent {
	private GlueScheduleResultVo scheduleResult;
	private BigDecimal productQty;

	public GuleColdDownEvent(GlueScheduleResultVo scheduleResult, BigDecimal productQty) {
		this.scheduleResult = scheduleResult;
		this.productQty = productQty;
	}

	/**
	 * 执行胶料冷却事件
	 */
	@Override
	public void excute(ScheduleEventQueue queue) {
		String majorType = scheduleResult.getMajorType();
		String glueCode = scheduleResult.getGlue();
		GlueScheduleStockPool glueStock = queue.getGlueStock();
		// 完成停放的库存增加到库存池中
		glueStock.addStock(glueCode, majorType, productQty); // 更新库存车数
		// 车数换算成重量
		MesPmtRecipeVo recipe = scheduleResult.getPmtRecipe();
		BigDecimal stockWeight = new BigDecimal(recipe.getLotTotalWeight().toString()).multiply(productQty);
		glueStock.addStockWeight(glueCode, majorType, stockWeight); // 更新库存重量
		queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|库存更新" + scheduleResult.getGlue());
	}
}
