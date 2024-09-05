package com.zlt.aps.mps.mapper;

import java.util.Date;

import org.apache.ibatis.annotations.Param;

/**
 * 月度计划统计数据mapper
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-15 10:48:48
 */
public interface MonthPlanStatisticsMapper {
	/**
	 * 更新月度计划的实际超欠产
	 * 
	 * @param year      年
	 * @param month     月
	 * @param startDate 开始日期
	 * @param endDate   结束日期
	 * @return
	 */
	int updateActualOverProduction(@Param("year") String year, @Param("month") String month,
			@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
