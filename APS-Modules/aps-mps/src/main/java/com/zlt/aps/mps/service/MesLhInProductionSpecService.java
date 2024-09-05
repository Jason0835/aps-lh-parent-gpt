package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 硫化机台当前生产规格服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-22 14:04:02
 */
public interface MesLhInProductionSpecService {
	/**
	 * 完成量同步
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	AjaxResult mergeData(String dataVersion);
}
