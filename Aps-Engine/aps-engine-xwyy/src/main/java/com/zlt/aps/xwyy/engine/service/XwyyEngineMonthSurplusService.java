package com.zlt.aps.xwyy.engine.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.xwyy.engine.vo.XwyyMonthSurplusVo;
import com.zlt.aps.xwyy.engine.vo.XwyyScheduleResultVo;

/**
 * 纤维压延根据月度计划调整计划量服务
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:29:41
 * @Version 1.0
 */
public interface XwyyEngineMonthSurplusService {

	/**
	 * 计算月度计划量
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:34:10
	 * @param scheduleDate 排产日期
	 * @param scheduleList 纤维压延排程结果明细列表
	 * @param closeOutNum  收尾提醒阈值
	 */
	void calculateMonthSurplus(Date scheduleDate, List<XwyyScheduleResultVo> scheduleList, String closeOutNum);

	/**
	 * 抓取排产日对应月份的月度计划生产信息
	 * 
	 * @param scheduleDate
	 * @return key：物料编号，value：月度生产计划
	 */
	Map<String, XwyyMonthSurplusVo> getMonthSurplusMap(Date scheduleDate);

	/**
	 * 设置收尾提示标识 和 生产状态字段
	 * 
	 * @param resultVo       排产明细
	 * @param monthSurplusVo 月度计划
	 * @param closeOutNum    收尾提醒阈值
	 */
	void setStatusAndCloseTip(XwyyScheduleResultVo resultVo, XwyyMonthSurplusVo monthSurplusVo, String closeOutNum);
}
