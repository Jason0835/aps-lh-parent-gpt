package com.zlt.mix.schedule.engine.util.event;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;
import com.zlt.mix.schedule.engine.util.ScheduleEventUtils;
import com.zlt.mix.schedule.engine.util.ShiftClassUtil;
import com.zlt.mix.schedule.engine.vo.GlueScheduleMachineProductVo;
import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 机台生产结束事件，用于释放机台，并添加胶料冷却
 *
 * @author hakimryan
 */
public class FinishProductEvent implements ScheduleEvent {
	private GlueScheduleResultVo scheduleResult;
	private final static BigDecimal ONE_THOUSAND = new BigDecimal("1000");
	/**
	 * 如果提前预占产能，就标记为false
	 */
	private Boolean updateProductTimeTag = true;

	public FinishProductEvent(GlueScheduleResultVo scheduleResult) {
		this.scheduleResult = scheduleResult;
	}
	
	public FinishProductEvent(GlueScheduleResultVo scheduleResult, Boolean updateProductTimeTag) {
		this.scheduleResult = scheduleResult;
		this.updateProductTimeTag = updateProductTimeTag;
	}

	/**
	 * 执行生产结束事件
	 */
	@Override
	public void excute(ScheduleEventQueue queue) {
		String machineCode = scheduleResult.getMachineCode();
		Date currentTime = queue.getCurrentTime();

		// 更新机台生产状态
		GlueScheduleMachineProductVo machineProduct = queue.getMachineProduct(machineCode);
		machineProduct.setState(GlueEngineConstants.MACHINE_STATE_WAIT);// 机台状态设置回空闲
		Date startProductTime = machineProduct.getStartProductTime();
		Integer startProduceShiftClass = ShiftClassUtil.getShiftClass(startProductTime); // 当前班次为开始时间的所在班次

		if (Boolean.TRUE.equals(updateProductTimeTag)) {
			// 扣减掉生产时长，扣减量 = 当前时间 - 开始生产时间
			BigDecimal productTime = machineProduct.getProductTime(startProduceShiftClass);
			productTime = productTime.subtract(new BigDecimal(currentTime.getTime() - startProductTime.getTime())
					.divide(ONE_THOUSAND, 0, RoundingMode.DOWN)); // 换算成秒
			machineProduct.updateProductTime(productTime, startProduceShiftClass);
		}

		// 更新排程的生产状态为空闲
		Integer currentShiftClass = ShiftClassUtil.getShiftClass(currentTime); // 当前班次为开始时间的所在班次
		Boolean classStatues = machineProduct.getStatus(currentShiftClass);
		// 机台班次状态可用，则切换至待机状态，否则切换至关机状态
		machineProduct.setState(
				classStatues ? GlueEngineConstants.MACHINE_STATE_WAIT : GlueEngineConstants.MACHINE_STATE_OFF);
		scheduleResult.setProductState(GlueEngineConstants.MACHINE_STATE_WAIT);

        queue.addLog(DateUtils.parseDateToStr("HH:mm:ss", queue.getCurrentTime()) + "|生产完成" + scheduleResult.getGlue()
                + "+" + machineCode);

        // 存在接续生产的排产，需要连续排产
        ScheduleEventUtils.continueSchedule(queue, scheduleResult, currentTime, machineProduct);
    }
}
