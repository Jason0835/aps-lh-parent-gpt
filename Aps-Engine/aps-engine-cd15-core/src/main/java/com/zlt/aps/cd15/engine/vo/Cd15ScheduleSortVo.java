package com.zlt.aps.cd15.engine.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 15度裁断排序值对象
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-10 11:13:26
 * @Version 1.0
 */
@Data
public class Cd15ScheduleSortVo {
	/** 钢压大卷编号 */
	private String bigRollCode;
	/**
	 * 裁断角度
	 */
	private Double cuttingAngle;
	/**
	 * 本分组排产个数
	 */
	private int scheduleNumber;
	/**
	 * 可供时长
	 */
	private Double supplyTime;
	/**
	 * 同一分组的15度排产记录
	 */
	private List<Cd15ScheduleResultVo> scheduleList = new ArrayList<>();
}
