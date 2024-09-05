package com.zlt.aps.cx.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cx.api.domain.entity.CxCheckConstruction;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;

/**
 * 施工信息检测Mapper接口
 * 
 * @author Gim
 * @date 2022-02-25
 */
public interface CxCheckConstructionMapper {

	/**
	 * 查询施工信息检测列表
	 * 
	 * @param cxCheckConstruction 施工信息检测
	 * @return 施工信息检测集合
	 */
	public List<CxCheckConstruction> selectCxCheckConstructionList(CxCheckConstruction cxCheckConstruction);

	/**
	 * 新增施工信息检测
	 * 
	 * @param cxCheckConstruction 施工信息检测
	 * @return 结果
	 */
	public int insertCxCheckConstruction(CxCheckConstruction cxCheckConstruction);

	/**
	 * 查询待检查的施工列表，抓取指定年月的月度计划关联的施工信息
	 * 
	 * @param year  年份
	 * @param month 月份
	 * @return
	 */
	List<CxProductConstructionInfo> listCheckConstructionInfo(@Param("year") String year, @Param("month") String month);
	
	/**
	 * 查询待检查的施工信息
	 * @param embryoCode	胎胚编号
	 * @param bomVersion	施工版本
	 * @return
	 */
	List<CxProductConstructionInfo> getCheckConstructionInfo(@Param("embryoCode") String embryoCode, @Param("bomVersion") String bomVersion);
}
