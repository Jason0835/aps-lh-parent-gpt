package com.zlt.mix.schedule.engine.util.event;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.MesPmtRecipeVo;

/**
 * 机台首批生产结束事件，用于添加胶料冷却计时
 * 
 * @author hakimryan
 *
 */
public class FinishFirstBatchEvent implements ScheduleEvent {
	private GlueScheduleResultVo scheduleResult;
	private BigDecimal productQty;

	public FinishFirstBatchEvent(GlueScheduleResultVo scheduleResult, BigDecimal productQty) {
		this.scheduleResult = scheduleResult;
		this.productQty = productQty;
	}

	/**
	 * 执行生产结束事件
	 */
	@Override
	public void excute(ScheduleEventQueue queue) {
		String machineCode = scheduleResult.getMachineCode();
		MesPmtRecipeVo recipe = scheduleResult.getPmtRecipe();
		Map<String, String> params = queue.getParams();
		Date currentTime = queue.getCurrentTime();

		// 计算冷却时间
		Long minParkTime = recipe.getMinParkTime() * 60 * 60; // 物料最少停放时长,需要把小时换算成秒
		Long switchTime = new Long(params.getOrDefault(GlueEngineConstants.SCHEDULE_SWITCH_TIME, "0")); // 排程切换时间
		long cooldownTime = minParkTime > switchTime ? minParkTime - switchTime : 0;
		// 库存更新时间 = 当前时间 + 物料最少停放时长
		Date finishDate = DateUtils.addSeconds(currentTime, (int) cooldownTime);
		// 往队列添加胶料停放完成事件
		queue.addEvent(new GuleColdDownEvent(scheduleResult, productQty), finishDate);
		queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|开始停放" + scheduleResult.getGlue()
				+ "+" + machineCode);
	}
}
