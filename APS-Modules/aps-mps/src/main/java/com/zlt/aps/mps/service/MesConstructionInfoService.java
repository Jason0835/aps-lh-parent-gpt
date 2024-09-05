package com.zlt.aps.mps.service;

import java.util.Date;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 施工标同步接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-9-13 14:07:21
 */
public interface MesConstructionInfoService {

	/**
	 * 将指定版本的bom数据合并到施工表中
	 * 
	 * @param dataVersion bom数据版本
	 * @return
	 */
	AjaxResult mergeBomToConstruction(String dataVersion);

	/**
	 * 将指定版本的plm数据合并到施工表中
	 * 
	 * @param dataVersion plm数据版本
	 * @return
	 */
	AjaxResult mergePlmToConstruction(String dataVersion);
	
	/**
	 * 将指定时间之后的施工版本更新到投产施工表中
	 * @param updateTime
	 * @return
	 */
	AjaxResult mergeProductConstructionInfo(Date updateTime);
}
