package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.engine.vo.Cd15MonthSurplusVo;

/**
 * 15度裁断月度计划数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-12 11:37:24
 * @Version 1.0
 */
public interface Cd15EngineMonthSurplusMapper {
	/**
	 * 获取指定月份的15度裁断月度计划
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-12 11:38:42
	 * @param year  年
	 * @param month 月
	 * @return 符合条件的月度计划列表
	 */
	List<Cd15MonthSurplusVo> listCd15MonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
