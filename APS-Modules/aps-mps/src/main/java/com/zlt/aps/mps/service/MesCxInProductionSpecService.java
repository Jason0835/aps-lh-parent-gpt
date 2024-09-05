package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型机台当前生产规格接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 13:05:41
 */
public interface MesCxInProductionSpecService {
	
	/**
	 * 完成量同步
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	AjaxResult mergeSpes(String dataVersion);
}
