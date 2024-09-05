package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 成型中夜班完成量接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 10:45:28
 */
public interface MesCxMidNightFinishService {
	
	/**
	 * 完成量同步
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	AjaxResult mergeFinishQty(String dataVersion);
}
