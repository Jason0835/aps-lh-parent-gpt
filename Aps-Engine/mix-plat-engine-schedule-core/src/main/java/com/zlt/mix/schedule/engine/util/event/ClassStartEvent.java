package com.zlt.mix.schedule.engine.util.event;

import java.util.Date;

import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;
import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;
import com.zlt.mix.schedule.engine.util.ShiftClassUtil;
import com.zlt.mix.schedule.engine.vo.GlueScheduleMachineProductVo;

/**
 * 开班事件，主要根据班次状态开启或者关闭机台
 * 
 * @author hakimryan
 *
 */
public class ClassStartEvent implements ScheduleEvent {

	@Override
	public void excute(ScheduleEventQueue queue) {
		Date currentTime = queue.getCurrentTime();
		Integer shiftClass = ShiftClassUtil.getShiftClass(currentTime);

		for (GlueScheduleMachineProductVo machineProduct : queue.getMachineProductMap().values()) {
			boolean classStatues = machineProduct.getStatus(shiftClass); // 班次状态
			String productState = machineProduct.getState(); // 机台生产状态
			// 根据切换机台的关机与待机状态
			// 正在生产的机台不动，在生产完成切换的时候处理
			if (classStatues && GlueEngineConstants.MACHINE_STATE_OFF.equals(productState)) {
				// 机台班次状态可用，且机台处于关机状态，则切换至待机状态
				machineProduct.setState(GlueEngineConstants.MACHINE_STATE_WAIT);
			} else if (!classStatues && GlueEngineConstants.MACHINE_STATE_WAIT.equals(productState)) {
				// 机台班次状态不可用，且机台处于待机状态，则切换至关机状态
				machineProduct.setState(GlueEngineConstants.MACHINE_STATE_OFF);
			}
		}

		queue.addLog(shiftClass + "班开班！");
	}

}
