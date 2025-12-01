package com.zlt.mix.schedule.mapper;

import java.util.List;

import com.zlt.mix.schedule.api.domain.entity.GlueScheduleSupplement;
import com.zlt.mix.setting.api.domain.entity.GlueCommonDemand;

/**
 * 生产补量
 *
 */
public interface GlueScheduleSupplementMapper {
	/**
	 * 检查当天是否已经补量
	 * 
	 * @param supplement
	 * @return
	 */
	boolean hasSupplement(GlueScheduleSupplement supplement);

	/**
	 * 查询生产补量数据
	 * 
	 * @param supplement
	 * @return
	 */
	List<GlueScheduleSupplement> listGlueScheduleSupplement(GlueScheduleSupplement supplement);

	/**
	 * 查询密炼机常用大规格设置列表
	 * 
	 * @param glueCommonDemand 密炼机常用大规格设置
	 * @return 密炼机常用大规格设置集合
	 */
	List<GlueCommonDemand> listGlueCommonDemandList(GlueCommonDemand glueCommonDemand);

	/**
	 * 保存补量数据
	 * 
	 * @param supplementList
	 * @return
	 */
	int saveSupplement(List<GlueScheduleSupplement> supplementList);
}
