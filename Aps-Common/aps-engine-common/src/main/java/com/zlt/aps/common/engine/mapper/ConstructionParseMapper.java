package com.zlt.aps.common.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.common.engine.domain.MdmMonthProdPlan;
import com.zlt.aps.common.engine.domain.TMesConstructionInfo;
import com.zlt.aps.common.engine.domain.TMesConstructionMapping;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;

/**
 * 施工标参数数据库操作接口
 * 
 * @Description
 */
public interface ConstructionParseMapper {
	/**
	 * 查询施工表字段映射配置列表
	 * 
	 * @return
	 */
	List<TMesConstructionMapping> selectTPlmBomConstructionMapping();

	/**
	 * 查询中间库的施工表数据，需要根据胎胚号与胎胚版本
	 * 
	 * @param empryoCode    胎胚号
	 * @param empryoVersion 胎胚版本
	 * @param materialType  物料类型
	 * @return
	 */
	List<TMesConstructionInfo> selectTMesConstructionInfo(@Param("embryoCode") String embryoCode,
			@Param("embryoVersion") String embryoVersion, @Param("materialType") String materialType,
			@Param("enable") boolean enable);
	
	/**
	 * 获取月计划中的版本对应的施工信息
	 * @param list
	 * @return
	 */
	List<CxProductConstructionInfo> listEmbryoVersion(@Param("prodList") List<MdmMonthProdPlan> prodList);

	/**
	 * 初始化投产施工指定胎胚号的施工信息
	 * 
	 * @param embryoCodeList 胎胚号列表
	 */
	void initProductConstructionInfo(@Param("embryoCodeList") List<String> embryoCodeList);

	/**
	 * 获取指定胎胚的单版本投产施工记录，如果有多个版本则不返回信息
	 * 
	 * @param embryoCodeList 胎胚号列表
	 * @return
	 */
	List<CxProductConstructionInfo> selectSingleVersionConstruction(
			@Param("embryoCodeList") List<String> embryoCodeList);
	
	/**
	 * 获取最近一次月度计划版本中选择的版本信息
	 * @return
	 */
	List<MdmMonthProdPlan> selectLatestProdPlanBomDataVersionList();
}
