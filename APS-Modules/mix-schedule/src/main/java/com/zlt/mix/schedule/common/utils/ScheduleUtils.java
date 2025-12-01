package com.zlt.mix.schedule.common.utils;

import java.util.Date;

import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.schedule.engine.util.ShiftClassUtil;

/**
 * 排程工具类
 * 
 * @author hakimryan
 *
 */
public class ScheduleUtils {

	/**
	 * 检查班次是否可编辑
	 * 
	 * @param scheduleDate 待确认排产日期
	 * @param classShift   待确认班次
	 * @return
	 */
	public static Boolean checkCLassEditable(Date scheduleDate, Integer classShift) {
		if (scheduleDate == null || classShift == null) {
			return false;
		}
		Date currentTime = DateUtil.now();
		Date nowScheduleDate = ShiftClassUtil.getScheduleDate(currentTime); // 当前时间所属排产日期
		if (scheduleDate.compareTo(nowScheduleDate) != 0) { // 如果单据排产日不是当前所属排产日，则只能编辑未来的单据
			return nowScheduleDate.compareTo(scheduleDate) < 0;
		} else {
			Integer nowClass = ShiftClassUtil.getShiftClass(currentTime); // 当前时间所在班次
			return nowClass <= classShift; // 只允许编辑含当前班次之后的班次数据
		}
	}
}
