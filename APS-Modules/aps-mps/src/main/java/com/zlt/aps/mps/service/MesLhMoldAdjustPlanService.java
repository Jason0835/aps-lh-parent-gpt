package com.zlt.aps.mps.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
/**
 * 硫化工序模具调整计划接口服务
 * 
 * @Description
 * @Author zlt
 * @Date 2022-3-22 14:04:07
 */
public interface MesLhMoldAdjustPlanService {
	/**
	 * 合并数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	AjaxResult mergeData(String dataVersion);
}
