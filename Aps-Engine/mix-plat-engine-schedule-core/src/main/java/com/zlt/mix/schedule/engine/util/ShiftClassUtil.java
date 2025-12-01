package com.zlt.mix.schedule.engine.util;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.schedule.engine.constants.GlueEngineConstants;

import java.util.Date;

/**
 * 班次工具
 * 
 * @author hakimryan
 *
 */
public class ShiftClassUtil {
	// 各班开班时间与排产日0点的时间间隔
	private final static int MID_START_TIME_INTERVAL = -5;
	private final static int NIGHT_START_TIME_INTERVAL = 7;
	private final static int DAY_START_TIME_INTERVAL = 19;
	// 各班结束时间与排产日0点的时间间隔
	private final static int MID_END_TIME_INTERVAL = 7;
	private final static int NIGHT_END_TIME_INTERVAL = 19;
	private final static int DAY_END_TIME_INTERVAL = 19;
	// 一个班秒数（两班制）
	public final static long ONE_SHIFT_CLASS_TIME = 12 * 60 * 60;
	// 班制，目前两班制
	public final static int SHIFT_CLASS = 2;

	/**
	 * 获取当前班次的下一个班次
	 * 
	 * @param shiftClass 当前班次
	 * @return
	 */
	public static Integer getNextShiftClass(int shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return GlueEngineConstants.SHIFT_CLASS_NIGHT;
		// case GlueEngineConstants.SHIFT_CLASS_NIGHT:
		// 	return GlueEngineConstants.SHIFT_CLASS_DAY;
		default:
			return null;
		}
	}

	/**
	 * 获取当前班次的上一个班次
	 * 
	 * @param shiftClass 当前班次
	 * @return
	 */
	public static Integer getPreviousShiftClass(int shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_DAY:
			return GlueEngineConstants.SHIFT_CLASS_NIGHT;
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return GlueEngineConstants.SHIFT_CLASS_MID;
		// 夜班的上一个班，为前一天早班
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return GlueEngineConstants.SHIFT_CLASS_DAY;
		default:
			return null;
		}
	}

	/**
	 * 获取各班开班具体时间点
	 * 
	 * @param scheduleDate 排产日
	 * @param shiftClass   班次
	 * @return
	 */
	public static Date getShiftClassStartTime(Date scheduleDate, int shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return DateUtils.addHours(scheduleDate, MID_START_TIME_INTERVAL);
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return DateUtils.addHours(scheduleDate, NIGHT_START_TIME_INTERVAL);
		default:
			return DateUtils.addHours(scheduleDate, DAY_START_TIME_INTERVAL);
		}
	}

	/**
	 * 获取各班结束具体时间点
	 * 
	 * @param scheduleDate 排产日
	 * @param shiftClass   班次
	 * @return
	 */
	public static Date getShiftClassEndTime(Date scheduleDate, int shiftClass) {
		switch (shiftClass) {
		case GlueEngineConstants.SHIFT_CLASS_MID:
			return DateUtils.addHours(scheduleDate, MID_END_TIME_INTERVAL);
		case GlueEngineConstants.SHIFT_CLASS_NIGHT:
			return DateUtils.addHours(scheduleDate, NIGHT_END_TIME_INTERVAL);
		default:
			return DateUtils.addHours(scheduleDate, DAY_END_TIME_INTERVAL);
		}
	}

	/**
	 * 根据时间获取所在班次
	 * 
	 * @param dateTime 时间
	 * @return
	 */
	public static Integer getShiftClass(Date dateTime) {
		if (dateTime == null) {
			return null;
		}
		return getShiftClass(new Integer(DateUtils.parseDateToStr("HH", dateTime)));
	}

	/**
	 * 根据小时数获取班次
	 * 
	 * @param hour 小时数
	 * @return
	 */
	public static int getShiftClass(int hour) {
		if (hour >= 19 && hour <= 23) {
			return GlueEngineConstants.SHIFT_CLASS_MID;
		} else if (hour >= 0 && hour < 7) {
			return GlueEngineConstants.SHIFT_CLASS_MID;
		} else {
			return GlueEngineConstants.SHIFT_CLASS_NIGHT;
		}
	}
	
	/**
	 * 获取实际排产日
	 * @param date
	 * @return
	 */
	public static Date getScheduleDate(Date date) {
		if (date == null) {
			return date;
		}
		String hour = DateUtils.parseDateToStr("HH", date); // 小时
		if (Integer.parseInt(hour) >= DAY_END_TIME_INTERVAL) {
			return DateUtil.thatDay(DateUtils.addDays(date, 1));
		} else {
			return DateUtil.thatDay(date);
		}
	}
}
