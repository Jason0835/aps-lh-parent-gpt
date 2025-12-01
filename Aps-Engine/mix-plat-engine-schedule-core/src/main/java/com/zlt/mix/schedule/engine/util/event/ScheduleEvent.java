package com.zlt.mix.schedule.engine.util.event;

import com.zlt.mix.schedule.engine.util.ScheduleEventQueue;

/**
 * 排产事件
 * 
 * @author hakimryan
 *
 */
public interface ScheduleEvent {
	/**
	 * 事件执行
	 * 
	 * @param queue 事件队列
	 */
	void excute(ScheduleEventQueue queue);
}
