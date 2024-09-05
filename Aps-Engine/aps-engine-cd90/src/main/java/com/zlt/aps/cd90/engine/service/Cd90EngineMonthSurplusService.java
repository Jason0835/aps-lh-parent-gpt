package com.zlt.aps.cd90.engine.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.cd90.engine.vo.Cd90MonthSurplusVo;
import com.zlt.aps.cd90.engine.vo.Cd90ScheduleResultVo;

/**
 * 90度裁断根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:29:41
 * @Version 1.0
 */
public interface Cd90EngineMonthSurplusService {

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-14 11:34:10
	 * @param scheduleDate    排产日期
	 * @param scheduleList    90度裁断排程结果明细列表
	 * @param closeOutNum     收尾提醒阈值
	 * @param defaultLossRate 耗损率默认值
	 */
	void calculateMonthSurplus(Date scheduleDate, List<Cd90ScheduleResultVo> scheduleList, String closeOutNum,
			String defaultLossRate);

	/**
	 * 抓取排产日对应月份的月度计划生产信息
	 * 
	 * @param scheduleDate
	 * @return key：物料编号，value：月度生产计划
	 */
	Map<String, Cd90MonthSurplusVo> getMonthSurplusMap(Date scheduleDate);

	/**
	 * 设置收尾提示标识 和 生产状态字段
	 * 
	 * @param resultVo       排产明细
	 * @param monthSurplusVo 月度计划
	 * @param closeOutNum    收尾提醒阈值
	 */
	void setStatusAndCloseTip(Cd90ScheduleResultVo resultVo, Cd90MonthSurplusVo monthSurplusVo, String closeOutNum);
}
