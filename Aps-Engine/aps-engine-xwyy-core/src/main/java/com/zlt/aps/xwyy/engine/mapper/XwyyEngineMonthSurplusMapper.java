package com.zlt.aps.xwyy.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.xwyy.engine.vo.XwyyMonthSurplusVo;

/**
 *纤维压延月度计划数据Mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:37:24
 * @Version 1.0
 */
public interface XwyyEngineMonthSurplusMapper {
	
	/**
	 * 获取指定月份的纤维压延月度计划
	 * 
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-22 11:38:42
	 * @param year  年
	 * @param month 月
	 * @return 符合条件的月度计划列表
	 */
	List<XwyyMonthSurplusVo> listXwyyMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
