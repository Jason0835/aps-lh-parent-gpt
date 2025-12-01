package com.zlt.aps.cx.mapper.entity;


import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.MdmMonthProdPlan;
import com.zlt.aps.cxlh.cx.api.domain.vo.TMesConstructionInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.TMesConstructionMapping;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
	 * @param embryoCode    胎胚号
	 * @param embryoVersion 胎胚版本
	 * @param materialType  物料类型
	 * @return
	 */
	List<TMesConstructionInfo> selectTMesConstructionInfo(@Param("embryoCode") String embryoCode,
														  @Param("embryoVersion") String embryoVersion, @Param("materialType") String materialType,
														  @Param("enable") boolean enable);
}
